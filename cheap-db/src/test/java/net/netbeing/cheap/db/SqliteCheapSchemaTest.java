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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SqliteCheapSchemaTest {

    private DataSource dataSource;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        // Create an in-memory SQLite database
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite::memory:");
        this.dataSource = ds;
        // Keep connection open to prevent in-memory database deletion
        this.connection = ds.getConnection();
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Close the connection (which will delete the in-memory database)
        if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
        }
        this.connection = null;
        this.dataSource = null;
    }

    @Test
    void testAllSchemaExecution() throws SQLException {
        SqliteDao sqliteDao = new SqliteDao(dataSource);

        // Execute the main schema DDL using SqliteDao with connection
        sqliteDao.executeMainSchemaDdl(connection);

        // Verify that key tables were created
        assertTrue(tableExists(connection, "aspect_def"), "aspect_def table should exist");
        assertTrue(tableExists(connection, "property_def"), "property_def table should exist");
        assertTrue(tableExists(connection, "entity"), "entity table should exist");
        assertTrue(tableExists(connection, "catalog"), "catalog table should exist");
        assertTrue(tableExists(connection, "catalog_aspect_def"), "catalog_aspect_def table should exist");
        assertTrue(tableExists(connection, "hierarchy"), "hierarchy table should exist");
        assertTrue(tableExists(connection, "aspect"), "aspect table should exist");
        assertTrue(tableExists(connection, "property_value"), "property_value table should exist");

        // Execute the audit schema DDL using SqliteDao with connection
        sqliteDao.executeAuditSchemaDdl(connection);

        // Verify that audit columns were added to key tables
        assertTrue(columnExists(connection, "aspect_def", "created_at"), "aspect_def should have created_at column");
        assertTrue(columnExists(connection, "aspect_def", "updated_at"), "aspect_def should have updated_at column");
        assertTrue(columnExists(connection, "property_def", "created_at"), "property_def should have created_at column");
        assertTrue(columnExists(connection, "property_def", "updated_at"), "property_def should have updated_at column");
        assertTrue(columnExists(connection, "catalog", "created_at"), "catalog should have created_at column");
        assertTrue(columnExists(connection, "catalog", "updated_at"), "catalog should have updated_at column");
        assertTrue(columnExists(connection, "aspect", "created_at"), "aspect should have created_at column");
        assertTrue(columnExists(connection, "aspect", "updated_at"), "aspect should have updated_at column");

        // Verify that the update triggers were created
        assertTrue(triggerExists(connection, "update_aspect_def_updated_at"), "update_aspect_def_updated_at trigger should exist");
        assertTrue(triggerExists(connection, "update_catalog_updated_at"), "update_catalog_updated_at trigger should exist");

        // Execute the drop schema DDL using SqliteDao with connection
        sqliteDao.executeDropSchemaDdl(connection);

        // Verify that key tables have been dropped
        assertFalse(tableExists(connection, "aspect_def"), "aspect_def table should be dropped");
        assertFalse(tableExists(connection, "property_def"), "property_def table should be dropped");
        assertFalse(tableExists(connection, "entity"), "entity table should be dropped");
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

        // Verify that the update triggers were dropped
        assertFalse(triggerExists(connection, "update_aspect_def_updated_at"), "update_aspect_def_updated_at trigger should be dropped");
        assertFalse(triggerExists(connection, "update_catalog_updated_at"), "update_catalog_updated_at trigger should be dropped");
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

    private boolean triggerExists(Connection connection, String triggerName) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type='trigger' AND name = ?";
        try (var stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, triggerName);
            try (var rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Test
    void testTruncateAllTables() throws SQLException {
        SqliteDao sqliteDao = new SqliteDao(dataSource);

        // Execute the main schema DDL
        sqliteDao.executeMainSchemaDdl(connection);

        Connection conn = connection;
            // Populate all tables with at least 1 row

            // Insert into entity
            String entityId = UUID.randomUUID().toString();
            executeUpdate(conn, "INSERT INTO entity (entity_id) VALUES (?)", entityId);

            // Insert into aspect_def
            String aspectDefId = UUID.randomUUID().toString();
            executeUpdate(conn, "INSERT INTO aspect_def (aspect_def_id, name, hash_version, is_readable, is_writable, can_add_properties, can_remove_properties) VALUES (?, ?, ?, ?, ?, ?, ?)",
                aspectDefId, "test_aspect", "hash123", 1, 1, 0, 0);

            // Insert into property_def
            executeUpdate(conn, "INSERT INTO property_def (aspect_def_id, name, property_type, default_value, has_default_value, is_readable, is_writable, is_nullable, is_removable, is_multivalued) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                aspectDefId, "test_prop", "STR", null, 0, 1, 1, 1, 0, 0);

            // Insert into catalog
            String catalogId = UUID.randomUUID().toString();
            executeUpdate(conn, "INSERT INTO catalog (catalog_id, species, uri, upstream_catalog_id, version_number) VALUES (?, ?, ?, ?, ?)",
                catalogId, "SINK", null, null, 1L);

            // Insert into catalog_aspect_def
            executeUpdate(conn, "INSERT INTO catalog_aspect_def (catalog_id, aspect_def_id) VALUES (?, ?)",
                catalogId, aspectDefId);

            // Insert into hierarchy
            executeUpdate(conn, "INSERT INTO hierarchy (catalog_id, name, hierarchy_type, version_number) VALUES (?, ?, ?, ?)",
                catalogId, "test_hierarchy", "EL", 1L);

            // Insert into aspect
            executeUpdate(conn, "INSERT INTO aspect (entity_id, aspect_def_id, catalog_id, hierarchy_name) VALUES (?, ?, ?, ?)",
                entityId, aspectDefId, catalogId, "test_hierarchy");

            // Insert into property_value
            executeUpdate(conn, "INSERT INTO property_value (entity_id, aspect_def_id, catalog_id, property_name, value_text, value_binary) VALUES (?, ?, ?, ?, ?, ?)",
                entityId, aspectDefId, catalogId, "test_prop", "test_value", null);

            // Insert into hierarchy_entity_list
            executeUpdate(conn, "INSERT INTO hierarchy_entity_list (catalog_id, hierarchy_name, entity_id, list_order) VALUES (?, ?, ?, ?)",
                catalogId, "test_hierarchy", entityId, 0);

            // Insert into hierarchy_entity_set
            String entityId2 = UUID.randomUUID().toString();
            executeUpdate(conn, "INSERT INTO entity (entity_id) VALUES (?)", entityId2);
            executeUpdate(conn, "INSERT INTO hierarchy (catalog_id, name, hierarchy_type, version_number) VALUES (?, ?, ?, ?)",
                catalogId, "test_set", "ES", 1L);
            executeUpdate(conn, "INSERT INTO hierarchy_entity_set (catalog_id, hierarchy_name, entity_id, set_order) VALUES (?, ?, ?, ?)",
                catalogId, "test_set", entityId2, 0);

            // Insert into hierarchy_entity_directory
            executeUpdate(conn, "INSERT INTO hierarchy (catalog_id, name, hierarchy_type, version_number) VALUES (?, ?, ?, ?)",
                catalogId, "test_dir", "ED", 1L);
            executeUpdate(conn, "INSERT INTO hierarchy_entity_directory (catalog_id, hierarchy_name, entity_key, entity_id, dir_order) VALUES (?, ?, ?, ?, ?)",
                catalogId, "test_dir", "key1", entityId, 0);

            // Insert into hierarchy_entity_tree_node
            executeUpdate(conn, "INSERT INTO hierarchy (catalog_id, name, hierarchy_type, version_number) VALUES (?, ?, ?, ?)",
                catalogId, "test_tree", "ET", 1L);
            String nodeId = UUID.randomUUID().toString();
            executeUpdate(conn, "INSERT INTO hierarchy_entity_tree_node (node_id, catalog_id, hierarchy_name, parent_node_id, node_key, entity_id, node_path, tree_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                nodeId, catalogId, "test_tree", null, "", entityId, "", 0);

            // Insert into hierarchy_aspect_map
            executeUpdate(conn, "INSERT INTO hierarchy (catalog_id, name, hierarchy_type, version_number) VALUES (?, ?, ?, ?)",
                catalogId, "test_aspect", "AM", 1L);
            executeUpdate(conn, "INSERT INTO hierarchy_aspect_map (catalog_id, hierarchy_name, entity_id, aspect_def_id, map_order) VALUES (?, ?, ?, ?, ?)",
                catalogId, "test_aspect", entityId, aspectDefId, 0);

            // Verify all tables have at least 1 row
            assertTrue(getRowCount(conn, "entity") >= 1, "entity should have at least 1 row");
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
        sqliteDao.executeTruncateSchemaDdl(connection);

        // Verify all tables are empty
        assertEquals(0, getRowCount(conn, "entity"), "entity should be empty after truncate");
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

    private void executeUpdate(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
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
