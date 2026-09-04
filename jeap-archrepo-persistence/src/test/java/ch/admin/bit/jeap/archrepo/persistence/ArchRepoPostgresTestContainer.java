package ch.admin.bit.jeap.archrepo.persistence;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

/**
 * The one PostgreSQL of this module's tests.
 * <p>
 * The version is the one the service runs on in production (Aurora PostgreSQL 17): the point of testing against
 * a container instead of an in-memory database is that the database answers the way the deployed one does.
 * <p>
 * Started once for the whole test JVM and never stopped - the Spring contexts are cached and shared between the
 * test classes, so a container managed per class would be gone while a context still used it.
 */
public final class ArchRepoPostgresTestContainer {

    private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine"));

    static {
        CONTAINER.start();
    }

    private ArchRepoPostgresTestContainer() {
    }

    public static PostgreSQLContainer container() {
        return CONTAINER;
    }

    /**
     * A database of its own on the shared container, for a test that cannot share one - a migration test, which
     * needs a database no migration has run on yet.
     *
     * @return the JDBC url of the new database; user and password are the container's
     */
    public static String createDatabase() {
        String name = "db_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(
                CONTAINER.getJdbcUrl(), CONTAINER.getUsername(), CONTAINER.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("create database " + name);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create the database " + name, e);
        }
        return CONTAINER.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/" + name + "$1");
    }
}
