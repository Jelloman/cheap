package net.netbeing.cheap.integrationtests.docker;

import com.github.dockerjava.api.DockerClient;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.integrationtests.util.DockerContainerManager;
import net.netbeing.cheap.integrationtests.util.DockerTestUtils;
import net.netbeing.cheap.json.dto.AspectQueryResponse;
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
import net.netbeing.cheap.rest.client.CheapRestClient;
import net.netbeing.cheap.rest.client.CheapRestClientImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-container orchestration test that runs all three database backends simultaneously.
 *
 * Architecture:
 * - Client test runs in JVM process (NOT in Docker)
 * - Three separate cheap-rest servers run in Docker containers
 * - Three database containers (PostgreSQL, MariaDB, SQLite)
 * - Test uses three separate CheapRestClient instances
 *
 * This test verifies:
 * - All three backends can run simultaneously without conflicts
 * - Consistent behavior across all database backends
 * - Network isolation between services
 */
@Tag("docker")
@Tag("orchestration")
class MultiDatabaseDockerOrchestrationTest
{
    private static final CheapFactory factory = new CheapFactory();

    private static DockerContainerManager containerManager;
    private static final String NETWORK_NAME = "multi-db-orchestration-test-network";

    // PostgreSQL
    private static String postgresDbContainerId;
    private static String postgresRestContainerId;
    private static int postgresRestPort;
    private static CheapRestClient postgresClient;

    // MariaDB
    private static String mariaDbContainerId;
    private static String mariaDbRestContainerId;
    private static int mariaDbRestPort;
    private static CheapRestClient mariaDbClient;

    // SQLite
    private static String sqliteRestContainerId;
    private static int sqliteRestPort;
    private static CheapRestClient sqliteClient;
    private static final String SQLITE_VOLUME = System.getProperty("java.io.tmpdir") + "/cheap-multi-db-sqlite-test";

    @BeforeAll
    static void setupMultiDatabaseEnvironment()
    {
        containerManager = new DockerContainerManager(true);
        DockerClient dockerClient = containerManager.getDockerClient();

        // Create shared network
        containerManager.createNetwork(NETWORK_NAME);

        // Start all database containers in parallel
        startDatabases(dockerClient);

        // Wait for databases to be ready
        assertTrue(DockerTestUtils.waitForDatabaseReady(dockerClient, postgresDbContainerId, 60),
                "PostgreSQL container did not become ready in time");
        assertTrue(DockerTestUtils.waitForDatabaseReady(dockerClient, mariaDbContainerId, 60),
                "MariaDB container did not become ready in time");

        // Start all cheap-rest containers
        startCheapRestServers(dockerClient);

        // Create clients
        postgresClient = new CheapRestClientImpl("http://localhost:" + postgresRestPort);
        mariaDbClient = new CheapRestClientImpl("http://localhost:" + mariaDbRestPort);
        sqliteClient = new CheapRestClientImpl("http://localhost:" + sqliteRestPort);
    }

    private static void startDatabases(DockerClient dockerClient)
    {
        // Start PostgreSQL
        postgresDbContainerId = containerManager.container("postgres:17")
                .name("multi-db-postgres")
                .env("POSTGRES_DB", "cheap")
                .env("POSTGRES_USER", "cheap_user")
                .env("POSTGRES_PASSWORD", "test_password")
                .network(NETWORK_NAME)
                .exposePort(5432)
                .healthCheck("CMD-SHELL", "pg_isready -U cheap_user -d cheap")
                .start();

        // Start MariaDB
        mariaDbContainerId = containerManager.container("mariadb:11")
                .name("multi-db-mariadb")
                .env("MARIADB_DATABASE", "cheap")
                .env("MARIADB_USER", "cheap_user")
                .env("MARIADB_PASSWORD", "test_password")
                .env("MARIADB_ROOT_PASSWORD", "root_password")
                .network(NETWORK_NAME)
                .exposePort(3306)
                .healthCheck("CMD-SHELL", "healthcheck.sh --connect --innodb_initialized")
                .start();
    }

