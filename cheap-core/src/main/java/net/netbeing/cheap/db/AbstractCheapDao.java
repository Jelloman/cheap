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

package net.netbeing.cheap.db;

import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract base class providing common functionality for CheapDao implementations.
 * Handles high-level save/load orchestration while delegating database-specific
 * operations to concrete subclasses.
 *
 * <p>This class implements the template method pattern, defining the overall
 * structure of save and load operations while requiring subclasses to provide
 * database-specific implementations for individual operations.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Managing AspectTableMapping registration and lookup</li>
 *   <li>Orchestrating multistep save operations (catalog, hierarchies, aspects)</li>
 *   <li>Routing save/load operations to appropriate mapped or default table handlers</li>
 *   <li>Providing common type conversion utilities</li>
 * </ul>
 *
 * <h2>Subclass Requirements</h2>
 * Concrete implementations must provide database-specific code for:
 * <ul>
 *   <li>Saving and loading each hierarchy type (ENTITY_LIST, ENTITY_SET, ENTITY_DIR, ENTITY_TREE, ASPECT_MAP)</li>
 *   <li>Saving catalog records and AspectDef linkages</li>
 *   <li>Loading AspectDefs and their associated hierarchies</li>
 *   <li>Handling aspects in both mapped tables and default tables</li>
 * </ul>
 *
 * @see CheapDao
 * @see AspectTableMapping
 * @see CheapFactory
 */
public abstract class AbstractCheapDao implements CheapDao
{
    /**
     * The database adapter used by this DAO.
     */
    protected final CheapJdbcAdapter adapter;

    /**
     * Logger for database operations. Subclasses may use this for logging.
     */
    protected final Logger logger;

    /**
     * Registry of AspectTableMappings, keyed by AspectDef name.
     * When an AspectDef is registered here, its aspects are persisted to
     * a custom table instead of the default aspect/property_value tables.
     */
    protected final Map<String, AspectTableMapping> aspectTableMappings = new LinkedHashMap<>();


    /**
     * Constructs a new AbstractCheapDao with the given database adapter and logger.
     *
     * @param adapter the database adapter
     * @param logger the logger to use for database operations, or null to use default logger
     */
    protected AbstractCheapDao(@NotNull CheapJdbcAdapter adapter, Logger logger)
    {
        this.adapter = adapter;
        this.logger = logger != null ? logger : LoggerFactory.getLogger(AbstractCheapDao.class);
    }

    @Override
    public void addAspectTableMapping(@NotNull AspectTableMapping mapping)
    {
        aspectTableMappings.put(mapping.aspectDef().name(), mapping);
    }

    @Override
    public AspectTableMapping getAspectTableMapping(@NotNull String aspectDefName)
    {
        return aspectTableMappings.get(aspectDefName);
    }

