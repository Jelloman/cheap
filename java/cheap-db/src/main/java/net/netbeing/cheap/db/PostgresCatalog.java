package net.netbeing.cheap.db;

import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import java.sql.SQLDataException;
import java.util.Date;

import static java.util.Map.entry;

public class PostgresCatalog extends CatalogImpl
{
    protected final List<String> tables = new LinkedList<>();
    protected final Map<String, AspectDef> tableAspects = new LinkedHashMap<>();
    protected DataSource dataSource;
    
    public PostgresCatalog(@NotNull DataSource dataSource) {
        this.dataSource = dataSource;
        loadTables();
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

    private void loadTables() {
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

    public AspectDef getTableDef(String tableName)
    {
        AspectDef def = tableAspects.get(tableName);
        if (def != null) {
            return def;
        }
        return loadTableDef(tableName);
    }

    public AspectDef loadTableDef(String tableName)
    {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource not set. Use constructor with DataSource to initialize the catalog.");
        }
        
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl(tableName);
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            try (ResultSet columns = metaData.getColumns(null, "public", tableName, null)) {
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String postgresTypeName = columns.getString("TYPE_NAME").toUpperCase();
                    boolean isNullable = columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                    
                    PropertyType propertyType = mapPostgresTypeToPropertyType(postgresTypeName);
                    PropertyDefImpl propertyDef = new PropertyDefImpl(columnName, propertyType, true, true, isNullable, true, false);
                    
                    aspectDef.add(propertyDef);
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load table definition for table: " + tableName, e);
        }

        tableAspects.put(tableName, aspectDef);
        
        return aspectDef;
    }
    
    private PropertyType mapPostgresTypeToPropertyType(String postgresType)
    {
        PropertyType propertyType = POSTGRES_TO_PROPERTY_TYPE.get(postgresType);
        if (propertyType != null) {
            return propertyType;
        }
        
        // Handle parameterized types like VARCHAR(50) by extracting base type
        String baseType = postgresType.split("\\(")[0].trim();
        propertyType = POSTGRES_TO_PROPERTY_TYPE.get(baseType);
        
        // Handle array types like INTEGER[]
        if (baseType.endsWith("[]")) {
            return PropertyType.Text; // Arrays mapped to Text for now
        }
        
        // Default to Text type if no mapping found
        return propertyType != null ? propertyType : PropertyType.Text;
    }

    public AspectMapHierarchy loadTable(String tableName, int maxRows)
    {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource not set. Use constructor with DataSource to initialize the catalog.");
        }
        
        AspectDef aspectDef = getTableDef(tableName);
        HierarchyDef hierarchyDef = new HierarchyDefImpl("postgres:table:" + tableName, HierarchyType.ASPECT_MAP);
        AspectMapHierarchyImpl hierarchy = new AspectMapHierarchyImpl(this, hierarchyDef, aspectDef);

        String query = "SELECT * FROM " + tableName;
        if (maxRows >= 0) {
            query += " LIMIT " + maxRows;
        }
        
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (resultSet.next()) {
                Entity entity = new EntityImpl();
                AspectPropertyMapImpl aspect = new AspectPropertyMapImpl(entity, aspectDef);
                
                // Create properties for each column
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = resultSet.getObject(i);
                    
                    PropertyDef propDef = aspectDef.propertyDef(columnName);
                    if (propDef != null) {
                        PropertyImpl property = new PropertyImpl(propDef, convertValue(value, propDef.type()));
                        aspect.unsafeAdd(property);
                    }
                }
                
                hierarchy.put(entity, aspect);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load table data for table: " + tableName, e);
        }

        return hierarchy;
    }
    
    private Object convertValue(Object value, PropertyType expectedType) throws SQLDataException
    {
        if (value == null) {
            return null;
        }
        
        // Convert based on expected PropertyType
        switch (expectedType) {
            case Integer:
                return switch (value) {
                    case Number n -> n.longValue();
                    case String s -> Long.valueOf(s);
                    default -> throw new SQLDataException("Expected Long type but found " + value.getClass());
                };
            case Float:
                return switch (value) {
                    case Number n -> n.doubleValue();
                    case String s -> Double.valueOf(s);
                    default -> throw new SQLDataException("Expected Double type but found " + value.getClass());
                };
            case Boolean:
                return switch (value) {
                    case Boolean b -> value;
                    case Number n -> n.intValue() != 0;
                    case String s -> Boolean.valueOf(s);
                    default -> throw new SQLDataException("Expected Boolean type but found " + value.getClass());
                };
            case UUID:
                if (value instanceof UUID) {
                    return value;
                }
                break;
            case DateTime:
                if (value instanceof java.sql.Date) {
                    return ((java.sql.Date)value).toLocalDate().toString();
                } else if (value instanceof Date) {
                    return ((Date)value).toInstant().toString();
                }
                break;
            case String:
            case Text:
            case BigInteger:
            case URI:
                return value.toString();
            case BLOB:
            case CLOB:
                // For now, return raw value - could be enhanced with streaming support
                return value;
        }
        
        return value;
    }

    public List<String> getTables()
    {
        return new LinkedList<>(tables);
    }
    
    public DataSource getDataSource()
    {
        return dataSource;
    }
}