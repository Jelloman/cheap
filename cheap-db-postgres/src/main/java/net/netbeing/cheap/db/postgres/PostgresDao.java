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

import net.netbeing.cheap.db.AbstractCheapDao;
import net.netbeing.cheap.db.AspectTableMapping;
import net.netbeing.cheap.db.CheapDao;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.impl.basic.CheapFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Data Access Object for persisting and loading complete Catalog instances
 * to/from a PostgreSQL database using the Cheap schema.
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
 * PostgresDao works with a normalized schema that closely mirrors the Cheap data model:
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
 * DataSource ds = createPostgresDataSource("jdbc:postgresql://localhost/cheapdb");
 * PostgresDao dao = new PostgresDao(ds);
 *
 * // Create schema (first time only)
 * dao.executeMainSchemaDdl(ds);
 *
 * // Create a catalog with data
 * Catalog catalog = adapter.getFactory().createCatalog(UUID.randomUUID(), CatalogSpecies.SOURCE, null, null, 1);
 * AspectDef customerDef = adapter.getFactory().createImmutableAspectDef("Customer", props);
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
 * PropertyTypes are mapped to PostgreSQL column types and internal 3-letter codes:
 * </p>
 * <ul>
 *   <li>Integer → BIGINT / INT</li>
 *   <li>Float → DOUBLE PRECISION / FLT</li>
 *   <li>Boolean → BOOLEAN / BLN</li>
 *   <li>String → TEXT / STR</li>
 *   <li>DateTime → TIMESTAMP WITH TIME ZONE / DAT</li>
 *   <li>UUID → UUID / UID</li>
 *   <li>BLOB → BYTEA / BLB</li>
 * </ul>
 *
 * @see CheapDao
 * @see AspectTableMapping
 * @see CheapFactory
 * @see Catalog
 */
