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

import net.netbeing.cheap.db.AspectTableMapping;
import net.netbeing.cheap.db.CatalogPersistence;
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
 * to/from a MariaDB database using the Cheap schema.
 * <p>
 * This DAO provides comprehensive persistence capabilities for the entire Cheap data model,
 * including catalogs, aspect definitions, hierarchies, entities, aspects, and properties.
 * It supports transactional saves and loads with full referential integrity.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Full Model Persistence:</b> Saves/loads complete catalog structures including all
 *       definitions, entities, hierarchies, aspects, and properties</li>
 *   <li><b>Transaction Management:</b> All save operations execute within transactions with
 *       automatic rollback on failure</li>
 *   <li><b>Two Persistence Modes:</b> Supports both default schema tables and custom table
 *       mappings via {@link AspectTableMapping}</li>
 *   <li><b>DDL Management:</b> Provides methods to create, audit, and drop the Cheap database schema</li>
 *   <li><b>CheapFactory Integration:</b> Uses CheapFactory for consistent object creation and
 *       entity registry management</li>
 * </ul>
 *
 * <h2>Database Schema</h2>
 * <p>
 * MariaDbDao works with a normalized schema that closely mirrors the Cheap data model:
 * </p>
 * <ul>
 *   <li><b>entity:</b> All entities with their UUIDs</li>
 *   <li><b>catalog:</b> Catalog metadata (species, URI, upstream, version)</li>
 *   <li><b>aspect_def:</b> Aspect definitions with access control flags</li>
 *   <li><b>property_def:</b> Property definitions within aspect definitions</li>
 *   <li><b>hierarchy:</b> Hierarchy metadata (name, type, version)</li>
 *   <li><b>hierarchy_entity_list, hierarchy_entity_set, hierarchy_entity_directory,
 *       hierarchy_entity_tree_node, hierarchy_aspect_map:</b> Type-specific hierarchy content tables</li>
 *   <li><b>aspect:</b> Aspect-to-entity associations</li>
 *   <li><b>property_value:</b> Property values with simplified storage (value_text and value_binary columns,
 *       one row per value for multivalued properties)</li>
 * </ul>
 *
 * <h2>Persistence Modes</h2>
 *
 * <h3>Default Mode</h3>
 * <p>
 * By default, aspects are stored in the generic {@code aspect} and {@code property_value} tables
 * using an EAV (Entity-Attribute-Value) pattern. This provides maximum flexibility for dynamic
 * schemas but may be less performant for queries.
 * </p>
 *
 * <h3>Custom Table Mapping Mode</h3>
 * <p>
 * For aspects that benefit from traditional relational table structure, you can register
 * an {@link AspectTableMapping} to store aspects in a custom table. The custom table must
 * have an {@code entity_id} column as the primary key, plus columns for each property.
 * This provides better query performance and easier integration with SQL-based tools.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Initialize DAO with data source
 * DataSource ds = createMariaDbDataSource("jdbc:mariadb://localhost/cheapdb");
 * MariaDbDao dao = new MariaDbDao(ds);
 *
 * // Create schema (first time only)
 * dao.executeMainSchemaDdl(ds);
 *
 * // Create a catalog with data
 * Catalog catalog = factory.createCatalog(UUID.randomUUID(), CatalogSpecies.SOURCE, null, null, 1);
 * AspectDef customerDef = factory.createImmutableAspectDef("Customer", props);
 * catalog.extend(customerDef);
 * // ... populate with hierarchies and data ...
 *
 * // Save catalog
 * dao.saveCatalog(catalog);
 *
 * // Load catalog
 * Catalog loaded = dao.loadCatalog(catalog.globalId());
 *
 * // Use custom table mapping for better performance
 * AspectTableMapping mapping = new AspectTableMapping(
 *     "Customer",
 *     "customers_table",
 *     Map.of("name", "customer_name", "email", "email_address")
 * );
 * dao.addAspectTableMapping(mapping);
 * dao.createAspectTable(customerDef, "customers_table");
 * }</pre>
 *
 * <h2>Transaction Handling</h2>
 * <p>
 * All save operations execute within a single database transaction. If any part of the save
 * fails, the entire transaction is rolled back to maintain data consistency. Load operations
 * do not use explicit transactions but maintain consistency through foreign key constraints.
 * </p>
 *
 * <h2>Type Mapping</h2>
 * <p>
 * PropertyTypes are mapped to MariaDB column types and internal 3-letter codes:
 * </p>
 * <ul>
 *   <li>Integer → BIGINT / INT</li>
 *   <li>Float → DOUBLE / FLT</li>
 *   <li>Boolean → BOOLEAN / BLN</li>
 *   <li>String → TEXT / STR</li>
 *   <li>DateTime → TIMESTAMP WITH TIME ZONE / DAT</li>
 *   <li>UUID → CHAR(36) / UID</li>
 *   <li>BLOB → LONGBLOB / BLB</li>
 * </ul>
 *
 * @see CatalogPersistence
 * @see AspectTableMapping
 * @see CheapFactory
 * @see Catalog
 */
