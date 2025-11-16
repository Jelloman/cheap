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

package net.netbeing.cheap.json.jackson;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.impl.basic.AspectObjectMapImpl;
import net.netbeing.cheap.impl.basic.CatalogImpl;
import net.netbeing.cheap.impl.basic.EntityImpl;
import net.netbeing.cheap.impl.basic.HierarchyDefImpl;
import net.netbeing.cheap.impl.basic.ImmutableAspectDefImpl;
import net.netbeing.cheap.impl.basic.PropertyDefBuilder;
import net.netbeing.cheap.json.jackson.serialize.CheapJacksonSerializer;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.impl.basic.CheapFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for CheapJacksonSerializer using actual implementation classes.
 * Tests the Jackson-based serializer against the raw serializer to ensure
 * equivalent functionality.
 */
@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "ConstantValue"})
class CheapJacksonSerializerTest
{
    static final CheapFactory factory = new CheapFactory();
    
    private static final UUID CATALOG_ID = UUID.fromString("550e8400-e29b-41d4-a716-444444444444");

    private static final String WRITE_OUTPUT_PATH = null;

    private static CatalogImpl createTestCatalog()
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        // Create a simple AspectDef
        PropertyDef nameProp = new PropertyDefBuilder().setName("name").setType(PropertyType.String)
            .setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true)
            .setIsNullable(false).setIsMultivalued(false).build();
        PropertyDef ageProp = new PropertyDefBuilder().setName("age").setType(PropertyType.Integer)
            .setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true).setIsNullable(true)
            .setIsMultivalued(false).build();
        Map<String, PropertyDef> personProps = ImmutableMap.of("name", nameProp, "age", ageProp);
        UUID aspectDefId = UUID.fromString("82758400-e24b-41d4-a726-446644440000");
        AspectDef personAspectDef = new ImmutableAspectDefImpl("person", aspectDefId, personProps);

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
        if (WRITE_OUTPUT_PATH != null)
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "simple-catalog.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        String expectedJson = loadExpectedJson("simple-catalog.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testSimpleCatalogToJsonCompact() throws IOException
    {
        CatalogImpl catalog = setupSimpleCatalog();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "simple-catalog-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("simple-catalog-compact.json");

        assertEquals(expectedJson, jacksonResult);
    }
    
    @Test
    void testCatalogWithAspectDefToJson() throws IOException
    {
        CatalogImpl catalog = createTestCatalog();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "catalog-with-aspectdef.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("catalog-with-aspectdef.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithAspectDefToJsonCompact() throws IOException
    {
        CatalogImpl catalog = createTestCatalog();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "catalog-with-aspectdef-compact.json"), jacksonResult,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
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
        EntityDirectoryHierarchy entityDirectory = catalog.createEntityDirectory(entityDirDef.name(), 0L);
        entityDirectory.put("admin", entity1);
        entityDirectory.put("user1", entity2);

        return catalog;
    }

    @Test
    void testCatalogWithEntityDirectoryHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityDirectoryHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "entity-directory.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("entity-directory.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithEntityDirectoryHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityDirectoryHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "entity-directory-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
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
        EntityListHierarchy entityList = catalog.createEntityList(entityListDef.name(), 0L); // NOSONAR
        entityList.add(entity1);
        entityList.add(entity2);

        return catalog;
    }

    @Test
    void testCatalogWithEntityListHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityListHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "entity-list.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("entity-list.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithEntityListHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityListHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "entity-list-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
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
        EntitySetHierarchy entitySet = catalog.createEntitySet(entitySetDef.name(), 0L); // NOSONAR
        entitySet.add(entity1);
        entitySet.add(entity2);

        return catalog;
    }

    @Test
    void testCatalogWithEntitySetHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntitySetHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "entity-set.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("entity-set.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithEntitySetHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntitySetHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "entity-set-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
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
        EntityTreeHierarchy entityTree = catalog.createEntityTree(entityTreeDef.name(), 0L);
        entityTree.root().setValue(parentEntity);
        EntityTreeHierarchy.Node childNode = factory.createTreeNode(childEntity, entityTree.root());
        entityTree.root().put("documents", childNode);

        return catalog;
    }

    @Test
    void testCatalogWithEntityTreeHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityTreeHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "entity-tree.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("entity-tree.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithEntityTreeHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithEntityTreeHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "entity-tree-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("entity-tree-compact.json");

        assertEquals(expectedJson, jacksonResult);
    }
    
    private CatalogImpl setupCatalogWithAspectMapHierarchy()
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        UUID entityId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Entity entity = new EntityImpl(entityId);

        PropertyDef nameProp = new PropertyDefBuilder().setName("name").setType(PropertyType.String)
            .setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true)
            .setIsNullable(false).setIsMultivalued(false).build();
        Map<String, PropertyDef> personProps = ImmutableMap.of("name", nameProp);
        UUID aspectDefId = UUID.fromString("12348400-e24b-41d4-a716-446644440000");
        AspectDef personAspectDef = new ImmutableAspectDefImpl("person", aspectDefId, personProps);
        AspectMapHierarchy personAspects = catalog.extend(personAspectDef);

        AspectObjectMapImpl personAspect = new AspectObjectMapImpl(entity, personAspectDef);
        personAspect.write("name", "John Doe");
        personAspects.put(entity, personAspect);

        return catalog;
    }

    @Test
    void testCatalogWithAspectMapHierarchy() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithAspectMapHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "aspect-map.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("aspect-map.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithAspectMapHierarchyCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithAspectMapHierarchy();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "aspect-map-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("aspect-map-compact.json");

        assertEquals(expectedJson, jacksonResult);
    }
    
    private CatalogImpl setupFullCatalogWithAllHierarchyTypes()
    {
        // Create a custom CatalogDef that doesn't include all hierarchies
        // This simulates a case where one hierarchy will have its def embedded in JSON
        PropertyDef nameProp = new PropertyDefBuilder().setName("name").setType(PropertyType.String)
            .setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true)
            .setIsNullable(false).setIsMultivalued(false).build();
        PropertyDef ageProp = new PropertyDefBuilder().setName("age").setType(PropertyType.Integer)
            .setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true).setIsNullable(true)
            .setIsMultivalued(false).build();
        Map<String, PropertyDef> personProps = ImmutableMap.of("age", ageProp, "name", nameProp);
        UUID aspectDefId1 = UUID.fromString("12348400-e24b-41d4-a716-446644440000");
        AspectDef personAspectDef = new ImmutableAspectDefImpl("person", aspectDefId1, personProps);

        PropertyDef titleProp = new PropertyDefBuilder().setName("title").setType(PropertyType.String)
            .setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true)
            .setIsNullable(false).setIsMultivalued(false).build();
        PropertyDef descProp = new PropertyDefBuilder().setName("description").setType(PropertyType.String)
            .setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true).setIsNullable(true)
            .setIsMultivalued(false).build();
        Map<String, PropertyDef> docProps = ImmutableMap.of("title", titleProp, "description", descProp);
        UUID aspectDefId2 = UUID.fromString("73737400-e24b-41d4-a716-446644440000");
        AspectDef docAspectDef = new ImmutableAspectDefImpl("document", aspectDefId2, docProps);

        // Create HierarchyDefs for most (but not all) hierarchies that will be added
        HierarchyDef entityDirDef = new HierarchyDefImpl("userDirectory", HierarchyType.ENTITY_DIR);
        HierarchyDef entityListDef = new HierarchyDefImpl("taskQueue", HierarchyType.ENTITY_LIST);
        HierarchyDef entitySetDef = new HierarchyDefImpl("activeUsers", HierarchyType.ENTITY_SET);
        // Intentionally omitting entityTreeDef from CatalogDef to test embedded def scenario

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
        AspectMapHierarchy personAspects = catalog.createAspectMap(personAspectDef, 0L);
        AspectMapHierarchy docAspects = catalog.createAspectMap(docAspectDef, 0L);

        // 1. EntityDirectoryHierarchy (using def from CatalogDef)
        EntityDirectoryHierarchy entityDirectory = catalog.createEntityDirectory(entityDirDef.name(), 0L);
        entityDirectory.put("admin", entity1);
        entityDirectory.put("user1", entity2);
        entityDirectory.put("guest", entity3);
        catalog.addHierarchy(entityDirectory);

        // 2. EntityListHierarchy (using def from CatalogDef)
        EntityListHierarchy entityList = catalog.createEntityList(entityListDef.name(), 0L);
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity3);
        entityList.add(entity1); // Allow duplicates in list
        catalog.addHierarchy(entityList);

        // 3. EntitySetHierarchy (using def from CatalogDef)
        EntitySetHierarchy entitySet = catalog.createEntitySet(entitySetDef.name(), 0L);
        entitySet.add(entity1);
        entitySet.add(entity2);
        entitySet.add(entity4);
        catalog.addHierarchy(entitySet);

        // 4. EntityTreeHierarchy (NOT in CatalogDef - will have embedded def in JSON)
        HierarchyDef entityTreeDef = new HierarchyDefImpl("fileSystem", HierarchyType.ENTITY_TREE);
        EntityTreeHierarchy entityTree = catalog.createEntityTree(entityTreeDef.name(), 0L);
        entityTree.root().setValue(entity1); // root entity
        EntityTreeHierarchy.Node childNode1 = factory.createTreeNode(entity2, entityTree.root());
        EntityTreeHierarchy.Node childNode2 = factory.createTreeNode(entity3, entityTree.root());
        entityTree.root().put("documents", childNode1);
        entityTree.root().put("images", childNode2);
        // Add nested child
        EntityTreeHierarchy.Node subChild = factory.createTreeNode(entity4, childNode1);
        childNode1.put("reports", subChild);
        catalog.addHierarchy(entityTree);

        // 5. First AspectMapHierarchy (person aspects)
        AspectObjectMapImpl personAspect1 = new AspectObjectMapImpl(entity1, personAspectDef);
        personAspect1.write("age", 30);
        personAspect1.write("name", "John Doe");
        AspectObjectMapImpl personAspect2 = new AspectObjectMapImpl(entity2, personAspectDef);
        personAspect2.write("age", 25);
        personAspect2.write("name", "Jane Smith");
        personAspects.put(entity1, personAspect1);
        personAspects.put(entity2, personAspect2);

        // 6. Second AspectMapHierarchy (document aspects)
        AspectObjectMapImpl docAspect1 = new AspectObjectMapImpl(entity3, docAspectDef);
        docAspect1.write("title", "User Manual");
        docAspect1.write("description", "Complete user guide");
        AspectObjectMapImpl docAspect2 = new AspectObjectMapImpl(entity4, docAspectDef);
        docAspect2.write("title", "API Documentation");
        docAspect2.write("description", "REST API reference");
        docAspects.put(entity3, docAspect1);
        docAspects.put(entity4, docAspect2);

        return catalog;
    }

    @Test
    void testFullCatalogWithAllHierarchyTypes() throws IOException
    {
        CatalogImpl catalog = setupFullCatalogWithAllHierarchyTypes();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "full-catalog.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("full-catalog.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testFullCatalogWithAllHierarchyTypesCompact() throws IOException
    {
        CatalogImpl catalog = setupFullCatalogWithAllHierarchyTypes();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "full-catalog-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("full-catalog-compact.json");

        assertEquals(expectedJson, jacksonResult);
    }

    private CatalogImpl setupCatalogWithMultivaluedProperties()
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        UUID entityId1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID entityId2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Entity entity1 = new EntityImpl(entityId1);
        Entity entity2 = new EntityImpl(entityId2);

        // Create PropertyDefs with multivalued properties
        PropertyDef tagsProp = new PropertyDefBuilder().setName("tags").setType(PropertyType.String).setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true).setIsNullable(true).setIsMultivalued(true).build();
        PropertyDef scoresProp = new PropertyDefBuilder().setName("scores").setType(PropertyType.Integer).setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true).setIsNullable(true).setIsMultivalued(true).build();
        PropertyDef ratingsProp = new PropertyDefBuilder().setName("ratings").setType(PropertyType.Float).setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true).setIsNullable(true).setIsMultivalued(true).build();
        PropertyDef titleProp = new PropertyDefBuilder().setName("title").setType(PropertyType.String).setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true).setIsNullable(false).setIsMultivalued(false).build();

        Map<String, PropertyDef> productProps = ImmutableMap.of(
            "tags", tagsProp,
            "scores", scoresProp,
            "ratings", ratingsProp,
            "title", titleProp
        );
        UUID aspectDefId = UUID.fromString("09348400-e24b-41d4-a716-446644440000");
        AspectDef productAspectDef = new ImmutableAspectDefImpl("product", aspectDefId, productProps);
        AspectMapHierarchy productAspects = catalog.extend(productAspectDef);

        // Create first product with multivalued properties
        AspectObjectMapImpl product1 = new AspectObjectMapImpl(entity1, productAspectDef);
        product1.write("tags", List.of("electronics", "gadget", "popular"));
        product1.write("scores", List.of(100L, 95L, 87L));
        product1.write("ratings", List.of(4.5, 4.8, 4.2));
        product1.write("title", "Smart Watch");
        productAspects.put(entity1, product1);

        // Create second product with multivalued properties
        AspectObjectMapImpl product2 = new AspectObjectMapImpl(entity2, productAspectDef);
        product2.write("tags", List.of("software", "productivity"));
        product2.write("scores", List.of(98L, 92L));
        product2.write("ratings", List.of(4.7, 4.9));
        product2.write("title", "Office Suite");
        productAspects.put(entity2, product2);

        return catalog;
    }

    @Test
    void testCatalogWithMultivaluedProperties() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithMultivaluedProperties();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, true);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "multivalued-properties.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("multivalued-properties.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogWithMultivaluedPropertiesCompact() throws IOException
    {
        CatalogImpl catalog = setupCatalogWithMultivaluedProperties();

        String jacksonResult = CheapJacksonSerializer.toJson(catalog, false);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "multivalued-properties-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("multivalued-properties-compact.json");

        assertEquals(expectedJson, jacksonResult);
    }

    private CatalogDef setupCatalogDef()
    {
        // Create AspectDefs
        PropertyDef nameProp = new PropertyDefBuilder().setName("name").setType(PropertyType.String).setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true).setIsNullable(false).setIsMultivalued(false).build();
        PropertyDef ageProp = new PropertyDefBuilder().setName("age").setType(PropertyType.Integer).setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true).setIsNullable(true).setIsMultivalued(false).build();
        Map<String, PropertyDef> personProps = ImmutableMap.of("name", nameProp, "age", ageProp);
        UUID aspectDefId1 = UUID.fromString("82758400-e24b-41d4-a726-446644440000");
        AspectDef personAspectDef = new ImmutableAspectDefImpl("person", aspectDefId1, personProps);

        PropertyDef titleProp = new PropertyDefBuilder().setName("title").setType(PropertyType.String).setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true).setIsNullable(false).setIsMultivalued(false).build();
        PropertyDef descProp = new PropertyDefBuilder().setName("description").setType(PropertyType.String).setDefaultValue(null).setHasDefaultValue(false).setIsReadable(true).setIsWritable(true).setIsNullable(true).setIsMultivalued(false).build();
        Map<String, PropertyDef> docProps = ImmutableMap.of("title", titleProp, "description", descProp);
        UUID aspectDefId2 = UUID.fromString("73737400-e24b-41d4-a716-446644440000");
        AspectDef docAspectDef = new ImmutableAspectDefImpl("document", aspectDefId2, docProps);

        // Create HierarchyDefs
        HierarchyDef entityDirDef = new HierarchyDefImpl("userDirectory", HierarchyType.ENTITY_DIR);
        HierarchyDef entityListDef = new HierarchyDefImpl("taskQueue", HierarchyType.ENTITY_LIST);
        HierarchyDef entitySetDef = new HierarchyDefImpl("activeUsers", HierarchyType.ENTITY_SET);

        List<AspectDef> aspectDefs = List.of(personAspectDef, docAspectDef);
        List<HierarchyDef> hierarchyDefs = List.of(entityDirDef, entityListDef, entitySetDef);

        return factory.createCatalogDef(hierarchyDefs, aspectDefs);
    }

    @Test
    void testCatalogDefToJson() throws IOException
    {
        CatalogDef catalogDef = setupCatalogDef();

        String jacksonResult = CheapJacksonSerializer.toJson(catalogDef, true);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "catalog-def.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("catalog-def.json");

        assertEquals(expectedJson, jacksonResult);
    }

    @Test
    void testCatalogDefToJsonCompact() throws IOException
    {
        CatalogDef catalogDef = setupCatalogDef();

        String jacksonResult = CheapJacksonSerializer.toJson(catalogDef, false);
        if (WRITE_OUTPUT_PATH != null) {
            Files.writeString(Paths.get(WRITE_OUTPUT_PATH, "catalog-def-compact.json"), jacksonResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        String expectedJson = loadExpectedJson("catalog-def-compact.json");

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