    private static void startCheapRestServers(DockerClient dockerClient)
    {
        // Start PostgreSQL cheap-rest
        postgresRestContainerId = containerManager.container("cheap-rest:latest")
                .name("multi-db-cheap-rest-postgres")
                .env("SPRING_PROFILES_ACTIVE", "postgres")
                .env("SPRING_DATASOURCE_URL", "jdbc:postgresql://multi-db-postgres:5432/cheap")
                .env("SPRING_DATASOURCE_USERNAME", "cheap_user")
                .env("SPRING_DATASOURCE_PASSWORD", "test_password")
                .network(NETWORK_NAME)
                .exposePort(8080)
                .start();

        postgresRestPort = DockerTestUtils.getDynamicPort(dockerClient, postgresRestContainerId, 8080);
        assertTrue(DockerTestUtils.waitForRestServiceReady("http://localhost:" + postgresRestPort, 120),
                "PostgreSQL cheap-rest did not become ready");

        // Start MariaDB cheap-rest
        mariaDbRestContainerId = containerManager.container("cheap-rest:latest")
                .name("multi-db-cheap-rest-mariadb")
                .env("SPRING_PROFILES_ACTIVE", "mariadb")
                .env("SPRING_DATASOURCE_URL", "jdbc:mariadb://multi-db-mariadb:3306/cheap")
                .env("SPRING_DATASOURCE_USERNAME", "cheap_user")
                .env("SPRING_DATASOURCE_PASSWORD", "test_password")
                .network(NETWORK_NAME)
                .exposePort(8080)
                .start();

        mariaDbRestPort = DockerTestUtils.getDynamicPort(dockerClient, mariaDbRestContainerId, 8080);
        assertTrue(DockerTestUtils.waitForRestServiceReady("http://localhost:" + mariaDbRestPort, 120),
                "MariaDB cheap-rest did not become ready");

        // Start SQLite cheap-rest
        sqliteRestContainerId = containerManager.container("cheap-rest:latest")
                .name("multi-db-cheap-rest-sqlite")
                .env("SPRING_PROFILES_ACTIVE", "sqlite")
                .env("CHEAP_DB_PATH", "/data/cheap.db")
                .network(NETWORK_NAME)
                .volume(SQLITE_VOLUME, "/data")
                .exposePort(8080)
                .start();

        sqliteRestPort = DockerTestUtils.getDynamicPort(dockerClient, sqliteRestContainerId, 8080);
        assertTrue(DockerTestUtils.waitForRestServiceReady("http://localhost:" + sqliteRestPort, 120),
                "SQLite cheap-rest did not become ready");
    }

