package net.netbeing.cheap.integrationtests.base;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import net.netbeing.cheap.db.postgres.PostgresCheapSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base class for integration tests using embedded PostgreSQL database.
 * Sets up and tears down an embedded PostgreSQL instance for testing.
 */
@ActiveProfiles("postgres-test")
public abstract class PostgresRestIntegrationTest extends BaseRestIntegrationTest
{
    protected static EmbeddedPostgres embeddedPostgres;
    protected static DataSource dataSource;

    @BeforeAll
    public static void setUpPostgres() throws Exception
    {
        // Start embedded PostgreSQL
        embeddedPostgres = EmbeddedPostgres.builder()
            .setPort(5433) // Use non-standard port to avoid conflicts
            .start();

        dataSource = embeddedPostgres.getPostgresDatabase();

        // Initialize schema
        PostgresCheapSchema schema = new PostgresCheapSchema();
        schema.executeMainSchemaDdl(dataSource);
        schema.executeAuditSchemaDdl(dataSource);
    }

    @AfterAll
    public static void tearDownPostgres() throws Exception
    {
        if (embeddedPostgres != null)
        {
            embeddedPostgres.close();
        }
    }

    @BeforeEach
    @Override
    public void setUp() throws SQLException
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
        PostgresCheapSchema schema = new PostgresCheapSchema();
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
}
