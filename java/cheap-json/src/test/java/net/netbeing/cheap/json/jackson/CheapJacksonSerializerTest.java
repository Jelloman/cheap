package net.netbeing.cheap.json.jackson;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.json.jackson.serialize.CheapJacksonSerializer;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for CheapJacksonSerializer using actual implementation classes.
 * Tests the Jackson-based serializer against the raw serializer to ensure
 * equivalent functionality.
 */
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class CheapJacksonSerializerTest
{
    private static final UUID CATALOG_ID = UUID.fromString("550e8400-e29b-41d4-a716-444444444444");

    private static final boolean WRITE_OUTPUT = true;
    private static final String WRITE_OUTPUT_PATH = "D:\\src\\tmp";

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

    private CatalogImpl setupSimpleCatalog()
    {
        // Create a simple catalog with no custom aspects or hierarchies
        return new CatalogImpl(CATALOG_ID);
    }

    @Test
    void testSimpleCatalogToJson() throws IOException
    {
        CatalogImpl catalog = setupSimpleCatalog();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        Files.writeString(Paths.get("D:\\src\\tmp", "simple-catalog.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("simple-catalog.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testSimpleCatalogToJsonCompact() throws IOException
    {
        CatalogImpl catalog = setupSimpleCatalog();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        Files.writeString(Paths.get("D:\\src\\tmp", "simple-catalog-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("simple-catalog-compact.json");

        assertEquals(expectedJson, jacksonResult);
    }
    
    @Test
    void testCatalogWithAspectDefToJson() throws IOException
    {
        CatalogImpl catalog = createTestCatalog();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        Files.writeString(Paths.get("D:\\src\\tmp", "catalog-with-aspectdef.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("catalog-with-aspectdef.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithAspectDefToJsonCompact() throws IOException
    {
        CatalogImpl catalog = createTestCatalog();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        Files.writeString(Paths.get("D:\\src\\tmp", "catalog-with-aspectdef-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("catalog-with-aspectdef-compact.json");

        assertEquals(expectedJson, jacksonResult);
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

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        Files.writeString(Paths.get("D:\\src\\tmp", "entity-directory.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("entity-directory.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithEntityDirectoryHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityDirectoryHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        Files.writeString(Paths.get("D:\\src\\tmp", "entity-directory-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("entity-directory-compact.json");

        assertEquals(expectedJson, jacksonResult);
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

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        Files.writeString(Paths.get("D:\\src\\tmp", "entity-list.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("entity-list.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithEntityListHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityListHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        Files.writeString(Paths.get("D:\\src\\tmp", "entity-list-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("entity-list-compact.json");

        assertEquals(expectedJson, jacksonResult);
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

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        Files.writeString(Paths.get("D:\\src\\tmp", "entity-set.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("entity-set.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithEntitySetHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntitySetHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        Files.writeString(Paths.get("D:\\src\\tmp", "entity-set-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("entity-set-compact.json");

        assertEquals(expectedJson, jacksonResult);
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

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        Files.writeString(Paths.get("D:\\src\\tmp", "entity-tree.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("entity-tree.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithEntityTreeHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityTreeHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        Files.writeString(Paths.get("D:\\src\\tmp", "entity-tree-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("entity-tree-compact.json");

        assertEquals(expectedJson, jacksonResult);
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

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        Files.writeString(Paths.get("D:\\src\\tmp", "aspect-map.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("aspect-map.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithAspectMapHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithAspectMapHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        Files.writeString(Paths.get("D:\\src\\tmp", "aspect-map-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("aspect-map-compact.json");

        assertEquals(expectedJson, jacksonResult);
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
        personAspect1.unsafeWrite("age", 30);
        personAspect1.unsafeWrite("name", "John Doe");
        AspectObjectMapImpl personAspect2 = new AspectObjectMapImpl(entity2, personAspectDef);
        personAspect2.unsafeWrite("age", 25);
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

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        Files.writeString(Paths.get("D:\\src\\tmp", "full-catalog.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("full-catalog.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testFullCatalogWithAllHierarchyTypesCompact() throws IOException
    {
        CatalogImpl catalog = setupFullCatalogWithAllHierarchyTypes();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        Files.writeString(Paths.get("D:\\src\\tmp", "full-catalog-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String expectedJson = loadExpectedJson("full-catalog-compact.json");

        assertEquals(expectedJson, jacksonResult);
    }

    private String loadExpectedJson(String filename) throws IOException
    {
        try (InputStream is = getClass().getResourceAsStream("/jackson/" + filename)) {
            if (is == null) {
                throw new IOException("Resource not found: /jackson/" + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}