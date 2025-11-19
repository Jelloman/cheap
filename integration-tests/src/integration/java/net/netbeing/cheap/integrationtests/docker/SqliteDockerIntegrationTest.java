package net.netbeing.cheap.integrationtests.docker;

import com.github.dockerjava.api.DockerClient;
import net.netbeing.cheap.integrationtests.base.BaseClientIntegrationTest;
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Docker-based integration test for SQLite backend.
 *
 * Architecture:
 * - Client test runs in JVM process (NOT in Docker)
 * - Server (cheap-rest) runs in Docker container with embedded SQLite
 * - Test ONLY uses CheapRestClient (NO database access)
 */
@Tag("docker")
@Tag("sqlite")
class SqliteDockerIntegrationTest extends BaseClientIntegrationTest
{
    private static CheapRestClient client;
    private static DockerContainerManager containerManager;
    private static String cheapRestContainerId;
    private static int cheapRestPort;
    private static final String NETWORK_NAME = "sqlite-docker-test-network";
    private static final String SQLITE_VOLUME = System.getProperty("java.io.tmpdir") + "/cheap-sqlite-docker-test";

    @BeforeAll
    static void setupDockerEnvironment()
    {
        containerManager = new DockerContainerManager(true);
        DockerClient dockerClient = containerManager.getDockerClient();

        // Create network
        containerManager.createNetwork(NETWORK_NAME);

        // Start cheap-rest container with SQLite
        cheapRestContainerId = containerManager.container("cheap-rest:latest")
                .name("cheap-rest-sqlite-docker-test")
                .env("SPRING_PROFILES_ACTIVE", "sqlite")
                .env("CHEAP_DB_PATH", "/data/cheap.db")
                .network(NETWORK_NAME)
                .volume(SQLITE_VOLUME, "/data")
                .exposePort(8080)
                .start();

        // Get dynamically mapped port
        cheapRestPort = DockerTestUtils.getDynamicPort(dockerClient, cheapRestContainerId, 8080);
        assertTrue(cheapRestPort > 0, "Failed to get dynamic port for cheap-rest container");

        // Wait for cheap-rest to be ready
        String baseUrl = "http://localhost:" + cheapRestPort;
        assertTrue(DockerTestUtils.waitForRestServiceReady(baseUrl, 120),
                "cheap-rest container did not become ready in time");

        // Initialize client
        client = new CheapRestClientImpl(baseUrl);
    }

    @AfterAll
    static void teardownDockerEnvironment()
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
    void catalogLifecycle()
    {
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse createResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        assertNotNull(createResponse);
        assertNotNull(createResponse.catalogId());

        CatalogDef retrieved = client.getCatalogDef(createResponse.catalogId());
        assertNotNull(retrieved);
    }

    @Test
    void aspectDefCRUD()
    {
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        Map<String, PropertyDef> personProps = new LinkedHashMap<>();
        personProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        personProps.put("age", factory.createPropertyDef("age", PropertyType.Integer));
        AspectDef personAspectDef = factory.createImmutableAspectDef("person", personProps);

        client.registerAspectDef(personAspectDef);
        var response = client.createAspectDef(catalogId, personAspectDef);
        assertNotNull(response);
        assertNotNull(response.aspectDefId());

        AspectDef retrieved = client.getAspectDef(catalogId, response.aspectDefId());
        assertNotNull(retrieved);
        assertEquals("person", retrieved.name());
    }

    @Test
    void aspectUpsertAndQuery()
    {
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        Map<String, PropertyDef> productProps = new LinkedHashMap<>();
        productProps.put("sku", factory.createPropertyDef("sku", PropertyType.String));
        productProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        productProps.put("price", factory.createPropertyDef("price", PropertyType.Float));
        AspectDef productAspectDef = factory.createImmutableAspectDef("product", productProps);

        client.createAspectDef(catalogId, productAspectDef);
        client.registerAspectDef(productAspectDef);

        UUID productId1 = testUuid(10001);
        UUID productId2 = testUuid(10002);
        Entity entity1 = factory.createEntity(productId1);
        Entity entity2 = factory.createEntity(productId2);

        Map<UUID, Map<String, Object>> aspects = new LinkedHashMap<>();
        aspects.put(productId1, Map.of("sku", "SQLITE-001", "name", "SQLite Widget", "price", 14.99));
        aspects.put(productId2, Map.of("sku", "SQLITE-002", "name", "SQLite Gadget", "price", 19.99));

        UpsertAspectsResponse upsertResponse = client.upsertAspects(catalogId, "product", aspects);
        assertEquals(2, upsertResponse.successCount());

        Set<UUID> entityIds = Set.of(productId1, productId2);
        Set<String> aspectDefNames = Set.of("product");

        AspectQueryResponse queryResponse = client.queryAspects(catalogId, entityIds, aspectDefNames);
        assertNotNull(queryResponse);
        assertEquals(1, queryResponse.results().size());

        AspectMap aspectMap = queryResponse.results().getFirst();
        assertEquals("product", aspectMap.aspectDef().name());

        Aspect product1 = aspectMap.get(entity1);
        Aspect product2 = aspectMap.get(entity2);
        assertNotNull(product1);
        assertEquals("SQLITE-001", product1.get("sku").read());
        assertEquals("SQLite Widget", product1.get("name").read());
        assertNotNull(product2);
        assertEquals("SQLITE-002", product2.get("sku").read());
        assertEquals("SQLite Gadget", product2.get("name").read());
    }

