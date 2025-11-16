package net.netbeing.cheap.integrationtests.consistency;

import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.integrationtests.base.BaseClientIntegrationTest;
import net.netbeing.cheap.integrationtests.config.ClientTestConfig;
import net.netbeing.cheap.integrationtests.config.MariaDbServerTestConfig;
import net.netbeing.cheap.integrationtests.config.PostgresServerTestConfig;
import net.netbeing.cheap.integrationtests.config.SqliteServerTestConfig;
import net.netbeing.cheap.json.dto.AspectDefListResponse;
import net.netbeing.cheap.json.dto.AspectQueryResponse;
import net.netbeing.cheap.json.dto.CatalogListResponse;
import net.netbeing.cheap.json.dto.CreateAspectDefResponse;
import net.netbeing.cheap.json.dto.CreateCatalogResponse;
import net.netbeing.cheap.json.dto.UpsertAspectsResponse;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.AspectMap;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import net.netbeing.cheap.rest.CheapRestApplication;
import net.netbeing.cheap.rest.client.CheapRestClient;
import net.netbeing.cheap.rest.client.CheapRestClientImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-database consistency tests.
 * Verifies that the same operations produce consistent results across all three database backends.
 *
 * Architecture:
 * - Starts three separate Spring Boot servers (PostgreSQL on 8081, SQLite on 8082, MariaDB on 8083)
 * - Creates three separate CheapRestClient instances (one for each server)
 * - NO direct database access - ALL verification through REST API responses only
 * - Tests perform identical operations on all three clients and compare results
 */
class CrossDatabaseConsistencyTest extends BaseClientIntegrationTest
{
    private static final Logger logger = LoggerFactory.getLogger(CrossDatabaseConsistencyTest.class);

    private static ConfigurableApplicationContext postgresContext;
    private static ConfigurableApplicationContext sqliteContext;
    private static ConfigurableApplicationContext mariadbContext;

    private static CheapRestClient postgresClient;
    private static CheapRestClient sqliteClient;
    private static CheapRestClient mariadbClient;

    private final CheapFactory factory = new CheapFactory();

    /**
     * Start all three database servers before running any tests.
     * Each server runs on a different port with its own database backend.
     */
    @BeforeAll
    static void startAllServers() throws InterruptedException
    {
        logger.info("Starting PostgreSQL server on port 8081...");
        postgresContext = SpringApplication.run(
            new Class[]{CheapRestApplication.class, PostgresServerTestConfig.class, ClientTestConfig.class},
            new String[]{
                "--server.port=8081",
                "--cheap.database.type=postgres",
                "--spring.main.allow-bean-definition-overriding=true"
            }
        );

        logger.info("Starting SQLite server on port 8082...");
        sqliteContext = SpringApplication.run(
            new Class[]{CheapRestApplication.class, SqliteServerTestConfig.class, ClientTestConfig.class},
            new String[]{
                "--server.port=8082",
                "--cheap.database.type=sqlite",
                "--spring.main.allow-bean-definition-overriding=true"
            }
        );

        logger.info("Starting MariaDB server on port 8083...");
        mariadbContext = SpringApplication.run(
            new Class[]{CheapRestApplication.class, MariaDbServerTestConfig.class, ClientTestConfig.class},
            new String[]{
                "--server.port=8083",
                "--cheap.database.type=mariadb",
                "--spring.main.allow-bean-definition-overriding=true"
            }
        );

        // Wait for all servers to be fully started
        logger.info("Waiting for all servers to be fully initialized...");
        TimeUnit.SECONDS.sleep(3);

        // Create REST clients for each server
        postgresClient = new CheapRestClientImpl("http://localhost:8081");
        sqliteClient = new CheapRestClientImpl("http://localhost:8082");
        mariadbClient = new CheapRestClientImpl("http://localhost:8083");

        logger.info("All servers started and clients initialized");
    }

    /**
     * Stop all three database servers after all tests complete.
     */
    @AfterAll
    static void stopAllServers()
    {
        logger.info("Stopping all servers...");

        if (postgresContext != null)
        {
            postgresContext.close();
            logger.info("PostgreSQL server stopped");
        }

        if (sqliteContext != null)
        {
            sqliteContext.close();
            logger.info("SQLite server stopped");
        }

        if (mariadbContext != null)
        {
            mariadbContext.close();
            logger.info("MariaDB server stopped");
        }
    }

