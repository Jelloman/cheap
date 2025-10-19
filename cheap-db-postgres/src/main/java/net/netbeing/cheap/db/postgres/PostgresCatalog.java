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

package net.netbeing.cheap.db.postgres;

import net.netbeing.cheap.db.JdbcCatalogBase;
import net.netbeing.cheap.impl.basic.EntityImpl;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.PropertyType;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Catalog implementation that loads PostgreSQL database tables as Cheap AspectMapHierarchies.
 * <p>
 * This class extends {@link JdbcCatalogBase} to provide PostgreSQL-specific database metadata
 * access and type mapping. It introspects PostgreSQL database schemas to automatically create
 * AspectDefs from table definitions, and loads table data as aspects within the catalog.
 * </p>
 * <p>
 * Each table in the PostgreSQL database is represented as an AspectDef with properties
 * corresponding to the table's columns. The table data is loaded into an AspectMapHierarchy
 * where each row becomes an Aspect attached to an Entity.
 * </p>
 * <p>
 * PostgreSQL-specific characteristics handled by this class:
 * </p>
 * <ul>
 *   <li>Schema support (defaults to "public" schema)</li>
 *   <li>Rich type system including UUID, JSON, arrays, geometric types, etc.</li>
 *   <li>Metadata accessed via information_schema.tables</li>
 *   <li>Strong type checking with custom type conversions</li>
 *   <li>Support for serial/bigserial auto-increment types</li>
 * </ul>
 * <p>
 * Type mapping includes comprehensive coverage of PostgreSQL types:
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
 * DataSource dataSource = createPostgreSQLDataSource("jdbc:postgresql://localhost/mydb");
 * PostgresCatalog catalog = new PostgresCatalog(dataSource);
 *
 * // Get list of available tables
 * List<String> tables = catalog.getTables();
 *
 * // Load a specific table as an AspectMapHierarchy
 * AspectMapHierarchy products = catalog.loadTable("products", 1000);
 * }</pre>
 *
 * @see JdbcCatalogBase
 * @see PostgresDao
 */
public class PostgresCatalog extends JdbcCatalogBase
{
    /**
     * Constructs a new PostgreSQL catalog connected to the given data source.
     * <p>
     * This constructor immediately loads the list of available tables from the PostgreSQL
     * database by querying the information_schema.tables view for all BASE TABLEs in the
     * 'public' schema. The table names are cached for subsequent use.
     * </p>
     *
     * @param dataSource the PostgreSQL data source to use for database access
     * @throws NullPointerException if dataSource is null
     */
    public PostgresCatalog(@NotNull DataSource dataSource)
    {
        super(dataSource);
    }
    
