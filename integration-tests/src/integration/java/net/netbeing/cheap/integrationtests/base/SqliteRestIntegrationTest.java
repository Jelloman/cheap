package net.netbeing.cheap.integrationtests.base;

import net.netbeing.cheap.db.sqlite.SqliteCheapSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.sqlite.SQLiteDataSource;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Base class for integration tests using SQLite database.
 * Sets up and tears down a temporary SQLite database file for testing.
 */
@ActiveProfiles("sqlite-test")
public abstract class SqliteRestIntegrationTest extends BaseRestIntegrationTest
{
    protected static Path tempDbPath;
    protected static SQLiteDataSource dataSource;

    @BeforeAll
    public static void setUpSqlite() throws Exception
    {
        // Create temporary database file
        tempDbPath = Files.createTempFile("cheap-integration-test-", ".db");

        // Set up data source
        dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDbPath.toAbsolutePath());

        // Initialize schema
        SqliteCheapSchema schema = new SqliteCheapSchema();
        schema.executeMainSchemaDdl(dataSource);
        schema.executeAuditSchemaDdl(dataSource);
    }

    @AfterAll
    public static void tearDownSqlite() throws Exception
    {
        // Delete temporary database file
        if (tempDbPath != null && Files.exists(tempDbPath))
        {
            try
            {
                Files.delete(tempDbPath);
            }
            catch (IOException e)
            {
                // Log but don't fail - temp files will be cleaned up by OS
                System.err.println("Warning: Failed to delete temporary database file: " + tempDbPath);
            }
        }
    }

    @BeforeEach
    @Override
    public void setUp()
    {
        super.setUp();
        try
        {
            cleanupDatabase();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to clean up database before test", e);
        }
    }

    @Override
    protected void cleanupDatabase() throws Exception
    {
        // Truncate all tables to ensure clean state between tests
        SqliteCheapSchema schema = new SqliteCheapSchema();
        schema.executeTruncateSchemaDdl(dataSource);
    }

    /**
     * Get the test database DataSource.
     * Useful for direct database access in tests.
     */
    protected DataSource getDataSource()
    {
        return dataSource;
    }

    /**
     * Execute a SQL statement directly.
     * Useful for verification queries in tests.
     */
    protected void executeSql(String sql) throws Exception
    {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement())
        {
            stmt.execute(sql);
        }
    }

    /**
     * Get the path to the temporary database file.
     */
    protected Path getDatabasePath()
    {
        return tempDbPath;
    }
}