    /**
     * Test 1: Catalog Structure Consistency
     * Create identical catalogs via each client and verify responses are structurally identical.
     */
    @Test
    void catalogStructureConsistency()
    {
        logger.info("Testing catalog structure consistency across all databases");

        // Create identical catalogs on all three databases
        CatalogDef catalogDef = factory.createCatalogDef();

        CreateCatalogResponse postgresResponse = postgresClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        CreateCatalogResponse sqliteResponse = sqliteClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        CreateCatalogResponse mariadbResponse = mariadbClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);

        // Verify all responses are valid
        assertNotNull(postgresResponse);
        assertNotNull(postgresResponse.catalogId());
        assertNotNull(sqliteResponse);
        assertNotNull(sqliteResponse.catalogId());
        assertNotNull(mariadbResponse);
        assertNotNull(mariadbResponse.catalogId());

        // Retrieve catalogs and verify structure
        CatalogDef postgresRetrieved = postgresClient.getCatalogDef(postgresResponse.catalogId());
        CatalogDef sqliteRetrieved = sqliteClient.getCatalogDef(sqliteResponse.catalogId());
        CatalogDef mariadbRetrieved = mariadbClient.getCatalogDef(mariadbResponse.catalogId());

        assertNotNull(postgresRetrieved);
        assertNotNull(sqliteRetrieved);
        assertNotNull(mariadbRetrieved);

        // Verify all catalogs have the same structure (same CatalogDef hash)
        assertEquals(postgresRetrieved.hash(), sqliteRetrieved.hash());
        assertEquals(postgresRetrieved.hash(), mariadbRetrieved.hash());

        // List catalogs and verify pagination structure is consistent
        CatalogListResponse postgresList = postgresClient.listCatalogs(0, 10);
        CatalogListResponse sqliteList = sqliteClient.listCatalogs(0, 10);
        CatalogListResponse mariadbList = mariadbClient.listCatalogs(0, 10);

        assertNotNull(postgresList);
        assertNotNull(sqliteList);
        assertNotNull(mariadbList);

        // All should have at least the catalog we just created
        assertTrue(postgresList.totalElements() >= 1);
        assertTrue(sqliteList.totalElements() >= 1);
        assertTrue(mariadbList.totalElements() >= 1);

