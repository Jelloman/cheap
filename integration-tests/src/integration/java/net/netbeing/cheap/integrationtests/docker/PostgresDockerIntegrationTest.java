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
import org.junit.jupiter.api.BeforeEach;
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
 * Docker-based integration test for PostgreSQL backend.
 *
 * Architecture:
 * - Client test runs in JVM process (NOT in Docker)
 * - Server (cheap-rest) runs in Docker container
 * - PostgreSQL runs in Docker container
 * - Test ONLY uses CheapRestClient (NO database access)
 */
@Tag("docker")
@Tag("postgres")
class PostgresDockerIntegrationTest extends BaseClientIntegrationTest
{
    private static String baseUrl;
    private static DockerContainerManager containerManager;
    private static String cheapRestContainerId;
    private static CheapRestClient mainClient;

    private static final String NETWORK_NAME = "postgres-docker-test-network";

    @BeforeAll
    static void setupDockerEnvironment()
    {
        String postgresContainerId;
        containerManager = new DockerContainerManager(true);

        // Create network
        containerManager.createNetwork(NETWORK_NAME);

        // Start PostgreSQL container
        postgresContainerId = containerManager.container("postgres:17")
                .name("postgres-docker-test")
                .env("POSTGRES_DB", "cheap")
                .env("POSTGRES_USER", "cheap_user")
                .env("POSTGRES_PASSWORD", "test_password")
                .network(NETWORK_NAME)
                .exposePort(5432)
                .healthCheck("CMD-SHELL", "pg_isready -U cheap_user -d cheap")
                .start();

        // Wait for PostgreSQL to be ready
        DockerClient dockerClient = containerManager.getDockerClient();
        assertTrue(DockerTestUtils.waitForDatabaseReady(dockerClient, postgresContainerId, 60),
                "PostgreSQL container did not become ready in time");

        startContainer();
    }

    private static void startContainer()
    {
        int cheapRestPort;
        // Start cheap-rest container
        cheapRestContainerId = containerManager.container("cheap-rest:latest")
            .name("cheap-rest-postgres-docker-test")
            .env("SPRING_PROFILES_ACTIVE", "postgres")
            .env("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres-docker-test:5432/cheap")
            .env("SPRING_DATASOURCE_USERNAME", "cheap_user")
            .env("SPRING_DATASOURCE_PASSWORD", "test_password")
            .env("DB_PASSWORD", "test_password")
            .network(NETWORK_NAME)
            .exposePort(8080)
            .start();

        // Get dynamically mapped port
        DockerClient dockerClient = containerManager.getDockerClient();
        cheapRestPort = DockerTestUtils.getDynamicPort(dockerClient, cheapRestContainerId, 8080);
        assertTrue(cheapRestPort > 0, "Failed to get dynamic port for cheap-rest container");

        // Wait for cheap-rest to be ready
        baseUrl = "http://localhost:" + cheapRestPort;
        assertTrue(DockerTestUtils.waitForRestServiceReady(baseUrl, 120), "cheap-rest container did not become ready in time");

        mainClient = new CheapRestClientImpl(baseUrl);
    }

    @BeforeEach
    void setupClient()
    {
        setClient(mainClient);
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

        UUID productId1 = testUuid(3001);
        UUID productId2 = testUuid(3002);
        Entity entity1 = factory.createEntity(productId1);
        Entity entity2 = factory.createEntity(productId2);

        Map<UUID, Map<String, Object>> aspects = new LinkedHashMap<>();
        aspects.put(productId1, Map.of("sku", "PROD-001", "name", "Widget", "price", 19.99));
        aspects.put(productId2, Map.of("sku", "PROD-002", "name", "Gadget", "price", 29.99));

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
        assertEquals("PROD-001", product1.get("sku").read());
        assertEquals("Widget", product1.get("name").read());
        assertNotNull(product2);
        assertEquals("PROD-002", product2.get("sku").read());
        assertEquals("Gadget", product2.get("name").read());
    }

    @Test
    void customTableMapping()
    {
        Map<String, PropertyDef> addressProps = new LinkedHashMap<>();
        addressProps.put("street", factory.createPropertyDef("street", PropertyType.String));
        addressProps.put("city", factory.createPropertyDef("city", PropertyType.String));
        addressProps.put("zip", factory.createPropertyDef("zip", PropertyType.String));
        AspectDef addressAspectDef = factory.createImmutableAspectDef("address", addressProps);

        client.registerAspectDef(addressAspectDef);

        CatalogDef catalogDef = factory.createCatalogDef(Collections.emptyList(), List.of(addressAspectDef));
        CreateCatalogResponse catalogResponse = client.createCatalog(catalogDef, CatalogSpecies.SINK, null);
        UUID catalogId = catalogResponse.catalogId();

        UUID entityId1 = testUuid(4001);
        UUID entityId2 = testUuid(4002);

        Map<UUID, Map<String, Object>> addresses = new LinkedHashMap<>();
        addresses.put(entityId1, Map.of("street", "123 Main St", "city", "Springfield", "zip", "12345"));
        addresses.put(entityId2, Map.of("street", "456 Oak Ave", "city", "Portland", "zip", "67890"));

        UpsertAspectsResponse upsertResponse = client.upsertAspects(catalogId, "address", addresses);
        assertEquals(2, upsertResponse.successCount());

        AspectQueryResponse queryResponse = client.queryAspects(
                catalogId, Set.of(entityId1, entityId2), Set.of("address"));
        assertNotNull(queryResponse);
        assertEquals(1, queryResponse.results().size());

        AspectMap addressMap = queryResponse.results().getFirst();
        assertEquals(2, addressMap.size());
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

        UUID itemId = testUuid(5001);
        Map<UUID, Map<String, Object>> items = new LinkedHashMap<>();
        items.put(itemId, Map.of("name", "Test Item"));

        UpsertAspectsResponse upsertResponse = client.upsertAspects(catalogId, "item", items);
        assertEquals(1, upsertResponse.successCount());

        // Restart cheap-rest container
        containerManager.stopContainer(cheapRestContainerId);
        containerManager.removeContainer(cheapRestContainerId);

        startContainer();

        // Create new client pointing to restarted server
        client = new CheapRestClientImpl(baseUrl);

        // Re-register AspectDef in new client instance
        client.registerAspectDef(itemAspectDef);

        // Query data - should still exist
        AspectQueryResponse queryResponse = client.queryAspects(catalogId, Set.of(itemId), Set.of("item"));
        assertNotNull(queryResponse);
        assertEquals(1, queryResponse.results().size());

        AspectMap itemMap = queryResponse.results().getFirst();
        Aspect item = itemMap.get(factory.createEntity(itemId));
        assertNotNull(item);
        assertEquals("Test Item", item.get("name").read());
    }
}
