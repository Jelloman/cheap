package net.netbeing.cheap.integrationtests.base;

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract base class for REST client integration tests with common test methods.
 * Contains @Test methods that verify common functionality across all database backends.
 *
 * Database-specific test classes should extend this to inherit all common tests
 * and add their own database-specific tests.
 *
 * This is separate from BaseClientIntegrationTest to allow CrossDatabaseConsistencyTest
 * to extend BaseClientIntegrationTest without inheriting these @Test methods.
 */
public abstract class AbstractRestClientIntegrationTest extends BaseClientIntegrationTest
{
    @Test
    void catalogLifecycle()
    {
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse createResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        assertNotNull(createResponse);
        assertNotNull(createResponse.catalogId());

        CatalogDef retrieved = client.getCatalogDef(createResponse.catalogId());
        assertNotNull(retrieved);

        CatalogListResponse listResponse = client.listCatalogs(0, 100);
        assertNotNull(listResponse);
        assertTrue(listResponse.totalElements() >= 1);
        assertTrue(listResponse.catalogIds().contains(createResponse.catalogId()));
    }

    @Test
    void aspectDefCRUD()
    {
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        Map<String, PropertyDef> personProps = new LinkedHashMap<>();
        personProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        personProps.put("age", factory.createPropertyDef("age", PropertyType.Integer));
        AspectDef personAspectDef = factory.createImmutableAspectDef("person", personProps);

        CreateAspectDefResponse personResponse = client.createAspectDef(catalogId, personAspectDef);
        assertNotNull(personResponse);
        assertNotNull(personResponse.aspectDefId());

        Map<String, PropertyDef> contactProps = new LinkedHashMap<>();
        contactProps.put("email", factory.createPropertyDef("email", PropertyType.String));
        contactProps.put("phone", factory.createPropertyDef("phone", PropertyType.String));
        AspectDef contactAspectDef = factory.createImmutableAspectDef("contact", contactProps);

        CreateAspectDefResponse contactResponse = client.createAspectDef(catalogId, contactAspectDef);
        assertNotNull(contactResponse);
        assertNotNull(contactResponse.aspectDefId());

        AspectDefListResponse listResponse = client.listAspectDefs(catalogId, 0, 10);
        assertNotNull(listResponse);
        assertEquals(2, listResponse.totalElements());

        AspectDef retrievedPerson = client.getAspectDef(catalogId, personResponse.aspectDefId());
        assertNotNull(retrievedPerson);
        assertEquals("person", retrievedPerson.name());

        AspectDef retrievedContact = client.getAspectDefByName(catalogId, "contact");
        assertNotNull(retrievedContact);
        assertEquals("contact", retrievedContact.name());
        assertEquals(contactResponse.aspectDefId(), retrievedContact.globalId());
    }

    @Test
    void aspectUpsert()
    {
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        Map<String, PropertyDef> productProps = new LinkedHashMap<>();
        productProps.put("sku", factory.createPropertyDef("sku", PropertyType.String));
        productProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        productProps.put("price", factory.createPropertyDef("price", PropertyType.Float));
        AspectDef productAspectDef = factory.createImmutableAspectDef("product", productProps);

        client.createAspectDef(catalogId, productAspectDef);
        client.registerAspectDef(productAspectDef);

        UUID productId1 = testUuid(2001);
        UUID productId2 = testUuid(2002);
        Entity entity1 = factory.createEntity(productId1);
        Entity entity2 = factory.createEntity(productId2);

        Map<UUID, Map<String, Object>> aspects = new LinkedHashMap<>();
        aspects.put(productId1, Map.of("sku", "PROD-001", "name", "Widget", "price", 19.99));
        aspects.put(productId2, Map.of("sku", "PROD-002", "name", "Gadget", "price", 29.99));

        UpsertAspectsResponse upsertResponse = client.upsertAspects(catalogId, "product", aspects);
        assertEquals(2, upsertResponse.successCount());

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
        UUID nonExistentCatalogId = UUID.randomUUID();
        assertThrows(Exception.class, () -> client.getCatalogDef(nonExistentCatalogId));

        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        UUID nonExistentAspectDefId = UUID.randomUUID();
        assertThrows(Exception.class, () -> client.getAspectDef(catalogId, nonExistentAspectDefId));
        assertThrows(Exception.class, () -> client.getAspectDefByName(catalogId, "nonexistent"));
        assertThrows(Exception.class, () -> client.getEntityList(catalogId, "nonexistent-list", 0, 10));
    }

