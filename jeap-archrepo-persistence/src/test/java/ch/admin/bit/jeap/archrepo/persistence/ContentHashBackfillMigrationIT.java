package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.ContentHash;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code V2_6_0} backfill against a database that is in the state the previous release left behind.
 * <p>
 * Flyway is driven directly rather than through Spring, because the point is the transition: migrate to
 * {@code 2.5.0}, put artifacts there the way the old code would have left them - a hash column that exists but is
 * empty - and only then let the new migration run.
 */
class ContentHashBackfillMigrationIT {

    private static final byte[] SPEC = "{\"openapi\":\"3.0.0\"}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SCHEMA = "{\"name\":\"s\",\"version\":\"42\",\"tables\":[]}".getBytes(StandardCharsets.UTF_8);
    private static final Timestamp STORED_AT = Timestamp.from(Instant.parse("2020-01-01T00:00:00Z"));

    private String url;
    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        url = "jdbc:h2:mem:backfill-" + UUID.randomUUID()
              + ";INIT=CREATE SCHEMA IF NOT EXISTS data;DATABASE_TO_UPPER=FALSE;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        connection = DriverManager.getConnection(url, "sa", "");
        // The migrations create their objects in the 'data' schema; the migration itself resolves them through
        // Flyway's default schema, this connection has to be pointed at it explicitly
        execute("set schema data");
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    @Test
    void backfillsTheHashOfArtifactsStoredBeforeTheColumnExisted() throws Exception {
        migrateTo("2.5.0");
        UUID specId = insertOpenApiSpec(SPEC);
        UUID schemaId = insertDatabaseSchema(SCHEMA);
        assertThat(contentHashOf("open_api_spec", specId)).isNull();
        assertThat(contentHashOf("system_component_database_schema", schemaId)).isNull();

        migrateTo(null);

        assertThat(contentHashOf("open_api_spec", specId)).isEqualTo(ContentHash.of(SPEC));
        assertThat(contentHashOf("system_component_database_schema", schemaId)).isEqualTo(ContentHash.of(SCHEMA));
    }

    @Test
    void doesNotTouchTheModificationTime() throws Exception {
        // A backfill must not make an artifact look freshly published - the index reports modified_at
        migrateTo("2.5.0");
        UUID specId = insertOpenApiSpec(SPEC);

        migrateTo(null);

        assertThat(timestampOf("open_api_spec", specId, "created_at")).isEqualTo(STORED_AT);
        assertThat(timestampOf("open_api_spec", specId, "modified_at")).isNull();
    }

    @Test
    void leavesAnArtifactWithoutContentAlone() throws Exception {
        // open_api_spec.content is nullable; there is no hash to compute and the row must survive the migration
        migrateTo("2.5.0");
        UUID specId = insertOpenApiSpec(null);

        migrateTo(null);

        assertThat(contentHashOf("open_api_spec", specId)).isNull();
    }

    @Test
    void isIdempotentAndLeavesAnAlreadyHashedArtifactUntouched() throws Exception {
        migrateTo("2.5.0");
        UUID specId = insertOpenApiSpec(SPEC);
        execute("update open_api_spec set content_hash = 'kept' where id = '" + specId + "'");

        migrateTo(null);

        assertThat(contentHashOf("open_api_spec", specId)).isEqualTo("kept");
    }

    @Test
    void runsOnAnEmptyDatabase() throws Exception {
        migrateTo(null);

        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("select count(*) from flyway_schema_history where success = true")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isPositive();
        }
    }

    private void migrateTo(String target) {
        var configuration = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration/common")
                .defaultSchema("data")
                .schemas("data");
        if (target != null) {
            configuration = configuration.target(target);
        }
        configuration.load().migrate();
    }

    private UUID insertOpenApiSpec(byte[] content) throws Exception {
        UUID systemId = insertSystem();
        UUID componentId = insertComponent(systemId);
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement(
                "insert into open_api_spec (id, system_id, provider_id, content, created_at) values (?,?,?,?,?)")) {
            ps.setObject(1, id);
            ps.setObject(2, systemId);
            ps.setObject(3, componentId);
            ps.setBytes(4, content);
            ps.setTimestamp(5, STORED_AT);
            ps.executeUpdate();
        }
        return id;
    }

    private UUID insertDatabaseSchema(byte[] schema) throws Exception {
        UUID systemId = insertSystem();
        UUID componentId = insertComponent(systemId);
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement(
                "insert into system_component_database_schema "
                + "(id, system_id, system_component_id, schema, schema_version, created_at) values (?,?,?,?,?,?)")) {
            ps.setObject(1, id);
            ps.setObject(2, systemId);
            ps.setObject(3, componentId);
            ps.setBytes(4, schema);
            ps.setString(5, "42");
            ps.setTimestamp(6, STORED_AT);
            ps.executeUpdate();
        }
        return id;
    }

    private UUID insertSystem() throws Exception {
        UUID teamId = UUID.randomUUID();
        execute("insert into team (id, name, created_at) values ('" + teamId + "', 'team-" + teamId + "', now())");
        UUID systemId = UUID.randomUUID();
        execute("insert into system (id, name, default_owner_id, created_at) values ('" + systemId + "', 'sys-"
                + systemId + "', '" + teamId + "', now())");
        return systemId;
    }

    private UUID insertComponent(UUID systemId) throws Exception {
        UUID teamId;
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("select default_owner_id from system where id = '" + systemId + "'")) {
            rs.next();
            teamId = rs.getObject(1, UUID.class);
        }
        UUID componentId = UUID.randomUUID();
        execute("insert into system_component (id, name, system_id, team_id, type, created_at) values ('"
                + componentId + "', 'comp-" + componentId + "', '" + systemId + "', '" + teamId
                + "', 'BACKEND_SERVICE', now())");
        return componentId;
    }

    private void execute(String sql) throws Exception {
        try (Statement s = connection.createStatement()) {
            s.execute(sql);
        }
    }

    private String contentHashOf(String table, UUID id) throws Exception {
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("select content_hash from " + table + " where id = '" + id + "'")) {
            rs.next();
            return rs.getString(1);
        }
    }

    private Timestamp timestampOf(String table, UUID id, String column) throws Exception {
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("select " + column + " from " + table + " where id = '" + id + "'")) {
            rs.next();
            return rs.getTimestamp(1);
        }
    }
}