    private static final Map<String, PropertyType> POSTGRES_TO_PROPERTY_TYPE = Map.<String, PropertyType>ofEntries(
        // Integer types
        entry("SMALLINT", PropertyType.Integer),
        entry("INTEGER", PropertyType.Integer),
        entry("INT", PropertyType.Integer),
        entry("INT4", PropertyType.Integer),
        entry("BIGINT", PropertyType.Integer),
        entry("INT8", PropertyType.Integer),
        entry("SMALLSERIAL", PropertyType.Integer),
        entry("SERIAL", PropertyType.Integer),
        entry("SERIAL4", PropertyType.Integer),
        entry("BIGSERIAL", PropertyType.Integer),
        entry("SERIAL8", PropertyType.Integer),
        
        // Floating point types
        entry("REAL", PropertyType.Float),
        entry("FLOAT4", PropertyType.Float),
        entry("DOUBLE PRECISION", PropertyType.Float),
        entry("FLOAT8", PropertyType.Float),
        entry("NUMERIC", PropertyType.Float),
        entry("DECIMAL", PropertyType.Float),
        
        // Text types
        entry("TEXT", PropertyType.Text),
        entry("CHARACTER VARYING", PropertyType.String),
        entry("VARCHAR", PropertyType.String),
        entry("CHARACTER", PropertyType.String),
        entry("CHAR", PropertyType.String),
        entry("BPCHAR", PropertyType.String),
        entry("NAME", PropertyType.String),
        
        // Binary types
        entry("BYTEA", PropertyType.BLOB),
        
        // Boolean type
        entry("BOOLEAN", PropertyType.Boolean),
        entry("BOOL", PropertyType.Boolean),
        
        // Date/Time types
        entry("DATE", PropertyType.DateTime),
        entry("TIME", PropertyType.DateTime),
        entry("TIME WITHOUT TIME ZONE", PropertyType.DateTime),
        entry("TIME WITH TIME ZONE", PropertyType.DateTime),
        entry("TIMESTAMP", PropertyType.DateTime),
        entry("TIMESTAMP WITHOUT TIME ZONE", PropertyType.DateTime),
        entry("TIMESTAMP WITH TIME ZONE", PropertyType.DateTime),
        entry("TIMESTAMPTZ", PropertyType.DateTime),
        entry("INTERVAL", PropertyType.String),
        
        // UUID type
        entry("UUID", PropertyType.UUID),
        
        // JSON types
        entry("JSON", PropertyType.Text),
        entry("JSONB", PropertyType.Text),
        
        // Array types - mapped to Text for now
        entry("ARRAY", PropertyType.Text),
        
        // Network address types
        entry("INET", PropertyType.String),
        entry("CIDR", PropertyType.String),
        entry("MACADDR", PropertyType.String),
        entry("MACADDR8", PropertyType.String),
        
        // Bit string types
        entry("BIT", PropertyType.String),
        entry("BIT VARYING", PropertyType.String),
        entry("VARBIT", PropertyType.String),
        
        // Money type
        entry("MONEY", PropertyType.Float),
        
        // Geometric types - mapped to Text for now
        entry("POINT", PropertyType.Text),
        entry("LINE", PropertyType.Text),
        entry("LSEG", PropertyType.Text),
        entry("BOX", PropertyType.Text),
        entry("PATH", PropertyType.Text),
        entry("POLYGON", PropertyType.Text),
        entry("CIRCLE", PropertyType.Text),
        
        // Range types - mapped to Text for now
        entry("INT4RANGE", PropertyType.Text),
        entry("INT8RANGE", PropertyType.Text),
        entry("NUMRANGE", PropertyType.Text),
        entry("TSRANGE", PropertyType.Text),
        entry("TSTZRANGE", PropertyType.Text),
        entry("DATERANGE", PropertyType.Text),
        
        // Object identifier types
        entry("OID", PropertyType.Integer),
        entry("REGPROC", PropertyType.String),
        entry("REGPROCEDURE", PropertyType.String),
        entry("REGOPER", PropertyType.String),
        entry("REGOPERATOR", PropertyType.String),
        entry("REGCLASS", PropertyType.String),
        entry("REGTYPE", PropertyType.String),
        entry("REGCONFIG", PropertyType.String),
        entry("REGDICTIONARY", PropertyType.String),
        
        // Text search types
        entry("TSVECTOR", PropertyType.Text),
        entry("TSQUERY", PropertyType.Text),
        
        // XML type
        entry("XML", PropertyType.Text)
    );

    @Override
    protected void loadTables()
    {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT table_name FROM information_schema.tables " +
                 "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'");
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                tables.add(resultSet.getString("table_name"));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load tables from PostgreSQL database", e);
        }
    }

    @Override
    protected String getSchemaName()
    {
        return "public";
    }

    @Override
    protected Entity createEntity()
    {
        return new EntityImpl();
    }

    @Override
    protected PropertyType mapDbTypeToPropertyType(String dbTypeName)
    {
        PropertyType propertyType = POSTGRES_TO_PROPERTY_TYPE.get(dbTypeName);
        if (propertyType != null) {
            return propertyType;
        }

        // Handle parameterized types like VARCHAR(50) by extracting base type
        String baseType = extractBaseType(dbTypeName);
        propertyType = POSTGRES_TO_PROPERTY_TYPE.get(baseType);

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

        // Handle PostgreSQL-specific type conversions, fall back to base implementation
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