    @Test
    void createHierarchyEntitySet()
    {
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        HierarchyDef hierarchyDef = factory.createHierarchyDef("test-entity-set", HierarchyType.ENTITY_SET);
        CreateHierarchyResponse createResponse = client.createHierarchy(catalogId, hierarchyDef);

        assertNotNull(createResponse);
        assertEquals("test-entity-set", createResponse.hierarchyName());
        assertTrue(createResponse.success());
    }

    @Test
    void createHierarchyEntityList()
    {
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        HierarchyDef hierarchyDef = factory.createHierarchyDef("test-entity-list", HierarchyType.ENTITY_LIST);
        CreateHierarchyResponse createResponse = client.createHierarchy(catalogId, hierarchyDef);

        assertNotNull(createResponse);
        assertEquals("test-entity-list", createResponse.hierarchyName());
        assertTrue(createResponse.success());
    }

    @Test
    void createHierarchyEntityDirectory()
    {
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        HierarchyDef hierarchyDef = factory.createHierarchyDef("test-entity-directory", HierarchyType.ENTITY_DIR);
        CreateHierarchyResponse createResponse = client.createHierarchy(catalogId, hierarchyDef);

        assertNotNull(createResponse);
        assertEquals("test-entity-directory", createResponse.hierarchyName());
        assertTrue(createResponse.success());
    }

    @Test
    void createHierarchyEntityTree()
    {
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        HierarchyDef hierarchyDef = factory.createHierarchyDef("test-entity-tree", HierarchyType.ENTITY_TREE);
        CreateHierarchyResponse createResponse = client.createHierarchy(catalogId, hierarchyDef);

        assertNotNull(createResponse);
        assertEquals("test-entity-tree", createResponse.hierarchyName());
        assertTrue(createResponse.success());
    }

    @Test
    void entityListAddAndRemove()
    {
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        HierarchyDef hierarchyDef = factory.createHierarchyDef("my-list", HierarchyType.ENTITY_LIST);
        client.createHierarchy(catalogId, hierarchyDef);

        UUID entity1 = testUuid(3001);
        UUID entity2 = testUuid(3002);
        UUID entity3 = testUuid(3003);
        List<UUID> idsToAdd = List.of(entity1, entity2, entity3);

        EntityIdsOperationResponse addResponse = client.addEntityIds(catalogId, "my-list", idsToAdd);
        assertNotNull(addResponse);
        assertEquals(3, addResponse.count());

        EntityListResponse listResponse = client.getEntityList(catalogId, "my-list", 0, 10);
        assertNotNull(listResponse);
        assertEquals(3, listResponse.totalElements());
        assertTrue(listResponse.entityIds().contains(entity1));
        assertTrue(listResponse.entityIds().contains(entity2));
        assertTrue(listResponse.entityIds().contains(entity3));

        List<UUID> idsToRemove = List.of(entity2);
        EntityIdsOperationResponse removeResponse = client.removeEntityIds(catalogId, "my-list", idsToRemove);
        assertNotNull(removeResponse);
        assertEquals(1, removeResponse.count());

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
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        HierarchyDef hierarchyDef = factory.createHierarchyDef("my-set", HierarchyType.ENTITY_SET);
        client.createHierarchy(catalogId, hierarchyDef);

        UUID entity1 = testUuid(4001);
        UUID entity2 = testUuid(4002);
        UUID entity3 = testUuid(4003);
        List<UUID> idsToAdd = List.of(entity1, entity2, entity3);

        EntityIdsOperationResponse addResponse = client.addEntityIds(catalogId, "my-set", idsToAdd);
        assertNotNull(addResponse);
        assertEquals(3, addResponse.count());

        EntityListResponse setResponse = client.getEntityList(catalogId, "my-set", 0, 10);
        assertNotNull(setResponse);
        assertEquals(3, setResponse.totalElements());

        List<UUID> idsToRemove = List.of(entity1, entity3);
        EntityIdsOperationResponse removeResponse = client.removeEntityIds(catalogId, "my-set", idsToRemove);
        assertNotNull(removeResponse);
        assertEquals(2, removeResponse.count());

        EntityListResponse setAfterRemove = client.getEntityList(catalogId, "my-set", 0, 10);
        assertNotNull(setAfterRemove);
        assertEquals(1, setAfterRemove.totalElements());
        assertTrue(setAfterRemove.entityIds().contains(entity2));
    }

