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

package net.netbeing.cheap.db.sqlite;

import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for executing SQLite DDL scripts to manage the Cheap database schema.
 * Provides methods to create, audit, drop, and truncate the Cheap schema in SQLite databases.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // With DataSource
 * DataSource dataSource = createSqliteDataSource("jdbc:sqlite:cheapdb.db");
 * SqliteCheapSchema schema = new SqliteCheapSchema();
 * schema.executeMainSchemaDdl(dataSource);
 *
 * // With Connection (for in-memory databases)
 * Connection connection = dataSource.getConnection();
 * schema.executeMainSchemaDdl(connection);
 * }</pre>
 */
public class SqliteCheapSchema
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
        try (var inputStream = SqliteCheapSchema.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new SQLException("DDL resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SQLException("Failed to load DDL resource: " + resourcePath, e);
        }
    }

    /**
     * Executes a DDL script against the database using a DataSource.
     *
     * @param dataSource the data source to execute the DDL against
     * @param ddlContent the DDL content to execute
     * @throws SQLException if database operation fails
     */
    private static void executeDdl(@NotNull DataSource dataSource, String ddlContent) throws SQLException
    {
        try (Connection conn = dataSource.getConnection()) {
            executeDdl(conn, ddlContent);
        }
    }

    /**
     * Executes a DDL script against the database using a Connection.
     *
     * @param conn the database connection to execute the DDL against
     * @param ddlContent the DDL content to execute
     * @throws SQLException if database operation fails
     */
    private static void executeDdl(@NotNull Connection conn, String ddlContent) throws SQLException
    {
        // Enable foreign keys for SQLite
        try (Statement pragmaStmt = conn.createStatement()) {
            pragmaStmt.execute("PRAGMA foreign_keys = ON");
        }

        // Split DDL into individual statements and execute each one
        // SQLite doesn't support executing multiple statements in a single call
        try (Statement stmt = conn.createStatement()) {
            executeSqlStatements(stmt, ddlContent);
        }
    }

    /**
     * Executes multiple SQL statements from a DDL script.
     * Properly parses SQL statements considering BEGIN...END blocks, comments, and string literals.
     * This is necessary because SQLite doesn't support executing multiple statements in a single call.
     */
    private static void executeSqlStatements(Statement stmt, String ddlContent) throws SQLException
    {
        List<String> statements = parseSqlStatements(ddlContent);
        for (String sql : statements) {
            // Remove all comment lines and check if there's actual SQL left
            String sqlWithoutComments = removeCommentLines(sql);
            if (!sqlWithoutComments.isEmpty()) {
                stmt.execute(sql);  // Execute the original SQL (comments are fine for SQLite)
            }
        }
    }

    /**
     * Removes comment-only lines from SQL, keeping lines with actual SQL content.
     */
    private static String removeCommentLines(String sql)
    {
        StringBuilder result = new StringBuilder();
        for (String line : sql.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                result.append(line).append('\n');
            }
        }
        return result.toString().trim();
    }

    /**
     * Parses SQL DDL content into individual statements, properly handling:
     * - BEGIN...END blocks in triggers
     * - Line comments (--)
     * - Block comments (/* ... *\/)
     * - String literals (')
     * Only semicolons outside of these contexts are treated as statement terminators.
     */
    private static List<String> parseSqlStatements(String ddlContent) // NOSONAR
    {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();

        int i = 0;
        int beginEndDepth = 0;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inString = false;

        while (i < ddlContent.length()) { // NOSONAR
            char c = ddlContent.charAt(i);
            char next = (i + 1 < ddlContent.length()) ? ddlContent.charAt(i + 1) : '\0';

            // Handle line comments
            if (!inString && !inBlockComment && c == '-' && next == '-') {
                inLineComment = true;
                currentStatement.append(c);
                i++;
                continue;
            }

            // End of line comment
            if (inLineComment && (c == '\n' || c == '\r')) {
                inLineComment = false;
                currentStatement.append(c);
                i++;
                continue;
            }

            // Handle block comments
            if (!inString && !inLineComment && c == '/' && next == '*') {
                inBlockComment = true;
                currentStatement.append(c);
                i++;
                continue;
            }

            // End of block comment
            if (inBlockComment && c == '*' && next == '/') {
                inBlockComment = false;
                currentStatement.append(c).append(next);
                i += 2;
                continue;
            }

            // Handle string literals
            if (!inLineComment && !inBlockComment && c == '\'') {
                inString = !inString;
                currentStatement.append(c);
                i++;
                continue;
            }

            // Not inside any special context, check for keywords
            if (!inString && !inLineComment && !inBlockComment) {
                // Check for BEGIN keyword
                if (isKeywordAt(ddlContent, i, "BEGIN")) {
                    beginEndDepth++;
                    currentStatement.append(ddlContent, i, i + 5);
                    i += 5;
                    continue;
                }

                // Check for END keyword
                if (isKeywordAt(ddlContent, i, "END")) {
                    if (beginEndDepth > 0) {
                        beginEndDepth--;
                    }
                    currentStatement.append(ddlContent, i, i + 3);
                    i += 3;
                    continue;
                }

                // Check for statement-terminating semicolon
                if (c == ';' && beginEndDepth == 0) {
                    // This is a statement terminator
                    String stmt = currentStatement.toString().trim();
                    if (!stmt.isEmpty()) {
                        statements.add(stmt);
                    }
                    currentStatement = new StringBuilder();
                    i++;
                    continue;
                }
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
     * Checks if a SQL keyword appears at the given position in the content.
     * Ensures the keyword is not part of a larger identifier.
     */
    private static boolean isKeywordAt(String content, int pos, String keyword)
    {
        // Check if keyword matches (case-insensitive)
        if (pos + keyword.length() > content.length()) {
            return false;
        }

        String substr = content.substring(pos, pos + keyword.length());
        if (!substr.equalsIgnoreCase(keyword)) {
            return false;
        }

        // Check that it's not part of a larger word
        // Must be preceded by whitespace/start of string
        if (pos > 0) {
            char before = content.charAt(pos - 1);
            if (Character.isLetterOrDigit(before) || before == '_') {
                return false;
            }
        }

        // Must be followed by whitespace/end of string/semicolon/parenthesis
        if (pos + keyword.length() < content.length()) {
            char after = content.charAt(pos + keyword.length());
            return !Character.isLetterOrDigit(after) && after != '_';
        }

        return true;
    }

    // ===== DataSource-based DDL Execution Methods =====

    /**
     * Executes the main Cheap schema DDL script to create all core tables and indexes.
     * This creates the foundation database structure for the Cheap data model.
     *
     * @param dataSource the data source to execute the DDL against
     * @throws SQLException if database operation fails
     */
    public void executeMainSchemaDdl(@NotNull DataSource dataSource) throws SQLException
    {
        String ddlContent = loadDdlResource("/db/schemas/sqlite/sqlite-cheap.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/sqlite/sqlite-cheap-audit.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/sqlite/sqlite-cheap-drop.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/sqlite/sqlite-cheap-truncate.sql");
        executeDdl(dataSource, ddlContent);
    }

    // ===== Connection-based DDL Execution Methods =====

    /**
     * Executes the main Cheap schema DDL script to create all core tables and indexes.
     * This creates the foundation database structure for the Cheap data model.
     * <p>
     * This overload accepts a Connection, useful for in-memory SQLite databases where
     * the connection must remain open to prevent database deletion.
     *
     * @param connection the database connection to execute the DDL against
     * @throws SQLException if database operation fails
     */
    public void executeMainSchemaDdl(@NotNull Connection connection) throws SQLException
    {
        String ddlContent = loadDdlResource("/db/schemas/sqlite/sqlite-cheap.sql");
        executeDdl(connection, ddlContent);
    }

    /**
     * Executes the audit schema DDL script to add audit columns and triggers.
     * This should be run after the main schema DDL.
     * <p>
     * This overload accepts a Connection, useful for in-memory SQLite databases where
     * the connection must remain open to prevent database deletion.
     *
     * @param connection the database connection to execute the DDL against
     * @throws SQLException if database operation fails
     */
    public void executeAuditSchemaDdl(@NotNull Connection connection) throws SQLException
    {
        String ddlContent = loadDdlResource("/db/schemas/sqlite/sqlite-cheap-audit.sql");
        executeDdl(connection, ddlContent);
    }

    /**
     * Executes the drop schema DDL script to remove all Cheap database objects.
     * This completely cleans up the Cheap schema from the database.
     * <p>
     * This overload accepts a Connection, useful for in-memory SQLite databases where
     * the connection must remain open to prevent database deletion.
     *
     * @param connection the database connection to execute the DDL against
     * @throws SQLException if database operation fails
     */
    public void executeDropSchemaDdl(@NotNull Connection connection) throws SQLException
    {
        String ddlContent = loadDdlResource("/db/schemas/sqlite/sqlite-cheap-drop.sql");
        executeDdl(connection, ddlContent);
    }

    /**
     * Executes the truncate schema DDL script to delete all data from Cheap tables
     * while preserving the schema structure. This is useful for clearing test data
     * or resetting the database without recreating all tables and constraints.
     * <p>
     * This overload accepts a Connection, useful for in-memory SQLite databases where
     * the connection must remain open to prevent database deletion.
     *
     * @param connection the database connection to execute the DDL against
     * @throws SQLException if database operation fails
     */
    public void executeTruncateSchemaDdl(@NotNull Connection connection) throws SQLException
    {
        String ddlContent = loadDdlResource("/db/schemas/sqlite/sqlite-cheap-truncate.sql");
        executeDdl(connection, ddlContent);
    }
}
