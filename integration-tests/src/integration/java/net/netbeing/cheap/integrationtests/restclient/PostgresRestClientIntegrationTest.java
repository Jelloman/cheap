package net.netbeing.cheap.integrationtests.restclient;

import net.netbeing.cheap.integrationtests.base.AbstractRestClientIntegrationTest;
import net.netbeing.cheap.integrationtests.config.ClientTestConfig;
import net.netbeing.cheap.integrationtests.config.PostgresServerTestConfig;
import net.netbeing.cheap.integrationtests.util.TestStartEndLogger;
import net.netbeing.cheap.json.dto.AspectQueryResponse;
import net.netbeing.cheap.json.dto.CreateCatalogResponse;
import net.netbeing.cheap.json.dto.UpsertAspectsResponse;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.AspectMap;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import net.netbeing.cheap.rest.CheapRestApplication;
import net.netbeing.cheap.rest.client.CheapRestClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end REST integration tests for PostgreSQL backend.
 * Tests the complete flow: REST client -> REST API -> Service -> DAO -> PostgreSQL.
 * ALL tests interact ONLY through the REST client - NO direct database access.
 *
 * Common tests are inherited from BaseClientIntegrationTest.
 * This class only contains PostgreSQL-specific tests.
 */
@SpringBootTest(
    classes = CheapRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "server.port=8081",
        "cheap.database.type=postgres"
    }
)
@ContextConfiguration
@Import({PostgresServerTestConfig.class, ClientTestConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ResourceLock("port-8081")
@ExtendWith(TestStartEndLogger.class)
@Tag("embedded")
@Tag("postgres")
class PostgresRestClientIntegrationTest extends AbstractRestClientIntegrationTest
{
    /**
     * Inject the PostgreSQL-specific REST client.
     * This client is configured to connect to the server on port 8081.
     */
    @Autowired
    @Qualifier("postgresClient")
    @Override
    protected void setClient(CheapRestClient client)
    {
        super.setClient(client);
    }

    /**
     * Helper method to create the "address" AspectDef used by PostgreSQL-specific tests.
     * The AspectTableMapping for this is registered on the server at startup.
     */
    private AspectDef createAddressAspectDef()
    {
        Map<String, PropertyDef> addressProps = new LinkedHashMap<>();
        addressProps.put("street", factory.createPropertyDef("street", PropertyType.String));
        addressProps.put("city", factory.createPropertyDef("city", PropertyType.String));
        addressProps.put("zip", factory.createPropertyDef("zip", PropertyType.String));
        return factory.createImmutableAspectDef("address", addressProps);
    }

    @Test
    void customTableMapping()
    {
        AspectDef addressAspectDef =  createAddressAspectDef();
        client.registerAspectDef(addressAspectDef);

        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef(Collections.emptyList(), List.of(addressAspectDef));
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Upsert address aspects using the pre-registered "address" AspectDef
        UUID entityId1 = testUuid(1001);
        UUID entityId2 = testUuid(1002);
        Entity entity1 = factory.createEntity(entityId1);
        Entity entity2 = factory.createEntity(entityId2);

        Map<UUID, Map<String, Object>> aspects = new LinkedHashMap<>();
        aspects.put(entityId1, Map.of(
            "street", "123 Main St",
            "city", "Springfield",
            "zip", "12345"
        ));
        aspects.put(entityId2, Map.of(
            "street", "456 Oak Ave",
            "city", "Shelbyville",
            "zip", "67890"
        ));

        UpsertAspectsResponse upsertResponse = client.upsertAspects(catalogId, "address", aspects);
        assertNotNull(upsertResponse);
        assertEquals(2, upsertResponse.successCount());

        // Verify data was stored correctly by querying back via REST client
        Set<UUID> entityIds = Set.of(entityId1, entityId2);
        Set<String> aspectDefNames = Set.of("address");

        AspectQueryResponse queryResponse = client.queryAspects(catalogId, entityIds, aspectDefNames);
        assertNotNull(queryResponse);
        assertEquals(1, queryResponse.results().size());

        AspectMap aspectMap = queryResponse.results().getFirst();
        assertEquals("address", aspectMap.aspectDef().name());

        // Verify first address
        Aspect address1 = aspectMap.get(entity1);
        assertNotNull(address1);
        assertEquals("123 Main St", address1.get("street").read());
        assertEquals("Springfield", address1.get("city").read());
        assertEquals("12345", address1.get("zip").read());

        // Verify second address
        Aspect address2 = aspectMap.get(entity2);
        assertNotNull(address2);
        assertEquals("456 Oak Ave", address2.get("street").read());
        assertEquals("Shelbyville", address2.get("city").read());
        assertEquals("67890", address2.get("zip").read());
    }
}