    @Test
    void entityDirectoryOperations()
    {
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        HierarchyDef hierarchyDef = factory.createHierarchyDef("my-directory", HierarchyType.ENTITY_DIR);
        client.createHierarchy(catalogId, hierarchyDef);

        UUID entity1 = testUuid(5001);
        UUID entity2 = testUuid(5002);
        UUID entity3 = testUuid(5003);
        Map<String, UUID> entriesToAdd = Map.of("file1.txt", entity1, "file2.txt", entity2, "file3.txt", entity3);

        DirectoryOperationResponse addResponse = client.addDirectoryEntries(catalogId, "my-directory", entriesToAdd);
        assertNotNull(addResponse);
        assertEquals(3, addResponse.count());

        EntityDirectoryResponse dirResponse = client.getEntityDirectory(catalogId, "my-directory");
        assertNotNull(dirResponse);
        assertEquals(3, dirResponse.content().size());
        assertEquals(entity1, dirResponse.content().get("file1.txt"));
        assertEquals(entity2, dirResponse.content().get("file2.txt"));
        assertEquals(entity3, dirResponse.content().get("file3.txt"));

        DirectoryOperationResponse removeByNameResponse = client.removeDirectoryEntriesByNames(catalogId, "my-directory", List.of("file2.txt"));
        assertNotNull(removeByNameResponse);
        assertEquals(1, removeByNameResponse.count());

        EntityDirectoryResponse dirAfterNameRemove = client.getEntityDirectory(catalogId, "my-directory");
        assertNotNull(dirAfterNameRemove);
        assertEquals(2, dirAfterNameRemove.content().size());
        assertFalse(dirAfterNameRemove.content().containsKey("file2.txt"));

        DirectoryOperationResponse removeByIdResponse = client.removeDirectoryEntriesByEntityIds(catalogId, "my-directory", List.of(entity3));
        assertNotNull(removeByIdResponse);
        assertEquals(1, removeByIdResponse.count());

        EntityDirectoryResponse dirAfterIdRemove = client.getEntityDirectory(catalogId, "my-directory");
        assertNotNull(dirAfterIdRemove);
        assertEquals(1, dirAfterIdRemove.content().size());
        assertEquals(entity1, dirAfterIdRemove.content().get("file1.txt"));
    }

    @Test
    void entityTreeOperations()
    {
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        HierarchyDef hierarchyDef = factory.createHierarchyDef("my-tree", HierarchyType.ENTITY_TREE);
        CreateHierarchyResponse response = client.createHierarchy(catalogId, hierarchyDef);

        assertTrue(response.success());
        assertEquals("my-tree", response.hierarchyName());

        UUID entity1 = testUuid(6001);
        UUID entity2 = testUuid(6002);
        Map<String, UUID> rootNodes = Map.of("folder1", entity1, "folder2", entity2);

        TreeOperationResponse addRootResponse = client.addTreeNodes(catalogId, "my-tree", null, rootNodes);
        assertNotNull(addRootResponse);
        assertEquals(2, addRootResponse.nodesAffected());

        UUID entity3 = testUuid(6003);
        UUID entity4 = testUuid(6004);
        Map<String, UUID> childNodes = Map.of("file1.txt", entity3, "file2.txt", entity4);

        TreeOperationResponse addChildResponse = client.addTreeNodes(catalogId, "my-tree", "/folder1", childNodes);
        assertNotNull(addChildResponse);
        assertEquals(2, addChildResponse.nodesAffected());

        EntityTreeResponse treeResponse = client.getEntityTree(catalogId, "my-tree");
        assertNotNull(treeResponse);
        assertNotNull(treeResponse.root());

        TreeOperationResponse removeResponse = client.removeTreeNodes(catalogId, "my-tree", List.of("/folder1"));
        assertNotNull(removeResponse);
        assertTrue(removeResponse.nodesAffected() >= 1);

        EntityTreeResponse treeAfterRemove = client.getEntityTree(catalogId, "my-tree");
        assertNotNull(treeAfterRemove);
    }
}
