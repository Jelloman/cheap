package net.netbeing.cheap.integrationtests.restclient;

import net.netbeing.cheap.integrationtests.base.AbstractRestClientIntegrationTest;
import net.netbeing.cheap.integrationtests.config.ClientTestConfig;
import net.netbeing.cheap.integrationtests.config.SqliteServerTestConfig;
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
 * End-to-end REST integration tests for SQLite backend.
 * Tests the complete flow: REST client -> REST API -> Service -> DAO -> SQLite.
 * ALL tests interact ONLY through the REST client - NO direct database access.
 *
 * Common tests are inherited from BaseClientIntegrationTest.
 * This class only contains SQLite-specific tests.
 */
@SpringBootTest(
    classes = CheapRestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "server.port=8082",
        "cheap.database.type=sqlite"
    }
)
@ContextConfiguration
@Import({SqliteServerTestConfig.class, ClientTestConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ResourceLock("port-8082")
@ExtendWith(TestStartEndLogger.class)
class SqliteRestClientIntegrationTest extends AbstractRestClientIntegrationTest
{
    /**
     * Inject the SQLite-specific REST client.
     * This client is configured to connect to the server on port 8082.
     */
    @Autowired
    @Qualifier("sqliteClient")
    @Override
    protected void setClient(CheapRestClient client)
    {
        super.setClient(client);
    }

    /**
     * Helper method to create the "order_item" AspectDef used by SQLite-specific tests.
     * The AspectTableMapping for this is registered on the server at startup.
     */
    private AspectDef createOrderItemAspectDef()
    {
        Map<String, PropertyDef> orderItemProps = new LinkedHashMap<>();
        orderItemProps.put("product_name", factory.createPropertyDef("product_name", PropertyType.String));
        orderItemProps.put("quantity", factory.createPropertyDef("quantity", PropertyType.Integer));
        orderItemProps.put("price", factory.createPropertyDef("price", PropertyType.Float));
        return factory.createImmutableAspectDef("order_item", orderItemProps);
    }

    @Test
    void customTableMapping()
    {
        AspectDef orderItemAspectDef =  createOrderItemAspectDef();
        client.registerAspectDef(orderItemAspectDef);

        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef(Collections.emptyList(), List.of(orderItemAspectDef));
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Upsert order_item aspects using the pre-registered "order_item" AspectDef
        UUID entityId1 = testUuid(1001);
        UUID entityId2 = testUuid(1002);
        Entity entity1 = factory.createEntity(entityId1);
        Entity entity2 = factory.createEntity(entityId2);

        Map<UUID, Map<String, Object>> aspects = new LinkedHashMap<>();
        aspects.put(entityId1, Map.of(
            "product_name", "Widget Deluxe",
            "quantity", 5,
            "price", 49.99
        ));
        aspects.put(entityId2, Map.of(
            "product_name", "Gadget Pro",
            "quantity", 3,
            "price", 89.99
        ));

        UpsertAspectsResponse upsertResponse = client.upsertAspects(catalogId, "order_item", aspects);
        assertNotNull(upsertResponse);
        assertEquals(2, upsertResponse.successCount());

        // Verify data was stored correctly by querying back via REST client
        Set<UUID> entityIds = Set.of(entityId1, entityId2);
        Set<String> aspectDefNames = Set.of("order_item");

        AspectQueryResponse queryResponse = client.queryAspects(catalogId, entityIds, aspectDefNames);
        assertNotNull(queryResponse);
        assertEquals(1, queryResponse.results().size());

        AspectMap aspectMap = queryResponse.results().getFirst();
        assertEquals("order_item", aspectMap.aspectDef().name());

        // Verify first order_item
        Aspect orderItem1 = aspectMap.get(entity1);
        assertNotNull(orderItem1);
        assertEquals("Widget Deluxe", orderItem1.get("product_name").read());
        assertEquals(5L, orderItem1.get("quantity").read());
        assertEquals(49.99, ((Number) orderItem1.get("price").read()).doubleValue(), 0.001);

        // Verify second order_item
        Aspect orderItem2 = aspectMap.get(entity2);
        assertNotNull(orderItem2);
        assertEquals("Gadget Pro", orderItem2.get("product_name").read());
        assertEquals(3L, orderItem2.get("quantity").read());
        assertEquals(89.99, ((Number) orderItem2.get("price").read()).doubleValue(), 0.001);
    }
}
