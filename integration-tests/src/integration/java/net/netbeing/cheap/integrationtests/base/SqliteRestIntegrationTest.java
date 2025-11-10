package net.netbeing.cheap.integrationtests.base;

import net.netbeing.cheap.db.sqlite.SqliteCheapSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.sqlite.SQLiteDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for integration tests using SQLite database.
 * Sets up and tears down a temporary SQLite database file for testing.
 * WARNING: each subclass will reuse static resources in this class, so
 * they cannot be run in parallel.
 */
@ActiveProfiles("sqlite-test")
public abstract class SqliteRestIntegrationTest extends BaseRestIntegrationTest
{
    protected static Path tempDbPath;
    protected static SQLiteDataSource dataSource;
    private static final AtomicInteger fileNumber = new AtomicInteger(0);

    @BeforeAll
    public static void setUpSqlite() throws IOException, SQLException
    {
        // Create temporary database file
        tempDbPath = Files.createTempFile("cheap-integration-test-" + fileNumber.getAndIncrement(), ".db");

        // Set up data source
        dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDbPath.toAbsolutePath());

        // Initialize schema
        SqliteCheapSchema schema = new SqliteCheapSchema();
        schema.executeMainSchemaDdl(dataSource);
        schema.executeAuditSchemaDdl(dataSource);
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry)
    {
        // Configure Spring to use the test's temporary database
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + tempDbPath.toAbsolutePath());
    }

    @AfterAll
    public static void tearDownSqlite() throws IOException
    {
        // Delete temporary database file
        if (tempDbPath != null && Files.exists(tempDbPath))
        {
            Files.delete(tempDbPath);
        }
    }

    @BeforeEach
    @Override
    public void setUp() throws SQLException
    {
        super.setUp();
        cleanupDatabase();
    }

    @Override
    protected void cleanupDatabase() throws SQLException
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
    protected void executeSql(String sql) throws SQLException
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
