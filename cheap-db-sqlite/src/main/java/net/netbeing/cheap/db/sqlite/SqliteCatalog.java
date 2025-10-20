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

package net.netbeing.cheap.db.sqlite;

import net.netbeing.cheap.db.JdbcCatalogBase;
import net.netbeing.cheap.impl.basic.LocalEntityOneCatalogImpl;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.PropertyType;
import net.netbeing.cheap.util.CheapException;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Catalog implementation that loads SQLite database tables as Cheap AspectMapHierarchies.
 * <p>
 * This class extends {@link JdbcCatalogBase} to provide SQLite-specific database metadata
 * access and type mapping. It introspects SQLite database schemas to automatically create
 * AspectDefs from table definitions, and loads table data as aspects within the catalog.
 * </p>
 * <p>
 * Each table in the SQLite database is represented as an AspectDef with properties
 * corresponding to the table's columns. The table data is loaded into an AspectMapHierarchy
 * where each row becomes an Aspect attached to an Entity.
 * </p>
 * <p>
 * SQLite-specific characteristics handled by this class:
 * </p>
 * <ul>
 *   <li>No schema names (SQLite doesn't use schemas)</li>
 *   <li>Type affinity system where column types map to property types</li>
 *   <li>Metadata accessed via sqlite_master system table</li>
 *   <li>Dynamic typing with TEXT, INTEGER, REAL, BLOB, and NUMERIC affinities</li>
 * </ul>
 * <p>
 * Usage example:
 * </p>
 * <pre>{@code
 * DataSource dataSource = createSQLiteDataSource("mydata.db");
 * SqliteCatalog catalog = SqliteCatalog.loadDb(dataSource);
 *
 * // Get list of available tables
 * List<String> tables = catalog.getTables();
 *
 * // Load a specific table as an AspectMapHierarchy
 * AspectMapHierarchy customers = catalog.loadTable("customers", 1000);
 * }</pre>
 *
 * @see JdbcCatalogBase
 */
public class SqliteCatalog extends JdbcCatalogBase
{
    /**
     * Constructs a new SQLite catalog connected to the given data source.
     * <p>
     * This constructor immediately loads the list of available tables from the SQLite database
     * by querying the sqlite_master system table. The table names are cached for subsequent use.
     * </p>
     *
     * @param adapter the SQLite database adapter to use for database access
     */
    public SqliteCatalog(@NotNull SqliteAdapter adapter)
    {
        super(adapter);
    }
    
    private static final Map<String, PropertyType> SQLITE_TO_PROPERTY_TYPE = Map.ofEntries(
        // Integer types
        entry("INTEGER", PropertyType.Integer),
        entry("INT", PropertyType.Integer),
        entry("TINYINT", PropertyType.Integer),
        entry("SMALLINT", PropertyType.Integer),
        entry("MEDIUMINT", PropertyType.Integer),
        entry("BIGINT", PropertyType.Integer),
        entry("INT2", PropertyType.Integer),
        entry("INT8", PropertyType.Integer),
        
        // Floating point types
        entry("REAL", PropertyType.Float),
        entry("DOUBLE", PropertyType.Float),
        entry("DOUBLE PRECISION", PropertyType.Float),
        entry("FLOAT", PropertyType.Float),
        entry("NUMERIC", PropertyType.Float),
        entry("DECIMAL", PropertyType.Float),
        
        // Text types
        entry("TEXT", PropertyType.Text),
        entry("CHARACTER", PropertyType.String),
        entry("VARCHAR", PropertyType.String),
        entry("VARYING CHARACTER", PropertyType.String),
        entry("NCHAR", PropertyType.String),
        entry("NATIVE CHARACTER", PropertyType.String),
        entry("NVARCHAR", PropertyType.String),
        entry("CLOB", PropertyType.CLOB),
        
        // Binary types
        entry("BLOB", PropertyType.BLOB),
        
        // Boolean type (SQLite stores as numeric)
        entry("BOOLEAN", PropertyType.Boolean)
    );

    @Override
    protected void loadTables()
    {
        try (Connection connection = adapter.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table';")) {

            while (resultSet.next()) {
                tables.add(resultSet.getString("name"));
            }

        } catch (SQLException e) {
            throw new CheapException("Failed to load tables from database", e);
        }
    }

    @Override
    protected String getSchemaName()
    {
        return null; // SQLite doesn't use schemas
    }

    @Override
    protected Entity createEntity()
    {
        return new LocalEntityOneCatalogImpl(this);
    }

    /**
     * Maps SQLite type names to Cheap PropertyTypes.
     * Handles SQLite's type affinity system where column types are flexible.
     * Supports parameterized types like VARCHAR(50) by extracting the base type.
     *
     * @param dbTypeName the SQLite type name (e.g., "INTEGER", "VARCHAR", "TEXT")
     * @return the corresponding PropertyType, defaults to Text if unknown
     */
    @Override
    protected PropertyType mapDbTypeToPropertyType(String dbTypeName)
    {
        PropertyType propertyType = SQLITE_TO_PROPERTY_TYPE.get(dbTypeName);
        if (propertyType != null) {
            return propertyType;
        }

        // Handle parameterized types like VARCHAR(50) by extracting base type
        String baseType = extractBaseType(dbTypeName);
        propertyType = SQLITE_TO_PROPERTY_TYPE.get(baseType);

        // Default to Text type if no mapping found
        return propertyType != null ? propertyType : PropertyType.Text;
    }

    // convertValue() inherited from JdbcCatalogBase handles SQLite conversions correctly
}

