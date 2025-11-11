package net.netbeing.cheap.integrationtests.config;

import net.netbeing.cheap.db.AspectTableMapping;
import net.netbeing.cheap.db.CheapDao;
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
@TestPropertySource(properties = {
    "server.port=8082",
    "cheap.database.type=sqlite"
})
public class SqliteServerTestConfig
{
    private static final Logger logger = LoggerFactory.getLogger(SqliteServerTestConfig.class);
    private static Path tempDbPath;

    /**
     * Provides a SQLite DataSource for testing.
     */
    @Bean
    @Primary
    public DataSource sqliteDataSource() throws IOException, SQLException
    {
        logger.info("Setting up SQLite database for integration tests");

        // Create temporary database file
        tempDbPath = Files.createTempFile("cheap-sqlite-integration-test-", ".db");
        logger.info("Created temporary SQLite database at: {}", tempDbPath.toAbsolutePath());

        // Set up data source
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDbPath.toAbsolutePath());

        // Initialize schema
        SqliteCheapSchema schema = new SqliteCheapSchema();
        schema.executeMainSchemaDdl(dataSource);
        schema.executeAuditSchemaDdl(dataSource);

        logger.info("SQLite database initialized with schema");
        return dataSource;
    }

    /**
     * Registers the "order_item" AspectTableMapping on application startup.
     * This simulates custom table mapping configuration that would normally be
     * done in production configuration.
     */
    @Bean
    public ApplicationRunner registerOrderItemTableMapping(CheapDao cheapDao, CheapFactory factory)
    {
        return args -> {
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