    @Override
    public void saveCatalog(@NotNull Catalog catalog) throws SQLException
    {
        if (catalog == null) { // NOSONAR
            throw new IllegalArgumentException("Catalog cannot be null");
        }

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
    public void saveCatalog(@NotNull Connection conn, @NotNull Catalog catalog) throws SQLException
    {
        // Save the Catalog entity itself first and foremost
        saveEntity(conn, catalog);

        // Save the Catalog table record (must be before linking aspect defs due to FK constraint)
        saveCatalogRecord(conn, catalog);

        // Save AspectDefs
        for (AspectDef aspectDef : catalog.aspectDefs()) {
            saveAspectDef(conn, aspectDef);
            // Link the AspectDef to this Catalog
            linkCatalogToAspectDef(conn, catalog, aspectDef);
        }

        // Save all entities, aspects, and properties from hierarchies
        for (Hierarchy hierarchy : catalog.hierarchies()) {
            saveHierarchy(conn, hierarchy);
            saveHierarchyContent(conn, hierarchy);
        }
    }

    /**
     * Creates a link between a Catalog and an AspectDef in the database.
     * This is typically implemented as an insert into a join table (e.g., catalog_aspect_def).
     *
     * <p>Implementations should use INSERT OR IGNORE or equivalent to make this operation idempotent.
     *
     * @param conn the database connection to use
     * @param catalog the Catalog to link
     * @param aspectDef the AspectDef to link
     * @throws SQLException if database operation fails
     */
    protected abstract void linkCatalogToAspectDef(@NotNull Connection conn, @NotNull Catalog catalog, @NotNull AspectDef aspectDef) throws SQLException;

    /**
     * Persists the catalog metadata record to the database.
     * This typically saves to a "catalog" table with columns for species, URI, upstream, and version.
     *
     * @param conn the database connection to use
     * @param catalog the Catalog whose metadata should be saved
     * @throws SQLException if database operation fails
     */
    protected abstract void saveCatalogRecord(@NotNull Connection conn, @NotNull Catalog catalog) throws SQLException;

    /**
     * Saves the content of a hierarchy based on its type.
     * Dispatches to the appropriate type-specific save method.
     *
     * @param conn the database connection to use
     * @param hierarchy the Hierarchy whose content should be saved
     * @throws SQLException if database operation fails
     */
    protected void saveHierarchyContent(@NotNull Connection conn, @NotNull Hierarchy hierarchy) throws SQLException
    {
        switch (hierarchy.type()) {
            case ENTITY_LIST -> saveEntityListContent(conn, (EntityListHierarchy) hierarchy);
            case ENTITY_SET -> saveEntitySetContent(conn, (EntitySetHierarchy) hierarchy);
            case ENTITY_DIR -> saveEntityDirectoryContent(conn, (EntityDirectoryHierarchy) hierarchy);
            case ENTITY_TREE -> saveEntityTreeContent(conn, (EntityTreeHierarchy) hierarchy);
            case ASPECT_MAP -> saveAspectMapContent(conn, (AspectMapHierarchy) hierarchy);
        }
    }

    /**
     * Persists the content of an EntityListHierarchy to the database.
     * This typically involves saving entity references along with their list order.
     *
     * @param conn the database connection to use
     * @param hierarchy the EntityListHierarchy to save
     * @throws SQLException if database operation fails
     */
    protected abstract void saveEntityListContent(@NotNull Connection conn, @NotNull EntityListHierarchy hierarchy) throws SQLException;

    /**
     * Persists the content of an EntitySetHierarchy to the database.
     * This typically involves saving entity references along with their set order (if ordered).
     *
     * @param conn the database connection to use
     * @param hierarchy the EntitySetHierarchy to save
     * @throws SQLException if database operation fails
     */
    protected abstract void saveEntitySetContent(@NotNull Connection conn, @NotNull EntitySetHierarchy hierarchy) throws SQLException;

    /**
     * Persists the content of an EntityDirectoryHierarchy to the database.
     * This typically involves saving string keys mapped to entity references.
     *
     * @param conn the database connection to use
     * @param hierarchy the EntityDirectoryHierarchy to save
     * @throws SQLException if database operation fails
     */
    protected abstract void saveEntityDirectoryContent(@NotNull Connection conn, @NotNull EntityDirectoryHierarchy hierarchy) throws SQLException;

    /**
     * Persists the content of an EntityTreeHierarchy to the database.
     * This typically involves recursive traversal to save all nodes with parent-child relationships.
     *
     * @param conn the database connection to use
     * @param hierarchy the EntityTreeHierarchy to save
     * @throws SQLException if database operation fails
     */
    protected abstract void saveEntityTreeContent(@NotNull Connection conn, @NotNull EntityTreeHierarchy hierarchy) throws SQLException;

    /**
     * Saves the content of an AspectMapHierarchy, routing to either mapped table
     * or default table implementation based on whether a mapping is registered.
     *
     * @param conn the database connection to use
     * @param hierarchy the AspectMapHierarchy to save
     * @throws SQLException if database operation fails
     */
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

    /**
     * Persists the content of an AspectMapHierarchy to the default aspect/property_value tables.
     * This provides schema flexibility at the cost of some query performance.
     *
     * @param conn the database connection to use
     * @param hierarchy the AspectMapHierarchy to save
     * @throws SQLException if database operation fails
     */
    protected abstract void saveAspectMapContentToDefaultTables(@NotNull Connection conn, @NotNull AspectMapHierarchy hierarchy) throws SQLException;

    /**
     * Persists the content of an AspectMapHierarchy to a custom mapped table.
     * This provides better query performance and type safety by using typed columns.
     *
     * @param conn the database connection to use
     * @param hierarchy the AspectMapHierarchy to save
     * @param mapping the AspectTableMapping defining the custom table structure
     * @throws SQLException if database operation fails
     */
    protected void saveAspectMapContentToMappedTable(Connection conn, AspectMapHierarchy hierarchy, AspectTableMapping mapping) throws SQLException
    {
        final UUID catalogId = hierarchy.catalog().globalId();
        // Pre-save cleanup based on flags
        clearMappedTable(conn, mapping, catalogId);
        // If hasEntityId, no pre-save cleanup needed (will use ON DUPLICATE KEY UPDATE)

        StringBuilder sql = buildAspectMapSql(mapping);

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (Map.Entry<Entity, Aspect> entry : hierarchy.entrySet()) {
                saveEntity(conn, entry.getKey());
                if (entry.getValue() != null) {
                    saveAspectToMappedTable(mapping, entry.getKey(), entry.getValue(), stmt, catalogId);
                }
            }
        }
    }

