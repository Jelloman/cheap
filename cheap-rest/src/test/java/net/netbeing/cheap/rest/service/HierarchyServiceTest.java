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
import net.netbeing.cheap.rest.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HierarchyService.
 */
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
        assertTrue(hierarchy instanceof EntitySetHierarchy);
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
        assertTrue(hierarchy instanceof EntityTreeHierarchy);
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
        assertTrue(hierarchy instanceof EntityDirectoryHierarchy);
    }
}
