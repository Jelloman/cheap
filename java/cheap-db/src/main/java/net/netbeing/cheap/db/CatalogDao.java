package net.netbeing.cheap.db;

import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.Map;
import java.util.UUID;

/**
 * Data Access Object for persisting and loading complete Catalog instances
 * to/from a PostgreSQL database using the Cheap schema.
 */
@SuppressWarnings("DuplicateBranchesInSwitch")
public class CatalogDao implements CatalogPersistence
{

    private final DataSource dataSource;
    private final CheapFactory factory;

    public CatalogDao(@NotNull DataSource dataSource)
    {
        this.dataSource = dataSource;
        this.factory = new CheapFactory();
    }

    public CatalogDao(@NotNull DataSource dataSource, @NotNull CheapFactory factory)
    {
        this.dataSource = dataSource;
        this.factory = factory;
    }

    private static String loadDdlResource(String resourcePath) throws SQLException
    {
        try (var inputStream = CatalogDao.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new SQLException("DDL resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
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

        // Save AspectDefs
        for (AspectDef aspectDef : catalog.aspectDefs()) {
            saveAspectDef(conn, aspectDef);
            // Link the AspectDef to this Catalog
            linkCatalogToAspectDef(conn, catalog.globalId(), aspectDef);
        }

        // Save the Catalog table record
        saveCatalogRecord(conn, catalog);

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
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (name) DO NOTHING";
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
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (aspect_def_id, name) DO NOTHING";
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
            + "VALUES (?, ?, ?, ?, ?) ON CONFLICT (catalog_id) DO NOTHING";
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
            "VALUES (?, ?, ?, ?) ON CONFLICT (catalog_id, name) DO NOTHING";
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
        String sql = "INSERT INTO hierarchy_entity_list (hierarchy_catalog_id, hierarchy_name, entity_id, list_order) VALUES (?, ?, ?, ?)";
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
        String sql = "INSERT INTO hierarchy_entity_set (hierarchy_catalog_id, hierarchy_name, entity_id, set_order) VALUES (?, ?, ?, ?)";
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
        String sql = "INSERT INTO hierarchy_entity_directory (hierarchy_catalog_id, hierarchy_name, entity_key, entity_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String key : hierarchy.keySet()) {
                Entity entity = hierarchy.get(key);
                if (entity != null) {
                    saveEntity(conn, entity);
                    stmt.setObject(1, catalogId);
                    stmt.setString(2, hierarchyName);
                    stmt.setString(3, key);
                    stmt.setObject(4, entity.globalId());
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }

    private void saveEntityTreeContent(Connection conn, UUID catalogId, String hierarchyName, EntityTreeHierarchy hierarchy) throws SQLException
    {
        // Save tree nodes recursively
        saveTreeNode(conn, catalogId, hierarchyName, hierarchy.root(), "", "", null);
    }

    private void saveTreeNode(Connection conn, UUID catalogId, String hierarchyName, EntityTreeHierarchy.Node node,
                              String nodeKey, String nodePath, UUID parentNodeId) throws SQLException
    {
        UUID nodeId = UUID.randomUUID();
        UUID entityId = node.value() == null ? null : node.value().globalId();

        String sql = "INSERT INTO hierarchy_entity_tree_node " +
            "(node_id, hierarchy_catalog_id, hierarchy_name, parent_node_id, node_key, entity_id, node_path) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, nodeId);
            stmt.setObject(2, catalogId);
            stmt.setString(3, hierarchyName);
            stmt.setObject(4, parentNodeId);
            stmt.setString(5, nodeKey);
            stmt.setObject(6, entityId);
            stmt.setString(7, nodePath); // node.path() - method needs checking
            stmt.executeUpdate();
        }

        // Recursively save children
        if (!node.isLeaf()) {
            for (var entry : node.entrySet()) {
                String name = entry.getKey();
                String childPath = nodePath + '/' + name;
                EntityTreeHierarchy.Node child = entry.getValue();
                if (child != null) {
                    saveTreeNode(conn, catalogId, hierarchyName, child, name, childPath, nodeId);
                }
            }
        }
    }

    private void saveAspectMapContent(Connection conn, UUID catalogId, String hierarchyName, AspectMapHierarchy hierarchy) throws SQLException
    {
        String aspectSql = "INSERT INTO aspect (entity_id, aspect_def_id, catalog_id, hierarchy_catalog_id, hierarchy_name) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        String hierarchyMapSql = "INSERT INTO hierarchy_aspect_map (hierarchy_catalog_id, hierarchy_name, entity_id, aspect_def_id, catalog_id, map_order) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        UUID aspectDefId = getAspectDefId(conn, hierarchy.aspectDef().name());

        try (PreparedStatement aspectStmt = conn.prepareStatement(aspectSql);
             PreparedStatement mapStmt = conn.prepareStatement(hierarchyMapSql))
        {
            int order = 0;
            for (Entity entity : hierarchy.keySet()) {
                saveEntity(conn, entity);

                Aspect aspect = hierarchy.get(entity);
                if (aspect != null) {
                    // Save aspect
                    aspectStmt.setObject(1, entity.globalId());
                    aspectStmt.setObject(2, aspectDefId);
                    aspectStmt.setObject(3, catalogId);
                    aspectStmt.setObject(4, catalogId);
                    aspectStmt.setString(5, hierarchyName);
                    aspectStmt.addBatch();

                    // Save hierarchy mapping
                    mapStmt.setObject(1, catalogId);
                    mapStmt.setString(2, hierarchyName);
                    mapStmt.setObject(3, entity.globalId());
                    mapStmt.setObject(4, aspectDefId);
                    mapStmt.setObject(5, catalogId);
                    mapStmt.setInt(6, order++);
                    mapStmt.addBatch();

                    // Save properties
                    saveAspectProperties(conn, entity.globalId(), aspectDefId, catalogId, aspect);
                }
            }

            aspectStmt.executeBatch();
            mapStmt.executeBatch();
        }
    }

    private void saveAspectProperties(Connection conn, UUID entityId, UUID aspectDefId, UUID catalogId, Aspect aspect) throws SQLException
    {
        String sql = "INSERT INTO property_value (entity_id, aspect_def_id, catalog_id, property_name, " +
            "value_text, value_integer, value_float, value_boolean, value_datetime, value_binary, " +
            "value_type, is_null) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Property saving is simplified for now - would need full aspect interface implementation
            // This is a placeholder that saves no properties
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

                // Load and add all hierarchies
                loadHierarchies(conn, catalog);

                return catalog;
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

                    Hierarchy hierarchy = createAndLoadHierarchy(conn, catalog, type, name, version);
                    catalog.addHierarchy(hierarchy);
                }
            }
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
        String sql = "SELECT entity_id, list_order FROM hierarchy_entity_list WHERE hierarchy_catalog_id = ? AND hierarchy_name = ? ORDER BY list_order";
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
        String sql = "SELECT entity_id FROM hierarchy_entity_set WHERE hierarchy_catalog_id = ? AND hierarchy_name = ? ORDER BY set_order";
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
        String sql = "SELECT entity_key, entity_id FROM hierarchy_entity_directory WHERE hierarchy_catalog_id = ? AND hierarchy_name = ?";
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
        // Load tree structure - simplified implementation
        // In a full implementation, would need to reconstruct the tree structure
    }

