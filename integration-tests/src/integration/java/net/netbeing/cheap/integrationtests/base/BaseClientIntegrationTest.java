package net.netbeing.cheap.integrationtests.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.netbeing.cheap.rest.client.CheapRestClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Abstract base class for all client-side integration tests.
 * Provides common utilities for:
 * - JSON loading from test resources
 * - REST client access
 * - Test data generation
 *
 * NO @SpringBootTest annotation - subclasses must configure their own Spring context.
 * NO database access methods - tests interact ONLY through REST client.
 */
public abstract class BaseClientIntegrationTest
{
    protected CheapRestClient client;

    protected ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Set the REST client. This is called by Spring via autowiring in subclasses.
     *
     * @param client REST client to use
     */
    protected void setClient(CheapRestClient client)
    {
        this.client = client;
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
}
