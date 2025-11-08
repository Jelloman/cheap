package net.netbeing.cheap.integrationtests.dao;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import net.netbeing.cheap.db.AspectTableMapping;
import net.netbeing.cheap.db.postgres.PostgresAdapter;
import net.netbeing.cheap.db.postgres.PostgresCheapSchema;
import net.netbeing.cheap.db.postgres.PostgresDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PostgreSQL DAO with AspectTableMapping.
 * Tests two custom tables with different AspectTableMapping patterns.
 * Plain JUnit test without Spring Boot.
 */
class PostgresDaoAspectTableMappingTest
{
    private static EmbeddedPostgres embeddedPostgres;
    private static DataSource dataSource;

    private PostgresDao postgresDao;
    private PostgresAdapter adapter;
    private CheapFactory factory;

    private AspectDef personAspectDef;
    private AspectDef settingsAspectDef;

    private AspectTableMapping personTableMapping;
    private AspectTableMapping settingsTableMapping;

    @BeforeAll
    public static void setUpDatabase() throws Exception
    {
        // Start embedded PostgreSQL
        embeddedPostgres = EmbeddedPostgres.builder()
            .setPort(5433)
            .start();

        dataSource = embeddedPostgres.getPostgresDatabase();

        // Initialize schema
        PostgresCheapSchema schema = new PostgresCheapSchema();
        schema.executeMainSchemaDdl(dataSource);
        schema.executeAuditSchemaDdl(dataSource);
    }

    @AfterAll
    public static void tearDownDatabase() throws Exception
    {
        if (embeddedPostgres != null)
        {
            embeddedPostgres.close();
        }
    }

    @BeforeEach
    public void setUp() throws Exception
    {
        // Clean database before each test
        PostgresCheapSchema schema = new PostgresCheapSchema();
        schema.executeTruncateSchemaDdl(dataSource);

        factory = new CheapFactory();
        adapter = new PostgresAdapter(dataSource, factory);
        postgresDao = new PostgresDao(adapter);

        // Create AspectDef for person table (entity_id, name, age)
        Map<String, PropertyDef> personProps = new LinkedHashMap<>();
        personProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        personProps.put("age", factory.createPropertyDef("age", PropertyType.Integer));
        personAspectDef = factory.createImmutableAspectDef("person", personProps);

        // Create AspectDef for settings table (catalog_id, entity_id, key, value)
        Map<String, PropertyDef> settingsProps = new LinkedHashMap<>();
        settingsProps.put("key", factory.createPropertyDef("key", PropertyType.String));
        settingsProps.put("value", factory.createPropertyDef("value", PropertyType.String));
        settingsAspectDef = factory.createImmutableAspectDef("settings", settingsProps);

        // Create AspectTableMapping for person (Pattern 3: Entity ID only)
        Map<String, String> personColumnMapping = Map.of(
            "name", "name",
            "age", "age"
        );
        personTableMapping = new AspectTableMapping(
            personAspectDef,
            "person",
            personColumnMapping,
            false,  // hasCatalogId
            true    // hasEntityId
        );

        // Create AspectTableMapping for settings (Pattern 4: Both IDs)
        Map<String, String> settingsColumnMapping = Map.of(
            "key", "key",
            "value", "value"
        );
        settingsTableMapping = new AspectTableMapping(
            settingsAspectDef,
            "settings",
            settingsColumnMapping,
            true,   // hasCatalogId
            true    // hasEntityId
        );

        // Register mappings with DAO
        postgresDao.addAspectTableMapping(personTableMapping);
        postgresDao.addAspectTableMapping(settingsTableMapping);

        // Create the custom tables
        try
        {
            postgresDao.createTable(personTableMapping);
            postgresDao.createTable(settingsTableMapping);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to create custom tables", e);
        }
    }

