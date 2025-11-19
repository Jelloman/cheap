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

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.model.HierarchyType;
import net.netbeing.cheap.model.PropertyType;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Interface for saving and loading Cheap elements to/from a database.
 * Provides operations to persist the entire Cheap model structure including
 * definitions, entities, aspects, properties, and hierarchies.
 *
 * <p>This interface supports two persistence strategies for aspects:
 * <ul>
 *   <li><b>Default Tables:</b> Uses generic aspect/property_value tables for flexible schema</li>
 *   <li><b>Mapped Tables:</b> Uses custom tables with typed columns for better performance</li>
 * </ul>
 *
 * <p>Implementations must handle transactional integrity, ensuring that catalog saves
 * and loads are atomic operations. Foreign key constraints should be enforced where
 * supported by the underlying database.
 *
 * @see AspectTableMapping
 * @see Catalog
 * @see AbstractCheapDao
 */
public interface CheapDao extends AutoCloseable
{
    /**
     * Checks if a catalog exists in the database.
     *
     * @param catalogId the global ID of the catalog to check
     * @return true if the catalog exists, false otherwise
     * @throws SQLException if database operation fails
     */
    boolean catalogExists(@NotNull UUID catalogId) throws SQLException;

    /**
     * Registers an AspectTableMapping to enable aspects to be persisted in a custom table
     * with typed columns instead of the generic aspect/property_value tables.
     *
     * <p>Once registered, all save and load operations for the mapped AspectDef will use
     * the custom table. This provides better query performance and type safety at the
     * cost of requiring a predefined schema.
     *
     * @param mapping the AspectTableMapping defining the custom table structure
     */
    void addAspectTableMapping(@NotNull AspectTableMapping mapping);

    /**
     * Retrieves the AspectTableMapping for a given AspectDef name, if one has been registered.
     *
     * @param aspectDefName the fully-qualified name of the AspectDef
     * @return the registered AspectTableMapping, or null if no mapping exists
     */
    AspectTableMapping getAspectTableMapping(@NotNull String aspectDefName);

    /**
     * Creates a database table based on an AspectTableMapping definition.
     * The table structure includes optional catalog_id and entity_id columns,
     * plus typed columns for each mapped property.
     *
     * <p>Implementations should use "CREATE TABLE IF NOT EXISTS" or equivalent
     * to make this operation idempotent.
     *
     * @param mapping the AspectTableMapping defining the table structure
     * @throws SQLException if table creation fails
     */
    void createTable(@NotNull AspectTableMapping mapping) throws SQLException;

    /**
     * Maps a Cheap PropertyType to the corresponding SQL column type for the target database.
     * Implementations must provide appropriate type mappings for their specific database system.
     *
     * <p>Common mappings include:
     * <ul>
     *   <li>Integer → BIGINT or INTEGER</li>
     *   <li>Float → DOUBLE or REAL</li>
     *   <li>String/Text → VARCHAR or TEXT</li>
     *   <li>DateTime → TIMESTAMP or TEXT</li>
     *   <li>UUID → UUID or TEXT</li>
     * </ul>
     *
     * @param type the PropertyType to map
     * @return the SQL type name for the target database
     */
    String mapPropertyTypeToSqlType(@NotNull PropertyType type);

    /**
     * Saves a complete catalog to the database, including all its definitions,
     * entities, aspects, properties, and hierarchies.
     *
     * <p>This method creates a new database connection and wraps the operation in a transaction.
     * If any part of the save fails, all changes are rolled back.
     *
     * <p>The save process follows this order:
     * <ol>
     *   <li>Save the Catalog entity and catalog record</li>
     *   <li>Save all AspectDefs and link them to the catalog</li>
     *   <li>Save all Hierarchies and their content (entities, aspects, properties)</li>
     * </ol>
     *
     * @param catalog the catalog to save
     * @throws SQLException if database operation fails
     * @throws IllegalArgumentException if catalog is null or has invalid state
     */
    void saveCatalog(@NotNull Catalog catalog) throws SQLException;

    /**
     * Saves a complete catalog using an existing database connection.
     * This variant is useful for integrating catalog saves into larger transactions.
     *
     * <p>The caller is responsible for transaction management (commit/rollback).
     * The connection's auto-commit setting is not modified.
     *
     * @param conn the database connection to use
     * @param catalog the catalog to save
     * @throws SQLException if database operation fails
     */
    void saveCatalog(@NotNull Connection conn, @NotNull Catalog catalog) throws SQLException;

    /**
     * Persists an AspectDef and all its PropertyDefs to the database.
     * If the AspectDef already exists (based on name), it will be updated.
     *
     * @param conn the database connection to use
     * @param aspectDef the AspectDef to save
     * @throws SQLException if database operation fails
     */
    void saveAspectDef(@NotNull Connection conn, @NotNull AspectDef aspectDef) throws SQLException;

    /*
     * Persists an Entity to the database. If the entity already exists
     * (based on global ID), this operation is a no-op.
     *
     * @param conn the database connection to use
     * @param entity the Entity to save
     * @throws SQLException if database operation fails
     * void saveEntity(@NotNull Connection conn, @NotNull Entity entity) throws SQLException;
     */

