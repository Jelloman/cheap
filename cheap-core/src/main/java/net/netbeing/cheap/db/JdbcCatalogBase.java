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

import net.netbeing.cheap.impl.basic.CatalogImpl;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.AspectMapHierarchy;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.MutableAspectDef;
import net.netbeing.cheap.model.Property;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import net.netbeing.cheap.util.CheapException;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Base class for Catalog implementations that load database tables via JDBC.
 * Provides common functionality for mapping database schemas to Cheap AspectDefs
 * and loading table data into AspectMapHierarchies.
 */
public abstract class JdbcCatalogBase extends CatalogImpl
{
    /**
     * The database adapter used by this catalog adapter.
     */
    protected final CheapJdbcAdapter adapter;

    /** List of table names available in the database. Populated by {@link #loadTables()}. */
    protected final List<String> tables = new LinkedList<>();

    /** Cache of AspectDef objects created for each table, keyed by table name. */
    protected final Map<String, AspectDef> tableAspects = new LinkedHashMap<>();

    /**
     * Constructs a new catalog connected to the given data source.
     * Immediately loads the list of available tables from the database.
     *
     * @param adapter the database adapter to use for database access
     */
    protected JdbcCatalogBase(@NotNull CheapJdbcAdapter adapter)
    {
        this.adapter = adapter;
        loadTables();
    }

    public CheapJdbcAdapter getAdapter()
    {
        return adapter;
    }

    /**
     * Loads the list of table names from the database.
     * Subclasses must implement the database-specific query.
     */
    protected abstract void loadTables();

    /**
     * Maps a database-specific type name to a PropertyType.
     *
     * @param dbTypeName the database type name
     * @return the corresponding PropertyType
     */
    protected abstract PropertyType mapDbTypeToPropertyType(String dbTypeName);

    /**
     * Creates an Entity appropriate for this catalog implementation.
     *
     * @return a new Entity instance
     */
    protected abstract Entity createEntity();

    /**
     * Gets the schema name to use for metadata queries, or null if not applicable.
     *
     * @return the schema name, or null
     */
    protected abstract String getSchemaName();

    /**
     * Gets the AspectDef for a database table, loading it from the database if necessary.
     * This method caches AspectDefs to avoid repeated introspection of the same table.
     *
     * @param tableName the name of the database table
     * @return the AspectDef representing the table's schema
     */
    public AspectDef getTableDef(String tableName)
    {
        AspectDef def = tableAspects.get(tableName);
        if (def != null) {
            return def;
        }
        return loadTableDef(tableName);
    }

    /**
     * Loads a table's schema from the database and creates an AspectDef.
     * Uses JDBC metadata to introspect the table's column names, types, and nullability.
     * The resulting AspectDef is cached for future use.
     *
     * @param tableName the name of the database table to introspect
     * @return a MutableAspectDef representing the table structure
     * @throws IllegalStateException if adapter is not set
     * @throws RuntimeException if database introspection fails
     */
    public AspectDef loadTableDef(@NotNull String tableName)
    {
        MutableAspectDef aspectDef = adapter.getFactory().createMutableAspectDef(tableName);

        try (Connection connection = adapter.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            try (ResultSet columns = metaData.getColumns(null, getSchemaName(), tableName, null)) {
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String dbTypeName = columns.getString("TYPE_NAME").toUpperCase();
                    boolean isNullable = columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;

                    PropertyType propertyType = mapDbTypeToPropertyType(dbTypeName);
                    PropertyDef propertyDef = adapter.factory.createPropertyDefBuilder()
                        .setName(columnName)
                        .setType(propertyType)
                        .setIsReadable(true)
                        .setIsWritable(true)
                        .setIsNullable(isNullable)
                        .setIsRemovable(true)
                        .setIsMultivalued(false).build();

                    aspectDef.add(propertyDef);
                }
            }

        } catch (SQLException e) {
            throw new CheapException("Failed to load table definition for table: " + tableName, e);
        }

        tableAspects.put(tableName, aspectDef);

        return aspectDef;
    }

    /**
     * Loads data from a database table into an AspectMapHierarchy.
     * <p>
     * Each row in the table becomes an Aspect attached to an Entity. The table's columns
     * are mapped to properties according to the AspectDef created from the table schema.
     * </p>
     * <p>
     * This method performs a SELECT * query with an optional row limit. For large tables,
     * specifying maxRows helps prevent memory issues.
     * </p>
     *
     * @param tableName the name of the database table to load
     * @param maxRows maximum number of rows to load, or -1 for unlimited
     * @return an AspectMapHierarchy containing the table data
     * @throws IllegalStateException if adapter is not set
     * @throws RuntimeException if the query fails
     */
    public AspectMapHierarchy loadTable(@NotNull String tableName, int maxRows)
    {
        if (adapter == null) {
            throw new IllegalStateException("DataSource not set. Use constructor with DataSource to initialize the catalog.");
        }

        AspectDef aspectDef = getTableDef(tableName);
        AspectMapHierarchy hierarchy = createAspectMap(aspectDef, 0L);

        String query = "SELECT * FROM " + tableName;
        if (maxRows >= 0) {
            query += " LIMIT " + maxRows;
        }

        try (Connection connection = adapter.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Entity entity = createEntity();
                Aspect aspect = adapter.getFactory().createPropertyMapAspect(entity, aspectDef);

                // Create properties for each column
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = resultSet.getObject(i);

                    PropertyDef propDef = aspectDef.propertyDef(columnName);
                    if (propDef != null) {
                        Property property = adapter.getFactory().createProperty(propDef, convertValue(value, propDef));
                        aspect.unsafeAdd(property);
                    }
                }

                hierarchy.put(entity, aspect);
            }

        } catch (SQLException e) {
            throw new CheapException("Failed to load table data for table: " + tableName, e);
        }

        return hierarchy;
    }

    /**
     * Converts a database value to the appropriate Java type for the given PropertyType.
     * This default implementation delegates all behavior to the PropertyValueAdapter of
     * this object.
     *
     * @param value the database value
     * @param expectedProperty the expected PropertyDef
     * @return the converted value
     */
    protected Object convertValue(Object value, PropertyDef expectedProperty)
    {
        return adapter.getValueAdapter().coerce(expectedProperty, value);
    }

    /**
     * Handles parameterized type names (e.g., VARCHAR(50)) by extracting the base type.
     *
     * @param dbTypeName the full database type name
     * @return the base type name
     */
    protected String extractBaseType(String dbTypeName)
    {
        return dbTypeName.split("\\(")[0].trim();
    }

    /**
     * Returns a list of all table names available in the database.
     * The list is populated when the catalog is constructed.
     *
     * @return a new list containing all table names
     */
    public List<String> getTables()
    {
        return new LinkedList<>(tables);
    }
}
