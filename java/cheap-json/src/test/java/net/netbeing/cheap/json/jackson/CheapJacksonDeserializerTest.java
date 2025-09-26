package net.netbeing.cheap.json.jackson;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.json.jackson.deserialize.CheapJacksonDeserializer;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CheapJacksonDeserializer using JSON files from test resources.
 * Tests the Jackson-based deserializer's ability to reconstruct Cheap objects
 * from their JSON representations.
 */
public class CheapJacksonDeserializerTest
{
    private final CheapJacksonDeserializer deserializer = new CheapJacksonDeserializer();

    @Test
    void testDeserializeSimpleCatalog() throws IOException
    {
        String json = loadJsonResource("simple-catalog.json");
        Catalog catalog = deserializer.fromJson(json);

        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-444444444444"), catalog.globalId());
        assertNull(catalog.uri());
        assertEquals(CatalogSpecies.SINK, catalog.species());
        assertFalse(catalog.isStrict());
        assertNotNull(catalog.def());
        assertFalse(catalog.def().aspectDefs().iterator().hasNext());
        assertFalse(catalog.def().hierarchyDefs().iterator().hasNext());
    }

    @Test
    void testDeserializeCatalogWithAspectDef() throws IOException
    {
        String json = loadJsonResource("catalog-with-aspectdef.json");
        Catalog catalog = deserializer.fromJson(json);

        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-444444444444"), catalog.globalId());
        assertTrue(catalog.def().aspectDefs().iterator().hasNext());
        AspectDef personAspectDef = catalog.def().aspectDef("person");
        assertNotNull(personAspectDef);
        assertEquals("person", personAspectDef.name());
        PropertyDef nameProp = personAspectDef.propertyDef("name");
        assertNotNull(nameProp);
        assertEquals("name", nameProp.name());
        assertEquals(PropertyType.String, nameProp.type());
        assertFalse(nameProp.isNullable());

        PropertyDef ageProp = personAspectDef.propertyDef("age");
        assertNotNull(ageProp);
        assertEquals("age", ageProp.name());
        assertEquals(PropertyType.Integer, ageProp.type());
        assertTrue(ageProp.isNullable());
    }

