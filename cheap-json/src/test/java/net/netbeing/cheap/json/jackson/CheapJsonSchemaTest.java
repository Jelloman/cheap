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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.json.jackson.serialize.CheapJacksonSerializer;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.AspectMapHierarchy;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.EntityTreeHierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;
import net.netbeing.cheap.model.Property;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Comprehensive tests for JSON schema validation.
 * Tests all JSON schemas against example files and programmatically constructed objects.
 */
class CheapJsonSchemaTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonSchemaFactory SCHEMA_FACTORY =
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder ->
        builder.schemaMappers(schemaMappers ->
            schemaMappers.mapPrefix("https://netbeing.net/cheap/", "classpath:json/")
        )
    );
    private static final Map<String, JsonSchema> SCHEMAS = new HashMap<>();

    private static final UUID CATALOG_ID = UUID.fromString("550e8400-e29b-41d4-a716-444444444444");

    @BeforeAll
    static void loadSchemas() throws IOException
    {
        SCHEMAS.put("catalog", loadSchema("catalog.schema.json"));
        SCHEMAS.put("aspectdef", loadSchema("aspectdef.schema.json"));
        SCHEMAS.put("propertydef", loadSchema("propertydef.schema.json"));
        SCHEMAS.put("hierarchy", loadSchema("hierarchy.schema.json"));
        SCHEMAS.put("aspect", loadSchema("aspect.schema.json"));
        SCHEMAS.put("property", loadSchema("property.schema.json"));
        SCHEMAS.put("catalogdef", loadSchema("catalogdef.schema.json"));
        SCHEMAS.put("hierarchydef", loadSchema("hierarchydef.schema.json"));
    }

    private static JsonSchema loadSchema(String schemaFilename) throws IOException
    {
        try (InputStream is = CheapJsonSchemaTest.class.getResourceAsStream("/json/" + schemaFilename)) {
            if (is == null) {
                throw new IOException("Schema not found: /json/" + schemaFilename);
            }
            return SCHEMA_FACTORY.getSchema(is);
        }
    }

    private String loadTestJson(String filename) throws IOException
    {
        try (InputStream is = getClass().getResourceAsStream("/jackson/" + filename)) {
            if (is == null) {
                throw new IOException("Resource not found: /jackson/" + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void validateJson(String json, String schemaName)
    {
        try {
            JsonNode jsonNode = MAPPER.readTree(json);
            JsonSchema schema = SCHEMAS.get(schemaName);
            assertNotNull(schema, "Schema not found: " + schemaName);

            Set<ValidationMessage> errors = schema.validate(jsonNode);
            if (!errors.isEmpty()) {
                fail("JSON validation failed for " + schemaName + " schema:\n" +
                    errors.stream()
                        .map(ValidationMessage::getMessage)
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("Unknown error"));
            }
        } catch (Exception e) {
            fail("Exception during validation: " + e.getMessage(), e);
        }
    }

    // ===== Test: Validate All Example JSON Files =====

    @Test
    void testValidateAllExampleJsonFiles() throws IOException
    {
        List<String> testFiles = List.of(
            "simple-catalog.json",
            "catalog-with-aspectdef.json",
            "full-catalog.json",
            "multivalued-properties.json",
            "entity-list.json",
            "entity-set.json",
            "entity-directory.json",
            "entity-tree.json",
            "aspect-map.json"
        );

        for (String filename : testFiles) {
            String json = loadTestJson(filename);
            validateJson(json, "catalog");
        }
    }

    // ===== Tests: Construct and Validate Catalogs with Various Hierarchies =====

    @Test
    void testCatalogWithAllHierarchyTypes()
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        // Create AspectDef for AspectMap testing
        PropertyDef nameProp = new PropertyDefBuilder()
            .setName("name")
            .setType(PropertyType.String)
            .setIsNullable(false)
            .build();
        PropertyDef ageProp = new PropertyDefBuilder()
            .setName("age")
            .setType(PropertyType.Integer)
            .build();

        Map<String, PropertyDef> personProps = ImmutableMap.of("name", nameProp, "age", ageProp);
        UUID aspectDefId = UUID.fromString("12348400-e24b-41d4-a716-446644440000");
        AspectDef personAspectDef = new ImmutableAspectDefImpl("person", aspectDefId, personProps);
        catalog.extend(personAspectDef);

        // Create entities
        UUID entityId1 = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID entityId2 = UUID.fromString("10000000-0000-0000-0000-000000000002");
        UUID entityId3 = UUID.fromString("10000000-0000-0000-0000-000000000003");
        Entity entity1 = new EntityImpl(entityId1);
        Entity entity2 = new EntityImpl(entityId2);
        Entity entity3 = new EntityImpl(entityId3);

        // EntityList Hierarchy
        EntityListHierarchyImpl entityList = new EntityListHierarchyImpl(catalog, "taskQueue");
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity3);
        entityList.add(entity1); // Duplicate allowed in list

        // EntitySet Hierarchy
        EntitySetHierarchyImpl entitySet = new EntitySetHierarchyImpl(catalog, "activeUsers");
        entitySet.add(entity1);
        entitySet.add(entity2);
        entitySet.add(entity3);

        // EntityDirectory Hierarchy
        EntityDirectoryHierarchyImpl entityDirectory = new EntityDirectoryHierarchyImpl(catalog, "userDirectory");
        entityDirectory.put("admin", entity1);
        entityDirectory.put("user1", entity2);
        entityDirectory.put("guest", entity3);

        // EntityTree Hierarchy
        Entity rootEntity = new EntityImpl(UUID.fromString("10000000-0000-0000-0000-000000000010"));
        EntityTreeHierarchyImpl entityTree = new EntityTreeHierarchyImpl(catalog, "fileSystem", rootEntity);
        EntityTreeHierarchy.Node root = entityTree.root();
        EntityTreeHierarchy.Node documentsNode = new EntityTreeHierarchyImpl.NodeImpl(entity2);
        EntityTreeHierarchy.Node imagesNode = new EntityTreeHierarchyImpl.NodeImpl(entity3);
        root.put("documents", documentsNode);
        root.put("images", imagesNode);

        // AspectMap Hierarchy - retrieve the auto-created hierarchy from extend()
        AspectMapHierarchy aspectMap = catalog.aspects(personAspectDef.name());
        AspectObjectMapImpl aspect1 = new AspectObjectMapImpl(entity1, personAspectDef);
        aspect1.put(new PropertyImpl(nameProp, "John Doe"));
        aspect1.put(new PropertyImpl(ageProp, 30L));
        aspectMap.put(entity1, aspect1);

        AspectObjectMapImpl aspect2 = new AspectObjectMapImpl(entity2, personAspectDef);
        aspect2.put(new PropertyImpl(nameProp, "Jane Smith"));
        aspect2.put(new PropertyImpl(ageProp, 25L));
        aspectMap.put(entity2, aspect2);

        String json = CheapJacksonSerializer.toJson(catalog, true);
        validateJson(json, "catalog");
    }

    @Test
    void testCatalogWithMultipleAspectMaps() throws IOException
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        // Create first AspectDef
        PropertyDef titleProp = new PropertyDefBuilder()
            .setName("title")
            .setType(PropertyType.String)
            .setIsNullable(false)
            .build();
        PropertyDef descProp = new PropertyDefBuilder()
            .setName("description")
            .setType(PropertyType.String)
            .build();

        Map<String, PropertyDef> documentProps = ImmutableMap.of("title", titleProp, "description", descProp);
        UUID docAspectDefId = UUID.fromString("73737400-e24b-41d4-a716-446644440000");
        AspectDef documentAspectDef = new ImmutableAspectDefImpl("document", docAspectDefId, documentProps);
        catalog.extend(documentAspectDef);

        // Create second AspectDef
        PropertyDef nameProp = new PropertyDefBuilder()
            .setName("name")
            .setType(PropertyType.String)
            .setIsNullable(false)
            .build();
        PropertyDef ageProp = new PropertyDefBuilder()
            .setName("age")
            .setType(PropertyType.Integer)
            .build();

        Map<String, PropertyDef> personProps = ImmutableMap.of("name", nameProp, "age", ageProp);
        UUID personAspectDefId = UUID.fromString("12348400-e24b-41d4-a716-446644440000");
        AspectDef personAspectDef = new ImmutableAspectDefImpl("person", personAspectDefId, personProps);
        catalog.extend(personAspectDef);

        // Create entities
        UUID entityId1 = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID entityId2 = UUID.fromString("10000000-0000-0000-0000-000000000002");
        UUID entityId3 = UUID.fromString("10000000-0000-0000-0000-000000000003");
        UUID entityId4 = UUID.fromString("10000000-0000-0000-0000-000000000004");
        Entity entity1 = new EntityImpl(entityId1);
        Entity entity2 = new EntityImpl(entityId2);
        Entity entity3 = new EntityImpl(entityId3);
        Entity entity4 = new EntityImpl(entityId4);

        // First AspectMap for person - retrieve the auto-created hierarchy
        AspectMapHierarchy personAspectMap = catalog.aspects(personAspectDef.name());
        AspectObjectMapImpl personAspect1 = new AspectObjectMapImpl(entity1, personAspectDef);
        personAspect1.put(new PropertyImpl(nameProp, "Alice"));
        personAspect1.put(new PropertyImpl(ageProp, 28L));
        personAspectMap.put(entity1, personAspect1);

        AspectObjectMapImpl personAspect2 = new AspectObjectMapImpl(entity2, personAspectDef);
        personAspect2.put(new PropertyImpl(nameProp, "Bob"));
        personAspect2.put(new PropertyImpl(ageProp, 35L));
        personAspectMap.put(entity2, personAspect2);

        // Second AspectMap for document - retrieve the auto-created hierarchy
        AspectMapHierarchy documentAspectMap = catalog.aspects(documentAspectDef.name());
        AspectObjectMapImpl docAspect1 = new AspectObjectMapImpl(entity3, documentAspectDef);
        docAspect1.put(new PropertyImpl(titleProp, "User Manual"));
        docAspect1.put(new PropertyImpl(descProp, "Complete user guide"));
        documentAspectMap.put(entity3, docAspect1);

        AspectObjectMapImpl docAspect2 = new AspectObjectMapImpl(entity4, documentAspectDef);
        docAspect2.put(new PropertyImpl(titleProp, "API Documentation"));
        docAspect2.put(new PropertyImpl(descProp, "REST API reference"));
        documentAspectMap.put(entity4, docAspect2);

        String json = CheapJacksonSerializer.toJson(catalog, true);
        validateJson(json, "catalog");
    }

    @Test
    void testCatalogWithMultivaluedProperties() throws IOException
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        // Create AspectDef with multivalued properties
        PropertyDef tagsProp = new PropertyDefBuilder()
            .setName("tags")
            .setType(PropertyType.String)
            .setIsMultivalued(true)
            .build();
        PropertyDef scoresProp = new PropertyDefBuilder()
            .setName("scores")
            .setType(PropertyType.Integer)
            .setIsMultivalued(true)
            .build();
        PropertyDef ratingsProp = new PropertyDefBuilder()
            .setName("ratings")
            .setType(PropertyType.Float)
            .setIsMultivalued(true)
            .build();
        PropertyDef titleProp = new PropertyDefBuilder()
            .setName("title")
            .setType(PropertyType.String)
            .setIsNullable(false)
            .build();

        Map<String, PropertyDef> productProps = ImmutableMap.of(
            "tags", tagsProp,
            "scores", scoresProp,
            "ratings", ratingsProp,
            "title", titleProp
        );
        UUID productAspectDefId = UUID.fromString("09348400-e24b-41d4-a716-446644440000");
        AspectDef productAspectDef = new ImmutableAspectDefImpl("product", productAspectDefId, productProps);
        catalog.extend(productAspectDef);

        // Create entities with multivalued properties
        UUID entityId1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID entityId2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        Entity entity1 = new EntityImpl(entityId1);
        Entity entity2 = new EntityImpl(entityId2);

        // Retrieve the auto-created hierarchy
        AspectMapHierarchy productMap = catalog.aspects(productAspectDef.name());

        AspectObjectMapImpl product1 = new AspectObjectMapImpl(entity1, productAspectDef);
        product1.put(new PropertyImpl(titleProp, "Smart Watch"));
        product1.put(new PropertyImpl(tagsProp, ImmutableList.of("electronics", "gadget", "popular")));
        product1.put(new PropertyImpl(scoresProp, ImmutableList.of(100L, 95L, 87L)));
        product1.put(new PropertyImpl(ratingsProp, ImmutableList.of(4.5, 4.8, 4.2)));
        productMap.put(entity1, product1);

        AspectObjectMapImpl product2 = new AspectObjectMapImpl(entity2, productAspectDef);
        product2.put(new PropertyImpl(titleProp, "Office Suite"));
        product2.put(new PropertyImpl(tagsProp, ImmutableList.of("software", "productivity")));
        product2.put(new PropertyImpl(scoresProp, ImmutableList.of(98L, 92L)));
        product2.put(new PropertyImpl(ratingsProp, ImmutableList.of(4.7, 4.9)));
        productMap.put(entity2, product2);

        String json = CheapJacksonSerializer.toJson(catalog, true);
        validateJson(json, "catalog");
    }

    @Test
    void testCatalogWithDeepEntityTree() throws IOException
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        // Create a deeper tree structure
        UUID rootId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID docId = UUID.fromString("10000000-0000-0000-0000-000000000002");
        UUID imgId = UUID.fromString("10000000-0000-0000-0000-000000000003");
        UUID reportId = UUID.fromString("10000000-0000-0000-0000-000000000004");
        UUID photoId = UUID.fromString("10000000-0000-0000-0000-000000000005");
        UUID videoId = UUID.fromString("10000000-0000-0000-0000-000000000006");

        Entity rootEntity = new EntityImpl(rootId);
        EntityTreeHierarchyImpl tree = new EntityTreeHierarchyImpl(catalog, "fileSystem", rootEntity);

        EntityTreeHierarchy.Node root = tree.root();
        EntityTreeHierarchy.Node documentsNode = new EntityTreeHierarchyImpl.NodeImpl(new EntityImpl(docId));
        EntityTreeHierarchy.Node imagesNode = new EntityTreeHierarchyImpl.NodeImpl(new EntityImpl(imgId));

        // Add children to documents
        EntityTreeHierarchy.Node reportsNode = new EntityTreeHierarchyImpl.NodeImpl(new EntityImpl(reportId));
        documentsNode.put("reports", reportsNode);

        // Add children to images
        EntityTreeHierarchy.Node photosNode = new EntityTreeHierarchyImpl.NodeImpl(new EntityImpl(photoId));
        EntityTreeHierarchy.Node videosNode = new EntityTreeHierarchyImpl.NodeImpl(new EntityImpl(videoId));
        imagesNode.put("photos", photosNode);
        imagesNode.put("videos", videosNode);

        root.put("documents", documentsNode);
        root.put("images", imagesNode);

        String json = CheapJacksonSerializer.toJson(catalog, true);
        validateJson(json, "catalog");
    }

    @Test
    void testCatalogWithLargeEntityList() throws IOException
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        EntityListHierarchyImpl entityList = new EntityListHierarchyImpl(catalog, "eventQueue");

        // Add many entities including duplicates
        for (int i = 1; i <= 20; i++) {
            UUID entityId = UUID.fromString(String.format("10000000-0000-0000-0000-%012d", i));
            Entity entity = new EntityImpl(entityId);
            entityList.add(entity);

            // Add some duplicates
            if (i % 5 == 0) {
                entityList.add(entity);
            }
        }

        String json = CheapJacksonSerializer.toJson(catalog, true);
        validateJson(json, "catalog");
    }

    // ===== Tests: Individual Schema Types =====

    @Test
    void testAspectDefSchema() throws IOException
    {
        PropertyDef prop1 = new PropertyDefBuilder()
            .setName("email")
            .setType(PropertyType.String)
            .setIsNullable(false)
            .build();
        PropertyDef prop2 = new PropertyDefBuilder()
            .setName("verified")
            .setType(PropertyType.Boolean)
            .build();

        Map<String, PropertyDef> props = ImmutableMap.of("email", prop1, "verified", prop2);
        UUID aspectDefId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        AspectDef aspectDef = new ImmutableAspectDefImpl("user", aspectDefId, props);

        String json = CheapJacksonSerializer.toJson(aspectDef, true);
        validateJson(json, "aspectdef");
    }

    @Test
    void testPropertyDefSchema() throws IOException
    {
        PropertyDef propDef = new PropertyDefBuilder()
            .setName("timestamp")
            .setType(PropertyType.DateTime)
            .setIsNullable(true)
            .setIsMultivalued(false)
            .build();

        String json = CheapJacksonSerializer.toJson(propDef, true);
        validateJson(json, "propertydef");
    }

    @Test
    void testPropertyDefSchemaWithMultivalued() throws IOException
    {
        PropertyDef propDef = new PropertyDefBuilder()
            .setName("phoneNumbers")
            .setType(PropertyType.String)
            .setIsNullable(true)
            .setIsMultivalued(true)
            .build();

        String json = CheapJacksonSerializer.toJson(propDef, true);
        validateJson(json, "propertydef");
    }

    @Test
    void testHierarchySchemaEntityList() throws IOException
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);
        EntityListHierarchyImpl hierarchy = new EntityListHierarchyImpl(catalog, "myList");

        UUID entityId1 = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
        UUID entityId2 = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
        hierarchy.add(new EntityImpl(entityId1));
        hierarchy.add(new EntityImpl(entityId2));

        String json = CheapJacksonSerializer.toJson(hierarchy, true);
        validateJson(json, "hierarchy");
    }

    @Test
    void testHierarchySchemaEntitySet() throws IOException
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);
        EntitySetHierarchyImpl hierarchy = new EntitySetHierarchyImpl(catalog, "mySet");

        UUID entityId1 = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
        UUID entityId2 = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
        hierarchy.add(new EntityImpl(entityId1));
        hierarchy.add(new EntityImpl(entityId2));

        String json = CheapJacksonSerializer.toJson(hierarchy, true);
        validateJson(json, "hierarchy");
    }

    @Test
    void testHierarchySchemaEntityDirectory() throws IOException
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);
        EntityDirectoryHierarchyImpl hierarchy = new EntityDirectoryHierarchyImpl(catalog, "myDirectory");

        UUID entityId1 = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
        UUID entityId2 = UUID.fromString("cccccccc-0000-0000-0000-000000000002");
        hierarchy.put("key1", new EntityImpl(entityId1));
        hierarchy.put("key2", new EntityImpl(entityId2));

        String json = CheapJacksonSerializer.toJson(hierarchy, true);
        validateJson(json, "hierarchy");
    }

    @Test
    void testHierarchySchemaEntityTree() throws IOException
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);
        UUID rootId = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
        EntityTreeHierarchyImpl hierarchy = new EntityTreeHierarchyImpl(catalog, "myTree", new EntityImpl(rootId));

        EntityTreeHierarchy.Node root = hierarchy.root();
        UUID childId = UUID.fromString("dddddddd-0000-0000-0000-000000000002");
        EntityTreeHierarchy.Node child = new EntityTreeHierarchyImpl.NodeImpl(new EntityImpl(childId));
        root.put("child", child);

        String json = CheapJacksonSerializer.toJson(hierarchy, true);
        validateJson(json, "hierarchy");
    }

    @Test
    void testHierarchySchemaAspectMap() throws IOException
    {
        CatalogImpl catalog = new CatalogImpl(CATALOG_ID);

        PropertyDef nameProp = new PropertyDefBuilder()
            .setName("name")
            .setType(PropertyType.String)
            .setIsNullable(false)
            .build();

        Map<String, PropertyDef> props = ImmutableMap.of("name", nameProp);
        UUID aspectDefId = UUID.fromString("eeeeeeee-0000-0000-0000-000000000000");
        AspectDef aspectDef = new ImmutableAspectDefImpl("testAspect", aspectDefId, props);
        catalog.extend(aspectDef);

        // Retrieve the auto-created hierarchy
        AspectMapHierarchy hierarchy = catalog.aspects(aspectDef.name());

        UUID entityId = UUID.fromString("ffffffff-0000-0000-0000-000000000001");
        Entity entity = new EntityImpl(entityId);
        AspectObjectMapImpl aspect = new AspectObjectMapImpl(entity, aspectDef);
        aspect.put(new PropertyImpl(nameProp, "Test Name"));
        hierarchy.put(entity, aspect);

        String json = CheapJacksonSerializer.toJson(hierarchy, true);
        validateJson(json, "hierarchy");
    }

    @Test
    void testAspectSchema() throws IOException
    {
        UUID aspectDefId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        UUID entityId = UUID.fromString("87654321-4321-4321-4321-210987654321");

        PropertyDef nameProp = new PropertyDefBuilder()
            .setName("name")
            .setType(PropertyType.String)
            .setIsNullable(false)
            .build();
        PropertyDef ageProp = new PropertyDefBuilder()
            .setName("age")
            .setType(PropertyType.Integer)
            .build();

        Map<String, PropertyDef> props = ImmutableMap.of("name", nameProp, "age", ageProp);
        AspectDef aspectDef = new ImmutableAspectDefImpl("person", aspectDefId, props);
        Entity entity = new EntityImpl(entityId);

        AspectObjectMapImpl aspect = new AspectObjectMapImpl(entity, aspectDef);
        aspect.put(new PropertyImpl(nameProp, "Alice"));
        aspect.put(new PropertyImpl(ageProp, 30L));

        String json = CheapJacksonSerializer.toJson(aspect, true);
        validateJson(json, "aspect");
    }

    @Test
    void testAspectSchemaWithMultivaluedProperties() throws IOException
    {
        UUID aspectDefId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        UUID entityId = UUID.fromString("87654321-4321-4321-4321-210987654321");

        PropertyDef tagsProp = new PropertyDefBuilder()
            .setName("tags")
            .setType(PropertyType.String)
            .setIsMultivalued(true)
            .build();
        PropertyDef scoresProp = new PropertyDefBuilder()
            .setName("scores")
            .setType(PropertyType.Integer)
            .setIsMultivalued(true)
            .build();

        Map<String, PropertyDef> props = ImmutableMap.of("tags", tagsProp, "scores", scoresProp);
        AspectDef aspectDef = new ImmutableAspectDefImpl("tagged", aspectDefId, props);
        Entity entity = new EntityImpl(entityId);

        AspectObjectMapImpl aspect = new AspectObjectMapImpl(entity, aspectDef);
        aspect.put(new PropertyImpl(tagsProp, ImmutableList.of("tag1", "tag2", "tag3")));
        aspect.put(new PropertyImpl(scoresProp, ImmutableList.of(100L, 200L, 300L)));

        String json = CheapJacksonSerializer.toJson(aspect, true);
        validateJson(json, "aspect");
    }

    @Test
    void testPropertySchema() throws IOException
    {
        PropertyDef propDef = new PropertyDefBuilder()
            .setName("username")
            .setType(PropertyType.String)
            .setIsNullable(false)
            .build();

        Property property = new PropertyImpl(propDef, "john_doe");

        String json = CheapJacksonSerializer.toJson(property, true);
        validateJson(json, "property");
    }

    @Test
    void testCatalogDefSchema() throws IOException
    {
        CatalogDefImpl catalogDef = new CatalogDefImpl();

        String json = CheapJacksonSerializer.toJson(catalogDef, true);
        validateJson(json, "catalogdef");
    }

    @Test
    void testHierarchyDefSchema() throws IOException
    {
        HierarchyDef hierarchyDef = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_LIST);

        String json = CheapJacksonSerializer.toJson(hierarchyDef, true);
        validateJson(json, "hierarchydef");
    }

    @Test
    void testHierarchyDefSchemaAllTypes() throws IOException
    {
        for (HierarchyType type : HierarchyType.values()) {
            HierarchyDef hierarchyDef = new HierarchyDefImpl("test_" + type.typeCode(), type);
            String json = CheapJacksonSerializer.toJson(hierarchyDef, true);
            validateJson(json, "hierarchydef");
        }
    }
}
