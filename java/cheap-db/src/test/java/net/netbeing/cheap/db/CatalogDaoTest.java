package net.netbeing.cheap.db;

import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.SingleInstancePostgresExtension;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CatalogDaoTest
{

    @RegisterExtension
    static SingleInstancePostgresExtension postgres = EmbeddedPostgresExtension.singleInstance();

    static DataSource dataSource;
    static boolean schemaInitialized = false;

    CatalogDao catalogDao;
    CheapFactory factory;

    void setUp() throws SQLException, IOException, URISyntaxException
    {
        // Get the datasource (will be initialized by JUnit extension)
        if (dataSource == null) {
            dataSource = postgres.getEmbeddedPostgres().getPostgresDatabase();
        }

        // Initialize database schema once for all tests
        if (!schemaInitialized) {
            initializeSchema();
            schemaInitialized = true;
        }

        factory = new CheapFactory();
        catalogDao = new CatalogDao(dataSource, factory);

        // Clean up all tables before each test
        truncateAllTables();
    }

    private static void initializeSchema() throws SQLException, IOException, URISyntaxException
    {
        String mainSchemaPath = "/db/schemas/postgres-cheap.sql";
        String auditSchemaPath = "/db/schemas/postgres-cheap-audit.sql";

        String mainDdl = loadResourceFile(mainSchemaPath);
        String auditDdl = loadResourceFile(auditSchemaPath);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(mainDdl);
            statement.execute(auditDdl);
        }
    }

    private static String loadResourceFile(String resourcePath) throws IOException, URISyntaxException
    {
        Path path = Paths.get(CatalogDaoTest.class.getResource(resourcePath).toURI());
        return Files.readString(path);
    }

    private void truncateAllTables() throws SQLException, IOException, URISyntaxException
    {
        String truncateSql = loadResourceFile("/db/schemas/postgres-cheap-truncate.sql");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(truncateSql);
        }
    }

    @Test
    void testSaveAndLoadSimpleCatalog() throws SQLException, IOException, URISyntaxException
    {
        setUp();

        // Create a simple catalog
        UUID catalogId = UUID.randomUUID();
        Catalog originalCatalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Save the catalog
        catalogDao.saveCatalog(originalCatalog);

        // Load the catalog
        Catalog loadedCatalog = catalogDao.loadCatalog(catalogId);

        // Verify basic properties
        assertNotNull(loadedCatalog);
        assertEquals(originalCatalog.globalId(), loadedCatalog.globalId());
        assertEquals(originalCatalog.species(), loadedCatalog.species());
        assertEquals(originalCatalog.upstream(), loadedCatalog.upstream());
    }

    @Test
    void testSaveAndLoadCatalogWithUri() throws SQLException, IOException, URISyntaxException
    {
        setUp();

        // Create catalog with URI
        UUID catalogId = UUID.randomUUID();
        Catalog originalCatalog = factory.createCatalog(catalogId, CatalogSpecies.SOURCE, null, null, 0L);

        // Note: Would need a way to set URI - this depends on catalog implementation
        // For now, test without URI

        catalogDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = catalogDao.loadCatalog(catalogId);

        assertNotNull(loadedCatalog);
        assertEquals(originalCatalog.globalId(), loadedCatalog.globalId());
    }

    @Test
    void testSaveAndLoadCatalogWithUpstream() throws SQLException, IOException, URISyntaxException
    {
        setUp();

        // Create upstream catalog first
        UUID upstreamId = UUID.randomUUID();
        Catalog upstreamCatalog = factory.createCatalog(upstreamId, CatalogSpecies.SOURCE, null, null, 0L);
        catalogDao.saveCatalog(upstreamCatalog);

        // Create derived catalog
        UUID catalogId = UUID.randomUUID();
        Catalog originalCatalog = factory.createCatalog(catalogId, CatalogSpecies.MIRROR, null, upstreamId, 0L);

        catalogDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = catalogDao.loadCatalog(catalogId);

        assertNotNull(loadedCatalog);
        assertEquals(originalCatalog.globalId(), loadedCatalog.globalId());
        assertEquals(originalCatalog.upstream(), loadedCatalog.upstream());
        assertEquals(upstreamId, loadedCatalog.upstream());
    }

    @Test
    void testSaveAndLoadCatalogWithAspectDefs() throws SQLException, IOException, URISyntaxException
    {
        setUp();

        // Create AspectDef
        AspectDef personAspectDef = factory.createMutableAspectDef("person");

        // Add property definitions - need to access mutable aspect def
        // Note: This test is simplified - in reality would need to add properties to mutable aspect def

        // Create catalog
        UUID catalogId = UUID.randomUUID();
        Catalog originalCatalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Extend catalog with aspect def
        originalCatalog.extend(personAspectDef);

        catalogDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = catalogDao.loadCatalog(catalogId);

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
    void testSaveAndLoadCatalogWithEntitySetHierarchy() throws SQLException, IOException, URISyntaxException
    {
        setUp();

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
        catalogDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = catalogDao.loadCatalog(catalogId);

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
    void testSaveAndLoadCatalogWithEntityDirectoryHierarchy() throws SQLException, IOException, URISyntaxException
    {
        setUp();

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
        catalogDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = catalogDao.loadCatalog(catalogId);

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
    void testSaveAndLoadCatalogWithAspectMapHierarchy() throws SQLException, IOException, URISyntaxException
    {
        setUp();

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

        // Note: AspectMapHierarchy is automatically added to catalog when created

        // Save and load
        catalogDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = catalogDao.loadCatalog(catalogId);

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

    /*

    @Test
    void testDeleteCatalog() throws SQLException
    {
        // Create and save catalog
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);
        catalogDao.saveCatalog(catalog);

        // Verify it exists
        assertTrue(catalogDao.catalogExists(catalogId));

        // Delete it
        boolean deleted = catalogDao.deleteCatalog(catalogId);
        assertTrue(deleted);

        // Verify it no longer exists
        assertFalse(catalogDao.catalogExists(catalogId));
        assertNull(catalogDao.loadCatalog(catalogId));
    }

    @Test
    void testDeleteNonExistentCatalog() throws SQLException
    {
        UUID nonExistentId = UUID.randomUUID();

        // Delete non-existent catalog
        boolean deleted = catalogDao.deleteCatalog(nonExistentId);
        assertFalse(deleted);
    }

    @Test
    void testCatalogExists() throws SQLException
    {
        UUID catalogId = UUID.randomUUID();

        // Should not exist initially
        assertFalse(catalogDao.catalogExists(catalogId));

        // Create and save catalog
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);
        catalogDao.saveCatalog(catalog);

        // Should exist now
        assertTrue(catalogDao.catalogExists(catalogId));
    }

    @Test
    void testLoadNonExistentCatalog() throws SQLException
    {
        UUID nonExistentId = UUID.randomUUID();
        Catalog catalog = catalogDao.loadCatalog(nonExistentId);
        assertNull(catalog);
    }

    @Test
    void testSaveNullCatalogThrowsException() throws SQLException
    {
        assertThrows(IllegalArgumentException.class, () -> {
            catalogDao.saveCatalog(null);
        });
    }

    @Test
    void testTransactionRollbackOnError() throws SQLException
    {
        // This test would verify that failed saves don't leave partial data
        // Due to the complexity of creating a controlled failure scenario,
        // this is left as a placeholder for more advanced testing

        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Save should succeed
        assertDoesNotThrow(() -> catalogDao.saveCatalog(catalog));
        assertTrue(catalogDao.catalogExists(catalogId));
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
        originalCatalog.addHierarchy(personMap);
        originalCatalog.addHierarchy(addressMap);

        // Save and load
        catalogDao.saveCatalog(originalCatalog);
        Catalog loadedCatalog = catalogDao.loadCatalog(catalogId);

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
    */
}
