package net.netbeing.cheap.db;

import io.zonky.test.db.postgres.embedded.FlywayPreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class PostgresCatalogTest
{
    @RegisterExtension
    public static PreparedDbExtension flywayDB = EmbeddedPostgresExtension.preparedDatabase(FlywayPreparer.forClasspathLocation("db/pg"));

    private DataSource dataSource;
    private PostgresCatalog catalog;
    private static boolean cheapSchemaInitialized = false;

    @BeforeEach
    void setUp() {
        dataSource = flywayDB.getTestDatabase();
        catalog = new PostgresCatalog(dataSource);
    }

    private void initializeCheapSchema() throws SQLException, IOException, URISyntaxException {
        if (cheapSchemaInitialized) {
            return;
        }

        String mainSchemaPath = "/db/schemas/postgres-cheap.sql";
        String auditSchemaPath = "/db/schemas/postgres-cheap-audit.sql";

        String mainDdl = loadResourceFile(mainSchemaPath);
        String auditDdl = loadResourceFile(auditSchemaPath);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(mainDdl);
            statement.execute(auditDdl);
        }

        cheapSchemaInitialized = true;
    }

    @SuppressWarnings("DataFlowIssue")
    private static String loadResourceFile(String resourcePath) throws IOException, URISyntaxException {
        Path path = Paths.get(PostgresCatalogTest.class.getResource(resourcePath).toURI());
        return Files.readString(path);
    }

    @Test
    void testLoadDb()
    {
        assertNotNull(catalog, "Catalog should not be null");
        
        List<String> tables = catalog.getTables();
        assertNotNull(tables, "Tables list should not be null");
        assertEquals(3, tables.size(), "Should have exactly two tables (test_table + flyway_schema_history)");
        assertTrue(tables.contains("test_table"), "Should contain test_table");
        assertTrue(tables.contains("test_aspect_mapping"), "Should contain test_table");
    }
    
    @Test
    void testLoadDbStaticMethod()
    {
        PostgresCatalog staticCatalog = new PostgresCatalog(dataSource);
        
        assertNotNull(staticCatalog, "Catalog should not be null");
        
        List<String> tables = staticCatalog.getTables();
        assertNotNull(tables, "Tables list should not be null");
        assertEquals(3, tables.size(), "Should have exactly two tables");
        assertTrue(tables.contains("test_table"), "Should contain test_table");
        assertTrue(tables.contains("test_aspect_mapping"), "Should contain test_table");
    }

    @Test
    void testLoadTableDef()
    {
        AspectDef tableDef = catalog.loadTableDef("test_table");
        
        assertNotNull(tableDef, "Table definition should not be null");
        assertEquals("test_table", tableDef.name(), "AspectDef name should match table name");
        
        List<? extends PropertyDef> properties = tableDef.propertyDefs().stream().toList();
        assertEquals(9, properties.size(), "Should have 9 columns");
        
        // Verify each column
        PropertyDef idProp = tableDef.propertyDef("id");
        assertNotNull(idProp, "id column should exist");
        assertEquals("id", idProp.name(), "Column name should be 'id'");
        assertEquals(PropertyType.Integer, idProp.type(), "id should be Integer type");
        assertFalse(idProp.isNullable(), "id should not be nullable");
        
        PropertyDef stringProp = tableDef.propertyDef("string_col");
        assertNotNull(stringProp, "string_col should exist");
        assertEquals(PropertyType.String, stringProp.type(), "string_col should be Text type");
        assertTrue(stringProp.isNullable(), "string_col should be nullable");
        
        PropertyDef integerProp = tableDef.propertyDef("integer_col");
        assertNotNull(integerProp, "integer_col should exist");
        assertEquals(PropertyType.Integer, integerProp.type(), "integer_col should be Integer type");
        
        PropertyDef floatProp = tableDef.propertyDef("float_col");
        assertNotNull(floatProp, "float_col should exist");
        assertEquals(PropertyType.Float, floatProp.type(), "float_col should be Float type");
        
        PropertyDef dateProp = tableDef.propertyDef("date_col");
        assertNotNull(dateProp, "date_col should exist");
        assertEquals(PropertyType.DateTime, dateProp.type(), "date_col should be DateTime type");
        
        PropertyDef timestampProp = tableDef.propertyDef("timestamp_col");
        assertNotNull(timestampProp, "timestamp_col should exist");
        assertEquals(PropertyType.DateTime, timestampProp.type(), "timestamp_col should be DateTime type");
        
        PropertyDef booleanProp = tableDef.propertyDef("boolean_col");
        assertNotNull(booleanProp, "boolean_col should exist");
        assertEquals(PropertyType.Boolean, booleanProp.type(), "boolean_col should be Boolean type");
        
        PropertyDef uuidProp = tableDef.propertyDef("uuid_col");
        assertNotNull(uuidProp, "uuid_col should exist");
        assertEquals(PropertyType.UUID, uuidProp.type(), "uuid_col should be Text type (UUID as text)");
        
        PropertyDef blobProp = tableDef.propertyDef("blob_col");
        assertNotNull(blobProp, "blob_col should exist");
        assertEquals(PropertyType.BLOB, blobProp.type(), "blob_col should be BLOB type");
    }
    
    @Test
    void testLoadTableDefNonExistentTable()
    {
        AspectDef tableDef = catalog.loadTableDef("non_existent_table");
        
        assertNotNull(tableDef, "Table definition should not be null even for non-existent table");
        assertEquals("non_existent_table", tableDef.name(), "AspectDef name should match requested table name");
        assertTrue(tableDef.propertyDefs().isEmpty(), "Should have no columns for non-existent table");
    }
    
    @Test
    void testLoadTable()
    {
        AspectMapHierarchy table = catalog.loadTable("test_table", -1);
        
        assertNotNull(table, "Table hierarchy should not be null");
        assertEquals("test_table", table.name(), "Hierarchy name should match expected pattern");
        assertEquals(HierarchyType.ASPECT_MAP, table.type(), "Should be ASPECT_MAP hierarchy type");
        assertEquals("test_table", table.aspectDef().name(), "AspectDef name should match table name");
        
        // Check that we loaded all rows (should be 2)
        assertEquals(2, table.size(), "Should have 2 rows loaded");
        
        // Verify data in the loaded aspects
        boolean foundRow1 = false, foundRow2 = false;
        
        for (Aspect aspect : table.values()) {
            Property idProp = aspect.get("id");
            assertNotNull(idProp, "id property should exist");
            
            Long idValue = (Long) idProp.unsafeRead();
            if (idValue == 1) {
                foundRow1 = true;
                
                Property stringProp = aspect.get("string_col");
                assertEquals("string1", stringProp.unsafeRead(), "string_col should be 'string1'");
                
                Property integerProp = aspect.get("integer_col");
                assertEquals(1, (Long) integerProp.unsafeRead(), "integer_col should be 1");
                
                Property floatProp = aspect.get("float_col");
                assertEquals(1.5, ((Double) floatProp.unsafeRead()), "float_col should be 1.5");
                
                Property dateProp = aspect.get("date_col");
                assertEquals("2025-01-01", dateProp.unsafeRead(), "date_col should be 2025-01-01");
                
                Property timestampProp = aspect.get("timestamp_col");
                //String expectedTime = LocalDateTime.of(2025, 1, 11, 18, 18, 18, 18000000).toInstant()
                assertEquals("2025-01-12T02:18:18.018Z", timestampProp.unsafeRead(), "timestamp_col should match expected timestamp");
                
                Property booleanProp = aspect.get("boolean_col");
                assertEquals(true, (Boolean) booleanProp.unsafeRead(), "boolean_col should be true");
                
                Property uuidProp = aspect.get("uuid_col");
                assertEquals(UUID.fromString("4186bfb6-b135-48af-9236-95cacdb20327"), uuidProp.unsafeRead(), "uuid_col should match expected UUID");
                
                Property blobProp = aspect.get("blob_col");
                assertNotNull(blobProp.unsafeRead(), "blob_col should not be null");
                
            } else if (idValue == 2) {
                foundRow2 = true;
                
                Property stringProp = aspect.get("string_col");
                assertEquals("string2", stringProp.unsafeRead(), "string_col should be 'string2'");
                
                Property integerProp = aspect.get("integer_col");
                assertEquals(2, (Long) integerProp.unsafeRead(), "integer_col should be 2");
                
                Property floatProp = aspect.get("float_col");
                assertEquals(2.5, ((Double) floatProp.unsafeRead()), "float_col should be 2.5");
                
                Property dateProp = aspect.get("date_col");
                assertEquals("2025-02-02", dateProp.unsafeRead(), "date_col should be 2025-02-02");
                
                Property timestampProp = aspect.get("timestamp_col");
                assertEquals("2025-02-02T10:02:02.002Z", timestampProp.unsafeRead(), "timestamp_col should match expected timestamp");
                
                Property booleanProp = aspect.get("boolean_col");
                assertEquals(false, (Boolean) booleanProp.unsafeRead(), "boolean_col should be false");
                
                Property uuidProp = aspect.get("uuid_col");
                assertEquals(UUID.fromString("655a99b9-af7c-4f2f-afa8-c4801986b9d4"), uuidProp.unsafeRead(), "uuid_col should match expected UUID");
            }
        }
        
        assertTrue(foundRow1, "Should have found row with id=1");
        assertTrue(foundRow2, "Should have found row with id=2");
    }
    
    @Test
    void testLoadTableWithMaxRows()
    {
        AspectMapHierarchy table = catalog.loadTable("test_table", 1);
        
        assertNotNull(table, "Table hierarchy should not be null");
        assertEquals(1, table.size(), "Should have only 1 row loaded due to maxRows limit");
        
        // Verify the loaded row
        Aspect aspect = table.values().iterator().next();
        Property idProp = aspect.get("id");
        assertNotNull(idProp, "id property should exist");
        assertEquals(1, (Long) idProp.unsafeRead(), "Should have loaded the first row (id=1)");
    }
    
    @Test
    void testLoadTableNonExistentTable()
    {
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> catalog.loadTable("non_existent_table", -1),
            "Should throw RuntimeException for non-existent table");
        
        assertTrue(exception.getMessage().contains("Failed to load table data"),
            "Exception message should indicate failure to load table data");
    }
    
    @Test
    void testLoadTableZeroMaxRows()
    {
        AspectMapHierarchy table = catalog.loadTable("test_table", 0);
        
        assertNotNull(table, "Table hierarchy should not be null");
        assertEquals(0, table.size(), "Should have no rows loaded when maxRows is 0");
    }

    @Test
    void constructor_LoadsTestTable()
    {
        // Should be a single test table and the Flyway schema table
        assertEquals(3, catalog.getTables().size());

        AspectDef aspectDef = catalog.getTableDef("test_table");
        assertEquals(9, aspectDef.propertyDefs().size());
    }

    @Test
    void testAspectTableMapping() throws Exception
    {
        // Initialize Cheap schema for this test
        initializeCheapSchema();

        // Create a PostgresCatalog with a table mapping
        PostgresCatalog persistCatalog = new PostgresCatalog(dataSource);

        // Create AspectDef matching test_aspect_mapping columns
        AspectDef aspectDef = persistCatalog.loadTableDef("test_aspect_mapping");

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

        // Add mapping to catalog
        persistCatalog.addAspectTableMapping(mapping);

        // Create DAO
        CatalogDao dao = new CatalogDao(dataSource);
        net.netbeing.cheap.util.CheapFactory factory = new net.netbeing.cheap.util.CheapFactory();

        // Create aspect map hierarchy
        AspectMapHierarchy hierarchy = factory.createAspectMapHierarchy(persistCatalog, aspectDef);

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
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM test_aspect_mapping");
        }

        // Save catalog (should use mapped table for the hierarchy)
        dao.saveCatalog(persistCatalog);

        // Verify the data was saved to the mapped table by querying directly
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
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
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM aspect WHERE entity_id IN (?, ?)")) {
            stmt.setObject(1, entityId1);
            stmt.setObject(2, entityId2);
            try (var rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Should not have saved to aspect table when using mapped table");
            }
        }
    }

}