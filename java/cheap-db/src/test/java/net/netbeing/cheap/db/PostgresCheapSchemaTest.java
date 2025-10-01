package net.netbeing.cheap.db;

import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.SingleInstancePostgresExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class PostgresCheapSchemaTest {

    @RegisterExtension
    public static SingleInstancePostgresExtension postgres = EmbeddedPostgresExtension.singleInstance();

    @Test
    void testAllSchemaExecution() throws SQLException {
        DataSource dataSource = postgres.getEmbeddedPostgres().getPostgresDatabase();
        CatalogDao catalogDao = new CatalogDao(dataSource);

        // Execute the main schema DDL using CatalogDao
        catalogDao.executeMainSchemaDdl(dataSource);

        try (Connection connection = dataSource.getConnection()) {

            // Verify that key tables were created
            assertTrue(tableExists(connection, "aspect_def"), "aspect_def table should exist");
            assertTrue(tableExists(connection, "property_def"), "property_def table should exist");
            assertTrue(tableExists(connection, "entity"), "entity table should exist");
            assertTrue(tableExists(connection, "catalog"), "catalog table should exist");
            assertTrue(tableExists(connection, "catalog_aspect_def"), "catalog_aspect_def table should exist");
            assertTrue(tableExists(connection, "hierarchy"), "hierarchy table should exist");
            assertTrue(tableExists(connection, "aspect"), "aspect table should exist");
            assertTrue(tableExists(connection, "property_value"), "property_value table should exist");

        }

        // Execute the audit schema DDL using CatalogDao
        catalogDao.executeAuditSchemaDdl(dataSource);

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

        // Execute the drop schema DDL using CatalogDao
        catalogDao.executeDropSchemaDdl(dataSource);

        try (Connection connection = dataSource.getConnection()) {

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

    private boolean functionExists(Connection connection, String functionName) throws SQLException {
        String sql = "SELECT EXISTS (SELECT 1 FROM pg_proc WHERE proname = ?)";
        try (var stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, functionName);
            try (var rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }
}