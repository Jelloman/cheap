package net.netbeing.cheap.db;

import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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
 *   <li><b>property_value:</b> Property values with type-specific columns (value_text, value_integer,
 *       value_float, value_boolean, value_datetime, value_binary)</li>
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
 * @see CatalogPersistence
 * @see AspectTableMapping
 * @see CheapFactory
 * @see Catalog
 */
@SuppressWarnings("DuplicateBranchesInSwitch")
public class PostgresDao implements CatalogPersistence
{
    private final DataSource dataSource;
    private final CheapFactory factory;
    private final Map<String, AspectTableMapping> aspectTableMappings = new LinkedHashMap<>();

    /**
     * Constructs a new PostgresDao with the given data source.
     * Creates a new CheapFactory instance for object creation and entity management.
     *
     * @param dataSource the PostgreSQL data source to use for database operations
     */
    public PostgresDao(@NotNull DataSource dataSource)
    {
        this.dataSource = dataSource;
        this.factory = new CheapFactory();
    }

    /**
     * Constructs a new PostgresDao with the given data source and factory.
     * This constructor allows sharing a CheapFactory instance across multiple DAOs
     * to maintain a consistent entity registry.
     *
     * @param dataSource the PostgreSQL data source to use for database operations
     * @param factory the CheapFactory to use for object creation and entity management
     */
    public PostgresDao(@NotNull DataSource dataSource, @NotNull CheapFactory factory)
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
        aspectTableMappings.put(mapping.aspectDefName(), mapping);
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
     * Creates a database table for storing aspects based on an AspectDef.
     * The table will have an entity_id column as the primary key, plus columns
     * for each property in the AspectDef.
     *
     * @param aspectDef the AspectDef defining the table structure
     * @param tableName the name of the table to create
     * @throws SQLException if table creation fails
     */
    public void createAspectTable(@NotNull AspectDef aspectDef, @NotNull String tableName) throws SQLException
    {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(tableName).append(" (\n");
        sql.append("    entity_id UUID NOT NULL PRIMARY KEY");

        for (PropertyDef propDef : aspectDef.propertyDefs()) {
            sql.append(",\n    ");
            sql.append(propDef.name()).append(" ");
            sql.append(mapPropertyTypeToSqlType(propDef.type()));
            if (!propDef.isNullable()) {
                sql.append(" NOT NULL");
            }
        }

        sql.append("\n)");

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql.toString());
        }
    }

    /**
     * Maps a PropertyType to the corresponding PostgreSQL column type.
     */
    private String mapPropertyTypeToSqlType(PropertyType type)
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

    private static String loadDdlResource(String resourcePath) throws SQLException
    {
        try (var inputStream = PostgresDao.class.getResourceAsStream(resourcePath)) {
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
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(ddlContent);
        }
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
            "VALUES (?, ?) ON CONFLICT (catalog_id, aspect_def_id) DO NOTHING";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, catalogId);
            stmt.setObject(2, aspectDefId);
            stmt.executeUpdate();
        }
    }

    private void saveAspectDef(Connection conn, AspectDef aspectDef) throws SQLException
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
            stmt.setObject(1, UUID.randomUUID());
            stmt.setString(2, aspectDef.name());
            stmt.setString(3, "TODO_HASH"); // TODO: Fix hash serialization
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
        String sql = "INSERT INTO entity (entity_id) VALUES (?) ON CONFLICT (entity_id) DO NOTHING";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, entity.globalId());
            stmt.executeUpdate();
        }
    }

    private void saveCatalogRecord(Connection conn, Catalog catalog) throws SQLException
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

    private void saveHierarchy(Connection conn, Hierarchy hierarchy) throws SQLException
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
            "ON CONFLICT (catalog_id, hierarchy_name, list_order) DO UPDATE SET " +
            "entity_id = EXCLUDED.entity_id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int order = 0;
            for (Entity entity : hierarchy) {
                saveEntity(conn, entity);
                stmt.setObject(1, catalogId);
                stmt.setString(2, hierarchyName);
                stmt.setObject(3, entity.globalId());
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
            "ON CONFLICT (catalog_id, hierarchy_name, entity_id) DO UPDATE SET " +
            "set_order = EXCLUDED.set_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int order = 0;
            for (Entity entity : hierarchy) {
                saveEntity(conn, entity);
                stmt.setObject(1, catalogId);
                stmt.setString(2, hierarchyName);
                stmt.setObject(3, entity.globalId());
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
            "ON CONFLICT (catalog_id, hierarchy_name, entity_key) DO UPDATE SET " +
            "entity_id = EXCLUDED.entity_id, " +
            "dir_order = EXCLUDED.dir_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int order = 0;
            for (String key : hierarchy.keySet()) {
                Entity entity = hierarchy.get(key);
                if (entity != null) {
                    saveEntity(conn, entity);
                    stmt.setObject(1, catalogId);
                    stmt.setString(2, hierarchyName);
                    stmt.setString(3, key);
                    stmt.setObject(4, entity.globalId());
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
            "ON CONFLICT (catalog_id, hierarchy_name, parent_node_id, node_key) DO UPDATE SET " +
            "entity_id = EXCLUDED.entity_id, " +
            "node_path = EXCLUDED.node_path, " +
            "tree_order = EXCLUDED.tree_order";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, nodeId);
            stmt.setObject(2, catalogId);
            stmt.setString(3, hierarchyName);
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
            "ON CONFLICT (entity_id, aspect_def_id, catalog_id) DO UPDATE SET " +
            "hierarchy_name = EXCLUDED.hierarchy_name";
        String hierarchyMapSql = "INSERT INTO hierarchy_aspect_map (catalog_id, hierarchy_name, entity_id, aspect_def_id, map_order) " +
            "VALUES (?, ?, ?, ?, ?) " +
            "ON CONFLICT (catalog_id, hierarchy_name, entity_id) DO UPDATE SET " +
            "aspect_def_id = EXCLUDED.aspect_def_id, " +
            "map_order = EXCLUDED.map_order";

        UUID aspectDefId = getAspectDefId(conn, hierarchy.aspectDef().name());

        int order = 0;
        for (Entity entity : hierarchy.keySet()) {
            saveEntity(conn, entity);

            Aspect aspect = hierarchy.get(entity);
            if (aspect != null) {
                // Save aspect
                try (PreparedStatement aspectStmt = conn.prepareStatement(aspectSql)) {
                    aspectStmt.setObject(1, entity.globalId());
                    aspectStmt.setObject(2, aspectDefId);
                    aspectStmt.setObject(3, catalogId);
                    aspectStmt.setString(4, hierarchyName);
                    aspectStmt.executeUpdate();
                }

                // Save hierarchy mapping
                try (PreparedStatement mapStmt = conn.prepareStatement(hierarchyMapSql)) {
                    mapStmt.setObject(1, catalogId);
                    mapStmt.setString(2, hierarchyName);
                    mapStmt.setObject(3, entity.globalId());
                    mapStmt.setObject(4, aspectDefId);
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
        // Build column list and placeholders for INSERT
        StringBuilder columns = new StringBuilder("entity_id");
        StringBuilder placeholders = new StringBuilder("?");

        for (Map.Entry<String, String> entry : mapping.propertyToColumnMap().entrySet()) {
            columns.append(", ").append(entry.getValue());
            placeholders.append(", ?");
        }

        StringBuilder sql = new StringBuilder("INSERT INTO " + mapping.tableName()
            + " (" + columns + ") VALUES (" + placeholders + ") " +
            "ON CONFLICT (entity_id) DO UPDATE SET ");

        // Build UPDATE clause
        boolean first = true;
        for (Map.Entry<String, String> entry : mapping.propertyToColumnMap().entrySet()) {
            if (!first) sql.append(", ");
            sql.append(entry.getValue()).append(" = EXCLUDED.").append(entry.getValue());
            first = false;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (Entity entity : hierarchy.keySet()) {
                saveEntity(conn, entity);

                Aspect aspect = hierarchy.get(entity);
                if (aspect != null) {
                    int paramIndex = 1;
                    stmt.setObject(paramIndex++, entity.globalId());

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
        String sql = "INSERT INTO property_value (entity_id, aspect_def_id, catalog_id, property_name, " +
            "value_text, value_integer, value_float, value_boolean, value_datetime, value_binary, " +
            "value_type, is_null) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (entity_id, aspect_def_id, catalog_id, property_name) DO UPDATE SET " +
            "value_text = EXCLUDED.value_text, " +
            "value_integer = EXCLUDED.value_integer, " +
            "value_float = EXCLUDED.value_float, " +
            "value_boolean = EXCLUDED.value_boolean, " +
            "value_datetime = EXCLUDED.value_datetime, " +
            "value_binary = EXCLUDED.value_binary, " +
            "value_type = EXCLUDED.value_type, " +
            "is_null = EXCLUDED.is_null";

        AspectDef aspectDef = aspect.def();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (PropertyDef propDef : aspectDef.propertyDefs()) {
                String propName = propDef.name();
                Object value = aspect.readObj(propName);

                stmt.setObject(1, entityId);
                stmt.setObject(2, aspectDefId);
                stmt.setObject(3, catalogId);
                stmt.setString(4, propName);

                // Set all value columns to null initially
                stmt.setString(5, null);  // value_text
                stmt.setLong(6, 0);       // value_integer
                stmt.setDouble(7, 0.0);   // value_float
                stmt.setBoolean(8, false); // value_boolean
                stmt.setTimestamp(9, null); // value_datetime
                stmt.setBytes(10, null);    // value_binary

                boolean isNull = (value == null);
                stmt.setBoolean(12, isNull);

                if (!isNull) {
                    PropertyType type = propDef.type();
                    stmt.setString(11, mapPropertyTypeToDbType(type));

                    switch (type) {
                        case Integer -> {
                            stmt.setLong(6, ((Number) value).longValue());
                        }
                        case Float -> {
                            stmt.setDouble(7, ((Number) value).doubleValue());
                        }
                        case Boolean -> {
                            stmt.setBoolean(8, (Boolean) value);
                        }
                        case DateTime -> {
                            stmt.setTimestamp(9, convertToTimestamp(value));
                        }
                        case BLOB -> {
                            stmt.setBytes(10, (byte[]) value);
                        }
                        default -> {
                            // All other types stored as text
                            stmt.setString(5, value.toString());
                        }
                    }
                } else {
                    stmt.setString(11, mapPropertyTypeToDbType(propDef.type()));
                }

                stmt.addBatch();
            }

            stmt.executeBatch();
        }
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
            stmt.setObject(1, catalog.globalId());
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
            stmt.setObject(1, catalog.globalId());
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
            stmt.setObject(1, catalogId);
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID entityId = rs.getObject("entity_id", UUID.class);
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
            stmt.setObject(1, catalogId);
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID entityId = rs.getObject("entity_id", UUID.class);
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
            stmt.setObject(1, catalogId);
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("entity_key");
                    UUID entityId = rs.getObject("entity_id", UUID.class);
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
            stmt.setObject(1, catalogId);
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID nodeId = rs.getObject("node_id", UUID.class);
                    UUID parentNodeId = rs.getObject("parent_node_id", UUID.class);
                    String nodeKey = rs.getString("node_key");
                    UUID entityId = rs.getObject("entity_id", UUID.class);

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
            stmt.setObject(1, catalogId);
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID entityId = rs.getObject("entity_id", UUID.class);

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
        StringBuilder columns = new StringBuilder("entity_id");
        for (String columnName : mapping.propertyToColumnMap().values()) {
            columns.append(", ").append(columnName);
        }

        String sql = "SELECT " + columns + " FROM " + mapping.tableName();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID entityId = rs.getObject("entity_id", UUID.class);
                Entity entity = factory.getOrRegisterNewEntity(entityId);

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

    private Aspect loadAspect(Connection conn, Entity entity, AspectDef aspectDef, UUID catalogId) throws SQLException
    {
        Aspect aspect = factory.createPropertyMapAspect(entity, aspectDef);

        String sql = "SELECT property_name, value_text, value_integer, value_float, value_boolean, " +
            "value_datetime, value_binary, value_type, is_null " +
            "FROM property_value " +
            "WHERE entity_id = ? AND aspect_def_id = ? AND catalog_id = ?";

        UUID aspectDefId = getAspectDefId(conn, aspectDef.name());

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, entity.globalId());
            stmt.setObject(2, aspectDefId);
            stmt.setObject(3, catalogId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String propertyName = rs.getString("property_name");
                    String valueType = rs.getString("value_type");
                    boolean isNull = rs.getBoolean("is_null");

                    PropertyDef propDef = aspectDef.propertyDef(propertyName);
                    if (propDef != null) {
                        Object value = null;
                        if (!isNull) {
                            value = extractValueFromResultSet(rs, mapDbTypeToPropertyType(valueType));
                        }

                        Property property = factory.createProperty(propDef, value);
                        aspect.put(property);
                    }
                }
            }
        }

        return aspect;
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
        String aspectSql = "SELECT hash_version, is_readable, is_writable, can_add_properties, can_remove_properties " +
            "FROM aspect_def WHERE name = ?";

        String hashVersion = null;
        boolean isReadable = true, isWritable = true, canAddProperties = false, canRemoveProperties = false;

        try (PreparedStatement stmt = conn.prepareStatement(aspectSql)) {
            stmt.setString(1, aspectDefName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    hashVersion = rs.getString("hash_version");
                    isReadable = rs.getBoolean("is_readable");
                    isWritable = rs.getBoolean("is_writable");
                    canAddProperties = rs.getBoolean("can_add_properties");
                    canRemoveProperties = rs.getBoolean("can_remove_properties");
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
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, catalogId);
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
                    return rs.getObject("aspect_def_id", UUID.class);
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
            case UUID -> stmt.setObject(paramIndex, value instanceof UUID ? value : UUID.fromString(value.toString()));
            case BLOB -> stmt.setBytes(paramIndex, (byte[]) value);
            default -> stmt.setString(paramIndex, value.toString());
        }
    }

    /**
     * Extracts a property value from a ResultSet based on the PropertyType.
     * Used when loading aspects from the default property_value table.
     */
    private Object extractValueFromResultSet(ResultSet rs, PropertyType type) throws SQLException
    {
        return switch (type) {
            case Integer -> rs.getLong("value_integer");
            case Float -> rs.getDouble("value_float");
            case Boolean -> rs.getBoolean("value_boolean");
            case DateTime -> rs.getTimestamp("value_datetime");
            case BLOB -> rs.getBytes("value_binary");
            default -> rs.getString("value_text");
        };
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
        String ddlContent = loadDdlResource("/db/schemas/postgres-cheap.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/postgres-cheap-audit.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/postgres-cheap-drop.sql");
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
        String ddlContent = loadDdlResource("/db/schemas/postgres-cheap-truncate.sql");
        executeDdl(dataSource, ddlContent);
    }
}