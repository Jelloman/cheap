package net.netbeing.cheap.integrationtests.restclient;

import net.netbeing.cheap.db.AspectTableMapping;
import net.netbeing.cheap.db.CheapDao;
import net.netbeing.cheap.db.postgres.PostgresDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.integrationtests.base.PostgresRestIntegrationTest;
import net.netbeing.cheap.json.dto.*;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end REST integration tests for PostgreSQL backend.
 * Tests the complete flow: REST client -> REST API -> Service -> DAO -> PostgreSQL.
 */
class PostgresRestClientIntegrationTest extends PostgresRestIntegrationTest
{
    @Autowired
    private CheapDao cheapDao;

    @Autowired
    private CheapFactory factory;

    private AspectTableMapping addressTableMapping;
    private AspectDef addressAspectDef;

    @BeforeEach
    @Override
    public void setUp()
    {
        super.setUp();

        // Create AspectDef for address custom table
        Map<String, PropertyDef> addressProps = new LinkedHashMap<>();
        addressProps.put("street", factory.createPropertyDef("street", PropertyType.String));
        addressProps.put("city", factory.createPropertyDef("city", PropertyType.String));
        addressProps.put("zip", factory.createPropertyDef("zip", PropertyType.String));
        addressAspectDef = factory.createImmutableAspectDef("address", addressProps);

        // Create AspectTableMapping for address
        Map<String, String> columnMapping = Map.of(
            "street", "street",
            "city", "city",
            "zip", "zip"
        );
        addressTableMapping = new AspectTableMapping(
            addressAspectDef,
            "address",
            columnMapping,
            false,  // hasCatalogId
            true    // hasEntityId
        );

        // Register mapping with DAO and create table
        if (cheapDao instanceof PostgresDao postgresDao)
        {
            postgresDao.addAspectTableMapping(addressTableMapping);
            try
            {
                postgresDao.createTable(addressTableMapping);
            }
            catch (Exception e)
            {
                // Table might already exist from previous test
                // This is OK - it will be truncated by cleanup
            }
        }
    }

    @Test
    void test01_CatalogLifecycle() throws Exception
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef(
            "test-catalog",
            "Test Catalog Description",
            null
        );

        CreateCatalogResponse createResponse = client.createCatalog(
            catalogDef,
            CatalogSpecies.SINK,
            null
        );

        assertNotNull(createResponse);
        assertNotNull(createResponse.catalogId());

        // Retrieve catalog
        CatalogDef retrieved = client.getCatalog(createResponse.catalogId());

        assertNotNull(retrieved);
        assertEquals("test-catalog", retrieved.name());
        assertEquals("Test Catalog Description", retrieved.description());

        // List catalogs
        CatalogListResponse listResponse = client.listCatalogs(0, 10);

