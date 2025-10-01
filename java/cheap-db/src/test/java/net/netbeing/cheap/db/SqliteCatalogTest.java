package net.netbeing.cheap.db;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqliteCatalogTest {
    
    private SQLiteDataSource dataSource;
    private SqliteCatalog catalog;
    
    @BeforeEach
    void setUp() {
        String testDbPath = Paths.get("src", "test", "resources", "test-dbeaver.sqlite").toString();
        String url = "jdbc:sqlite:" + testDbPath;
        
        dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);
        
        catalog = new SqliteCatalog(dataSource);
    }
    
    @Test
    void testLoadDb() {
        assertNotNull(catalog, "Catalog should not be null");
        
        List<String> tables = catalog.getTables();
        assertNotNull(tables, "Tables list should not be null");
        assertEquals(1, tables.size(), "Should have exactly one table");
        assertEquals("test_data", tables.getFirst(), "Table should be named 'test_data'");
    }
    
    @Test
    void testLoadDbStaticMethod() {
        SqliteCatalog staticCatalog = SqliteCatalog.loadDb(dataSource);
        
        assertNotNull(staticCatalog, "Catalog should not be null");
        
        List<String> tables = staticCatalog.getTables();
        assertNotNull(tables, "Tables list should not be null");
        assertEquals(1, tables.size(), "Should have exactly one table");
        assertEquals("test_data", tables.getFirst(), "Table should be named 'test_data'");
    }
    
    @Test
    void testLoadDbInvalidPath() {
        String invalidPath = "/invalid/path/that/cannot/exist/file.db";
        String url = "jdbc:sqlite:" + invalidPath;

        dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> SqliteCatalog.loadDb(dataSource),
            "Should throw RuntimeException for invalid path");
        
        assertTrue(exception.getMessage().contains("Failed to load tables from database"),
            "Exception message should indicate failure to load database");
    }
    
    @Test
    void testLoadTableDef() {
        
        AspectDef tableDef = catalog.loadTableDef("test_data");
        
        assertNotNull(tableDef, "Table definition should not be null");
        assertEquals("test_data", tableDef.name(), "AspectDef name should match table name");
        
        List<? extends PropertyDef> properties = tableDef.propertyDefs().stream().toList();
        assertEquals(5, properties.size(), "Should have 5 columns");
        
        // Verify each column
        PropertyDef idProp = tableDef.propertyDef("id");
        assertNotNull(idProp, "id column should exist");
        assertEquals("id", idProp.name(), "Column name should be 'id'");
        assertEquals(PropertyType.Integer, idProp.type(), "id should be Integer type");
        assertFalse(idProp.isNullable(), "id should not be nullable");
        
        PropertyDef numericProp = tableDef.propertyDef("numeric_col");
        assertNotNull(numericProp, "numeric_col should exist");
        assertEquals(PropertyType.Float, numericProp.type(), "numeric_col should be Float type");
        assertTrue(numericProp.isNullable(), "numeric_col should be nullable");
        
        PropertyDef realProp = tableDef.propertyDef("real_col");
        assertNotNull(realProp, "real_col should exist");
        assertEquals(PropertyType.Float, realProp.type(), "real_col should be Float type");
        
        PropertyDef textProp = tableDef.propertyDef("text_col");
        assertNotNull(textProp, "text_col should exist");
        assertEquals(PropertyType.Text, textProp.type(), "text_col should be Text type");
        
        PropertyDef blobProp = tableDef.propertyDef("blob_col");
        assertNotNull(blobProp, "blob_col should exist");
        assertEquals(PropertyType.BLOB, blobProp.type(), "blob_col should be BLOB type");
    }
    
    @Test
    void testLoadTableDefNonExistentTable() {
        
        AspectDef tableDef = catalog.loadTableDef("non_existent_table");
        
        assertNotNull(tableDef, "Table definition should not be null even for non-existent table");
        assertEquals("non_existent_table", tableDef.name(), "AspectDef name should match requested table name");
        assertTrue(tableDef.propertyDefs().isEmpty(), "Should have no columns for non-existent table");
    }
    
    @Test
    void testLoadTableDefWithoutDataSource() {
        SqliteCatalog emptyCatalog = new SqliteCatalog();
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> emptyCatalog.loadTableDef("test_data"),
            "Should throw IllegalStateException when DataSource not set");
        
        assertTrue(exception.getMessage().contains("DataSource not set"),
            "Exception message should indicate DataSource not set");
    }
    
    @Test
    void testLoadTable() {
        
        AspectMapHierarchy table = catalog.loadTable("test_data", -1);
        
        assertNotNull(table, "Table hierarchy should not be null");
        assertEquals("test_data", table.name(), "Hierarchy name should match expected pattern");
        assertEquals(HierarchyType.ASPECT_MAP, table.type(), "Should be ASPECT_MAP hierarchy type");
        assertEquals("test_data", table.aspectDef().name(), "AspectDef name should match table name");
        
        // Check that we loaded all rows (should be 2)
        assertEquals(2, table.size(), "Should have 2 rows loaded");
        
        // Verify data in the loaded aspects
        boolean foundRow1 = false, foundRow2 = false;
        
        for (Aspect aspect : table.values()) {
            Property idProp = aspect.get("id");
            assertNotNull(idProp, "id property should exist");
            
            Long idValue = (Long) idProp.unsafeRead();
            if (idValue == 1L) {
                foundRow1 = true;
                
                Property numericProp = aspect.get("numeric_col");
                assertEquals(1.0, ((Double) numericProp.unsafeRead()), "numeric_col should be 1.0");
                
                Property realProp = aspect.get("real_col");
                assertEquals(1.5, ((Double) realProp.unsafeRead()), "real_col should be 1.5");
                
                Property textProp = aspect.get("text_col");
                assertEquals("one", textProp.unsafeRead(), "text_col should be 'one'");
                
                Property blobProp = aspect.get("blob_col");
                assertNotNull(blobProp.unsafeRead(), "blob_col should not be null");
                
            } else if (idValue == 2L) {
                foundRow2 = true;
                
                Property numericProp = aspect.get("numeric_col");
                assertEquals(2.0, ((Double) numericProp.unsafeRead()), "numeric_col should be 2.0");
                
                Property realProp = aspect.get("real_col");
                assertEquals(2.5, ((Double) realProp.unsafeRead()), "real_col should be 2.5");
                
                Property textProp = aspect.get("text_col");
                assertEquals("two", textProp.unsafeRead(), "text_col should be 'two'");
            }
        }
        
        assertTrue(foundRow1, "Should have found row with id=1");
        assertTrue(foundRow2, "Should have found row with id=2");
    }
    
    @Test
    void testLoadTableWithMaxRows() {
        
        AspectMapHierarchy table = catalog.loadTable("test_data", 1);
        
        assertNotNull(table, "Table hierarchy should not be null");
        assertEquals(1, table.size(), "Should have only 1 row loaded due to maxRows limit");
        
        // Verify the loaded row
        Aspect aspect = table.values().iterator().next();
        Property idProp = aspect.get("id");
        assertNotNull(idProp, "id property should exist");
        assertEquals(1L, (Long) idProp.unsafeRead(), "Should have loaded the first row (id=1)");
    }
    
    @Test
    void testLoadTableNonExistentTable() {
        
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> catalog.loadTable("non_existent_table", -1),
            "Should throw RuntimeException for non-existent table");
        
        assertTrue(exception.getMessage().contains("Failed to load table data"),
            "Exception message should indicate failure to load table data");
    }
    
    @Test
    void testLoadTableWithoutDataSource() {
        SqliteCatalog emptyCatalog = new SqliteCatalog();
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> emptyCatalog.loadTable("test_data", -1),
            "Should throw IllegalStateException when DataSource not set");
        
        assertTrue(exception.getMessage().contains("DataSource not set"),
            "Exception message should indicate DataSource not set");
    }
    
    @Test
    void testLoadTableZeroMaxRows() {
        
        AspectMapHierarchy table = catalog.loadTable("test_data", 0);
        
        assertNotNull(table, "Table hierarchy should not be null");
        assertEquals(0, table.size(), "Should have no rows loaded when maxRows is 0");
    }
}