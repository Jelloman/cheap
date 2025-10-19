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

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.AspectMapHierarchy;
import net.netbeing.cheap.model.HierarchyType;
import net.netbeing.cheap.model.Property;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import net.netbeing.cheap.util.PropertyValueAdapter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;


class MariaDbCatalogTest
{
    static final int EXPECTED_TABLE_COUNT = 1;

    static final String DB_NAME = "cheap";
    static volatile MariaDbTestDb db;

    @BeforeAll
    static void setUpAll() throws Exception
    {
        if (db == null) {
            db = new MariaDbTestDb(DB_NAME);
        }
    }

    @AfterAll
    static void tearDownAll() throws Exception
    {
        db.tearDown();
    }

    private MariaDbCatalog catalog;

    @BeforeEach
    void setUp() throws SQLException
    {
        catalog = new MariaDbCatalog(db.dataSource);
        catalog.setValueAdapter(new PropertyValueAdapter(TimeZone.getTimeZone("GMT")));
        initializeTestData();
    }

    @AfterEach
    void tearDown() throws SQLException
    {
        catalog = null;
        clearTestData();
    }

    private static void initializeTestData() throws SQLException
    {
        try (Connection connection = db.dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("SET SESSION time_zone = '+0:00'");
            // Create test_table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS test_table (" +
                    "    id INT PRIMARY KEY AUTO_INCREMENT," +
                    "    string_col VARCHAR(100)," +
                    "    integer_col INTEGER," +
                    "    float_col DOUBLE," +
                    "    date_col DATE," +
                    "    timestamp_col TIMESTAMP," +
                    "    boolean_col BOOLEAN," +
                    "    uuid_col CHAR(36)," +
                    "    blob_col BLOB" +
                    ")"
            );

            // Insert test data
            statement.execute(
                "INSERT INTO test_table " +
                    "(id, string_col, integer_col, float_col, date_col, timestamp_col, boolean_col, uuid_col, blob_col) " +
                    "VALUES(1, 'string1', 1, 1.5, '2025-01-01', '2025-01-11 18:18:18.018', true, " +
                    "'4186bfb6-b135-48af-9236-95cacdb20327', UNHEX('48F308FB637E67EC3B27B09400914BEA'))"
            );

            statement.execute(
                "INSERT INTO test_table " +
                    "(id, string_col, integer_col, float_col, date_col, timestamp_col, boolean_col, uuid_col, blob_col) " +
                    "VALUES(2, 'string2', 2, 2.5, '2025-02-02', '2025-02-02 02:02:02.002', false, " +
                    "'655a99b9-af7c-4f2f-afa8-c4801986b9d4', UNHEX('013D7D16D7AE67EC3B27B95B765C8CEB'))"
            );
        }
    }

    private static void clearTestData() throws SQLException
    {
        try (Connection connection = db.dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Create test_table
            statement.execute("TRUNCATE TABLE test_table");
        }
    }

    @Test
    void testLoadDb()
    {
        assertNotNull(catalog, "Catalog should not be null");

        List<String> tables = catalog.getTables();
        assertNotNull(tables, "Tables list should not be null");
        assertEquals(EXPECTED_TABLE_COUNT, tables.size(), "Should have one table");
        assertTrue(tables.contains("test_table"), "Should contain test_table");
    }

    @Test
    void testLoadDbStaticMethod()
    {
        MariaDbCatalog staticCatalog = new MariaDbCatalog(db.dataSource);

        assertNotNull(staticCatalog, "Catalog should not be null");

        List<String> tables = staticCatalog.getTables();
        assertNotNull(tables, "Tables list should not be null");
        assertEquals(EXPECTED_TABLE_COUNT, tables.size(), "Should have one table");
        assertTrue(tables.contains("test_table"), "Should contain test_table");
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
        assertEquals(PropertyType.String, uuidProp.type(), "uuid_col should be String type (UUID as text)");

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

            Long idValue = (Long) idProp.read();
            if (idValue == 1) {
                foundRow1 = true;

                Property stringProp = aspect.get("string_col");
                assertEquals("string1", stringProp.read(), "string_col should be 'string1'");

                Property integerProp = aspect.get("integer_col");
                assertEquals(1, (Long) integerProp.read(), "integer_col should be 1");

                Property floatProp = aspect.get("float_col");
                assertEquals(1.5, ((Double) floatProp.read()), "float_col should be 1.5");

                Property dateProp = aspect.get("date_col");
                assertEquals("2025-01-01T00:00Z[GMT]", dateProp.read().toString(), "date_col should be 2025-01-01T00:00Z[GMT]");

                Property timestampProp = aspect.get("timestamp_col");
                assertEquals("2025-01-11T18:18:18Z[GMT]", timestampProp.read().toString(), "timestamp_col should be 2025-01-11T18:18:18Z[GMT]");

                Property booleanProp = aspect.get("boolean_col");
                assertEquals(true, (Boolean) booleanProp.read(), "boolean_col should be true");

                Property uuidProp = aspect.get("uuid_col");
                assertEquals("4186bfb6-b135-48af-9236-95cacdb20327", uuidProp.read(), "uuid_col should match expected UUID");

                Property blobProp = aspect.get("blob_col");
                assertNotNull(blobProp.read(), "blob_col should not be null");

            } else if (idValue == 2) {
                foundRow2 = true;

                Property stringProp = aspect.get("string_col");
                assertEquals("string2", stringProp.read(), "string_col should be 'string2'");

                Property integerProp = aspect.get("integer_col");
                assertEquals(2, (Long) integerProp.read(), "integer_col should be 2");

                Property floatProp = aspect.get("float_col");
                assertEquals(2.5, ((Double) floatProp.read()), "float_col should be 2.5");

                Property dateProp = aspect.get("date_col");
                assertEquals("2025-02-02T00:00Z[GMT]", dateProp.read().toString(), "date_col should be 2025-02-02T00:00Z[GMT]");

                Property timestampProp = aspect.get("timestamp_col");
                assertEquals("2025-02-02T02:02:02Z[GMT]", timestampProp.read().toString(), "timestamp_col should be 2025-02-02T02:02:02Z[GMT]");

                Property booleanProp = aspect.get("boolean_col");
                assertEquals(false, (Boolean) booleanProp.read(), "boolean_col should be false");

                Property uuidProp = aspect.get("uuid_col");
                assertEquals("655a99b9-af7c-4f2f-afa8-c4801986b9d4", uuidProp.read(), "uuid_col should match expected UUID");
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
        assertEquals(1, (Long) idProp.read(), "Should have loaded the first row (id=1)");
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
        assertEquals(EXPECTED_TABLE_COUNT, catalog.getTables().size());

        AspectDef aspectDef = catalog.getTableDef("test_table");
        assertEquals(9, aspectDef.propertyDefs().size());
    }

}
