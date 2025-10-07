package net.netbeing.cheap.db;

import net.netbeing.cheap.model.Catalog;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Interface for saving and loading complete Catalog instances to/from a database.
 * Provides operations to persist the entire Cheap model structure including
 * definitions, entities, aspects, properties, and hierarchies.
 */
public interface CatalogPersistence {

    /**
     * Saves a complete catalog to the database, including all its definitions,
     * entities, aspects, properties, and hierarchies.
     *
     * @param catalog the catalog to save
     * @throws SQLException if database operation fails
     * @throws IllegalArgumentException if catalog is null or has invalid state
     */
    void saveCatalog(@NotNull Catalog catalog) throws SQLException;

    /**
     * Loads a complete catalog from the database by its global ID.
     *
     * @param catalogId the global ID of the catalog to load
     * @return the loaded catalog, or null if not found
     * @throws SQLException if database operation fails
     */
    Catalog loadCatalog(@NotNull UUID catalogId) throws SQLException;

    /**
     * Deletes a catalog and all its associated data from the database.
     * This includes all hierarchies, entities, aspects, and properties.
     *
     * @param catalogId the global ID of the catalog to delete
     * @return true if the catalog was deleted, false if it didn't exist
     * @throws SQLException if database operation fails
     */
    boolean deleteCatalog(@NotNull UUID catalogId) throws SQLException;

    /**
     * Checks if a catalog exists in the database.
     *
     * @param catalogId the global ID of the catalog to check
     * @return true if the catalog exists, false otherwise
     * @throws SQLException if database operation fails
     */
    boolean catalogExists(@NotNull UUID catalogId) throws SQLException;

    /**
     * Executes the main Cheap schema DDL script to create all core tables and indexes.
     * This creates the foundation database structure for the Cheap data model.
     *
     * @param dataSource the data source to execute the DDL against
     * @throws SQLException if database operation fails
     */
    void executeMainSchemaDdl(@NotNull DataSource dataSource) throws SQLException;

    /**
     * Executes the audit schema DDL script to add audit columns and triggers.
     * This should be run after the main schema DDL.
     *
     * @param dataSource the data source to execute the DDL against
     * @throws SQLException if database operation fails
     */
    void executeAuditSchemaDdl(@NotNull DataSource dataSource) throws SQLException;

    /**
     * Executes the drop schema DDL script to remove all Cheap database objects.
     * This completely cleans up the Cheap schema from the database.
     *
     * @param dataSource the data source to execute the DDL against
     * @throws SQLException if database operation fails
     */
    void executeDropSchemaDdl(@NotNull javax.sql.DataSource dataSource) throws SQLException;
}