        assertNotNull(listResponse);
        assertTrue(listResponse.totalElements() >= 1);
        assertTrue(listResponse.catalogIds().contains(createResponse.catalogId()));
    }

    @Test
    void test02_AspectDefCRUD() throws Exception
    {
        // Create catalog first
        CatalogDef catalogDef = factory.createCatalogDef("test-catalog-2", "For AspectDef testing", null);
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
        assertEquals(addressResponse.aspectDefId(), retrievedAddress.id());
    }

    @Test
    void test03_CustomTableMapping() throws Exception
    {
        // Create catalog and aspect def
        CatalogDef catalogDef = factory.createCatalogDef("test-catalog-3", "For custom table testing", null);
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

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
        assertEquals(2, upsertResponse.upsertedCount());

        // Verify data in custom table via direct DB query
        try (Connection conn = getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT entity_id, street, city, zip FROM address ORDER BY street"))
        {
            assertTrue(rs.next());
            assertEquals(entityId1, rs.getObject("entity_id", UUID.class));
            assertEquals("123 Main St", rs.getString("street"));
            assertEquals("Springfield", rs.getString("city"));
            assertEquals("12345", rs.getString("zip"));

            assertTrue(rs.next());
            assertEquals(entityId2, rs.getObject("entity_id", UUID.class));
            assertEquals("456 Oak Ave", rs.getString("street"));
            assertEquals("Shelbyville", rs.getString("city"));
            assertEquals("67890", rs.getString("zip"));

            assertFalse(rs.next());
        }
    }

    @Test
    void test04_AspectUpsert() throws Exception
    {
        // Create catalog and aspect def
        CatalogDef catalogDef = factory.createCatalogDef("test-catalog-4", "For aspect upsert testing", null);
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        Map<String, PropertyDef> productProps = new LinkedHashMap<>();
        productProps.put("sku", factory.createPropertyDef("sku", PropertyType.String));
        productProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        productProps.put("price", factory.createPropertyDef("price", PropertyType.Real));
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
        assertEquals(2, upsertResponse.upsertedCount());

        // Query aspects back
        Set<UUID> entityIds = Set.of(productId1, productId2);
        Set<String> aspectDefNames = Set.of("product");

        AspectQueryResponse queryResponse = client.queryAspects(catalogId, entityIds, aspectDefNames);
        assertNotNull(queryResponse);
        assertEquals(2, queryResponse.aspects().size());

        Map<String, Object> product1 = queryResponse.aspects().get(productId1).get("product");
        assertNotNull(product1);
        assertEquals("PROD-001", product1.get("sku"));
        assertEquals("Widget", product1.get("name"));

        Map<String, Object> product2 = queryResponse.aspects().get(productId2).get("product");
        assertNotNull(product2);
        assertEquals("PROD-002", product2.get("sku"));
        assertEquals("Gadget", product2.get("name"));
    }

    @Test
    void test05_EntityListHierarchy() throws Exception
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef("test-catalog-5", "For EntityList testing", null);
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Create catalog with EntityList hierarchy through DAO
        Catalog catalog = cheapDao.loadCatalog(catalogId);
        EntityListHierarchy entityList = factory.createEntityListHierarchy(catalog, "users");

        // Add entities
        for (int i = 0; i < 25; i++)
        {
            UUID entityId = testUuid(3000 + i);
            Entity entity = factory.createEntity(entityId);
            entityList.add(entity);
        }

        cheapDao.saveCatalog(catalog);

        // Retrieve via REST client with pagination
        EntityListResponse page1 = client.getEntityList(catalogId, "users", 0, 10);
        assertNotNull(page1);
        assertEquals(25, page1.totalElements());
        assertEquals(10, page1.entityIds().size());

        EntityListResponse page2 = client.getEntityList(catalogId, "users", 1, 10);
        assertNotNull(page2);
        assertEquals(25, page2.totalElements());
        assertEquals(10, page2.entityIds().size());

        EntityListResponse page3 = client.getEntityList(catalogId, "users", 2, 10);
        assertNotNull(page3);
        assertEquals(25, page3.totalElements());
        assertEquals(5, page3.entityIds().size());

        // Verify no duplicate IDs across pages
        Set<UUID> allIds = new HashSet<>();
        allIds.addAll(page1.entityIds());
        allIds.addAll(page2.entityIds());
        allIds.addAll(page3.entityIds());
        assertEquals(25, allIds.size());
    }

    @Test
    void test06_EntityDirectoryHierarchy() throws Exception
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef("test-catalog-6", "For EntityDirectory testing", null);
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Create catalog with EntityDirectory hierarchy through DAO
        Catalog catalog = cheapDao.loadCatalog(catalogId);
        EntityDirectoryHierarchy directory = factory.createEntityDirectoryHierarchy(catalog, "files");

        // Add entries
        directory.put("README.md", factory.createEntity(testUuid(4001)));
        directory.put("src/main.java", factory.createEntity(testUuid(4002)));
        directory.put("src/util.java", factory.createEntity(testUuid(4003)));

        cheapDao.saveCatalog(catalog);

        // Retrieve via REST client
        EntityDirectoryResponse response = client.getEntityDirectory(catalogId, "files");
        assertNotNull(response);
        assertEquals(3, response.entries().size());

        assertTrue(response.entries().containsKey("README.md"));
        assertTrue(response.entries().containsKey("src/main.java"));
        assertTrue(response.entries().containsKey("src/util.java"));

        assertEquals(testUuid(4001), response.entries().get("README.md"));
        assertEquals(testUuid(4002), response.entries().get("src/main.java"));
        assertEquals(testUuid(4003), response.entries().get("src/util.java"));
    }

    @Test
    void test07_AspectMapHierarchy() throws Exception
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef("test-catalog-7", "For AspectMap testing", null);
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Create aspect def
        Map<String, PropertyDef> metadataProps = new LinkedHashMap<>();
        metadataProps.put("key", factory.createPropertyDef("key", PropertyType.String));
        metadataProps.put("value", factory.createPropertyDef("value", PropertyType.String));
        AspectDef metadataAspectDef = factory.createImmutableAspectDef("metadata", metadataProps);

        client.createAspectDef(catalogId, metadataAspectDef);

        // Create AspectMap hierarchy with aspects through DAO
        Catalog catalog = cheapDao.loadCatalog(catalogId);
        AspectMapHierarchy aspectMap = factory.createAspectMapHierarchy(catalog, metadataAspectDef);

        // Add 30 aspects
        for (int i = 0; i < 30; i++)
        {
            UUID entityId = testUuid(5000 + i);
            Entity entity = factory.createEntity(entityId);
            Aspect aspect = factory.createPropertyMapAspect(entity, metadataAspectDef);
            aspect.put(factory.createProperty(metadataAspectDef.propertyDef("key"), "key-" + i));
            aspect.put(factory.createProperty(metadataAspectDef.propertyDef("value"), "value-" + i));
            aspectMap.put(entity, aspect);
        }

        cheapDao.saveCatalog(catalog);

        // Retrieve via REST client with pagination
        AspectMapResponse page1 = client.getAspectMap(catalogId, "metadata", 0, 10);
        assertNotNull(page1);
        assertEquals(30, page1.totalElements());
        assertEquals(10, page1.aspects().size());

        AspectMapResponse page2 = client.getAspectMap(catalogId, "metadata", 1, 10);
        assertEquals(30, page2.totalElements());
        assertEquals(10, page2.aspects().size());

        AspectMapResponse page3 = client.getAspectMap(catalogId, "metadata", 2, 10);
        assertEquals(30, page3.totalElements());
        assertEquals(10, page3.aspects().size());

        // Verify no duplicate entity IDs across pages
        Set<UUID> allEntityIds = new HashSet<>();
        allEntityIds.addAll(page1.aspects().keySet());
        allEntityIds.addAll(page2.aspects().keySet());
        allEntityIds.addAll(page3.aspects().keySet());
        assertEquals(30, allEntityIds.size());
    }

    @Test
    void test08_ErrorHandling() throws Exception
    {
        // Test 404 - catalog not found
        UUID nonExistentCatalogId = UUID.randomUUID();
        assertThrows(
            Exception.class,
            () -> client.getCatalog(nonExistentCatalogId),
            "Should throw exception for non-existent catalog"
        );

        // Create catalog for other error tests
        CatalogDef catalogDef = factory.createCatalogDef("test-catalog-8", "For error testing", null);
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
