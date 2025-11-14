package net.netbeing.cheap.integrationtests.base;

import ch.vorburger.exec.ManagedProcessException;
import net.netbeing.cheap.db.mariadb.MariaDbCheapSchema;
import net.netbeing.cheap.integrationtests.util.MariaDbRunnerExtension;
import net.netbeing.cheap.integrationtests.util.MariaDbIntegrationTestDb;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mariadb.jdbc.MariaDbDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base class for integration tests using MariaDB database.
 * Uses DatabaseRunnerExtension to start a single MariaDB4j instance,
 * then creates a separate database for each test class.
 */
@ActiveProfiles("mariadb-test")
@ExtendWith(MariaDbRunnerExtension.class)
@SuppressWarnings("unused")
public abstract class MariaDbRestIntegrationTest extends BaseRestIntegrationTest
{
    protected MariaDbIntegrationTestDb testDb;

    /**
     * Override to specify whether to use foreign keys.
     * Default is true.
     */
    protected boolean useForeignKeys()
    {
        return true;
    }

    /**
     * Override to specify the database name.
     * Default is the simple class name in lowercase.
     */
    protected String getDatabaseName()
    {
        return getClass().getSimpleName().toLowerCase();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) throws ManagedProcessException, SQLException
    {
        // Configure Spring to use the MariaDB4j instance's actual port and database
        // The testDb will be initialized during test setup, but we need the port from the shared MariaDB4j instance
        if (MariaDbRunnerExtension.getDbConfig() != null && MariaDbRunnerExtension.getMariaDB() != null)
        {
            int port = MariaDbRunnerExtension.getDbConfig().getPort();
            // Create a default 'test' database for Spring Boot to connect to
            MariaDbRunnerExtension.getMariaDB().createDB("test");

            // Initialize schema in the test database
            MariaDbDataSource dataSource = new MariaDbDataSource();
            dataSource.setUrl("jdbc:mariadb://localhost:" + port + "/test?allowMultiQueries=true");
            dataSource.setUser("root");
            dataSource.setPassword("");

            MariaDbCheapSchema schema = new MariaDbCheapSchema();
            schema.executeMainSchemaDdl(dataSource);
            schema.executeForeignKeysDdl(dataSource);  // Enable foreign keys for integration tests
            schema.executeAuditSchemaDdl(dataSource);

            String url = "jdbc:mariadb://localhost:" + port + "/test?allowMultiQueries=true";
            registry.add("spring.datasource.url", () -> url);
        }
    }

    @BeforeAll
    public static void setUpMariaDb()
    {
        // Test DB will be created in subclass-specific @BeforeAll
    }

    @AfterAll
    public static void tearDownMariaDb()
    {
        // Nothing to do - MariaDB4j instance is shared and cleaned up by extension
    }

    @BeforeEach
    @Override
    public void setUp() throws SQLException, ManagedProcessException
    {
        super.setUp();
        // Ensure test DB is initialized
        if (testDb == null)
        {
            testDb = new MariaDbIntegrationTestDb(getDatabaseName(), useForeignKeys());
            testDb.initializeCheapSchema();
        }
    }

    @AfterEach
    public void tearDown() throws SQLException
    {
        cleanupDatabase();
    }

    @Override
    protected void cleanupDatabase() throws SQLException
    {
        if (testDb != null)
        {
            testDb.truncateAllTables();
        }
    }

    /**
     * Get the test database DataSource.
     * Useful for direct database access in tests.
     */
    protected DataSource getDataSource()
    {
        return testDb.dataSource;
    }

    /**
     * Execute a SQL statement directly.
     * Useful for verification queries in tests.
     */
    protected void executeSql(String sql) throws SQLException
    {
        try (Connection conn = testDb.dataSource.getConnection();
             Statement stmt = conn.createStatement())
        {
            stmt.execute(sql);
        }
    }
}
