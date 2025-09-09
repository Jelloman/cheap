package net.netbeing.cheap.db;

import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.junit.jupiter.api.Test;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqliteCatalogTest {
    
    @Test
    void testLoadDb() {
        String testDbPath = Paths.get("src", "test", "resources", "test-dbeaver.sqlite").toString();
        
        SqliteCatalog catalog = SqliteCatalog.loadDb(testDbPath);
        
        assertNotNull(catalog, "Catalog should not be null");
        
        List<String> tables = catalog.getTables();
        assertNotNull(tables, "Tables list should not be null");
        assertEquals(1, tables.size(), "Should have exactly one table");
        assertEquals("test_data", tables.getFirst(), "Table should be named 'test_data'");
    }
    
    @Test
    void testLoadDbInvalidPath() {
        String invalidPath = "/invalid/path/that/cannot/exist/file.db";
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> SqliteCatalog.loadDb(invalidPath),
            "Should throw RuntimeException for invalid path");
        
        assertTrue(exception.getMessage().contains("Failed to load database from path"),
            "Exception message should indicate failure to load database");
    }
    
    @Test
    void testLoadTableDef() {
        String testDbPath = Paths.get("src", "test", "resources", "test-dbeaver.sqlite").toString();
        SqliteCatalog catalog = SqliteCatalog.loadDb(testDbPath);
        
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
        String testDbPath = Paths.get("src", "test", "resources", "test-dbeaver.sqlite").toString();
        SqliteCatalog catalog = SqliteCatalog.loadDb(testDbPath);
        
        AspectDef tableDef = catalog.loadTableDef("non_existent_table");
        
        assertNotNull(tableDef, "Table definition should not be null even for non-existent table");
        assertEquals("non_existent_table", tableDef.name(), "AspectDef name should match requested table name");
        assertTrue(tableDef.propertyDefs().isEmpty(), "Should have no columns for non-existent table");
    }
    
    @Test
    void testLoadTableDefWithoutLoadDb() {
        SqliteCatalog catalog = new SqliteCatalog();
        
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> catalog.loadTableDef("test_data"),
            "Should throw IllegalStateException when database path not set");
        
        assertTrue(exception.getMessage().contains("Database path not set"),
            "Exception message should indicate database path not set");
    }
}