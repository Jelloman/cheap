package net.netbeing.cheap.json.jackson;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for CheapJacksonSerializer using actual implementation classes.
 * Tests the Jackson-based serializer against the raw serializer to ensure
 * equivalent functionality.
 */
public class CheapJacksonSerializerTest
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
        String expectedJson = loadExpectedJson("simple-catalog-expected.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testSimpleCatalogToJsonCompact() throws IOException
    {
        CatalogImpl catalog = setupSimpleCatalog();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        String expectedJson = loadExpectedJson("simple-catalog-expected-compact.json");

        assertEquals(expectedJson, jacksonResult);
    }
    
    @Test
    void testCatalogWithAspectDefToJson() throws IOException
    {
        CatalogImpl catalog = createTestCatalog();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        String expectedJson = loadExpectedJson("catalog-with-aspectdef-expected.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithAspectDefToJsonCompact() throws IOException
    {
        CatalogImpl catalog = createTestCatalog();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        String expectedJson = loadExpectedJson("catalog-with-aspectdef-expected-compact.json");

        assertEquals(expectedJson, jacksonResult);
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
        catalog.hierarchies().put("userDirectory", entityDirectory);

        return catalog;
    }

    @Test
    void testCatalogWithEntityDirectoryHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityDirectoryHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        String expectedJson = loadExpectedJson("entity-directory-expected.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithEntityDirectoryHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityDirectoryHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        String expectedJson = loadExpectedJson("entity-directory-expected-compact.json");

        assertEquals(expectedJson, jacksonResult);
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
        catalog.hierarchies().put("taskQueue", entityList);

        return catalog;
    }

    @Test
    void testCatalogWithEntityListHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityListHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        String expectedJson = loadExpectedJson("entity-list-expected.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithEntityListHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityListHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        String expectedJson = loadExpectedJson("entity-list-expected-compact.json");

        assertEquals(expectedJson, jacksonResult);
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
        catalog.hierarchies().put("activeUsers", entitySet);

        return catalog;
    }

    @Test
    void testCatalogWithEntitySetHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntitySetHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        String expectedJson = loadExpectedJson("entity-set-expected.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithEntitySetHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntitySetHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        String expectedJson = loadExpectedJson("entity-set-expected-compact.json");

        assertEquals(expectedJson, jacksonResult);
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
        catalog.hierarchies().put("fileSystem", entityTree);

        return catalog;
    }

    @Test
    void testCatalogWithEntityTreeHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityTreeHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        String expectedJson = loadExpectedJson("entity-tree-expected.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithEntityTreeHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityTreeHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        String expectedJson = loadExpectedJson("entity-tree-expected-compact.json");

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
        catalog.extend(personAspectDef);

        AspectMapHierarchyImpl personAspects = new AspectMapHierarchyImpl(personAspectDef);
        AspectObjectMapImpl personAspect = new AspectObjectMapImpl(entity, personAspectDef);
        personAspect.unsafeWrite("name", "John Doe");
        personAspects.put(entity, personAspect);
        catalog.hierarchies().put("personData", personAspects);

        return catalog;
    }

    @Test
    void testCatalogWithAspectMapHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithAspectMapHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        String expectedJson = loadExpectedJson("aspect-map-expected.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithAspectMapHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithAspectMapHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        String expectedJson = loadExpectedJson("aspect-map-expected-compact.json");

        assertEquals(expectedJson, jacksonResult);
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
        Map<String, PropertyDef> personProps = ImmutableMap.of("age", ageProp, "name", nameProp);
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
        personAspect1.unsafeWrite("age", "30");
        personAspect1.unsafeWrite("name", "John Doe");
        AspectObjectMapImpl personAspect2 = new AspectObjectMapImpl(entity2, personAspectDef);
        personAspect2.unsafeWrite("age", "25");
        personAspect2.unsafeWrite("name", "Jane Smith");
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

        return catalog;
    }

    @Test
    void testFullCatalogWithAllHierarchyTypes() throws IOException
    {
        CatalogImpl catalog = setupFullCatalogWithAllHierarchyTypes();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        String expectedJson = loadExpectedJson("full-catalog-expected.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testFullCatalogWithAllHierarchyTypesCompact() throws IOException
    {
        CatalogImpl catalog = setupFullCatalogWithAllHierarchyTypes();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        String expectedJson = loadExpectedJson("full-catalog-expected-compact.json");

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