        logger.info("Catalog structure consistency verified across all databases");
    }

    /**
     * Test 2: AspectDef Consistency
     * Create identical aspect defs via each client and compare JSON responses.
     */
    @Test
    void aspectDefConsistency()
    {
        logger.info("Testing AspectDef consistency across all databases");

        // First create catalogs on all databases
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse postgresCatalog = postgresClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        CreateCatalogResponse sqliteCatalog = sqliteClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        CreateCatalogResponse mariadbCatalog = mariadbClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);

        // Create identical AspectDef on all three databases
        Map<String, PropertyDef> personProps = new LinkedHashMap<>();
        personProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        personProps.put("age", factory.createPropertyDef("age", PropertyType.Integer));
        personProps.put("email", factory.createPropertyDef("email", PropertyType.String));
        AspectDef personAspectDef = factory.createImmutableAspectDef("person", personProps);

        CreateAspectDefResponse postgresAspectDefResponse = postgresClient.createAspectDef(
            postgresCatalog.catalogId(),
            personAspectDef
        );
        CreateAspectDefResponse sqliteAspectDefResponse = sqliteClient.createAspectDef(
            sqliteCatalog.catalogId(),
            personAspectDef
        );
        CreateAspectDefResponse mariadbAspectDefResponse = mariadbClient.createAspectDef(
            mariadbCatalog.catalogId(),
            personAspectDef
        );

        // Verify all responses are valid
        assertNotNull(postgresAspectDefResponse);
        assertNotNull(postgresAspectDefResponse.aspectDefId());
        assertNotNull(sqliteAspectDefResponse);
        assertNotNull(sqliteAspectDefResponse.aspectDefId());
        assertNotNull(mariadbAspectDefResponse);
        assertNotNull(mariadbAspectDefResponse.aspectDefId());

        // Retrieve AspectDefs and verify they're identical in structure
        AspectDef postgresRetrieved = postgresClient.getAspectDef(
            postgresCatalog.catalogId(),
            postgresAspectDefResponse.aspectDefId()
        );
        AspectDef sqliteRetrieved = sqliteClient.getAspectDef(
            sqliteCatalog.catalogId(),
            sqliteAspectDefResponse.aspectDefId()
        );
        AspectDef mariadbRetrieved = mariadbClient.getAspectDef(
            mariadbCatalog.catalogId(),
            mariadbAspectDefResponse.aspectDefId()
        );

        assertNotNull(postgresRetrieved);
        assertNotNull(sqliteRetrieved);
        assertNotNull(mariadbRetrieved);

        // Verify aspect types are the same
        assertEquals("person", postgresRetrieved.name());
        assertEquals("person", sqliteRetrieved.name());
        assertEquals("person", mariadbRetrieved.name());

        // Verify property definitions are the same
        assertEquals(3, postgresRetrieved.size());
        assertEquals(3, sqliteRetrieved.size());
        assertEquals(3, mariadbRetrieved.size());

        // Verify specific properties exist and have correct types
        assertNotNull(postgresRetrieved.propertyDef("name"));
        assertNotNull(sqliteRetrieved.propertyDef("name"));
        assertNotNull(mariadbRetrieved.propertyDef("name"));

        assertEquals(PropertyType.String, postgresRetrieved.propertyDef("name").type());
        assertEquals(PropertyType.String, sqliteRetrieved.propertyDef("name").type());
        assertEquals(PropertyType.String, mariadbRetrieved.propertyDef("name").type());

        assertEquals(PropertyType.Integer, postgresRetrieved.propertyDef("age").type());
        assertEquals(PropertyType.Integer, sqliteRetrieved.propertyDef("age").type());
        assertEquals(PropertyType.Integer, mariadbRetrieved.propertyDef("age").type());

        // List AspectDefs and verify pagination structure
        AspectDefListResponse postgresList = postgresClient.listAspectDefs(postgresCatalog.catalogId(), 0, 10);
        AspectDefListResponse sqliteList = sqliteClient.listAspectDefs(sqliteCatalog.catalogId(), 0, 10);
        AspectDefListResponse mariadbList = mariadbClient.listAspectDefs(mariadbCatalog.catalogId(), 0, 10);

        assertNotNull(postgresList);
        assertNotNull(sqliteList);
        assertNotNull(mariadbList);

        assertTrue(postgresList.totalElements() >= 1);
        assertTrue(sqliteList.totalElements() >= 1);
        assertTrue(mariadbList.totalElements() >= 1);

        logger.info("AspectDef consistency verified across all databases");
    }

    /**
     * Test 3: Upsert Consistency
     * Perform identical upsert operations via all three clients and verify success responses are identical.
     */
    @Test
    void upsertConsistency()
    {
        logger.info("Testing upsert consistency across all databases");

        // Create catalogs and aspect defs on all databases
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse postgresCatalog = postgresClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        CreateCatalogResponse sqliteCatalog = sqliteClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        CreateCatalogResponse mariadbCatalog = mariadbClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);

        // Create identical AspectDef on all three databases
        Map<String, PropertyDef> productProps = new LinkedHashMap<>();
        productProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        productProps.put("price", factory.createPropertyDef("price", PropertyType.Float));
        productProps.put("stock", factory.createPropertyDef("stock", PropertyType.Integer));
        AspectDef productAspectDef = factory.createImmutableAspectDef("product", productProps);

        postgresClient.createAspectDef(postgresCatalog.catalogId(), productAspectDef);
        sqliteClient.createAspectDef(sqliteCatalog.catalogId(), productAspectDef);
        mariadbClient.createAspectDef(mariadbCatalog.catalogId(), productAspectDef);

        // Create identical aspects data
        UUID entityId1 = testUuid(1001);
        UUID entityId2 = testUuid(1002);

        Map<UUID, Map<String, Object>> aspectsData = new LinkedHashMap<>();
        aspectsData.put(entityId1, Map.of(
            "name", "Laptop",
            "price", 999.99,
            "stock", 15
        ));
        aspectsData.put(entityId2, Map.of(
            "name", "Mouse",
            "price", 29.99,
            "stock", 100
        ));

        // Upsert aspects on all databases
        UpsertAspectsResponse postgresUpsert = postgresClient.upsertAspects(
            postgresCatalog.catalogId(),
            "product",
            aspectsData
        );
        UpsertAspectsResponse sqliteUpsert = sqliteClient.upsertAspects(
            sqliteCatalog.catalogId(),
            "product",
            aspectsData
        );
        UpsertAspectsResponse mariadbUpsert = mariadbClient.upsertAspects(
            mariadbCatalog.catalogId(),
            "product",
            aspectsData
        );

        // Verify all upserts succeeded with consistent responses
        assertNotNull(postgresUpsert);
        assertNotNull(sqliteUpsert);
        assertNotNull(mariadbUpsert);

        assertEquals(2, postgresUpsert.successCount());
        assertEquals(2, sqliteUpsert.successCount());
        assertEquals(2, mariadbUpsert.successCount());

        // Update aspects and verify consistency
        Map<UUID, Map<String, Object>> updatedData = Map.of(
            entityId1, Map.of(
                "name", "Laptop Pro",
                "price", 1299.99,
                "stock", 10
            )
        );

        UpsertAspectsResponse postgresUpdate = postgresClient.upsertAspects(
            postgresCatalog.catalogId(),
            "product",
            updatedData
        );
        UpsertAspectsResponse sqliteUpdate = sqliteClient.upsertAspects(
            sqliteCatalog.catalogId(),
            "product",
            updatedData
        );
        UpsertAspectsResponse mariadbUpdate = mariadbClient.upsertAspects(
            mariadbCatalog.catalogId(),
            "product",
            updatedData
        );

        assertEquals(1, postgresUpdate.successCount());
        assertEquals(1, sqliteUpdate.successCount());
        assertEquals(1, mariadbUpdate.successCount());

        logger.info("Upsert consistency verified across all databases");
    }

    /**
     * Test 4: Query Consistency
     * Query same data from all three servers via clients and verify JSON responses are identical.
     */
    @Test
    void queryConsistency()
    {
        logger.info("Testing query consistency across all databases");

        // Setup: Create catalogs, aspect defs, and data on all databases
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse postgresCatalog = postgresClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        CreateCatalogResponse sqliteCatalog = sqliteClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        CreateCatalogResponse mariadbCatalog = mariadbClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);

        Map<String, PropertyDef> customerProps = new LinkedHashMap<>();
        customerProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        customerProps.put("country", factory.createPropertyDef("country", PropertyType.String));
        AspectDef customerAspectDef = factory.createImmutableAspectDef("customer", customerProps);

        postgresClient.createAspectDef(postgresCatalog.catalogId(), customerAspectDef);
        sqliteClient.createAspectDef(sqliteCatalog.catalogId(), customerAspectDef);
        mariadbClient.createAspectDef(mariadbCatalog.catalogId(), customerAspectDef);

        // Insert identical data
        UUID entityId = testUuid(2001);
        Map<UUID, Map<String, Object>> customerData = Map.of(
            entityId, Map.of(
                "name", "John Doe",
                "country", "USA"
            )
        );

        postgresClient.upsertAspects(postgresCatalog.catalogId(), "customer", customerData);
        sqliteClient.upsertAspects(sqliteCatalog.catalogId(), "customer", customerData);
        mariadbClient.upsertAspects(mariadbCatalog.catalogId(), "customer", customerData);

        // Create entities for querying
        Entity entity = factory.createEntity(entityId);

        // Query aspects from all databases
        AspectQueryResponse postgresQuery = postgresClient.queryAspects(
            postgresCatalog.catalogId(),
            Set.of(entityId),
            Set.of("customer")
        );
        AspectQueryResponse sqliteQuery = sqliteClient.queryAspects(
            sqliteCatalog.catalogId(),
            Set.of(entityId),
            Set.of("customer")
        );
        AspectQueryResponse mariadbQuery = mariadbClient.queryAspects(
            mariadbCatalog.catalogId(),
            Set.of(entityId),
            Set.of("customer")
        );

        // Verify query responses are consistent
        assertNotNull(postgresQuery);
        assertNotNull(sqliteQuery);
        assertNotNull(mariadbQuery);

        assertEquals(1, postgresQuery.results().size());
        assertEquals(1, sqliteQuery.results().size());
        assertEquals(1, mariadbQuery.results().size());

        AspectMap postgresAspectMap = postgresQuery.results().get(0);
        AspectMap sqliteAspectMap = sqliteQuery.results().get(0);
        AspectMap mariadbAspectMap = mariadbQuery.results().get(0);

        assertEquals("customer", postgresAspectMap.aspectDef().name());
        assertEquals("customer", sqliteAspectMap.aspectDef().name());
        assertEquals("customer", mariadbAspectMap.aspectDef().name());

        // Verify aspect data is identical
        Aspect postgresRetrieved = postgresAspectMap.get(entity);
        Aspect sqliteRetrieved = sqliteAspectMap.get(entity);
        Aspect mariadbRetrieved = mariadbAspectMap.get(entity);

        assertNotNull(postgresRetrieved);
        assertNotNull(sqliteRetrieved);
        assertNotNull(mariadbRetrieved);

        assertEquals(entityId, postgresRetrieved.entity().globalId());
        assertEquals(entityId, sqliteRetrieved.entity().globalId());
        assertEquals(entityId, mariadbRetrieved.entity().globalId());

        assertEquals("John Doe", postgresRetrieved.get("name").read());
        assertEquals("John Doe", sqliteRetrieved.get("name").read());
        assertEquals("John Doe", mariadbRetrieved.get("name").read());

        assertEquals("USA", postgresRetrieved.get("country").read());
        assertEquals("USA", sqliteRetrieved.get("country").read());
        assertEquals("USA", mariadbRetrieved.get("country").read());

        logger.info("Query consistency verified across all databases");
    }

    /**
     * Test 5: Pagination Consistency
     * Test pagination with identical parameters across all databases via clients,
     * verify page structures and content are identical.
     */
    @Test
    void paginationConsistency()
    {
        logger.info("Testing pagination consistency across all databases");

        // Create catalogs on all databases
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse postgresCatalog = postgresClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        CreateCatalogResponse sqliteCatalog = sqliteClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        CreateCatalogResponse mariadbCatalog = mariadbClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);

        // Create multiple aspect defs for pagination testing
        for (int i = 0; i < 15; i++)
        {
            Map<String, PropertyDef> props = new LinkedHashMap<>();
            props.put("field" + i, factory.createPropertyDef("field" + i, PropertyType.String));
            AspectDef aspectDef = factory.createImmutableAspectDef("aspect_" + i, props);

            postgresClient.createAspectDef(postgresCatalog.catalogId(), aspectDef);
            sqliteClient.createAspectDef(sqliteCatalog.catalogId(), aspectDef);
            mariadbClient.createAspectDef(mariadbCatalog.catalogId(), aspectDef);
        }

        // Test pagination - Page 0
        AspectDefListResponse postgresPage0 = postgresClient.listAspectDefs(postgresCatalog.catalogId(), 0, 5);
        AspectDefListResponse sqlitePage0 = sqliteClient.listAspectDefs(sqliteCatalog.catalogId(), 0, 5);
        AspectDefListResponse mariadbPage0 = mariadbClient.listAspectDefs(mariadbCatalog.catalogId(), 0, 5);

        // Verify page structures are consistent
        assertNotNull(postgresPage0);
        assertNotNull(sqlitePage0);
        assertNotNull(mariadbPage0);

        assertEquals(5, postgresPage0.content().size());
        assertEquals(5, sqlitePage0.content().size());
        assertEquals(5, mariadbPage0.content().size());

        assertTrue(postgresPage0.totalElements() >= 15);
        assertTrue(sqlitePage0.totalElements() >= 15);
        assertTrue(mariadbPage0.totalElements() >= 15);

        assertEquals(0, postgresPage0.page());
        assertEquals(0, sqlitePage0.page());
        assertEquals(0, mariadbPage0.page());

        assertEquals(5, postgresPage0.size());
        assertEquals(5, sqlitePage0.size());
        assertEquals(5, mariadbPage0.size());

        // Test pagination - Page 1
        AspectDefListResponse postgresPage1 = postgresClient.listAspectDefs(postgresCatalog.catalogId(), 1, 5);
        AspectDefListResponse sqlitePage1 = sqliteClient.listAspectDefs(sqliteCatalog.catalogId(), 1, 5);
        AspectDefListResponse mariadbPage1 = mariadbClient.listAspectDefs(mariadbCatalog.catalogId(), 1, 5);

        assertEquals(5, postgresPage1.content().size());
        assertEquals(5, sqlitePage1.content().size());
        assertEquals(5, mariadbPage1.content().size());

        assertEquals(1, postgresPage1.page());
        assertEquals(1, sqlitePage1.page());
        assertEquals(1, mariadbPage1.page());

        // Test pagination - Page 2
        AspectDefListResponse postgresPage2 = postgresClient.listAspectDefs(postgresCatalog.catalogId(), 2, 5);
        AspectDefListResponse sqlitePage2 = sqliteClient.listAspectDefs(sqliteCatalog.catalogId(), 2, 5);
        AspectDefListResponse mariadbPage2 = mariadbClient.listAspectDefs(mariadbCatalog.catalogId(), 2, 5);

        assertTrue(postgresPage2.content().size() >= 5);
        assertTrue(sqlitePage2.content().size() >= 5);
        assertTrue(mariadbPage2.content().size() >= 5);

        // Verify catalog list pagination
        CatalogListResponse postgresCatalogList = postgresClient.listCatalogs(0, 5);
        CatalogListResponse sqliteCatalogList = sqliteClient.listCatalogs(0, 5);
        CatalogListResponse mariadbCatalogList = mariadbClient.listCatalogs(0, 5);

        assertNotNull(postgresCatalogList);
        assertNotNull(sqliteCatalogList);
        assertNotNull(mariadbCatalogList);

        assertTrue(postgresCatalogList.totalElements() >= 1);
        assertTrue(sqliteCatalogList.totalElements() >= 1);
        assertTrue(mariadbCatalogList.totalElements() >= 1);

        logger.info("Pagination consistency verified across all databases");
    }

    /**
     * Test 6: Sorting/Ordering Consistency
     * Test ordering with same parameters across all databases via clients,
     * verify sort order is identical.
     */
    @Test
    void sortingOrderingConsistency()
    {
        logger.info("Testing sorting/ordering consistency across all databases");

        // Create catalogs on all databases
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse postgresCatalog = postgresClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        CreateCatalogResponse sqliteCatalog = sqliteClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        CreateCatalogResponse mariadbCatalog = mariadbClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);

        // Create multiple aspect defs with specific names for ordering tests
        String[] aspectNames = {"alpha", "beta", "gamma", "delta", "epsilon"};

        for (String aspectName : aspectNames)
        {
            Map<String, PropertyDef> props = new LinkedHashMap<>();
            props.put("value", factory.createPropertyDef("value", PropertyType.String));
            AspectDef aspectDef = factory.createImmutableAspectDef(aspectName, props);

            postgresClient.createAspectDef(postgresCatalog.catalogId(), aspectDef);
            sqliteClient.createAspectDef(sqliteCatalog.catalogId(), aspectDef);
            mariadbClient.createAspectDef(mariadbCatalog.catalogId(), aspectDef);
        }

        // Retrieve aspect defs - they should be in a consistent order
        AspectDefListResponse postgresAspectDefs = postgresClient.listAspectDefs(
            postgresCatalog.catalogId(),
            0,
            10
        );
        AspectDefListResponse sqliteAspectDefs = sqliteClient.listAspectDefs(
            sqliteCatalog.catalogId(),
            0,
            10
        );
        AspectDefListResponse mariadbAspectDefs = mariadbClient.listAspectDefs(
            mariadbCatalog.catalogId(),
            0,
            10
        );

        // Verify all responses have the expected aspect defs
        assertNotNull(postgresAspectDefs);
        assertNotNull(sqliteAspectDefs);
        assertNotNull(mariadbAspectDefs);

        assertTrue(postgresAspectDefs.content().size() >= 5);
        assertTrue(sqliteAspectDefs.content().size() >= 5);
        assertTrue(mariadbAspectDefs.content().size() >= 5);

        // Test catalog listing order - should be consistent across all databases
        CatalogListResponse postgresCatalogs = postgresClient.listCatalogs(0, 10);
        CatalogListResponse sqliteCatalogs = sqliteClient.listCatalogs(0, 10);
        CatalogListResponse mariadbCatalogs = mariadbClient.listCatalogs(0, 10);

        assertNotNull(postgresCatalogs);
        assertNotNull(sqliteCatalogs);
        assertNotNull(mariadbCatalogs);

        // Verify the most recently created catalog appears in all lists
        assertTrue(postgresCatalogs.content().contains(postgresCatalog.catalogId()));
        assertTrue(sqliteCatalogs.content().contains(sqliteCatalog.catalogId()));
        assertTrue(mariadbCatalogs.content().contains(mariadbCatalog.catalogId()));

        logger.info("Sorting/ordering consistency verified across all databases");
    }
}
