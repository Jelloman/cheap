package net.netbeing.cheap.json;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for CheapJsonUtil using actual implementation classes.
 */
public class CheapJsonRawSerializerTest
{
    @Test
    void testPropertyDefToJson()
    {
        PropertyDef propertyDef = new PropertyDefImpl("testProp", PropertyType.String, null, false, true, false, true, false, false);
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.propertyDefToJson(propertyDef, sb, true, 0);
        
        String expected = """
            {
              "name":"testProp",
              "type":"String",
              "hasDefaultValue":false,
              "isReadable":true,
              "isWritable":false,
              "isNullable":true,
              "isRemovable":false,
              "isMultivalued":false
            }""";
        assertEquals(expected, sb.toString());
    }

    @Test
    void testPropertyDefWithDefaultValueToJson()
    {
        PropertyDef propertyDef = new PropertyDefImpl("testProp", PropertyType.Integer, "42", true, true, true, true, true, false);
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.propertyDefToJson(propertyDef, sb, true, 0);
        
        String expected = """
            {
              "name":"testProp",
              "type":"Integer",
              "hasDefaultValue":true,
              "defaultValue":"42",
              "isReadable":true,
              "isWritable":true,
              "isNullable":true,
              "isRemovable":true,
              "isMultivalued":false
            }""";
        assertEquals(expected, sb.toString());
    }

    @Test
    void testHierarchyDefToJson()
    {
        HierarchyDef hierarchyDef = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_LIST, true);
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.hierarchyDefToJson(hierarchyDef, sb, true, 0);
        
