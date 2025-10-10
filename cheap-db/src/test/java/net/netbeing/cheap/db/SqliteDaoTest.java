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

package net.netbeing.cheap.db;

import com.google.common.collect.ImmutableList;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SqliteDaoTest
{
    DataSource dataSource;
    Connection connection;
    SqliteDao sqliteDao;
    CheapFactory factory;

    @BeforeEach
    void setUp() throws SQLException
    {
        // Create an in-memory SQLite database with shared cache
        // Using shared cache mode allows multiple connections to access the same in-memory database
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:file::memory:?cache=shared");
        this.dataSource = ds;
        // Keep connection open to prevent in-memory database deletion
        this.connection = ds.getConnection();

        // Initialize factory and DAO
        factory = new CheapFactory();
        sqliteDao = new SqliteDao(dataSource, factory);

        // Initialize schema
        initializeSchema();
    }

    @AfterEach
    void tearDown() throws SQLException
    {
        // Close the connection (which will delete the in-memory database)
        if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
        }
        this.connection = null;
        this.dataSource = null;
        this.sqliteDao = null;
        this.factory = null;
    }

    private void initializeSchema() throws SQLException
    {
        // Use SqliteDao methods to execute DDL
        sqliteDao.executeMainSchemaDdl(connection);
        sqliteDao.executeAuditSchemaDdl(connection);
    }

    @Test
    void testSaveAndLoadSimpleCatalog() throws SQLException
    {
        // Create a simple catalog
        UUID catalogId = UUID.randomUUID();
        Catalog originalCatalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Save the catalog
        sqliteDao.saveCatalog(originalCatalog);

        // Load the catalog
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);

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

        sqliteDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);

        assertNotNull(loadedCatalog);
        assertEquals(originalCatalog.globalId(), loadedCatalog.globalId());
    }

    @Test
    void testSaveAndLoadCatalogWithUpstream() throws SQLException
    {
        // Create upstream catalog first
        UUID upstreamId = UUID.randomUUID();
        Catalog upstreamCatalog = factory.createCatalog(upstreamId, CatalogSpecies.SOURCE, null, null, 0L);
        sqliteDao.saveCatalog(upstreamCatalog);

        // Create derived catalog
        UUID catalogId = UUID.randomUUID();
        Catalog originalCatalog = factory.createCatalog(catalogId, CatalogSpecies.MIRROR, null, upstreamId, 0L);

        sqliteDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);

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

        sqliteDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);

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
        sqliteDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);

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
        sqliteDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);

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
        sqliteDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);

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
        sqliteDao.saveCatalog(catalog);

        // Verify it exists
        assertTrue(sqliteDao.catalogExists(catalogId));

        // Delete it
        boolean deleted = sqliteDao.deleteCatalog(catalogId);
        assertTrue(deleted);

        // Verify it no longer exists
        assertFalse(sqliteDao.catalogExists(catalogId));
        assertNull(sqliteDao.loadCatalog(catalogId));
    }

    @Test
    void testDeleteNonExistentCatalog() throws SQLException
    {
        UUID nonExistentId = UUID.randomUUID();

        // Delete non-existent catalog
        boolean deleted = sqliteDao.deleteCatalog(nonExistentId);
        assertFalse(deleted);
    }

    @Test
    void testCatalogExists() throws SQLException
    {
        UUID catalogId = UUID.randomUUID();

        // Should not exist initially
        assertFalse(sqliteDao.catalogExists(catalogId));

        // Create and save catalog
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);
        sqliteDao.saveCatalog(catalog);

        // Should exist now
        assertTrue(sqliteDao.catalogExists(catalogId));
    }

    @Test
    void testLoadNonExistentCatalog() throws SQLException
    {
        UUID nonExistentId = UUID.randomUUID();
        Catalog catalog = sqliteDao.loadCatalog(nonExistentId);
        assertNull(catalog);
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testSaveNullCatalogThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> {
            sqliteDao.saveCatalog(null);
        });
    }

    @Test
    void testTransactionRollbackOnError() throws SQLException
    {
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Save should succeed
        assertDoesNotThrow(() -> sqliteDao.saveCatalog(catalog));
        assertTrue(sqliteDao.catalogExists(catalogId));
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
        sqliteDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);

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

        java.util.Map<String, PropertyDef> propDefs = java.util.Map.of("tags", tagsProp);
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

        java.util.List<String> tags = ImmutableList.of("electronics", "gadget", "popular");
        aspect.put(factory.createProperty(tagsProp, tags));

        hierarchy.put(entity, aspect);

        // Save catalog
        sqliteDao.saveCatalog(catalog);

        // Verify database rows - should have 3 rows for the multivalued property
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(
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
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("product");
        Entity loadedEntity = factory.getOrRegisterNewEntity(entityId);
        Aspect loadedAspect = loadedHierarchy.get(loadedEntity);

        assertNotNull(loadedAspect);
        Object loadedValue = loadedAspect.readObj("tags");
        assertInstanceOf(java.util.List.class, loadedValue);

        @SuppressWarnings("unchecked")
        java.util.List<String> loadedTags = (java.util.List<String>) loadedValue;
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

        java.util.Map<String, PropertyDef> propDefs = java.util.Map.of("scores", scoresProp);
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

        java.util.List<Long> scores = ImmutableList.of(100L, 95L, 87L, 92L);
        aspect.put(factory.createProperty(scoresProp, scores));

        hierarchy.put(entity, aspect);

        // Save and load
        sqliteDao.saveCatalog(catalog);
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);

        // Verify loaded data
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("test_results");
        Entity loadedEntity = factory.getOrRegisterNewEntity(entityId);
        Aspect loadedAspect = loadedHierarchy.get(loadedEntity);

        @SuppressWarnings("unchecked")
        java.util.List<Long> loadedScores = (java.util.List<Long>) loadedAspect.readObj("scores");
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

        java.util.Map<String, PropertyDef> propDefs = java.util.Map.of("tags", tagsProp);
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

        java.util.List<String> emptyTags = ImmutableList.of();
        aspect.put(factory.createProperty(tagsProp, emptyTags));

        hierarchy.put(entity, aspect);

        // Save catalog
        sqliteDao.saveCatalog(catalog);

        // Verify no rows in database for empty list
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM property_value WHERE entity_id = ? AND property_name = ?")) {
            stmt.setString(1, entityId.toString());
            stmt.setString(2, "tags");
            try (var rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Empty list should have no rows in database");
            }
        }

        // Load catalog and verify empty list is restored
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("product");
        Entity loadedEntity = factory.getOrRegisterNewEntity(entityId);
        Aspect loadedAspect = loadedHierarchy.get(loadedEntity);

        Object loadedValue = loadedAspect.readObj("tags");
        assertInstanceOf(java.util.List.class, loadedValue);

        @SuppressWarnings("unchecked")
        java.util.List<String> loadedTags = (java.util.List<String>) loadedValue;
        assertTrue(loadedTags.isEmpty(), "Should load as empty list");
    }

    @Test
    void testSaveAndLoadNullMultivaluedProperty() throws SQLException
    {
        // Create AspectDef with nullable multivalued property
        PropertyDef tagsProp = factory.createPropertyDef("tags", PropertyType.String,
            true, true, true, true, true);

        java.util.Map<String, PropertyDef> propDefs = java.util.Map.of("tags", tagsProp);
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
        sqliteDao.saveCatalog(catalog);

        // Verify no rows in database (null multivalued is treated same as empty list)
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM property_value WHERE entity_id = ? AND property_name = ?")) {
            stmt.setString(1, entityId.toString());
            stmt.setString(2, "tags");
            try (var rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Null multivalued property should have no rows (same as empty list)");
            }
        }

        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("product");
        Entity loadedEntity = factory.getOrRegisterNewEntity(entityId);
        Aspect loadedAspect = loadedHierarchy.get(loadedEntity);

        // With the simplified schema, null and empty list are indistinguishable for multivalued properties
        // Both are represented by no rows, and both load as empty list
        Object loadedValue = loadedAspect.readObj("tags");
        assertInstanceOf(java.util.List.class, loadedValue);
        @SuppressWarnings("unchecked")
        java.util.List<String> loadedTags = (java.util.List<String>) loadedValue;
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

        java.util.Map<String, PropertyDef> propDefs = new java.util.LinkedHashMap<>();
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
        sqliteDao.saveCatalog(catalog);

        // Verify database rows
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(
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

        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("product");
        Entity loadedEntity = factory.getOrRegisterNewEntity(entityId);
        Aspect loadedAspect = loadedHierarchy.get(loadedEntity);

        // Verify single-valued property
        assertEquals("Smart Watch", loadedAspect.readObj("title"));

        // Verify multivalued String property
        @SuppressWarnings("unchecked")
        java.util.List<String> loadedTags = (java.util.List<String>) loadedAspect.readObj("tags");
        assertEquals(2, loadedTags.size());
        assertEquals("electronics", loadedTags.get(0));
        assertEquals("gadget", loadedTags.get(1));

        // Verify multivalued Float property
        @SuppressWarnings("unchecked")
        java.util.List<Double> loadedPrices = (java.util.List<Double>) loadedAspect.readObj("prices");
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

        java.util.Map<String, PropertyDef> propDefs = new java.util.LinkedHashMap<>();
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
        sqliteDao.saveCatalog(catalog);
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);

        // Verify loaded data
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("test_data");
        Entity loadedEntity = factory.getOrRegisterNewEntity(entityId);
        Aspect loadedAspect = loadedHierarchy.get(loadedEntity);

        // Verify Boolean list
        @SuppressWarnings("unchecked")
        java.util.List<Boolean> loadedFlags = (java.util.List<Boolean>) loadedAspect.readObj("flags");
        assertEquals(4, loadedFlags.size());
        assertTrue(loadedFlags.get(0));
        assertFalse(loadedFlags.get(1));
        assertTrue(loadedFlags.get(2));
        assertTrue(loadedFlags.get(3));

        // Verify UUID list
        @SuppressWarnings("unchecked")
        java.util.List<UUID> loadedIds = (java.util.List<UUID>) loadedAspect.readObj("ids");
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

        java.util.Map<String, PropertyDef> propDefs = java.util.Map.of("tags", tagsProp);
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
        sqliteDao.saveCatalog(catalog);

        // Update with list of 5 items
        Aspect updatedAspect = factory.createPropertyMapAspect(entity, productDef);
        updatedAspect.put(factory.createProperty(tagsProp,
            ImmutableList.of("new1", "new2", "new3", "new4", "new5")));
        hierarchy.put(entity, updatedAspect);

        // Save again
        sqliteDao.saveCatalog(catalog);

        // Verify database has 5 rows (old rows should be deleted)
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM property_value WHERE entity_id = ? AND property_name = ?")) {
            stmt.setString(1, entityId.toString());
            stmt.setString(2, "tags");
            try (var rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(5, rs.getInt(1), "Should have 5 rows after update");
            }
        }

        // Load and verify new values
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("product");
        Entity loadedEntity = factory.getOrRegisterNewEntity(entityId);
        Aspect loadedAspect = loadedHierarchy.get(loadedEntity);

        @SuppressWarnings("unchecked")
        java.util.List<String> loadedTags = (java.util.List<String>) loadedAspect.readObj("tags");
        assertEquals(5, loadedTags.size());
        assertEquals("new1", loadedTags.get(0));
        assertEquals("new2", loadedTags.get(1));
        assertEquals("new3", loadedTags.get(2));
        assertEquals("new4", loadedTags.get(3));
        assertEquals("new5", loadedTags.get(4));
    }
}
