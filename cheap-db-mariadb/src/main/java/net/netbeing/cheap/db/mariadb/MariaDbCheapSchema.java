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

import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for executing MariaDB DDL scripts to manage the Cheap database schema.
 * Provides methods to create, audit, drop, and truncate the Cheap schema in MariaDB databases.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DataSource dataSource = createMariaDbDataSource("jdbc:mariadb://localhost/cheapdb");
 * MariaDbCheapSchema schema = new MariaDbCheapSchema();
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
public class MariaDbCheapSchema
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
        try (var inputStream = MariaDbCheapSchema.class.getResourceAsStream(resourcePath)) {
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
     * MariaDB requires each statement to be executed separately.
     *
     * @param dataSource the data source to execute the DDL against
     * @param ddlContent the DDL content to execute
     * @throws SQLException if database operation fails
     */
    private static void executeDdl(@NotNull DataSource dataSource, String ddlContent) throws SQLException
    {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            List<String> statements = parseSqlStatements(ddlContent);
            for (String sql : statements) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql);
                }
            }
        }
    }

    /**
     * Parses SQL statements from DDL content, properly handling comments and string literals.
     * Splits on semicolons that are not inside comments or strings.
     */
    private static List<String> parseSqlStatements(String ddlContent)
    {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();

        int i = 0;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inSingleQuoteString = false;
        boolean inDoubleQuoteString = false;
        boolean inBacktickString = false;

        while (i < ddlContent.length()) {
            char c = ddlContent.charAt(i);
            char next = (i + 1 < ddlContent.length()) ? ddlContent.charAt(i + 1) : '\0';

            // Handle line comments
            if (!inSingleQuoteString && !inDoubleQuoteString && !inBacktickString && !inBlockComment && c == '-' && next == '-') {
                inLineComment = true;
                i += 2;
                continue; // Skip the comment entirely
            }

            // End of line comment
            if (inLineComment && (c == '\n' || c == '\r')) {
                inLineComment = false;
                currentStatement.append(c);
                i++;
                continue;
            }

            // Skip characters inside line comments
            if (inLineComment) {
                i++;
                continue;
            }

            // Handle block comments
            if (!inSingleQuoteString && !inDoubleQuoteString && !inBacktickString && !inLineComment && c == '/' && next == '*') {
                inBlockComment = true;
                i += 2;
                continue; // Skip the comment entirely
            }

            // End of block comment
            if (inBlockComment && c == '*' && next == '/') {
                inBlockComment = false;
                i += 2;
                continue;
            }

            // Skip characters inside block comments
            if (inBlockComment) {
                i++;
                continue;
            }

            // Handle string literals
            if (!inLineComment && !inBlockComment) {
                if (c == '\'' && !inDoubleQuoteString && !inBacktickString) {
                    // Check for escaped quote
                    if (inSingleQuoteString && next == '\'') {
                        currentStatement.append(c).append(next);
                        i += 2;
                        continue;
                    }
                    inSingleQuoteString = !inSingleQuoteString;
                    currentStatement.append(c);
                    i++;
                    continue;
                }

                if (c == '"' && !inSingleQuoteString && !inBacktickString) {
                    inDoubleQuoteString = !inDoubleQuoteString;
                    currentStatement.append(c);
                    i++;
                    continue;
                }

                if (c == '`' && !inSingleQuoteString && !inDoubleQuoteString) {
                    inBacktickString = !inBacktickString;
                    currentStatement.append(c);
                    i++;
                    continue;
                }
            }

            // Check for statement terminator
            if (!inSingleQuoteString && !inDoubleQuoteString && !inBacktickString && !inLineComment && !inBlockComment && c == ';') {
                String stmt = currentStatement.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                currentStatement = new StringBuilder();
                i++;
                continue;
            }

            // Default: append character
            currentStatement.append(c);
            i++;
        }

        // Add final statement if any
        String finalStmt = currentStatement.toString().trim();
        if (!finalStmt.isEmpty()) {
            statements.add(finalStmt);
        }

        return statements;
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
        String ddlContent = loadDdlResource("/db/schemas/mariadb/mariadb-cheap.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/mariadb/mariadb-cheap-audit.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/mariadb/mariadb-cheap-drop.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/mariadb/mariadb-cheap-truncate.sql");
        executeDdl(dataSource, ddlContent);
    }
}
