package net.netbeing.cheap.db;

import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.model.*;

import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

import static java.util.Map.entry;

public class SqliteCatalog extends CatalogImpl
{
    protected final List<String> tables = new LinkedList<>();
    protected final Map<String, AspectDef> tableAspects = new LinkedHashMap<>();
    protected DataSource dataSource;
    
    public SqliteCatalog() {
        // Default constructor for testing
    }
    
    public SqliteCatalog(DataSource dataSource) {
        this.dataSource = dataSource;
        loadTables();
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
    
    private void loadTables() {
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
            
            try (ResultSet columns = metaData.getColumns(null, null, tableName, null)) {
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String sqliteTypeName = columns.getString("TYPE_NAME").toUpperCase();
                    boolean isNullable = columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                    
                    PropertyType propertyType = mapSqliteTypeToPropertyType(sqliteTypeName);
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
    
    private PropertyType mapSqliteTypeToPropertyType(String sqliteType)
    {
        PropertyType propertyType = SQLITE_TO_PROPERTY_TYPE.get(sqliteType);
        if (propertyType != null) {
            return propertyType;
        }
        
        // Handle parameterized types like VARCHAR(50) by extracting base type
        String baseType = sqliteType.split("\\(")[0].trim();
        propertyType = SQLITE_TO_PROPERTY_TYPE.get(baseType);
        
        // Default to Text type if no mapping found
        return propertyType != null ? propertyType : PropertyType.Text;
    }

    public AspectMapHierarchy loadTable(String tableName, int maxRows)
    {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource not set. Use constructor with DataSource to initialize the catalog.");
        }
        
        AspectDef aspectDef = getTableDef(tableName);
        HierarchyDef hierarchyDef = new HierarchyDefImpl("sqlite:table:" + tableName, HierarchyType.ASPECT_MAP);
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
                LocalEntity entity = new LocalEntityOneCatalogImpl(this);
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
    
    private Object convertValue(Object value, PropertyType expectedType)
    {
        if (value == null) {
            return null;
        }
        
        // Convert based on expected PropertyType
        switch (expectedType) {
            case Integer:
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                break;
            case Float:
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                break;
            case Boolean:
                if (value instanceof Number) {
                    return ((Number) value).intValue() != 0;
                }
                break;
            case String:
            case Text:
            case BigInteger:
            case DateTime:
            case URI:
            case UUID:
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
}
