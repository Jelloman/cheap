package net.netbeing.cheap.integrationtests.restclient;

import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.integrationtests.base.SqliteClientIntegrationTest;
import net.netbeing.cheap.json.dto.AspectDefListResponse;
import net.netbeing.cheap.json.dto.AspectQueryResponse;
import net.netbeing.cheap.json.dto.CatalogListResponse;
import net.netbeing.cheap.json.dto.CreateAspectDefResponse;
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
import org.junit.jupiter.api.Test;

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
 */
@SuppressWarnings("unused")
class SqliteRestClientIntegrationTest extends SqliteClientIntegrationTest
{
    private final CheapFactory factory = new CheapFactory();

    /**
     * Helper method to create the "order_item" AspectDef used by multiple tests.
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
    void catalogLifecycle()
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef();

        CreateCatalogResponse createResponse = client.createCatalog(
            catalogDef,
            CatalogSpecies.SINK,
            null
        );

        assertNotNull(createResponse);
        assertNotNull(createResponse.catalogId());

        // Retrieve catalog
        CatalogDef retrieved = client.getCatalogDef(createResponse.catalogId());

        assertNotNull(retrieved);

        // List catalogs
        CatalogListResponse listResponse = client.listCatalogs(0, 10);

        assertNotNull(listResponse);
        assertTrue(listResponse.totalElements() >= 1);
        assertTrue(listResponse.content().contains(createResponse.catalogId()));
    }

    @Test
    void aspectDefCRUD()
    {
        // Create catalog first
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Create multiple aspect defs
        Map<String, PropertyDef> personProps = new LinkedHashMap<>();
        personProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        personProps.put("age", factory.createPropertyDef("age", PropertyType.Integer));
        AspectDef personAspectDef = factory.createImmutableAspectDef("person", personProps);

        CreateAspectDefResponse personResponse = client.createAspectDef(catalogId, personAspectDef);
        assertNotNull(personResponse);
        assertNotNull(personResponse.aspectDefId());

        // Create contact aspect def
        Map<String, PropertyDef> contactProps = new LinkedHashMap<>();
        contactProps.put("email", factory.createPropertyDef("email", PropertyType.String));
        contactProps.put("phone", factory.createPropertyDef("phone", PropertyType.String));
        AspectDef contactAspectDef = factory.createImmutableAspectDef("contact", contactProps);

        CreateAspectDefResponse contactResponse = client.createAspectDef(catalogId, contactAspectDef);
        assertNotNull(contactResponse);
        assertNotNull(contactResponse.aspectDefId());

        // List aspect defs
        AspectDefListResponse listResponse = client.listAspectDefs(catalogId, 0, 10);
        assertNotNull(listResponse);
        assertEquals(2, listResponse.totalElements());

        // Get aspect def by ID
        AspectDef retrievedPerson = client.getAspectDef(catalogId, personResponse.aspectDefId());
        assertNotNull(retrievedPerson);
        assertEquals("person", retrievedPerson.name());

        // Get aspect def by name
        AspectDef retrievedContact = client.getAspectDefByName(catalogId, "contact");
        assertNotNull(retrievedContact);
        assertEquals("contact", retrievedContact.name());
        assertEquals(contactResponse.aspectDefId(), retrievedContact.globalId());
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

    @Test
    void aspectUpsert()
    {
        // Create catalog and aspect def
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        Map<String, PropertyDef> productProps = new LinkedHashMap<>();
        productProps.put("sku", factory.createPropertyDef("sku", PropertyType.String));
        productProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        productProps.put("price", factory.createPropertyDef("price", PropertyType.Float));
        AspectDef productAspectDef = factory.createImmutableAspectDef("product", productProps);

        client.createAspectDef(catalogId, productAspectDef);

        // Register AspectDef so Aspects can be deserialized
        client.registerAspectDef(productAspectDef);

        // Upsert aspects
        UUID productId1 = testUuid(2001);
        UUID productId2 = testUuid(2002);
        Entity entity1 = factory.createEntity(productId1);
        Entity entity2 = factory.createEntity(productId2);

        Map<UUID, Map<String, Object>> aspects = new LinkedHashMap<>();
        aspects.put(productId1, Map.of(
            "sku", "PROD-001",
            "name", "Widget",
            "price", 19.99
        ));
        aspects.put(productId2, Map.of(
            "sku", "PROD-002",
            "name", "Gadget",
            "price", 29.99
        ));

        UpsertAspectsResponse upsertResponse = client.upsertAspects(catalogId, "product", aspects);
        assertEquals(2, upsertResponse.successCount());

        // Query aspects back
        Set<UUID> entityIds = Set.of(productId1, productId2);
        Set<String> aspectDefNames = Set.of("product");

        AspectQueryResponse queryResponse = client.queryAspects(catalogId, entityIds, aspectDefNames);
        assertNotNull(queryResponse);
        assertEquals(1, queryResponse.results().size());

        AspectMap aspectMap = queryResponse.results().getFirst();
        assertEquals("product", aspectMap.aspectDef().name());

        Aspect product1 = aspectMap.get(entity1);
        Aspect product2 = aspectMap.get(entity2);
        assertNotNull(product1);
        assertEquals("PROD-001", product1.get("sku").read());
        assertEquals("Widget", product1.get("name").read());

        assertNotNull(product2);
        assertEquals("PROD-002", product2.get("sku").read());
        assertEquals("Gadget", product2.get("name").read());
    }

    // TODO: Re-enable once REST API endpoints for hierarchy creation are implemented
    // @Test
    // @Disabled("Requires REST API endpoints to create and populate EntityList hierarchies")
    // void entityListHierarchy()
    // {
    //     // This test needs:
    //     // 1. POST /catalogs/{id}/hierarchies/entity-list/{name} - create EntityList hierarchy
    //     // 2. POST /catalogs/{id}/hierarchies/entity-list/{name}/entities - add entities
    //     // OR: Pre-populated test data on server startup
    // }

    // TODO: Re-enable once REST API endpoints for hierarchy creation are implemented
    // @Test
    // @Disabled("Requires REST API endpoints to create and populate EntityDirectory hierarchies")
    // void entityDirectoryHierarchy()
    // {
    //     // This test needs:
    //     // 1. POST /catalogs/{id}/hierarchies/entity-directory/{name} - create EntityDirectory hierarchy
    //     // 2. POST /catalogs/{id}/hierarchies/entity-directory/{name}/entries - add directory entries
    //     // OR: Pre-populated test data on server startup
    // }

    // TODO: Re-enable once REST API endpoints for hierarchy creation are implemented
    // @Test
    // @Disabled("Requires REST API endpoints to create and populate AspectMap hierarchies")
    // void aspectMapHierarchy()
    // {
    //     // This test needs:
    //     // 1. POST /catalogs/{id}/hierarchies/aspect-map/{aspectDefName} - create AspectMap hierarchy
    //     // 2. PUT /catalogs/{id}/hierarchies/aspect-map/{aspectDefName}/aspects - upsert aspects into map
    //     // OR: Pre-populated test data on server startup
    // }

    @Test
    void errorHandling()
    {
        // Test 404 - catalog not found
        UUID nonExistentCatalogId = UUID.randomUUID();
        assertThrows(
            Exception.class,
            () -> client.getCatalogDef(nonExistentCatalogId),
            "Should throw exception for non-existent catalog"
        );

        // Create catalog for other error tests
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Test 404 - aspect def not found
        UUID nonExistentAspectDefId = UUID.randomUUID();
        assertThrows(
            Exception.class,
            () -> client.getAspectDef(catalogId, nonExistentAspectDefId),
            "Should throw exception for non-existent aspect def"
        );

        // Test 404 - aspect def by name not found
        assertThrows(
            Exception.class,
            () -> client.getAspectDefByName(catalogId, "nonexistent"),
            "Should throw exception for non-existent aspect def name"
        );

        // Test 404 - hierarchy not found
        assertThrows(
            Exception.class,
            () -> client.getEntityList(catalogId, "nonexistent-list", 0, 10),
            "Should throw exception for non-existent hierarchy"
        );
    }
}
