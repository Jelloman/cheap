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

import net.netbeing.cheap.db.AspectTableMapping;
import net.netbeing.cheap.db.CatalogPersistence;
import net.netbeing.cheap.db.postgres.PostgresDao;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Data Access Object for persisting and loading complete Catalog instances
 * to/from a SQLite database using the Cheap schema.
 * <p>
 * This DAO provides comprehensive persistence capabilities for the entire Cheap data model,
 * including catalogs, aspect definitions, hierarchies, entities, aspects, and properties.
 * It supports transactional saves and loads with full referential integrity.
 * </p>
 *
 * <h2>SQLite-Specific Considerations</h2>
 * <ul>
 *   <li><b>UUID Storage:</b> UUIDs are stored as TEXT (36-character strings)</li>
 *   <li><b>Boolean Storage:</b> Booleans are stored as INTEGER (0 or 1)</li>
 *   <li><b>Timestamp Storage:</b> Timestamps are stored as TEXT in ISO8601 format</li>
 *   <li><b>Foreign Keys:</b> Must be explicitly enabled per connection</li>
 * </ul>
 *
 * @see CatalogPersistence
 * @see PostgresDao
 * @see AspectTableMapping
 * @see CheapFactory
 * @see Catalog
 */
@SuppressWarnings("DuplicateBranchesInSwitch")
public class SqliteDao implements CatalogPersistence
{
    private final DataSource dataSource;
    private final CheapFactory factory;
    private final Map<String, AspectTableMapping> aspectTableMappings = new LinkedHashMap<>();

    /**
     * Constructs a new SqliteDao with the given data source.
     * Creates a new CheapFactory instance for object creation and entity management.
     *
     * @param dataSource the SQLite data source to use for database operations
     */
    public SqliteDao(@NotNull DataSource dataSource)
    {
        this.dataSource = dataSource;
        this.factory = new CheapFactory();
    }

    /**
     * Constructs a new SqliteDao with the given data source and factory.
     * This constructor allows sharing a CheapFactory instance across multiple DAOs
     * to maintain a consistent entity registry.
     *
     * @param dataSource the SQLite data source to use for database operations
     * @param factory the CheapFactory to use for object creation and entity management
     */
    public SqliteDao(@NotNull DataSource dataSource, @NotNull CheapFactory factory)
    {
        this.dataSource = dataSource;
        this.factory = factory;
    }

    /**
     * Adds an AspectTableMapping to enable aspects to be saved/loaded from a custom table.
     *
     * @param mapping the AspectTableMapping to add
     */
    public void addAspectTableMapping(@NotNull AspectTableMapping mapping)
    {
        aspectTableMappings.put(mapping.aspectDef().name(), mapping);
    }

    /**
     * Gets the AspectTableMapping for the given AspectDef name, if one exists.
     *
     * @param aspectDefName the AspectDef name
     * @return the AspectTableMapping, or null if not mapped
     */
    public AspectTableMapping getAspectTableMapping(@NotNull String aspectDefName)
    {
        return aspectTableMappings.get(aspectDefName);
    }

