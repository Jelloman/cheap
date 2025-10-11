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

package net.netbeing.cheap.db.mariadb;

import net.netbeing.cheap.db.JdbcCatalogBase;
import net.netbeing.cheap.db.sqlite.SqliteCatalog;
import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import java.sql.SQLDataException;

import static java.util.Map.entry;

/**
 * Catalog implementation that loads MariaDB database tables as Cheap AspectMapHierarchies.
 * <p>
 * This class extends {@link JdbcCatalogBase} to provide MariaDB-specific database metadata
 * access and type mapping. It introspects MariaDB database schemas to automatically create
 * AspectDefs from table definitions, and loads table data as aspects within the catalog.
 * </p>
 * <p>
 * Each table in the MariaDB database is represented as an AspectDef with properties
 * corresponding to the table's columns. The table data is loaded into an AspectMapHierarchy
 * where each row becomes an Aspect attached to an Entity.
 * </p>
 * <p>
 * MariaDB-specific characteristics handled by this class:
 * </p>
 * <ul>
 *   <li>Schema support (defaults to "public" schema)</li>
 *   <li>Rich type system including UUID, JSON, arrays, geometric types, etc.</li>
 *   <li>Metadata accessed via information_schema.tables</li>
 *   <li>Strong type checking with custom type conversions</li>
 *   <li>Support for serial/bigserial auto-increment types</li>
 * </ul>
 * <p>
 * Type mapping includes comprehensive coverage of MariaDB types:
 * </p>
 * <ul>
 *   <li>Numeric: INTEGER, BIGINT, REAL, DOUBLE PRECISION, NUMERIC, DECIMAL, SERIAL</li>
 *   <li>Text: TEXT, VARCHAR, CHAR, JSON, JSONB, XML</li>
 *   <li>Binary: BYTEA</li>
 *   <li>Date/Time: DATE, TIME, TIMESTAMP (with/without time zone), INTERVAL</li>
 *   <li>Boolean: BOOLEAN, BOOL</li>
 *   <li>UUID: Native UUID support</li>
 *   <li>Network: INET, CIDR, MACADDR</li>
 *   <li>Geometric: POINT, LINE, BOX, CIRCLE, etc. (mapped to Text)</li>
 *   <li>Arrays: All array types (mapped to Text)</li>
 * </ul>
 * <p>
 * Usage example:
 * </p>
 * <pre>{@code
 * DataSource dataSource = createMariaDBDataSource("jdbc:mariadb://localhost/mydb");
 * MariaDbCatalog catalog = new MariaDbCatalog(dataSource);
 *
 * // Get list of available tables
 * List<String> tables = catalog.getTables();
 *
 * // Load a specific table as an AspectMapHierarchy
 * AspectMapHierarchy products = catalog.loadTable("products", 1000);
 * }</pre>
 *
 * @see JdbcCatalogBase
 * @see SqliteCatalog
 * @see MariaDbDao
 */
public class MariaDbCatalog extends JdbcCatalogBase
{
    /**
     * Constructs a new MariaDB catalog connected to the given data source.
     * <p>
     * This constructor immediately loads the list of available tables from the MariaDB
     * database by querying the information_schema.tables view for all BASE TABLEs in the
     * 'public' schema. The table names are cached for subsequent use.
     * </p>
     *
     * @param dataSource the MariaDB data source to use for database access
     * @throws NullPointerException if dataSource is null
     */
    public MariaDbCatalog(@NotNull DataSource dataSource)
    {
        super(dataSource);
    }

