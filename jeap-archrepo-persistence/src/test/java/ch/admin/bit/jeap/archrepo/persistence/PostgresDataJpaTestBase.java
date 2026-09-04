package ch.admin.bit.jeap.archrepo.persistence;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Points a {@code @DataJpaTest} at the module's PostgreSQL container instead of at an in-memory database.
 * <p>
 * An in-memory stand-in accepts SQL that PostgreSQL rejects - an untyped parameter, a cast it does not have -
 * and orders rows differently, so a green suite said nothing about the database the service actually runs on.
 * <p>
 * Only the data source is contributed here: the test classes keep their own {@code @DataJpaTest} declarations,
 * because several of them need Hibernate properties of their own. Every test runs in a transaction that is
 * rolled back, so they share one database without seeing each other's rows.
 */
public abstract class PostgresDataJpaTestBase {

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ArchRepoPostgresTestContainer.container()::getJdbcUrl);
        registry.add("spring.datasource.username", ArchRepoPostgresTestContainer.container()::getUsername);
        registry.add("spring.datasource.password", ArchRepoPostgresTestContainer.container()::getPassword);
    }
}
