package db.migration.common;

import ch.admin.bit.jeap.archrepo.metamodel.ContentHash;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fills the {@code content_hash} added by {@code V2_5_0} for the artifacts that were stored before it existed.
 * <p>
 * The hash is what the docs API serves as an entity tag, so every stored artifact needs one. {@code V2_5_0} cannot
 * compute it: SHA-256 is not available under the same name on every database the migrations run on. Doing it here
 * rather than lazily on first read keeps the read path free of writes - which is what lets those reads be routed
 * to a read replica - and guarantees the hash exists before the application serves its first request, because
 * migrations run before the persistence layer is started.
 * <p>
 * <b>The package is part of the configuration.</b> Flyway resolves a classpath location to a package when it looks
 * for Java migrations, and the library ships {@code spring.flyway.locations=classpath:db/migration/common}. Living
 * in {@code db.migration.common} is therefore what makes this migration discoverable without any instance having
 * to add a location.
 * <p>
 * Updating with plain SQL also leaves {@code modified_at} alone - the auditing callback that would otherwise bump
 * it is a JPA callback and does not apply here. A backfill must not make an artifact look freshly published.
 */
@SuppressWarnings("java:S101") // Flyway derives the version and the description from the class name
public class V2_6_0__Backfill_content_hash extends BaseJavaMigration {

    private static final int BATCH_SIZE = 50;

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        backfill(connection, "open_api_spec", "content");
        backfill(connection, "system_component_database_schema", "schema");
    }

    private void backfill(Connection connection, String table, String contentColumn) throws SQLException {
        List<UUID> ids = idsWithoutHash(connection, table, contentColumn);
        if (ids.isEmpty()) {
            return;
        }
        // The content is read one row at a time rather than in one result set, so a landscape with many large
        // artifacts does not have to fit into memory at once.
        try (PreparedStatement read = connection.prepareStatement(
                     "select " + contentColumn + " from " + table + " where id = ?");
             PreparedStatement write = connection.prepareStatement(
                     "update " + table + " set content_hash = ? where id = ?")) {
            int pending = 0;
            for (UUID id : ids) {
                byte[] content = readContent(read, id);
                if (content == null) {
                    continue;
                }
                write.setString(1, ContentHash.of(content));
                write.setObject(2, id);
                write.addBatch();
                if (++pending == BATCH_SIZE) {
                    write.executeBatch();
                    pending = 0;
                }
            }
            if (pending > 0) {
                write.executeBatch();
            }
        }
    }

    private List<UUID> idsWithoutHash(Connection connection, String table, String contentColumn) throws SQLException {
        List<UUID> ids = new ArrayList<>();
        String sql = "select id from " + table
                     + " where content_hash is null and " + contentColumn + " is not null";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                ids.add(rs.getObject(1, UUID.class));
            }
        }
        return ids;
    }

    private byte[] readContent(PreparedStatement read, UUID id) throws SQLException {
        read.setObject(1, id);
        try (ResultSet rs = read.executeQuery()) {
            return rs.next() ? rs.getBytes(1) : null;
        }
    }
}
