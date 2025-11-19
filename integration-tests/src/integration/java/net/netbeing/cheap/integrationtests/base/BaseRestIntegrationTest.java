package net.netbeing.cheap.integrationtests.base;

import ch.vorburger.exec.ManagedProcessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.netbeing.cheap.rest.CheapRestApplication;
import net.netbeing.cheap.rest.client.CheapRestClient;
import net.netbeing.cheap.rest.client.CheapRestClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Abstract base class for all REST integration tests.
 * Provides common utilities for:
 * - JSON loading from test resources
 * - Creating test catalogs and aspect definitions
 * - REST client setup
 * - Database cleanup
 */
@SpringBootTest(
    classes = CheapRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public abstract class BaseRestIntegrationTest
{
    @LocalServerPort
    protected int port;

    protected CheapRestClientImpl client;
    protected ObjectMapper objectMapper;

    /**
     * Initialize the REST client before each test.
     * Subclasses can override to add additional setup.
     */
    @BeforeEach
    @SuppressWarnings("java:S1130") // Sonar gets it wrong here; this rule should not apply when subclasses throw the listed exceptions.
    public void setUp() throws SQLException, ManagedProcessException
    {
        client = createRestClient();
        objectMapper = client.getObjectMapper();
    }

    /**
     * Create a REST client instance.
     * Can be overridden by subclasses if needed.
     */
    protected CheapRestClientImpl createRestClient()
    {
        return new CheapRestClientImpl("http://localhost:" + port);
    }

    /**
     * Load JSON content from the test resources directory.
     *
     * @param relativePath Path relative to integration-tests/ resources directory
     * @return JSON content as a string
     * @throws IOException if file cannot be read
     */
    protected String loadJson(String relativePath) throws IOException
    {
        String fullPath = "integration-tests/" + relativePath;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fullPath))
        {
            if (is == null)
            {
                throw new IOException("Resource not found: " + fullPath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Parse JSON string into an object of the specified type.
     *
     * @param json JSON string
     * @param clazz Target class
     * @return Parsed object
     * @throws IOException if JSON parsing fails
     */
    protected <T> T parseJson(String json, Class<T> clazz) throws IOException
    {
        return objectMapper.readValue(json, clazz);
    }

    /**
     * Load and parse JSON from test resources.
     *
     * @param relativePath Path relative to integration-tests/ resources directory
     * @param clazz Target class
     * @return Parsed object
     * @throws IOException if file cannot be read or JSON parsing fails
     */
    protected <T> T loadJsonAs(String relativePath, Class<T> clazz) throws IOException
    {
        String json = loadJson(relativePath);
        return parseJson(json, clazz);
    }

    /**
     * Get the REST client for tests to use.
     *
     * @return REST client
     */
    protected CheapRestClient getClient()
    {
        return client;
    }

    /**
     * Generate a fixed test UUID based on a seed value.
     * Useful for creating reproducible test data.
     *
     * @param seed Seed value (0-99999)
     * @return UUID with the seed embedded
     */
    protected UUID testUuid(int seed)
    {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", seed));
    }

    /**
     * Database-specific cleanup method.
     * Subclasses must implement to clean up their specific database.
     */
    protected abstract void cleanupDatabase() throws SQLException;
}
