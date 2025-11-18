package net.netbeing.cheap.integrationtests.base;

import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.rest.client.CheapRestClient;

import java.util.UUID;

/**
 * Abstract base class for all client-side integration tests.
 * Provides common utilities for REST client access and test data generation.
 *
 * Subclasses must configure their own Spring context with @SpringBootTest.
 * All tests interact ONLY through REST client - NO direct database access.
 */
public abstract class BaseClientIntegrationTest
{
    protected CheapRestClient client;
    protected final CheapFactory factory = new CheapFactory();

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
     * Get the REST client for tests to use.
     *
     * @return REST client
     */
    @SuppressWarnings("unused")
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