@SuppressWarnings("DuplicateBranchesInSwitch")
public class PostgresDao extends AbstractCheapDao
{
    /**
     * Constructs a new PostgresDao with the given data source.
     * Creates a new CheapFactory instance for object creation and entity management.
     *
     * @param adapter the PostgreSQL database adapter to use for database operations
     */
    public PostgresDao(@NotNull PostgresAdapter adapter)
    {
        super(adapter, LoggerFactory.getLogger(PostgresDao.class));
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
    @Override
    public void createTable(@NotNull AspectTableMapping mapping) throws SQLException
    {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(mapping.tableName()).append(" (\n");

        boolean hasColumns = false;

        // Add catalog_id column if needed
        if (mapping.hasCatalogId()) {
            sql.append("    catalog_id UUID NOT NULL");
            hasColumns = true;
        }

        // Add entity_id column if needed
        if (mapping.hasEntityId()) {
            if (hasColumns) sql.append(",\n");
            sql.append("    entity_id UUID NOT NULL");
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

        try (Connection conn = adapter.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql.toString());
        }

        addAspectTableMapping(mapping);
    }

    /**
     * Maps a PropertyType to the corresponding PostgreSQL column type.
     */
    @Override
    public String mapPropertyTypeToSqlType(@NotNull PropertyType type)
    {
        return switch (type) {
            case Integer -> "BIGINT";
            case Float -> "DOUBLE PRECISION";
            case Boolean -> "BOOLEAN";
            case String -> "TEXT";
            case Text -> "TEXT";
            case BigInteger -> "TEXT";
            case BigDecimal -> "TEXT";
            case DateTime -> "TIMESTAMP WITH TIME ZONE";
            case URI -> "TEXT";
            case UUID -> "UUID";
            case CLOB -> "TEXT";
            case BLOB -> "BYTEA";
        };
    }

    @Override
    public void saveCatalog(@NotNull Catalog catalog) throws SQLException
    {
        try (Connection conn = adapter.getConnection()) {
            conn.setAutoCommit(false);
            try {
                saveCatalog(conn, catalog);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    @Override
    protected void linkCatalogToAspectDef(@NotNull Connection conn, @NotNull Catalog catalog, @NotNull AspectDef aspectDef) throws SQLException
    {
        UUID aspectDefId = aspectDef.globalId();
        String sql = "INSERT INTO catalog_aspect_def (catalog_id, aspect_def_id) " +
            "VALUES (?, ?) ON CONFLICT (catalog_id, aspect_def_id) DO NOTHING";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, catalog.globalId());
            stmt.setObject(2, aspectDefId);
            stmt.executeUpdate();
        }
    }

    @Override
    public void saveAspectDef(@NotNull Connection conn, @NotNull AspectDef aspectDef) throws SQLException
    {
        String sql =
            "INSERT INTO aspect_def (aspect_def_id, name, hash_version, is_readable, is_writable, can_add_properties, can_remove_properties) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (name) DO UPDATE SET " +
                "hash_version = EXCLUDED.hash_version, " +
                "is_readable = EXCLUDED.is_readable, " +
                "is_writable = EXCLUDED.is_writable, " +
                "can_add_properties = EXCLUDED.can_add_properties, " +
                "can_remove_properties = EXCLUDED.can_remove_properties";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, aspectDef.globalId());
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
        UUID aspectDefId = aspectDef.globalId();

        String sql = "INSERT INTO property_def (aspect_def_id, name, property_type, default_value, " +
            "has_default_value, is_readable, is_writable, is_nullable, is_removable, is_multivalued) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (aspect_def_id, name) DO UPDATE SET " +
            "property_type = EXCLUDED.property_type, " +
            "default_value = EXCLUDED.default_value, " +
            "has_default_value = EXCLUDED.has_default_value, " +
            "is_readable = EXCLUDED.is_readable, " +
            "is_writable = EXCLUDED.is_writable, " +
            "is_nullable = EXCLUDED.is_nullable, " +
            "is_removable = EXCLUDED.is_removable, " +
            "is_multivalued = EXCLUDED.is_multivalued";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, aspectDefId);
            stmt.setString(2, propDef.name());
            stmt.setString(3, propDef.type().typeCode());
            stmt.setString(4, propDef.hasDefaultValue() && propDef.defaultValue() != null ? propDef.defaultValue().toString() : null);
            stmt.setBoolean(5, propDef.hasDefaultValue());
            stmt.setBoolean(6, propDef.isReadable());
            stmt.setBoolean(7, propDef.isWritable());
            stmt.setBoolean(8, propDef.isNullable());
            stmt.setBoolean(9, propDef.isRemovable());
            stmt.setBoolean(10, propDef.isMultivalued());
            stmt.executeUpdate();
        }
    }


    @Override
    public void saveEntity(@NotNull Connection conn, @NotNull Entity entity) throws SQLException
    {
        String sql = "INSERT INTO entity (entity_id) VALUES (?) ON CONFLICT (entity_id) DO NOTHING";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, entity.globalId());
            stmt.executeUpdate();
        }
    }

    @Override
    protected void saveCatalogRecord(@NotNull Connection conn, @NotNull Catalog catalog) throws SQLException
    {
        String sql = "INSERT INTO catalog (catalog_id, species, uri, upstream_catalog_id, version_number) "
            + "VALUES (?, ?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id) DO UPDATE SET " +
            "species = EXCLUDED.species, " +
            "uri = EXCLUDED.uri, " +
            "upstream_catalog_id = EXCLUDED.upstream_catalog_id, " +
            "version_number = EXCLUDED.version_number";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, catalog.globalId());
            stmt.setString(2, catalog.species().name());
            stmt.setString(3, catalog.uri() != null ? catalog.uri().toString() : null);
            stmt.setObject(4, catalog.upstream());
            stmt.setLong(5, catalog.version());
            stmt.executeUpdate();
        }
    }

    @Override
    public void saveHierarchy(@NotNull Connection conn, @NotNull Hierarchy hierarchy) throws SQLException
    {
        UUID catalogId = hierarchy.catalog().globalId();

        String sql = "INSERT INTO hierarchy (catalog_id, name, hierarchy_type, version_number) " +
            "VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id, name) DO UPDATE SET " +
            "hierarchy_type = EXCLUDED.hierarchy_type, " +
            "version_number = EXCLUDED.version_number";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, catalogId);
            stmt.setString(2, hierarchy.name());
            stmt.setString(3, hierarchy.type().typeCode());
            stmt.setLong(4, hierarchy.version());
            stmt.executeUpdate();
        }
    }

    @Override
    protected void saveEntityListContent(@NotNull Connection conn, @NotNull EntityListHierarchy hierarchy) throws SQLException
    {
        String sql = "INSERT INTO hierarchy_entity_list (catalog_id, hierarchy_name, entity_id, list_order) " +
            "VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id, hierarchy_name, list_order) DO UPDATE SET " +
            "entity_id = EXCLUDED.entity_id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int order = 0;
            for (Entity entity : hierarchy) {
                saveEntity(conn, entity);
                stmt.setObject(1, hierarchy.catalog().globalId());
                stmt.setString(2, hierarchy.name());
                stmt.setObject(3, entity.globalId());
                stmt.setInt(4, order++);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    protected void saveEntitySetContent(@NotNull Connection conn, @NotNull EntitySetHierarchy hierarchy) throws SQLException
    {
        String sql = "INSERT INTO hierarchy_entity_set (catalog_id, hierarchy_name, entity_id, set_order) " +
            "VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id, hierarchy_name, entity_id) DO UPDATE SET " +
            "set_order = EXCLUDED.set_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int order = 0;
            for (Entity entity : hierarchy) {
                saveEntity(conn, entity);
                stmt.setObject(1, hierarchy.catalog().globalId());
                stmt.setString(2, hierarchy.name());
                stmt.setObject(3, entity.globalId());
                stmt.setInt(4, order++);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    protected void saveEntityDirectoryContent(@NotNull Connection conn, @NotNull EntityDirectoryHierarchy hierarchy) throws SQLException
    {
        String sql = "INSERT INTO hierarchy_entity_directory (catalog_id, hierarchy_name, entity_key, entity_id, dir_order) " +
            "VALUES (?, ?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id, hierarchy_name, entity_key) DO UPDATE SET " +
            "entity_id = EXCLUDED.entity_id, " +
            "dir_order = EXCLUDED.dir_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int order = 0;
            for (Map.Entry<String,Entity> entry : hierarchy.entrySet()) {
                Entity entity = entry.getValue();
                if (entity != null) {
                    saveEntity(conn, entity);
                    stmt.setObject(1, hierarchy.catalog().globalId());
                    stmt.setString(2, hierarchy.name());
                    stmt.setString(3, entry.getKey());
                    stmt.setObject(4, entity.globalId());
                    stmt.setInt(5, order++);
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }

    @Override
    protected void saveEntityTreeContent(@NotNull Connection conn, @NotNull EntityTreeHierarchy hierarchy) throws SQLException
    {
        // Save tree nodes recursively
        saveTreeNode(conn, hierarchy, hierarchy.root(), "", "", null, 0);
    }

    private void saveTreeNode(Connection conn, EntityTreeHierarchy hierarchy, EntityTreeHierarchy.Node node,
                              String nodeKey, String nodePath, UUID parentNodeId, int order) throws SQLException
    {
        UUID nodeId = UUID.randomUUID();
        UUID entityId = node.value() == null ? null : node.value().globalId();

        String sql = "INSERT INTO hierarchy_entity_tree_node " +
            "(node_id, catalog_id, hierarchy_name, parent_node_id, node_key, entity_id, node_path, tree_order) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id, hierarchy_name, parent_node_id, node_key) DO UPDATE SET " +
            "entity_id = EXCLUDED.entity_id, " +
            "node_path = EXCLUDED.node_path, " +
            "tree_order = EXCLUDED.tree_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, nodeId);
            stmt.setObject(2, hierarchy.catalog().globalId());
            stmt.setString(3, hierarchy.name());
            stmt.setObject(4, parentNodeId);
            stmt.setString(5, nodeKey);
            stmt.setObject(6, entityId);
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
                    saveTreeNode(conn, hierarchy, child, name, childPath, nodeId, childOrder++);
                }
            }
        }
    }

    @Override
    protected void saveAspectMapContent(Connection conn, AspectMapHierarchy hierarchy) throws SQLException
    {
        // Check if this AspectDef has a table mapping
        AspectTableMapping mapping = getAspectTableMapping(hierarchy.aspectDef().name());

        if (mapping != null) {
            saveAspectMapContentToMappedTable(conn, hierarchy, mapping);
        } else {
            saveAspectMapContentToDefaultTables(conn, hierarchy);
        }
    }

    @Override
    protected void saveAspectMapContentToDefaultTables(@NotNull Connection conn, @NotNull AspectMapHierarchy hierarchy) throws SQLException
    {
        String aspectSql = "INSERT INTO aspect (entity_id, aspect_def_id, catalog_id, hierarchy_name) " +
            "VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (entity_id, aspect_def_id, catalog_id) DO UPDATE SET " +
            "hierarchy_name = EXCLUDED.hierarchy_name";
        String hierarchyMapSql = "INSERT INTO hierarchy_aspect_map (catalog_id, hierarchy_name, entity_id, aspect_def_id, map_order) " +
            "VALUES (?, ?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id, hierarchy_name, entity_id) DO UPDATE SET " +
            "aspect_def_id = EXCLUDED.aspect_def_id, " +
            "map_order = EXCLUDED.map_order";

        UUID aspectDefId = hierarchy.aspectDef().globalId();

        int order = 0;
        try (PreparedStatement aspectStmt = conn.prepareStatement(aspectSql);
             PreparedStatement mapStmt = conn.prepareStatement(hierarchyMapSql)) {

            aspectStmt.setObject(2, aspectDefId);
            mapStmt.setObject(4, aspectDefId);

            for (Map.Entry<Entity,Aspect> entry : hierarchy.entrySet()) {
                Entity entity = entry.getKey();
                saveEntity(conn, entity);

                Aspect aspect = entry.getValue();
                if (aspect != null) {
                    // Save aspect
                    aspectStmt.setObject(1, entity.globalId());
                    aspectStmt.setObject(3, hierarchy.catalog().globalId());
                    aspectStmt.setString(4, hierarchy.name());
                    aspectStmt.executeUpdate();
                }

                mapStmt.setObject(1, hierarchy.catalog().globalId());
                mapStmt.setString(2, hierarchy.name());
                mapStmt.setObject(3, entity.globalId());
                mapStmt.setInt(5, order++);
                mapStmt.executeUpdate(); // NOSONAR - TODO: add batch size to adapter and then batch this

                // Save properties
                saveAspectProperties(conn, entity.globalId(), aspectDefId, hierarchy.catalog().globalId(), aspect);
            }
        }
    }

    @Override
    protected @NotNull StringBuilder buildAspectMapSql(@NotNull AspectTableMapping mapping)
    {
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
                sql.append(columnName).append(" = EXCLUDED.").append(columnName);
                first = false;
            }
        }
        return sql;
    }

    @Override
    protected void clearMappedTable(@NotNull Connection conn, @NotNull AspectTableMapping mapping, @NotNull UUID catalogId) throws SQLException
    {
        if (!mapping.hasEntityId() && !mapping.hasCatalogId()) {
            // No IDs: TRUNCATE the entire table
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("TRUNCATE TABLE " + mapping.tableName());
            }
        } else if (!mapping.hasEntityId() && mapping.hasCatalogId()) {
            // Catalog ID only: DELETE rows for this catalog
            String deleteSql = "DELETE FROM " + mapping.tableName() + " WHERE catalog_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setObject(1, catalogId);
                stmt.executeUpdate();
            }
        }
    }

    @Override
    protected void saveAspectToMappedTable(@NotNull AspectTableMapping mapping, @NotNull Entity entity, Aspect aspect, @NotNull PreparedStatement stmt, @NotNull UUID catalogId) throws SQLException
    {
        int paramIndex = 1;

        if (mapping.hasCatalogId()) {
            stmt.setObject(paramIndex++, catalogId);
        }

        if (mapping.hasEntityId()) {
            stmt.setObject(paramIndex++, entity.globalId());
        }

        for (String propName : mapping.propertyToColumnMap().keySet()) {
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

    private void saveAspectProperties(Connection conn, UUID entityId, UUID aspectDefId, UUID catalogId, Aspect aspect) throws SQLException
    {
        // First, delete existing property values for this aspect to handle updates properly
        String deleteSql = "DELETE FROM property_value WHERE entity_id = ? AND aspect_def_id = ? AND catalog_id = ?";
        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            deleteStmt.setObject(1, entityId);
            deleteStmt.setObject(2, aspectDefId);
            deleteStmt.setObject(3, catalogId);
            deleteStmt.executeUpdate();
        }

        String sql = "INSERT INTO property_value (entity_id, aspect_def_id, catalog_id, property_name, value_index, " +
            "value_text, value_binary) VALUES (?, ?, ?, ?, ?, ?, ?)";

        AspectDef aspectDef = aspect.def();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, entityId);
            stmt.setObject(2, aspectDefId);
            stmt.setObject(3, catalogId);
            for (PropertyDef propDef : aspectDef.propertyDefs()) {
                String propName = propDef.name();
                Object value = aspect.readObj(propName);
                PropertyType type = propDef.type();

                if (value == null) {
                    // For null values:
                    // - Single-valued: insert a row with NULL
                    // - Multivalued: don't insert any rows (null will be distinguished from empty list during load)
                    if (!propDef.isMultivalued()) {
                        stmt.setString(4, propName);
                        stmt.setInt(5, 0); // NOSONAR - sonar bug, doesn't see setInt(5,i) below
                        stmt.setString(6, null); // value_text
                        stmt.setBytes(7, null);  // value_binary
                        stmt.addBatch();
                    }
                } else if (propDef.isMultivalued() && value instanceof List) {
                    // For multivalued properties, insert one row per value
                    @SuppressWarnings("unchecked")
                    List<Object> listValues = (List<Object>) value;

                    stmt.setString(4, propName);
                    // If empty list, don't insert any rows (empty list represented by no rows)
                    for (int i = 0; i < listValues.size(); i++) {
                        Object itemValue = listValues.get(i);
                        stmt.setInt(5, i); // value_index

                        if (type == PropertyType.BLOB) {
                            stmt.setString(6, null); // value_text
                            stmt.setBytes(7, (byte[]) itemValue); // value_binary
                        } else {
                            stmt.setString(6, adapter.getValueAdapter().convertValueToString(itemValue, type)); // value_text
                            stmt.setBytes(7, null); // value_binary
                        }
                        stmt.addBatch();
                    }
                } else {
                    // For single-valued properties, insert one row with value_index 0
                    stmt.setString(4, propName);
                    stmt.setInt(5, 0); // NOSONAR - sonar bug, doesn't see setInt(5,i) above

                    if (type == PropertyType.BLOB) {
                        stmt.setString(6, null); // value_text
                        stmt.setBytes(7, (byte[]) value); // value_binary
                    } else {
                        stmt.setString(6, adapter.getValueAdapter().convertValueToString(value, type)); // value_text
                        stmt.setBytes(7, null); // value_binary
                    }
                    stmt.addBatch();
                }
            }

            stmt.executeBatch();
        }
    }

    @Override
    public Catalog loadCatalogWithConnection(@NotNull Connection conn, @NotNull UUID catalogId) throws SQLException
    {
        // Load catalog basic info
        String sql = "SELECT catalog_id, species, uri, upstream_catalog_id, version_number FROM catalog WHERE catalog_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, catalogId);
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
                UUID upstream = rs.getObject("upstream_catalog_id", UUID.class);
                long version = rs.getLong("version_number");

                // Create catalog with version
                Catalog catalog = adapter.getFactory().createCatalog(catalogId, species, uri, upstream, version);

                // Load and extend catalog with AspectDefs
                loadAndExtendAspectDefs(conn, catalog);

                // Load and add all hierarchies
                loadHierarchies(conn, catalog);

                return catalog;
            }
        }
    }

    @Override
    public Hierarchy createAndLoadHierarchy(@NotNull Connection conn, @NotNull Catalog catalog, @NotNull HierarchyType type, @NotNull String hierarchyName, long version) throws SQLException
    {
        switch (type) {
            case ENTITY_LIST -> {
                EntityListHierarchy hierarchy = adapter.getFactory().createEntityListHierarchy(catalog, hierarchyName, version);
                loadEntityListContent(conn, hierarchy);
                return hierarchy;
            }
            case ENTITY_SET -> {
                EntitySetHierarchy hierarchy = adapter.getFactory().createEntitySetHierarchy(catalog, hierarchyName, version);
                loadEntitySetContent(conn, hierarchy);
                return hierarchy;
            }
            case ENTITY_DIR -> {
                EntityDirectoryHierarchy hierarchy = adapter.getFactory().createEntityDirectoryHierarchy(catalog, hierarchyName, version);
                loadEntityDirectoryContent(conn, hierarchy);
                return hierarchy;
            }
            case ENTITY_TREE -> {
                Entity rootEntity = adapter.getFactory().createEntity();
                EntityTreeHierarchy hierarchy = adapter.getFactory().createEntityTreeHierarchy(catalog, hierarchyName, null, 0L);
                hierarchy.root().setValue(rootEntity);
                loadEntityTreeContent(conn, hierarchy);
                return hierarchy;
            }
            case ASPECT_MAP -> {
                AspectDef aspectDef = loadAspectDefForHierarchy(conn, catalog.globalId(), hierarchyName);
                AspectMapHierarchy hierarchy = adapter.getFactory().createAspectMapHierarchy(catalog, aspectDef, version);
                loadAspectMapContent(conn, hierarchy);
                return hierarchy;
            }
            default -> throw new IllegalArgumentException("Unknown hierarchy type: " + type);
        }
    }

    @Override
    protected void loadEntityListContent(Connection conn, EntityListHierarchy hierarchy) throws SQLException
    {
        String sql = "SELECT entity_id, list_order FROM hierarchy_entity_list WHERE catalog_id = ? AND hierarchy_name = ? ORDER BY list_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, hierarchy.catalog().globalId());
            stmt.setString(2, hierarchy.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID entityId = rs.getObject("entity_id", UUID.class);
                    Entity entity = adapter.getFactory().getOrRegisterNewEntity(entityId);
                    hierarchy.add(entity);
                }
            }
        }
    }

    @Override
    protected void loadEntitySetContent(Connection conn, EntitySetHierarchy hierarchy) throws SQLException
    {
        String sql = "SELECT entity_id FROM hierarchy_entity_set WHERE catalog_id = ? AND hierarchy_name = ? ORDER BY set_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, hierarchy.catalog().globalId());
            stmt.setString(2, hierarchy.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID entityId = rs.getObject("entity_id", UUID.class);
                    Entity entity = adapter.getFactory().getOrRegisterNewEntity(entityId);
                    hierarchy.add(entity);
                }
            }
        }
    }

    @Override
    protected void loadEntityDirectoryContent(Connection conn, EntityDirectoryHierarchy hierarchy) throws SQLException
    {
        String sql = "SELECT entity_key, entity_id FROM hierarchy_entity_directory " +
            "WHERE catalog_id = ? AND hierarchy_name = ? ORDER BY dir_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, hierarchy.catalog().globalId());
            stmt.setString(2, hierarchy.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("entity_key");
                    UUID entityId = rs.getObject("entity_id", UUID.class);
                    Entity entity = adapter.getFactory().getOrRegisterNewEntity(entityId);
                    hierarchy.put(key, entity);
                }
            }
        }
    }

    @Override
    protected void loadEntityTreeContent(Connection conn, EntityTreeHierarchy hierarchy) throws SQLException
    {
        // Load all tree nodes into a map for efficient parent-child relationship building
        Map<UUID, NodeRecord> nodeMap = new HashMap<>();
        UUID rootNodeId = null;

        String sql = "SELECT node_id, parent_node_id, node_key, entity_id " +
            "FROM hierarchy_entity_tree_node " +
            "WHERE catalog_id = ? AND hierarchy_name = ? " +
            "ORDER BY node_path, tree_order";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, hierarchy.catalog().globalId());
            stmt.setString(2, hierarchy.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID nodeId = rs.getObject("node_id", UUID.class);
                    UUID parentNodeId = rs.getObject("parent_node_id", UUID.class);
                    String nodeKey = rs.getString("node_key");
                    UUID entityId = rs.getObject("entity_id", UUID.class);

                    Entity entity = entityId != null ? adapter.getFactory().getOrRegisterNewEntity(entityId) : null;
                    EntityTreeHierarchy.Node node = adapter.getFactory().createTreeNode(entity);

                    NodeRecord nodeRec = new NodeRecord(nodeId, parentNodeId, nodeKey, node);
                    nodeMap.put(nodeId, nodeRec);

                    // Root node has no parent
                    if (parentNodeId == null) {
                        rootNodeId = nodeId;
                    }
                }
            }
        }

        // Build the tree structure by adding children to their parents
        for (NodeRecord nodeRec : nodeMap.values()) {
            if (nodeRec.parentNodeId != null) {
                NodeRecord parentRecord = nodeMap.get(nodeRec.parentNodeId);
                if (parentRecord != null) {
                    parentRecord.node.put(nodeRec.nodeKey, nodeRec.node);
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

    @Override
    protected void loadAspectMapContent(Connection conn, AspectMapHierarchy hierarchy) throws SQLException
    {
        // Check if this AspectDef has a table mapping
        AspectTableMapping mapping = getAspectTableMapping(hierarchy.aspectDef().name());

        if (mapping != null) {
            loadAspectMapContentFromMappedTable(conn, hierarchy, mapping);
        } else {
            loadAspectMapContentFromDefaultTables(conn, hierarchy);
        }
    }

    @Override
    protected void loadAspectMapContentFromDefaultTables(Connection conn, AspectMapHierarchy hierarchy) throws SQLException
    {
        String sql = "SELECT entity_id FROM hierarchy_aspect_map " +
            "WHERE catalog_id = ? AND hierarchy_name = ? ORDER BY map_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, hierarchy.catalog().globalId());
            stmt.setString(2, hierarchy.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID entityId = rs.getObject("entity_id", UUID.class);

                    Entity entity = adapter.getFactory().getOrRegisterNewEntity(entityId);
                    Aspect aspect = loadAspect(conn, entity, hierarchy.aspectDef(), hierarchy.catalog());
                    hierarchy.put(entity, aspect);
                }
            }
        }
    }

    @Override
    protected void loadAspectMapContentFromMappedTable(Connection conn, AspectMapHierarchy hierarchy, AspectTableMapping mapping) throws SQLException
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
                stmt.setObject(1, hierarchy.catalog().globalId());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Entity entity;

                    if (mapping.hasEntityId()) {
                        // Entity ID is in the table - use it
                        UUID entityId = rs.getObject("entity_id", UUID.class);
                        entity = adapter.getFactory().getOrRegisterNewEntity(entityId);
                    } else {
                        // No entity ID in table - generate a new one
                        entity = adapter.getFactory().createEntity();
                    }

                    Aspect aspect = adapter.getFactory().createPropertyMapAspect(entity, hierarchy.aspectDef());

                    // Load properties from mapped columns
                    for (Map.Entry<String, String> entry : mapping.propertyToColumnMap().entrySet()) {
                        String propName = entry.getKey();
                        String columnName = entry.getValue();

                        PropertyDef propDef = hierarchy.aspectDef().propertyDef(propName);
                        if (propDef != null) {
                            Object value = rs.getObject(columnName);
                            Property property = adapter.getFactory().createProperty(propDef, value);
                            aspect.put(property);
                        }
                    }

                    hierarchy.put(entity, aspect);
                }
            }
        }
    }

    @Override
    public Aspect loadAspect(@NotNull Connection conn, @NotNull Entity entity, @NotNull AspectDef aspectDef, @NotNull Catalog catalog) throws SQLException
    {
        Aspect aspect = adapter.getFactory().createPropertyMapAspect(entity, aspectDef);

        String sql = "SELECT property_name, value_index, value_text, value_binary " +
            "FROM property_value " +
            "WHERE entity_id = ? AND aspect_def_id = ? AND catalog_id = ? " +
            "ORDER BY property_name, value_index";

        UUID aspectDefId = aspectDef.globalId();

        // Track which properties we've loaded from the database
        Set<String> loadedProperties = new HashSet<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, entity.globalId());
            stmt.setObject(2, aspectDefId);
            stmt.setObject(3, catalog.globalId());
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
                Property property = adapter.getFactory().createProperty(propDef, Collections.emptyList());
                aspect.put(property);
            }
        }

        return aspect;
    }

    @Override
    protected AspectDef loadAspectDefForHierarchy(Connection conn, UUID catalogId, String hierarchyName) throws SQLException
    {
        // For AspectMap hierarchies, the hierarchy name matches the AspectDef name
        // Try to load the AspectDef directly by name
        try {
            return loadAspectDef(conn, hierarchyName);
        } catch (SQLException e) {
            throw new SQLException("Could not find AspectDef for hierarchy: " + hierarchyName + " in catalog " + catalogId, e);
        }
    }

    @Override
    public AspectDef loadAspectDef(@NotNull Connection conn, @NotNull String aspectDefName) throws SQLException
    {
        // First load the AspectDef basic info except hash_version
        String aspectSql = "SELECT aspect_def_id, is_readable, is_writable, can_add_properties, can_remove_properties " +
            "FROM aspect_def WHERE name = ?";

        UUID aspectDefId;
        boolean isReadable;
        boolean isWritable;
        boolean canAddProperties;
        boolean canRemoveProperties;

        try (PreparedStatement stmt = conn.prepareStatement(aspectSql)) {
            stmt.setString(1, aspectDefName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    aspectDefId = rs.getObject("aspect_def_id", UUID.class);
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
            stmt.setObject(1, aspectDefId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String propName = rs.getString("name");
                    PropertyType type = PropertyType.fromTypeCode(rs.getString("property_type"));
                    String defaultValue = rs.getString("default_value");
                    boolean hasDefaultValue = rs.getBoolean("has_default_value");
                    boolean propReadable = rs.getBoolean("is_readable");
                    boolean propWritable = rs.getBoolean("is_writable");
                    boolean isNullable = rs.getBoolean("is_nullable");
                    boolean isRemovable = rs.getBoolean("is_removable");
                    boolean isMultivalued = rs.getBoolean("is_multivalued");

                    PropertyDef propDef = adapter.getFactory().createPropertyDef(propName, type, defaultValue, hasDefaultValue,
                        propReadable, propWritable, isNullable, isRemovable, isMultivalued);

                    propertyDefMap.put(propName, propDef);
                }
            }
        }

        // Choose the appropriate AspectDef implementation based on the flags
        AspectDef aspectDef;
        if (canAddProperties && canRemoveProperties) {
            // Fully mutable - use MutableAspectDefImpl
            aspectDef = adapter.getFactory().createMutableAspectDef(aspectDefName, aspectDefId, propertyDefMap);
        } else if (!canAddProperties && !canRemoveProperties) {
            // Fully immutable - use ImmutableAspectDefImpl
            aspectDef = adapter.getFactory().createImmutableAspectDef(aspectDefName, aspectDefId, propertyDefMap);
        } else {
            // Mixed mutability - use FullAspectDefImpl
            aspectDef = adapter.getFactory().createFullAspectDef(aspectDefName, aspectDefId, propertyDefMap,
                isReadable, isWritable, canAddProperties, canRemoveProperties);
        }

        return aspectDef;
    }

    @Override
    public boolean deleteCatalog(@NotNull UUID catalogId) throws SQLException
    {
        try (Connection conn = adapter.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = "DELETE FROM catalog WHERE catalog_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setObject(1, catalogId);
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
        try (Connection conn = adapter.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, catalogId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ===== Value Conversion Methods =====

    /**
     * Sets a UUID parameter in a PreparedStatement using PostgreSQL's native UUID type.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param parameterIndex the parameter index (1-based)
     * @param value the UUID value to set
     * @throws SQLException if database operation fails
     */
    @Override
    protected void setUuidParameter(PreparedStatement stmt, int parameterIndex, UUID value) throws SQLException
    {
        stmt.setObject(parameterIndex, value);
    }

    /**
     * Sets a property value in a PreparedStatement, handling type conversions.
     * Used when saving aspects to custom mapped tables.
     */
    @Override
    protected void setPropertyValue(@NotNull PreparedStatement stmt, int paramIndex, Object value, @NotNull PropertyType type) throws SQLException
    {
        if (value == null) {
            stmt.setObject(paramIndex, null);
            return;
        }

        switch (type) {
            case Integer -> stmt.setLong(paramIndex, ((Number) value).longValue());
            case Float -> stmt.setDouble(paramIndex, ((Number) value).doubleValue());
            case Boolean -> stmt.setBoolean(paramIndex, (Boolean) value);
            case DateTime -> stmt.setTimestamp(paramIndex, adapter.getValueAdapter().convertToTimestamp(value));
            case UUID -> stmt.setObject(paramIndex, value instanceof UUID ? value : UUID.fromString(value.toString()));
            case BLOB -> stmt.setBytes(paramIndex, (byte[]) value);
            default -> stmt.setString(paramIndex, value.toString());
        }
    }

}