    @Test
    void customTableMapping()
    {
        Map<String, PropertyDef> orderProps = new LinkedHashMap<>();
        orderProps.put("order_number", factory.createPropertyDef("order_number", PropertyType.String));
        orderProps.put("total_amount", factory.createPropertyDef("total_amount", PropertyType.Float));
        orderProps.put("status", factory.createPropertyDef("status", PropertyType.String));
        AspectDef orderAspectDef = factory.createImmutableAspectDef("order_item", orderProps);

        client.registerAspectDef(orderAspectDef);

        CatalogDef catalogDef = factory.createCatalogDef(Collections.emptyList(), List.of(orderAspectDef));
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        UUID entityId1 = testUuid(11001);
        UUID entityId2 = testUuid(11002);

        Map<UUID, Map<String, Object>> orders = new LinkedHashMap<>();
        orders.put(entityId1, Map.of("order_number", "ORD-001", "total_amount", 99.99, "status", "pending"));
        orders.put(entityId2, Map.of("order_number", "ORD-002", "total_amount", 149.99, "status", "shipped"));

        UpsertAspectsResponse upsertResponse = client.upsertAspects(catalogId, "order_item", orders);
        assertEquals(2, upsertResponse.successCount());

        AspectQueryResponse queryResponse = client.queryAspects(
                catalogId, Set.of(entityId1, entityId2), Set.of("order_item"));
        assertNotNull(queryResponse);
        assertEquals(1, queryResponse.results().size());

        AspectMap orderMap = queryResponse.results().getFirst();
        assertEquals(2, orderMap.size());
    }

    @Test
    void databaseFilePersistenceAcrossRestart()
    {
        // Create catalog and upsert data
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        Map<String, PropertyDef> itemProps = new LinkedHashMap<>();
        itemProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        AspectDef itemAspectDef = factory.createImmutableAspectDef("item", itemProps);

        client.createAspectDef(catalogId, itemAspectDef);
        client.registerAspectDef(itemAspectDef);

        UUID itemId = testUuid(12001);
        Map<UUID, Map<String, Object>> items = new LinkedHashMap<>();
        items.put(itemId, Map.of("name", "SQLite Persistent Item"));

        UpsertAspectsResponse upsertResponse = client.upsertAspects(catalogId, "item", items);
        assertEquals(1, upsertResponse.successCount());

        // Restart cheap-rest container (SQLite database file persists in volume)
        containerManager.stopContainer(cheapRestContainerId);
        containerManager.removeContainer(cheapRestContainerId);

        cheapRestContainerId = containerManager.container("cheap-rest:latest")
                .name("cheap-rest-sqlite-docker-test-restarted")
                .env("SPRING_PROFILES_ACTIVE", "sqlite")
                .env("CHEAP_DB_PATH", "/data/cheap.db")
                .network(NETWORK_NAME)
                .volume(SQLITE_VOLUME, "/data")
                .exposePort(8080)
                .start();

        cheapRestPort = DockerTestUtils.getDynamicPort(containerManager.getDockerClient(), cheapRestContainerId, 8080);
        String newBaseUrl = "http://localhost:" + cheapRestPort;
        assertTrue(DockerTestUtils.waitForRestServiceReady(newBaseUrl, 120));

        // Create new client pointing to restarted server
        client = new CheapRestClientImpl(newBaseUrl);

        // Re-register AspectDef in new client instance
        client.registerAspectDef(itemAspectDef);

        // Query data - should still exist because database file persisted
        AspectQueryResponse queryResponse = client.queryAspects(catalogId, Set.of(itemId), Set.of("item"));
        assertNotNull(queryResponse);
        assertEquals(1, queryResponse.results().size());

        AspectMap itemMap = queryResponse.results().getFirst();
        Aspect item = itemMap.get(factory.createEntity(itemId));
        assertNotNull(item);
        assertEquals("SQLite Persistent Item", item.get("name").read());
    }

    @Test
    void concurrentOperations()
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Create AspectDef
        Map<String, PropertyDef> dataProps = new LinkedHashMap<>();
        dataProps.put("value", factory.createPropertyDef("value", PropertyType.String));
        dataProps.put("timestamp", factory.createPropertyDef("timestamp", PropertyType.Integer));
        AspectDef dataAspectDef = factory.createImmutableAspectDef("data", dataProps);

        client.createAspectDef(catalogId, dataAspectDef);
        client.registerAspectDef(dataAspectDef);

        // Perform multiple upserts in sequence (simulating concurrent operations)
        for (int i = 0; i < 5; i++) {
            UUID entityId = testUuid(13000 + i);
            Map<UUID, Map<String, Object>> data = new LinkedHashMap<>();
            data.put(entityId, Map.of("value", "Data-" + i, "timestamp", i));

            UpsertAspectsResponse response = client.upsertAspects(catalogId, "data", data);
            assertEquals(1, response.successCount());
        }

        // Verify all data
        Set<UUID> allIds = Set.of(
                testUuid(13000), testUuid(13001), testUuid(13002),
                testUuid(13003), testUuid(13004)
        );

        AspectQueryResponse queryResponse = client.queryAspects(catalogId, allIds, Set.of("data"));
        assertNotNull(queryResponse);
        assertEquals(1, queryResponse.results().size());

        AspectMap dataMap = queryResponse.results().getFirst();
        assertEquals(5, dataMap.size());
    }
}
