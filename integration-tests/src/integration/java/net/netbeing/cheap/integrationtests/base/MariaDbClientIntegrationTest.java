package net.netbeing.cheap.integrationtests.base;

import net.netbeing.cheap.integrationtests.config.ClientTestConfig;
import net.netbeing.cheap.integrationtests.config.MariaDbServerTestConfig;
import net.netbeing.cheap.rest.CheapRestApplication;
import net.netbeing.cheap.rest.client.CheapRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * Base class for MariaDB REST client integration tests.
 * Configures a complete test environment with:
 * - cheap-rest server running on port 8083 with MariaDB backend
 * - AspectTableMapping for "inventory" table registered on server
 * - CheapRestClient configured to connect to the MariaDB server
 *
 * Tests extending this class interact ONLY through the REST client,
 * with NO direct database access.
 */
@SpringBootTest(
    classes = CheapRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "server.port=8083",
        "cheap.database.type=mariadb"
    }
)
@ContextConfiguration
@Import({MariaDbServerTestConfig.class, ClientTestConfig.class})
public abstract class MariaDbClientIntegrationTest extends BaseClientIntegrationTest
{
    /**
     * Inject the MariaDB-specific REST client.
     * This client is configured to connect to the server on port 8083.
     */
    @Autowired
    @Qualifier("mariadbClient")
    @Override
    protected void setClient(CheapRestClient client)
    {
        super.setClient(client);
    }
}
