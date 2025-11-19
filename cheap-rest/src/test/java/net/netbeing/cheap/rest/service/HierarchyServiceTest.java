/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.rest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.netbeing.cheap.json.jackson.deserialize.CheapJacksonDeserializer;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.rest.TestStartEndLogger;
import net.netbeing.cheap.rest.exception.ResourceNotFoundException;
import net.netbeing.cheap.rest.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HierarchyService.
 */
@ExtendWith(TestStartEndLogger.class)
class HierarchyServiceTest extends BaseServiceTest
{
    private UUID catalogId;

    @BeforeEach
    void setupHierarchyTest()
    {
        // Create a catalog with various hierarchy types
        HierarchyDef listHierarchy = factory.createHierarchyDef("myList", HierarchyType.ENTITY_LIST);
        HierarchyDef setHierarchy = factory.createHierarchyDef("mySet", HierarchyType.ENTITY_SET);
        HierarchyDef dirHierarchy = factory.createHierarchyDef("myDir", HierarchyType.ENTITY_DIR);

        List<HierarchyDef> hierarchies = Arrays.asList(listHierarchy, setHierarchy, dirHierarchy);

        CatalogDef catalogDef = factory.createCatalogDef(hierarchies, Collections.emptyList());

        catalogId = catalogService.createCatalog(catalogDef, CatalogSpecies.SINK, null, java.net.URI.create("http://example.com/api/catalog"));
    }

    @Test
    void testGetHierarchy()
    {
        Hierarchy hierarchy = hierarchyService.getHierarchy(catalogId, "myList");
        assertNotNull(hierarchy);
        assertEquals("myList", hierarchy.name());
        assertEquals(HierarchyType.ENTITY_LIST, hierarchy.type());
    }

    @Test
    void testGetHierarchyNotFound()
    {
        assertThrows(ResourceNotFoundException.class, () -> hierarchyService.getHierarchy(catalogId, "nonexistent"));
    }