    @Test
    void testDeserializeEntityDirectoryHierarchy() throws IOException
    {
        String json = loadJsonResource("entity-directory.json");
        Catalog catalog = deserializer.fromJson(json);

        Hierarchy hierarchy = catalog.hierarchy("userDirectory");
        assertNotNull(hierarchy);
        assertInstanceOf(EntityDirectoryHierarchy.class, hierarchy);

        EntityDirectoryHierarchy entityDir = (EntityDirectoryHierarchy) hierarchy;
        assertEquals(HierarchyType.ENTITY_DIR, entityDir.def().type());
        assertTrue(entityDir.def().isModifiable());

        assertTrue(entityDir.containsKey("admin"));
        assertTrue(entityDir.containsKey("user1"));

        Entity adminEntity = entityDir.get("admin");
        assertNotNull(adminEntity);
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), adminEntity.globalId());
    }

    @Test
    void testDeserializeEntityListHierarchy() throws IOException
    {
        String json = loadJsonResource("entity-list.json");
        Catalog catalog = deserializer.fromJson(json);

        Hierarchy hierarchy = catalog.hierarchy("taskQueue");
        assertNotNull(hierarchy);
        assertInstanceOf(EntityListHierarchy.class, hierarchy);

        EntityListHierarchy entityList = (EntityListHierarchy) hierarchy;
        assertEquals(HierarchyType.ENTITY_LIST, entityList.def().type());
        assertEquals(2, entityList.size());

        Entity firstEntity = entityList.get(0);
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), firstEntity.globalId());
    }

    @Test
    void testDeserializeEntitySetHierarchy() throws IOException
    {
        String json = loadJsonResource("entity-set.json");
        Catalog catalog = deserializer.fromJson(json);

        Hierarchy hierarchy = catalog.hierarchy("activeUsers");
        assertNotNull(hierarchy);
        assertInstanceOf(EntitySetHierarchy.class, hierarchy);

        EntitySetHierarchy entitySet = (EntitySetHierarchy) hierarchy;
        assertEquals(HierarchyType.ENTITY_SET, entitySet.def().type());
        assertEquals(2, entitySet.size());

        assertTrue(entitySet.contains(deserializer.getFactory().getEntity(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))));
    }

    @Test
    void testDeserializeEntityTreeHierarchy() throws IOException
    {
        String json = loadJsonResource("entity-tree.json");
        Catalog catalog = deserializer.fromJson(json);

        Hierarchy hierarchy = catalog.hierarchy("fileSystem");
        assertNotNull(hierarchy);
        assertInstanceOf(EntityTreeHierarchy.class, hierarchy);

        EntityTreeHierarchy entityTree = (EntityTreeHierarchy) hierarchy;
        assertEquals(HierarchyType.ENTITY_TREE, entityTree.def().type());

        EntityTreeHierarchy.Node root = entityTree.root();
        assertNotNull(root);
        assertNotNull(root.value());
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), root.value().globalId());

        assertTrue(root.containsKey("documents"));
        EntityTreeHierarchy.Node documentsNode = root.get("documents");
        assertNotNull(documentsNode);
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"), documentsNode.value().globalId());
    }

    @Test
    void testDeserializeAspectMapHierarchy() throws IOException
    {
        // Use a fresh deserializer with custom factory to avoid AspectDef conflicts from previous tests
        CheapFactory customFactory = new CheapFactory();
        CheapJacksonDeserializer freshDeserializer = new CheapJacksonDeserializer(customFactory);
        String json = loadJsonResource("aspect-map.json");
        Catalog catalog = freshDeserializer.fromJson(json);

        Hierarchy hierarchy = catalog.hierarchy("person");
        assertNotNull(hierarchy);
        assertInstanceOf(AspectMapHierarchy.class, hierarchy);

        AspectMapHierarchy aspectMap = (AspectMapHierarchy) hierarchy;
        assertEquals(HierarchyType.ASPECT_MAP, aspectMap.def().type());
        assertEquals("person", aspectMap.aspectDef().name());

        assertFalse(aspectMap.isEmpty());
        Entity entity = freshDeserializer.getFactory().getEntity(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        assertTrue(aspectMap.containsKey(entity));

        Aspect aspect = aspectMap.get(entity);
        assertNotNull(aspect);
        assertEquals("John Doe", aspect.readObj("name"));
    }

    @Test
    void testDeserializeFullCatalogWithAllHierarchyTypes() throws IOException
    {
        // Use a fresh deserializer with custom factory to avoid AspectDef conflicts from previous tests
        CheapFactory customFactory = new CheapFactory();
        CheapJacksonDeserializer freshDeserializer = new CheapJacksonDeserializer(customFactory);
        String json = loadJsonResource("full-catalog.json");
        Catalog catalog = freshDeserializer.fromJson(json);

        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-444444444444"), catalog.globalId());
        assertEquals(CatalogSpecies.SINK, catalog.species());

        // Verify AspectDefs
        int aspectDefCount = 0;
        for (AspectDef ad : catalog.def().aspectDefs()) {
            aspectDefCount++;
        }
        assertEquals(2, aspectDefCount);
        assertNotNull(catalog.def().aspectDef("person"));
        assertNotNull(catalog.def().aspectDef("document"));

        // Verify HierarchyDefs
        int hierarchyDefCount = 0;
        for (HierarchyDef hd : catalog.def().hierarchyDefs()) {
            hierarchyDefCount++;
        }
        assertEquals(5, hierarchyDefCount);
        assertNotNull(catalog.def().hierarchyDef("person"));
        assertNotNull(catalog.def().hierarchyDef("document"));
        assertNotNull(catalog.def().hierarchyDef("userDirectory"));
        assertNotNull(catalog.def().hierarchyDef("taskQueue"));
        assertNotNull(catalog.def().hierarchyDef("activeUsers"));

        // Verify all hierarchies are present
        int hierarchyCount = 0;
        for (Hierarchy h : catalog.hierarchies()) {
            hierarchyCount++;
        }
        assertEquals(6, hierarchyCount);
        assertNotNull(catalog.hierarchy("person"));
        assertNotNull(catalog.hierarchy("document"));
        assertNotNull(catalog.hierarchy("userDirectory"));
        assertNotNull(catalog.hierarchy("taskQueue"));
        assertNotNull(catalog.hierarchy("activeUsers"));
        assertNotNull(catalog.hierarchy("fileSystem"));

        // Test EntityTreeHierarchy with nested structure
        EntityTreeHierarchy entityTree = (EntityTreeHierarchy) catalog.hierarchy("fileSystem");
        EntityTreeHierarchy.Node root = entityTree.root();
        assertTrue(root.containsKey("documents"));
        EntityTreeHierarchy.Node documentsNode = root.get("documents");
        assertTrue(documentsNode.containsKey("reports"));

        // Test AspectMapHierarchy with actual data
        AspectMapHierarchy personAspects = (AspectMapHierarchy) catalog.hierarchy("person");
        assertEquals(2, personAspects.size());
        Entity entity1 = freshDeserializer.getFactory().getEntity(UUID.fromString("10000000-0000-0000-0000-000000000001"));
        Aspect personAspect = personAspects.get(entity1);
        assertEquals("John Doe", personAspect.readObj("name"));
        assertEquals(30L, personAspect.readObj("age"));
    }

    @Test
    void testDeserializeCompactFormats() throws IOException
    {
        // Test that compact formats can be deserialized correctly
        String compactJson = loadJsonResource("simple-catalog-expected-compact.json");
        String prettyJson = loadJsonResource("simple-catalog.json");

        Catalog compactCatalog = deserializer.fromJson(compactJson);
        Catalog prettyCatalog = deserializer.fromJson(prettyJson);

        assertEquals(compactCatalog.globalId(), prettyCatalog.globalId());
        assertEquals(compactCatalog.species(), prettyCatalog.species());
        assertEquals(compactCatalog.isStrict(), prettyCatalog.isStrict());
    }

    @Test
    void testDeserializerWithCustomFactory() throws IOException
    {
        CheapFactory customFactory = new CheapFactory();
        CheapJacksonDeserializer customDeserializer = new CheapJacksonDeserializer(customFactory);

        String json = loadJsonResource("simple-catalog.json");
        Catalog catalog = customDeserializer.fromJson(json);

        assertSame(customFactory, customDeserializer.getFactory());
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-444444444444"), catalog.globalId());
    }

    private String loadJsonResource(String filename) throws IOException
    {
        try (InputStream is = getClass().getResourceAsStream("/jackson/" + filename)) {
            if (is == null) {
                throw new IOException("Resource not found: /jackson/" + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}