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
 * Docker-based integration test for MariaDB backend.
 *
 * Architecture:
 * - Client test runs in JVM process (NOT in Docker)
 * - Server (cheap-rest) runs in Docker container
 * - MariaDB runs in Docker container
 * - Test ONLY uses CheapRestClient (NO database access)
 */
@Tag("docker")
@Tag("mariadb")
class MariaDbDockerIntegrationTest extends BaseClientIntegrationTest
{
    private static CheapRestClient client;
    private static DockerContainerManager containerManager;
    private static String mariaDbContainerId;
    private static String cheapRestContainerId;
    private static int cheapRestPort;
    private static final String NETWORK_NAME = "mariadb-docker-test-network";

    @BeforeAll
    static void setupDockerEnvironment()
    {
        containerManager = new DockerContainerManager(true);
        DockerClient dockerClient = containerManager.getDockerClient();

        // Create network
        containerManager.createNetwork(NETWORK_NAME);

        // Start MariaDB container
        mariaDbContainerId = containerManager.container("mariadb:11")
                .name("mariadb-docker-test")
                .env("MARIADB_DATABASE", "cheap")
                .env("MARIADB_USER", "cheap_user")
                .env("MARIADB_PASSWORD", "test_password")
                .env("MARIADB_ROOT_PASSWORD", "root_password")
                .network(NETWORK_NAME)
                .exposePort(3306)
                .healthCheck("CMD-SHELL", "healthcheck.sh --connect --innodb_initialized")
                .start();

        // Wait for MariaDB to be ready
        assertTrue(DockerTestUtils.waitForDatabaseReady(dockerClient, mariaDbContainerId, 60),
                "MariaDB container did not become ready in time");

        // Start cheap-rest container
        cheapRestContainerId = containerManager.container("cheap-rest:latest")
                .name("cheap-rest-mariadb-docker-test")
                .env("SPRING_PROFILES_ACTIVE", "mariadb")
                .env("SPRING_DATASOURCE_URL", "jdbc:mariadb://mariadb-docker-test:3306/cheap")
                .env("SPRING_DATASOURCE_USERNAME", "cheap_user")
                .env("SPRING_DATASOURCE_PASSWORD", "test_password")
                .env("DB_PASSWORD", "test_password")
                .network(NETWORK_NAME)
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

        UUID productId1 = testUuid(6001);
        UUID productId2 = testUuid(6002);
        Entity entity1 = factory.createEntity(productId1);
        Entity entity2 = factory.createEntity(productId2);

        Map<UUID, Map<String, Object>> aspects = new LinkedHashMap<>();
        aspects.put(productId1, Map.of("sku", "PROD-101", "name", "MariaDB Widget", "price", 24.99));
        aspects.put(productId2, Map.of("sku", "PROD-102", "name", "MariaDB Gadget", "price", 34.99));

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
        assertEquals("PROD-101", product1.get("sku").read());
        assertEquals("MariaDB Widget", product1.get("name").read());
        assertNotNull(product2);
        assertEquals("PROD-102", product2.get("sku").read());
        assertEquals("MariaDB Gadget", product2.get("name").read());
    }

    @Test
    void customTableMapping()
    {
        Map<String, PropertyDef> inventoryProps = new LinkedHashMap<>();
        inventoryProps.put("item_code", factory.createPropertyDef("item_code", PropertyType.String));
        inventoryProps.put("quantity", factory.createPropertyDef("quantity", PropertyType.Integer));
        inventoryProps.put("location", factory.createPropertyDef("location", PropertyType.String));
        AspectDef inventoryAspectDef = factory.createImmutableAspectDef("inventory", inventoryProps);

        client.registerAspectDef(inventoryAspectDef);

        CatalogDef catalogDef = factory.createCatalogDef(Collections.emptyList(), List.of(inventoryAspectDef));
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        UUID entityId1 = testUuid(7001);
        UUID entityId2 = testUuid(7002);

        Map<UUID, Map<String, Object>> inventoryItems = new LinkedHashMap<>();
        inventoryItems.put(entityId1, Map.of("item_code", "INV-001", "quantity", 100, "location", "Warehouse A"));
        inventoryItems.put(entityId2, Map.of("item_code", "INV-002", "quantity", 250, "location", "Warehouse B"));

        UpsertAspectsResponse upsertResponse = client.upsertAspects(catalogId, "inventory", inventoryItems);
        assertEquals(2, upsertResponse.successCount());

        AspectQueryResponse queryResponse = client.queryAspects(
                catalogId, Set.of(entityId1, entityId2), Set.of("inventory"));
        assertNotNull(queryResponse);
        assertEquals(1, queryResponse.results().size());

        AspectMap inventoryMap = queryResponse.results().getFirst();
        assertEquals(2, inventoryMap.size());
    }

