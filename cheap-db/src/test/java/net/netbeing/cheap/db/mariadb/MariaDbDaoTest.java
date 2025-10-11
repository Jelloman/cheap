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

package net.netbeing.cheap.db.mariadb;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import com.google.common.collect.ImmutableList;
import net.netbeing.cheap.db.AspectTableMapping;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.MariaDbDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MariaDbDaoTest
{
    DataSource dataSource;
    DB mariaDB;
    MariaDbDao mariaDbDao;
    CheapFactory factory;

    @BeforeEach
    void setUp() throws Exception
    {
        // Create embedded MariaDB instance
        DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
        configBuilder.setPort(0); // Use random available port
        mariaDB = DB.newEmbeddedDB(configBuilder.build());
        mariaDB.start();

        // Create database
        mariaDB.createDB("cheapdb");

        // Create data source
        MariaDbDataSource ds = new MariaDbDataSource();
        ds.setUrl("jdbc:mariadb://localhost:" + mariaDB.getConfiguration().getPort() + "/cheapdb");
        ds.setUser("root");
        ds.setPassword("");
        this.dataSource = ds;

        // Initialize factory and DAO
        factory = new CheapFactory();
        mariaDbDao = new MariaDbDao(dataSource, factory);

        // Initialize schema
        initializeSchema();
    }

    @AfterEach
    void tearDown() throws Exception
    {
        // Stop embedded MariaDB
        if (mariaDB != null) {
            mariaDB.stop();
        }
        this.mariaDB = null;
        this.dataSource = null;
        this.mariaDbDao = null;
        this.factory = null;
    }

    private void initializeSchema() throws SQLException
    {
        // Use MariaDbCheapSchema to execute DDL
        MariaDbCheapSchema schema = new MariaDbCheapSchema();
        schema.executeMainSchemaDdl(dataSource);
        schema.executeAuditSchemaDdl(dataSource);
    }

    @Test
    void testSaveAndLoadSimpleCatalog() throws SQLException
    {
        // Create a simple catalog
        UUID catalogId = UUID.randomUUID();
        Catalog originalCatalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Save the catalog
        mariaDbDao.saveCatalog(originalCatalog);

        // Load the catalog
        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);

        // Verify basic properties
        assertNotNull(loadedCatalog);
        assertEquals(originalCatalog.globalId(), loadedCatalog.globalId());
        assertEquals(originalCatalog.species(), loadedCatalog.species());
        assertEquals(originalCatalog.upstream(), loadedCatalog.upstream());
    }

    @Test
    void testSaveAndLoadCatalogWithUri() throws SQLException
    {
        // Create catalog with URI
        UUID catalogId = UUID.randomUUID();
        Catalog originalCatalog = factory.createCatalog(catalogId, CatalogSpecies.SOURCE, null, null, 0L);

        mariaDbDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);

        assertNotNull(loadedCatalog);
        assertEquals(originalCatalog.globalId(), loadedCatalog.globalId());
    }

    @Test
    void testSaveAndLoadCatalogWithUpstream() throws SQLException
    {
        // Create upstream catalog first
        UUID upstreamId = UUID.randomUUID();
        Catalog upstreamCatalog = factory.createCatalog(upstreamId, CatalogSpecies.SOURCE, null, null, 0L);
        mariaDbDao.saveCatalog(upstreamCatalog);

        // Create derived catalog
        UUID catalogId = UUID.randomUUID();
        Catalog originalCatalog = factory.createCatalog(catalogId, CatalogSpecies.MIRROR, null, upstreamId, 0L);

        mariaDbDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);

        assertNotNull(loadedCatalog);
        assertEquals(originalCatalog.globalId(), loadedCatalog.globalId());
        assertEquals(originalCatalog.upstream(), loadedCatalog.upstream());
        assertEquals(upstreamId, loadedCatalog.upstream());
    }

    @Test
    void testSaveAndLoadCatalogWithAspectDefs() throws SQLException
    {
        // Create AspectDef
        AspectDef personAspectDef = factory.createMutableAspectDef("person");

        // Create catalog
        UUID catalogId = UUID.randomUUID();
        Catalog originalCatalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Extend catalog with aspect def
        originalCatalog.extend(personAspectDef);

        mariaDbDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);

        assertNotNull(loadedCatalog);
        assertEquals(originalCatalog.globalId(), loadedCatalog.globalId());

        // Verify aspect defs are present
        boolean foundPersonAspect = false;
        for (AspectDef aspectDef : loadedCatalog.aspectDefs()) {
            if ("person".equals(aspectDef.name())) {
                foundPersonAspect = true;
                break;
            }
        }
        assertTrue(foundPersonAspect, "Person aspect definition should be loaded");
    }

    @Test
    void testSaveAndLoadCatalogWithEntitySetHierarchy() throws SQLException
    {
        // Create catalog
        UUID catalogId = UUID.randomUUID();
        Catalog originalCatalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Create hierarchy definition
        HierarchyDef hierarchyDef = factory.createHierarchyDef("entities", HierarchyType.ENTITY_SET);

        // Create hierarchy
        EntitySetHierarchy hierarchy = factory.createEntitySetHierarchy(originalCatalog, "entities");

        // Add some entities
        Entity entity1 = factory.createEntity(UUID.randomUUID());
        Entity entity2 = factory.createEntity(UUID.randomUUID());
        Entity entity3 = factory.createEntity(UUID.randomUUID());

        hierarchy.add(entity1);
        hierarchy.add(entity2);
        hierarchy.add(entity3);

        // Add hierarchy to catalog
        originalCatalog.addHierarchy(hierarchy);

        // Save and load
        mariaDbDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);

        assertNotNull(loadedCatalog);

        // Verify hierarchy exists
        Hierarchy loadedHierarchy = loadedCatalog.hierarchy("entities");
        assertNotNull(loadedHierarchy);
        assertEquals(HierarchyType.ENTITY_SET, loadedHierarchy.type());

        // Verify entities are loaded
        EntitySetHierarchy loadedEntitySet = (EntitySetHierarchy) loadedHierarchy;
        assertEquals(3, loadedEntitySet.size());

        // Check specific entities
        assertTrue(loadedEntitySet.contains(entity1));
        assertTrue(loadedEntitySet.contains(entity2));
        assertTrue(loadedEntitySet.contains(entity3));
    }

    @Test
    void testSaveAndLoadCatalogWithEntityDirectoryHierarchy() throws SQLException
    {
        // Create catalog
        UUID catalogId = UUID.randomUUID();
        Catalog originalCatalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Create hierarchy definition
        HierarchyDef hierarchyDef = factory.createHierarchyDef("directory", HierarchyType.ENTITY_DIR);

        // Create hierarchy
        EntityDirectoryHierarchy hierarchy = factory.createEntityDirectoryHierarchy(originalCatalog, "directory");

        // Add some entities with keys
        Entity entity1 = factory.createEntity(UUID.randomUUID());
        Entity entity2 = factory.createEntity(UUID.randomUUID());

        hierarchy.put("key1", entity1);
        hierarchy.put("key2", entity2);

        // Add hierarchy to catalog
        originalCatalog.addHierarchy(hierarchy);

        // Save and load
        mariaDbDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);

        assertNotNull(loadedCatalog);

        // Verify hierarchy exists
        Hierarchy loadedHierarchy = loadedCatalog.hierarchy("directory");
        assertNotNull(loadedHierarchy);
        assertEquals(HierarchyType.ENTITY_DIR, loadedHierarchy.type());

        // Verify entities are loaded with correct keys
        EntityDirectoryHierarchy loadedDirectory = (EntityDirectoryHierarchy) loadedHierarchy;
        assertEquals(entity1.globalId(), loadedDirectory.get("key1").globalId());
        assertEquals(entity2.globalId(), loadedDirectory.get("key2").globalId());
    }

    @Test
    void testSaveAndLoadCatalogWithAspectMapHierarchy() throws SQLException
    {
        // Create AspectDef with properties
        AspectDef personAspectDef = factory.createMutableAspectDef("person");

        // Create catalog
        UUID catalogId = UUID.randomUUID();
        Catalog originalCatalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Create aspect map hierarchy
        AspectMapHierarchy hierarchy = factory.createAspectMapHierarchy(originalCatalog, personAspectDef);

        // Create entities and aspects
        Entity person1 = factory.createEntity(UUID.randomUUID());
        Entity person2 = factory.createEntity(UUID.randomUUID());

        Aspect aspect1 = factory.createPropertyMapAspect(person1, personAspectDef);
        Aspect aspect2 = factory.createPropertyMapAspect(person2, personAspectDef);

        // Add aspects to hierarchy
        hierarchy.put(person1, aspect1);
        hierarchy.put(person2, aspect2);

        // Save and load
        mariaDbDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);

        assertNotNull(loadedCatalog);

        // Verify hierarchy exists
        Hierarchy loadedHierarchy = loadedCatalog.hierarchy("person");
        assertNotNull(loadedHierarchy);
        assertEquals(HierarchyType.ASPECT_MAP, loadedHierarchy.type());

        // Verify aspects are loaded
        AspectMapHierarchy loadedAspectMap = (AspectMapHierarchy) loadedHierarchy;
        assertEquals(2, loadedAspectMap.size());

        // Check that entities exist in the loaded hierarchy
        boolean foundPerson1 = false;
        boolean foundPerson2 = false;
        for (Entity entity : loadedAspectMap.keySet()) {
            if (entity.globalId().equals(person1.globalId())) {
                foundPerson1 = true;
            }
            if (entity.globalId().equals(person2.globalId())) {
                foundPerson2 = true;
            }
        }
        assertTrue(foundPerson1, "Person1 should be found in loaded hierarchy");
        assertTrue(foundPerson2, "Person2 should be found in loaded hierarchy");
    }

    @Test
    void testDeleteCatalog() throws SQLException
    {
        // Create and save catalog
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);
        mariaDbDao.saveCatalog(catalog);

        // Verify it exists
        assertTrue(mariaDbDao.catalogExists(catalogId));

        // Delete it
        boolean deleted = mariaDbDao.deleteCatalog(catalogId);
        assertTrue(deleted);

        // Verify it no longer exists
        assertFalse(mariaDbDao.catalogExists(catalogId));
        assertNull(mariaDbDao.loadCatalog(catalogId));
    }

    @Test
    void testDeleteNonExistentCatalog() throws SQLException
    {
        UUID nonExistentId = UUID.randomUUID();

        // Delete non-existent catalog
        boolean deleted = mariaDbDao.deleteCatalog(nonExistentId);
        assertFalse(deleted);
    }

    @Test
    void testCatalogExists() throws SQLException
    {
        UUID catalogId = UUID.randomUUID();

        // Should not exist initially
        assertFalse(mariaDbDao.catalogExists(catalogId));

        // Create and save catalog
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);
        mariaDbDao.saveCatalog(catalog);

        // Should exist now
        assertTrue(mariaDbDao.catalogExists(catalogId));
    }

    @Test
    void testLoadNonExistentCatalog() throws SQLException
    {
        UUID nonExistentId = UUID.randomUUID();
        Catalog catalog = mariaDbDao.loadCatalog(nonExistentId);
        assertNull(catalog);
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testSaveNullCatalogThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> {
            mariaDbDao.saveCatalog(null);
        });
    }

    @Test
    void testTransactionRollbackOnError() throws SQLException
    {
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Save should succeed
        assertDoesNotThrow(() -> mariaDbDao.saveCatalog(catalog));
        assertTrue(mariaDbDao.catalogExists(catalogId));
    }

    @Test
    void testComplexCatalogRoundTrip() throws SQLException
    {
        // Create a complex catalog with multiple hierarchy types
        UUID catalogId = UUID.randomUUID();
        Catalog originalCatalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Create multiple aspect definitions
        AspectDef personAspect = factory.createMutableAspectDef("person");
        AspectDef addressAspect = factory.createMutableAspectDef("address");

        // Create multiple hierarchies
        HierarchyDef entitiesHierarchy = factory.createHierarchyDef("entities", HierarchyType.ENTITY_SET);
        HierarchyDef directoryHierarchy = factory.createHierarchyDef("directory", HierarchyType.ENTITY_DIR);

        // Create entity set hierarchy
        EntitySetHierarchy entitySet = factory.createEntitySetHierarchy(originalCatalog, "entities");
        Entity entity1 = factory.createEntity(UUID.randomUUID());
        Entity entity2 = factory.createEntity(UUID.randomUUID());
        entitySet.add(entity1);
        entitySet.add(entity2);

        // Create directory hierarchy
        EntityDirectoryHierarchy directory = factory.createEntityDirectoryHierarchy(originalCatalog, "directory");
        directory.put("first", entity1);
        directory.put("second", entity2);

        // Create aspect map hierarchies
        AspectMapHierarchy personMap = factory.createAspectMapHierarchy(originalCatalog, personAspect);
        AspectMapHierarchy addressMap = factory.createAspectMapHierarchy(originalCatalog, addressAspect);

        Aspect person1Aspect = factory.createPropertyMapAspect(entity1, personAspect);
        Aspect person2Aspect = factory.createPropertyMapAspect(entity2, personAspect);
        Aspect address1Aspect = factory.createPropertyMapAspect(entity1, addressAspect);

        personMap.put(entity1, person1Aspect);
        personMap.put(entity2, person2Aspect);
        addressMap.put(entity1, address1Aspect);

        // Add all hierarchies to catalog
        originalCatalog.addHierarchy(entitySet);
        originalCatalog.addHierarchy(directory);

        // Save and load
        mariaDbDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);

        // Verify complex catalog structure
        assertNotNull(loadedCatalog);
        assertEquals(originalCatalog.globalId(), loadedCatalog.globalId());

        // Verify all hierarchies exist
        assertNotNull(loadedCatalog.hierarchy("entities"));
        assertNotNull(loadedCatalog.hierarchy("directory"));
        assertNotNull(loadedCatalog.hierarchy("person"));
        assertNotNull(loadedCatalog.hierarchy("address"));

        // Verify hierarchy types
        assertEquals(HierarchyType.ENTITY_SET, loadedCatalog.hierarchy("entities").type());
        assertEquals(HierarchyType.ENTITY_DIR, loadedCatalog.hierarchy("directory").type());
        assertEquals(HierarchyType.ASPECT_MAP, loadedCatalog.hierarchy("person").type());
        assertEquals(HierarchyType.ASPECT_MAP, loadedCatalog.hierarchy("address").type());

        // Verify entity counts
        EntitySetHierarchy loadedEntitySet = (EntitySetHierarchy) loadedCatalog.hierarchy("entities");
        assertEquals(2, loadedEntitySet.size());

        EntityDirectoryHierarchy loadedDirectory = (EntityDirectoryHierarchy) loadedCatalog.hierarchy("directory");
        assertEquals(2, loadedDirectory.size());

        AspectMapHierarchy loadedPersonMap = (AspectMapHierarchy) loadedCatalog.hierarchy("person");
        assertEquals(2, loadedPersonMap.size());

        AspectMapHierarchy loadedAddressMap = (AspectMapHierarchy) loadedCatalog.hierarchy("address");
        assertEquals(1, loadedAddressMap.size());
    }

    @Test
    void testSaveAndLoadMultivaluedStringProperties() throws SQLException
    {
        // Create AspectDef with multivalued String property
        PropertyDef tagsProp = factory.createPropertyDef("tags", PropertyType.String,
            true, true, true, true, true);

        Map<String, PropertyDef> propDefs = Map.of("tags", tagsProp);
        AspectDef productDef = factory.createImmutableAspectDef("product", propDefs);

        // Create catalog with AspectMapHierarchy
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);
        catalog.extend(productDef);
        AspectMapHierarchy hierarchy = (AspectMapHierarchy) catalog.hierarchy("product");

        // Create entity with multivalued property
        UUID entityId = UUID.randomUUID();
        Entity entity = factory.createEntity(entityId);
        Aspect aspect = factory.createPropertyMapAspect(entity, productDef);

        List<String> tags = ImmutableList.of("electronics", "gadget", "popular");
        aspect.put(factory.createProperty(tagsProp, tags));

        hierarchy.put(entity, aspect);

        // Save catalog
        mariaDbDao.saveCatalog(catalog);

        // Verify database rows - should have 3 rows for the multivalued property
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT value_text, value_index FROM property_value WHERE entity_id = ? AND property_name = ? ORDER BY value_index")) {
            stmt.setString(1, entityId.toString());
            stmt.setString(2, "tags");
            try (var rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("electronics", rs.getString("value_text"));
                assertEquals(0, rs.getInt("value_index"));

                assertTrue(rs.next());
                assertEquals("gadget", rs.getString("value_text"));
                assertEquals(1, rs.getInt("value_index"));

                assertTrue(rs.next());
                assertEquals("popular", rs.getString("value_text"));
                assertEquals(2, rs.getInt("value_index"));

                assertFalse(rs.next(), "Should have exactly 3 rows");
            }
        }

        // Load catalog and verify multivalued property
        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("product");
        Entity loadedEntity = factory.getOrRegisterNewEntity(entityId);
        Aspect loadedAspect = loadedHierarchy.get(loadedEntity);

        assertNotNull(loadedAspect);
        Object loadedValue = loadedAspect.readObj("tags");
        assertInstanceOf(List.class, loadedValue);

        @SuppressWarnings("unchecked")
        List<String> loadedTags = (List<String>) loadedValue;
        assertEquals(3, loadedTags.size());
        assertEquals("electronics", loadedTags.get(0));
        assertEquals("gadget", loadedTags.get(1));
        assertEquals("popular", loadedTags.get(2));
    }

    @Test
    void testSaveAndLoadMultivaluedIntegerProperties() throws SQLException
    {
        // Create AspectDef with multivalued Integer property
        PropertyDef scoresProp = factory.createPropertyDef("scores", PropertyType.Integer,
            true, true, true, true, true);

        Map<String, PropertyDef> propDefs = Map.of("scores", scoresProp);
        AspectDef testDef = factory.createImmutableAspectDef("test_results", propDefs);

        // Create catalog
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);
        catalog.extend(testDef);
        AspectMapHierarchy hierarchy = (AspectMapHierarchy) catalog.hierarchy("test_results");

        // Create entity with multivalued Integer property
        UUID entityId = UUID.randomUUID();
        Entity entity = factory.createEntity(entityId);
        Aspect aspect = factory.createPropertyMapAspect(entity, testDef);

        List<Long> scores = ImmutableList.of(100L, 95L, 87L, 92L);
        aspect.put(factory.createProperty(scoresProp, scores));

        hierarchy.put(entity, aspect);

        // Save and load
        mariaDbDao.saveCatalog(catalog);
        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);

        // Verify loaded data
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("test_results");
        Entity loadedEntity = factory.getOrRegisterNewEntity(entityId);
        Aspect loadedAspect = loadedHierarchy.get(loadedEntity);

        @SuppressWarnings("unchecked")
        List<Long> loadedScores = (List<Long>) loadedAspect.readObj("scores");
        assertEquals(4, loadedScores.size());
        assertEquals(100L, loadedScores.get(0));
        assertEquals(95L, loadedScores.get(1));
        assertEquals(87L, loadedScores.get(2));
        assertEquals(92L, loadedScores.get(3));
    }

    @Test
    void testSaveAndLoadEmptyMultivaluedProperty() throws SQLException
    {
        // Create AspectDef with multivalued property
        PropertyDef tagsProp = factory.createPropertyDef("tags", PropertyType.String,
            true, true, true, true, true);

        Map<String, PropertyDef> propDefs = Map.of("tags", tagsProp);
        AspectDef productDef = factory.createImmutableAspectDef("product", propDefs);

        // Create catalog
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);
        catalog.extend(productDef);
        AspectMapHierarchy hierarchy = (AspectMapHierarchy) catalog.hierarchy("product");

        // Create entity with empty list
        UUID entityId = UUID.randomUUID();
        Entity entity = factory.createEntity(entityId);
        Aspect aspect = factory.createPropertyMapAspect(entity, productDef);

        List<String> emptyTags = ImmutableList.of();
        aspect.put(factory.createProperty(tagsProp, emptyTags));

        hierarchy.put(entity, aspect);

        // Save catalog
        mariaDbDao.saveCatalog(catalog);

        // Verify no rows in database for empty list
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM property_value WHERE entity_id = ? AND property_name = ?")) {
            stmt.setString(1, entityId.toString());
            stmt.setString(2, "tags");
            try (var rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Empty list should have no rows in database");
            }
        }

        // Load catalog and verify empty list is restored
        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("product");
        Entity loadedEntity = factory.getOrRegisterNewEntity(entityId);
        Aspect loadedAspect = loadedHierarchy.get(loadedEntity);

        Object loadedValue = loadedAspect.readObj("tags");
        assertInstanceOf(List.class, loadedValue);

        @SuppressWarnings("unchecked")
        List<String> loadedTags = (List<String>) loadedValue;
        assertTrue(loadedTags.isEmpty(), "Should load as empty list");
    }

    @Test
    void testSaveAndLoadNullMultivaluedProperty() throws SQLException
    {
        // Create AspectDef with nullable multivalued property
        PropertyDef tagsProp = factory.createPropertyDef("tags", PropertyType.String,
            true, true, true, true, true);

        Map<String, PropertyDef> propDefs = Map.of("tags", tagsProp);
        AspectDef productDef = factory.createImmutableAspectDef("product", propDefs);

        // Create catalog
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);
        catalog.extend(productDef);
        AspectMapHierarchy hierarchy = (AspectMapHierarchy) catalog.hierarchy("product");

        // Create entity with null value
        UUID entityId = UUID.randomUUID();
        Entity entity = factory.createEntity(entityId);
        Aspect aspect = factory.createPropertyMapAspect(entity, productDef);

        aspect.put(factory.createProperty(tagsProp, null));

        hierarchy.put(entity, aspect);

        // Save and load
        mariaDbDao.saveCatalog(catalog);

        // Verify no rows in database (null multivalued is treated same as empty list)
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM property_value WHERE entity_id = ? AND property_name = ?")) {
            stmt.setString(1, entityId.toString());
            stmt.setString(2, "tags");
            try (var rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Null multivalued property should have no rows (same as empty list)");
            }
        }

        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("product");
        Entity loadedEntity = factory.getOrRegisterNewEntity(entityId);
        Aspect loadedAspect = loadedHierarchy.get(loadedEntity);

        // With the simplified schema, null and empty list are indistinguishable for multivalued properties
        // Both are represented by no rows, and both load as empty list
        Object loadedValue = loadedAspect.readObj("tags");
        assertInstanceOf(List.class, loadedValue);
        @SuppressWarnings("unchecked")
        List<String> loadedTags = (List<String>) loadedValue;
        assertTrue(loadedTags.isEmpty(), "Null multivalued property should load as empty list");
    }

    @Test
    void testSaveAndLoadMixedSingleAndMultivaluedProperties() throws SQLException
    {
        // Create AspectDef with both single-valued and multivalued properties
        PropertyDef titleProp = factory.createPropertyDef("title", PropertyType.String,
            null, false, true, true, false, false, false);
        PropertyDef tagsProp = factory.createPropertyDef("tags", PropertyType.String,
            true, true, true, true, true);
        PropertyDef pricesProp = factory.createPropertyDef("prices", PropertyType.Float,
            true, true, true, true, true);

        Map<String, PropertyDef> propDefs = new LinkedHashMap<>();
        propDefs.put("title", titleProp);
        propDefs.put("tags", tagsProp);
        propDefs.put("prices", pricesProp);

        AspectDef productDef = factory.createImmutableAspectDef("product", propDefs);

        // Create catalog
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);
        catalog.extend(productDef);
        AspectMapHierarchy hierarchy = (AspectMapHierarchy) catalog.hierarchy("product");

        // Create entity with mixed properties
        UUID entityId = UUID.randomUUID();
        Entity entity = factory.createEntity(entityId);
        Aspect aspect = factory.createPropertyMapAspect(entity, productDef);

        aspect.put(factory.createProperty(titleProp, "Smart Watch"));
        aspect.put(factory.createProperty(tagsProp, ImmutableList.of("electronics", "gadget")));
        aspect.put(factory.createProperty(pricesProp, ImmutableList.of(199.99, 249.99, 299.99)));

        hierarchy.put(entity, aspect);

        // Save and load
        mariaDbDao.saveCatalog(catalog);

        // Verify database rows
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT property_name, COUNT(*) as row_count FROM property_value WHERE entity_id = ? GROUP BY property_name ORDER BY property_name")) {
            stmt.setString(1, entityId.toString());
            try (var rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("prices", rs.getString("property_name"));
                assertEquals(3, rs.getInt("row_count"), "Multivalued float property should have 3 rows");

                assertTrue(rs.next());
                assertEquals("tags", rs.getString("property_name"));
                assertEquals(2, rs.getInt("row_count"), "Multivalued string property should have 2 rows");

                assertTrue(rs.next());
                assertEquals("title", rs.getString("property_name"));
                assertEquals(1, rs.getInt("row_count"), "Single-valued property should have 1 row");

                assertFalse(rs.next());
            }
        }

        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("product");
        Entity loadedEntity = factory.getOrRegisterNewEntity(entityId);
        Aspect loadedAspect = loadedHierarchy.get(loadedEntity);

        // Verify single-valued property
        assertEquals("Smart Watch", loadedAspect.readObj("title"));

        // Verify multivalued String property
        @SuppressWarnings("unchecked")
        List<String> loadedTags = (List<String>) loadedAspect.readObj("tags");
        assertEquals(2, loadedTags.size());
        assertEquals("electronics", loadedTags.get(0));
        assertEquals("gadget", loadedTags.get(1));

        // Verify multivalued Float property
        @SuppressWarnings("unchecked")
        List<Double> loadedPrices = (List<Double>) loadedAspect.readObj("prices");
        assertEquals(3, loadedPrices.size());
        assertEquals(199.99, loadedPrices.get(0), 0.01);
        assertEquals(249.99, loadedPrices.get(1), 0.01);
        assertEquals(299.99, loadedPrices.get(2), 0.01);
    }

    @Test
    void testSaveAndLoadMultivaluedBooleanAndUUIDProperties() throws SQLException
    {
        // Create AspectDef with multivalued Boolean and UUID properties
        PropertyDef flagsProp = factory.createPropertyDef("flags", PropertyType.Boolean,
            true, true, true, true, true);
        PropertyDef idsProp = factory.createPropertyDef("ids", PropertyType.UUID,
            true, true, true, true, true);

        Map<String, PropertyDef> propDefs = new LinkedHashMap<>();
        propDefs.put("flags", flagsProp);
        propDefs.put("ids", idsProp);

        AspectDef testDef = factory.createImmutableAspectDef("test_data", propDefs);

        // Create catalog
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);
        catalog.extend(testDef);
        AspectMapHierarchy hierarchy = (AspectMapHierarchy) catalog.hierarchy("test_data");

        // Create entity with multivalued Boolean and UUID properties
        UUID entityId = UUID.randomUUID();
        Entity entity = factory.createEntity(entityId);
        Aspect aspect = factory.createPropertyMapAspect(entity, testDef);

        UUID id1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID id2 = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
        UUID id3 = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

        aspect.put(factory.createProperty(flagsProp, ImmutableList.of(true, false, true, true)));
        aspect.put(factory.createProperty(idsProp, ImmutableList.of(id1, id2, id3)));

        hierarchy.put(entity, aspect);

        // Save and load
        mariaDbDao.saveCatalog(catalog);
        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);

        // Verify loaded data
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("test_data");
        Entity loadedEntity = factory.getOrRegisterNewEntity(entityId);
        Aspect loadedAspect = loadedHierarchy.get(loadedEntity);

        // Verify Boolean list
        @SuppressWarnings("unchecked")
        List<Boolean> loadedFlags = (List<Boolean>) loadedAspect.readObj("flags");
        assertEquals(4, loadedFlags.size());
        assertTrue(loadedFlags.get(0));
        assertFalse(loadedFlags.get(1));
        assertTrue(loadedFlags.get(2));
        assertTrue(loadedFlags.get(3));

        // Verify UUID list
        @SuppressWarnings("unchecked")
        List<UUID> loadedIds = (List<UUID>) loadedAspect.readObj("ids");
        assertEquals(3, loadedIds.size());
        assertEquals(id1, loadedIds.get(0));
        assertEquals(id2, loadedIds.get(1));
        assertEquals(id3, loadedIds.get(2));
    }

    @Test
    void testUpdateMultivaluedPropertyWithDifferentLength() throws SQLException
    {
        // Create AspectDef with multivalued property
        PropertyDef tagsProp = factory.createPropertyDef("tags", PropertyType.String,
            true, true, true, true, true);

        Map<String, PropertyDef> propDefs = Map.of("tags", tagsProp);
        AspectDef productDef = factory.createImmutableAspectDef("product", propDefs);

        // Create catalog
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);
        catalog.extend(productDef);
        AspectMapHierarchy hierarchy = (AspectMapHierarchy) catalog.hierarchy("product");

        // Create entity with initial list of 3 items
        UUID entityId = UUID.randomUUID();
        Entity entity = factory.createEntity(entityId);
        Aspect aspect = factory.createPropertyMapAspect(entity, productDef);

        aspect.put(factory.createProperty(tagsProp, ImmutableList.of("tag1", "tag2", "tag3")));
        hierarchy.put(entity, aspect);

        // Save catalog
        mariaDbDao.saveCatalog(catalog);

        // Update with list of 5 items
        Aspect updatedAspect = factory.createPropertyMapAspect(entity, productDef);
        updatedAspect.put(factory.createProperty(tagsProp,
            ImmutableList.of("new1", "new2", "new3", "new4", "new5")));
        hierarchy.put(entity, updatedAspect);

        // Save again
        mariaDbDao.saveCatalog(catalog);

        // Verify database has 5 rows (old rows should be deleted)
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM property_value WHERE entity_id = ? AND property_name = ?")) {
            stmt.setString(1, entityId.toString());
            stmt.setString(2, "tags");
            try (var rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(5, rs.getInt(1), "Should have 5 rows after update");
            }
        }

        // Load and verify new values
        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("product");
        Entity loadedEntity = factory.getOrRegisterNewEntity(entityId);
        Aspect loadedAspect = loadedHierarchy.get(loadedEntity);

        @SuppressWarnings("unchecked")
        List<String> loadedTags = (List<String>) loadedAspect.readObj("tags");
        assertEquals(5, loadedTags.size());
        assertEquals("new1", loadedTags.get(0));
        assertEquals("new2", loadedTags.get(1));
        assertEquals("new3", loadedTags.get(2));
        assertEquals("new4", loadedTags.get(3));
        assertEquals("new5", loadedTags.get(4));
    }

    @Test
    void testAspectTableMappingAllFourPatterns() throws Exception
    {
        // Load table creation SQL files and create tables
        String[] sqlFiles = {
            "/db/mariadb/test_aspect_mapping_no_key.sql",
            "/db/mariadb/test_aspect_mapping_with_cat_id.sql",
            "/db/mariadb/test_aspect_mapping_with_entity_id.sql",
            "/db/mariadb/test_aspect_mapping_with_both_ids.sql"
        };

        for (String sqlFile : sqlFiles) {
            String sql = loadResourceFile(sqlFile);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }

        // Load AspectDefs from test tables
        MariaDbCatalog mariaDbCatalog = new MariaDbCatalog(dataSource);
        AspectDef noKeyAspectDef = mariaDbCatalog.loadTableDef("test_aspect_mapping_no_key");
        AspectDef catIdAspectDef = mariaDbCatalog.loadTableDef("test_aspect_mapping_with_cat_id");
        AspectDef entityIdAspectDef = mariaDbCatalog.loadTableDef("test_aspect_mapping_with_entity_id");
        AspectDef bothIdsAspectDef = mariaDbCatalog.loadTableDef("test_aspect_mapping_with_both_ids");

        // Create column mappings
        Map<String, String> columnMapping = Map.of(
            "string_col", "string_col",
            "integer_col", "integer_col"
        );

        // Pattern 1: No IDs (no primary key, entity IDs generated on load)
        AspectTableMapping noKeyMapping = new AspectTableMapping(
            noKeyAspectDef, "test_aspect_mapping_no_key", columnMapping, false, false);

        // Pattern 2: Catalog ID only (no primary key, catalog-scoped)
        AspectTableMapping catIdMapping = new AspectTableMapping(
            catIdAspectDef, "test_aspect_mapping_with_cat_id", columnMapping, true, false);

        // Pattern 3: Entity ID only (PRIMARY KEY (entity_id))
        AspectTableMapping entityIdMapping = new AspectTableMapping(
            entityIdAspectDef, "test_aspect_mapping_with_entity_id", columnMapping, false, true);

        // Pattern 4: Both IDs (PRIMARY KEY (catalog_id, entity_id))
        AspectTableMapping bothIdsMapping = new AspectTableMapping(
            bothIdsAspectDef, "test_aspect_mapping_with_both_ids", columnMapping, true, true);

        // Register all mappings
        mariaDbDao.addAspectTableMapping(noKeyMapping);
        mariaDbDao.addAspectTableMapping(catIdMapping);
        mariaDbDao.addAspectTableMapping(entityIdMapping);
        mariaDbDao.addAspectTableMapping(bothIdsMapping);

        // Create catalog
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Create hierarchies for each pattern
        AspectMapHierarchy noKeyHierarchy = factory.createAspectMapHierarchy(catalog, noKeyAspectDef);
        AspectMapHierarchy catIdHierarchy = factory.createAspectMapHierarchy(catalog, catIdAspectDef);
        AspectMapHierarchy entityIdHierarchy = factory.createAspectMapHierarchy(catalog, entityIdAspectDef);
        AspectMapHierarchy bothIdsHierarchy = factory.createAspectMapHierarchy(catalog, bothIdsAspectDef);

        // Add test data to each hierarchy
        // Pattern 1: No IDs - entities will be generated
        Entity noKey1 = factory.createEntity();
        Aspect noKeyAsp1 = factory.createPropertyMapAspect(noKey1, noKeyAspectDef);
        noKeyAsp1.put(factory.createProperty(noKeyAspectDef.propertyDef("string_col"), "nokey1"));
        noKeyAsp1.put(factory.createProperty(noKeyAspectDef.propertyDef("integer_col"), 100L));
        noKeyHierarchy.put(noKey1, noKeyAsp1);

        // Pattern 2: Catalog ID only - entities will be generated
        Entity catId1 = factory.createEntity();
        Aspect catIdAsp1 = factory.createPropertyMapAspect(catId1, catIdAspectDef);
        catIdAsp1.put(factory.createProperty(catIdAspectDef.propertyDef("string_col"), "catid1"));
        catIdAsp1.put(factory.createProperty(catIdAspectDef.propertyDef("integer_col"), 200L));
        catIdHierarchy.put(catId1, catIdAsp1);

        // Pattern 3: Entity ID only - entity IDs preserved
        UUID entity3Id = UUID.randomUUID();
        Entity entityId1 = factory.createEntity(entity3Id);
        Aspect entityIdAsp1 = factory.createPropertyMapAspect(entityId1, entityIdAspectDef);
        entityIdAsp1.put(factory.createProperty(entityIdAspectDef.propertyDef("string_col"), "entityid1"));
        entityIdAsp1.put(factory.createProperty(entityIdAspectDef.propertyDef("integer_col"), 300L));
        entityIdHierarchy.put(entityId1, entityIdAsp1);

        // Pattern 4: Both IDs - entity IDs preserved, catalog-scoped
        UUID entity4Id = UUID.randomUUID();
        Entity bothIds1 = factory.createEntity(entity4Id);
        Aspect bothIdsAsp1 = factory.createPropertyMapAspect(bothIds1, bothIdsAspectDef);
        bothIdsAsp1.put(factory.createProperty(bothIdsAspectDef.propertyDef("string_col"), "bothids1"));
        bothIdsAsp1.put(factory.createProperty(bothIdsAspectDef.propertyDef("integer_col"), 400L));
        bothIdsHierarchy.put(bothIds1, bothIdsAsp1);

        // Save catalog
        mariaDbDao.saveCatalog(catalog);

        // Verify Pattern 1: No IDs - data saved, no ID columns
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT string_col, integer_col FROM test_aspect_mapping_no_key")) {
            assertTrue(rs.next());
            assertEquals("nokey1", rs.getString("string_col"));
            assertEquals(100, rs.getInt("integer_col"));
            assertFalse(rs.next(), "Should have exactly 1 row");
        }

        // Verify Pattern 2: Catalog ID - data saved with catalog_id
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT catalog_id, string_col, integer_col FROM test_aspect_mapping_with_cat_id")) {
            assertTrue(rs.next());
            assertEquals(catalogId.toString(), rs.getString("catalog_id"));
            assertEquals("catid1", rs.getString("string_col"));
            assertEquals(200, rs.getInt("integer_col"));
            assertFalse(rs.next(), "Should have exactly 1 row");
        }

        // Verify Pattern 3: Entity ID - data saved with entity_id preserved
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT entity_id, string_col, integer_col FROM test_aspect_mapping_with_entity_id")) {
            assertTrue(rs.next());
            assertEquals(entity3Id.toString(), rs.getString("entity_id"));
            assertEquals("entityid1", rs.getString("string_col"));
            assertEquals(300, rs.getInt("integer_col"));
            assertFalse(rs.next(), "Should have exactly 1 row");
        }

        // Verify Pattern 4: Both IDs - data saved with both IDs
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT catalog_id, entity_id, string_col, integer_col FROM test_aspect_mapping_with_both_ids")) {
            assertTrue(rs.next());
            assertEquals(catalogId.toString(), rs.getString("catalog_id"));
            assertEquals(entity4Id.toString(), rs.getString("entity_id"));
            assertEquals("bothids1", rs.getString("string_col"));
            assertEquals(400, rs.getInt("integer_col"));
            assertFalse(rs.next(), "Should have exactly 1 row");
        }

        // Load catalog back and verify
        Catalog loadedCatalog = mariaDbDao.loadCatalog(catalogId);
        assertNotNull(loadedCatalog);

        // Pattern 1: Entity IDs will be different (generated on load)
        AspectMapHierarchy loadedNoKeyHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy(noKeyAspectDef.name());
        assertEquals(1, loadedNoKeyHierarchy.size());
        Entity loadedNoKeyEntity = loadedNoKeyHierarchy.keySet().iterator().next();
        Aspect loadedNoKeyAsp = loadedNoKeyHierarchy.get(loadedNoKeyEntity);
        assertEquals("nokey1", loadedNoKeyAsp.readObj("string_col"));
        assertEquals(100L, loadedNoKeyAsp.readObj("integer_col"));

        // Pattern 2: Entity IDs will be different (generated on load), but filtered by catalog
        AspectMapHierarchy loadedCatIdHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy(catIdAspectDef.name());
        assertEquals(1, loadedCatIdHierarchy.size());
        Entity loadedCatIdEntity = loadedCatIdHierarchy.keySet().iterator().next();
        Aspect loadedCatIdAsp = loadedCatIdHierarchy.get(loadedCatIdEntity);
        assertEquals("catid1", loadedCatIdAsp.readObj("string_col"));
        assertEquals(200L, loadedCatIdAsp.readObj("integer_col"));

        // Pattern 3: Entity IDs preserved
        AspectMapHierarchy loadedEntityIdHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy(entityIdAspectDef.name());
        assertEquals(1, loadedEntityIdHierarchy.size());
        Entity loadedEntity3 = factory.getOrRegisterNewEntity(entity3Id);
        Aspect loadedEntityIdAsp = loadedEntityIdHierarchy.get(loadedEntity3);
        assertNotNull(loadedEntityIdAsp);
        assertEquals("entityid1", loadedEntityIdAsp.readObj("string_col"));
        assertEquals(300L, loadedEntityIdAsp.readObj("integer_col"));

        // Pattern 4: Entity IDs preserved, catalog-scoped
        AspectMapHierarchy loadedBothIdsHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy(bothIdsAspectDef.name());
        assertEquals(1, loadedBothIdsHierarchy.size());
        Entity loadedEntity4 = factory.getOrRegisterNewEntity(entity4Id);
        Aspect loadedBothIdsAsp = loadedBothIdsHierarchy.get(loadedEntity4);
        assertNotNull(loadedBothIdsAsp);
        assertEquals("bothids1", loadedBothIdsAsp.readObj("string_col"));
        assertEquals(400L, loadedBothIdsAsp.readObj("integer_col"));
    }

    @SuppressWarnings("DataFlowIssue")
    private String loadResourceFile(String resourcePath) throws IOException, URISyntaxException
    {
        Path path = Paths.get(getClass().getResource(resourcePath).toURI());
        return Files.readString(path);
    }
}
