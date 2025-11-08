package net.netbeing.cheap.integrationtests.base;

import net.netbeing.cheap.integrationtests.util.MariaDbIntegrationTestDb;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Base class for integration tests using MariaDB database.
 * Uses DatabaseRunnerExtension to start a single MariaDB4j instance,
 * then creates a separate database for each test class.
 */
@ActiveProfiles("mariadb-test")
@ExtendWith(net.netbeing.cheap.integrationtests.util.DatabaseRunnerExtension.class)
public abstract class MariaDbRestIntegrationTest extends BaseRestIntegrationTest
{
    protected static MariaDbIntegrationTestDb testDb;

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

    @BeforeAll
    public static void setUpMariaDb() throws Exception
    {
        // Test DB will be created in subclass-specific @BeforeAll
    }

    @AfterAll
    public static void tearDownMariaDb() throws Exception
    {
        // Nothing to do - MariaDB4j instance is shared and cleaned up by extension
    }

    @BeforeEach
    @Override
    public void setUp()
    {
        super.setUp();
        try
        {
            // Ensure test DB is initialized
            if (testDb == null)
            {
                testDb = new MariaDbIntegrationTestDb(getDatabaseName(), useForeignKeys());
                testDb.initializeCheapSchema();
            }
            cleanupDatabase();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to set up MariaDB test database", e);
        }
    }

    @AfterEach
    public void tearDown()
    {
        // Additional cleanup if needed
    }

    @Override
    protected void cleanupDatabase() throws Exception
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
    protected void executeSql(String sql) throws Exception
    {
        try (Connection conn = testDb.dataSource.getConnection();
             Statement stmt = conn.createStatement())
        {
            stmt.execute(sql);
        }
    }
}
