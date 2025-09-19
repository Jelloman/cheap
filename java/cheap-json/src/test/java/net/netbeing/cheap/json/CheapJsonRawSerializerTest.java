package net.netbeing.cheap.json;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for CheapJsonUtil using actual implementation classes.
 */
public class CheapJsonRawSerializerTest
{
    private static final UUID CATALOG_ID = UUID.fromString("550e8400-e29b-41d4-a716-444444444444");

    private static CatalogImpl createTestCatalog()
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        // Create a simple AspectDef
        PropertyDef nameProp = new PropertyDefImpl("name", PropertyType.String, null, false, true, true, false, false, false);
        PropertyDef ageProp = new PropertyDefImpl("age", PropertyType.Integer, null, false, true, true, true, false, false);
        Map<String, PropertyDef> personProps = ImmutableMap.of("name", nameProp, "age", ageProp);
        AspectDef personAspectDef = new ImmutableAspectDefImpl("person", personProps);

        catalog.extend(personAspectDef);
        return catalog;
    }
    @Test
    void testPropertyDefToJson() throws IOException
    {
        PropertyDef propertyDef = new PropertyDefImpl("testProp", PropertyType.String, null, false, true, false, true, false, false);
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.propertyDefToJson(propertyDef, sb, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "propertydef-expected.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("propertydef-expected.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testPropertyDefWithDefaultValueToJson() throws IOException
    {
        PropertyDef propertyDef = new PropertyDefImpl("testProp", PropertyType.Integer, "42", true, true, true, true, true, false);
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.propertyDefToJson(propertyDef, sb, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "propertydef-with-default-expected.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("propertydef-with-default-expected.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testHierarchyDefToJson() throws IOException
    {
        HierarchyDef hierarchyDef = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_LIST, true);
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.hierarchyDefToJson(hierarchyDef, sb, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "hierarchydef-expected.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("hierarchydef-expected.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testAspectToJson() throws IOException
    {
        Entity entity = new EntityImpl(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        PropertyDef prop1 = new PropertyDefImpl("name", PropertyType.String, null, false, true, true, false, false, false);

        Map<String, PropertyDef> propDefMap = ImmutableMap.of("name", prop1);
        AspectDef aspectDef = new ImmutableAspectDefImpl("testAspect", propDefMap);

        AspectObjectMapImpl aspect = new AspectObjectMapImpl(entity, aspectDef);
        aspect.unsafeWrite("name", "John Doe");

        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.aspectToJson(aspect, sb, true, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "aspect-expected.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("aspect-expected.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testEntityListHierarchyToJson() throws IOException
    {
        UUID uuid1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        HierarchyDef def = new HierarchyDefImpl("entityList", HierarchyType.ENTITY_LIST, true);
        EntityListHierarchyImpl hierarchy = new EntityListHierarchyImpl(def);
        hierarchy.add(new EntityImpl(uuid1));
        hierarchy.add(new EntityImpl(uuid2));

        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.entityListHierarchyToJson(hierarchy, sb, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-list-hierarchy-expected.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-list-hierarchy-expected.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testEntitySetHierarchyToJson() throws IOException
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
        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-set-hierarchy-expected.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-set-hierarchy-expected.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testEntityDirectoryHierarchyToJson() throws IOException
    {
        UUID uuid1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        HierarchyDef def = new HierarchyDefImpl("entityDir", HierarchyType.ENTITY_DIR, true);
        EntityDirectoryHierarchyImpl hierarchy = new EntityDirectoryHierarchyImpl(def);
        hierarchy.put("first", new EntityImpl(uuid1));
        hierarchy.put("second", new EntityImpl(uuid2));

        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.entityDirectoryHierarchyToJson(hierarchy, sb, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-directory-hierarchy-expected.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-directory-hierarchy-expected.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testTreeNodeToJson() throws IOException
    {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Entity entity = new EntityImpl(uuid);
        HierarchyDef def = new HierarchyDefImpl("entityTree", HierarchyType.ENTITY_TREE, true);
        EntityTreeHierarchyImpl hierarchy = new EntityTreeHierarchyImpl(def, entity);

        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.treeNodeToJson(hierarchy.root(), sb, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "tree-node-expected.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("tree-node-expected.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testTreeNodeWithChildrenToJson() throws IOException
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

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "tree-node-with-children-expected.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("tree-node-with-children-expected.json");
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

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "aspectdef-expected.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "catalogdef-expected.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("catalogdef-expected.json");
        assertEquals(expected, sb.toString());
    }

    private CatalogImpl setupSimpleCatalog()
    {
        // Create a simple catalog with no custom aspects or hierarchies
        return new CatalogImpl(CATALOG_ID);
    }

    @Test
    void testSimpleCatalogToJson() throws IOException
    {
        CatalogImpl catalog = setupSimpleCatalog();

        String result = CheapJsonRawSerializer.toJson(catalog, true);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "simple-catalog-expected.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("simple-catalog-expected.json");
        assertEquals(expected, result);
    }

    @Test
    void testSimpleCatalogToJsonCompact() throws IOException
    {
        CatalogImpl catalog = setupSimpleCatalog();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "simple-catalog-expected-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("simple-catalog-expected-compact.json");
        assertEquals(expected, result);
    }

    @Test
    void testCatalogWithAspectDefToJson() throws IOException
    {
        CatalogImpl catalog = createTestCatalog();

        String result = CheapJsonRawSerializer.toJson(catalog, true);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "catalog-with-aspectdef-expected.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("catalog-with-aspectdef-expected.json");
        assertEquals(expected, result);
    }

    @Test
    void testCatalogWithAspectDefToJsonCompact() throws IOException
    {
        CatalogImpl catalog = createTestCatalog();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "catalog-with-aspectdef-expected-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("catalog-with-aspectdef-expected-compact.json");
        assertEquals(expected, result);
    }

    private CatalogImpl setupCatalogWithEntityDirectoryHierarchy()
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        UUID entityId1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID entityId2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Entity entity1 = new EntityImpl(entityId1);
        Entity entity2 = new EntityImpl(entityId2);

        HierarchyDef entityDirDef = new HierarchyDefImpl("userDirectory", HierarchyType.ENTITY_DIR, true);
        EntityDirectoryHierarchyImpl entityDirectory = new EntityDirectoryHierarchyImpl(entityDirDef);
        entityDirectory.put("admin", entity1);
        entityDirectory.put("user1", entity2);
        catalog.addHierarchy(entityDirectory);

        return catalog;
    }

    @Test
    void testCatalogWithEntityDirectoryHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityDirectoryHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, true);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-directory-expected.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-directory-expected.json");
        assertEquals(expected, result);
    }

    @Test
    void testCatalogWithEntityDirectoryHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityDirectoryHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-directory-expected-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-directory-expected-compact.json");
        assertEquals(expected, result);
    }

    private CatalogImpl setupCatalogWithEntityListHierarchy()
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        UUID entityId1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID entityId2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Entity entity1 = new EntityImpl(entityId1);
        Entity entity2 = new EntityImpl(entityId2);

        HierarchyDef entityListDef = new HierarchyDefImpl("taskQueue", HierarchyType.ENTITY_LIST, true);
        EntityListHierarchyImpl entityList = new EntityListHierarchyImpl(entityListDef);
        entityList.add(entity1);
        entityList.add(entity2);
        catalog.addHierarchy(entityList);

        return catalog;
    }

    @Test
    void testCatalogWithEntityListHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityListHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, true);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-list-expected.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-list-expected.json");
        assertEquals(expected, result);
    }

    @Test
    void testCatalogWithEntityListHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityListHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-list-expected-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-list-expected-compact.json");
        assertEquals(expected, result);
    }

    private CatalogImpl setupCatalogWithEntitySetHierarchy()
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        UUID entityId1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID entityId2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Entity entity1 = new EntityImpl(entityId1);
        Entity entity2 = new EntityImpl(entityId2);

        HierarchyDef entitySetDef = new HierarchyDefImpl("activeUsers", HierarchyType.ENTITY_SET, true);
        EntitySetHierarchyImpl entitySet = new EntitySetHierarchyImpl(entitySetDef);
        entitySet.add(entity1);
        entitySet.add(entity2);
        catalog.addHierarchy(entitySet);

        return catalog;
    }

    @Test
    void testCatalogWithEntitySetHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntitySetHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, true);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-set-expected.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-set-expected.json");
        assertEquals(expected, result);
    }

    @Test
    void testCatalogWithEntitySetHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntitySetHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-set-expected-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-set-expected-compact.json");
        assertEquals(expected, result);
    }

    private CatalogImpl setupCatalogWithEntityTreeHierarchy()
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        UUID entityId1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID entityId2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Entity parentEntity = new EntityImpl(entityId1);
        Entity childEntity = new EntityImpl(entityId2);

        HierarchyDef entityTreeDef = new HierarchyDefImpl("fileSystem", HierarchyType.ENTITY_TREE, true);
        EntityTreeHierarchyImpl entityTree = new EntityTreeHierarchyImpl(entityTreeDef, parentEntity);
        EntityTreeHierarchyImpl.NodeImpl childNode = new EntityTreeHierarchyImpl.NodeImpl(childEntity, entityTree.root());
        entityTree.root().put("documents", childNode);
        catalog.addHierarchy(entityTree);

        return catalog;
    }

    @Test
    void testCatalogWithEntityTreeHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityTreeHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, true);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-tree-expected.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-tree-expected.json");
        assertEquals(expected, result);
    }

    @Test
    void testCatalogWithEntityTreeHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityTreeHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-tree-expected-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-tree-expected-compact.json");
        assertEquals(expected, result);
    }

    private CatalogImpl setupCatalogWithAspectMapHierarchy()
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        UUID entityId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Entity entity = new EntityImpl(entityId);

        PropertyDef nameProp = new PropertyDefImpl("name", PropertyType.String, null, false, true, true, false, false, false);
        Map<String, PropertyDef> personProps = ImmutableMap.of("name", nameProp);
        AspectDef personAspectDef = new ImmutableAspectDefImpl("person", personProps);
        AspectMapHierarchy personAspects = catalog.extend(personAspectDef);

        AspectObjectMapImpl personAspect = new AspectObjectMapImpl(entity, personAspectDef);
        personAspect.unsafeWrite("name", "John Doe");
        personAspects.put(entity, personAspect);

        return catalog;
    }

    @Test
    void testCatalogWithAspectMapHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithAspectMapHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, true);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "aspect-map-expected.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("aspect-map-expected.json");
        assertEquals(expected, result);
    }

    @Test
    void testCatalogWithAspectMapHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithAspectMapHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "aspect-map-expected-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("aspect-map-expected-compact.json");
        assertEquals(expected, result);
    }

    private CatalogImpl setupFullCatalogWithAllHierarchyTypes()
    {
        // Create a catalog with all hierarchy types and real data
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);
        
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
        AspectMapHierarchy personAspects = catalog.extend(personAspectDef);
        AspectMapHierarchy docAspects = catalog.extend(docAspectDef);

        // 1. EntityDirectoryHierarchy
        HierarchyDef entityDirDef = new HierarchyDefImpl("userDirectory", HierarchyType.ENTITY_DIR, true);
        EntityDirectoryHierarchyImpl entityDirectory = new EntityDirectoryHierarchyImpl(entityDirDef);
        entityDirectory.put("admin", entity1);
        entityDirectory.put("user1", entity2);
        entityDirectory.put("guest", entity3);
        catalog.addHierarchy(entityDirectory);
        
        // 2. EntityListHierarchy
        HierarchyDef entityListDef = new HierarchyDefImpl("taskQueue", HierarchyType.ENTITY_LIST, true);
        EntityListHierarchyImpl entityList = new EntityListHierarchyImpl(entityListDef);
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity3);
        entityList.add(entity1); // Allow duplicates in list
        catalog.addHierarchy(entityList);
        
        // 3. EntitySetHierarchy
        HierarchyDef entitySetDef = new HierarchyDefImpl("activeUsers", HierarchyType.ENTITY_SET, true);
        EntitySetHierarchyImpl entitySet = new EntitySetHierarchyImpl(entitySetDef);
        entitySet.add(entity1);
        entitySet.add(entity2);
        entitySet.add(entity4);
        catalog.addHierarchy(entitySet);
        
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
        catalog.addHierarchy(entityTree);
        
        // 5. First AspectMapHierarchy (person aspects)
        AspectObjectMapImpl personAspect1 = new AspectObjectMapImpl(entity1, personAspectDef);
        personAspect1.unsafeWrite("name", "John Doe");
        personAspect1.unsafeWrite("age", "30");
        AspectObjectMapImpl personAspect2 = new AspectObjectMapImpl(entity2, personAspectDef);
        personAspect2.unsafeWrite("name", "Jane Smith");
        personAspect2.unsafeWrite("age", "25");
        personAspects.put(entity1, personAspect1);
        personAspects.put(entity2, personAspect2);

        // 6. Second AspectMapHierarchy (document aspects)
        AspectObjectMapImpl docAspect1 = new AspectObjectMapImpl(entity3, docAspectDef);
        docAspect1.unsafeWrite("title", "User Manual");
        docAspect1.unsafeWrite("description", "Complete user guide");
        AspectObjectMapImpl docAspect2 = new AspectObjectMapImpl(entity4, docAspectDef);
        docAspect2.unsafeWrite("title", "API Documentation");
        docAspect2.unsafeWrite("description", "REST API reference");
        docAspects.put(entity3, docAspect1);
        docAspects.put(entity4, docAspect2);

        return catalog;
    }

    @Test
    void testFullCatalogWithAllHierarchyTypes() throws IOException
    {
        CatalogImpl catalog = setupFullCatalogWithAllHierarchyTypes();

        String result = CheapJsonRawSerializer.toJson(catalog, true);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "full-catalog-expected.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("full-catalog-expected.json");
        assertEquals(expected, result);
    }

    @Test
    void testFullCatalogWithAllHierarchyTypesCompact() throws IOException
    {
        CatalogImpl catalog = setupFullCatalogWithAllHierarchyTypes();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "full-catalog-expected-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("full-catalog-expected-compact.json");
        assertEquals(expected, result);
    }

    private String loadJsonFromFile(String filename) throws IOException
    {
        // Try to use classpath first
        try {
            var resource = getClass().getClassLoader().getResourceAsStream("json/" + filename);
            if (resource != null) {
                return new String(resource.readAllBytes()).trim();
            }
        } catch (Exception e) {
            // Fall back to file system path
        }

        // Fall back to the original path approach
        Path path = Paths.get("src/test/resources/json/" + filename);
        return Files.readString(path).trim();
    }
}