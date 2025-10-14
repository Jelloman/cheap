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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractCheapDao implements CheapDao
{
    protected final Logger logger;
    protected final DataSource dataSource;
    protected final CheapFactory factory;
    protected final Map<String, AspectTableMapping> aspectTableMappings = new LinkedHashMap<>();

    /**
     * Constructs a new PostgresDao with the given data source and factory.
     * This constructor allows sharing a CheapFactory instance across multiple DAOs
     * to maintain a consistent entity registry.
     *
     * @param dataSource the PostgreSQL data source to use for database operations
     * @param factory the CheapFactory to use for object creation and entity management
     */
    protected AbstractCheapDao(@NotNull DataSource dataSource, @NotNull CheapFactory factory, Logger logger)
    {
        this.dataSource = dataSource;
        this.factory = factory;
        this.logger = logger != null ? logger : LoggerFactory.getLogger(AbstractCheapDao.class);
    }

    /**
     * Adds an AspectTableMapping to enable aspects to be saved/loaded from a custom table.
     *
     * @param mapping the AspectTableMapping to add
     */
    @Override
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

    protected abstract void linkCatalogToAspectDef(Connection conn, Catalog catalog, AspectDef aspectDef) throws SQLException;

    protected abstract void saveCatalogRecord(Connection conn, Catalog catalog) throws SQLException;

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


    protected abstract void saveEntityListContent(Connection conn, EntityListHierarchy hierarchy) throws SQLException;

    protected abstract void saveEntitySetContent(Connection conn, EntitySetHierarchy hierarchy) throws SQLException;

    protected abstract void saveEntityDirectoryContent(Connection conn, EntityDirectoryHierarchy hierarchy) throws SQLException;

    protected abstract void saveEntityTreeContent(Connection conn, EntityTreeHierarchy hierarchy) throws SQLException;

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

    protected abstract void saveAspectMapContentToDefaultTables(Connection conn, AspectMapHierarchy hierarchy) throws SQLException;

    protected abstract void saveAspectMapContentToMappedTable(Connection conn, AspectMapHierarchy hierarchy, AspectTableMapping mapping) throws SQLException;

    /**
     * Converts a property value to its string representation for storage in value_text column.
     */
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

    protected abstract void loadEntityListContent(Connection conn, EntityListHierarchy hierarchy) throws SQLException;

    protected abstract void loadEntitySetContent(Connection conn, EntitySetHierarchy hierarchy) throws SQLException;

    protected abstract void loadEntityDirectoryContent(Connection conn, EntityDirectoryHierarchy hierarchy) throws SQLException;

    protected abstract void loadEntityTreeContent(Connection conn, EntityTreeHierarchy hierarchy) throws SQLException;

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

    protected abstract void loadAspectMapContentFromDefaultTables(Connection conn, AspectMapHierarchy hierarchy) throws SQLException;

    protected abstract void loadAspectMapContentFromMappedTable(Connection conn, AspectMapHierarchy hierarchy, AspectTableMapping mapping) throws SQLException;

    protected abstract AspectDef loadAspectDefForHierarchy(Connection conn, UUID catalogId, String hierarchyName) throws SQLException;

    // ===== Value Conversion Methods =====

    /**
     * Converts a DateTime value to a Timestamp for database storage.
     * Handles various date/time types including Timestamp, Date, Instant, and ZonedDateTime.
     *
     * @param value the date/time value
     * @return a Timestamp suitable for database storage
     * @throws IllegalStateException if the value type is not supported
     */
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