    @Test
    void testSaveAndLoadCatalogWithCustomTables() throws Exception
    {
        // Create catalog
        UUID catalogId = testUuid(1000);
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Create AspectMapHierarchy for person aspects
        AspectMapHierarchy personHierarchy = factory.createAspectMapHierarchy(catalog, personAspectDef);

        // Add person data
        UUID personId1 = testUuid(1);
        Entity person1 = factory.createEntity(personId1);
        Aspect personAspect1 = factory.createPropertyMapAspect(person1, personAspectDef);
        personAspect1.put(factory.createProperty(personAspectDef.propertyDef("name"), "John Doe"));
        personAspect1.put(factory.createProperty(personAspectDef.propertyDef("age"), 30L));
        personHierarchy.put(person1, personAspect1);

        UUID personId2 = testUuid(2);
        Entity person2 = factory.createEntity(personId2);
        Aspect personAspect2 = factory.createPropertyMapAspect(person2, personAspectDef);
        personAspect2.put(factory.createProperty(personAspectDef.propertyDef("name"), "Jane Smith"));
        personAspect2.put(factory.createProperty(personAspectDef.propertyDef("age"), 28L));
        personHierarchy.put(person2, personAspect2);

        // Create AspectMapHierarchy for settings aspects
        AspectMapHierarchy settingsHierarchy = factory.createAspectMapHierarchy(catalog, settingsAspectDef);

        // Add settings data
        UUID settingId1 = testUuid(101);
        Entity setting1 = factory.createEntity(settingId1);
        Aspect settingAspect1 = factory.createPropertyMapAspect(setting1, settingsAspectDef);
        settingAspect1.put(factory.createProperty(settingsAspectDef.propertyDef("key"), "theme"));
        settingAspect1.put(factory.createProperty(settingsAspectDef.propertyDef("value"), "dark"));
        settingsHierarchy.put(setting1, settingAspect1);

        // Save catalog
        postgresDao.saveCatalog(catalog);

        // Verify person data in custom table
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT entity_id, name, age FROM person ORDER BY age"))
        {
            assertTrue(rs.next());
            assertEquals(personId2, rs.getObject("entity_id", UUID.class));
            assertEquals("Jane Smith", rs.getString("name"));
            assertEquals(28, rs.getInt("age"));

            assertTrue(rs.next());
            assertEquals(personId1, rs.getObject("entity_id", UUID.class));
            assertEquals("John Doe", rs.getString("name"));
            assertEquals(30, rs.getInt("age"));

            assertFalse(rs.next());
        }

        // Verify settings data in custom table
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT catalog_id, entity_id, key, value FROM settings"))
        {
            assertTrue(rs.next());
            assertEquals(catalogId, rs.getObject("catalog_id", UUID.class));
            assertEquals(settingId1, rs.getObject("entity_id", UUID.class));
            assertEquals("theme", rs.getString("key"));
            assertEquals("dark", rs.getString("value"));

            assertFalse(rs.next());
        }

        // Load catalog back
        Catalog loadedCatalog = postgresDao.loadCatalog(catalogId);
        assertNotNull(loadedCatalog);

        // Verify person aspects loaded correctly
        AspectMapHierarchy loadedPersonHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("person");
        assertNotNull(loadedPersonHierarchy);
        assertEquals(2, loadedPersonHierarchy.size());

        Aspect loadedPerson1 = loadedPersonHierarchy.get(person1);
        assertNotNull(loadedPerson1);
        assertEquals("John Doe", loadedPerson1.readObj("name"));
        assertEquals(30L, loadedPerson1.readObj("age"));

        Aspect loadedPerson2 = loadedPersonHierarchy.get(person2);
        assertNotNull(loadedPerson2);
        assertEquals("Jane Smith", loadedPerson2.readObj("name"));
        assertEquals(28L, loadedPerson2.readObj("age"));

        // Verify settings aspects loaded correctly
        AspectMapHierarchy loadedSettingsHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("settings");
        assertNotNull(loadedSettingsHierarchy);
        assertEquals(1, loadedSettingsHierarchy.size());

        Aspect loadedSetting1 = loadedSettingsHierarchy.get(setting1);
        assertNotNull(loadedSetting1);
        assertEquals("theme", loadedSetting1.readObj("key"));
        assertEquals("dark", loadedSetting1.readObj("value"));
    }