    /**
     * Creates a database table for storing aspects based on an AspectTableMapping.
     * The table structure is determined by the mapping's hasCatalogId and hasEntityId flags:
     * <ul>
     *   <li>hasCatalogId=false, hasEntityId=false: No primary key, no ID columns</li>
     *   <li>hasCatalogId=true, hasEntityId=false: catalog_id column, no primary key</li>
     *   <li>hasCatalogId=false, hasEntityId=true: entity_id column with PRIMARY KEY (entity_id)</li>
     *   <li>hasCatalogId=true, hasEntityId=true: Both columns with PRIMARY KEY (catalog_id, entity_id)</li>
     * </ul>
     *
     * @param mapping the AspectTableMapping defining the table structure
     * @throws SQLException if table creation fails
     */
    public void createTable(@NotNull AspectTableMapping mapping) throws SQLException
    {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(mapping.tableName()).append(" (\n");

        boolean hasColumns = false;

        // Add catalog_id column if needed
        if (mapping.hasCatalogId()) {
            sql.append("    catalog_id TEXT NOT NULL");
            hasColumns = true;
        }

        // Add entity_id column if needed
        if (mapping.hasEntityId()) {
            if (hasColumns) sql.append(",\n");
            sql.append("    entity_id TEXT NOT NULL");
            hasColumns = true;
        }

        // Add property columns
        for (PropertyDef propDef : mapping.aspectDef().propertyDefs()) {
            // Only add columns that are in the mapping
            String columnName = mapping.propertyToColumnMap().get(propDef.name());
            if (columnName != null) {
                if (hasColumns) sql.append(",\n");
                sql.append("    ").append(columnName).append(" ");
                sql.append(mapPropertyTypeToSqlType(propDef.type()));
                if (!propDef.isNullable()) {
                    sql.append(" NOT NULL");
                }
                hasColumns = true;
            }
        }

        // Add primary key constraint
        if (mapping.hasEntityId() && mapping.hasCatalogId()) {
            sql.append(",\n    PRIMARY KEY (catalog_id, entity_id)");
        } else if (mapping.hasEntityId()) {
            sql.append(",\n    PRIMARY KEY (entity_id)");
        }
        // No primary key if !hasEntityId

        sql.append("\n)");

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql.toString());
        }
    }

    /**
     * Maps a PropertyType to the corresponding SQLite column type.
     */
    private String mapPropertyTypeToSqlType(PropertyType type)
    {
        return switch (type) {
            case Integer -> "INTEGER";
            case Float -> "REAL";
            case Boolean -> "INTEGER";
            case String -> "TEXT";
            case Text -> "TEXT";
            case BigInteger -> "TEXT";
            case BigDecimal -> "TEXT";
            case DateTime -> "TEXT";
            case URI -> "TEXT";
            case UUID -> "TEXT";
            case CLOB -> "TEXT";
            case BLOB -> "BLOB";
        };
    }

    private static String loadDdlResource(String resourcePath) throws SQLException
    {
        try (var inputStream = SqliteDao.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new SQLException("DDL resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SQLException("Failed to load DDL resource: " + resourcePath, e);
        }
    }

    private static void executeDdl(@NotNull DataSource dataSource, String ddlContent) throws SQLException
    {
        try (Connection conn = dataSource.getConnection()) {
            executeDdl(conn, ddlContent);
        }
    }

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
    private static List<String> parseSqlStatements(String ddlContent)
    {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();

        int i = 0;
        int beginEndDepth = 0;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inString = false;

        while (i < ddlContent.length()) {
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
            if (Character.isLetterOrDigit(after) || after == '_') {
                return false;
            }
        }

        return true;
    }

    @Override
    public void saveCatalog(@NotNull Catalog catalog) throws SQLException
    {
        if (catalog == null) {
            throw new IllegalArgumentException("Catalog cannot be null");
        }

        try (Connection conn = dataSource.getConnection()) {
            // Enable foreign keys
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }

            conn.setAutoCommit(false);
            try {
                saveCatalogWithTransaction(conn, catalog);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private void saveCatalogWithTransaction(Connection conn, Catalog catalog) throws SQLException
    {
        // Save the Catalog entity itself first and foremost
        saveEntity(conn, catalog);

        // Save the Catalog table record (must be before linking aspect defs due to FK constraint)
        saveCatalogRecord(conn, catalog);

        // Save AspectDefs
        for (AspectDef aspectDef : catalog.aspectDefs()) {
            saveAspectDef(conn, aspectDef);
            // Link the AspectDef to this Catalog
            linkCatalogToAspectDef(conn, catalog.globalId(), aspectDef);
        }

        // Save all entities, aspects, and properties from hierarchies
        for (Hierarchy hierarchy : catalog.hierarchies()) {
            saveHierarchy(conn, hierarchy);
            saveHierarchyContent(conn, hierarchy);
        }
    }

    private void linkCatalogToAspectDef(Connection conn, UUID catalogId, AspectDef aspectDef) throws SQLException
    {
        String aspectDefId = getAspectDefId(conn, aspectDef.name());
        String sql = "INSERT OR IGNORE INTO catalog_aspect_def (catalog_id, aspect_def_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId.toString());
            stmt.setString(2, aspectDefId);
            stmt.executeUpdate();
        }
    }

    private void saveAspectDef(Connection conn, AspectDef aspectDef) throws SQLException
    {
        String sql =
            "INSERT INTO aspect_def (aspect_def_id, name, hash_version, is_readable, is_writable, can_add_properties, can_remove_properties) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (name) DO UPDATE SET " +
                "hash_version = excluded.hash_version, " +
                "is_readable = excluded.is_readable, " +
                "is_writable = excluded.is_writable, " +
                "can_add_properties = excluded.can_add_properties, " +
                "can_remove_properties = excluded.can_remove_properties";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, aspectDef.name());
            stmt.setLong(3, aspectDef.hash());
            stmt.setInt(4, aspectDef.isReadable() ? 1 : 0);
            stmt.setInt(5, aspectDef.isWritable() ? 1 : 0);
            stmt.setInt(6, aspectDef.canAddProperties() ? 1 : 0);
            stmt.setInt(7, aspectDef.canRemoveProperties() ? 1 : 0);
            stmt.executeUpdate();
        }

        // Save property definitions
        for (PropertyDef propDef : aspectDef.propertyDefs()) {
            savePropertyDef(conn, aspectDef, propDef);
        }
    }

    private void savePropertyDef(Connection conn, AspectDef aspectDef, PropertyDef propDef) throws SQLException
    {
        // First get the aspect_def_id
        String aspectDefId = getAspectDefId(conn, aspectDef.name());

        String sql = "INSERT INTO property_def (aspect_def_id, name, property_type, default_value, " +
            "has_default_value, is_readable, is_writable, is_nullable, is_removable, is_multivalued) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (aspect_def_id, name) DO UPDATE SET " +
            "property_type = excluded.property_type, " +
            "default_value = excluded.default_value, " +
            "has_default_value = excluded.has_default_value, " +
            "is_readable = excluded.is_readable, " +
            "is_writable = excluded.is_writable, " +
            "is_nullable = excluded.is_nullable, " +
            "is_removable = excluded.is_removable, " +
            "is_multivalued = excluded.is_multivalued";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, aspectDefId);
            stmt.setString(2, propDef.name());
            stmt.setString(3, mapPropertyTypeToDbType(propDef.type()));
            stmt.setString(4, propDef.hasDefaultValue() ? propDef.defaultValue().toString() : null);
            stmt.setInt(5, propDef.hasDefaultValue() ? 1 : 0);
            stmt.setInt(6, propDef.isReadable() ? 1 : 0);
            stmt.setInt(7, propDef.isWritable() ? 1 : 0);
            stmt.setInt(8, propDef.isNullable() ? 1 : 0);
            stmt.setInt(9, propDef.isRemovable() ? 1 : 0);
            stmt.setInt(10, propDef.isMultivalued() ? 1 : 0);
            stmt.executeUpdate();
        }
    }


    private void saveEntity(Connection conn, Entity entity) throws SQLException
    {
        String sql = "INSERT OR IGNORE INTO entity (entity_id) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entity.globalId().toString());
            stmt.executeUpdate();
        }
    }

    private void saveCatalogRecord(Connection conn, Catalog catalog) throws SQLException
    {
        String sql = "INSERT INTO catalog (catalog_id, species, uri, upstream_catalog_id, version_number) "
            + "VALUES (?, ?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id) DO UPDATE SET " +
            "species = excluded.species, " +
            "uri = excluded.uri, " +
            "upstream_catalog_id = excluded.upstream_catalog_id, " +
            "version_number = excluded.version_number";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalog.globalId().toString());
            stmt.setString(2, catalog.species().name());
            stmt.setString(3, catalog.uri() != null ? catalog.uri().toString() : null);
            stmt.setString(4, catalog.upstream() != null ? catalog.upstream().toString() : null);
            stmt.setLong(5, catalog.version());
            stmt.executeUpdate();
        }
    }

    private void saveHierarchy(Connection conn, Hierarchy hierarchy) throws SQLException
    {
        String catalogId = hierarchy.catalog().globalId().toString();

        String sql = "INSERT INTO hierarchy (catalog_id, name, hierarchy_type, version_number) " +
            "VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id, name) DO UPDATE SET " +
            "hierarchy_type = excluded.hierarchy_type, " +
            "version_number = excluded.version_number";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId);
            stmt.setString(2, hierarchy.name());
            stmt.setString(3, mapHierarchyTypeToDbType(hierarchy.type()));
            stmt.setLong(4, hierarchy.version());
            stmt.executeUpdate();
        }
    }

    private void saveHierarchyContent(Connection conn, Hierarchy hierarchy) throws SQLException
    {
        String catalogId = hierarchy.catalog().globalId().toString();
        String hierarchyName = hierarchy.name();

        switch (hierarchy.type()) {
            case ENTITY_LIST -> saveEntityListContent(conn, catalogId, hierarchyName, (EntityListHierarchy) hierarchy);
            case ENTITY_SET -> saveEntitySetContent(conn, catalogId, hierarchyName, (EntitySetHierarchy) hierarchy);
            case ENTITY_DIR -> saveEntityDirectoryContent(conn, catalogId, hierarchyName, (EntityDirectoryHierarchy) hierarchy);
            case ENTITY_TREE -> saveEntityTreeContent(conn, catalogId, hierarchyName, (EntityTreeHierarchy) hierarchy);
            case ASPECT_MAP -> saveAspectMapContent(conn, catalogId, hierarchyName, (AspectMapHierarchy) hierarchy);
        }
    }

    private void saveEntityListContent(Connection conn, String catalogId, String hierarchyName, EntityListHierarchy hierarchy) throws SQLException
    {
        String sql = "INSERT INTO hierarchy_entity_list (catalog_id, hierarchy_name, entity_id, list_order) " +
            "VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id, hierarchy_name, list_order) DO UPDATE SET " +
            "entity_id = excluded.entity_id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int order = 0;
            for (Entity entity : hierarchy) {
                saveEntity(conn, entity);
                stmt.setString(1, catalogId);
                stmt.setString(2, hierarchyName);
                stmt.setString(3, entity.globalId().toString());
                stmt.setInt(4, order++);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void saveEntitySetContent(Connection conn, String catalogId, String hierarchyName, EntitySetHierarchy hierarchy) throws SQLException
    {
        String sql = "INSERT INTO hierarchy_entity_set (catalog_id, hierarchy_name, entity_id, set_order) " +
            "VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id, hierarchy_name, entity_id) DO UPDATE SET " +
            "set_order = excluded.set_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int order = 0;
            for (Entity entity : hierarchy) {
                saveEntity(conn, entity);
                stmt.setString(1, catalogId);
                stmt.setString(2, hierarchyName);
                stmt.setString(3, entity.globalId().toString());
                stmt.setInt(4, order++);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void saveEntityDirectoryContent(Connection conn, String catalogId, String hierarchyName, EntityDirectoryHierarchy hierarchy) throws SQLException
    {
        String sql = "INSERT INTO hierarchy_entity_directory (catalog_id, hierarchy_name, entity_key, entity_id, dir_order) " +
            "VALUES (?, ?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id, hierarchy_name, entity_key) DO UPDATE SET " +
            "entity_id = excluded.entity_id, " +
            "dir_order = excluded.dir_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int order = 0;
            for (String key : hierarchy.keySet()) {
                Entity entity = hierarchy.get(key);
                if (entity != null) {
                    saveEntity(conn, entity);
                    stmt.setString(1, catalogId);
                    stmt.setString(2, hierarchyName);
                    stmt.setString(3, key);
                    stmt.setString(4, entity.globalId().toString());
                    stmt.setInt(5, order++);
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }

    private void saveEntityTreeContent(Connection conn, String catalogId, String hierarchyName, EntityTreeHierarchy hierarchy) throws SQLException
    {
        // Save tree nodes recursively
        saveTreeNode(conn, catalogId, hierarchyName, hierarchy.root(), "", "", null, 0);
    }

    private void saveTreeNode(Connection conn, String catalogId, String hierarchyName, EntityTreeHierarchy.Node node,
                              String nodeKey, String nodePath, String parentNodeId, int order) throws SQLException
    {
        String nodeId = UUID.randomUUID().toString();
        String entityId = node.value() == null ? null : node.value().globalId().toString();

        String sql = "INSERT INTO hierarchy_entity_tree_node " +
            "(node_id, catalog_id, hierarchy_name, parent_node_id, node_key, entity_id, node_path, tree_order) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id, hierarchy_name, parent_node_id, node_key) DO UPDATE SET " +
            "entity_id = excluded.entity_id, " +
            "node_path = excluded.node_path, " +
            "tree_order = excluded.tree_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nodeId);
            stmt.setString(2, catalogId);
            stmt.setString(3, hierarchyName);
            stmt.setString(4, parentNodeId);
            stmt.setString(5, nodeKey);
            stmt.setString(6, entityId);
            stmt.setString(7, nodePath);
            stmt.setInt(8, order);
            stmt.executeUpdate();
        }

        // Recursively save children
        if (!node.isLeaf()) {
            int childOrder = 0;
            for (var entry : node.entrySet()) {
                String name = entry.getKey();
                String childPath = nodePath + '/' + name;
                EntityTreeHierarchy.Node child = entry.getValue();
                if (child != null) {
                    saveTreeNode(conn, catalogId, hierarchyName, child, name, childPath, nodeId, childOrder++);
                }
            }
        }
    }

    private void saveAspectMapContent(Connection conn, String catalogId, String hierarchyName, AspectMapHierarchy hierarchy) throws SQLException
    {
        // Check if this AspectDef has a table mapping
        AspectTableMapping mapping = getAspectTableMapping(hierarchy.aspectDef().name());

        if (mapping != null) {
            saveAspectMapContentToMappedTable(conn, catalogId, hierarchyName, hierarchy, mapping);
        } else {
            saveAspectMapContentToDefaultTables(conn, catalogId, hierarchyName, hierarchy);
        }
    }

    private void saveAspectMapContentToDefaultTables(Connection conn, String catalogId, String hierarchyName, AspectMapHierarchy hierarchy) throws SQLException
    {
        String aspectSql = "INSERT INTO aspect (entity_id, aspect_def_id, catalog_id, hierarchy_name) " +
            "VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (entity_id, aspect_def_id, catalog_id) DO UPDATE SET " +
            "hierarchy_name = excluded.hierarchy_name";
        String hierarchyMapSql = "INSERT INTO hierarchy_aspect_map (catalog_id, hierarchy_name, entity_id, aspect_def_id, map_order) " +
            "VALUES (?, ?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id, hierarchy_name, entity_id) DO UPDATE SET " +
            "aspect_def_id = excluded.aspect_def_id, " +
            "map_order = excluded.map_order";

        String aspectDefId = getAspectDefId(conn, hierarchy.aspectDef().name());

        int order = 0;
        for (Entity entity : hierarchy.keySet()) {
            saveEntity(conn, entity);

            Aspect aspect = hierarchy.get(entity);
            if (aspect != null) {
                // Save aspect
                try (PreparedStatement aspectStmt = conn.prepareStatement(aspectSql)) {
                    aspectStmt.setString(1, entity.globalId().toString());
                    aspectStmt.setString(2, aspectDefId);
                    aspectStmt.setString(3, catalogId);
                    aspectStmt.setString(4, hierarchyName);
                    aspectStmt.executeUpdate();
                }

                // Save hierarchy mapping
                try (PreparedStatement mapStmt = conn.prepareStatement(hierarchyMapSql)) {
                    mapStmt.setString(1, catalogId);
                    mapStmt.setString(2, hierarchyName);
                    mapStmt.setString(3, entity.globalId().toString());
                    mapStmt.setString(4, aspectDefId);
                    mapStmt.setInt(5, order++);
                    mapStmt.executeUpdate();
                }

                // Save properties
                saveAspectProperties(conn, entity.globalId().toString(), aspectDefId, catalogId, aspect);
            }
        }
    }

    private void saveAspectMapContentToMappedTable(Connection conn, String catalogId, String hierarchyName, AspectMapHierarchy hierarchy, AspectTableMapping mapping) throws SQLException
    {
        // Pre-save cleanup based on flags
        if (!mapping.hasEntityId() && !mapping.hasCatalogId()) {
            // No IDs: DELETE all rows (SQLite doesn't have TRUNCATE)
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM " + mapping.tableName());
            }
        } else if (!mapping.hasEntityId() && mapping.hasCatalogId()) {
            // Catalog ID only: DELETE rows for this catalog
            String deleteSql = "DELETE FROM " + mapping.tableName() + " WHERE catalog_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setString(1, catalogId);
                stmt.executeUpdate();
            }
        }
        // If hasEntityId, no pre-save cleanup needed (will use ON CONFLICT)

        // Build column list and placeholders for INSERT
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        boolean firstCol = true;

        if (mapping.hasCatalogId()) {
            columns.append("catalog_id");
            placeholders.append("?");
            firstCol = false;
        }

        if (mapping.hasEntityId()) {
            if (!firstCol) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append("entity_id");
            placeholders.append("?");
            firstCol = false;
        }

        for (String columnName : mapping.propertyToColumnMap().values()) {
            if (!firstCol) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(columnName);
            placeholders.append("?");
            firstCol = false;
        }

        // Build SQL with appropriate ON CONFLICT clause
        StringBuilder sql = new StringBuilder("INSERT INTO " + mapping.tableName()
            + " (" + columns + ") VALUES (" + placeholders + ")");

        if (mapping.hasEntityId()) {
            // Only add ON CONFLICT when we have a primary key
            if (mapping.hasCatalogId()) {
                sql.append(" ON CONFLICT (catalog_id, entity_id) DO UPDATE SET ");
            } else {
                sql.append(" ON CONFLICT (entity_id) DO UPDATE SET ");
            }

            // Build UPDATE clause
            boolean first = true;
            for (String columnName : mapping.propertyToColumnMap().values()) {
                if (!first) sql.append(", ");
                sql.append(columnName).append(" = excluded.").append(columnName);
                first = false;
            }
        }
        // No ON CONFLICT clause if !hasEntityId (simple INSERT after cleanup)

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (Entity entity : hierarchy.keySet()) {
                saveEntity(conn, entity);

                Aspect aspect = hierarchy.get(entity);
                if (aspect != null) {
                    int paramIndex = 1;

                    if (mapping.hasCatalogId()) {
                        stmt.setString(paramIndex++, catalogId);
                    }

                    if (mapping.hasEntityId()) {
                        stmt.setString(paramIndex++, entity.globalId().toString());
                    }

                    for (Map.Entry<String, String> entry : mapping.propertyToColumnMap().entrySet()) {
                        String propName = entry.getKey();
                        Object value = aspect.readObj(propName);

                        PropertyDef propDef = aspect.def().propertyDef(propName);
                        if (propDef != null) {
                            setPropertyValue(stmt, paramIndex++, value, propDef.type());
                        } else {
                            stmt.setObject(paramIndex++, value);
                        }
                    }

                    stmt.executeUpdate();
                }
            }
        }
    }

    private void saveAspectProperties(Connection conn, String entityId, String aspectDefId, String catalogId, Aspect aspect) throws SQLException
    {
        // First, delete existing property values for this aspect to handle updates properly
        String deleteSql = "DELETE FROM property_value WHERE entity_id = ? AND aspect_def_id = ? AND catalog_id = ?";
        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            deleteStmt.setString(1, entityId);
            deleteStmt.setString(2, aspectDefId);
            deleteStmt.setString(3, catalogId);
            deleteStmt.executeUpdate();
        }

        String sql = "INSERT INTO property_value (entity_id, aspect_def_id, catalog_id, property_name, value_index, " +
            "value_text, value_binary) VALUES (?, ?, ?, ?, ?, ?, ?)";

        AspectDef aspectDef = aspect.def();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (PropertyDef propDef : aspectDef.propertyDefs()) {
                String propName = propDef.name();
                Object value = aspect.readObj(propName);
                PropertyType type = propDef.type();

                if (value == null) {
                    // For null values:
                    // - Single-valued: insert a row with NULL
                    // - Multivalued: don't insert any rows (null will be distinguished from empty list during load)
                    if (!propDef.isMultivalued()) {
                        stmt.setString(1, entityId);
                        stmt.setString(2, aspectDefId);
                        stmt.setString(3, catalogId);
                        stmt.setString(4, propName);
                        stmt.setInt(5, 0); // value_index

                        if (type == PropertyType.BLOB) {
                            stmt.setString(6, null); // value_text
                            stmt.setBytes(7, null);  // value_binary
                        } else {
                            stmt.setString(6, null); // value_text
                            stmt.setBytes(7, null);  // value_binary
                        }
                        stmt.addBatch();
                    }
                } else if (propDef.isMultivalued() && value instanceof List) {
                    // For multivalued properties, insert one row per value
                    @SuppressWarnings("unchecked")
                    List<Object> listValues = (List<Object>) value;

                    // If empty list, don't insert any rows (empty list represented by no rows)
                    for (int i = 0; i < listValues.size(); i++) {
                        Object itemValue = listValues.get(i);
                        stmt.setString(1, entityId);
                        stmt.setString(2, aspectDefId);
                        stmt.setString(3, catalogId);
                        stmt.setString(4, propName);
                        stmt.setInt(5, i); // value_index

                        if (type == PropertyType.BLOB) {
                            stmt.setString(6, null); // value_text
                            stmt.setBytes(7, (byte[]) itemValue); // value_binary
                        } else {
                            stmt.setString(6, convertValueToString(itemValue, type)); // value_text
                            stmt.setBytes(7, null); // value_binary
                        }
                        stmt.addBatch();
                    }
                } else {
                    // For single-valued properties, insert one row with value_index 0
                    stmt.setString(1, entityId);
                    stmt.setString(2, aspectDefId);
                    stmt.setString(3, catalogId);
                    stmt.setString(4, propName);
                    stmt.setInt(5, 0); // value_index

                    if (type == PropertyType.BLOB) {
                        stmt.setString(6, null); // value_text
                        stmt.setBytes(7, (byte[]) value); // value_binary
                    } else {
                        stmt.setString(6, convertValueToString(value, type)); // value_text
                        stmt.setBytes(7, null); // value_binary
                    }
                    stmt.addBatch();
                }
            }

            stmt.executeBatch();
        }
    }

    /**
     * Converts a property value to its string representation for storage in value_text column.
     */
    private String convertValueToString(Object value, PropertyType type) throws SQLException
    {
        return switch (type) {
            case DateTime -> convertToTimestamp(value).toString();
            case UUID -> value.toString();
            default -> value.toString();
        };
    }

    @Override
    public Catalog loadCatalog(@NotNull UUID catalogId) throws SQLException
    {
        try (Connection conn = dataSource.getConnection()) {
            // Enable foreign keys
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            return loadCatalogWithConnection(conn, catalogId);
        }
    }

    private Catalog loadCatalogWithConnection(Connection conn, UUID catalogId) throws SQLException
    {
        // Load catalog basic info
        String sql = "SELECT catalog_id, species, uri, upstream_catalog_id, version_number FROM catalog WHERE catalog_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null; // Catalog not found
                }

                CatalogSpecies species = CatalogSpecies.valueOf(rs.getString("species"));
                String uriStr = rs.getString("uri");
                URI uri = null;
                if (uriStr != null) {
                    try {
                        uri = new URI(uriStr);
                    } catch (URISyntaxException e) {
                        throw new SQLException(e);
                    }
                }
                String upstreamStr = rs.getString("upstream_catalog_id");
                UUID upstream = upstreamStr != null ? UUID.fromString(upstreamStr) : null;
                long version = rs.getLong("version_number");

                // Create catalog with version
                Catalog catalog = factory.createCatalog(catalogId, species, uri, upstream, version);

                // Load and extend catalog with AspectDefs
                loadAndExtendAspectDefs(conn, catalog);

                // Load and add all hierarchies
                loadHierarchies(conn, catalog);

                return catalog;
            }
        }
    }

    private void loadAndExtendAspectDefs(Connection conn, Catalog catalog) throws SQLException
    {
        String sql = "SELECT ad.name FROM catalog_aspect_def cad " +
            "JOIN aspect_def ad ON cad.aspect_def_id = ad.aspect_def_id " +
            "WHERE cad.catalog_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalog.globalId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String aspectDefName = rs.getString("name");
                    AspectDef aspectDef = loadAspectDef(conn, aspectDefName);
                    catalog.extend(aspectDef);
                }
            }
        }
    }

    private void loadHierarchies(Connection conn, Catalog catalog) throws SQLException
    {
        String sql = "SELECT name, hierarchy_type, version_number FROM hierarchy WHERE catalog_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalog.globalId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String typeStr = rs.getString("hierarchy_type");
                    long version = rs.getLong("version_number");
                    HierarchyType type = mapDbTypeToHierarchyType(typeStr);

                    // Check if hierarchy already exists (it may have been created by extend())
                    Hierarchy existingHierarchy = catalog.hierarchy(name);
                    if (existingHierarchy != null) {
                        // Hierarchy already exists, load content into it based on type
                        loadExistingHierarchyContent(conn, catalog.globalId().toString(), existingHierarchy, type);
                    } else {
                        // Create and load new hierarchy
                        Hierarchy hierarchy = createAndLoadHierarchy(conn, catalog, type, name, version);
                        catalog.addHierarchy(hierarchy);
                    }
                }
            }
        }
    }

    private void loadExistingHierarchyContent(Connection conn, String catalogId, Hierarchy hierarchy, HierarchyType type) throws SQLException
    {
        String hierarchyName = hierarchy.name();
        switch (type) {
            case ENTITY_LIST -> loadEntityListContent(conn, catalogId, hierarchyName, (EntityListHierarchy) hierarchy);
            case ENTITY_SET -> loadEntitySetContent(conn, catalogId, hierarchyName, (EntitySetHierarchy) hierarchy);
            case ENTITY_DIR -> loadEntityDirectoryContent(conn, catalogId, hierarchyName, (EntityDirectoryHierarchy) hierarchy);
            case ENTITY_TREE -> loadEntityTreeContent(conn, catalogId, hierarchyName, (EntityTreeHierarchy) hierarchy);
            case ASPECT_MAP -> loadAspectMapContent(conn, catalogId, hierarchyName, (AspectMapHierarchy) hierarchy);
            default -> throw new IllegalArgumentException("Unknown hierarchy type: " + type);
        }
    }

    private Hierarchy createAndLoadHierarchy(Connection conn, Catalog catalog, HierarchyType type, String hierarchyName, long version) throws SQLException
    {
        switch (type) {
            case ENTITY_LIST -> {
                EntityListHierarchy hierarchy = factory.createEntityListHierarchy(catalog, hierarchyName, version);
                loadEntityListContent(conn, catalog.globalId().toString(), hierarchyName, hierarchy);
                return hierarchy;
            }
            case ENTITY_SET -> {
                EntitySetHierarchy hierarchy = factory.createEntitySetHierarchy(catalog, hierarchyName, version);
                loadEntitySetContent(conn, catalog.globalId().toString(), hierarchyName, hierarchy);
                return hierarchy;
            }
            case ENTITY_DIR -> {
                EntityDirectoryHierarchy hierarchy = factory.createEntityDirectoryHierarchy(catalog, hierarchyName, version);
                loadEntityDirectoryContent(conn, catalog.globalId().toString(), hierarchyName, hierarchy);
                return hierarchy;
            }
            case ENTITY_TREE -> {
                Entity rootEntity = factory.createEntity();
                EntityTreeHierarchy hierarchy = factory.createEntityTreeHierarchy(catalog, hierarchyName, rootEntity);
                loadEntityTreeContent(conn, catalog.globalId().toString(), hierarchyName, hierarchy);
                return hierarchy;
            }
            case ASPECT_MAP -> {
                AspectDef aspectDef = loadAspectDefForHierarchy(conn, catalog.globalId().toString(), hierarchyName);
                AspectMapHierarchy hierarchy = factory.createAspectMapHierarchy(catalog, aspectDef, version);
                loadAspectMapContent(conn, catalog.globalId().toString(), hierarchyName, hierarchy);
                return hierarchy;
            }
            default -> throw new IllegalArgumentException("Unknown hierarchy type: " + type);
        }
    }

    private void loadEntityListContent(Connection conn, String catalogId, String hierarchyName, EntityListHierarchy hierarchy) throws SQLException
    {
        String sql = "SELECT entity_id, list_order FROM hierarchy_entity_list WHERE catalog_id = ? AND hierarchy_name = ? ORDER BY list_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId);
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID entityId = UUID.fromString(rs.getString("entity_id"));
                    Entity entity = factory.getOrRegisterNewEntity(entityId);
                    hierarchy.add(entity);
                }
            }
        }
    }

    private void loadEntitySetContent(Connection conn, String catalogId, String hierarchyName, EntitySetHierarchy hierarchy) throws SQLException
    {
        String sql = "SELECT entity_id FROM hierarchy_entity_set WHERE catalog_id = ? AND hierarchy_name = ? ORDER BY set_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId);
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID entityId = UUID.fromString(rs.getString("entity_id"));
                    Entity entity = factory.getOrRegisterNewEntity(entityId);
                    hierarchy.add(entity);
                }
            }
        }
    }

    private void loadEntityDirectoryContent(Connection conn, String catalogId, String hierarchyName, EntityDirectoryHierarchy hierarchy) throws SQLException
    {
        String sql = "SELECT entity_key, entity_id FROM hierarchy_entity_directory " +
            "WHERE catalog_id = ? AND hierarchy_name = ? ORDER BY dir_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId);
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("entity_key");
                    UUID entityId = UUID.fromString(rs.getString("entity_id"));
                    Entity entity = factory.getOrRegisterNewEntity(entityId);
                    hierarchy.put(key, entity);
                }
            }
        }
    }

    private void loadEntityTreeContent(Connection conn, String catalogId, String hierarchyName, EntityTreeHierarchy hierarchy) throws SQLException
    {
        // Load all tree nodes into a map for efficient parent-child relationship building
        Map<String, NodeRecord> nodeMap = new HashMap<>();
        String rootNodeId = null;

        String sql = "SELECT node_id, parent_node_id, node_key, entity_id " +
            "FROM hierarchy_entity_tree_node " +
            "WHERE catalog_id = ? AND hierarchy_name = ? " +
            "ORDER BY node_path, tree_order";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId);
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String nodeId = rs.getString("node_id");
                    String parentNodeId = rs.getString("parent_node_id");
                    String nodeKey = rs.getString("node_key");
                    String entityIdStr = rs.getString("entity_id");

                    Entity entity = entityIdStr != null ? factory.getOrRegisterNewEntity(UUID.fromString(entityIdStr)) : null;
                    EntityTreeHierarchy.Node node = factory.createTreeNode(entity);

                    NodeRecord record = new NodeRecord(nodeId, parentNodeId, nodeKey, node);
                    nodeMap.put(nodeId, record);

                    // Root node has no parent
                    if (parentNodeId == null) {
                        rootNodeId = nodeId;
                    }
                }
            }
        }

        // Build the tree structure by adding children to their parents
        for (NodeRecord record : nodeMap.values()) {
            if (record.parentNodeId != null) {
                NodeRecord parentRecord = nodeMap.get(record.parentNodeId);
                if (parentRecord != null) {
                    parentRecord.node.put(record.nodeKey, record.node);
                }
            }
        }

        // Populate the root node of the hierarchy
        if (rootNodeId != null) {
            NodeRecord rootRecord = nodeMap.get(rootNodeId);
            if (rootRecord != null) {
                EntityTreeHierarchy.Node hierarchyRoot = hierarchy.root();
                // Copy all children from loaded root to hierarchy root
                hierarchyRoot.putAll(rootRecord.node);
                // Set the entity value if present
                if (rootRecord.node.value() != null) {
                    hierarchyRoot.setValue(rootRecord.node.value());
                }
            }
        }
    }

    // Helper class to hold node information during tree reconstruction
    private record NodeRecord(
        String nodeId,
        String parentNodeId,
        String nodeKey,
        EntityTreeHierarchy.Node node
    ) {}

    private void loadAspectMapContent(Connection conn, String catalogId, String hierarchyName, AspectMapHierarchy hierarchy) throws SQLException
    {
        // Check if this AspectDef has a table mapping
        AspectTableMapping mapping = getAspectTableMapping(hierarchy.aspectDef().name());

        if (mapping != null) {
            loadAspectMapContentFromMappedTable(conn, catalogId, hierarchyName, hierarchy, mapping);
        } else {
            loadAspectMapContentFromDefaultTables(conn, catalogId, hierarchyName, hierarchy);
        }
    }

    private void loadAspectMapContentFromDefaultTables(Connection conn, String catalogId, String hierarchyName, AspectMapHierarchy hierarchy) throws SQLException
    {
        String sql = "SELECT entity_id FROM hierarchy_aspect_map " +
            "WHERE catalog_id = ? AND hierarchy_name = ? ORDER BY map_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId);
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID entityId = UUID.fromString(rs.getString("entity_id"));

                    Entity entity = factory.getOrRegisterNewEntity(entityId);
                    Aspect aspect = loadAspect(conn, entity, hierarchy.aspectDef(), catalogId);
                    hierarchy.put(entity, aspect);
                }
            }
        }
    }

    private void loadAspectMapContentFromMappedTable(Connection conn, String catalogId, String hierarchyName, AspectMapHierarchy hierarchy, AspectTableMapping mapping) throws SQLException
    {
        // Build column list for SELECT
        StringBuilder columns = new StringBuilder();
        boolean firstCol = true;

        if (mapping.hasCatalogId()) {
            columns.append("catalog_id");
            firstCol = false;
        }

        if (mapping.hasEntityId()) {
            if (!firstCol) columns.append(", ");
            columns.append("entity_id");
            firstCol = false;
        }

        for (String columnName : mapping.propertyToColumnMap().values()) {
            if (!firstCol) columns.append(", ");
            columns.append(columnName);
            firstCol = false;
        }

        // Build SQL with WHERE clause for catalog_id if present
        StringBuilder sql = new StringBuilder("SELECT " + columns + " FROM " + mapping.tableName());
        if (mapping.hasCatalogId()) {
            sql.append(" WHERE catalog_id = ?");
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            if (mapping.hasCatalogId()) {
                stmt.setString(1, catalogId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Entity entity;

                    if (mapping.hasEntityId()) {
                        // Entity ID is in the table - use it
                        UUID entityId = UUID.fromString(rs.getString("entity_id"));
                        entity = factory.getOrRegisterNewEntity(entityId);
                    } else {
                        // No entity ID in table - generate a new one
                        entity = factory.createEntity();
                    }

                    Aspect aspect = factory.createPropertyMapAspect(entity, hierarchy.aspectDef());

                    // Load properties from mapped columns
                    for (Map.Entry<String, String> entry : mapping.propertyToColumnMap().entrySet()) {
                        String propName = entry.getKey();
                        String columnName = entry.getValue();

                        PropertyDef propDef = hierarchy.aspectDef().propertyDef(propName);
                        if (propDef != null) {
                            Object value = rs.getObject(columnName);
                            // Handle UUID conversion from TEXT
                            if (propDef.type() == PropertyType.UUID && value != null) {
                                value = UUID.fromString(value.toString());
                            }
                            Property property = factory.createProperty(propDef, value);
                            aspect.put(property);
                        }
                    }

                    hierarchy.put(entity, aspect);
                }
            }
        }
    }

    private Aspect loadAspect(Connection conn, Entity entity, AspectDef aspectDef, String catalogId) throws SQLException
    {
        Aspect aspect = factory.createPropertyMapAspect(entity, aspectDef);

        String sql = "SELECT property_name, value_index, value_text, value_binary " +
            "FROM property_value " +
            "WHERE entity_id = ? AND aspect_def_id = ? AND catalog_id = ? " +
            "ORDER BY property_name, value_index";

        String aspectDefId = getAspectDefId(conn, aspectDef.name());

        // Track which properties we've loaded from the database
        Set<String> loadedProperties = new HashSet<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entity.globalId().toString());
            stmt.setString(2, aspectDefId);
            stmt.setString(3, catalogId);
            try (ResultSet rs = stmt.executeQuery()) {
                String currentPropertyName = null;
                List<Object> multivaluedValues = new ArrayList<>();
                PropertyDef currentPropDef = null;

                while (rs.next()) {
                    String propertyName = rs.getString("property_name");
                    String valueText = rs.getString("value_text");
                    byte[] valueBinary = rs.getBytes("value_binary");

                    PropertyDef propDef = aspectDef.propertyDef(propertyName);
                    if (propDef == null) {
                        continue; // Skip unknown properties
                    }

                    // Check if we've moved to a new property
                    if (!propertyName.equals(currentPropertyName)) {
                        // Save the previous property if it exists
                        if (currentPropertyName != null) {
                            saveLoadedProperty(aspect, currentPropDef, multivaluedValues);
                            loadedProperties.add(currentPropertyName);
                        }

                        // Start collecting values for the new property
                        currentPropertyName = propertyName;
                        currentPropDef = propDef;
                        multivaluedValues = new ArrayList<>();
                    }

                    // Extract and add the value
                    Object value = extractPropertyValue(propDef.type(), valueText, valueBinary);
                    multivaluedValues.add(value);
                }

                // Save the last property
                if (currentPropertyName != null) {
                    saveLoadedProperty(aspect, currentPropDef, multivaluedValues);
                    loadedProperties.add(currentPropertyName);
                }
            }
        }

        // Handle properties that had no rows in the database
        // For multivalued properties: no rows means empty list
        // For single-valued properties: no rows means null (or use default value if available)
        for (PropertyDef propDef : aspectDef.propertyDefs()) {
            if (!loadedProperties.contains(propDef.name()) && propDef.isMultivalued()) {
                // Multivalued property with no rows → create with empty list
                Property property = factory.createProperty(propDef, Collections.emptyList());
                aspect.put(property);
            }
        }

        return aspect;
    }

    /**
     * Saves a loaded property to an aspect, handling both single-valued and multivalued properties.
     */
    private void saveLoadedProperty(Aspect aspect, PropertyDef propDef, List<Object> values)
    {
        if (values.isEmpty()) {
            // No rows found - for multivalued, this means empty list
            if (propDef.isMultivalued()) {
                Property property = factory.createProperty(propDef, Collections.emptyList());
                aspect.put(property);
            }
            // For single-valued, don't add the property (will use default value if available)
        } else if (propDef.isMultivalued()) {
            // Multivalued property - create property with list of all values
            Property property = factory.createProperty(propDef, new ArrayList<>(values));
            aspect.put(property);
        } else {
            // Single-valued property - use the first (and only) value
            Object value = values.getFirst();
            Property property = factory.createProperty(propDef, value);
            aspect.put(property);
        }
    }

    /**
     * Extracts a property value from the result set based on the property type.
     * Uses value_text for all types except BLOB (which uses value_binary).
     */
    private Object extractPropertyValue(PropertyType type, String valueText, byte[] valueBinary) throws SQLException
    {
        if (type == PropertyType.BLOB) {
            return valueBinary; // May be null
        }

        if (valueText == null) {
            return null;
        }

        return switch (type) {
            case Integer -> Long.parseLong(valueText);
            case Float -> Double.parseDouble(valueText);
            case Boolean -> Boolean.parseBoolean(valueText);
            case String, Text, CLOB -> valueText;
            case BigInteger -> new BigInteger(valueText);
            case BigDecimal -> new BigDecimal(valueText);
            case DateTime -> Timestamp.valueOf(valueText);
            case URI -> {
                try {
                    yield new URI(valueText);
                } catch (URISyntaxException e) {
                    throw new SQLException("Invalid URI value: " + valueText, e);
                }
            }
            case UUID -> UUID.fromString(valueText);
            case BLOB -> throw new IllegalStateException("BLOB should be handled before this switch");
        };
    }

    private AspectDef loadAspectDefForHierarchy(Connection conn, String catalogId, String hierarchyName) throws SQLException
    {
        // For AspectMap hierarchies, the hierarchy name matches the AspectDef name
        // Try to load the AspectDef directly by name
        try {
            return loadAspectDef(conn, hierarchyName);
        } catch (SQLException e) {
            throw new SQLException("Could not find AspectDef for hierarchy: " + hierarchyName + " in catalog " + catalogId, e);
        }
    }

    private AspectDef loadAspectDef(Connection conn, String aspectDefName) throws SQLException
    {
        // First load the AspectDef basic info including hash_version
        String aspectSql = "SELECT hash_version, is_readable, is_writable, can_add_properties, can_remove_properties " +
            "FROM aspect_def WHERE name = ?";

        long hashVersion;
        boolean isReadable = true, isWritable = true, canAddProperties = false, canRemoveProperties = false;

        try (PreparedStatement stmt = conn.prepareStatement(aspectSql)) {
            stmt.setString(1, aspectDefName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    hashVersion = rs.getLong("hash_version");
                    isReadable = rs.getInt("is_readable") == 1;
                    isWritable = rs.getInt("is_writable") == 1;
                    canAddProperties = rs.getInt("can_add_properties") == 1;
                    canRemoveProperties = rs.getInt("can_remove_properties") == 1;
                }
            }
        }

        // Load property definitions first
        String propSql = "SELECT pd.name, pd.property_type, pd.default_value, pd.has_default_value, " +
            "pd.is_readable, pd.is_writable, pd.is_nullable, pd.is_removable, pd.is_multivalued " +
            "FROM property_def pd JOIN aspect_def ad ON pd.aspect_def_id = ad.aspect_def_id " +
            "WHERE ad.name = ?";

        Map<String, PropertyDef> propertyDefMap = new LinkedHashMap<>();

        try (PreparedStatement stmt = conn.prepareStatement(propSql)) {
            stmt.setString(1, aspectDefName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String propName = rs.getString("name");
                    PropertyType type = mapDbTypeToPropertyType(rs.getString("property_type"));
                    String defaultValue = rs.getString("default_value");
                    boolean hasDefaultValue = rs.getInt("has_default_value") == 1;
                    boolean propReadable = rs.getInt("is_readable") == 1;
                    boolean propWritable = rs.getInt("is_writable") == 1;
                    boolean isNullable = rs.getInt("is_nullable") == 1;
                    boolean isRemovable = rs.getInt("is_removable") == 1;
                    boolean isMultivalued = rs.getInt("is_multivalued") == 1;

                    PropertyDef propDef = factory.createPropertyDef(propName, type, defaultValue, hasDefaultValue,
                        propReadable, propWritable, isNullable, isRemovable, isMultivalued);

                    propertyDefMap.put(propName, propDef);
                }
            }
        }

        // Choose the appropriate AspectDef implementation based on the flags
        AspectDef aspectDef;
        if (canAddProperties && canRemoveProperties) {
            // Fully mutable - use MutableAspectDefImpl
            aspectDef = factory.createMutableAspectDef(aspectDefName, propertyDefMap);
        } else if (!canAddProperties && !canRemoveProperties) {
            // Fully immutable - use ImmutableAspectDefImpl
            aspectDef = factory.createImmutableAspectDef(aspectDefName, propertyDefMap);
        } else {
            // Mixed mutability - use FullAspectDefImpl
            aspectDef = factory.createFullAspectDef(aspectDefName, UUID.randomUUID(), propertyDefMap,
                isReadable, isWritable, canAddProperties, canRemoveProperties);
        }

        return aspectDef;
    }

    @Override
    public boolean deleteCatalog(@NotNull UUID catalogId) throws SQLException
    {
        try (Connection conn = dataSource.getConnection()) {
            // Enable foreign keys
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }

            conn.setAutoCommit(false);
            try {
                String sql = "DELETE FROM catalog WHERE catalog_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, catalogId.toString());
                    int deleted = stmt.executeUpdate();
                    conn.commit();
                    return deleted > 0;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    @Override
    public boolean catalogExists(@NotNull UUID catalogId) throws SQLException
    {
        String sql = "SELECT 1 FROM catalog WHERE catalog_id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ===== Helper Methods =====

    /**
     * Looks up the UUID string for an AspectDef by name.
     */
    private String getAspectDefId(Connection conn, String name) throws SQLException
    {
        String sql = "SELECT aspect_def_id FROM aspect_def WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("aspect_def_id");
                }
            }
        }
        throw new SQLException("AspectDef not found: " + name);
    }

    // ===== Type Mapping Methods =====

    /**
     * Maps a PropertyType to the internal 3-letter database type code.
     */
    private String mapPropertyTypeToDbType(PropertyType type)
    {
        return switch (type) {
            case Integer -> "INT";
            case Float -> "FLT";
            case Boolean -> "BLN";
            case String -> "STR";
            case Text -> "TXT";
            case BigInteger -> "BGI";
            case BigDecimal -> "BGF";
            case DateTime -> "DAT";
            case URI -> "URI";
            case UUID -> "UID";
            case CLOB -> "CLB";
            case BLOB -> "BLB";
        };
    }

    /**
     * Maps a database type code to the corresponding PropertyType.
     */
    private PropertyType mapDbTypeToPropertyType(String dbType)
    {
        return switch (dbType) {
            case "INT" -> PropertyType.Integer;
            case "FLT" -> PropertyType.Float;
            case "BLN" -> PropertyType.Boolean;
            case "STR" -> PropertyType.String;
            case "TXT" -> PropertyType.Text;
            case "BGI" -> PropertyType.BigInteger;
            case "BGF" -> PropertyType.BigDecimal;
            case "DAT" -> PropertyType.DateTime;
            case "URI" -> PropertyType.URI;
            case "UID" -> PropertyType.UUID;
            case "CLB" -> PropertyType.CLOB;
            case "BLB" -> PropertyType.BLOB;
            default -> PropertyType.Text;
        };
    }

    /**
     * Maps a HierarchyType to the internal 2-letter database type code.
     */
    private String mapHierarchyTypeToDbType(HierarchyType type)
    {
        return switch (type) {
            case ENTITY_LIST -> "EL";
            case ENTITY_SET -> "ES";
            case ENTITY_DIR -> "ED";
            case ENTITY_TREE -> "ET";
            case ASPECT_MAP -> "AM";
        };
    }

    /**
     * Maps a database type code to the corresponding HierarchyType.
     */
    private HierarchyType mapDbTypeToHierarchyType(String dbType)
    {
        return switch (dbType) {
            case "EL" -> HierarchyType.ENTITY_LIST;
            case "ES" -> HierarchyType.ENTITY_SET;
            case "ED" -> HierarchyType.ENTITY_DIR;
            case "ET" -> HierarchyType.ENTITY_TREE;
            case "AM" -> HierarchyType.ASPECT_MAP;
            default -> throw new IllegalArgumentException("Unknown hierarchy type: " + dbType);
        };
    }

    // ===== Value Conversion Methods =====

    /**
     * Converts a DateTime value to a Timestamp for database storage.
     * Handles various date/time types including Timestamp, Date, Instant, and ZonedDateTime.
     *
     * @param value the date/time value
     * @return a Timestamp suitable for database storage
     * @throws IllegalStateException if the value type is not supported
     */
    private Timestamp convertToTimestamp(Object value)
    {
        return switch (value) {
            case Timestamp timestamp -> timestamp;
            case Date date -> new Timestamp(date.getTime());
            case Instant instant -> Timestamp.from(instant);
            case ZonedDateTime zonedDateTime -> Timestamp.from(zonedDateTime.toInstant());
            default -> throw new IllegalStateException("Unexpected value class for DateTime: " + value.getClass());
        };
    }

    /**
     * Sets a property value in a PreparedStatement, handling type conversions.
     * Used when saving aspects to custom mapped tables.
     */
    private void setPropertyValue(PreparedStatement stmt, int paramIndex, Object value, PropertyType type) throws SQLException
    {
        if (value == null) {
            stmt.setObject(paramIndex, null);
            return;
        }

        switch (type) {
            case Integer -> stmt.setLong(paramIndex, ((Number) value).longValue());
            case Float -> stmt.setDouble(paramIndex, ((Number) value).doubleValue());
            case Boolean -> stmt.setInt(paramIndex, (Boolean) value ? 1 : 0);
            case DateTime -> stmt.setString(paramIndex, convertToTimestamp(value).toString());
            case UUID -> stmt.setString(paramIndex, value.toString());
            case BLOB -> stmt.setBytes(paramIndex, (byte[]) value);
            default -> stmt.setString(paramIndex, value.toString());
        }
    }

    // ===== Static DDL Execution Methods =====

    /**
     * Executes the main Cheap schema DDL script to create all core tables and indexes.
     * This creates the foundation database structure for the Cheap data model.
     *
     * @param dataSource the data source to execute the DDL against
     * @throws SQLException if database operation fails
     */
    public void executeMainSchemaDdl(@NotNull DataSource dataSource) throws SQLException
    {
        String ddlContent = loadDdlResource("/db/schemas/sqlite-cheap.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/sqlite-cheap-audit.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/sqlite-cheap-drop.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/sqlite-cheap-truncate.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/sqlite-cheap.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/sqlite-cheap-audit.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/sqlite-cheap-drop.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/sqlite-cheap-truncate.sql");
        executeDdl(connection, ddlContent);
    }
}