@SuppressWarnings("DuplicateBranchesInSwitch")
public class MariaDbDao implements CatalogPersistence
{
    private final DataSource dataSource;
    private final CheapFactory factory;
    private final Map<String, AspectTableMapping> aspectTableMappings = new LinkedHashMap<>();

    /**
     * Constructs a new MariaDbDao with the given data source.
     * Creates a new CheapFactory instance for object creation and entity management.
     *
     * @param dataSource the MariaDB data source to use for database operations
     */
    public MariaDbDao(@NotNull DataSource dataSource)
    {
        this.dataSource = dataSource;
        this.factory = new CheapFactory();
    }

    /**
     * Constructs a new MariaDbDao with the given data source and factory.
     * This constructor allows sharing a CheapFactory instance across multiple DAOs
     * to maintain a consistent entity registry.
     *
     * @param dataSource the MariaDB data source to use for database operations
     * @param factory the CheapFactory to use for object creation and entity management
     */
    public MariaDbDao(@NotNull DataSource dataSource, @NotNull CheapFactory factory)
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
            sql.append("    catalog_id CHAR(36) NOT NULL");
            hasColumns = true;
        }

        // Add entity_id column if needed
        if (mapping.hasEntityId()) {
            if (hasColumns) sql.append(",\n");
            sql.append("    entity_id CHAR(36) NOT NULL");
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

        addAspectTableMapping(mapping);
    }

    /**
     * Maps a PropertyType to the corresponding MariaDB column type.
     */
    private String mapPropertyTypeToSqlType(PropertyType type)
    {
        return switch (type) {
            case Integer -> "BIGINT";
            case Float -> "DOUBLE";
            case Boolean -> "BOOLEAN";
            case String -> "TEXT";
            case Text -> "TEXT";
            case BigInteger -> "TEXT";
            case BigDecimal -> "TEXT";
            case DateTime -> "TIMESTAMP";
            case URI -> "TEXT";
            case UUID -> "CHAR(36)";
            case CLOB -> "TEXT";
            case BLOB -> "LONGBLOB";
        };
    }

    @Override
    public void saveCatalog(@NotNull Catalog catalog) throws SQLException
    {
        if (catalog == null) {
            throw new IllegalArgumentException("Catalog cannot be null");
        }

        try (Connection conn = dataSource.getConnection()) {
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
        UUID aspectDefId = getAspectDefId(conn, aspectDef.name());
        String sql = "INSERT INTO catalog_aspect_def (catalog_id, aspect_def_id) " +
            "VALUES (?, ?) ON DUPLICATE KEY UPDATE catalog_id=VALUES(catalog_id)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId.toString());
            stmt.setString(2, aspectDefId.toString());
            stmt.executeUpdate();
        }
    }

    private void saveAspectDef(Connection conn, AspectDef aspectDef) throws SQLException
    {
        String sql =
            "INSERT INTO aspect_def (aspect_def_id, name, hash_version, is_readable, is_writable, can_add_properties, can_remove_properties) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "hash_version = VALUES(hash_version), " +
                "is_readable = VALUES(is_readable), " +
                "is_writable = VALUES(is_writable), " +
                "can_add_properties = VALUES(can_add_properties), " +
                "can_remove_properties = VALUES(can_remove_properties)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, aspectDef.globalId().toString());
            stmt.setString(2, aspectDef.name());
            stmt.setLong(3, aspectDef.hash());
            stmt.setBoolean(4, aspectDef.isReadable());
            stmt.setBoolean(5, aspectDef.isWritable());
            stmt.setBoolean(6, aspectDef.canAddProperties());
            stmt.setBoolean(7, aspectDef.canRemoveProperties());
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
        UUID aspectDefId = getAspectDefId(conn, aspectDef.name());

        String sql = "INSERT INTO property_def (aspect_def_id, name, property_type, default_value, " +
            "has_default_value, is_readable, is_writable, is_nullable, is_removable, is_multivalued) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "property_type = VALUES(property_type), " +
            "default_value = VALUES(default_value), " +
            "has_default_value = VALUES(has_default_value), " +
            "is_readable = VALUES(is_readable), " +
            "is_writable = VALUES(is_writable), " +
            "is_nullable = VALUES(is_nullable), " +
            "is_removable = VALUES(is_removable), " +
            "is_multivalued = VALUES(is_multivalued)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, aspectDefId.toString());
            stmt.setString(2, propDef.name());
            stmt.setString(3, mapPropertyTypeToDbType(propDef.type()));
            stmt.setString(4, propDef.hasDefaultValue() ? propDef.defaultValue().toString() : null);
            stmt.setBoolean(5, propDef.hasDefaultValue());
            stmt.setBoolean(6, propDef.isReadable());
            stmt.setBoolean(7, propDef.isWritable());
            stmt.setBoolean(8, propDef.isNullable());
            stmt.setBoolean(9, propDef.isRemovable());
            stmt.setBoolean(10, propDef.isMultivalued());
            stmt.executeUpdate();
        }
    }


    private void saveEntity(Connection conn, Entity entity) throws SQLException
    {
        String sql = "INSERT INTO entity (entity_id) VALUES (?) ON DUPLICATE KEY UPDATE entity_id=VALUES(entity_id)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entity.globalId().toString());
            stmt.executeUpdate();
        }
    }

    private void saveCatalogRecord(Connection conn, Catalog catalog) throws SQLException
    {
        String sql = "INSERT INTO catalog (catalog_id, species, uri, upstream_catalog_id, version_number) "
            + "VALUES (?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "species = VALUES(species), " +
            "uri = VALUES(uri), " +
            "upstream_catalog_id = VALUES(upstream_catalog_id), " +
            "version_number = VALUES(version_number)";
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
        UUID catalogId = hierarchy.catalog().globalId();

        String sql = "INSERT INTO hierarchy (catalog_id, name, hierarchy_type, version_number) " +
            "VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "hierarchy_type = VALUES(hierarchy_type), " +
            "version_number = VALUES(version_number)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId.toString());
            stmt.setString(2, hierarchy.name());
            stmt.setString(3, mapHierarchyTypeToDbType(hierarchy.type()));
            stmt.setLong(4, hierarchy.version());
            stmt.executeUpdate();
        }
    }

    private void saveHierarchyContent(Connection conn, Hierarchy hierarchy) throws SQLException
    {
        UUID catalogId = hierarchy.catalog().globalId();
        String hierarchyName = hierarchy.name();

        switch (hierarchy.type()) {
            case ENTITY_LIST -> saveEntityListContent(conn, catalogId, hierarchyName, (EntityListHierarchy) hierarchy);
            case ENTITY_SET -> saveEntitySetContent(conn, catalogId, hierarchyName, (EntitySetHierarchy) hierarchy);
            case ENTITY_DIR -> saveEntityDirectoryContent(conn, catalogId, hierarchyName, (EntityDirectoryHierarchy) hierarchy);
            case ENTITY_TREE -> saveEntityTreeContent(conn, catalogId, hierarchyName, (EntityTreeHierarchy) hierarchy);
            case ASPECT_MAP -> saveAspectMapContent(conn, catalogId, hierarchyName, (AspectMapHierarchy) hierarchy);
        }
    }

    private void saveEntityListContent(Connection conn, UUID catalogId, String hierarchyName, EntityListHierarchy hierarchy) throws SQLException
    {
        String sql = "INSERT INTO hierarchy_entity_list (catalog_id, hierarchy_name, entity_id, list_order) " +
            "VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "entity_id = VALUES(entity_id)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int order = 0;
            for (Entity entity : hierarchy) {
                saveEntity(conn, entity);
                stmt.setString(1, catalogId.toString());
                stmt.setString(2, hierarchyName);
                stmt.setString(3, entity.globalId().toString());
                stmt.setInt(4, order++);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void saveEntitySetContent(Connection conn, UUID catalogId, String hierarchyName, EntitySetHierarchy hierarchy) throws SQLException
    {
        String sql = "INSERT INTO hierarchy_entity_set (catalog_id, hierarchy_name, entity_id, set_order) " +
            "VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "set_order = VALUES(set_order)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int order = 0;
            for (Entity entity : hierarchy) {
                saveEntity(conn, entity);
                stmt.setString(1, catalogId.toString());
                stmt.setString(2, hierarchyName);
                stmt.setString(3, entity.globalId().toString());
                stmt.setInt(4, order++);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void saveEntityDirectoryContent(Connection conn, UUID catalogId, String hierarchyName, EntityDirectoryHierarchy hierarchy) throws SQLException
    {
        String sql = "INSERT INTO hierarchy_entity_directory (catalog_id, hierarchy_name, entity_key, entity_id, dir_order) " +
            "VALUES (?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "entity_id = VALUES(entity_id), " +
            "dir_order = VALUES(dir_order)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int order = 0;
            for (String key : hierarchy.keySet()) {
                Entity entity = hierarchy.get(key);
                if (entity != null) {
                    saveEntity(conn, entity);
                    stmt.setString(1, catalogId.toString());
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

    private void saveEntityTreeContent(Connection conn, UUID catalogId, String hierarchyName, EntityTreeHierarchy hierarchy) throws SQLException
    {
        // Save tree nodes recursively
        saveTreeNode(conn, catalogId, hierarchyName, hierarchy.root(), "", "", null, 0);
    }

    private void saveTreeNode(Connection conn, UUID catalogId, String hierarchyName, EntityTreeHierarchy.Node node,
                              String nodeKey, String nodePath, UUID parentNodeId, int order) throws SQLException
    {
        UUID nodeId = UUID.randomUUID();
        UUID entityId = node.value() == null ? null : node.value().globalId();

        String sql = "INSERT INTO hierarchy_entity_tree_node " +
            "(node_id, catalog_id, hierarchy_name, parent_node_id, node_key, entity_id, node_path, tree_order) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "entity_id = VALUES(entity_id), " +
            "node_path = VALUES(node_path), " +
            "tree_order = VALUES(tree_order)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nodeId.toString());
            stmt.setString(2, catalogId.toString());
            stmt.setString(3, hierarchyName);
            stmt.setString(4, parentNodeId != null ? parentNodeId.toString() : null);
            stmt.setString(5, nodeKey);
            stmt.setString(6, entityId != null ? entityId.toString() : null);
            stmt.setString(7, nodePath); // node.path() - method needs checking
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

    private void saveAspectMapContent(Connection conn, UUID catalogId, String hierarchyName, AspectMapHierarchy hierarchy) throws SQLException
    {
        // Check if this AspectDef has a table mapping
        AspectTableMapping mapping = getAspectTableMapping(hierarchy.aspectDef().name());

        if (mapping != null) {
            saveAspectMapContentToMappedTable(conn, catalogId, hierarchyName, hierarchy, mapping);
        } else {
            saveAspectMapContentToDefaultTables(conn, catalogId, hierarchyName, hierarchy);
        }
    }

    private void saveAspectMapContentToDefaultTables(Connection conn, UUID catalogId, String hierarchyName, AspectMapHierarchy hierarchy) throws SQLException
    {
        String aspectSql = "INSERT INTO aspect (entity_id, aspect_def_id, catalog_id, hierarchy_name) " +
            "VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "hierarchy_name = VALUES(hierarchy_name)";
        String hierarchyMapSql = "INSERT INTO hierarchy_aspect_map (catalog_id, hierarchy_name, entity_id, aspect_def_id, map_order) " +
            "VALUES (?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "aspect_def_id = VALUES(aspect_def_id), " +
            "map_order = VALUES(map_order)";

        UUID aspectDefId = getAspectDefId(conn, hierarchy.aspectDef().name());

        int order = 0;
        for (Entity entity : hierarchy.keySet()) {
            saveEntity(conn, entity);

            Aspect aspect = hierarchy.get(entity);
            if (aspect != null) {
                // Save aspect
                try (PreparedStatement aspectStmt = conn.prepareStatement(aspectSql)) {
                    aspectStmt.setString(1, entity.globalId().toString());
                    aspectStmt.setString(2, aspectDefId.toString());
                    aspectStmt.setString(3, catalogId.toString());
                    aspectStmt.setString(4, hierarchyName);
                    aspectStmt.executeUpdate();
                }

                // Save hierarchy mapping
                try (PreparedStatement mapStmt = conn.prepareStatement(hierarchyMapSql)) {
                    mapStmt.setString(1, catalogId.toString());
                    mapStmt.setString(2, hierarchyName);
                    mapStmt.setString(3, entity.globalId().toString());
                    mapStmt.setString(4, aspectDefId.toString());
                    mapStmt.setInt(5, order++);
                    mapStmt.executeUpdate();
                }

                // Save properties
                saveAspectProperties(conn, entity.globalId(), aspectDefId, catalogId, aspect);
            }
        }
    }

    private void saveAspectMapContentToMappedTable(Connection conn, UUID catalogId, String hierarchyName, AspectMapHierarchy hierarchy, AspectTableMapping mapping) throws SQLException
    {
        // Pre-save cleanup based on flags
        if (!mapping.hasEntityId() && !mapping.hasCatalogId()) {
            // No IDs: TRUNCATE the entire table
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("TRUNCATE TABLE " + mapping.tableName());
            }
        } else if (!mapping.hasEntityId() && mapping.hasCatalogId()) {
            // Catalog ID only: DELETE rows for this catalog
            String deleteSql = "DELETE FROM " + mapping.tableName() + " WHERE catalog_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setString(1, catalogId.toString());
                stmt.executeUpdate();
            }
        }
        // If hasEntityId, no pre-save cleanup needed (will use ON DUPLICATE KEY UPDATE)

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

        // Build SQL with appropriate ON DUPLICATE KEY UPDATE clause
        StringBuilder sql = new StringBuilder("INSERT INTO " + mapping.tableName()
            + " (" + columns + ") VALUES (" + placeholders + ")");

        if (mapping.hasEntityId()) {
            // Only add ON DUPLICATE KEY UPDATE when we have a primary key
            sql.append(" ON DUPLICATE KEY UPDATE ");

            // Build UPDATE clause
            boolean first = true;
            for (String columnName : mapping.propertyToColumnMap().values()) {
                if (!first) sql.append(", ");
                sql.append(columnName).append(" = VALUES(").append(columnName).append(")");
                first = false;
            }
        }
        // No ON DUPLICATE KEY UPDATE clause if !hasEntityId (simple INSERT after cleanup)

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (Entity entity : hierarchy.keySet()) {
                saveEntity(conn, entity);

                Aspect aspect = hierarchy.get(entity);
                if (aspect != null) {
                    int paramIndex = 1;

                    if (mapping.hasCatalogId()) {
                        stmt.setString(paramIndex++, catalogId.toString());
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

    private void saveAspectProperties(Connection conn, UUID entityId, UUID aspectDefId, UUID catalogId, Aspect aspect) throws SQLException
    {
        // First, delete existing property values for this aspect to handle updates properly
        String deleteSql = "DELETE FROM property_value WHERE entity_id = ? AND aspect_def_id = ? AND catalog_id = ?";
        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            deleteStmt.setString(1, entityId.toString());
            deleteStmt.setString(2, aspectDefId.toString());
            deleteStmt.setString(3, catalogId.toString());
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
                        stmt.setString(1, entityId.toString());
                        stmt.setString(2, aspectDefId.toString());
                        stmt.setString(3, catalogId.toString());
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
                        stmt.setString(1, entityId.toString());
                        stmt.setString(2, aspectDefId.toString());
                        stmt.setString(3, catalogId.toString());
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
                    stmt.setString(1, entityId.toString());
                    stmt.setString(2, aspectDefId.toString());
                    stmt.setString(3, catalogId.toString());
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
                        loadExistingHierarchyContent(conn, catalog.globalId(), existingHierarchy, type);
                    } else {
                        // Create and load new hierarchy
                        Hierarchy hierarchy = createAndLoadHierarchy(conn, catalog, type, name, version);
                        catalog.addHierarchy(hierarchy);
                    }
                }
            }
        }
    }

    private void loadExistingHierarchyContent(Connection conn, UUID catalogId, Hierarchy hierarchy, HierarchyType type) throws SQLException
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
                loadEntityListContent(conn, catalog.globalId(), hierarchyName, hierarchy);
                return hierarchy;
            }
            case ENTITY_SET -> {
                EntitySetHierarchy hierarchy = factory.createEntitySetHierarchy(catalog, hierarchyName, version);
                loadEntitySetContent(conn, catalog.globalId(), hierarchyName, hierarchy);
                return hierarchy;
            }
            case ENTITY_DIR -> {
                EntityDirectoryHierarchy hierarchy = factory.createEntityDirectoryHierarchy(catalog, hierarchyName, version);
                loadEntityDirectoryContent(conn, catalog.globalId(), hierarchyName, hierarchy);
                return hierarchy;
            }
            case ENTITY_TREE -> {
                Entity rootEntity = factory.createEntity();
                EntityTreeHierarchy hierarchy = factory.createEntityTreeHierarchy(catalog, hierarchyName, rootEntity);
                loadEntityTreeContent(conn, catalog.globalId(), hierarchyName, hierarchy);
                return hierarchy;
            }
            case ASPECT_MAP -> {
                AspectDef aspectDef = loadAspectDefForHierarchy(conn, catalog.globalId(), hierarchyName);
                AspectMapHierarchy hierarchy = factory.createAspectMapHierarchy(catalog, aspectDef, version);
                loadAspectMapContent(conn, catalog.globalId(), hierarchyName, hierarchy);
                return hierarchy;
            }
            default -> throw new IllegalArgumentException("Unknown hierarchy type: " + type);
        }
    }

    private void loadEntityListContent(Connection conn, UUID catalogId, String hierarchyName, EntityListHierarchy hierarchy) throws SQLException
    {
        String sql = "SELECT entity_id, list_order FROM hierarchy_entity_list WHERE catalog_id = ? AND hierarchy_name = ? ORDER BY list_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId.toString());
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String entityIdStr = rs.getString("entity_id");
                    UUID entityId = UUID.fromString(entityIdStr);
                    Entity entity = factory.getOrRegisterNewEntity(entityId);
                    hierarchy.add(entity);
                }
            }
        }
    }

    private void loadEntitySetContent(Connection conn, UUID catalogId, String hierarchyName, EntitySetHierarchy hierarchy) throws SQLException
    {
        String sql = "SELECT entity_id FROM hierarchy_entity_set WHERE catalog_id = ? AND hierarchy_name = ? ORDER BY set_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId.toString());
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String entityIdStr = rs.getString("entity_id");
                    UUID entityId = UUID.fromString(entityIdStr);
                    Entity entity = factory.getOrRegisterNewEntity(entityId);
                    hierarchy.add(entity);
                }
            }
        }
    }

    private void loadEntityDirectoryContent(Connection conn, UUID catalogId, String hierarchyName, EntityDirectoryHierarchy hierarchy) throws SQLException
    {
        String sql = "SELECT entity_key, entity_id FROM hierarchy_entity_directory " +
            "WHERE catalog_id = ? AND hierarchy_name = ? ORDER BY dir_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId.toString());
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("entity_key");
                    String entityIdStr = rs.getString("entity_id");
                    UUID entityId = UUID.fromString(entityIdStr);
                    Entity entity = factory.getOrRegisterNewEntity(entityId);
                    hierarchy.put(key, entity);
                }
            }
        }
    }

    private void loadEntityTreeContent(Connection conn, UUID catalogId, String hierarchyName, EntityTreeHierarchy hierarchy) throws SQLException
    {
        // Load all tree nodes into a map for efficient parent-child relationship building
        Map<UUID, NodeRecord> nodeMap = new HashMap<>();
        UUID rootNodeId = null;

        String sql = "SELECT node_id, parent_node_id, node_key, entity_id " +
            "FROM hierarchy_entity_tree_node " +
            "WHERE catalog_id = ? AND hierarchy_name = ? " +
            "ORDER BY node_path, tree_order";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId.toString());
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String nodeIdStr = rs.getString("node_id");
                    UUID nodeId = UUID.fromString(nodeIdStr);
                    String parentNodeIdStr = rs.getString("parent_node_id");
                    UUID parentNodeId = parentNodeIdStr != null ? UUID.fromString(parentNodeIdStr) : null;
                    String nodeKey = rs.getString("node_key");
                    String entityIdStr = rs.getString("entity_id");
                    UUID entityId = entityIdStr != null ? UUID.fromString(entityIdStr) : null;

                    Entity entity = entityId != null ? factory.getOrRegisterNewEntity(entityId) : null;
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
        UUID nodeId,
        UUID parentNodeId,
        String nodeKey,
        EntityTreeHierarchy.Node node
    ) {}

    private void loadAspectMapContent(Connection conn, UUID catalogId, String hierarchyName, AspectMapHierarchy hierarchy) throws SQLException
    {
        // Check if this AspectDef has a table mapping
        AspectTableMapping mapping = getAspectTableMapping(hierarchy.aspectDef().name());

        if (mapping != null) {
            loadAspectMapContentFromMappedTable(conn, catalogId, hierarchyName, hierarchy, mapping);
        } else {
            loadAspectMapContentFromDefaultTables(conn, catalogId, hierarchyName, hierarchy);
        }
    }

    private void loadAspectMapContentFromDefaultTables(Connection conn, UUID catalogId, String hierarchyName, AspectMapHierarchy hierarchy) throws SQLException
    {
        String sql = "SELECT entity_id FROM hierarchy_aspect_map " +
            "WHERE catalog_id = ? AND hierarchy_name = ? ORDER BY map_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, catalogId.toString());
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String entityIdStr = rs.getString("entity_id");
                    UUID entityId = UUID.fromString(entityIdStr);

                    Entity entity = factory.getOrRegisterNewEntity(entityId);
                    Aspect aspect = loadAspect(conn, entity, hierarchy.aspectDef(), catalogId);
                    hierarchy.put(entity, aspect);
                }
            }
        }
    }

    private void loadAspectMapContentFromMappedTable(Connection conn, UUID catalogId, String hierarchyName, AspectMapHierarchy hierarchy, AspectTableMapping mapping) throws SQLException
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
                stmt.setString(1, catalogId.toString());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Entity entity;

                    if (mapping.hasEntityId()) {
                        // Entity ID is in the table - use it
                        String entityIdStr = rs.getString("entity_id");
                        UUID entityId = UUID.fromString(entityIdStr);
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
                            Property property = factory.createProperty(propDef, value);
                            aspect.put(property);
                        }
                    }

                    hierarchy.put(entity, aspect);
                }
            }
        }
    }

    private Aspect loadAspect(Connection conn, Entity entity, AspectDef aspectDef, UUID catalogId) throws SQLException
    {
        Aspect aspect = factory.createPropertyMapAspect(entity, aspectDef);

        String sql = "SELECT property_name, value_index, value_text, value_binary " +
            "FROM property_value " +
            "WHERE entity_id = ? AND aspect_def_id = ? AND catalog_id = ? " +
            "ORDER BY property_name, value_index";

        UUID aspectDefId = getAspectDefId(conn, aspectDef.name());

        // Track which properties we've loaded from the database
        Set<String> loadedProperties = new HashSet<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entity.globalId().toString());
            stmt.setString(2, aspectDefId.toString());
            stmt.setString(3, catalogId.toString());
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

    private AspectDef loadAspectDefForHierarchy(Connection conn, UUID catalogId, String hierarchyName) throws SQLException
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
        String aspectSql = "SELECT aspect_def_id, hash_version, is_readable, is_writable, can_add_properties, can_remove_properties " +
            "FROM aspect_def WHERE name = ?";

        long hashVersion;
        UUID aspectDefId;
        boolean isReadable = true, isWritable = true, canAddProperties = false, canRemoveProperties = false;

        try (PreparedStatement stmt = conn.prepareStatement(aspectSql)) {
            stmt.setString(1, aspectDefName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    aspectDefId = UUID.fromString(rs.getString("aspect_def_id"));
                    hashVersion = rs.getLong("hash_version");
                    isReadable = rs.getBoolean("is_readable");
                    isWritable = rs.getBoolean("is_writable");
                    canAddProperties = rs.getBoolean("can_add_properties");
                    canRemoveProperties = rs.getBoolean("can_remove_properties");
                } else {
                    throw new SQLException("Unable to load AspectDef " + aspectDefName);
                }
            }
        }

        // Load property definitions first
        String propSql = "SELECT pd.name, pd.property_type, pd.default_value, pd.has_default_value, " +
            "pd.is_readable, pd.is_writable, pd.is_nullable, pd.is_removable, pd.is_multivalued " +
            "FROM property_def pd JOIN aspect_def ad ON pd.aspect_def_id = ad.aspect_def_id " +
            "WHERE ad.aspect_def_id = ?";

        Map<String, PropertyDef> propertyDefMap = new LinkedHashMap<>();

        try (PreparedStatement stmt = conn.prepareStatement(propSql)) {
            stmt.setString(1, aspectDefId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String propName = rs.getString("name");
                    PropertyType type = mapDbTypeToPropertyType(rs.getString("property_type"));
                    String defaultValue = rs.getString("default_value");
                    boolean hasDefaultValue = rs.getBoolean("has_default_value");
                    boolean propReadable = rs.getBoolean("is_readable");
                    boolean propWritable = rs.getBoolean("is_writable");
                    boolean isNullable = rs.getBoolean("is_nullable");
                    boolean isRemovable = rs.getBoolean("is_removable");
                    boolean isMultivalued = rs.getBoolean("is_multivalued");

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
            aspectDef = factory.createMutableAspectDef(aspectDefName, aspectDefId, propertyDefMap);
        } else if (!canAddProperties && !canRemoveProperties) {
            // Fully immutable - use ImmutableAspectDefImpl
            aspectDef = factory.createImmutableAspectDef(aspectDefName, aspectDefId, propertyDefMap);
        } else {
            // Mixed mutability - use FullAspectDefImpl
            aspectDef = factory.createFullAspectDef(aspectDefName, aspectDefId, propertyDefMap,
                isReadable, isWritable, canAddProperties, canRemoveProperties);
        }

        return aspectDef;
    }

    @Override
    public boolean deleteCatalog(@NotNull UUID catalogId) throws SQLException
    {
        try (Connection conn = dataSource.getConnection()) {
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
     * Looks up the UUID for an AspectDef by name.
     */
    private UUID getAspectDefId(Connection conn, String name) throws SQLException
    {
        String sql = "SELECT aspect_def_id FROM aspect_def WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String aspectDefIdStr = rs.getString("aspect_def_id");
                    return UUID.fromString(aspectDefIdStr);
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
            case Boolean -> stmt.setBoolean(paramIndex, (Boolean) value);
            case DateTime -> stmt.setTimestamp(paramIndex, convertToTimestamp(value));
            case UUID -> stmt.setString(paramIndex, value instanceof UUID ? value.toString() : value.toString());
            case BLOB -> stmt.setBytes(paramIndex, (byte[]) value);
            default -> stmt.setString(paramIndex, value.toString());
        }
    }

}
