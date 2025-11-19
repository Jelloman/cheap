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

package net.netbeing.cheap.db.postgres;

import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.SingleInstancePostgresExtension;
import net.netbeing.cheap.db.CheapTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PostgresCheapSchemaTest {

    @RegisterExtension
    public static SingleInstancePostgresExtension postgres = EmbeddedPostgresExtension.singleInstance();

    @Test
    void testAllSchemaExecution() throws SQLException {
        DataSource dataSource = postgres.getEmbeddedPostgres().getPostgresDatabase();
        PostgresCheapSchema schema = new PostgresCheapSchema();

        // Execute the main schema DDL using PostgresCheapSchema
        schema.executeMainSchemaDdl(dataSource);

        try (Connection connection = dataSource.getConnection()) {

            // Verify that key tables were created
            assertTrue(tableExists(connection, "aspect_def"), "aspect_def table should exist");
            assertTrue(tableExists(connection, "property_def"), "property_def table should exist");
//            assertTrue(tableExists(connection, "entity"), "entity table should exist");
            assertTrue(tableExists(connection, "catalog"), "catalog table should exist");
            assertTrue(tableExists(connection, "catalog_aspect_def"), "catalog_aspect_def table should exist");
            assertTrue(tableExists(connection, "hierarchy"), "hierarchy table should exist");
            assertTrue(tableExists(connection, "aspect"), "aspect table should exist");
            assertTrue(tableExists(connection, "property_value"), "property_value table should exist");

        }

        // Execute the audit schema DDL using PostgresCheapSchema
        schema.executeAuditSchemaDdl(dataSource);

        try (Connection connection = dataSource.getConnection()) {

            // Verify that audit columns were added to key tables
            assertTrue(columnExists(connection, "aspect_def", "created_at"), "aspect_def should have created_at column");
            assertTrue(columnExists(connection, "aspect_def", "updated_at"), "aspect_def should have updated_at column");
            assertTrue(columnExists(connection, "property_def", "created_at"), "property_def should have created_at column");
            assertTrue(columnExists(connection, "property_def", "updated_at"), "property_def should have updated_at column");
            assertTrue(columnExists(connection, "catalog", "created_at"), "catalog should have created_at column");
            assertTrue(columnExists(connection, "catalog", "updated_at"), "catalog should have updated_at column");
            assertTrue(columnExists(connection, "aspect", "created_at"), "aspect should have created_at column");
            assertTrue(columnExists(connection, "aspect", "updated_at"), "aspect should have updated_at column");

            // Verify that the update trigger function was created
            assertTrue(functionExists(connection, "update_updated_at_column"), "update_updated_at_column function should exist");

        }

        // Execute the drop schema DDL using PostgresCheapSchema
        schema.executeDropSchemaDdl(dataSource);

        try (Connection connection = dataSource.getConnection()) {

            // Verify that key tables have been dropped
            assertFalse(tableExists(connection, "aspect_def"), "aspect_def table should be dropped");
            assertFalse(tableExists(connection, "property_def"), "property_def table should be dropped");
//            assertFalse(tableExists(connection, "entity"), "entity table should be dropped");
            assertFalse(tableExists(connection, "catalog"), "catalog table should be dropped");
            assertFalse(tableExists(connection, "catalog_aspect_def"), "catalog_aspect_def table should be dropped");
            assertFalse(tableExists(connection, "hierarchy"), "hierarchy table should be dropped");
            assertFalse(tableExists(connection, "aspect"), "aspect table should be dropped");
            assertFalse(tableExists(connection, "property_value"), "property_value table should be dropped");

            // Verify that hierarchy content tables have been dropped
            assertFalse(tableExists(connection, "hierarchy_entity_list"), "hierarchy_entity_list table should be dropped");
            assertFalse(tableExists(connection, "hierarchy_entity_set"), "hierarchy_entity_set table should be dropped");
            assertFalse(tableExists(connection, "hierarchy_entity_directory"), "hierarchy_entity_directory table should be dropped");
            assertFalse(tableExists(connection, "hierarchy_entity_tree_node"), "hierarchy_entity_tree_node table should be dropped");
            assertFalse(tableExists(connection, "hierarchy_aspect_map"), "hierarchy_aspect_map table should be dropped");

            // Verify that the update trigger function was dropped
            assertFalse(functionExists(connection, "update_updated_at_column"), "update_updated_at_column function should be dropped");
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (var rs = connection.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (var rs = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private boolean functionExists(Connection connection, String functionName) throws SQLException {
        String sql = "SELECT EXISTS (SELECT 1 FROM pg_proc WHERE proname = ?)";
        try (var stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, functionName);
            try (var rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    @Test
    void testTruncateAllTables() throws SQLException {
        DataSource dataSource = postgres.getEmbeddedPostgres().getPostgresDatabase();
        PostgresCheapSchema schema = new PostgresCheapSchema();

        // Execute the main schema DDL
        schema.executeMainSchemaDdl(dataSource);

        try (Connection conn = dataSource.getConnection()) {
            // Populate all tables with at least 1 row
            CheapTestFactory testFactory = new CheapTestFactory();
            UUID catalogId = UUID.randomUUID();
            testFactory.populateAllHierarchyTypes(conn, catalogId);

            // Verify all tables have at least 1 row
//            assertTrue(getRowCount(conn, "entity") >= 1, "entity should have at least 1 row");
            assertTrue(getRowCount(conn, "aspect_def") >= 1, "aspect_def should have at least 1 row");
            assertTrue(getRowCount(conn, "property_def") >= 1, "property_def should have at least 1 row");
            assertTrue(getRowCount(conn, "catalog") >= 1, "catalog should have at least 1 row");
            assertTrue(getRowCount(conn, "catalog_aspect_def") >= 1, "catalog_aspect_def should have at least 1 row");
            assertTrue(getRowCount(conn, "hierarchy") >= 1, "hierarchy should have at least 1 row");
            assertTrue(getRowCount(conn, "aspect") >= 1, "aspect should have at least 1 row");
            assertTrue(getRowCount(conn, "property_value") >= 1, "property_value should have at least 1 row");
            assertTrue(getRowCount(conn, "hierarchy_entity_list") >= 1, "hierarchy_entity_list should have at least 1 row");
            assertTrue(getRowCount(conn, "hierarchy_entity_set") >= 1, "hierarchy_entity_set should have at least 1 row");
            assertTrue(getRowCount(conn, "hierarchy_entity_directory") >= 1, "hierarchy_entity_directory should have at least 1 row");
            assertTrue(getRowCount(conn, "hierarchy_entity_tree_node") >= 1, "hierarchy_entity_tree_node should have at least 1 row");
            assertTrue(getRowCount(conn, "hierarchy_aspect_map") >= 1, "hierarchy_aspect_map should have at least 1 row");

            // Execute truncate script
            schema.executeTruncateSchemaDdl(dataSource);

            // Verify all tables are empty
//            assertEquals(0, getRowCount(conn, "entity"), "entity should be empty after truncate");
            assertEquals(0, getRowCount(conn, "aspect_def"), "aspect_def should be empty after truncate");
            assertEquals(0, getRowCount(conn, "property_def"), "property_def should be empty after truncate");
            assertEquals(0, getRowCount(conn, "catalog"), "catalog should be empty after truncate");
            assertEquals(0, getRowCount(conn, "catalog_aspect_def"), "catalog_aspect_def should be empty after truncate");
            assertEquals(0, getRowCount(conn, "hierarchy"), "hierarchy should be empty after truncate");
            assertEquals(0, getRowCount(conn, "aspect"), "aspect should be empty after truncate");
            assertEquals(0, getRowCount(conn, "property_value"), "property_value should be empty after truncate");
            assertEquals(0, getRowCount(conn, "hierarchy_entity_list"), "hierarchy_entity_list should be empty after truncate");
            assertEquals(0, getRowCount(conn, "hierarchy_entity_set"), "hierarchy_entity_set should be empty after truncate");
            assertEquals(0, getRowCount(conn, "hierarchy_entity_directory"), "hierarchy_entity_directory should be empty after truncate");
            assertEquals(0, getRowCount(conn, "hierarchy_entity_tree_node"), "hierarchy_entity_tree_node should be empty after truncate");
            assertEquals(0, getRowCount(conn, "hierarchy_aspect_map"), "hierarchy_aspect_map should be empty after truncate");
        }
    }

    private int getRowCount(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
}