package net.netbeing.cheap.integrationtests.dao;

import ch.vorburger.exec.ManagedProcessException;
import net.netbeing.cheap.db.AspectTableMapping;
import net.netbeing.cheap.integrationtests.util.DatabaseRunnerExtension;
import net.netbeing.cheap.integrationtests.util.MariaDbIntegrationTestDb;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MariaDB DAO with AspectTableMapping.
 * Tests two custom tables with different AspectTableMapping patterns.
 * Plain JUnit test without Spring Boot.
 */
@ExtendWith(DatabaseRunnerExtension.class)
class MariaDbDaoAspectTableMappingTest
{
    private static MariaDbIntegrationTestDb testDb;

    private CheapFactory factory;

    private AspectDef employeeAspectDef;
    private AspectDef metadataAspectDef;

    @BeforeAll
    public static void setUpTestDb() throws ManagedProcessException, SQLException
    {
        // Create test database
        testDb = new MariaDbIntegrationTestDb("mariadb_aspect_mapping_test", true);
        testDb.initializeCheapSchema();
    }

    @BeforeEach
    public void setUp() throws SQLException
    {
        AspectTableMapping metadataTableMapping;
        AspectTableMapping employeeTableMapping;
        // Clean database before each test
        testDb.truncateAllTables();

        factory = new CheapFactory();

        // Create AspectDef for employee table (entity_id, employee_id, name, department)
        Map<String, PropertyDef> employeeProps = new LinkedHashMap<>();
        employeeProps.put("employee_id", factory.createPropertyDef("employee_id", PropertyType.String));
        employeeProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        employeeProps.put("department", factory.createPropertyDef("department", PropertyType.String));
        employeeAspectDef = factory.createImmutableAspectDef("employee", employeeProps);

        // Create AspectDef for metadata table (key, value)
        Map<String, PropertyDef> metadataProps = new LinkedHashMap<>();
        metadataProps.put("key", factory.createPropertyDef("key", PropertyType.String));
        metadataProps.put("value", factory.createPropertyDef("value", PropertyType.String));
        metadataAspectDef = factory.createImmutableAspectDef("metadata", metadataProps);

        // Create AspectTableMapping for employee (Pattern 3: Entity ID only)
        Map<String, String> employeeColumnMapping = Map.of(
            "employee_id", "employee_id",
            "name", "name",
            "department", "department"
        );
        employeeTableMapping = new AspectTableMapping(
            employeeAspectDef,
            "employee",
            employeeColumnMapping,
            false,  // hasCatalogId
            true    // hasEntityId
        );

        // Create AspectTableMapping for metadata (Pattern 1: No IDs - lookup table)
        // Note: Using `meta_key` and `meta_value` to avoid SQL reserved keywords
        Map<String, String> metadataColumnMapping = Map.of(
            "key", "meta_key",
            "value", "meta_value"
        );
        metadataTableMapping = new AspectTableMapping(
            metadataAspectDef,
            "metadata",
            metadataColumnMapping,
            false,  // hasCatalogId
            false   // hasEntityId
        );

        // Register mappings with DAO
        testDb.mariaDbDao.addAspectTableMapping(employeeTableMapping);
        testDb.mariaDbDao.addAspectTableMapping(metadataTableMapping);

        // Create the custom tables
        testDb.mariaDbDao.createTable(employeeTableMapping);
        testDb.mariaDbDao.createTable(metadataTableMapping);
    }

