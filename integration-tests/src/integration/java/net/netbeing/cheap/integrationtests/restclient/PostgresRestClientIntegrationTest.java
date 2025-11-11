package net.netbeing.cheap.integrationtests.restclient;

import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.integrationtests.base.PostgresClientIntegrationTest;
import net.netbeing.cheap.json.dto.*;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end REST integration tests for PostgreSQL backend.
 * Tests the complete flow: REST client -> REST API -> Service -> DAO -> PostgreSQL.
 * ALL tests interact ONLY through the REST client - NO direct database access.
 */
class PostgresRestClientIntegrationTest extends PostgresClientIntegrationTest
{
    private final CheapFactory factory = new CheapFactory();

    /**
     * Helper method to create the "address" AspectDef used by multiple tests.
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

        // Create address aspect def (for custom table)
        AspectDef addressAspectDef = createAddressAspectDef();
        CreateAspectDefResponse addressResponse = client.createAspectDef(catalogId, addressAspectDef);
        assertNotNull(addressResponse);
        assertNotNull(addressResponse.aspectDefId());

        // List aspect defs
        AspectDefListResponse listResponse = client.listAspectDefs(catalogId, 0, 10);
        assertNotNull(listResponse);
        assertEquals(2, listResponse.totalElements());

        // Get aspect def by ID
        AspectDef retrievedPerson = client.getAspectDef(catalogId, personResponse.aspectDefId());
        assertNotNull(retrievedPerson);
        assertEquals("person", retrievedPerson.name());

        // Get aspect def by name
        AspectDef retrievedAddress = client.getAspectDefByName(catalogId, "address");
        assertNotNull(retrievedAddress);
        assertEquals("address", retrievedAddress.name());
        assertEquals(addressResponse.aspectDefId(), retrievedAddress.globalId());
    }

    @Test
    void customTableMapping()
    {
        // Create catalog and aspect def
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        AspectDef addressAspectDef = createAddressAspectDef();
        client.createAspectDef(catalogId, addressAspectDef);

        // Upsert address aspects
        UUID entityId1 = testUuid(1001);
        UUID entityId2 = testUuid(1002);

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
        assertEquals(2, queryResponse.results().size());

        // Verify first address
        Aspect address1 = queryResponse.results().get(entityId1).get("address");
        assertNotNull(address1);
        assertEquals("123 Main St", address1.get("street").read());
        assertEquals("Springfield", address1.get("city").read());
        assertEquals("12345", address1.get("zip").read());

        // Verify second address
        Aspect address2 = queryResponse.results().get(entityId2).get("address");
        assertNotNull(address2);
        assertEquals("456 Oak Ave", address2.get("street").read());
        assertEquals("Shelbyville", address2.get("city").read());
        assertEquals("67890", address2.get("zip").read());
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

        // Upsert aspects
        UUID productId1 = testUuid(2001);
        UUID productId2 = testUuid(2002);

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
        assertEquals(2, queryResponse.results().size());

        Aspect product1 = queryResponse.results().get(productId1).get("product");
        assertNotNull(product1);
        assertEquals("PROD-001", product1.get("sku").read());
        assertEquals("Widget", product1.get("name").read());

        Aspect product2 = queryResponse.results().get(productId2).get("product");
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
