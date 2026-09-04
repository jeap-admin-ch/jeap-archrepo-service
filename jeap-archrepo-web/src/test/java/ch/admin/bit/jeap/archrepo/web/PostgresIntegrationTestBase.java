package ch.admin.bit.jeap.archrepo.web;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Runs a test of the web module against a real PostgreSQL rather than against an in-memory database.
 * <p>
 * The version is the one the service runs on in production (Aurora PostgreSQL 17). An in-memory stand-in
 * accepts SQL that PostgreSQL rejects - an untyped parameter, a cast it does not have - so a green suite said
 * nothing about the database the service actually runs on.
 * <p>
 * The container is started once for the whole test JVM and never stopped: the Spring contexts are cached and
 * shared between the test classes, so a container managed per class would be gone while a context still used
 * it. One database serves all of them, exactly as the one in-memory database did before.
 */
public abstract class PostgresIntegrationTestBase {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
