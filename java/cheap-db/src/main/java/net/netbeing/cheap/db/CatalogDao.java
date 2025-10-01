package net.netbeing.cheap.db;

import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;

/**
 * Data Access Object for persisting and loading complete Catalog instances
 * to/from a PostgreSQL database using the Cheap schema.
 */
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

        // TODO: Catalog.def() and Hierarchy.def() have been removed.
        // This persistence layer needs to be updated to work with the new model.
        // For now, just save AspectDefs directly from the catalog.

        // Save AspectDefs
        for (AspectDef aspectDef : catalog.aspectDefs()) {
            saveAspectDef(conn, aspectDef);
            // Link the AspectDef to this Catalog
            // TODO: populate the catalog_aspect_def table
        }

        // Save the Catalog table record
        saveCatalogRecord(conn, catalog);

        // Save all entities, aspects, and properties from hierarchies
        for (Hierarchy hierarchy : catalog.hierarchies()) {
            saveHierarchy(conn, hierarchy);
            saveHierarchyContent(conn, hierarchy);
        }
    }

    // TODO: CatalogDef no longer has globalId() - CatalogDef is now informational only
    // This method is commented out until the model is updated
    /*
    private void saveCatalogDef(Connection conn, CatalogDef catalogDef) throws SQLException
    {
        String sql = "INSERT INTO catalog_def (catalog_def_id, name, hash_version) " +
            "VALUES (?, ?, ?) ON CONFLICT (name) DO NOTHING";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, catalogDef.globalId());
            stmt.setString(2, "default_catalog_def"); // Use a default name since CatalogDef doesn't have name()
            stmt.setString(3, "TODO_HASH"); // TODO: Fix hash serialization
            stmt.executeUpdate();
        }
    }
    */

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

        String sql = "INSERT INTO property_def (property_def_id, aspect_def_id, name, property_type, " + "default_value, " +
            "has_default_value, is_readable, is_writable, is_nullable, is_removable, is_multivalued) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?," +
            " ?, ?, ?) ON CONFLICT (aspect_def_id, name) DO NOTHING";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.randomUUID());
            stmt.setObject(2, aspectDefId);
            stmt.setString(3, propDef.name());
            stmt.setString(4, mapPropertyTypeToDbType(propDef.type()));
            stmt.setString(5, propDef.hasDefaultValue() ? propDef.defaultValue().toString() : null);
            stmt.setBoolean(6, propDef.hasDefaultValue());
            stmt.setBoolean(7, propDef.isReadable());
            stmt.setBoolean(8, propDef.isWritable());
            stmt.setBoolean(9, propDef.isNullable());
            stmt.setBoolean(10, propDef.isRemovable());
            stmt.setBoolean(11, propDef.isMultivalued());
            stmt.executeUpdate();
        }
    }

    // TODO: HierarchyDef no longer has isModifiable() - HierarchyDef is now informational only
    // This method is commented out until the model is updated
    /*
    private void saveHierarchyDef(Connection conn, HierarchyDef hierarchyDef) throws SQLException
    {
        String sql = "INSERT INTO hierarchy_def (hierarchy_def_id, name, hierarchy_type, is_modifiable) "
            + "VALUES (?, ?, ?, ?) ON CONFLICT (name) DO NOTHING";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, UUID.randomUUID());
            stmt.setString(2, hierarchyDef.name());
            stmt.setString(3, mapHierarchyTypeToDbType(hierarchyDef.type()));
            stmt.setBoolean(4, hierarchyDef.isModifiable());
            stmt.executeUpdate();
        }
    }
    */

    private void linkCatalogDefToHierarchyDef(Connection conn, CatalogDef catalogDef, HierarchyDef hierarchyDef) throws SQLException
    {
        // Simplified implementation - CatalogDef linking is disabled for now
        // Would need proper CatalogDef interface methods to implement fully
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
        // TODO: Catalog.def() and Catalog.isStrict() no longer exist in the model
        // CatalogDef persistence is disabled for now
        UUID catalogDefId = null; // Was: catalog.def().globalId()
        if (catalogDefId.equals(catalog.globalId())) {
            // If the UUIDs of the Catalog and Def match, that means the Def is just a wrapper
            // and should not be persisted.
            catalogDefId = null;
        }

        String sql = "INSERT INTO catalog (catalog_id, catalog_def_id, species, uri, upstream_catalog_id, is_strict, version_number) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (catalog_id) DO NOTHING";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, catalog.globalId());
            stmt.setObject(2, catalogDefId);
            stmt.setString(3, catalog.species().name());
            stmt.setString(4, catalog.uri() != null ? catalog.uri().toString() : null);
            stmt.setObject(5, catalog.upstream());
            stmt.setBoolean(6, false); // Was: catalog.isStrict() - now defaults to false
            stmt.setLong(7, catalog.version());
            stmt.executeUpdate();
        }
    }

    private void saveHierarchy(Connection conn, Hierarchy hierarchy) throws SQLException
    {
        UUID catalogId = hierarchy.catalog().globalId();
        // TODO: Hierarchy.def() no longer exists - use hierarchy.name() directly
        UUID hierarchyDefOwnerId = getHierarchyDefOwnerId(conn, hierarchy.name());

        String sql = "INSERT INTO hierarchy (catalog_id, name, hierarchy_def_owner_id, hierarchy_def_name, version_number) " +
            "VALUES (?, ?, ?, ?, ?) ON CONFLICT (catalog_id, name) DO NOTHING";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, catalogId);
            stmt.setString(2, hierarchy.name()); // Was: hierarchy.def().name()
            stmt.setObject(3, hierarchyDefOwnerId);
            stmt.setString(4, hierarchy.name()); // Was: hierarchy.def().name()
            stmt.setLong(5, hierarchy.version());
            stmt.executeUpdate();
        }
    }

    private void saveHierarchyContent(Connection conn, Hierarchy hierarchy) throws SQLException
    {
        UUID catalogId = hierarchy.catalog().globalId();
        // TODO: Hierarchy.def() no longer exists - use hierarchy.name() and hierarchy.type() directly
        String hierarchyName = hierarchy.name(); // Was: hierarchy.def().name()

        switch (hierarchy.type()) { // Was: hierarchy.def().type()
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
        saveTreeNode(conn, catalogId, hierarchyName, hierarchy.root(), null);
    }

    private void saveTreeNode(Connection conn, UUID catalogId, String hierarchyName, EntityTreeHierarchy.Node node, UUID parentNodeId) throws SQLException
    {
        UUID nodeId = UUID.randomUUID();

        String sql = "INSERT INTO hierarchy_entity_tree_node (node_id, hierarchy_catalog_id, hierarchy_name, parent_node_id, node_key, entity_id, node_path) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, nodeId);
            stmt.setObject(2, catalogId);
            stmt.setString(3, hierarchyName);
            stmt.setObject(4, parentNodeId);
            stmt.setString(5, null); // node.key() - method needs checking
            stmt.setObject(6, null); // node.entity() - method needs checking
            stmt.setString(7, null); // node.path() - method needs checking
            stmt.executeUpdate();
        }

        // Save entity if present
        // if (node.entity() != null) {
        //     saveEntity(conn, node.entity());
        // }

        // Recursively save children
        // for (String childKey : node.childKeys()) {
        //     EntityTreeHierarchy.Node child = node.child(childKey);
        //     if (child != null) {
        //         saveTreeNode(conn, hierarchyId, child, nodeId);
        //     }
        // }
    }

    private void saveAspectMapContent(Connection conn, UUID catalogId, String hierarchyName, AspectMapHierarchy hierarchy) throws SQLException
    {
        String aspectSql = "INSERT INTO aspect (entity_id, aspect_def_id, catalog_id, hierarchy_catalog_id, hierarchy_name, is_transferable) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        String hierarchyMapSql = "INSERT INTO hierarchy_aspect_map (hierarchy_catalog_id, hierarchy_name, entity_id, aspect_def_id, catalog_id, map_order) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        UUID aspectDefId = getAspectDefId(conn, hierarchy.aspectDef().name());

        try (PreparedStatement aspectStmt = conn.prepareStatement(aspectSql); PreparedStatement mapStmt =
            conn.prepareStatement(hierarchyMapSql)) {

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
                    aspectStmt.setBoolean(6, false); // Default transferable to false
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
        String sql = "SELECT c.catalog_id, c.species, c.uri, c.upstream_catalog_id, c.is_strict, c.version_number, cd.name as catalog_def_name " + "FROM " +
            "catalog c LEFT JOIN catalog_def cd ON c.catalog_def_id = cd.catalog_def_id " + "WHERE c.catalog_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, catalogId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null; // Catalog not found
                }

                CatalogSpecies species = CatalogSpecies.valueOf(rs.getString("species"));
                String uriStr = rs.getString("uri");
                UUID upstream = rs.getObject("upstream_catalog_id", UUID.class);
                boolean isStrict = rs.getBoolean("is_strict"); // Read but ignore for now
                long version = rs.getLong("version_number");
                String catalogDefName = rs.getString("catalog_def_name");

                // Load CatalogDef if exists
                CatalogDef catalogDef = null;
                if (catalogDefName != null) {
                    catalogDef = loadCatalogDef(conn, catalogDefName);
                }

                // Create catalog with version
                // Create catalog with version - new signature: (UUID, CatalogSpecies, UUID upstream, long version)
                Catalog catalog = factory.createCatalog(catalogId, species, upstream, version);

                // Load and add all hierarchies
                loadHierarchies(conn, catalog);

                return catalog;
            }
        }
    }

    private CatalogDef loadCatalogDef(Connection conn, String catalogDefName) throws SQLException
    {
        // For simplicity, creating empty CatalogDef - could be enhanced to load full structure
        return factory.createCatalogDef();
    }

    private void loadHierarchies(Connection conn, Catalog catalog) throws SQLException
    {
        String sql = "SELECT h.name, h.version_number, hd.hierarchy_type " +
            "FROM hierarchy h " +
            "JOIN hierarchy_def hd ON h.hierarchy_def_owner_id = hd.owner_id AND h.hierarchy_def_name = hd.name " +
            "WHERE h.catalog_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, catalog.globalId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    long version = rs.getLong("version_number");
                    String typeStr = rs.getString("hierarchy_type");
                    HierarchyType type = mapDbTypeToHierarchyType(typeStr);

                    HierarchyDef hierarchyDef = factory.createHierarchyDef(name, type);

                    Hierarchy hierarchy = createAndLoadHierarchy(conn, catalog, hierarchyDef, name, version);
                    catalog.addHierarchy(hierarchy);
                }
            }
        }
    }

    private Hierarchy createAndLoadHierarchy(Connection conn, Catalog catalog, HierarchyDef def, String hierarchyName, long version) throws SQLException
    {
        switch (def.type()) {
            case ENTITY_LIST -> {
                // New signature: (Catalog, String name, long version)
                EntityListHierarchy hierarchy = factory.createEntityListHierarchy(catalog, hierarchyName, version);
                loadEntityListContent(conn, catalog.globalId(), hierarchyName, hierarchy);
                return hierarchy;
            }
            case ENTITY_SET -> {
                // New signature: (Catalog, String name, long version)
                EntitySetHierarchy hierarchy = factory.createEntitySetHierarchy(catalog, hierarchyName, version);
                loadEntitySetContent(conn, catalog.globalId(), hierarchyName, hierarchy);
                return hierarchy;
            }
            case ENTITY_DIR -> {
                // New signature: (Catalog, String name, long version)
                EntityDirectoryHierarchy hierarchy = factory.createEntityDirectoryHierarchy(catalog, hierarchyName, version);
                loadEntityDirectoryContent(conn, catalog.globalId(), hierarchyName, hierarchy);
                return hierarchy;
            }
            case ENTITY_TREE -> {
                Entity rootEntity = factory.createEntity();
                // New signature: (Catalog, String name, Entity rootEntity)
                // Note: No version parameter for tree hierarchy in current factory
                EntityTreeHierarchy hierarchy = factory.createEntityTreeHierarchy(catalog, hierarchyName, rootEntity);
                loadEntityTreeContent(conn, catalog.globalId(), hierarchyName, hierarchy);
                return hierarchy;
            }
            case ASPECT_MAP -> {
                // Need to get AspectDef for this hierarchy
                AspectDef aspectDef = loadAspectDefForHierarchy(conn, catalog.globalId(), hierarchyName);
                // New signature: (Catalog, AspectDef, long version)
                AspectMapHierarchy hierarchy = factory.createAspectMapHierarchy(catalog, aspectDef, version);
                loadAspectMapContent(conn, catalog.globalId(), hierarchyName, hierarchy);
                return hierarchy;
            }
            default -> throw new IllegalArgumentException("Unknown hierarchy type: " + def.type());
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

        AspectDef aspectDef = factory.createMutableAspectDef(aspectDefName);

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

                    // Add to mutable aspect def - would need access to add method
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
    private UUID getCatalogDefId(Connection conn, String name) throws SQLException
    {
        String sql = "SELECT catalog_def_id FROM catalog_def WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("catalog_def_id", UUID.class);
                }
            }
        }
        throw new SQLException("CatalogDef not found: " + name);
    }

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

    private UUID getHierarchyDefId(Connection conn, String name) throws SQLException
    {
        String sql = "SELECT hierarchy_def_id FROM hierarchy_def WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("hierarchy_def_id", UUID.class);
                }
            }
        }
        throw new SQLException("HierarchyDef not found: " + name);
    }

    // TODO: HierarchyDef no longer passed - use hierarchy name instead
    private UUID getHierarchyDefOwnerId(Connection conn, String hierarchyName) throws SQLException
    {
        // For now, create a simple catalog-def-owned hierarchy definition owner
        // This is a simplified implementation - in a full implementation would need to handle both catalog and hierarchy ownership
        String sql = "INSERT INTO hierarchy_def_owner (owner_type_code, catalog_def_id) VALUES ('C', NULL) RETURNING owner_id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("owner_id", UUID.class);
                }
            }
        }
        throw new SQLException("Failed to create hierarchy definition owner for: " + hierarchyName);
    }

    private UUID getPropertyDefId(Connection conn, String aspectDefName, String propertyName) throws SQLException
    {
        String sql = "SELECT pd.property_def_id FROM property_def pd " + "JOIN aspect_def ad ON pd.aspect_def_id = ad.aspect_def_id " +
            "WHERE ad.name = ? AND pd.name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, aspectDefName);
            stmt.setString(2, propertyName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("property_def_id", UUID.class);
                }
            }
        }
        throw new SQLException("PropertyDef not found: " + propertyName + " in AspectDef " + aspectDefName);
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
        String ddlContent = loadDdlResource("/db/schemas/postgres-cheap.ddl");
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
        String ddlContent = loadDdlResource("/db/schemas/postgres-cheap-audit.ddl");
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
        String ddlContent = loadDdlResource("/db/schemas/postgres-cheap-drop.ddl");
        executeDdl(dataSource, ddlContent);
    }
}