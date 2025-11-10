package net.netbeing.cheap.integrationtests.restclient;

import net.netbeing.cheap.db.AspectTableMapping;
import net.netbeing.cheap.db.CheapDao;
import net.netbeing.cheap.db.sqlite.SqliteDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.integrationtests.base.SqliteRestIntegrationTest;
import net.netbeing.cheap.json.dto.*;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end REST integration tests for SQLite backend.
 * Tests the complete flow: REST client -> REST API -> Service -> DAO -> SQLite.
 */
class SqliteRestClientIntegrationTest extends SqliteRestIntegrationTest
{
    private final CheapDao cheapDao;
    private final CheapFactory factory;
    private final AspectDef orderItemAspectDef;

    @Autowired
    public SqliteRestClientIntegrationTest(CheapDao cheapDao, CheapFactory factory)
    {
        this.cheapDao = cheapDao;
        this.factory = factory;

        // Create AspectDef for order_item custom table
        Map<String, PropertyDef> orderItemProps = new LinkedHashMap<>();
        orderItemProps.put("product_name", factory.createPropertyDef("product_name", PropertyType.String));
        orderItemProps.put("quantity", factory.createPropertyDef("quantity", PropertyType.Integer));
        orderItemProps.put("price", factory.createPropertyDef("price", PropertyType.Float));
        orderItemAspectDef = factory.createImmutableAspectDef("order_item", orderItemProps);
    }


    @BeforeEach
    @Override
    public void setUp() throws SQLException
    {
        AspectTableMapping orderItemTableMapping;
        super.setUp();

        // Create AspectTableMapping for order_item
        Map<String, String> columnMapping = Map.of(
            "product_name", "product_name",
            "quantity", "quantity",
            "price", "price"
        );
        orderItemTableMapping = new AspectTableMapping(
            orderItemAspectDef,
            "order_item",
            columnMapping,
            false,  // hasCatalogId
            true    // hasEntityId
        );

        // Register mapping with DAO and create table
        if (cheapDao instanceof SqliteDao sqliteDao)
        {
            sqliteDao.addAspectTableMapping(orderItemTableMapping);
            sqliteDao.createTable(orderItemTableMapping);
        }
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

        // Create order_item aspect def (for custom table)
        CreateAspectDefResponse orderItemResponse = client.createAspectDef(catalogId, orderItemAspectDef);
        assertNotNull(orderItemResponse);
        assertNotNull(orderItemResponse.aspectDefId());

        // List aspect defs
        AspectDefListResponse listResponse = client.listAspectDefs(catalogId, 0, 10);
        assertNotNull(listResponse);
        assertEquals(2, listResponse.totalElements());

        // Get aspect def by ID
        AspectDef retrievedPerson = client.getAspectDef(catalogId, personResponse.aspectDefId());
        assertNotNull(retrievedPerson);
        assertEquals("person", retrievedPerson.name());

        // Get aspect def by name
        AspectDef retrievedOrderItem = client.getAspectDefByName(catalogId, "order_item");
        assertNotNull(retrievedOrderItem);
        assertEquals("order_item", retrievedOrderItem.name());
        assertEquals(orderItemResponse.aspectDefId(), retrievedOrderItem.globalId());
    }

    @Test
    void customTableMapping() throws SQLException
    {
        // Create catalog and aspect def
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        client.createAspectDef(catalogId, orderItemAspectDef);

        // Upsert order_item aspects
        UUID entityId1 = testUuid(1001);
        UUID entityId2 = testUuid(1002);

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

        // Verify data in custom table via direct DB query
        try (Connection conn = getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT entity_id, product_name, quantity, price FROM order_item ORDER BY product_name"))
        {
            assertTrue(rs.next());
            assertEquals(entityId2.toString(), rs.getString("entity_id"));
            assertEquals("Gadget Pro", rs.getString("product_name"));
            assertEquals(3, rs.getInt("quantity"));
            assertEquals(89.99, rs.getDouble("price"), 0.001);

            assertTrue(rs.next());
            assertEquals(entityId1.toString(), rs.getString("entity_id"));
            assertEquals("Widget Deluxe", rs.getString("product_name"));
            assertEquals(5, rs.getInt("quantity"));
            assertEquals(49.99, rs.getDouble("price"), 0.001);

            assertFalse(rs.next());
        }
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

    @Test
    void entityListHierarchy() throws SQLException
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef();
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
        assertEquals(10, page1.content().size());

        EntityListResponse page2 = client.getEntityList(catalogId, "users", 1, 10);
        assertNotNull(page2);
        assertEquals(25, page2.totalElements());
        assertEquals(10, page2.content().size());

        EntityListResponse page3 = client.getEntityList(catalogId, "users", 2, 10);
        assertNotNull(page3);
        assertEquals(25, page3.totalElements());
        assertEquals(5, page3.content().size());

        // Verify no duplicate IDs across pages
        Set<UUID> allIds = new HashSet<>();
        allIds.addAll(page1.content());
        allIds.addAll(page2.content());
        allIds.addAll(page3.content());
        assertEquals(25, allIds.size());
    }

    @Test
    void entityDirectoryHierarchy() throws SQLException
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef();
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
        assertEquals(3, response.content().size());

        assertTrue(response.content().containsKey("README.md"));
        assertTrue(response.content().containsKey("src/main.java"));
        assertTrue(response.content().containsKey("src/util.java"));

        assertEquals(testUuid(4001), response.content().get("README.md"));
        assertEquals(testUuid(4002), response.content().get("src/main.java"));
        assertEquals(testUuid(4003), response.content().get("src/util.java"));
    }

    @Test
    void aspectMapHierarchy() throws SQLException
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef();
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
        assertEquals(10, page1.content().size());

        AspectMapResponse page2 = client.getAspectMap(catalogId, "metadata", 1, 10);
        assertEquals(30, page2.totalElements());
        assertEquals(10, page2.content().size());

        AspectMapResponse page3 = client.getAspectMap(catalogId, "metadata", 2, 10);
        assertEquals(30, page3.totalElements());
        assertEquals(10, page3.content().size());

        // Verify no duplicate entity IDs across pages
        Set<UUID> allEntityIds = new HashSet<>();
        allEntityIds.addAll(page1.content().keySet());
        allEntityIds.addAll(page2.content().keySet());
        allEntityIds.addAll(page3.content().keySet());
        assertEquals(30, allEntityIds.size());
    }

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
