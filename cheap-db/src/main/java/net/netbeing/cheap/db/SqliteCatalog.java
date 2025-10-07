package net.netbeing.cheap.db;

import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.model.*;

import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

import static java.util.Map.entry;

public class SqliteCatalog extends JdbcCatalogBase
{
    public SqliteCatalog()
    {
        // Default constructor for testing
    }

    public SqliteCatalog(DataSource dataSource)
    {
        super(dataSource);
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

    public static SqliteCatalog loadDb(DataSource dataSource)
    {
        return new SqliteCatalog(dataSource);
    }

    @Override
    protected void loadTables()
    {
        if (dataSource == null) {
            return;
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table';")) {

            while (resultSet.next()) {
                tables.add(resultSet.getString("name"));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load tables from database", e);
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