    @Test
    void testGetHierarchyCatalogNotFound()
    {
        UUID nonExistentCatalogId = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () -> hierarchyService.getHierarchy(nonExistentCatalogId, "myList"));
    }

    @Test
    void testGetEntityListContents() throws Exception
    {
        // Load catalog and add entities to list
        Catalog catalog = catalogService.getCatalog(catalogId);
        EntityListHierarchy listHierarchy = (EntityListHierarchy) catalog.hierarchy("myList");

        // Add some entities
        Entity entity1 = factory.createEntity();
        Entity entity2 = factory.createEntity();
        Entity entity3 = factory.createEntity();

        listHierarchy.add(entity1);
        listHierarchy.add(entity2);
        listHierarchy.add(entity3);

        dao.saveCatalog(catalog);

        // Get contents
        List<UUID> contents = hierarchyService.getEntityListContents(listHierarchy, 0, 10);
        assertNotNull(contents);
        assertEquals(3, contents.size());
    }

    @Test
    void testGetEntityListContentsPagination() throws Exception
    {
        // Load catalog and add entities to list
        Catalog catalog = catalogService.getCatalog(catalogId);
        EntityListHierarchy listHierarchy = (EntityListHierarchy) catalog.hierarchy("myList");

        // Add 5 entities
        for (int i = 0; i < 5; i++) {
            Entity entity = factory.createEntity();
            listHierarchy.add(entity);
        }

        dao.saveCatalog(catalog);

        // Test first page
        List<UUID> page1 = hierarchyService.getEntityListContents(listHierarchy, 0, 2);
        assertEquals(2, page1.size());

        // Test second page
        List<UUID> page2 = hierarchyService.getEntityListContents(listHierarchy, 1, 2);
        assertEquals(2, page2.size());

        // Test third page
        List<UUID> page3 = hierarchyService.getEntityListContents(listHierarchy, 2, 2);
        assertEquals(1, page3.size());
    }

    @Test
    void testGetEntityDirectoryContents() throws Exception
    {
        // Load catalog and add entities to directory
        Catalog catalog = catalogService.getCatalog(catalogId);
        EntityDirectoryHierarchy dirHierarchy = (EntityDirectoryHierarchy) catalog.hierarchy("myDir");

        // Add some entities
        Entity entity1 = factory.createEntity();
        Entity entity2 = factory.createEntity();

        dirHierarchy.put("key1", entity1);
        dirHierarchy.put("key2", entity2);

        dao.saveCatalog(catalog);

        // Get contents
        Map<String, UUID> contents = hierarchyService.getEntityDirectoryContents(dirHierarchy, 0, 10);
        assertNotNull(contents);
        assertEquals(2, contents.size());
        assertTrue(contents.containsKey("key1"));
        assertTrue(contents.containsKey("key2"));
    }

    @Test
    void testCountHierarchyItems()
    {
        // Load catalog
        Catalog catalog = catalogService.getCatalog(catalogId);

        // Test EntityList
        EntityListHierarchy listHierarchy = (EntityListHierarchy) catalog.hierarchy("myList");
        assertEquals(0, hierarchyService.countHierarchyItems(listHierarchy));

        Entity entity1 = factory.createEntity();
        Entity entity2 = factory.createEntity();
        listHierarchy.add(entity1);
        listHierarchy.add(entity2);

        assertEquals(2, hierarchyService.countHierarchyItems(listHierarchy));

        // Test EntitySet
        EntitySetHierarchy setHierarchy = (EntitySetHierarchy) catalog.hierarchy("mySet");
        assertEquals(0, hierarchyService.countHierarchyItems(setHierarchy));

        setHierarchy.add(entity1);
        setHierarchy.add(entity2);

        assertEquals(2, hierarchyService.countHierarchyItems(setHierarchy));

        // Test EntityDirectory
        EntityDirectoryHierarchy dirHierarchy = (EntityDirectoryHierarchy) catalog.hierarchy("myDir");
        assertEquals(0, hierarchyService.countHierarchyItems(dirHierarchy));

        dirHierarchy.put("key1", entity1);
        dirHierarchy.put("key2", entity2);

        assertEquals(2, hierarchyService.countHierarchyItems(dirHierarchy));
    }

    @Test
    void testCreateHierarchy() throws IOException
    {
        // Load the JSON test resource
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(CheapJacksonDeserializer.createCheapModule(factory));

        InputStream jsonStream = getClass().getResourceAsStream(
            "/http-tests/hierarchy/create-hierarchy.json"
        );
        assertNotNull(jsonStream, "Test resource file not found");

        JsonNode jsonNode = mapper.readTree(jsonStream);
        HierarchyDef hierarchyDef = mapper.treeToValue(
            jsonNode.get("hierarchyDef"),
            HierarchyDef.class
        );

        // Create the hierarchy
        String hierarchyName = hierarchyService.createHierarchy(catalogId, hierarchyDef);

        // Verify the hierarchy was created
        assertEquals("projects", hierarchyName);

        // Verify we can retrieve the hierarchy
        Hierarchy hierarchy = hierarchyService.getHierarchy(catalogId, "projects");
        assertNotNull(hierarchy);
        assertEquals("projects", hierarchy.name());
        assertEquals(HierarchyType.ENTITY_LIST, hierarchy.type());
    }

    @Test
    void testCreateHierarchySet() throws IOException
    {
        // Load the JSON test resource for Entity Set
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(CheapJacksonDeserializer.createCheapModule(factory));

        InputStream jsonStream = getClass().getResourceAsStream(
            "/http-tests/hierarchy/create-hierarchy-set.json"
        );
        assertNotNull(jsonStream, "Test resource file not found");

        JsonNode jsonNode = mapper.readTree(jsonStream);
        HierarchyDef hierarchyDef = mapper.treeToValue(
            jsonNode.get("hierarchyDef"),
            HierarchyDef.class
        );

        // Create the hierarchy
        String hierarchyName = hierarchyService.createHierarchy(catalogId, hierarchyDef);

        // Verify the hierarchy was created
        assertEquals("users", hierarchyName);

        // Verify we can retrieve the hierarchy
        Hierarchy hierarchy = hierarchyService.getHierarchy(catalogId, "users");
        assertNotNull(hierarchy);
        assertEquals("users", hierarchy.name());
        assertEquals(HierarchyType.ENTITY_SET, hierarchy.type());
        assertInstanceOf(EntitySetHierarchy.class, hierarchy);
    }

    @Test
    void testCreateHierarchyTree() throws IOException
    {
        // Load the JSON test resource for Entity Tree
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(CheapJacksonDeserializer.createCheapModule(factory));

        InputStream jsonStream = getClass().getResourceAsStream(
            "/http-tests/hierarchy/create-hierarchy-tree.json"
        );
        assertNotNull(jsonStream, "Test resource file not found");

        JsonNode jsonNode = mapper.readTree(jsonStream);
        HierarchyDef hierarchyDef = mapper.treeToValue(
            jsonNode.get("hierarchyDef"),
            HierarchyDef.class
        );

        // Create the hierarchy
        String hierarchyName = hierarchyService.createHierarchy(catalogId, hierarchyDef);

        // Verify the hierarchy was created
        assertEquals("categories", hierarchyName);

        // Verify we can retrieve the hierarchy
        Hierarchy hierarchy = hierarchyService.getHierarchy(catalogId, "categories");
        assertNotNull(hierarchy);
        assertEquals("categories", hierarchy.name());
        assertEquals(HierarchyType.ENTITY_TREE, hierarchy.type());
        assertInstanceOf(EntityTreeHierarchy.class, hierarchy);
    }

    @Test
    void testCreateHierarchyDirectory() throws IOException
    {
        // Load the JSON test resource for Entity Directory
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(CheapJacksonDeserializer.createCheapModule(factory));

        InputStream jsonStream = getClass().getResourceAsStream(
            "/http-tests/hierarchy/create-hierarchy-directory.json"
        );
        assertNotNull(jsonStream, "Test resource file not found");

        JsonNode jsonNode = mapper.readTree(jsonStream);
        HierarchyDef hierarchyDef = mapper.treeToValue(
            jsonNode.get("hierarchyDef"),
            HierarchyDef.class
        );

        // Create the hierarchy
        String hierarchyName = hierarchyService.createHierarchy(catalogId, hierarchyDef);

        // Verify the hierarchy was created
        assertEquals("documents", hierarchyName);

        // Verify we can retrieve the hierarchy
        Hierarchy hierarchy = hierarchyService.getHierarchy(catalogId, "documents");
        assertNotNull(hierarchy);
        assertEquals("documents", hierarchy.name());
        assertEquals(HierarchyType.ENTITY_DIR, hierarchy.type());
        assertInstanceOf(EntityDirectoryHierarchy.class, hierarchy);
    }

    // ========================================
    // Entity List/Set Mutation Operation Tests
    // ========================================

    @Test
    void testAddEntityIdsToEntityList()
    {
        // Create entity IDs to add
        UUID entity1Id = UUID.randomUUID();
        UUID entity2Id = UUID.randomUUID();
        UUID entity3Id = UUID.randomUUID();
        List<UUID> entityIds = Arrays.asList(entity1Id, entity2Id, entity3Id);

        // Add entities to list
        int count = hierarchyService.addEntityIds(catalogId, "myList", entityIds);

        // Verify count
        assertEquals(3, count);

        // Verify entities were added
        Catalog catalog = catalogService.getCatalog(catalogId);
        EntityListHierarchy listHierarchy = (EntityListHierarchy) catalog.hierarchy("myList");
        assertEquals(3, listHierarchy.size());

        // Verify the entity IDs are in the list
        List<UUID> contents = hierarchyService.getEntityListContents(listHierarchy, 0, 10);
        assertTrue(contents.contains(entity1Id));
        assertTrue(contents.contains(entity2Id));
        assertTrue(contents.contains(entity3Id));
    }

    @Test
    void testAddEntityIdsToEntitySet()
    {
        // Create entity IDs to add
        UUID entity1Id = UUID.randomUUID();
        UUID entity2Id = UUID.randomUUID();
        List<UUID> entityIds = Arrays.asList(entity1Id, entity2Id);

        // Add entities to set
        int count = hierarchyService.addEntityIds(catalogId, "mySet", entityIds);

        // Verify count
        assertEquals(2, count);

        // Verify entities were added
        Catalog catalog = catalogService.getCatalog(catalogId);
        EntitySetHierarchy setHierarchy = (EntitySetHierarchy) catalog.hierarchy("mySet");
        assertEquals(2, setHierarchy.size());
    }

    @Test
    void testAddEntityIdsToWrongHierarchyTypeThrows()
    {
        UUID entityId = UUID.randomUUID();
        List<UUID> entityIds = Collections.singletonList(entityId);

        // Try to add to a directory (should fail)
        assertThrows(ValidationException.class, () ->
            hierarchyService.addEntityIds(catalogId, "myDir", entityIds)
        );
    }

    @Test
    void testRemoveEntityIdsFromEntityList()
    {
        // First add some entities
        UUID entity1Id = UUID.randomUUID();
        UUID entity2Id = UUID.randomUUID();
        UUID entity3Id = UUID.randomUUID();
        List<UUID> entityIds = Arrays.asList(entity1Id, entity2Id, entity3Id);
        hierarchyService.addEntityIds(catalogId, "myList", entityIds);

        // Remove one entity
        int removed = hierarchyService.removeEntityIds(catalogId, "myList", Collections.singletonList(entity2Id));

        // Verify count
        assertEquals(1, removed);

        // Verify entity was removed
        Catalog catalog = catalogService.getCatalog(catalogId);
        EntityListHierarchy listHierarchy = (EntityListHierarchy) catalog.hierarchy("myList");
        assertEquals(2, listHierarchy.size());

        // Verify the correct entities remain
        List<UUID> contents = hierarchyService.getEntityListContents(listHierarchy, 0, 10);
        assertTrue(contents.contains(entity1Id));
        assertFalse(contents.contains(entity2Id));
        assertTrue(contents.contains(entity3Id));
    }

    @Test
    void testRemoveEntityIdsFromEntitySet()
    {
        // First add some entities
        UUID entity1Id = UUID.randomUUID();
        UUID entity2Id = UUID.randomUUID();
        List<UUID> entityIds = Arrays.asList(entity1Id, entity2Id);
        hierarchyService.addEntityIds(catalogId, "mySet", entityIds);

        // Remove one entity
        int removed = hierarchyService.removeEntityIds(catalogId, "mySet", Collections.singletonList(entity1Id));

        // Verify count
        assertEquals(1, removed);

        // Verify entity was removed
        Catalog catalog = catalogService.getCatalog(catalogId);
        EntitySetHierarchy setHierarchy = (EntitySetHierarchy) catalog.hierarchy("mySet");
        assertEquals(1, setHierarchy.size());
    }

    @Test
    void testRemoveNonexistentEntityIds()
    {
        // Try to remove entities that don't exist
        UUID nonExistentId = UUID.randomUUID();
        int removed = hierarchyService.removeEntityIds(catalogId, "myList", Collections.singletonList(nonExistentId));

        // Should return 0 (nothing removed)
        assertEquals(0, removed);
    }

    // ========================================
    // Entity Directory Mutation Operation Tests
    // ========================================

    @Test
    void testAddDirectoryEntries()
    {
        // Create entries to add
        UUID entity1Id = UUID.randomUUID();
        UUID entity2Id = UUID.randomUUID();
        Map<String, UUID> entries = new LinkedHashMap<>();
        entries.put("file1.txt", entity1Id);
        entries.put("file2.txt", entity2Id);

        // Add entries to directory
        int count = hierarchyService.addDirectoryEntries(catalogId, "myDir", entries);

        // Verify count
        assertEquals(2, count);

        // Verify entries were added
        Catalog catalog = catalogService.getCatalog(catalogId);
        EntityDirectoryHierarchy dirHierarchy = (EntityDirectoryHierarchy) catalog.hierarchy("myDir");
        assertEquals(2, dirHierarchy.size());

        // Verify the entries are in the directory
        Map<String, UUID> contents = hierarchyService.getEntityDirectoryContents(dirHierarchy, 0, 10);
        assertEquals(entity1Id, contents.get("file1.txt"));
        assertEquals(entity2Id, contents.get("file2.txt"));
    }

    @Test
    void testAddDirectoryEntriesToWrongHierarchyTypeThrows()
    {
        Map<String, UUID> entries = Map.of("key", UUID.randomUUID());

        // Try to add to a list (should fail)
        assertThrows(ValidationException.class, () ->
            hierarchyService.addDirectoryEntries(catalogId, "myList", entries)
        );
    }

    @Test
    void testRemoveDirectoryEntriesByNames()
    {
        // First add some entries
        UUID entity1Id = UUID.randomUUID();
        UUID entity2Id = UUID.randomUUID();
        UUID entity3Id = UUID.randomUUID();
        Map<String, UUID> entries = new LinkedHashMap<>();
        entries.put("file1.txt", entity1Id);
        entries.put("file2.txt", entity2Id);
        entries.put("file3.txt", entity3Id);
        hierarchyService.addDirectoryEntries(catalogId, "myDir", entries);

        // Remove one entry by name
        int removed = hierarchyService.removeDirectoryEntriesByNames(
            catalogId, "myDir", Collections.singletonList("file2.txt")
        );

        // Verify count
        assertEquals(1, removed);

        // Verify entry was removed
        Catalog catalog = catalogService.getCatalog(catalogId);
        EntityDirectoryHierarchy dirHierarchy = (EntityDirectoryHierarchy) catalog.hierarchy("myDir");
        assertEquals(2, dirHierarchy.size());

        // Verify the correct entries remain
        Map<String, UUID> contents = hierarchyService.getEntityDirectoryContents(dirHierarchy, 0, 10);
        assertTrue(contents.containsKey("file1.txt"));
        assertFalse(contents.containsKey("file2.txt"));
        assertTrue(contents.containsKey("file3.txt"));
    }

    @Test
    void testRemoveDirectoryEntriesByIds()
    {
        // First add some entries
        UUID entity1Id = UUID.randomUUID();
        UUID entity2Id = UUID.randomUUID();
        UUID entity3Id = UUID.randomUUID();
        Map<String, UUID> entries = new LinkedHashMap<>();
        entries.put("file1.txt", entity1Id);
        entries.put("file2.txt", entity2Id);
        entries.put("file3.txt", entity3Id);
        hierarchyService.addDirectoryEntries(catalogId, "myDir", entries);

        // Remove entries by entity ID
        int removed = hierarchyService.removeDirectoryEntriesByIds(
            catalogId, "myDir", Arrays.asList(entity1Id, entity3Id)
        );

        // Verify count
        assertEquals(2, removed);

        // Verify entries were removed
        Catalog catalog = catalogService.getCatalog(catalogId);
        EntityDirectoryHierarchy dirHierarchy = (EntityDirectoryHierarchy) catalog.hierarchy("myDir");
        assertEquals(1, dirHierarchy.size());

        // Verify the correct entry remains
        Map<String, UUID> contents = hierarchyService.getEntityDirectoryContents(dirHierarchy, 0, 10);
        assertFalse(contents.containsKey("file1.txt"));
        assertTrue(contents.containsKey("file2.txt"));
        assertFalse(contents.containsKey("file3.txt"));
    }

    // ========================================
    // Entity Tree Mutation Operation Tests
    // ========================================

    @Test
    void testAddTreeNodes() throws Exception
    {
        // First create a tree hierarchy
        HierarchyDef treeHierarchyDef = factory.createHierarchyDef("myTree", HierarchyType.ENTITY_TREE);
        hierarchyService.createHierarchy(catalogId, treeHierarchyDef);

        // Add nodes to the root
        UUID entity1Id = UUID.randomUUID();
        UUID entity2Id = UUID.randomUUID();
        Map<String, UUID> nodes = new LinkedHashMap<>();
        nodes.put("folder1", entity1Id);
        nodes.put("folder2", entity2Id);

        int count = hierarchyService.addTreeNodes(catalogId, "myTree", "/", nodes);

        // Verify count
        assertEquals(2, count);

        // Verify nodes were added
        Catalog catalog = catalogService.getCatalog(catalogId);
        EntityTreeHierarchy treeHierarchy = (EntityTreeHierarchy) catalog.hierarchy("myTree");
        EntityTreeHierarchy.Node root = treeHierarchy.root();
        assertEquals(2, root.size());
        assertTrue(root.containsKey("folder1"));
        assertTrue(root.containsKey("folder2"));
    }

    @Test
    void testAddTreeNodesToNonexistentParentThrows() throws Exception
    {
        // First create a tree hierarchy
        HierarchyDef treeHierarchyDef = factory.createHierarchyDef("myTree", HierarchyType.ENTITY_TREE);
        hierarchyService.createHierarchy(catalogId, treeHierarchyDef);

        // Try to add nodes to a non-existent parent
        Map<String, UUID> nodes = Map.of("child", UUID.randomUUID());

        assertThrows(ResourceNotFoundException.class, () ->
            hierarchyService.addTreeNodes(catalogId, "myTree", "/nonexistent", nodes)
        );
    }

    @Test
    void testAddTreeNodesToWrongHierarchyTypeThrows()
    {
        Map<String, UUID> nodes = Map.of("child", UUID.randomUUID());

        // Try to add to a list (should fail)
        assertThrows(ValidationException.class, () ->
            hierarchyService.addTreeNodes(catalogId, "myList", "/", nodes)
        );
    }

    @Test
    void testRemoveTreeNodesWithCascade() throws Exception
    {
        // First create a tree hierarchy with nodes
        HierarchyDef treeHierarchyDef = factory.createHierarchyDef("myTree", HierarchyType.ENTITY_TREE);
        hierarchyService.createHierarchy(catalogId, treeHierarchyDef);

        // Add parent nodes
        UUID folder1Id = UUID.randomUUID();
        UUID folder2Id = UUID.randomUUID();
        Map<String, UUID> parentNodes = new LinkedHashMap<>();
        parentNodes.put("folder1", folder1Id);
        parentNodes.put("folder2", folder2Id);
        hierarchyService.addTreeNodes(catalogId, "myTree", "/", parentNodes);

        // Add child nodes under folder1
        UUID file1Id = UUID.randomUUID();
        UUID file2Id = UUID.randomUUID();
        Map<String, UUID> childNodes = new LinkedHashMap<>();
        childNodes.put("file1.txt", file1Id);
        childNodes.put("file2.txt", file2Id);
        hierarchyService.addTreeNodes(catalogId, "myTree", "/folder1", childNodes);

        // Remove folder1 (should cascade to children)
        int removed = hierarchyService.removeTreeNodes(catalogId, "myTree", Collections.singletonList("/folder1"));

        // Verify count includes parent and all children (3 nodes total)
        assertEquals(3, removed);

        // Verify folder1 and its children were removed
        Catalog catalog = catalogService.getCatalog(catalogId);
        EntityTreeHierarchy treeHierarchy = (EntityTreeHierarchy) catalog.hierarchy("myTree");
        EntityTreeHierarchy.Node root = treeHierarchy.root();
        assertEquals(1, root.size());
        assertFalse(root.containsKey("folder1"));
        assertTrue(root.containsKey("folder2"));
    }

    @Test
    void testRemoveNonexistentTreeNode() throws Exception
    {
        // First create a tree hierarchy
        HierarchyDef treeHierarchyDef = factory.createHierarchyDef("myTree", HierarchyType.ENTITY_TREE);
        hierarchyService.createHierarchy(catalogId, treeHierarchyDef);

        // Try to remove a non-existent node
        int removed = hierarchyService.removeTreeNodes(
            catalogId, "myTree", Collections.singletonList("/nonexistent")
        );

        // Should return 0 (nothing removed)
        assertEquals(0, removed);
    }
}
