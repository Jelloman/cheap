package net.netbeing.cheap.json;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for CheapJsonUtil using actual implementation classes.
 */
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
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
        Map<String, PropertyDef> testProps = ImmutableMap.of("testProp", propertyDef);
        AspectDef testAspectDef = new ImmutableAspectDefImpl("test", testProps);
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.propertyDefToJson(testAspectDef, propertyDef, sb, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "propertydef.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("propertydef.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testPropertyDefWithDefaultValueToJson() throws IOException
    {
        PropertyDef propertyDef = new PropertyDefImpl("testProp", PropertyType.Integer, "42", true, true, true, true, true, false);
        Map<String, PropertyDef> testProps = ImmutableMap.of("testProp", propertyDef);
        AspectDef testAspectDef = new ImmutableAspectDefImpl("test", testProps);
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.propertyDefToJson(testAspectDef, propertyDef, sb, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "propertydef-with-default.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("propertydef-with-default.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testHierarchyDefToJson() throws IOException
    {
        HierarchyDef hierarchyDef = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_LIST);
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.hierarchyDefToJson(hierarchyDef, sb, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "hierarchydef.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("hierarchydef.json");
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

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "aspect.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("aspect.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testEntityListHierarchyToJson() throws IOException
    {
        UUID uuid1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        Catalog catalog = new CatalogImpl();
        HierarchyDef def = new HierarchyDefImpl("entityList", HierarchyType.ENTITY_LIST);
        EntityListHierarchyImpl hierarchy = new EntityListHierarchyImpl(catalog, def.name());
        hierarchy.add(new EntityImpl(uuid1));
        hierarchy.add(new EntityImpl(uuid2));

        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.entityListHierarchyToJson(hierarchy, sb, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-list-hierarchy.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-list-hierarchy.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testEntitySetHierarchyToJson() throws IOException
    {
        UUID uuid1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        HierarchyDef def = new HierarchyDefImpl("entitySet", HierarchyType.ENTITY_SET);
        EntitySetHierarchyImpl hierarchy = new EntitySetHierarchyImpl(new CatalogImpl(), def.name());
        hierarchy.add(new EntityImpl(uuid1));
        hierarchy.add(new EntityImpl(uuid2));

        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.entitySetHierarchyToJson(hierarchy, sb, true, 0);

        // LinkedHashSet preserves insertion order, so we can predict the order
        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-set-hierarchy.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-set-hierarchy.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testEntityDirectoryHierarchyToJson() throws IOException
    {
        UUID uuid1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        HierarchyDef def = new HierarchyDefImpl("entityDir", HierarchyType.ENTITY_DIR);
        EntityDirectoryHierarchyImpl hierarchy = new EntityDirectoryHierarchyImpl(new CatalogImpl(), def.name());
        hierarchy.put("first", new EntityImpl(uuid1));
        hierarchy.put("second", new EntityImpl(uuid2));

        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.entityDirectoryHierarchyToJson(hierarchy, sb, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-directory-hierarchy.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-directory-hierarchy.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testTreeNodeToJson() throws IOException
    {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Entity entity = new EntityImpl(uuid);
        HierarchyDef def = new HierarchyDefImpl("entityTree", HierarchyType.ENTITY_TREE);
        EntityTreeHierarchyImpl hierarchy = new EntityTreeHierarchyImpl(new CatalogImpl(), def.name(), entity);

        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.treeNodeToJson(hierarchy.root(), sb, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "tree-node.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("tree-node.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testTreeNodeWithChildrenToJson() throws IOException
    {
        UUID uuid1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID uuid2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Entity parentEntity = new EntityImpl(uuid1);
        Entity childEntity = new EntityImpl(uuid2);

        HierarchyDef def = new HierarchyDefImpl("entityTree", HierarchyType.ENTITY_TREE);
        EntityTreeHierarchyImpl hierarchy = new EntityTreeHierarchyImpl(new CatalogImpl(), def.name(), parentEntity);

        EntityTreeHierarchyImpl.NodeImpl childNode = new EntityTreeHierarchyImpl.NodeImpl(childEntity, hierarchy.root());
        hierarchy.root().put("child", childNode);

        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.treeNodeToJson(hierarchy.root(), sb, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "tree-node-with-children.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("tree-node-with-children.json");
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

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "aspectdef.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("aspectdef.json");
        assertEquals(expected, sb.toString());
    }

    @Test
    void testCatalogDefToJson() throws IOException
    {
        PropertyDef prop = new PropertyDefImpl("name", PropertyType.String, null, false, true, true, false, false, false);
        Map<String, PropertyDef> propDefMap = ImmutableMap.of("name", prop);
        AspectDef aspectDef = new ImmutableAspectDefImpl("person", propDefMap);
        
        HierarchyDef hierarchyDef = new HierarchyDefImpl("entities", HierarchyType.ENTITY_SET);
        
        CatalogDefImpl catalogDef = new CatalogDefImpl(List.of(hierarchyDef), List.of(aspectDef));
        
        StringBuilder sb = new StringBuilder();
        CheapJsonRawSerializer.catalogDefToJson(catalogDef, sb, true, 0);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "catalogdef.json"), sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("catalogdef.json");
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

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "simple-catalog.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("simple-catalog.json");
        assertEquals(expected, result);
    }

    @Test
    void testSimpleCatalogToJsonCompact() throws IOException
    {
        CatalogImpl catalog = setupSimpleCatalog();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "simple-catalog-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("simple-catalog-compact.json");
        assertEquals(expected, result);
    }

    @Test
    void testCatalogWithAspectDefToJson() throws IOException
    {
        CatalogImpl catalog = createTestCatalog();

        String result = CheapJsonRawSerializer.toJson(catalog, true);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "catalog-with-aspectdef.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("catalog-with-aspectdef.json");
        assertEquals(expected, result);
    }

    @Test
    void testCatalogWithAspectDefToJsonCompact() throws IOException
    {
        CatalogImpl catalog = createTestCatalog();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "catalog-with-aspectdef-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("catalog-with-aspectdef-compact.json");
        assertEquals(expected, result);
    }

    private CatalogImpl setupCatalogWithEntityDirectoryHierarchy()
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        UUID entityId1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID entityId2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Entity entity1 = new EntityImpl(entityId1);
        Entity entity2 = new EntityImpl(entityId2);

        HierarchyDef entityDirDef = new HierarchyDefImpl("userDirectory", HierarchyType.ENTITY_DIR);
        EntityDirectoryHierarchyImpl entityDirectory = new EntityDirectoryHierarchyImpl(catalog, entityDirDef.name());
        entityDirectory.put("admin", entity1);
        entityDirectory.put("user1", entity2);

        return catalog;
    }

    @Test
    void testCatalogWithEntityDirectoryHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityDirectoryHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, true);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-directory.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-directory.json");
        assertEquals(expected, result);
    }

    @Test
    void testCatalogWithEntityDirectoryHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityDirectoryHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-directory-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-directory-compact.json");
        assertEquals(expected, result);
    }

    private CatalogImpl setupCatalogWithEntityListHierarchy()
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        UUID entityId1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID entityId2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Entity entity1 = new EntityImpl(entityId1);
        Entity entity2 = new EntityImpl(entityId2);

        HierarchyDef entityListDef = new HierarchyDefImpl("taskQueue", HierarchyType.ENTITY_LIST);
        EntityListHierarchyImpl entityList = new EntityListHierarchyImpl(catalog, entityListDef.name());
        entityList.add(entity1);
        entityList.add(entity2);

        return catalog;
    }

    @Test
    void testCatalogWithEntityListHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityListHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, true);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-list.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-list.json");
        assertEquals(expected, result);
    }

    @Test
    void testCatalogWithEntityListHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityListHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-list-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-list-compact.json");
        assertEquals(expected, result);
    }

    private CatalogImpl setupCatalogWithEntitySetHierarchy()
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        UUID entityId1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID entityId2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Entity entity1 = new EntityImpl(entityId1);
        Entity entity2 = new EntityImpl(entityId2);

        HierarchyDef entitySetDef = new HierarchyDefImpl("activeUsers", HierarchyType.ENTITY_SET);
        EntitySetHierarchyImpl entitySet = new EntitySetHierarchyImpl(catalog, entitySetDef.name());
        entitySet.add(entity1);
        entitySet.add(entity2);

        return catalog;
    }

    @Test
    void testCatalogWithEntitySetHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntitySetHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, true);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-set.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-set.json");
        assertEquals(expected, result);
    }

    @Test
    void testCatalogWithEntitySetHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntitySetHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-set-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-set-compact.json");
        assertEquals(expected, result);
    }

    private CatalogImpl setupCatalogWithEntityTreeHierarchy()
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        UUID entityId1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID entityId2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Entity parentEntity = new EntityImpl(entityId1);
        Entity childEntity = new EntityImpl(entityId2);

        HierarchyDef entityTreeDef = new HierarchyDefImpl("fileSystem", HierarchyType.ENTITY_TREE);
        EntityTreeHierarchyImpl entityTree = new EntityTreeHierarchyImpl(catalog, entityTreeDef.name(), parentEntity);
        EntityTreeHierarchyImpl.NodeImpl childNode = new EntityTreeHierarchyImpl.NodeImpl(childEntity, entityTree.root());
        entityTree.root().put("documents", childNode);

        return catalog;
    }

    @Test
    void testCatalogWithEntityTreeHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityTreeHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, true);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-tree.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-tree.json");
        assertEquals(expected, result);
    }

    @Test
    void testCatalogWithEntityTreeHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityTreeHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "entity-tree-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("entity-tree-compact.json");
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

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "aspect-map.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("aspect-map.json");
        assertEquals(expected, result);
    }

    @Test
    void testCatalogWithAspectMapHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithAspectMapHierarchy();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "aspect-map-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("aspect-map-compact.json");
        assertEquals(expected, result);
    }

    private CatalogImpl setupFullCatalogWithAllHierarchyTypes()
    {
        // Create a custom CatalogDef that doesn't include all hierarchies
        // This simulates a case where one hierarchy will have its def embedded in JSON
        PropertyDef nameProp = new PropertyDefImpl("name", PropertyType.String, null, false, true, true, false, false, false);
        PropertyDef ageProp = new PropertyDefImpl("age", PropertyType.Integer, null, false, true, true, true, false, false);
        Map<String, PropertyDef> personProps = ImmutableMap.of("age", ageProp, "name", nameProp);
        AspectDef personAspectDef = new ImmutableAspectDefImpl("person", personProps);

        PropertyDef titleProp = new PropertyDefImpl("title", PropertyType.String, null, false, true, true, false, false, false);
        PropertyDef descProp = new PropertyDefImpl("description", PropertyType.String, null, false, true, true, true, false, false);
        Map<String, PropertyDef> docProps = ImmutableMap.of("title", titleProp, "description", descProp);
        AspectDef docAspectDef = new ImmutableAspectDefImpl("document", docProps);

        // Create HierarchyDefs for most (but not all) hierarchies that will be added
        HierarchyDef entityDirDef = new HierarchyDefImpl("userDirectory", HierarchyType.ENTITY_DIR);
        HierarchyDef entityListDef = new HierarchyDefImpl("taskQueue", HierarchyType.ENTITY_LIST);
        HierarchyDef entitySetDef = new HierarchyDefImpl("activeUsers", HierarchyType.ENTITY_SET);
        // Intentionally omitting entityTreeDef from CatalogDef to test embedded def scenario
        HierarchyDef personAspectsDef = new HierarchyDefImpl("person", HierarchyType.ASPECT_MAP);
        HierarchyDef docAspectsDef = new HierarchyDefImpl("document", HierarchyType.ASPECT_MAP);

        Collection<AspectDef> aspectDefs = ImmutableList.of(personAspectDef, docAspectDef);
        Collection<HierarchyDef> hierarchyDefs = ImmutableList.of(entityDirDef, entityListDef, entitySetDef, personAspectsDef, docAspectsDef);

        CatalogDef catalogDef = new CatalogDefImpl(hierarchyDefs, aspectDefs);

        // Create a non-strict catalog with explicit CatalogDef (strict=false allows additional hierarchies)
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID, CatalogSpecies.SINK, null, 0L);

        // Create some entities to use across hierarchies
        UUID entityId1 = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID entityId2 = UUID.fromString("10000000-0000-0000-0000-000000000002");
        UUID entityId3 = UUID.fromString("10000000-0000-0000-0000-000000000003");
        UUID entityId4 = UUID.fromString("10000000-0000-0000-0000-000000000004");

        Entity entity1 = new EntityImpl(entityId1);
        Entity entity2 = new EntityImpl(entityId2);
        Entity entity3 = new EntityImpl(entityId3);
        Entity entity4 = new EntityImpl(entityId4);

        // Create AspectMapHierarchies directly (using the ones from CatalogDef)
        AspectMapHierarchyImpl personAspects = new AspectMapHierarchyImpl(catalog, personAspectDef);
        AspectMapHierarchyImpl docAspects = new AspectMapHierarchyImpl(catalog, docAspectDef);

        // 1. EntityDirectoryHierarchy (using def from CatalogDef)
        EntityDirectoryHierarchyImpl entityDirectory = new EntityDirectoryHierarchyImpl(catalog, entityDirDef.name());
        entityDirectory.put("admin", entity1);
        entityDirectory.put("user1", entity2);
        entityDirectory.put("guest", entity3);
        catalog.addHierarchy(entityDirectory);

        // 2. EntityListHierarchy (using def from CatalogDef)
        EntityListHierarchyImpl entityList = new EntityListHierarchyImpl(catalog, entityListDef.name());
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity3);
        entityList.add(entity1); // Allow duplicates in list
        catalog.addHierarchy(entityList);

        // 3. EntitySetHierarchy (using def from CatalogDef)
        EntitySetHierarchyImpl entitySet = new EntitySetHierarchyImpl(catalog, entitySetDef.name());
        entitySet.add(entity1);
        entitySet.add(entity2);
        entitySet.add(entity4);
        catalog.addHierarchy(entitySet);

        // 4. EntityTreeHierarchy (NOT in CatalogDef - will have embedded def in JSON)
        HierarchyDef entityTreeDef = new HierarchyDefImpl("fileSystem", HierarchyType.ENTITY_TREE);
        EntityTreeHierarchyImpl entityTree = new EntityTreeHierarchyImpl(catalog, entityTreeDef.name(), entity1); // root entity
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
        personAspect1.unsafeWrite("age", "30");
        personAspect1.unsafeWrite("name", "John Doe");
        AspectObjectMapImpl personAspect2 = new AspectObjectMapImpl(entity2, personAspectDef);
        personAspect2.unsafeWrite("age", "25");
        personAspect2.unsafeWrite("name", "Jane Smith");
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

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "full-catalog.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("full-catalog.json");
        assertEquals(expected, result);
    }

    @Test
    void testFullCatalogWithAllHierarchyTypesCompact() throws IOException
    {
        CatalogImpl catalog = setupFullCatalogWithAllHierarchyTypes();

        String result = CheapJsonRawSerializer.toJson(catalog, false);

        Files.writeString(Paths.get("D:\\src\\tmp\\raw", "full-catalog-compact.json"), result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expected = loadJsonFromFile("full-catalog-compact.json");
        assertEquals(expected, result);
    }

    private String loadJsonFromFile(String filename) throws IOException
    {
        // Try to use classpath first
        try (var resource = getClass().getClassLoader().getResourceAsStream("jackson/" + filename)) {
            if (resource != null) {
                return new String(resource.readAllBytes()).trim();
            }
        } catch (Exception e) {
            // Fall back to file system path
        }

        // Fall back to the original path approach
        Path path = Paths.get("src/test/resources/jackson/" + filename);
        return Files.readString(path).trim();
    }
}