package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.system.Gateway;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/common")
class GatewayComponentMigrationTest {

    private static final String MIGRATION = "db/migration/common/V2_4_0__migrate-gateway-components.sql";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SystemComponentRepository systemComponentRepository;

    @Test
    void migrateLegacyGatewayType() {
        UUID teamId = UUID.randomUUID();
        UUID systemId = UUID.randomUUID();
        UUID gatewayId = UUID.randomUUID();
        UUID frontendId = UUID.randomUUID();
        UUID backendId = UUID.randomUUID();

        jdbcTemplate.update("INSERT INTO team (id, name, created_at) VALUES (?, ?, ?)",
                teamId, "gateway-team", ZonedDateTime.now());
        jdbcTemplate.update("INSERT INTO system (id, name, default_owner_id, created_at) VALUES (?, ?, ?, ?)",
                systemId, "gateway-system", teamId, ZonedDateTime.now());
        insertComponent(gatewayId, "test-gateway", "BACKEND_SERVICE", systemId, teamId);
        insertComponent(frontendId, "frontend-gateway", "FRONTEND", systemId, teamId);
        insertComponent(backendId, "test-service", "BACKEND_SERVICE", systemId, teamId);

        new ResourceDatabasePopulator(new ClassPathResource(MIGRATION)).execute(dataSource);

        assertThat(componentType(gatewayId)).isEqualTo("GATEWAY");
        assertThat(componentType(frontendId)).isEqualTo("FRONTEND");
        assertThat(componentType(backendId)).isEqualTo("BACKEND_SERVICE");

        entityManager.clear();
        SystemComponent migratedGateway = systemComponentRepository.findById(gatewayId).orElseThrow();
        assertThat(migratedGateway)
                .isInstanceOf(Gateway.class)
                .extracting(SystemComponent::getType)
                .isEqualTo(SystemComponentType.GATEWAY);
    }

    private void insertComponent(UUID id, String name, String type, UUID systemId, UUID teamId) {
        jdbcTemplate.update("""
                INSERT INTO system_component (id, name, system_id, team_id, type, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, name, systemId, teamId, type, ZonedDateTime.now());
    }

    private String componentType(UUID id) {
        return jdbcTemplate.queryForObject("SELECT type FROM system_component WHERE id = ?", String.class, id);
    }
}