    /**
     * Persists a Hierarchy metadata record to the database.
     * This saves the hierarchy definition but not its content.
     * Use separate methods to save hierarchy content.
     *
     * @param conn the database connection to use
     * @param hierarchy the Hierarchy to save
     * @throws SQLException if database operation fails
     * @see #saveCatalog(Connection, Catalog)
     */
    void saveHierarchy(@NotNull Connection conn, @NotNull Hierarchy hierarchy) throws SQLException;

    /**
     * Loads a complete catalog from the database by its global ID.
     * Creates a new database connection for the operation.
     *
     * <p>The load process reconstructs the entire catalog structure including:
     * <ul>
     *   <li>All AspectDefs referenced by the catalog</li>
     *   <li>All Hierarchies and their content</li>
     *   <li>All Entities, Aspects, and Properties</li>
     * </ul>
     *
     * @param catalogId the global ID of the catalog to load
     * @return the loaded Catalog, or null if not found
     * @throws SQLException if database operation fails
     */
    Catalog loadCatalog(@NotNull UUID catalogId) throws SQLException;

    /**
     * Loads a complete catalog using an existing database connection.
     * This variant is useful for loading catalogs within a larger transaction.
     *
     * @param conn the database connection to use
     * @param catalogId the global ID of the catalog to load
     * @return the loaded Catalog, or null if not found
     * @throws SQLException if database operation fails
     */
    Catalog loadCatalogWithConnection(@NotNull Connection conn, @NotNull UUID catalogId) throws SQLException;

    /**
     * Creates a new Hierarchy instance of the specified type and loads its content
     * from the database. The hierarchy is created using the configured CheapFactory.
     *
     * <p>This method handles all five hierarchy types:
     * <ul>
     *   <li>ENTITY_LIST - ordered list with possible duplicates</li>
     *   <li>ENTITY_SET - unique entities with optional ordering</li>
     *   <li>ENTITY_DIR - string-to-entity mapping</li>
     *   <li>ENTITY_TREE - hierarchical tree structure</li>
     *   <li>ASPECT_MAP - entity-to-aspect mapping</li>
     * </ul>
     *
     * @param conn the database connection to use
     * @param catalog the parent Catalog
     * @param type the type of hierarchy to create
     * @param hierarchyName the name of the hierarchy
     * @param version the version number of the hierarchy
     * @return the newly created and loaded Hierarchy
     * @throws SQLException if database operation fails
     */
    Hierarchy createAndLoadHierarchy(@NotNull Connection conn, @NotNull Catalog catalog, @NotNull HierarchyType type, @NotNull String hierarchyName, long version) throws SQLException;

    /**
     * Loads an Aspect from the database for a specific entity and AspectDef.
     * The aspect's properties are loaded from either custom mapped tables or
     * the default property_value table, depending on configuration.
     *
     * <p>For multivalued properties, all values are loaded and returned as a List.
     * Properties not present in the database will use their default values if defined.
     *
     * @param conn the database connection to use
     * @param entity the Entity that owns the aspect
     * @param aspectDef the AspectDef defining the aspect structure
     * @param catalog the Catalog context for the aspect
     * @return the loaded Aspect with all its properties
     * @throws SQLException if database operation fails
     */
    Aspect loadAspect(@NotNull Connection conn, @NotNull Entity entity, @NotNull AspectDef aspectDef, @NotNull Catalog catalog) throws SQLException;

    /**
     * Loads an AspectDef from the database by its name and catalog ID.
     * This includes loading all PropertyDefs and reconstructing the appropriate
     * AspectDef implementation based on mutability flags.
     *
     * <p>The returned AspectDef may be:
     * <ul>
     *   <li>ImmutableAspectDef - if canAddProperties and canRemoveProperties are both false</li>
     *   <li>MutableAspectDef - if canAddProperties and canRemoveProperties are both true</li>
     *   <li>FullAspectDef - if mutability is mixed</li>
     * </ul>
     *
     * @param conn the database connection to use
     * @param aspectDefName the fully-qualified name of the AspectDef
     * @return the loaded AspectDef
     * @throws SQLException if database operation fails or AspectDef not found
     */
    AspectDef loadAspectDef(@NotNull Connection conn, @NotNull String aspectDefName) throws SQLException;

    /**
     * Deletes a catalog and all its associated data from the database.
     * This includes all hierarchies, entities, aspects, and properties.
     *
     * <p>Implementations should use cascading deletes or explicit deletion of
     * dependent records to ensure complete removal. The operation should be
     * atomic (wrapped in a transaction).
     *
     * <p>Note: This does not delete AspectDefs, as they may be shared across catalogs.
     *
     * @param catalogId the global ID of the catalog to delete
     * @return true if the catalog was deleted, false if it didn't exist
     * @throws SQLException if database operation fails
     */
    boolean deleteCatalog(@NotNull UUID catalogId) throws SQLException;

    /**
     * A shutdown hook that can be wired into frameworks for lifecycle
     * callbacks, e.g., in Spring, \@Bean(destroyMethod = "close").
     */
    default void close() {}
}