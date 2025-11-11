package net.netbeing.cheap.integrationtests.config;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import net.netbeing.cheap.db.AspectTableMapping;
import net.netbeing.cheap.db.CheapDao;
import net.netbeing.cheap.db.mariadb.MariaDbCheapSchema;
import net.netbeing.cheap.db.mariadb.MariaDbDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.mariadb.jdbc.MariaDbDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MariaDB server configuration for integration tests.
 * Loads cheap-rest with ONLY cheap-db-mariadb, configures MariaDB test database,
 * registers AspectTableMapping for "inventory" table on startup, and runs on port 8083.
 */
@TestConfiguration
@TestPropertySource(properties = {
    "server.port=8083",
    "cheap.database.type=mariadb"
})
public class MariaDbServerTestConfig
{
    private static final Logger logger = LoggerFactory.getLogger(MariaDbServerTestConfig.class);
    private static DB embeddedMariaDb;

    /**
     * Provides an embedded MariaDB DataSource for testing.
     */
    @Bean
    @Primary
    public DataSource embeddedMariaDbDataSource() throws Exception
    {
        logger.info("Starting embedded MariaDB for integration tests");

        // Start embedded MariaDB4j on a non-standard port
        DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
        configBuilder.setPort(3307); // Non-standard port to avoid conflicts
        embeddedMariaDb = DB.newEmbeddedDB(configBuilder.build());
        embeddedMariaDb.start();

        // Create test database
        embeddedMariaDb.createDB("cheap_test");

        // Set up data source
        MariaDbDataSource dataSource = new MariaDbDataSource();
        dataSource.setUrl("jdbc:mariadb://localhost:3307/cheap_test?allowMultiQueries=true");
        dataSource.setUser("root");
        dataSource.setPassword("");

        // Initialize schema
        MariaDbCheapSchema schema = new MariaDbCheapSchema();
        schema.executeMainSchemaDdl(dataSource);
        schema.executeForeignKeysDdl(dataSource);  // Enable foreign keys for integration tests
        schema.executeAuditSchemaDdl(dataSource);

        logger.info("Embedded MariaDB initialized with schema");
        return dataSource;
    }

    /**
     * Registers the "inventory" AspectTableMapping on application startup.
     * This simulates custom table mapping configuration that would normally be
     * done in production configuration.
     */
    @Bean
    public ApplicationRunner registerInventoryTableMapping(CheapDao cheapDao, CheapFactory factory)
    {
        return args -> {
            logger.info("Registering 'inventory' AspectTableMapping for MariaDB server");

            // Create AspectDef for inventory custom table
            Map<String, PropertyDef> inventoryProps = new LinkedHashMap<>();
            inventoryProps.put("item_code", factory.createPropertyDef("item_code", PropertyType.String));
            inventoryProps.put("warehouse", factory.createPropertyDef("warehouse", PropertyType.String));
            inventoryProps.put("stock_qty", factory.createPropertyDef("stock_qty", PropertyType.Integer));
            AspectDef inventoryAspectDef = factory.createImmutableAspectDef("inventory", inventoryProps);

            // Create AspectTableMapping for inventory
            Map<String, String> columnMapping = Map.of(
                "item_code", "item_code",
                "warehouse", "warehouse",
                "stock_qty", "stock_qty"
            );
            AspectTableMapping inventoryTableMapping = new AspectTableMapping(
                inventoryAspectDef,
                "inventory",
                columnMapping,
                false,  // hasCatalogId
                true    // hasEntityId
            );

            // Register mapping with DAO and create table
            if (cheapDao instanceof MariaDbDao mariaDbDao)
            {
                mariaDbDao.addAspectTableMapping(inventoryTableMapping);
                mariaDbDao.createTable(inventoryTableMapping);
                logger.info("'inventory' AspectTableMapping registered and table created");
            }
            else
            {
                logger.warn("CheapDao is not a MariaDbDao, cannot register AspectTableMapping");
            }
        };
    }
}
