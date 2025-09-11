package net.netbeing.cheap.db;

import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.model.*;

import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

import static java.util.Map.entry;

public class PostgresCatalog extends CatalogImpl
{
    protected final List<String> tables = new LinkedList<>();
    protected final Map<String, AspectDef> tableAspects = new HashMap<>();
    protected DataSource dataSource;
    
    public PostgresCatalog() {
        // Default constructor for testing
    }
    
    public PostgresCatalog(DataSource dataSource) {
        this.dataSource = dataSource;
        loadTables();
    }
    
    private static final Map<String, PropertyType> POSTGRES_TO_PROPERTY_TYPE = Map.ofEntries(
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

    public static PostgresCatalog connect(String host, int port, String database, String username, String password)
    {
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        DataSource dataSource = createDataSource(jdbcUrl, username, password);
        return new PostgresCatalog(dataSource);
    }
    
    public static PostgresCatalog connect(String jdbcUrl, String username, String password)
    {
        DataSource dataSource = createDataSource(jdbcUrl, username, password);
        return new PostgresCatalog(dataSource);
    }
    
    private static DataSource createDataSource(String jdbcUrl, String username, String password) {
        return new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return DriverManager.getConnection(jdbcUrl, username, password);
            }
            
            @Override
            public Connection getConnection(String user, String pass) throws SQLException {
                return DriverManager.getConnection(jdbcUrl, user, pass);
            }
            
            // Other DataSource methods with default implementations
            @Override public java.io.PrintWriter getLogWriter() throws SQLException { return null; }
            @Override public void setLogWriter(java.io.PrintWriter out) throws SQLException {}
            @Override public void setLoginTimeout(int seconds) throws SQLException {}
            @Override public int getLoginTimeout() throws SQLException { return 0; }
            @Override public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException {
                throw new java.sql.SQLFeatureNotSupportedException();
            }
            @Override public <T> T unwrap(Class<T> iface) throws SQLException {
                throw new SQLException("Cannot unwrap to " + iface.getName());
            }
            @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
        };
    }
    
    private void loadTables() {
        if (dataSource == null) {
            return;
        }
        
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
        AspectMapHierarchyImpl hierarchy = new AspectMapHierarchyImpl(hierarchyDef, aspectDef);

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
                Entity entity = new BasicEntityImpl();
                AspectPropertyMapImpl aspect = new AspectPropertyMapImpl(this, entity, aspectDef);
                
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

        this.hierarchies().add(hierarchy);

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
                if (value instanceof Boolean) {
                    return value;
                }
                if (value instanceof Number) {
                    return ((Number) value).intValue() != 0;
                }
                break;
            case UUID:
                if (value instanceof java.util.UUID) {
                    return value.toString();
                }
                break;
            case DateTime:
                if (value instanceof Timestamp || value instanceof java.sql.Date || value instanceof Time) {
                    return value.toString();
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