    @Test
    void testUpdateAspectsInCustomTables() throws Exception
    {
        // Create and save initial catalog
        UUID catalogId = testUuid(2000);
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        AspectMapHierarchy personHierarchy = factory.createAspectMapHierarchy(catalog, personAspectDef);
        UUID personId = testUuid(10);
        Entity person = factory.createEntity(personId);
        Aspect personAspect = factory.createPropertyMapAspect(person, personAspectDef);
        personAspect.put(factory.createProperty(personAspectDef.propertyDef("name"), "Bob Johnson"));
        personAspect.put(factory.createProperty(personAspectDef.propertyDef("age"), 45L));
        personHierarchy.put(person, personAspect);

        postgresDao.saveCatalog(catalog);

        // Update the aspect
        personAspect.put(factory.createProperty(personAspectDef.propertyDef("age"), 46L));
        postgresDao.saveCatalog(catalog);

        // Verify update in database
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, age FROM person WHERE entity_id = '" + personId + "'"))
        {
            assertTrue(rs.next());
            assertEquals("Bob Johnson", rs.getString("name"));
            assertEquals(46, rs.getInt("age"));
        }

        // Load and verify
        Catalog loadedCatalog = postgresDao.loadCatalog(catalogId);
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("person");
        Aspect loadedAspect = loadedHierarchy.get(person);
        assertEquals(46L, loadedAspect.readObj("age"));
    }

    @Test
    void testDeleteCatalogCleansUpCustomTables() throws Exception
    {
        // Create and save catalog with data
        UUID catalogId = testUuid(3000);
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        AspectMapHierarchy personHierarchy = factory.createAspectMapHierarchy(catalog, personAspectDef);
        UUID personId = testUuid(20);
        Entity person = factory.createEntity(personId);
        Aspect personAspect = factory.createPropertyMapAspect(person, personAspectDef);
        personAspect.put(factory.createProperty(personAspectDef.propertyDef("name"), "Test Person"));
        personAspect.put(factory.createProperty(personAspectDef.propertyDef("age"), 25L));
        personHierarchy.put(person, personAspect);

        AspectMapHierarchy settingsHierarchy = factory.createAspectMapHierarchy(catalog, settingsAspectDef);
        UUID settingId = testUuid(201);
        Entity setting = factory.createEntity(settingId);
        Aspect settingAspect = factory.createPropertyMapAspect(setting, settingsAspectDef);
        settingAspect.put(factory.createProperty(settingsAspectDef.propertyDef("key"), "test"));
        settingAspect.put(factory.createProperty(settingsAspectDef.propertyDef("value"), "value"));
        settingsHierarchy.put(setting, settingAspect);

        postgresDao.saveCatalog(catalog);

        // Verify data exists
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM person"))
        {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM settings WHERE catalog_id = '" + catalogId + "'"))
        {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }

        // Delete catalog
        postgresDao.deleteCatalog(catalogId);

        // Verify person table is empty (no catalog_id to filter by)
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM person"))
        {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }

        // Verify settings table has no rows for this catalog
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM settings WHERE catalog_id = '" + catalogId + "'"))
        {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }

        // Verify catalog cannot be loaded
        Catalog loadedCatalog = postgresDao.loadCatalog(catalogId);
        assertNull(loadedCatalog);
    }

    @AfterEach
    public void cleanupCustomTables() throws Exception
    {
        // Clean up custom tables
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement())
        {
            stmt.execute("DROP TABLE IF EXISTS person CASCADE");
            stmt.execute("DROP TABLE IF EXISTS settings CASCADE");
        }
        catch (Exception e)
        {
            // Ignore errors during cleanup
        }
    }

    /**
     * Generate a fixed test UUID based on a seed value.
     */
    private UUID testUuid(int seed)
    {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", seed));
    }
}