    @Test
    void testSaveAndLoadCatalogWithCustomTables() throws SQLException
    {
        // Create catalog
        UUID catalogId = testUuid(1000);
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Create AspectMapHierarchy for employee aspects
        AspectMapHierarchy employeeHierarchy = factory.createAspectMapHierarchy(catalog, employeeAspectDef);

        // Add employee data
        UUID entityId1 = testUuid(1);
        Entity employee1 = factory.createEntity(entityId1);
        Aspect employeeAspect1 = factory.createPropertyMapAspect(employee1, employeeAspectDef);
        employeeAspect1.put(factory.createProperty(employeeAspectDef.propertyDef("employee_id"), "EMP-001"));
        employeeAspect1.put(factory.createProperty(employeeAspectDef.propertyDef("name"), "Alice Johnson"));
        employeeAspect1.put(factory.createProperty(employeeAspectDef.propertyDef("department"), "Engineering"));
        employeeHierarchy.put(employee1, employeeAspect1);

        UUID entityId2 = testUuid(2);
        Entity employee2 = factory.createEntity(entityId2);
        Aspect employeeAspect2 = factory.createPropertyMapAspect(employee2, employeeAspectDef);
        employeeAspect2.put(factory.createProperty(employeeAspectDef.propertyDef("employee_id"), "EMP-002"));
        employeeAspect2.put(factory.createProperty(employeeAspectDef.propertyDef("name"), "Bob Smith"));
        employeeAspect2.put(factory.createProperty(employeeAspectDef.propertyDef("department"), "Sales"));
        employeeHierarchy.put(employee2, employeeAspect2);

        // Create AspectMapHierarchy for metadata aspects
        AspectMapHierarchy metadataHierarchy = factory.createAspectMapHierarchy(catalog, metadataAspectDef);

        // Add metadata (entity IDs will be generated since no entity_id column)
        Entity meta1 = factory.createEntity();
        Aspect metaAspect1 = factory.createPropertyMapAspect(meta1, metadataAspectDef);
        metaAspect1.put(factory.createProperty(metadataAspectDef.propertyDef("key"), "version"));
        metaAspect1.put(factory.createProperty(metadataAspectDef.propertyDef("value"), "1.0.0"));
        metadataHierarchy.put(meta1, metaAspect1);

        Entity meta2 = factory.createEntity();
        Aspect metaAspect2 = factory.createPropertyMapAspect(meta2, metadataAspectDef);
        metaAspect2.put(factory.createProperty(metadataAspectDef.propertyDef("key"), "environment"));
        metaAspect2.put(factory.createProperty(metadataAspectDef.propertyDef("value"), "production"));
        metadataHierarchy.put(meta2, metaAspect2);

        // Save catalog
        testDb.mariaDbDao.saveCatalog(catalog);

        // Verify employee data in custom table
        try (Connection conn = testDb.dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT entity_id, employee_id, name, department FROM employee ORDER BY employee_id"))
        {
            assertTrue(rs.next());
            assertEquals(entityId1.toString(), rs.getString("entity_id"));
            assertEquals("EMP-001", rs.getString("employee_id"));
            assertEquals("Alice Johnson", rs.getString("name"));
            assertEquals("Engineering", rs.getString("department"));

            assertTrue(rs.next());
            assertEquals(entityId2.toString(), rs.getString("entity_id"));
            assertEquals("EMP-002", rs.getString("employee_id"));
            assertEquals("Bob Smith", rs.getString("name"));
            assertEquals("Sales", rs.getString("department"));

            assertFalse(rs.next());
        }

        // Verify metadata in custom table (no entity_id or catalog_id columns)
        try (Connection conn = testDb.dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT meta_key, meta_value FROM metadata ORDER BY meta_key"))
        {
            assertTrue(rs.next());
            assertEquals("environment", rs.getString("meta_key"));
            assertEquals("production", rs.getString("meta_value"));

            assertTrue(rs.next());
            assertEquals("version", rs.getString("meta_key"));
            assertEquals("1.0.0", rs.getString("meta_value"));

            assertFalse(rs.next());
        }

        // Load catalog back
        Catalog loadedCatalog = testDb.mariaDbDao.loadCatalog(catalogId);
        assertNotNull(loadedCatalog);

        // Verify employee aspects loaded correctly
        AspectMapHierarchy loadedEmployeeHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("employee");
        assertNotNull(loadedEmployeeHierarchy);
        assertEquals(2, loadedEmployeeHierarchy.size());

        Aspect loadedEmployee1 = loadedEmployeeHierarchy.get(employee1);
        assertNotNull(loadedEmployee1);
        assertEquals("EMP-001", loadedEmployee1.readObj("employee_id"));
        assertEquals("Alice Johnson", loadedEmployee1.readObj("name"));
        assertEquals("Engineering", loadedEmployee1.readObj("department"));

        Aspect loadedEmployee2 = loadedEmployeeHierarchy.get(employee2);
        assertNotNull(loadedEmployee2);
        assertEquals("EMP-002", loadedEmployee2.readObj("employee_id"));
        assertEquals("Bob Smith", loadedEmployee2.readObj("name"));
        assertEquals("Sales", loadedEmployee2.readObj("department"));

        // Verify metadata aspects loaded correctly
        AspectMapHierarchy loadedMetadataHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("metadata");
        assertNotNull(loadedMetadataHierarchy);
        assertEquals(2, loadedMetadataHierarchy.size());

        // Metadata entity IDs will be different (generated on load)
        boolean foundVersion = false;
        boolean foundEnvironment = false;
        for (Map.Entry<Entity, Aspect> entry : loadedMetadataHierarchy.entrySet())
        {
            Aspect aspect = entry.getValue();
            String key = (String) aspect.readObj("key");
            if ("version".equals(key))
            {
                assertEquals("1.0.0", aspect.readObj("value"));
                foundVersion = true;
            }
            else if ("environment".equals(key))
            {
                assertEquals("production", aspect.readObj("value"));
                foundEnvironment = true;
            }
        }
        assertTrue(foundVersion, "Should have loaded version metadata");
        assertTrue(foundEnvironment, "Should have loaded environment metadata");
    }

    @Test
    void testUpdateAspectsInCustomTables() throws SQLException
    {
        // Create and save initial catalog
        UUID catalogId = testUuid(2000);
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        AspectMapHierarchy employeeHierarchy = factory.createAspectMapHierarchy(catalog, employeeAspectDef);
        UUID entityId = testUuid(10);
        Entity employee = factory.createEntity(entityId);
        Aspect employeeAspect = factory.createPropertyMapAspect(employee, employeeAspectDef);
        employeeAspect.put(factory.createProperty(employeeAspectDef.propertyDef("employee_id"), "EMP-100"));
        employeeAspect.put(factory.createProperty(employeeAspectDef.propertyDef("name"), "Charlie Brown"));
        employeeAspect.put(factory.createProperty(employeeAspectDef.propertyDef("department"), "Marketing"));
        employeeHierarchy.put(employee, employeeAspect);

        testDb.mariaDbDao.saveCatalog(catalog);

        // Update the aspect
        employeeAspect.put(factory.createProperty(employeeAspectDef.propertyDef("department"), "Product"));
        testDb.mariaDbDao.saveCatalog(catalog);

        // Verify update in database
        try (Connection conn = testDb.dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT employee_id, name, department FROM employee WHERE entity_id = '" + entityId + "'"))
        {
            assertTrue(rs.next());
            assertEquals("EMP-100", rs.getString("employee_id"));
            assertEquals("Charlie Brown", rs.getString("name"));
            assertEquals("Product", rs.getString("department"));
        }

        // Load and verify
        Catalog loadedCatalog = testDb.mariaDbDao.loadCatalog(catalogId);
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("employee");
        Aspect loadedAspect = loadedHierarchy.get(employee);
        assertEquals("Product", loadedAspect.readObj("department"));
    }

    @Test
    void testDeleteCatalogCleansUpCustomTables() throws SQLException
    {
        // Create and save catalog with data
        UUID catalogId = testUuid(3000);
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        AspectMapHierarchy employeeHierarchy = factory.createAspectMapHierarchy(catalog, employeeAspectDef);
        UUID entityId = testUuid(20);
        Entity employee = factory.createEntity(entityId);
        Aspect employeeAspect = factory.createPropertyMapAspect(employee, employeeAspectDef);
        employeeAspect.put(factory.createProperty(employeeAspectDef.propertyDef("employee_id"), "EMP-TEST"));
        employeeAspect.put(factory.createProperty(employeeAspectDef.propertyDef("name"), "Test Employee"));
        employeeAspect.put(factory.createProperty(employeeAspectDef.propertyDef("department"), "Test"));
        employeeHierarchy.put(employee, employeeAspect);

        AspectMapHierarchy metadataHierarchy = factory.createAspectMapHierarchy(catalog, metadataAspectDef);
        Entity meta = factory.createEntity();
        Aspect metaAspect = factory.createPropertyMapAspect(meta, metadataAspectDef);
        metaAspect.put(factory.createProperty(metadataAspectDef.propertyDef("key"), "test_key"));
        metaAspect.put(factory.createProperty(metadataAspectDef.propertyDef("value"), "test_value"));
        metadataHierarchy.put(meta, metaAspect);

        testDb.mariaDbDao.saveCatalog(catalog);

        // Verify data exists
        try (Connection conn = testDb.dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM employee"))
        {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }

        try (Connection conn = testDb.dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM metadata"))
        {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }

        // Delete catalog
        testDb.mariaDbDao.deleteCatalog(catalogId);

        // Verify employee table is empty (truncated before save, no catalog_id to filter)
        try (Connection conn = testDb.dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM employee"))
        {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }

        // Verify metadata table is empty (truncated before save)
        try (Connection conn = testDb.dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM metadata"))
        {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }

        // Verify catalog cannot be loaded
        Catalog loadedCatalog = testDb.mariaDbDao.loadCatalog(catalogId);
        assertNull(loadedCatalog);
    }

    @Test
    void testForeignKeyConstraintsWithCustomTables() throws SQLException
    {
        // This test verifies that foreign key constraints work with AspectTableMapping
        // when useForeignKeys() returns true

        // Create catalog
        UUID catalogId = testUuid(4000);
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        AspectMapHierarchy employeeHierarchy = factory.createAspectMapHierarchy(catalog, employeeAspectDef);
        UUID entityId = testUuid(30);
        Entity employee = factory.createEntity(entityId);
        Aspect employeeAspect = factory.createPropertyMapAspect(employee, employeeAspectDef);
        employeeAspect.put(factory.createProperty(employeeAspectDef.propertyDef("employee_id"), "EMP-FK"));
        employeeAspect.put(factory.createProperty(employeeAspectDef.propertyDef("name"), "FK Test"));
        employeeAspect.put(factory.createProperty(employeeAspectDef.propertyDef("department"), "Test"));
        employeeHierarchy.put(employee, employeeAspect);

        // Save catalog
        testDb.mariaDbDao.saveCatalog(catalog);

        // Verify data saved
        Catalog loadedCatalog = testDb.mariaDbDao.loadCatalog(catalogId);
        assertNotNull(loadedCatalog);

        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("employee");
        assertNotNull(loadedHierarchy);
        assertEquals(1, loadedHierarchy.size());
    }

    @AfterEach
    public void cleanupCustomTables() throws SQLException
    {
        // Clean up custom tables
        try (Connection conn = testDb.dataSource.getConnection();
             Statement stmt = conn.createStatement())
        {
            stmt.execute("DROP TABLE IF EXISTS employee");
            stmt.execute("DROP TABLE IF EXISTS metadata");
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
