package net.netbeing.cheap.integrationtests.config;

import jakarta.annotation.PreDestroy;
import net.netbeing.cheap.db.AspectTableMapping;
import net.netbeing.cheap.db.CheapDao;
import net.netbeing.cheap.db.sqlite.SqliteAdapter;
import net.netbeing.cheap.db.sqlite.SqliteCheapSchema;
import net.netbeing.cheap.db.sqlite.SqliteDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SQLite server configuration for integration tests.
 * Loads cheap-rest with ONLY cheap-db-sqlite, configures SQLite database,
 * registers AspectTableMapping for "order_item" table on startup, and runs on port 8082.
 */
@TestConfiguration
@ComponentScan(basePackages = {"net.netbeing.cheap.integrationtests.util"})
@TestPropertySource(properties = {
    "server.port=8082",
    "cheap.database.type=sqlite"
})
@SuppressWarnings("java:S2696")
public class SqliteServerTestConfig
{
    private static final Logger logger = LoggerFactory.getLogger(SqliteServerTestConfig.class);

    // Static singleton to share SQLite database across all Spring contexts
    private static Path tempDbPath = null;
    private static DataSource dataSource = null;
    private static final Object LOCK = new Object();
    private static int referenceCount = 0;

    /**
     * Provides CheapFactory bean for creating Cheap objects.
     */
    @Bean
    public CheapFactory cheapFactory()
    {
        return new CheapFactory();
    }

    /**
     * Provides a SQLite DataSource for testing.
     * Uses static singleton to ensure only one SQLite database across all test contexts.
     */
    @Bean
    @Primary
    public DataSource sqliteDataSource() throws IOException, SQLException
    {
        synchronized (LOCK) {
            if (dataSource == null) {
                logger.info("Setting up SQLite database for integration tests");

                // Create temporary database file
                tempDbPath = Files.createTempFile("cheap-sqlite-integration-test-", ".db");
                logger.info("Created temporary SQLite database at: {}", tempDbPath.toAbsolutePath());

                // Set up data source
                SQLiteDataSource sqliteDataSource = new SQLiteDataSource();
                sqliteDataSource.setUrl("jdbc:sqlite:" + tempDbPath.toAbsolutePath());

                // Initialize schema
                SqliteCheapSchema schema = new SqliteCheapSchema();
                schema.executeDropSchemaDdl(sqliteDataSource);
                schema.executeMainSchemaDdl(sqliteDataSource);
                schema.executeAuditSchemaDdl(sqliteDataSource);

                dataSource = sqliteDataSource;
                logger.info("SQLite database initialized with schema");
            } else {
                logger.info("Reusing existing SQLite database at: {}", tempDbPath.toAbsolutePath());
            }

            referenceCount++;
            logger.info("SQLite reference count incremented to {}", referenceCount);

            return dataSource;
        }
    }

    @PreDestroy
    public void preDestroy() throws IOException
    {
        synchronized (LOCK) {
            referenceCount--;
            logger.info("SQLite reference count decremented to {}", referenceCount);

            if (referenceCount == 0 && tempDbPath != null) {
                logger.info("Deleting shared SQLite database (last context)");
                Files.deleteIfExists(tempDbPath);
                tempDbPath = null;
                dataSource = null;
            } else if (referenceCount > 0) {
                logger.info("Keeping shared SQLite database ({} contexts still active)", referenceCount);
            }
        }
    }

    /**
     * Provides SqliteDao bean for the server.
     */
    @Bean
    @Primary
    public CheapDao cheapDao(DataSource dataSource, CheapFactory factory)
    {
        logger.info("Creating SqliteDao bean for integration tests");
        SqliteAdapter adapter = new SqliteAdapter(dataSource, factory);
        return new SqliteDao(adapter);
    }

    /**
     * Registers the "order_item" AspectTableMapping on application startup.
     * This simulates custom table mapping configuration that would normally be
     * done in production configuration.
     */
    @Bean
    public ApplicationRunner registerOrderItemTableMapping(CheapDao cheapDao, CheapFactory factory)
    {
        return _ -> {
            logger.info("Registering 'order_item' AspectTableMapping for SQLite server");

            // Create AspectDef for order_item custom table
            Map<String, PropertyDef> orderItemProps = new LinkedHashMap<>();
            orderItemProps.put("product_name", factory.createPropertyDef("product_name", PropertyType.String));
            orderItemProps.put("quantity", factory.createPropertyDef("quantity", PropertyType.Integer));
            orderItemProps.put("price", factory.createPropertyDef("price", PropertyType.Float));
            AspectDef orderItemAspectDef = factory.createImmutableAspectDef("order_item", orderItemProps);

            // Create AspectTableMapping for order_item
            Map<String, String> columnMapping = Map.of(
                "product_name", "product_name",
                "quantity", "quantity",
                "price", "price"
            );
            AspectTableMapping orderItemTableMapping = new AspectTableMapping(
                orderItemAspectDef,
                "order_item",
                columnMapping,
                false,  // hasCatalogId
                true    // hasEntityId
            );

            // Register mapping with DAO and create table
            if (cheapDao instanceof SqliteDao sqliteDao)
            {
                sqliteDao.addAspectTableMapping(orderItemTableMapping);
                sqliteDao.createTable(orderItemTableMapping);
                logger.info("'order_item' AspectTableMapping registered and table created");
            }
            else
            {
                logger.warn("CheapDao is not a SqliteDao, cannot register AspectTableMapping");
            }
        };
    }
}