    private static final Map<String, PropertyType> MARIADB_TO_PROPERTY_TYPE = Map.<String, PropertyType>ofEntries(
        // Integer types
        entry("TINYINT", PropertyType.Integer),
        entry("SMALLINT", PropertyType.Integer),
        entry("MEDIUMINT", PropertyType.Integer),
        entry("INT", PropertyType.Integer),
        entry("INTEGER", PropertyType.Integer),
        entry("BIGINT", PropertyType.Integer),

        // Floating point types
        entry("FLOAT", PropertyType.Float),
        entry("DOUBLE", PropertyType.Float),
        entry("DECIMAL", PropertyType.Float),
        entry("NUMERIC", PropertyType.Float),
        entry("REAL", PropertyType.Float),

        // Text types
        entry("CHAR", PropertyType.String),
        entry("VARCHAR", PropertyType.String),
        entry("TINYTEXT", PropertyType.Text),
        entry("TEXT", PropertyType.Text),
        entry("MEDIUMTEXT", PropertyType.Text),
        entry("LONGTEXT", PropertyType.Text),

        // Binary types
        entry("BINARY", PropertyType.BLOB),
        entry("VARBINARY", PropertyType.BLOB),
        entry("TINYBLOB", PropertyType.BLOB),
        entry("BLOB", PropertyType.BLOB),
        entry("MEDIUMBLOB", PropertyType.BLOB),
        entry("LONGBLOB", PropertyType.BLOB),

        // Boolean type (stored as TINYINT(1) in MariaDB)
        entry("BOOLEAN", PropertyType.Boolean),
        entry("BOOL", PropertyType.Boolean),
        entry("TINYINT(1)", PropertyType.Boolean),

        // Date/Time types
        entry("DATE", PropertyType.DateTime),
        entry("TIME", PropertyType.DateTime),
        entry("DATETIME", PropertyType.DateTime),
        entry("TIMESTAMP", PropertyType.DateTime),
        entry("YEAR", PropertyType.Integer),

        // JSON type
        entry("JSON", PropertyType.Text),

        // Bit type
        entry("BIT", PropertyType.String),

        // Enum and Set types
        entry("ENUM", PropertyType.String),
        entry("SET", PropertyType.String)
    );

    @Override
    protected void loadTables()
    {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT table_name FROM information_schema.tables " +
                 "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'");
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                tables.add(resultSet.getString("table_name"));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load tables from MariaDB database", e);
        }
    }

    @Override
    protected String getSchemaName()
    {
        // In MariaDB, the schema is the database name
        try (Connection connection = dataSource.getConnection()) {
            return connection.getCatalog();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get schema name from MariaDB database", e);
        }
    }

    @Override
    protected Entity createEntity()
    {
        return new EntityImpl();
    }

    @Override
    protected PropertyType mapDbTypeToPropertyType(String dbTypeName)
    {
        PropertyType propertyType = MARIADB_TO_PROPERTY_TYPE.get(dbTypeName);
        if (propertyType != null) {
            return propertyType;
        }

        // Handle parameterized types like VARCHAR(50) by extracting base type
        String baseType = extractBaseType(dbTypeName);
        propertyType = MARIADB_TO_PROPERTY_TYPE.get(baseType);

        // Handle array types like INTEGER[]
        if (baseType.endsWith("[]")) {
            return PropertyType.Text; // Arrays mapped to Text for now
        }

        // Default to Text type if no mapping found
        return propertyType != null ? propertyType : PropertyType.Text;
    }

    @Override
    protected Object convertValue(Object value, PropertyType expectedType)
    {
        if (value == null) {
            return null;
        }

        // Handle MariaDB-specific type conversions, fall back to base implementation
        try {
            return switch (expectedType) {
                case Integer -> {
                    if (value instanceof Number n) {
                        yield n.longValue();
                    } else if (value instanceof String s) {
                        yield Long.valueOf(s);
                    }
                    throw new SQLDataException("Expected Long type but found " + value.getClass());
                }
                case Float -> {
                    if (value instanceof Number n) {
                        yield n.doubleValue();
                    } else if (value instanceof String s) {
                        yield Double.valueOf(s);
                    }
                    throw new SQLDataException("Expected Double type but found " + value.getClass());
                }
                case Boolean -> {
                    if (value instanceof Boolean b) {
                        yield value;
                    } else if (value instanceof Number n) {
                        yield n.intValue() != 0;
                    } else if (value instanceof String s) {
                        yield Boolean.valueOf(s);
                    }
                    throw new SQLDataException("Expected Boolean type but found " + value.getClass());
                }
                default ->
                    // Use base implementation for other types
                    super.convertValue(value, expectedType);
            };
        } catch (SQLDataException e) {
            throw new RuntimeException(e);
        }
    }
}
