package net.netbeing.cheap.integrationtests.util;

import ch.vorburger.exec.ManagedProcessException;
import net.netbeing.cheap.db.mariadb.MariaDbAdapter;
import net.netbeing.cheap.db.mariadb.MariaDbCheapSchema;
import net.netbeing.cheap.db.mariadb.MariaDbDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import org.mariadb.jdbc.MariaDbDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.TimeZone;

/**
 * Helper class for managing MariaDB test databases in integration tests.
 * Creates a separate database within the shared MariaDB4j instance for each test class.
 */
public class MariaDbIntegrationTestDb
{
    public final DataSource dataSource;
    public final MariaDbDao mariaDbDao;
    public final CheapFactory factory;
    public final String dbName;
    public final MariaDbAdapter adapter;
    public final boolean useForeignKeys;

    public MariaDbIntegrationTestDb(String dbName, boolean useForeignKeys) throws ManagedProcessException, SQLException
    {
        this.dbName = dbName;
        this.useForeignKeys = useForeignKeys;

        // Create database in shared MariaDB4j instance
        MariaDbRunnerExtension.getMariaDB().createDB(dbName);

        // Create data source
        MariaDbDataSource ds = new MariaDbDataSource();
        String url = MariaDbRunnerExtension.getDbConfig().getURL(dbName);
        url = url + (url.indexOf('?') >= 0 ? "&" : "?") + "allowMultiQueries=true";
        ds.setUrl(url);
        ds.setUser("root");
        ds.setPassword("");
        this.dataSource = ds;

        // Initialize factory and DAO
        factory = new CheapFactory();
        adapter = new MariaDbAdapter(dataSource, factory);
        adapter.setTimeZone(TimeZone.getTimeZone("UTC"));
        mariaDbDao = new MariaDbDao(adapter);
    }

    public void initializeCheapSchema() throws SQLException
    {
        // Use MariaDbCheapSchema to execute DDL
        MariaDbCheapSchema schema = new MariaDbCheapSchema();
        schema.executeMainSchemaDdl(dataSource);
        if (useForeignKeys)
        {
            schema.executeForeignKeysDdl(dataSource);
        }
        schema.executeAuditSchemaDdl(dataSource);
    }

    public void truncateAllTables() throws SQLException
    {
        // Use MariaDbCheapSchema to truncate all tables
        MariaDbCheapSchema schema = new MariaDbCheapSchema();
        schema.executeTruncateSchemaDdl(dataSource);
    }
}
