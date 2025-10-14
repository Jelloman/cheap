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

import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class for executing PostgreSQL DDL scripts to manage the Cheap database schema.
 * Provides methods to create, audit, drop, and truncate the Cheap schema in PostgreSQL databases.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DataSource dataSource = createPostgresDataSource("jdbc:postgresql://localhost/cheapdb");
 * PostgresCheapSchema schema = new PostgresCheapSchema();
 *
 * // Create the schema
 * schema.executeMainSchemaDdl(dataSource);
 *
 * // Add audit columns
 * schema.executeAuditSchemaDdl(dataSource);
 *
 * // Clear all data
 * schema.executeTruncateSchemaDdl(dataSource);
 *
 * // Drop the schema
 * schema.executeDropSchemaDdl(dataSource);
 * }</pre>
 */
public class PostgresCheapSchema
{
    /**
     * Loads a DDL resource file from the classpath.
     *
     * @param resourcePath the path to the resource file
     * @return the DDL content as a string
     * @throws SQLException if the resource cannot be loaded
     */
    private static String loadDdlResource(String resourcePath) throws SQLException
    {
        try (var inputStream = PostgresCheapSchema.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new SQLException("DDL resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SQLException("Failed to load DDL resource: " + resourcePath, e);
        }
    }

    /**
     * Executes a DDL script against the database.
     *
     * @param dataSource the data source to execute the DDL against
     * @param ddlContent the DDL content to execute
     * @throws SQLException if database operation fails
     */
    private static void executeDdl(@NotNull DataSource dataSource, String ddlContent) throws SQLException
    {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(ddlContent);
        }
    }

    /**
     * Executes the main Cheap schema DDL script to create all core tables and indexes.
     * This creates the foundation database structure for the Cheap data model.
     *
     * @param dataSource the data source to execute the DDL against
     * @throws SQLException if database operation fails
     */
    public void executeMainSchemaDdl(@NotNull DataSource dataSource) throws SQLException
    {
        String ddlContent = loadDdlResource("/db/schemas/postgres/postgres-cheap.sql");
        executeDdl(dataSource, ddlContent);
    }

    /**
     * Executes the audit schema DDL script to add audit columns and triggers.
     * This should be run after the main schema DDL.
     *
     * @param dataSource the data source to execute the DDL against
     * @throws SQLException if database operation fails
     */
    public void executeAuditSchemaDdl(@NotNull DataSource dataSource) throws SQLException
    {
        String ddlContent = loadDdlResource("/db/schemas/postgres/postgres-cheap-audit.sql");
        executeDdl(dataSource, ddlContent);
    }

    /**
     * Executes the drop schema DDL script to remove all Cheap database objects.
     * This completely cleans up the Cheap schema from the database.
     *
     * @param dataSource the data source to execute the DDL against
     * @throws SQLException if database operation fails
     */
    public void executeDropSchemaDdl(@NotNull DataSource dataSource) throws SQLException
    {
        String ddlContent = loadDdlResource("/db/schemas/postgres/postgres-cheap-drop.sql");
        executeDdl(dataSource, ddlContent);
    }

    /**
     * Executes the truncate schema DDL script to delete all data from Cheap tables
     * while preserving the schema structure. This is useful for clearing test data
     * or resetting the database without recreating all tables and constraints.
     *
     * @param dataSource the data source to execute the DDL against
     * @throws SQLException if database operation fails
     */
    public void executeTruncateSchemaDdl(@NotNull DataSource dataSource) throws SQLException
    {
        String ddlContent = loadDdlResource("/db/schemas/postgres/postgres-cheap-truncate.sql");
        executeDdl(dataSource, ddlContent);
    }
}
