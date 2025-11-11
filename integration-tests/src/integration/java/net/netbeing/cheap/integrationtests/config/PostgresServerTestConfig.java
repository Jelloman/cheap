package net.netbeing.cheap.integrationtests.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import net.netbeing.cheap.db.AspectTableMapping;
import net.netbeing.cheap.db.CheapDao;
import net.netbeing.cheap.db.postgres.PostgresAdapter;
import net.netbeing.cheap.db.postgres.PostgresCheapSchema;
import net.netbeing.cheap.db.postgres.PostgresDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PostgreSQL server configuration for integration tests.
 * Loads cheap-rest with ONLY cheap-db-postgres, configures embedded PostgreSQL,
 * registers AspectTableMapping for "address" table on startup, and runs on port 8081.
 */
@TestConfiguration
@TestPropertySource(properties = {
    "server.port=8081",
    "cheap.database.type=postgres"
})
public class PostgresServerTestConfig
{
    private static final Logger logger = LoggerFactory.getLogger(PostgresServerTestConfig.class);

    /**
     * Provides CheapFactory bean for creating Cheap objects.
     */
    @Bean
    public CheapFactory cheapFactory()
    {
        return new CheapFactory();
    }

    /**
     * Provides an embedded PostgreSQL DataSource for testing.
     */
    @Bean
    @Primary
    public DataSource embeddedPostgresDataSource() throws IOException, SQLException
    {
        logger.info("Starting embedded PostgreSQL for integration tests on port 5433");

        EmbeddedPostgres embeddedPostgres = EmbeddedPostgres.builder()
            .setPort(5433) // Non-standard port to avoid conflicts
            .start();

        DataSource dataSource = embeddedPostgres.getPostgresDatabase();

        // Initialize schema
        PostgresCheapSchema schema = new PostgresCheapSchema();
        schema.executeMainSchemaDdl(dataSource);
        schema.executeAuditSchemaDdl(dataSource);

        logger.info("Embedded PostgreSQL initialized with schema");
        return dataSource;
    }

    /**
     * Provides PostgresDao bean for the server.
     */
    @Bean
    @Primary
    public CheapDao cheapDao(DataSource dataSource, CheapFactory factory)
    {
        logger.info("Creating PostgresDao bean for integration tests");
        PostgresAdapter adapter = new PostgresAdapter(dataSource, factory);
        return new PostgresDao(adapter);
    }

    /**
     * Registers the "address" AspectTableMapping on application startup.
     * This simulates custom table mapping configuration that would normally be
     * done in production configuration.
     */
    @Bean
    public ApplicationRunner registerAddressTableMapping(CheapDao cheapDao, CheapFactory factory)
    {
        return args -> {
            logger.info("Registering 'address' AspectTableMapping for PostgreSQL server");

            // Create AspectDef for address custom table
            Map<String, PropertyDef> addressProps = new LinkedHashMap<>();
            addressProps.put("street", factory.createPropertyDef("street", PropertyType.String));
            addressProps.put("city", factory.createPropertyDef("city", PropertyType.String));
            addressProps.put("zip", factory.createPropertyDef("zip", PropertyType.String));
            AspectDef addressAspectDef = factory.createImmutableAspectDef("address", addressProps);

            // Create AspectTableMapping for address
            Map<String, String> columnMapping = Map.of(
                "street", "street",
                "city", "city",
                "zip", "zip"
            );
            AspectTableMapping addressTableMapping = new AspectTableMapping(
                addressAspectDef,
                "address",
                columnMapping,
                false,  // hasCatalogId
                true    // hasEntityId
            );

            // Register mapping with DAO and create table
            if (cheapDao instanceof PostgresDao postgresDao)
            {
                postgresDao.addAspectTableMapping(addressTableMapping);
                postgresDao.createTable(addressTableMapping);
                logger.info("'address' AspectTableMapping registered and table created");
            }
            else
            {
                logger.warn("CheapDao is not a PostgresDao, cannot register AspectTableMapping");
            }
        };
    }
}
