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
        // isStrict() removed from model
        assertFalse(catalog.aspectDefs().iterator().hasNext());
        assertFalse(catalog.hierarchies().iterator().hasNext());
    }

    @Test
    void testDeserializeCatalogWithAspectDef() throws IOException
    {
        String json = loadJsonResource("catalog-with-aspectdef.json");
        Catalog catalog = deserializer.fromJson(json);

        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-444444444444"), catalog.globalId());
        assertTrue(catalog.aspectDefs().iterator().hasNext());
        // Find person aspectDef
        AspectDef personAspectDef = null;
        for (AspectDef ad : catalog.aspectDefs()) {
            if ("person".equals(ad.name())) {
                personAspectDef = ad;
                break;
            }
        }
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
        assertEquals(HierarchyType.ENTITY_DIR, entityDir.type());

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
        assertEquals(HierarchyType.ENTITY_LIST, entityList.type());
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
        assertEquals(HierarchyType.ENTITY_SET, entitySet.type());
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
        assertEquals(HierarchyType.ENTITY_TREE, entityTree.type());

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
        assertEquals(HierarchyType.ASPECT_MAP, aspectMap.type());
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
        for (AspectDef ad : catalog.aspectDefs()) {
            aspectDefCount++;
        }
        assertEquals(2, aspectDefCount);

        // Find person and document AspectDefs
        AspectDef personDef = null;
        AspectDef documentDef = null;
        for (AspectDef ad : catalog.aspectDefs()) {
            if ("person".equals(ad.name())) personDef = ad;
            if ("document".equals(ad.name())) documentDef = ad;
        }
        assertNotNull(personDef);
        assertNotNull(documentDef);

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
        String compactJson = loadJsonResource("simple-catalog-compact.json");
        String prettyJson = loadJsonResource("simple-catalog.json");

        Catalog compactCatalog = deserializer.fromJson(compactJson);
        Catalog prettyCatalog = deserializer.fromJson(prettyJson);

        assertEquals(compactCatalog.globalId(), prettyCatalog.globalId());
        assertEquals(compactCatalog.species(), prettyCatalog.species());
        // isStrict() removed from model
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

    @Test
    void testDeserializeMultivaluedProperties() throws IOException
    {
        // Use a fresh deserializer with custom factory
        CheapFactory customFactory = new CheapFactory();
        CheapJacksonDeserializer freshDeserializer = new CheapJacksonDeserializer(customFactory);

        String json = loadJsonResource("multivalued-properties.json");
        Catalog catalog = freshDeserializer.fromJson(json);

        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-444444444444"), catalog.globalId());

        // Verify AspectDef with multivalued properties
        AspectDef productDef = null;
        for (AspectDef ad : catalog.aspectDefs()) {
            if ("product".equals(ad.name())) {
                productDef = ad;
                break;
            }
        }
        assertNotNull(productDef);

        // Check multivalued PropertyDefs
        PropertyDef tagsProp = productDef.propertyDef("tags");
        assertNotNull(tagsProp);
        assertTrue(tagsProp.isMultivalued());
        assertEquals(PropertyType.String, tagsProp.type());

        PropertyDef scoresProp = productDef.propertyDef("scores");
        assertNotNull(scoresProp);
        assertTrue(scoresProp.isMultivalued());
        assertEquals(PropertyType.Integer, scoresProp.type());

        PropertyDef ratingsProp = productDef.propertyDef("ratings");
        assertNotNull(ratingsProp);
        assertTrue(ratingsProp.isMultivalued());
        assertEquals(PropertyType.Float, ratingsProp.type());

        PropertyDef titleProp = productDef.propertyDef("title");
        assertNotNull(titleProp);
        assertFalse(titleProp.isMultivalued());

        // Verify AspectMap with multivalued property values
        AspectMapHierarchy productAspects = (AspectMapHierarchy) catalog.hierarchy("product");
        assertNotNull(productAspects);
        assertEquals(2, productAspects.size());

        // Check first product
        Entity entity1 = freshDeserializer.getFactory().getEntity(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        Aspect product1 = productAspects.get(entity1);
        assertNotNull(product1);

        assertEquals("Smart Watch", product1.readObj("title"));

        @SuppressWarnings("unchecked")
        java.util.List<String> tags1 = (java.util.List<String>) product1.readObj("tags");
        assertNotNull(tags1);
        assertEquals(3, tags1.size());
        assertEquals("electronics", tags1.get(0));
        assertEquals("gadget", tags1.get(1));
        assertEquals("popular", tags1.get(2));

        @SuppressWarnings("unchecked")
        java.util.List<Long> scores1 = (java.util.List<Long>) product1.readObj("scores");
        assertNotNull(scores1);
        assertEquals(3, scores1.size());
        assertEquals(100L, scores1.get(0));
        assertEquals(95L, scores1.get(1));
        assertEquals(87L, scores1.get(2));

        @SuppressWarnings("unchecked")
        java.util.List<Double> ratings1 = (java.util.List<Double>) product1.readObj("ratings");
        assertNotNull(ratings1);
        assertEquals(3, ratings1.size());
        assertEquals(4.5, ratings1.get(0), 0.01);
        assertEquals(4.8, ratings1.get(1), 0.01);
        assertEquals(4.2, ratings1.get(2), 0.01);

        // Check second product
        Entity entity2 = freshDeserializer.getFactory().getEntity(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));
        Aspect product2 = productAspects.get(entity2);
        assertNotNull(product2);

        assertEquals("Office Suite", product2.readObj("title"));

        @SuppressWarnings("unchecked")
        java.util.List<String> tags2 = (java.util.List<String>) product2.readObj("tags");
        assertEquals(2, tags2.size());
        assertEquals("software", tags2.get(0));
        assertEquals("productivity", tags2.get(1));
    }

    @Test
    void testDeserializeMultivaluedPropertiesCompact() throws IOException
    {
        // Use a fresh deserializer with custom factory
        CheapFactory customFactory = new CheapFactory();
        CheapJacksonDeserializer freshDeserializer = new CheapJacksonDeserializer(customFactory);

        String json = loadJsonResource("multivalued-properties-compact.json");
        Catalog catalog = freshDeserializer.fromJson(json);

        // Verify the data is the same as pretty format
        AspectMapHierarchy productAspects = (AspectMapHierarchy) catalog.hierarchy("product");
        assertNotNull(productAspects);
        assertEquals(2, productAspects.size());

        Entity entity1 = freshDeserializer.getFactory().getEntity(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        Aspect product1 = productAspects.get(entity1);

        @SuppressWarnings("unchecked")
        java.util.List<String> tags = (java.util.List<String>) product1.readObj("tags");
        assertEquals(3, tags.size());
        assertEquals("electronics", tags.get(0));
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