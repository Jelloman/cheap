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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
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
 *   <li>Orchestrating multi-step save operations (catalog, hierarchies, aspects)</li>
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
     * Logger for database operations. Subclasses may use this for logging.
     */
    protected final Logger logger;

    /**
     * Data source providing database connections.
     */
    protected final DataSource dataSource;

    /**
     * Factory for creating Cheap model objects (entities, aspects, hierarchies, etc.).
     * Maintains entity registry to ensure entity identity is preserved across loads.
     */
    protected final CheapFactory factory;

    /**
     * Registry of AspectTableMappings, keyed by AspectDef name.
     * When an AspectDef is registered here, its aspects are persisted to
     * a custom table instead of the default aspect/property_value tables.
     */
    protected final Map<String, AspectTableMapping> aspectTableMappings = new LinkedHashMap<>();

    /**
     * Constructs a new AbstractCheapDao with the given data source, factory, and logger.
     * This constructor allows sharing a CheapFactory instance across multiple DAOs
     * to maintain a consistent entity registry.
     *
     * @param dataSource the data source to use for database connections
     * @param factory the CheapFactory to use for object creation and entity management
     * @param logger the logger to use for database operations, or null to use default logger
     */
    protected AbstractCheapDao(@NotNull DataSource dataSource, @NotNull CheapFactory factory, Logger logger)
    {
        this.dataSource = dataSource;
        this.factory = factory;
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
        if (catalog == null) {
            throw new IllegalArgumentException("Catalog cannot be null");
        }

        try (Connection conn = dataSource.getConnection()) {
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
    protected abstract void linkCatalogToAspectDef(Connection conn, Catalog catalog, AspectDef aspectDef) throws SQLException;

    /**
     * Persists the catalog metadata record to the database.
     * This typically saves to a "catalog" table with columns for species, URI, upstream, and version.
     *
     * @param conn the database connection to use
     * @param catalog the Catalog whose metadata should be saved
     * @throws SQLException if database operation fails
     */
    protected abstract void saveCatalogRecord(Connection conn, Catalog catalog) throws SQLException;

    /**
     * Saves the content of a hierarchy based on its type.
     * Dispatches to the appropriate type-specific save method.
     *
     * @param conn the database connection to use
     * @param hierarchy the Hierarchy whose content should be saved
     * @throws SQLException if database operation fails
     */
    protected void saveHierarchyContent(Connection conn, Hierarchy hierarchy) throws SQLException
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
    protected abstract void saveEntityListContent(Connection conn, EntityListHierarchy hierarchy) throws SQLException;

    /**
     * Persists the content of an EntitySetHierarchy to the database.
     * This typically involves saving entity references along with their set order (if ordered).
     *
     * @param conn the database connection to use
     * @param hierarchy the EntitySetHierarchy to save
     * @throws SQLException if database operation fails
     */
    protected abstract void saveEntitySetContent(Connection conn, EntitySetHierarchy hierarchy) throws SQLException;

    /**
     * Persists the content of an EntityDirectoryHierarchy to the database.
     * This typically involves saving string keys mapped to entity references.
     *
     * @param conn the database connection to use
     * @param hierarchy the EntityDirectoryHierarchy to save
     * @throws SQLException if database operation fails
     */
    protected abstract void saveEntityDirectoryContent(Connection conn, EntityDirectoryHierarchy hierarchy) throws SQLException;

    /**
     * Persists the content of an EntityTreeHierarchy to the database.
     * This typically involves recursive traversal to save all nodes with parent-child relationships.
     *
     * @param conn the database connection to use
     * @param hierarchy the EntityTreeHierarchy to save
     * @throws SQLException if database operation fails
     */
    protected abstract void saveEntityTreeContent(Connection conn, EntityTreeHierarchy hierarchy) throws SQLException;

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
    protected abstract void saveAspectMapContentToDefaultTables(Connection conn, AspectMapHierarchy hierarchy) throws SQLException;

    /**
     * Persists the content of an AspectMapHierarchy to a custom mapped table.
     * This provides better query performance and type safety by using typed columns.
     *
     * @param conn the database connection to use
     * @param hierarchy the AspectMapHierarchy to save
     * @param mapping the AspectTableMapping defining the custom table structure
     * @throws SQLException if database operation fails
     */
    protected abstract void saveAspectMapContentToMappedTable(Connection conn, AspectMapHierarchy hierarchy, AspectTableMapping mapping) throws SQLException;

    @Override
    public String convertValueToString(Object value, PropertyType type) throws SQLException
    {
        return switch (type) {
            case DateTime -> convertToTimestamp(value).toString();
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

    @Override
    public Hierarchy createAndLoadHierarchy(Connection conn, Catalog catalog, HierarchyType type, String hierarchyName, long version) throws SQLException
    {
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

    // ===== Value Conversion Methods =====

    @Override
    public Timestamp convertToTimestamp(Object value)
    {
        return switch (value) {
            case Timestamp timestamp -> timestamp;
            case Date date -> new Timestamp(date.getTime());
            case Instant instant -> Timestamp.from(instant);
            case ZonedDateTime zonedDateTime -> Timestamp.from(zonedDateTime.toInstant());
            default -> throw new IllegalStateException("Unexpected value class for DateTime: " + value.getClass());
        };
    }

}