    protected abstract void clearMappedTable(@NotNull Connection conn, @NotNull AspectTableMapping mapping, @NotNull UUID catalogId) throws SQLException;

    protected abstract StringBuilder buildAspectMapSql(@NotNull AspectTableMapping mapping);

    protected abstract void setPropertyValue(@NotNull PreparedStatement stmt, int paramIndex, Object value, @NotNull PropertyType type) throws SQLException;

    protected void saveAspectToMappedTable(@NotNull AspectTableMapping mapping, @NotNull Entity entity, Aspect aspect, @NotNull PreparedStatement stmt, @NotNull UUID catalogId) throws SQLException
    {
        int paramIndex = 1;

        if (mapping.hasCatalogId()) {
            stmt.setString(paramIndex++, catalogId.toString());
        }

        if (mapping.hasEntityId()) {
            stmt.setString(paramIndex++, entity.globalId().toString());
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


    @Override
    public Catalog loadCatalog(@NotNull UUID catalogId) throws SQLException
    {
        try (Connection conn = adapter.getConnection()) {
            return loadCatalogWithConnection(conn, catalogId);
        }
    }

    @Override
    public Hierarchy createAndLoadHierarchy(@NotNull Connection conn, @NotNull Catalog catalog, @NotNull HierarchyType type, @NotNull String hierarchyName, long version) throws SQLException
    {
        CheapFactory factory = adapter.getFactory();
        switch (type) {
            case ENTITY_LIST -> {
                EntityListHierarchy hierarchy = factory.createEntityListHierarchy(catalog, hierarchyName, version);
                loadEntityListContent(conn, hierarchy);
                return hierarchy;
            }
            case ENTITY_SET -> {
                EntitySetHierarchy hierarchy = factory.createEntitySetHierarchy(catalog, hierarchyName, version);
                loadEntitySetContent(conn, hierarchy);
                return hierarchy;
            }
            case ENTITY_DIR -> {
                EntityDirectoryHierarchy hierarchy = factory.createEntityDirectoryHierarchy(catalog, hierarchyName, version);
                loadEntityDirectoryContent(conn, hierarchy);
                return hierarchy;
            }
            case ENTITY_TREE -> {
                Entity rootEntity = factory.createEntity();
                EntityTreeHierarchy hierarchy = factory.createEntityTreeHierarchy(catalog, hierarchyName, rootEntity);
                loadEntityTreeContent(conn, hierarchy);
                return hierarchy;
            }
            case ASPECT_MAP -> {
                AspectDef aspectDef = loadAspectDefForHierarchy(conn, catalog.globalId(), hierarchyName);
                AspectMapHierarchy hierarchy = factory.createAspectMapHierarchy(catalog, aspectDef, version);
                loadAspectMapContent(conn, hierarchy);
                return hierarchy;
            }
            default -> throw new IllegalArgumentException("Unknown hierarchy type: " + type);
        }
    }

    /**
     * Loads the content of an EntityListHierarchy from the database.
     * This typically involves reading entity references and adding them to the hierarchy in list order.
     *
     * @param conn the database connection to use
     * @param hierarchy the EntityListHierarchy to populate
     * @throws SQLException if database operation fails
     */
    protected abstract void loadEntityListContent(Connection conn, EntityListHierarchy hierarchy) throws SQLException;

    /**
     * Loads the content of an EntitySetHierarchy from the database.
     * This typically involves reading entity references and adding them to the hierarchy in set order.
     *
     * @param conn the database connection to use
     * @param hierarchy the EntitySetHierarchy to populate
     * @throws SQLException if database operation fails
     */
    protected abstract void loadEntitySetContent(Connection conn, EntitySetHierarchy hierarchy) throws SQLException;

    /**
     * Loads the content of an EntityDirectoryHierarchy from the database.
     * This typically involves reading string keys and their mapped entity references.
     *
     * @param conn the database connection to use
     * @param hierarchy the EntityDirectoryHierarchy to populate
     * @throws SQLException if database operation fails
     */
    protected abstract void loadEntityDirectoryContent(Connection conn, EntityDirectoryHierarchy hierarchy) throws SQLException;

    /**
     * Loads the content of an EntityTreeHierarchy from the database.
     * This typically involves reconstructing the tree structure from parent-child node relationships.
     *
     * @param conn the database connection to use
     * @param hierarchy the EntityTreeHierarchy to populate
     * @throws SQLException if database operation fails
     */
    protected abstract void loadEntityTreeContent(Connection conn, EntityTreeHierarchy hierarchy) throws SQLException;

    /**
     * Loads the content of an AspectMapHierarchy, routing to either mapped table
     * or default table implementation based on whether a mapping is registered.
     *
     * @param conn the database connection to use
     * @param hierarchy the AspectMapHierarchy to populate
     * @throws SQLException if database operation fails
     */
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

    /**
     * Loads the content of an AspectMapHierarchy from the default aspect/property_value tables.
     * This reconstructs aspects by reading properties from the generic property_value table.
     *
     * @param conn the database connection to use
     * @param hierarchy the AspectMapHierarchy to populate
     * @throws SQLException if database operation fails
     */
    protected abstract void loadAspectMapContentFromDefaultTables(Connection conn, AspectMapHierarchy hierarchy) throws SQLException;

    /**
     * Loads the content of an AspectMapHierarchy from a custom mapped table.
     * This reads aspects directly from typed columns in the mapped table.
     *
     * @param conn the database connection to use
     * @param hierarchy the AspectMapHierarchy to populate
     * @param mapping the AspectTableMapping defining the custom table structure
     * @throws SQLException if database operation fails
     */
    protected abstract void loadAspectMapContentFromMappedTable(Connection conn, AspectMapHierarchy hierarchy, AspectTableMapping mapping) throws SQLException;

    /**
     * Loads the AspectDef associated with a specific hierarchy in a catalog.
     * For AspectMap hierarchies, the hierarchy name typically matches the AspectDef name.
     *
     * @param conn the database connection to use
     * @param catalogId the ID of the catalog containing the hierarchy
     * @param hierarchyName the name of the hierarchy
     * @return the AspectDef for the hierarchy
     * @throws SQLException if database operation fails or AspectDef not found
     */
    protected abstract AspectDef loadAspectDefForHierarchy(Connection conn, UUID catalogId, String hierarchyName) throws SQLException;

    // ===== Common Load/Save Helper Methods =====

    /**
     * Loads AspectDefs for a catalog and extends the catalog with them.
     * This method is common to all database implementations.
     *
     * @param conn the database connection to use
     * @param catalog the Catalog to extend with AspectDefs
     * @throws SQLException if database operation fails
     */
    protected void loadAndExtendAspectDefs(Connection conn, Catalog catalog) throws SQLException
    {
        String sql = "SELECT ad.name FROM catalog_aspect_def cad " +
            "JOIN aspect_def ad ON cad.aspect_def_id = ad.aspect_def_id " +
            "WHERE cad.catalog_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setUuidParameter(stmt, 1, catalog.globalId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String aspectDefName = rs.getString("name");
                    AspectDef aspectDef = loadAspectDef(conn, aspectDefName);
                    catalog.extend(aspectDef);
                }
            }
        }
    }

    /**
     * Loads all hierarchies for a catalog from the database.
     * This method is common to all database implementations.
     *
     * @param conn the database connection to use
     * @param catalog the Catalog to load hierarchies into
     * @throws SQLException if database operation fails
     */
    protected void loadHierarchies(Connection conn, Catalog catalog) throws SQLException
    {
        String sql = "SELECT name, hierarchy_type, version_number FROM hierarchy WHERE catalog_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setUuidParameter(stmt, 1, catalog.globalId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String typeStr = rs.getString("hierarchy_type");
                    long version = rs.getLong("version_number");
                    HierarchyType type = HierarchyType.fromTypeCode(typeStr);

                    // Check if hierarchy already exists (it may have been created by extend())
                    Hierarchy existingHierarchy = catalog.hierarchy(name);
                    if (existingHierarchy != null) {
                        // Hierarchy already exists, load content into it based on type
                        loadExistingHierarchyContent(conn, existingHierarchy);
                    } else {
                        // Create and load new hierarchy
                        Hierarchy hierarchy = createAndLoadHierarchy(conn, catalog, type, name, version);
                        catalog.addHierarchy(hierarchy);
                    }
                }
            }
        }
    }

    /**
     * Dispatches to type-specific load methods based on hierarchy type.
     * This method is common to all database implementations.
     *
     * @param conn the database connection to use
     * @param hierarchy the Hierarchy to load content into
     * @throws SQLException if database operation fails
     */
    protected void loadExistingHierarchyContent(Connection conn, Hierarchy hierarchy) throws SQLException
    {
        switch (hierarchy.type()) {
            case ENTITY_LIST -> loadEntityListContent(conn, (EntityListHierarchy) hierarchy);
            case ENTITY_SET -> loadEntitySetContent(conn, (EntitySetHierarchy) hierarchy);
            case ENTITY_DIR -> loadEntityDirectoryContent(conn, (EntityDirectoryHierarchy) hierarchy);
            case ENTITY_TREE -> loadEntityTreeContent(conn, (EntityTreeHierarchy) hierarchy);
            case ASPECT_MAP -> loadAspectMapContent(conn, (AspectMapHierarchy) hierarchy);
            default -> throw new IllegalArgumentException("Unknown hierarchy type: " + hierarchy.type());
        }
    }

    /**
     * Saves a loaded property to an aspect, handling both single-valued and multivalued properties.
     * This method is common to all database implementations.
     *
     * @param aspect the Aspect to add the property to
     * @param propDef the PropertyDef defining the property structure
     * @param values the list of values loaded from the database
     */
    protected void saveLoadedProperty(Aspect aspect, PropertyDef propDef, List<Object> values)
    {
        if (values.isEmpty()) {
            // No rows found - for multivalued, this means empty list
            if (propDef.isMultivalued()) {
                Property property = adapter.getFactory().createProperty(propDef, Collections.emptyList());
                aspect.put(property);
            }
            // For single-valued, don't add the property (will use default value if available)
        } else if (propDef.isMultivalued()) {
            // Multivalued property - create property with list of all values
            Property property = adapter.getFactory().createProperty(propDef, new ArrayList<>(values));
            aspect.put(property);
        } else {
            // Single-valued property - use the first (and only) value
            Object value = values.getFirst();
            Property property = adapter.getFactory().createProperty(propDef, value);
            aspect.put(property);
        }
    }

    /**
     * Extracts a property value from the result set based on the property type.
     * Uses value_text for all types except BLOB (which uses value_binary).
     * This method is common to all database implementations.
     *
     * @param type the PropertyType indicating how to parse the value
     * @param valueText the text representation of the value (may be null)
     * @param valueBinary the binary representation of the value (may be null)
     * @return the parsed property value
     * @throws SQLException if parsing fails
     */
    protected Object extractPropertyValue(PropertyType type, String valueText, byte[] valueBinary) throws SQLException
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

    // ===== Abstract Methods for Database-Specific Type Handling =====

    /**
     * Sets a UUID parameter in a PreparedStatement using the database-specific approach.
     * Subclasses should implement this based on their database's UUID support.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param parameterIndex the parameter index (1-based)
     * @param value the UUID value to set
     * @throws SQLException if database operation fails
     */
    @SuppressWarnings("SameParameterValue")
    protected abstract void setUuidParameter(PreparedStatement stmt, int parameterIndex, UUID value) throws SQLException;

}