        String expected = """
            {
              "name":"testHierarchy",
              "type":"el",
              "isModifiable":true
            }""";
        assertEquals(expected, sb.toString());
    }

    @Test
    void testAspectToJson()
    {
        Entity entity = new EntityImpl(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        PropertyDef prop1 = new PropertyDefImpl("name", PropertyType.String, null, false, true, true, false, false, false);
        
        Map<String, PropertyDef> propDefMap = ImmutableMap.of("name", prop1);
        AspectDef aspectDef = new ImmutableAspectDefImpl("testAspect", propDefMap);
        
        AspectObjectMapImpl aspect = new AspectObjectMapImpl(entity, aspectDef);
        aspect.unsafeWrite("name", "John Doe");
        
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.aspectToJson(aspect, sb, true, 0);
        
        String expected = """
            {
              "aspectDefName":"testAspect",
              "entityId":"550e8400-e29b-41d4-a716-446655440000",
              "isTransferable":false,
              "name":"John Doe"
            }""";
        assertEquals(expected, sb.toString());
    }

    @Test
    void testEntityListHierarchyToJson()
    {
        UUID uuid1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        
        HierarchyDef def = new HierarchyDefImpl("entityList", HierarchyType.ENTITY_LIST, true);
        EntityListHierarchyImpl hierarchy = new EntityListHierarchyImpl(def);
        hierarchy.add(new EntityImpl(uuid1));
        hierarchy.add(new EntityImpl(uuid2));
        
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.entityListHierarchyToJson(hierarchy, sb, true, 0);
        
        String expected = """
            {
              "def":{"type":"entity_list"},
              "entities":[
                "550e8400-e29b-41d4-a716-446655440000",
                "550e8400-e29b-41d4-a716-446655440001"
              ]
            }""";
        assertEquals(expected, sb.toString());
    }

    @Test
    void testEntitySetHierarchyToJson()
    {
        UUID uuid1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        
        HierarchyDef def = new HierarchyDefImpl("entitySet", HierarchyType.ENTITY_SET, true);
        EntitySetHierarchyImpl hierarchy = new EntitySetHierarchyImpl(def);
        hierarchy.add(new EntityImpl(uuid1));
        hierarchy.add(new EntityImpl(uuid2));
        
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.entitySetHierarchyToJson(hierarchy, sb, true, 0);
        
        // LinkedHashSet preserves insertion order, so we can predict the order
        String expected = """
            {
              "def":{"type":"entity_set"},
              "entities":[
                "550e8400-e29b-41d4-a716-446655440001",
                "550e8400-e29b-41d4-a716-446655440000"
              ]
            }""";
        assertEquals(expected, sb.toString());
    }

    @Test
    void testEntityDirectoryHierarchyToJson()
    {
        UUID uuid1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        
        HierarchyDef def = new HierarchyDefImpl("entityDir", HierarchyType.ENTITY_DIR, true);
        EntityDirectoryHierarchyImpl hierarchy = new EntityDirectoryHierarchyImpl(def);
        hierarchy.put("first", new EntityImpl(uuid1));
        hierarchy.put("second", new EntityImpl(uuid2));
        
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.entityDirectoryHierarchyToJson(hierarchy, sb, true, 0);
        
        String expected = """
            {
              "def":{"type":"entity_dir"},
              "entities":{
                "first":"550e8400-e29b-41d4-a716-446655440000",
                "second":"550e8400-e29b-41d4-a716-446655440001"
              }
            }""";
        assertEquals(expected, sb.toString());
    }

    @Test
    void testTreeNodeToJson()
    {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Entity entity = new EntityImpl(uuid);
        HierarchyDef def = new HierarchyDefImpl("entityTree", HierarchyType.ENTITY_TREE, true);
        EntityTreeHierarchyImpl hierarchy = new EntityTreeHierarchyImpl(def, entity);

        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.treeNodeToJson(hierarchy.root(), sb, true, 0);
        
        String expected = """
            {
              "entityId":"550e8400-e29b-41d4-a716-446655440000"
            }""";
        assertEquals(expected, sb.toString());
    }

    @Test
    void testTreeNodeWithChildrenToJson()
    {
        UUID uuid1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Entity parentEntity = new EntityImpl(uuid1);
        Entity childEntity = new EntityImpl(uuid2);

        HierarchyDef def = new HierarchyDefImpl("entityTree", HierarchyType.ENTITY_TREE, true);
        EntityTreeHierarchyImpl hierarchy = new EntityTreeHierarchyImpl(def, parentEntity);

        EntityTreeHierarchyImpl.NodeImpl childNode = new EntityTreeHierarchyImpl.NodeImpl(childEntity, hierarchy.root());
        hierarchy.root().put("child", childNode);
        
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.treeNodeToJson(hierarchy.root(), sb, true, 0);
        
        String expected = """
            {
              "entityId":"550e8400-e29b-41d4-a716-446655440000",
              "children":{
                "child":{
                  "entityId":"550e8400-e29b-41d4-a716-446655440001"
                }
              }
            }""";
        assertEquals(expected, sb.toString());
    }

    @Test
    void testAspectDefToJson() throws IOException
    {
        PropertyDef prop1 = new PropertyDefImpl("name", PropertyType.String, null, false, true, true, false, false, false);
        PropertyDef prop2 = new PropertyDefImpl("age", PropertyType.Integer, null, false, true, true, true, false, false);
        
        Map<String, PropertyDef> propDefMap = ImmutableMap.of("age", prop2, "name", prop1);
        AspectDef aspectDef = new ImmutableAspectDefImpl("person", propDefMap);
        
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.aspectDefToJson(aspectDef, sb, true, 0);
        
        String expected = loadJsonFromFile("aspectdef-expected.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testCatalogDefToJson() throws IOException
    {
        PropertyDef prop = new PropertyDefImpl("name", PropertyType.String, null, false, true, true, false, false, false);
        Map<String, PropertyDef> propDefMap = ImmutableMap.of("name", prop);
        AspectDef aspectDef = new ImmutableAspectDefImpl("person", propDefMap);
        
        HierarchyDef hierarchyDef = new HierarchyDefImpl("entities", HierarchyType.ENTITY_SET, true);
        
        CatalogDefImpl catalogDef = new CatalogDefImpl(List.of(hierarchyDef), List.of(aspectDef));
        
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.catalogDefToJson(catalogDef, sb, true, 0);
        
        String expected = loadJsonFromFile("catalogdef-expected.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testSimpleCatalogToJson()
    {
        // Create a simple catalog with no custom aspects or hierarchies
        CatalogImpl catalog = new CatalogImpl();
        
        String result = CheapJsonRawSerializer.toJson(catalog, true);
        
        // Just verify it contains expected base structure
        assertTrue(result.contains("\"globalId\":\"" + catalog.globalId() + "\""));
        assertTrue(result.contains("\"species\":\"sink\""));
        assertTrue(result.contains("\"strict\":false"));
        assertTrue(result.contains("\"hierarchies\":"));
        assertTrue(result.contains("\"aspectDefs\":"));
        assertTrue(result.contains("\"aspects\":"));
    }

    @Test
    void testFullCatalogWithAllHierarchyTypes()
    {
        // Create a catalog with all hierarchy types and real data
        CatalogImpl catalog = new CatalogImpl();
        
        // Create some entities to use across hierarchies
        UUID entityId1 = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID entityId2 = UUID.fromString("10000000-0000-0000-0000-000000000002");
        UUID entityId3 = UUID.fromString("10000000-0000-0000-0000-000000000003");
        UUID entityId4 = UUID.fromString("10000000-0000-0000-0000-000000000004");

        Entity entity1 = new EntityImpl(entityId1);
        Entity entity2 = new EntityImpl(entityId2);
        Entity entity3 = new EntityImpl(entityId3);
        Entity entity4 = new EntityImpl(entityId4);

        // Create AspectDefs for AspectMapHierarchies
        PropertyDef nameProp = new PropertyDefImpl("name", PropertyType.String, null, false, true, true, false, false, false);
        PropertyDef ageProp = new PropertyDefImpl("age", PropertyType.Integer, null, false, true, true, true, false, false);
        Map<String, PropertyDef> personProps = ImmutableMap.of("name", nameProp, "age", ageProp);
        AspectDef personAspectDef = new ImmutableAspectDefImpl("person", personProps);
        
        PropertyDef titleProp = new PropertyDefImpl("title", PropertyType.String, null, false, true, true, false, false, false);
        PropertyDef descProp = new PropertyDefImpl("description", PropertyType.String, null, false, true, true, true, false, false);
        Map<String, PropertyDef> docProps = ImmutableMap.of("title", titleProp, "description", descProp);
        AspectDef docAspectDef = new ImmutableAspectDefImpl("document", docProps);
        
        // Add AspectDefs to catalog
        catalog.extend(personAspectDef);
        catalog.extend(docAspectDef);

        // 1. EntityDirectoryHierarchy
        HierarchyDef entityDirDef = new HierarchyDefImpl("userDirectory", HierarchyType.ENTITY_DIR, true);
        EntityDirectoryHierarchyImpl entityDirectory = new EntityDirectoryHierarchyImpl(entityDirDef);
        entityDirectory.put("admin", entity1);
        entityDirectory.put("user1", entity2);
        entityDirectory.put("guest", entity3);
        catalog.hierarchies().put("userDirectory", entityDirectory);
        
        // 2. EntityListHierarchy
        HierarchyDef entityListDef = new HierarchyDefImpl("taskQueue", HierarchyType.ENTITY_LIST, true);
        EntityListHierarchyImpl entityList = new EntityListHierarchyImpl(entityListDef);
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity3);
        entityList.add(entity1); // Allow duplicates in list
        catalog.hierarchies().put("taskQueue", entityList);
        
        // 3. EntitySetHierarchy
        HierarchyDef entitySetDef = new HierarchyDefImpl("activeUsers", HierarchyType.ENTITY_SET, true);
        EntitySetHierarchyImpl entitySet = new EntitySetHierarchyImpl(entitySetDef);
        entitySet.add(entity1);
        entitySet.add(entity2);
        entitySet.add(entity4);
        catalog.hierarchies().put("activeUsers", entitySet);
        
        // 4. EntityTreeHierarchy
        HierarchyDef entityTreeDef = new HierarchyDefImpl("fileSystem", HierarchyType.ENTITY_TREE, true);
        EntityTreeHierarchyImpl entityTree = new EntityTreeHierarchyImpl(entityTreeDef, entity1); // root entity
        EntityTreeHierarchyImpl.NodeImpl childNode1 = new EntityTreeHierarchyImpl.NodeImpl(entity2, entityTree.root());
        EntityTreeHierarchyImpl.NodeImpl childNode2 = new EntityTreeHierarchyImpl.NodeImpl(entity3, entityTree.root());
        entityTree.root().put("documents", childNode1);
        entityTree.root().put("images", childNode2);
        // Add nested child
        EntityTreeHierarchyImpl.NodeImpl subChild = new EntityTreeHierarchyImpl.NodeImpl(entity4, childNode1);
        childNode1.put("reports", subChild);
        catalog.hierarchies().put("fileSystem", entityTree);
        
        // 5. First AspectMapHierarchy (person aspects)
        AspectMapHierarchyImpl personAspects = new AspectMapHierarchyImpl(personAspectDef);
        AspectObjectMapImpl personAspect1 = new AspectObjectMapImpl(entity1, personAspectDef);
        personAspect1.unsafeWrite("name", "John Doe");
        personAspect1.unsafeWrite("age", "30");
        AspectObjectMapImpl personAspect2 = new AspectObjectMapImpl(entity2, personAspectDef);
        personAspect2.unsafeWrite("name", "Jane Smith");
        personAspect2.unsafeWrite("age", "25");
        personAspects.put(entity1, personAspect1);
        personAspects.put(entity2, personAspect2);
        catalog.hierarchies().put("personData", personAspects);
        
        // 6. Second AspectMapHierarchy (document aspects)
        AspectMapHierarchyImpl docAspects = new AspectMapHierarchyImpl(docAspectDef);
        AspectObjectMapImpl docAspect1 = new AspectObjectMapImpl(entity3, docAspectDef);
        docAspect1.unsafeWrite("title", "User Manual");
        docAspect1.unsafeWrite("description", "Complete user guide");
        AspectObjectMapImpl docAspect2 = new AspectObjectMapImpl(entity4, docAspectDef);
        docAspect2.unsafeWrite("title", "API Documentation");
        docAspect2.unsafeWrite("description", "REST API reference");
        docAspects.put(entity3, docAspect1);
        docAspects.put(entity4, docAspect2);
        catalog.hierarchies().put("documents", docAspects);
        
        // Generate JSON and verify it contains all expected components
        String result = CheapJsonRawSerializer.toJson(catalog, true);
        
        // Verify the JSON contains all the hierarchy types and data
        assertTrue(result.contains("\"userDirectory\""), "Should contain entity directory");
        assertTrue(result.contains("\"taskQueue\""), "Should contain entity list");
        assertTrue(result.contains("\"activeUsers\""), "Should contain entity set");
        assertTrue(result.contains("\"fileSystem\""), "Should contain entity tree");
        assertTrue(result.contains("\"personData\""), "Should contain person aspect map");
        assertTrue(result.contains("\"documents\""), "Should contain document aspect map");
        
        // Verify specific data content
        assertTrue(result.contains("\"admin\""), "Should contain admin user");
        assertTrue(result.contains("\"John Doe\""), "Should contain person name");
        assertTrue(result.contains("\"User Manual\""), "Should contain document title");
        assertTrue(result.contains("\"type\":\"entity_dir\""), "Should contain entity_dir type");
        assertTrue(result.contains("\"type\":\"entity_list\""), "Should contain entity_list type");
        assertTrue(result.contains("\"type\":\"entity_set\""), "Should contain entity_set type");
        assertTrue(result.contains("\"type\":\"entity_tree\""), "Should contain entity_tree type");
        assertTrue(result.contains("\"type\":\"aspect_map\""), "Should contain aspect_map type");
        
        // Verify nested tree structure
        assertTrue(result.contains("\"children\""), "Should contain tree children");
        assertTrue(result.contains("\"reports\""), "Should contain nested tree node");
        
        // Verify aspects are properly serialized
        assertTrue(result.contains("\"aspectDefName\":\"person\""), "Should contain person aspect");
        assertTrue(result.contains("\"aspectDefName\":\"document\""), "Should contain document aspect");
    }

    private String loadJsonFromFile(String filename) throws IOException
    {
        Path path = Paths.get("src/test/resources/json/" + filename);
        return Files.readString(path).trim();
    }
}