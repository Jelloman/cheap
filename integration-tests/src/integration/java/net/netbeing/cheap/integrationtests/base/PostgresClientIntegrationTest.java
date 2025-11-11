package net.netbeing.cheap.integrationtests.base;

import net.netbeing.cheap.integrationtests.config.ClientTestConfig;
import net.netbeing.cheap.integrationtests.config.PostgresServerTestConfig;
import net.netbeing.cheap.rest.CheapRestApplication;
import net.netbeing.cheap.rest.client.CheapRestClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * Base class for PostgreSQL REST client integration tests.
 * Configures a complete test environment with:
 * - cheap-rest server running on port 8081 with PostgreSQL backend
 * - AspectTableMapping for "address" table registered on server
 * - CheapRestClient configured to connect to the PostgreSQL server
 *
 * Tests extending this class interact ONLY through the REST client,
 * with NO direct database access.
 */
@SpringBootTest(
    classes = CheapRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@ContextConfiguration
@Import({PostgresServerTestConfig.class, ClientTestConfig.class})
public abstract class PostgresClientIntegrationTest extends BaseClientIntegrationTest
{
    /**
     * Inject the PostgreSQL-specific REST client.
     * This client is configured to connect to the server on port 8081.
     */
    @Override
    @Qualifier("postgresClient")
    protected CheapRestClient getClient()
    {
        return super.getClient();
    }
}