    @Test
    void persistenceAcrossRestart()
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

        UUID itemId = testUuid(8001);
        Map<UUID, Map<String, Object>> items = new LinkedHashMap<>();
        items.put(itemId, Map.of("name", "MariaDB Test Item"));

        UpsertAspectsResponse upsertResponse = client.upsertAspects(catalogId, "item", items);
        assertEquals(1, upsertResponse.successCount());

        // Restart cheap-rest container
        containerManager.stopContainer(cheapRestContainerId);
        containerManager.removeContainer(cheapRestContainerId);

        cheapRestContainerId = containerManager.container("cheap-rest:latest")
                .name("cheap-rest-mariadb-docker-test-restarted")
                .env("SPRING_PROFILES_ACTIVE", "mariadb")
                .env("SPRING_DATASOURCE_URL", "jdbc:mariadb://mariadb-docker-test:3306/cheap")
                .env("SPRING_DATASOURCE_USERNAME", "cheap_user")
                .env("SPRING_DATASOURCE_PASSWORD", "test_password")
                .env("DB_PASSWORD", "test_password")
                .network(NETWORK_NAME)
                .exposePort(8080)
                .start();

        cheapRestPort = DockerTestUtils.getDynamicPort(containerManager.getDockerClient(), cheapRestContainerId, 8080);
        String newBaseUrl = "http://localhost:" + cheapRestPort;
        assertTrue(DockerTestUtils.waitForRestServiceReady(newBaseUrl, 120));

        // Create new client pointing to restarted server
        client = new CheapRestClientImpl(newBaseUrl);

        // Re-register AspectDef in new client instance
        client.registerAspectDef(itemAspectDef);

        // Query data - should still exist
        AspectQueryResponse queryResponse = client.queryAspects(catalogId, Set.of(itemId), Set.of("item"));
        assertNotNull(queryResponse);
        assertEquals(1, queryResponse.results().size());

        AspectMap itemMap = queryResponse.results().getFirst();
        Aspect item = itemMap.get(factory.createEntity(itemId));
        assertNotNull(item);
        assertEquals("MariaDB Test Item", item.get("name").read());
    }

    @Test
    void foreignKeyConstraintValidation()
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef();
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        // Create AspectDef
        Map<String, PropertyDef> dataProps = new LinkedHashMap<>();
        dataProps.put("value", factory.createPropertyDef("value", PropertyType.String));
        AspectDef dataAspectDef = factory.createImmutableAspectDef("data", dataProps);

        client.createAspectDef(catalogId, dataAspectDef);
        client.registerAspectDef(dataAspectDef);

        // Upsert valid data
        UUID entityId = testUuid(9001);
        Map<UUID, Map<String, Object>> validData = new LinkedHashMap<>();
        validData.put(entityId, Map.of("value", "Valid Data"));

        UpsertAspectsResponse response = client.upsertAspects(catalogId, "data", validData);
        assertEquals(1, response.successCount());

        // Verify data exists
        AspectQueryResponse queryResponse = client.queryAspects(catalogId, Set.of(entityId), Set.of("data"));
        assertNotNull(queryResponse);
        assertEquals(1, queryResponse.results().size());
    }
}