    private void loadAspectMapContent(Connection conn, UUID catalogId, String hierarchyName, AspectMapHierarchy hierarchy) throws SQLException
    {
        String sql = "SELECT entity_id FROM hierarchy_aspect_map WHERE hierarchy_catalog_id = ? AND hierarchy_name = ? ORDER BY map_order";
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
                        // Note: Would need to add property to aspect - depends on aspect implementation
                    }
                }
            }
        }

        return aspect;
    }

    private AspectDef loadAspectDefForHierarchy(Connection conn, UUID catalogId, String hierarchyName) throws SQLException
    {
        String sql = "SELECT ad.name FROM hierarchy_aspect_map ham " +
            "JOIN aspect_def ad ON ham.aspect_def_id = ad.aspect_def_id " +
            "WHERE ham.hierarchy_catalog_id = ? AND ham.hierarchy_name = ? LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, catalogId);
            stmt.setString(2, hierarchyName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String aspectDefName = rs.getString("name");
                    return loadAspectDef(conn, aspectDefName);
                }
            }
        }

        throw new SQLException("Could not find AspectDef for hierarchy: " + hierarchyName + " in catalog " + catalogId);
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

        MutableAspectDef aspectDef = factory.createMutableAspectDef(aspectDefName);

        // Load property definitions
        String propSql = "SELECT pd.name, pd.property_type, pd.default_value, pd.has_default_value, " +
            "pd.is_readable, pd.is_writable, pd.is_nullable, pd.is_removable, pd.is_multivalued " +
            "FROM property_def pd JOIN aspect_def ad ON pd.aspect_def_id = ad.aspect_def_id " +
            "WHERE ad.name = ?";

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

                    aspectDef.add(propDef);
                }
            }
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

    // Helper methods for ID lookups
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

    // Type mapping helper methods
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

    // ===== Static DDL Execution Methods =====

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
}