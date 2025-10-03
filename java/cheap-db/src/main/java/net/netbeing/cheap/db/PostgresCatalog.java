package net.netbeing.cheap.db;

import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import java.sql.SQLDataException;

import static java.util.Map.entry;

public class PostgresCatalog extends JdbcCatalogBase
{
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
            switch (expectedType) {
                case Integer:
                    if (value instanceof Number n) {
                        return n.longValue();
                    } else if (value instanceof String s) {
                        return Long.valueOf(s);
                    }
                    throw new SQLDataException("Expected Long type but found " + value.getClass());
                case Float:
                    if (value instanceof Number n) {
                        return n.doubleValue();
                    } else if (value instanceof String s) {
                        return Double.valueOf(s);
                    }
                    throw new SQLDataException("Expected Double type but found " + value.getClass());
                case Boolean:
                    if (value instanceof Boolean b) {
                        return value;
                    } else if (value instanceof Number n) {
                        return n.intValue() != 0;
                    } else if (value instanceof String s) {
                        return Boolean.valueOf(s);
                    }
                    throw new SQLDataException("Expected Boolean type but found " + value.getClass());
                default:
                    // Use base implementation for other types
                    return super.convertValue(value, expectedType);
            }
        } catch (SQLDataException e) {
            throw new RuntimeException(e);
        }
    }
}