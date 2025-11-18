package net.netbeing.cheap.integrationtests.restclient;

import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.integrationtests.base.SqliteClientIntegrationTest;
import net.netbeing.cheap.json.dto.AspectDefListResponse;
import net.netbeing.cheap.json.dto.AspectQueryResponse;
import net.netbeing.cheap.json.dto.CatalogListResponse;
import net.netbeing.cheap.json.dto.CreateAspectDefResponse;
import net.netbeing.cheap.json.dto.CreateCatalogResponse;
import net.netbeing.cheap.json.dto.CreateHierarchyResponse;
import net.netbeing.cheap.json.dto.DirectoryOperationResponse;
import net.netbeing.cheap.json.dto.EntityDirectoryResponse;
import net.netbeing.cheap.json.dto.EntityIdsOperationResponse;
import net.netbeing.cheap.json.dto.EntityListResponse;
import net.netbeing.cheap.json.dto.EntityTreeResponse;
import net.netbeing.cheap.json.dto.TreeOperationResponse;
import net.netbeing.cheap.json.dto.UpsertAspectsResponse;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.AspectMap;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;
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
        assertTrue(listResponse.catalogIds().contains(createResponse.catalogId()));
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

    @Test
    void createHierarchyEntitySet()
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Create EntitySet hierarchy
        HierarchyDef hierarchyDef = factory.createHierarchyDef("test-entity-set", HierarchyType.ENTITY_SET);
        CreateHierarchyResponse createResponse = client.createHierarchy(catalogId, hierarchyDef);

        assertNotNull(createResponse);
        assertEquals("test-entity-set", createResponse.hierarchyName());
        assertTrue(createResponse.success());
    }

    @Test
    void createHierarchyEntityList()
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Create EntityList hierarchy
        HierarchyDef hierarchyDef = factory.createHierarchyDef("test-entity-list", HierarchyType.ENTITY_LIST);
        CreateHierarchyResponse createResponse = client.createHierarchy(catalogId, hierarchyDef);

        assertNotNull(createResponse);
        assertEquals("test-entity-list", createResponse.hierarchyName());
        assertTrue(createResponse.success());
    }

    @Test
    void createHierarchyEntityDirectory()
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Create EntityDirectory hierarchy
        HierarchyDef hierarchyDef = factory.createHierarchyDef("test-entity-directory", HierarchyType.ENTITY_DIR);
        CreateHierarchyResponse createResponse = client.createHierarchy(catalogId, hierarchyDef);

        assertNotNull(createResponse);
        assertEquals("test-entity-directory", createResponse.hierarchyName());
        assertTrue(createResponse.success());
    }

    @Test
    void createHierarchyEntityTree()
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Create EntityTree hierarchy
        HierarchyDef hierarchyDef = factory.createHierarchyDef("test-entity-tree", HierarchyType.ENTITY_TREE);
        CreateHierarchyResponse createResponse = client.createHierarchy(catalogId, hierarchyDef);

        assertNotNull(createResponse);
        assertEquals("test-entity-tree", createResponse.hierarchyName());
        assertTrue(createResponse.success());
    }

    @Test
    void entityListAddAndRemove()
    {
        // Create catalog and EntityList hierarchy
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        HierarchyDef hierarchyDef = factory.createHierarchyDef("my-list", HierarchyType.ENTITY_LIST);
        client.createHierarchy(catalogId, hierarchyDef);

        // Add entity IDs
        UUID entity1 = testUuid(3001);
        UUID entity2 = testUuid(3002);
        UUID entity3 = testUuid(3003);
        List<UUID> idsToAdd = List.of(entity1, entity2, entity3);

        EntityIdsOperationResponse addResponse = client.addEntityIds(catalogId, "my-list", idsToAdd);
        assertNotNull(addResponse);
        assertEquals(3, addResponse.count());

        // Verify entities were added
        EntityListResponse listResponse = client.getEntityList(catalogId, "my-list", 0, 10);
        assertNotNull(listResponse);
        assertEquals(3, listResponse.totalElements());
        assertTrue(listResponse.entityIds().contains(entity1));
        assertTrue(listResponse.entityIds().contains(entity2));
        assertTrue(listResponse.entityIds().contains(entity3));

        // Remove some entities
        List<UUID> idsToRemove = List.of(entity2);
        EntityIdsOperationResponse removeResponse = client.removeEntityIds(catalogId, "my-list", idsToRemove);
        assertNotNull(removeResponse);
        assertEquals(1, removeResponse.count());

        // Verify entity was removed
        EntityListResponse listAfterRemove = client.getEntityList(catalogId, "my-list", 0, 10);
        assertNotNull(listAfterRemove);
        assertEquals(2, listAfterRemove.totalElements());
        assertTrue(listAfterRemove.entityIds().contains(entity1));
        assertFalse(listAfterRemove.entityIds().contains(entity2));
        assertTrue(listAfterRemove.entityIds().contains(entity3));
    }

    @Test
    void entitySetAddAndRemove()
    {
        // Create catalog and EntitySet hierarchy
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        HierarchyDef hierarchyDef = factory.createHierarchyDef("my-set", HierarchyType.ENTITY_SET);
        client.createHierarchy(catalogId, hierarchyDef);

        // Add entity IDs
        UUID entity1 = testUuid(4001);
        UUID entity2 = testUuid(4002);
        UUID entity3 = testUuid(4003);
        List<UUID> idsToAdd = List.of(entity1, entity2, entity3);

        EntityIdsOperationResponse addResponse = client.addEntityIds(catalogId, "my-set", idsToAdd);
        assertNotNull(addResponse);
        assertEquals(3, addResponse.count());

        // Verify entities were added
        EntityListResponse setResponse = client.getEntityList(catalogId, "my-set", 0, 10);
        assertNotNull(setResponse);
        assertEquals(3, setResponse.totalElements());

        // Remove some entities
        List<UUID> idsToRemove = List.of(entity1, entity3);
        EntityIdsOperationResponse removeResponse = client.removeEntityIds(catalogId, "my-set", idsToRemove);
        assertNotNull(removeResponse);
        assertEquals(2, removeResponse.count());

        // Verify entities were removed
        EntityListResponse setAfterRemove = client.getEntityList(catalogId, "my-set", 0, 10);
        assertNotNull(setAfterRemove);
        assertEquals(1, setAfterRemove.totalElements());
        assertTrue(setAfterRemove.entityIds().contains(entity2));
    }

    @Test
    void entityDirectoryOperations()
    {
        // Create catalog and EntityDirectory hierarchy
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        HierarchyDef hierarchyDef = factory.createHierarchyDef("my-directory", HierarchyType.ENTITY_DIR);
        client.createHierarchy(catalogId, hierarchyDef);

        // Add directory entries
        UUID entity1 = testUuid(5001);
        UUID entity2 = testUuid(5002);
        UUID entity3 = testUuid(5003);
        Map<String, UUID> entriesToAdd = Map.of(
            "file1.txt", entity1,
            "file2.txt", entity2,
            "file3.txt", entity3
        );

        DirectoryOperationResponse addResponse = client.addDirectoryEntries(catalogId, "my-directory", entriesToAdd);
        assertNotNull(addResponse);
        assertEquals(3, addResponse.count());

        // Verify entries were added
        EntityDirectoryResponse dirResponse = client.getEntityDirectory(catalogId, "my-directory");
        assertNotNull(dirResponse);
        assertEquals(3, dirResponse.content().size());
        assertEquals(entity1, dirResponse.content().get("file1.txt"));
        assertEquals(entity2, dirResponse.content().get("file2.txt"));
        assertEquals(entity3, dirResponse.content().get("file3.txt"));

        // Remove entry by name
        DirectoryOperationResponse removeByNameResponse = client.removeDirectoryEntriesByNames(
            catalogId, "my-directory", List.of("file2.txt")
        );
        assertNotNull(removeByNameResponse);
        assertEquals(1, removeByNameResponse.count());

        // Verify entry was removed
        EntityDirectoryResponse dirAfterNameRemove = client.getEntityDirectory(catalogId, "my-directory");
        assertNotNull(dirAfterNameRemove);
        assertEquals(2, dirAfterNameRemove.content().size());
        assertFalse(dirAfterNameRemove.content().containsKey("file2.txt"));

        // Remove entry by entity ID
        DirectoryOperationResponse removeByIdResponse = client.removeDirectoryEntriesByEntityIds(
            catalogId, "my-directory", List.of(entity3)
        );
        assertNotNull(removeByIdResponse);
        assertEquals(1, removeByIdResponse.count());

        // Verify entry was removed
        EntityDirectoryResponse dirAfterIdRemove = client.getEntityDirectory(catalogId, "my-directory");
        assertNotNull(dirAfterIdRemove);
        assertEquals(1, dirAfterIdRemove.content().size());
        assertEquals(entity1, dirAfterIdRemove.content().get("file1.txt"));
    }

    @Test
    void entityTreeOperations()
    {
        // Create catalog and EntityTree hierarchy
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        HierarchyDef hierarchyDef = factory.createHierarchyDef("my-tree", HierarchyType.ENTITY_TREE);
        client.createHierarchy(catalogId, hierarchyDef);

        // Add root-level nodes
        UUID entity1 = testUuid(6001);
        UUID entity2 = testUuid(6002);
        Map<String, UUID> rootNodes = Map.of(
            "folder1", entity1,
            "folder2", entity2
        );

        TreeOperationResponse addRootResponse = client.addTreeNodes(catalogId, "my-tree", null, rootNodes);
        assertNotNull(addRootResponse);
        assertEquals(2, addRootResponse.nodesAffected());

        // Add child nodes under folder1
        UUID entity3 = testUuid(6003);
        UUID entity4 = testUuid(6004);
        Map<String, UUID> childNodes = Map.of(
            "file1.txt", entity3,
            "file2.txt", entity4
        );

        TreeOperationResponse addChildResponse = client.addTreeNodes(catalogId, "my-tree", "/folder1", childNodes);
        assertNotNull(addChildResponse);
        assertEquals(2, addChildResponse.nodesAffected());

        // Verify tree structure
        EntityTreeResponse treeResponse = client.getEntityTree(catalogId, "my-tree");
        assertNotNull(treeResponse);
        assertNotNull(treeResponse.root());

        // Remove a node (this should cascade delete children)
        TreeOperationResponse removeResponse = client.removeTreeNodes(catalogId, "my-tree", List.of("/folder1"));
        assertNotNull(removeResponse);
        assertTrue(removeResponse.nodesAffected() >= 1); // Should remove folder1 and its children

        // Verify node was removed
        EntityTreeResponse treeAfterRemove = client.getEntityTree(catalogId, "my-tree");
        assertNotNull(treeAfterRemove);
        // folder2 should still exist, but folder1 should be gone
    }
}
