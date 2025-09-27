package net.netbeing.cheap.db;

import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.SingleInstancePostgresExtension;
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
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class PostgresCheapSchemaTest {

    @RegisterExtension
    public static SingleInstancePostgresExtension postgres = EmbeddedPostgresExtension.singleInstance();

    @Test
    void testMainSchemaExecution() throws SQLException, IOException, URISyntaxException {
        String mainSchemaPath = "/db/schemas/postgres-cheap.ddl";
        String ddlContent = loadResourceFile(mainSchemaPath);

        assertNotNull(ddlContent, "Main schema DDL content should not be null");
        assertFalse(ddlContent.trim().isEmpty(), "Main schema DDL content should not be empty");

        DataSource dataSource = postgres.getEmbeddedPostgres().getPostgresDatabase();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Execute the main schema DDL
            statement.execute(ddlContent);

            // Verify that key tables were created
            assertTrue(tableExists(connection, "aspect_def"), "aspect_def table should exist");
            assertTrue(tableExists(connection, "property_def"), "property_def table should exist");
            assertTrue(tableExists(connection, "hierarchy_def"), "hierarchy_def table should exist");
            assertTrue(tableExists(connection, "catalog_def"), "catalog_def table should exist");
            assertTrue(tableExists(connection, "entity"), "entity table should exist");
            assertTrue(tableExists(connection, "catalog"), "catalog table should exist");
            assertTrue(tableExists(connection, "hierarchy"), "hierarchy table should exist");
            assertTrue(tableExists(connection, "aspect"), "aspect table should exist");
            assertTrue(tableExists(connection, "property_value"), "property_value table should exist");
        }
    }

    @Test
    void testAuditSchemaExecution() throws SQLException, IOException, URISyntaxException {
        String mainSchemaPath = "/db/schemas/postgres-cheap.ddl";
        String auditSchemaPath = "/db/schemas/postgres-cheap-audit.ddl";

        String mainDdlContent = loadResourceFile(mainSchemaPath);
        String auditDdlContent = loadResourceFile(auditSchemaPath);

        assertNotNull(mainDdlContent, "Main schema DDL content should not be null");
        assertNotNull(auditDdlContent, "Audit schema DDL content should not be null");

        DataSource dataSource = postgres.getEmbeddedPostgres().getPostgresDatabase();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Execute the main schema DDL first
            statement.execute(mainDdlContent);

            // Execute the audit schema DDL
            statement.execute(auditDdlContent);

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
    }

    @Test
    void testBothSchemasExecutionSequentially() throws SQLException, IOException, URISyntaxException {
        String mainSchemaPath = "/db/schemas/postgres-cheap.ddl";
        String auditSchemaPath = "/db/schemas/postgres-cheap-audit.ddl";

        String mainDdlContent = loadResourceFile(mainSchemaPath);
        String auditDdlContent = loadResourceFile(auditSchemaPath);

        DataSource dataSource = postgres.getEmbeddedPostgres().getPostgresDatabase();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Execute both DDL files in sequence
            statement.execute(mainDdlContent);
            statement.execute(auditDdlContent);

            // Verify that the complete schema is functional
            assertTrue(tableExists(connection, "aspect_def"), "aspect_def table should exist");
            assertTrue(columnExists(connection, "aspect_def", "created_at"), "aspect_def should have audit columns");
            assertTrue(tableExists(connection, "property_value"), "property_value table should exist");
            assertTrue(columnExists(connection, "property_value", "updated_at"), "property_value should have audit columns");
        }
    }

    private String loadResourceFile(String resourcePath) throws IOException, URISyntaxException {
        Path path = Paths.get(getClass().getResource(resourcePath).toURI());
        return Files.readString(path);
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