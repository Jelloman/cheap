package net.netbeing.cheap.integrationtests.restclient;

import net.netbeing.cheap.integrationtests.base.AbstractRestClientIntegrationTest;
import net.netbeing.cheap.integrationtests.config.ClientTestConfig;
import net.netbeing.cheap.integrationtests.config.MariaDbServerTestConfig;
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
 * End-to-end REST integration tests for MariaDB backend.
 * Tests the complete flow: REST client -> REST API -> Service -> DAO -> MariaDB.
 * ALL tests interact ONLY through the REST client - NO direct database access.
 *
 * Common tests are inherited from BaseClientIntegrationTest.
 * This class only contains MariaDB-specific tests.
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ResourceLock("port-8083")
@ExtendWith(TestStartEndLogger.class)
@Tag("embedded")
@Tag("mariadb")
class MariaDbRestClientIntegrationTest extends AbstractRestClientIntegrationTest
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

    /**
     * Helper method to create the "inventory" AspectDef used by MariaDB-specific tests.
     * The AspectTableMapping for this is registered on the server at startup.
     */
    private AspectDef createInventoryAspectDef()
    {
        Map<String, PropertyDef> inventoryProps = new LinkedHashMap<>();
        inventoryProps.put("item_code", factory.createPropertyDef("item_code", PropertyType.String));
        inventoryProps.put("warehouse", factory.createPropertyDef("warehouse", PropertyType.String));
        inventoryProps.put("stock_qty", factory.createPropertyDef("stock_qty", PropertyType.Integer));
        return factory.createImmutableAspectDef("inventory", inventoryProps);
    }

    @Test
    void customTableMapping()
    {
        AspectDef inventoryAspectDef = createInventoryAspectDef();
        client.registerAspectDef(inventoryAspectDef);

        // Create catalog
        // Note: The "inventory" AspectDef is already registered by the server for AspectTableMapping
        CatalogDef catalogDef = factory.createCatalogDef(Collections.emptyList(), List.of(inventoryAspectDef));
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Upsert inventory aspects using the pre-registered "inventory" AspectDef
        UUID entityId1 = testUuid(1001);
        UUID entityId2 = testUuid(1002);
        Entity entity1 = factory.createEntity(entityId1);
        Entity entity2 = factory.createEntity(entityId2);

        Map<UUID, Map<String, Object>> aspects = new LinkedHashMap<>();
        aspects.put(entityId1, Map.of(
            "item_code", "ITEM-A123",
            "warehouse", "Main Warehouse",
            "stock_qty", 150
        ));
        aspects.put(entityId2, Map.of(
            "item_code", "ITEM-B456",
            "warehouse", "South Depot",
            "stock_qty", 75
        ));

        UpsertAspectsResponse upsertResponse = client.upsertAspects(catalogId, "inventory", aspects);
        assertNotNull(upsertResponse);
        assertEquals(2, upsertResponse.successCount());

        // Verify data was stored correctly by querying back via REST client
        Set<UUID> entityIds = Set.of(entityId1, entityId2);
        Set<String> aspectDefNames = Set.of("inventory");

        AspectQueryResponse queryResponse = client.queryAspects(catalogId, entityIds, aspectDefNames);
        assertNotNull(queryResponse);
        assertEquals(1, queryResponse.results().size());

        AspectMap aspectMap = queryResponse.results().getFirst();
        assertEquals("inventory", aspectMap.aspectDef().name());

        // Verify first inventory item
        Aspect inventory1 = aspectMap.get(entity1);
        assertNotNull(inventory1);
        assertEquals("ITEM-A123", inventory1.get("item_code").read());
        assertEquals("Main Warehouse", inventory1.get("warehouse").read());
        assertEquals(150L, inventory1.get("stock_qty").read());

        // Verify second inventory item
        Aspect inventory2 = aspectMap.get(entity2);
        assertNotNull(inventory2);
        assertEquals("ITEM-B456", inventory2.get("item_code").read());
        assertEquals("South Depot", inventory2.get("warehouse").read());
        assertEquals(75L, inventory2.get("stock_qty").read());
    }

    @Test
    void foreignKeyConstraints()
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Create aspect def
        Map<String, PropertyDef> props = new LinkedHashMap<>();
        props.put("value", factory.createPropertyDef("value", PropertyType.String));
        AspectDef aspectDef = factory.createImmutableAspectDef("test_aspect", props);

        client.createAspectDef(catalogId, aspectDef);

        // Register AspectDef so Aspects can be deserialized
        client.registerAspectDef(aspectDef);

        // Upsert aspect
        UUID entityId = testUuid(9001);
        Map<UUID, Map<String, Object>> aspects = Map.of(
            entityId, Map.of("value", "test")
        );

        UpsertAspectsResponse upsertResponse = client.upsertAspects(catalogId, "test_aspect", aspects);
        assertEquals(1, upsertResponse.successCount());

        // Query back to verify FK relationships are working
        Set<UUID> entityIds = Set.of(entityId);
        Set<String> aspectDefNames = Set.of("test_aspect");

        AspectQueryResponse queryResponse = client.queryAspects(catalogId, entityIds, aspectDefNames);
        assertNotNull(queryResponse);
        assertEquals(1, queryResponse.results().size());
    }
}
