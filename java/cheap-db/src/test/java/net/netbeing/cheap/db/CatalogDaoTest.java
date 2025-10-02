package net.netbeing.cheap.db;

import io.zonky.test.db.postgres.embedded.FlywayPreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.junit.jupiter.api.Test;
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
    public static PreparedDbExtension flywayDB = EmbeddedPostgresExtension.preparedDatabase(FlywayPreparer.forClasspathLocation("db/pg"));

    static volatile DataSource dataSource;
    static volatile boolean schemaInitialized = false;

    CatalogDao catalogDao;
    CheapFactory factory;

    void setUp() throws SQLException, IOException, URISyntaxException
    {
        // Get the datasource (will be initialized by JUnit extension)
        dataSource = flywayDB.getTestDatabase();

        // Initialize database schema once for all tests
        if (!schemaInitialized) {
            initializeSchema();
            schemaInitialized = true;
        }
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(tableExists(connection, "catalog"), "catalog table should exist");
        }

        factory = new CheapFactory();
        catalogDao = new CatalogDao(dataSource, factory);

        // Clean up all tables before each test
        truncateAllTables();
    }

    @SuppressWarnings("SameParameterValue")
    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (var rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
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

    @SuppressWarnings("DataFlowIssue")
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

    @Test
    void testDeleteCatalog() throws SQLException, IOException, URISyntaxException
    {
        setUp();

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
    void testDeleteNonExistentCatalog() throws SQLException, IOException, URISyntaxException
    {
        setUp();

        UUID nonExistentId = UUID.randomUUID();

        // Delete non-existent catalog
        boolean deleted = catalogDao.deleteCatalog(nonExistentId);
        assertFalse(deleted);
    }

    @Test
    void testCatalogExists() throws SQLException, IOException, URISyntaxException
    {
        setUp();

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
    void testLoadNonExistentCatalog() throws SQLException, IOException, URISyntaxException
    {
        setUp();

        UUID nonExistentId = UUID.randomUUID();
        Catalog catalog = catalogDao.loadCatalog(nonExistentId);
        assertNull(catalog);
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testSaveNullCatalogThrowsException() throws SQLException, IOException, URISyntaxException
    {
        setUp();

        assertThrows(IllegalArgumentException.class, () -> {
            catalogDao.saveCatalog(null);
        });
    }

    @Test
    void testTransactionRollbackOnError() throws SQLException, IOException, URISyntaxException
    {
        setUp();

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
    void testComplexCatalogRoundTrip() throws SQLException, IOException, URISyntaxException
    {
        setUp();

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
        // Note: AspectMapHierarchies are automatically added to catalog when created
        originalCatalog.addHierarchy(entitySet);
        originalCatalog.addHierarchy(directory);

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

    @Test
    void testAspectTableMapping() throws Exception
    {
        setUp();

        // Load AspectDef from test_aspect_mapping table (created by Flyway migration)
        PostgresCatalog pgCatalog = new PostgresCatalog(dataSource);
        AspectDef aspectDef = pgCatalog.loadTableDef("test_aspect_mapping");

        // Create AspectTableMapping with property-to-column mappings
        AspectTableMapping mapping = new AspectTableMapping(
            "test_aspect_mapping",
            "test_aspect_mapping",
            java.util.Map.of(
                "string_col", "string_col",
                "integer_col", "integer_col",
                "float_col", "float_col",
                "date_col", "date_col",
                "timestamp_col", "timestamp_col",
                "boolean_col", "boolean_col",
                "uuid_col", "uuid_col",
                "blob_col", "blob_col"
            )
        );

        // Add mapping to DAO
        catalogDao.addAspectTableMapping(mapping);

        // Create catalog with AspectMapHierarchy
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Create aspect map hierarchy
        AspectMapHierarchy hierarchy = factory.createAspectMapHierarchy(catalog, aspectDef);

        // Create test entities and aspects
        UUID entityId1 = UUID.randomUUID();
        UUID entityId2 = UUID.randomUUID();

        Entity entity1 = factory.createEntity(entityId1);
        Entity entity2 = factory.createEntity(entityId2);

        Aspect aspect1 = factory.createPropertyMapAspect(entity1, aspectDef);
        aspect1.put(factory.createProperty(aspectDef.propertyDef("string_col"), "test1"));
        aspect1.put(factory.createProperty(aspectDef.propertyDef("integer_col"), 42L));
        aspect1.put(factory.createProperty(aspectDef.propertyDef("float_col"), 3.14));
        aspect1.put(factory.createProperty(aspectDef.propertyDef("boolean_col"), true));
        aspect1.put(factory.createProperty(aspectDef.propertyDef("uuid_col"), UUID.fromString("550e8400-e29b-41d4-a716-446655440000")));

        Aspect aspect2 = factory.createPropertyMapAspect(entity2, aspectDef);
        aspect2.put(factory.createProperty(aspectDef.propertyDef("string_col"), "test2"));
        aspect2.put(factory.createProperty(aspectDef.propertyDef("integer_col"), 99L));
        aspect2.put(factory.createProperty(aspectDef.propertyDef("float_col"), 2.71));
        aspect2.put(factory.createProperty(aspectDef.propertyDef("boolean_col"), false));
        aspect2.put(factory.createProperty(aspectDef.propertyDef("uuid_col"), UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")));

        hierarchy.put(entity1, aspect1);
        hierarchy.put(entity2, aspect2);

        // Clear any existing data in the table from migrations
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM test_aspect_mapping");
        }

        // Save catalog (should use mapped table for the hierarchy)
        catalogDao.saveCatalog(catalog);

        // Verify the data was saved to the mapped table by querying directly
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT * FROM test_aspect_mapping ORDER BY string_col")) {

            // Should have 2 rows
            assertTrue(rs.next(), "Should have first row");
            assertEquals(entityId1, rs.getObject("entity_id", UUID.class));
            assertEquals("test1", rs.getString("string_col"));
            assertEquals(42, rs.getInt("integer_col"));
            assertEquals(3.14, rs.getDouble("float_col"), 0.001);
            assertEquals(true, rs.getBoolean("boolean_col"));
            assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), rs.getObject("uuid_col", UUID.class));

            assertTrue(rs.next(), "Should have second row");
            assertEquals(entityId2, rs.getObject("entity_id", UUID.class));
            assertEquals("test2", rs.getString("string_col"));
            assertEquals(99, rs.getInt("integer_col"));
            assertEquals(2.71, rs.getDouble("float_col"), 0.001);
            assertEquals(false, rs.getBoolean("boolean_col"));
            assertEquals(UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"), rs.getObject("uuid_col", UUID.class));

            assertFalse(rs.next(), "Should have exactly 2 rows");
        }

        // Also verify that the default aspect/property tables were NOT used
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM aspect WHERE entity_id IN (?, ?)")) {
            stmt.setObject(1, entityId1);
            stmt.setObject(2, entityId2);
            try (var rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Should not have saved to aspect table when using mapped table");
            }
        }
    }

    @Test
    void testCreateAspectTableWithAllPropertyTypes() throws Exception
    {
        setUp();

        // Create an AspectDef with one Property of each type
        String aspectDefName = "all_types_aspect";

        PropertyDef intProp = factory.createPropertyDef("int_prop", PropertyType.Integer, null, false, true, true, true, false, false);
        PropertyDef floatProp = factory.createPropertyDef("float_prop", PropertyType.Float, null, false, true, true, true, false, false);
        PropertyDef boolProp = factory.createPropertyDef("bool_prop", PropertyType.Boolean, null, false, true, true, true, false, false);
        PropertyDef stringProp = factory.createPropertyDef("string_prop", PropertyType.String, null, false, true, true, true, false, false);
        PropertyDef textProp = factory.createPropertyDef("text_prop", PropertyType.Text, null, false, true, true, true, false, false);
        PropertyDef bigIntProp = factory.createPropertyDef("bigint_prop", PropertyType.BigInteger, null, false, true, true, true, false, false);
        PropertyDef bigDecProp = factory.createPropertyDef("bigdec_prop", PropertyType.BigDecimal, null, false, true, true, true, false, false);
        PropertyDef dateProp = factory.createPropertyDef("date_prop", PropertyType.DateTime, null, false, true, true, true, false, false);
        PropertyDef uriProp = factory.createPropertyDef("uri_prop", PropertyType.URI, null, false, true, true, true, false, false);
        PropertyDef uuidProp = factory.createPropertyDef("uuid_prop", PropertyType.UUID, null, false, true, true, true, false, false);
        PropertyDef clobProp = factory.createPropertyDef("clob_prop", PropertyType.CLOB, null, false, true, true, true, false, false);
        PropertyDef blobProp = factory.createPropertyDef("blob_prop", PropertyType.BLOB, null, false, true, true, true, false, false);

        java.util.Map<String, PropertyDef> propDefs = new java.util.LinkedHashMap<>();
        propDefs.put("int_prop", intProp);
        propDefs.put("float_prop", floatProp);
        propDefs.put("bool_prop", boolProp);
        propDefs.put("string_prop", stringProp);
        propDefs.put("text_prop", textProp);
        propDefs.put("bigint_prop", bigIntProp);
        propDefs.put("bigdec_prop", bigDecProp);
        propDefs.put("date_prop", dateProp);
        propDefs.put("uri_prop", uriProp);
        propDefs.put("uuid_prop", uuidProp);
        propDefs.put("clob_prop", clobProp);
        propDefs.put("blob_prop", blobProp);

        AspectDef aspectDef = factory.createImmutableAspectDef(aspectDefName, propDefs);

        // Create the table using createAspectTable()
        String tableName = "test_all_types";
        catalogDao.createAspectTable(aspectDef, tableName);

        // Create and register an AspectTableMapping
        java.util.Map<String, String> columnMapping = new java.util.LinkedHashMap<>();
        columnMapping.put("int_prop", "int_prop");
        columnMapping.put("float_prop", "float_prop");
        columnMapping.put("bool_prop", "bool_prop");
        columnMapping.put("string_prop", "string_prop");
        columnMapping.put("text_prop", "text_prop");
        columnMapping.put("bigint_prop", "bigint_prop");
        columnMapping.put("bigdec_prop", "bigdec_prop");
        columnMapping.put("date_prop", "date_prop");
        columnMapping.put("uri_prop", "uri_prop");
        columnMapping.put("uuid_prop", "uuid_prop");
        columnMapping.put("clob_prop", "clob_prop");
        columnMapping.put("blob_prop", "blob_prop");

        AspectTableMapping mapping = new AspectTableMapping(
            aspectDefName,
            tableName,
            columnMapping
        );
        catalogDao.addAspectTableMapping(mapping);

        // Create catalog with AspectMapHierarchy
        UUID catalogId = UUID.randomUUID();
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // extend() automatically creates an AspectMapHierarchy with the AspectDef's name
        catalog.extend(aspectDef);
        AspectMapHierarchy hierarchy = (AspectMapHierarchy) catalog.hierarchy(aspectDefName);

        // Create test entities and aspects with all property types
        UUID entityId1 = UUID.randomUUID();
        UUID entityId2 = UUID.randomUUID();

        Entity entity1 = factory.createEntity(entityId1);
        Entity entity2 = factory.createEntity(entityId2);

        // Create first aspect with test values
        Aspect aspect1 = factory.createPropertyMapAspect(entity1, aspectDef);
        aspect1.put(factory.createProperty(intProp, 42L));
        aspect1.put(factory.createProperty(floatProp, 3.14159));
        aspect1.put(factory.createProperty(boolProp, true));
        aspect1.put(factory.createProperty(stringProp, "Hello World"));
        aspect1.put(factory.createProperty(textProp, "This is a long text field with lots of content"));
        aspect1.put(factory.createProperty(bigIntProp, new java.math.BigInteger("12345678901234567890")));
        aspect1.put(factory.createProperty(bigDecProp, new java.math.BigDecimal("123.456789012345678901234567890")));
        aspect1.put(factory.createProperty(dateProp, java.time.ZonedDateTime.parse("2025-01-15T10:30:00Z")));
        aspect1.put(factory.createProperty(uriProp, new java.net.URI("https://example.com/path")));
        aspect1.put(factory.createProperty(uuidProp, UUID.fromString("550e8400-e29b-41d4-a716-446655440000")));
        aspect1.put(factory.createProperty(clobProp, "CLOB content here"));
        aspect1.put(factory.createProperty(blobProp, new byte[]{1, 2, 3, 4, 5}));

        // Create second aspect with different test values
        Aspect aspect2 = factory.createPropertyMapAspect(entity2, aspectDef);
        aspect2.put(factory.createProperty(intProp, 99L));
        aspect2.put(factory.createProperty(floatProp, 2.71828));
        aspect2.put(factory.createProperty(boolProp, false));
        aspect2.put(factory.createProperty(stringProp, "Goodbye World"));
        aspect2.put(factory.createProperty(textProp, "Another long text field"));
        aspect2.put(factory.createProperty(bigIntProp, new java.math.BigInteger("98765432109876543210")));
        aspect2.put(factory.createProperty(bigDecProp, new java.math.BigDecimal("987.654321098765432109876543210")));
        aspect2.put(factory.createProperty(dateProp, java.time.ZonedDateTime.parse("2025-12-31T23:59:59Z")));
        aspect2.put(factory.createProperty(uriProp, new java.net.URI("https://example.org/another")));
        aspect2.put(factory.createProperty(uuidProp, UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")));
        aspect2.put(factory.createProperty(clobProp, "Different CLOB content"));
        aspect2.put(factory.createProperty(blobProp, new byte[]{10, 20, 30, 40, 50}));

        hierarchy.put(entity1, aspect1);
        hierarchy.put(entity2, aspect2);

        // Save catalog (should use mapped table for the hierarchy)
        catalogDao.saveCatalog(catalog);

        // Load the catalog back
        Catalog loadedCatalog = catalogDao.loadCatalog(catalogId);
        assertNotNull(loadedCatalog);

        // Get the loaded hierarchy
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy(aspectDefName);
        assertNotNull(loadedHierarchy);

        // Verify first aspect was correctly reconstituted
        Entity loadedEntity1 = factory.getOrRegisterNewEntity(entityId1);
        Aspect loadedAspect1 = loadedHierarchy.get(loadedEntity1);
        assertNotNull(loadedAspect1);
        assertEquals(42L, loadedAspect1.unsafeReadObj("int_prop"));
        assertEquals(3.14159, (Double) loadedAspect1.unsafeReadObj("float_prop"), 0.00001);
        assertEquals(true, loadedAspect1.unsafeReadObj("bool_prop"));
        assertEquals("Hello World", loadedAspect1.unsafeReadObj("string_prop"));
        assertEquals("This is a long text field with lots of content", loadedAspect1.unsafeReadObj("text_prop"));
        assertEquals(new java.math.BigInteger("12345678901234567890"), new java.math.BigInteger(loadedAspect1.unsafeReadObj("bigint_prop").toString()));
        assertEquals(new java.math.BigDecimal("123.456789012345678901234567890"), new java.math.BigDecimal(loadedAspect1.unsafeReadObj("bigdec_prop").toString()));
        assertEquals("https://example.com/path", loadedAspect1.unsafeReadObj("uri_prop").toString());
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), loadedAspect1.unsafeReadObj("uuid_prop"));
        assertEquals("CLOB content here", loadedAspect1.unsafeReadObj("clob_prop"));
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, (byte[]) loadedAspect1.unsafeReadObj("blob_prop"));

        // Verify second aspect was correctly reconstituted
        Entity loadedEntity2 = factory.getOrRegisterNewEntity(entityId2);
        Aspect loadedAspect2 = loadedHierarchy.get(loadedEntity2);
        assertNotNull(loadedAspect2);
        assertEquals(99L, loadedAspect2.unsafeReadObj("int_prop"));
        assertEquals(2.71828, (Double) loadedAspect2.unsafeReadObj("float_prop"), 0.00001);
        assertEquals(false, loadedAspect2.unsafeReadObj("bool_prop"));
        assertEquals("Goodbye World", loadedAspect2.unsafeReadObj("string_prop"));
        assertEquals("Another long text field", loadedAspect2.unsafeReadObj("text_prop"));
        assertEquals(new java.math.BigInteger("98765432109876543210"), new java.math.BigInteger(loadedAspect2.unsafeReadObj("bigint_prop").toString()));
        assertEquals(new java.math.BigDecimal("987.654321098765432109876543210"), new java.math.BigDecimal(loadedAspect2.unsafeReadObj("bigdec_prop").toString()));
        assertEquals("https://example.org/another", loadedAspect2.unsafeReadObj("uri_prop").toString());
        assertEquals(UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"), loadedAspect2.unsafeReadObj("uuid_prop"));
        assertEquals("Different CLOB content", loadedAspect2.unsafeReadObj("clob_prop"));
        assertArrayEquals(new byte[]{10, 20, 30, 40, 50}, (byte[]) loadedAspect2.unsafeReadObj("blob_prop"));
    }
}