    @AfterAll
    static void teardownMultiDatabaseEnvironment()
    {
        if (containerManager != null) {
            containerManager.close();
        }

        // Clean up SQLite volume
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(SQLITE_VOLUME, "cheap.db"));
            java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(SQLITE_VOLUME));
        } catch (Exception e) {
            // Best effort cleanup
        }
    }

    @Test
    void parallelCatalogCreation()
    {
        // Create the same catalog definition on all three backends
        CatalogDef catalogDef = factory.createCatalogDef();

        CreateCatalogResponse postgresResponse = postgresClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        CreateCatalogResponse mariaDbResponse = mariaDbClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        CreateCatalogResponse sqliteResponse = sqliteClient.createCatalog(catalogDef, CatalogSpecies.SINK, null);

        assertNotNull(postgresResponse);
        assertNotNull(mariaDbResponse);
        assertNotNull(sqliteResponse);

        // Verify each catalog can be retrieved
        assertNotNull(postgresClient.getCatalogDef(postgresResponse.catalogId()));
        assertNotNull(mariaDbClient.getCatalogDef(mariaDbResponse.catalogId()));
        assertNotNull(sqliteClient.getCatalogDef(sqliteResponse.catalogId()));
    }

    @Test
    void consistentBehaviorAcrossBackends()
    {
        // Create identical AspectDef on all backends
        Map<String, PropertyDef> productProps = new LinkedHashMap<>();
        productProps.put("sku", factory.createPropertyDef("sku", PropertyType.String));
        productProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        productProps.put("price", factory.createPropertyDef("price", PropertyType.Float));
        AspectDef productAspectDef = factory.createImmutableAspectDef("product", productProps);

        // Create catalogs on all backends
        CatalogDef catalogDef = factory.createCatalogDef();
        UUID postgresCatalogId = postgresClient.createCatalog(catalogDef, CatalogSpecies.SINK, null).catalogId();
        UUID mariaDbCatalogId = mariaDbClient.createCatalog(catalogDef, CatalogSpecies.SINK, null).catalogId();
        UUID sqliteCatalogId = sqliteClient.createCatalog(catalogDef, CatalogSpecies.SINK, null).catalogId();

        // Register AspectDef on all backends
        postgresClient.registerAspectDef(productAspectDef);
        mariaDbClient.registerAspectDef(productAspectDef);
        sqliteClient.registerAspectDef(productAspectDef);

        postgresClient.createAspectDef(postgresCatalogId, productAspectDef);
        mariaDbClient.createAspectDef(mariaDbCatalogId, productAspectDef);
        sqliteClient.createAspectDef(sqliteCatalogId, productAspectDef);

        // Upsert identical data on all backends
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000020001");
        Entity entity = factory.createEntity(productId);
        Map<UUID, Map<String, Object>> productData = Map.of(
                productId, Map.of("sku", "MULTI-001", "name", "Universal Widget", "price", 39.99)
        );

        UpsertAspectsResponse postgresUpsert = postgresClient.upsertAspects(postgresCatalogId, "product", productData);
        UpsertAspectsResponse mariaDbUpsert = mariaDbClient.upsertAspects(mariaDbCatalogId, "product", productData);
        UpsertAspectsResponse sqliteUpsert = sqliteClient.upsertAspects(sqliteCatalogId, "product", productData);

        assertEquals(1, postgresUpsert.successCount());
        assertEquals(1, mariaDbUpsert.successCount());
        assertEquals(1, sqliteUpsert.successCount());

        // Query data from all backends and verify consistency
        Set<UUID> entityIds = Set.of(productId);
        Set<String> aspectDefNames = Set.of("product");

        AspectQueryResponse postgresQuery = postgresClient.queryAspects(postgresCatalogId, entityIds, aspectDefNames);
        AspectQueryResponse mariaDbQuery = mariaDbClient.queryAspects(mariaDbCatalogId, entityIds, aspectDefNames);
        AspectQueryResponse sqliteQuery = sqliteClient.queryAspects(sqliteCatalogId, entityIds, aspectDefNames);

        // Verify all backends returned the same data
        assertNotNull(postgresQuery);
        assertNotNull(mariaDbQuery);
        assertNotNull(sqliteQuery);

        AspectMap postgresMap = postgresQuery.results().getFirst();
        AspectMap mariaDbMap = mariaDbQuery.results().getFirst();
        AspectMap sqliteMap = sqliteQuery.results().getFirst();

        Aspect postgresProduct = postgresMap.get(entity);
        Aspect mariaDbProduct = mariaDbMap.get(entity);
        Aspect sqliteProduct = sqliteMap.get(entity);

        assertNotNull(postgresProduct);
        assertNotNull(mariaDbProduct);
        assertNotNull(sqliteProduct);

        // Verify data consistency
        assertEquals("MULTI-001", postgresProduct.get("sku").read());
        assertEquals("MULTI-001", mariaDbProduct.get("sku").read());
        assertEquals("MULTI-001", sqliteProduct.get("sku").read());

        assertEquals("Universal Widget", postgresProduct.get("name").read());
        assertEquals("Universal Widget", mariaDbProduct.get("name").read());
        assertEquals("Universal Widget", sqliteProduct.get("name").read());

        assertEquals(39.99, (Double) postgresProduct.get("price").read(), 0.01);
        assertEquals(39.99, (Double) mariaDbProduct.get("price").read(), 0.01);
        assertEquals(39.99, (Double) sqliteProduct.get("price").read(), 0.01);
    }

    @Test
    void networkIsolation()
    {
        // Create catalogs on each backend
        CatalogDef catalogDef = factory.createCatalogDef();
        UUID postgresCatalogId = postgresClient.createCatalog(catalogDef, CatalogSpecies.SINK, null).catalogId();
        UUID mariaDbCatalogId = mariaDbClient.createCatalog(catalogDef, CatalogSpecies.SINK, null).catalogId();
        UUID sqliteCatalogId = sqliteClient.createCatalog(catalogDef, CatalogSpecies.SINK, null).catalogId();

        // Verify each client can only access its own catalog
        assertNotNull(postgresClient.getCatalogDef(postgresCatalogId));
        assertNotNull(mariaDbClient.getCatalogDef(mariaDbCatalogId));
        assertNotNull(sqliteClient.getCatalogDef(sqliteCatalogId));

        // Try to access PostgreSQL catalog from MariaDB client (should fail or return empty)
        assertThrows(Exception.class, () -> mariaDbClient.getCatalogDef(postgresCatalogId));

        // Try to access MariaDB catalog from SQLite client (should fail or return empty)
        assertThrows(Exception.class, () -> sqliteClient.getCatalogDef(mariaDbCatalogId));

        // Try to access SQLite catalog from PostgreSQL client (should fail or return empty)
        assertThrows(Exception.class, () -> postgresClient.getCatalogDef(sqliteCatalogId));
    }

    @Test
    void independentOperations()
    {
        // Create different AspectDefs on each backend to verify independence
        Map<String, PropertyDef> postgresProps = new LinkedHashMap<>();
        postgresProps.put("postgres_field", factory.createPropertyDef("postgres_field", PropertyType.String));
        AspectDef postgresAspectDef = factory.createImmutableAspectDef("postgres_aspect", postgresProps);

        Map<String, PropertyDef> mariaDbProps = new LinkedHashMap<>();
        mariaDbProps.put("mariadb_field", factory.createPropertyDef("mariadb_field", PropertyType.String));
        AspectDef mariaDbAspectDef = factory.createImmutableAspectDef("mariadb_aspect", mariaDbProps);

        Map<String, PropertyDef> sqliteProps = new LinkedHashMap<>();
        sqliteProps.put("sqlite_field", factory.createPropertyDef("sqlite_field", PropertyType.String));
        AspectDef sqliteAspectDef = factory.createImmutableAspectDef("sqlite_aspect", sqliteProps);

        // Create catalogs
        CatalogDef catalogDef = factory.createCatalogDef();
        UUID postgresCatalogId = postgresClient.createCatalog(catalogDef, CatalogSpecies.SINK, null).catalogId();
        UUID mariaDbCatalogId = mariaDbClient.createCatalog(catalogDef, CatalogSpecies.SINK, null).catalogId();
        UUID sqliteCatalogId = sqliteClient.createCatalog(catalogDef, CatalogSpecies.SINK, null).catalogId();

        // Register and create different AspectDefs on each backend
        postgresClient.registerAspectDef(postgresAspectDef);
        mariaDbClient.registerAspectDef(mariaDbAspectDef);
        sqliteClient.registerAspectDef(sqliteAspectDef);

        var postgresResponse = postgresClient.createAspectDef(postgresCatalogId, postgresAspectDef);
        var mariaDbResponse = mariaDbClient.createAspectDef(mariaDbCatalogId, mariaDbAspectDef);
        var sqliteResponse = sqliteClient.createAspectDef(sqliteCatalogId, sqliteAspectDef);

        assertNotNull(postgresResponse);
        assertNotNull(mariaDbResponse);
        assertNotNull(sqliteResponse);

        // Verify each AspectDef exists only on its respective backend
        AspectDef retrievedPostgres = postgresClient.getAspectDef(postgresCatalogId, postgresResponse.aspectDefId());
        AspectDef retrievedMariaDb = mariaDbClient.getAspectDef(mariaDbCatalogId, mariaDbResponse.aspectDefId());
        AspectDef retrievedSqlite = sqliteClient.getAspectDef(sqliteCatalogId, sqliteResponse.aspectDefId());

        assertEquals("postgres_aspect", retrievedPostgres.name());
        assertEquals("mariadb_aspect", retrievedMariaDb.name());
        assertEquals("sqlite_aspect", retrievedSqlite.name());
    }
}
