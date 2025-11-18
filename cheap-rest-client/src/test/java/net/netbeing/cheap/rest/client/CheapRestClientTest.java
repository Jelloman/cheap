/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.rest.client;

import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.json.dto.*;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.rest.client.exception.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CheapRestClient using mocked HTTP responses.
 */
@SuppressWarnings("DataFlowIssue")
class CheapRestClientTest
{
    private MockWebServer mockWebServer;
    private CheapRestClient client;
    private CheapFactory factory;

    @BeforeEach
    void setUp() throws IOException
    {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("").toString();
        client = new CheapRestClientImpl(baseUrl);
        factory = new CheapFactory();
    }

    @AfterEach
    void tearDown() throws IOException
    {
        mockWebServer.shutdown();
    }

    // ========== Utility Methods ==========

    private @NotNull String loadTestResource(String path)
    {
        try {
            Path resourcePath = Path.of("src/test/resources/http-tests/" + path);
            return Files.readString(resourcePath);
        }
        catch (IOException e) {
            fail("Failed to load test resource: " + path, e);
            return null;
        }
    }

    // ========== Catalog Operation Tests ==========

    @Test
    @DisplayName("Should create catalog successfully")
    void testCreateCatalog() throws Exception
    {
        // Arrange
        String responseJson = loadTestResource("catalog/create-catalog-response.json");
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        CatalogDef catalogDef = factory.createCatalogDef();
        CatalogSpecies species = CatalogSpecies.SINK;

        // Act
        CreateCatalogResponse response = client.createCatalog(catalogDef, species, null);

        // Assert
        assertNotNull(response);
        assertNotNull(response.catalogId());
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), response.catalogId());
        assertEquals("Catalog created successfully", response.message());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/api/catalog", recordedRequest.getPath());
    }

    @Test
    @DisplayName("Should list catalogs with pagination")
    void testListCatalogs() throws Exception
    {
        // Arrange
        String responseJson = """
            {
              "content": [
                "550e8400-e29b-41d4-a716-446655440000",
                "550e8400-e29b-41d4-a716-446655440001"
              ],
              "page": 0,
              "size": 10,
              "totalElements": 2,
              "totalPages": 1
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        // Act
        CatalogListResponse response = client.listCatalogs(0, 10);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.content().size());
        assertEquals(0, response.page());
        assertEquals(10, response.size());
        assertEquals(2, response.totalElements());
        assertEquals(1, response.totalPages());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/api/catalog"));
        assertTrue(recordedRequest.getPath().contains("page=0"));
        assertTrue(recordedRequest.getPath().contains("size=10"));
    }

    @Test
    @DisplayName("Should get catalog by ID")
    void testGetCatalogDef() throws Exception
    {
        // Arrange
        String responseJson = loadTestResource("catalog/get-catalog-response.json");
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        // Act
        CatalogDef catalogDef = client.getCatalogDef(catalogId);

        // Assert
        assertNotNull(catalogDef);
        assertNotNull(catalogDef.hierarchyDefs());
        assertTrue(catalogDef.hierarchyDefs().iterator().hasNext());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/api/catalog/" + catalogId));
    }

    @Test
    @DisplayName("Should throw NotFoundException when catalog not found")
    void testGetCatalogDefNotFound()
    {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"message\":\"Catalog not found\"}"));

        UUID catalogId = UUID.randomUUID();

        // Act & Assert
        assertThrows(CheapRestNotFoundException.class, () -> client.getCatalogDef(catalogId));
    }

    // ========== AspectDef Operation Tests ==========

    @Test
    @DisplayName("Should create aspect definition successfully")
    void testCreateAspectDef() throws Exception
    {
        // Arrange
        String responseJson = """
            {
              "aspectDefId": "770e8400-e29b-41d4-a716-446655440001",
              "message": "AspectDef created successfully"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.randomUUID();
        PropertyDef nameProp = factory.createPropertyDef("name", PropertyType.String);
        PropertyDef ageProp = factory.createPropertyDef("age", PropertyType.Integer);

        AspectDef aspectDef = factory.createImmutableAspectDef(
            "person",
            UUID.randomUUID(),
            Map.of("name", nameProp, "age", ageProp)
        );

        // Act
        CreateAspectDefResponse response = client.createAspectDef(catalogId, aspectDef);

        // Assert
        assertNotNull(response);
        assertNotNull(response.aspectDefId());
        assertEquals("AspectDef created successfully", response.message());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/api/catalog/" + catalogId + "/aspect-defs"));
    }

    @Test
    @DisplayName("Should list aspect definitions with pagination")
    void testListAspectDefs() throws Exception
    {
        // Arrange - use simpler JSON that doesn't require full deserialization
        String responseJson = """
            {
              "catalogId": "550e8400-e29b-41d4-a716-446655440000",
              "content": [],
              "page": 0,
              "size": 10,
              "totalElements": 0,
              "totalPages": 0
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.randomUUID();

        // Act
        AspectDefListResponse response = client.listAspectDefs(catalogId, 0, 10);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.content().size());
        assertEquals(0, response.page());
        assertEquals(10, response.size());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("page=0"));
        assertTrue(recordedRequest.getPath().contains("size=10"));
    }

    @Test
    @DisplayName("Should get aspect definition by ID")
    void testGetAspectDef() throws Exception
    {
        // Arrange
        String responseJson = loadTestResource("aspectdef/create-aspectdef-request.json");
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.randomUUID();
        UUID aspectDefId = UUID.fromString("770e8400-e29b-41d4-a716-446655440001");

        // Act
        AspectDef aspectDef = client.getAspectDef(catalogId, aspectDefId);

        // Assert
        assertNotNull(aspectDef);
        assertEquals("com.example.ProductAspect", aspectDef.name());
        assertNotNull(aspectDef.propertyDefs());
        assertFalse(aspectDef.propertyDefs().isEmpty());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/aspect-defs/" + aspectDefId));
    }

    @Test
    @DisplayName("Should get aspect definition by name")
    void testGetAspectDefByName() throws Exception
    {
        // Arrange
        String responseJson = loadTestResource("aspectdef/create-aspectdef-request.json");
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.randomUUID();
        String aspectDefName = "com.example.ProductAspect";

        // Act
        AspectDef aspectDef = client.getAspectDefByName(catalogId, aspectDefName);

        // Assert
        assertNotNull(aspectDef);
        assertEquals(aspectDefName, aspectDef.name());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/aspect-defs/" + aspectDefName));
    }

    // ========== Aspect Operation Tests ==========

    @Test
    @DisplayName("Should upsert aspects successfully")
    void testUpsertAspects() throws Exception
    {
        // Arrange
        String responseJson = loadTestResource("aspect/upsert-aspects-response-success.json");
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.randomUUID();
        String aspectDefName = "person";
        UUID entityId = UUID.randomUUID();

        Map<UUID, Map<String, Object>> aspects = Map.of(
            entityId, Map.of(
                "firstName", "John",
                "lastName", "Doe",
                "age", 30
            )
        );

        // Act
        UpsertAspectsResponse response = client.upsertAspects(catalogId, aspectDefName, aspects);

        // Assert
        assertNotNull(response);
        assertNotNull(response.results());
        assertFalse(response.results().isEmpty());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/aspects/" + aspectDefName));
    }

    @Test
    @DisplayName("Should query aspects successfully")
    void testQueryAspects() throws Exception
    {
        // Arrange - simplified response structure
        String responseJson = """
            {
              "catalogId": "550e8400-e29b-41d4-a716-446655440000",
              "results": []
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID entityId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Set<UUID> entityIds = Set.of(entityId);
        Set<String> aspectDefNames = Set.of("person");

        // Act
        AspectQueryResponse response = client.queryAspects(catalogId, entityIds, aspectDefNames);

        // Assert
        assertNotNull(response);
        assertNotNull(response.results());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/aspects/query"));
    }

    // ========== Hierarchy Operation Tests ==========

    @Test
    @DisplayName("Should get entity list successfully")
    void testGetEntityList() throws Exception
    {
        // Arrange
        String responseJson = """
            {
              "catalogId": "550e8400-e29b-41d4-a716-446655440000",
              "hierarchyName": "people",
              "content": [
                "550e8400-e29b-41d4-a716-446655440000",
                "550e8400-e29b-41d4-a716-446655440001"
              ],
              "page": 0,
              "size": 10,
              "totalElements": 2,
              "totalPages": 1
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.randomUUID();
        String hierarchyName = "people";

        // Act
        EntityListResponse response = client.getEntityList(catalogId, hierarchyName, 0, 10);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.content().size());
        assertEquals(0, response.page());
        assertEquals(10, response.size());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/hierarchies/" + hierarchyName));
        assertTrue(recordedRequest.getPath().contains("page=0"));
    }

    @Test
    @DisplayName("Should get entity directory successfully")
    void testGetEntityDirectory() throws Exception
    {
        // Arrange
        String responseJson = """
            {
              "catalogId": "550e8400-e29b-41d4-a716-446655440000",
              "hierarchyName": "people_directory",
              "content": {
                "alice": "550e8400-e29b-41d4-a716-446655440000",
                "bob": "550e8400-e29b-41d4-a716-446655440001"
              },
              "page": 0,
              "size": 10,
              "totalElements": 2,
              "totalPages": 1
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.randomUUID();
        String hierarchyName = "people_directory";

        // Act
        EntityDirectoryResponse response = client.getEntityDirectory(catalogId, hierarchyName);

        // Assert
        assertNotNull(response);
        assertNotNull(response.content());
        assertEquals(2, response.content().size());
        assertTrue(response.content().containsKey("alice"));

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/hierarchies/" + hierarchyName));
    }

    @Test
    @DisplayName("Should get entity tree successfully")
    void testGetEntityTree() throws Exception
    {
        // Arrange
        String responseJson = """
            {
              "catalogId": "550e8400-e29b-41d4-a716-446655440000",
              "hierarchyName": "org_tree",
              "root": {
                "entity": "550e8400-e29b-41d4-a716-446655440000",
                "children": []
              }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.randomUUID();
        String hierarchyName = "org_tree";

        // Act
        EntityTreeResponse response = client.getEntityTree(catalogId, hierarchyName);

        // Assert
        assertNotNull(response);
        assertNotNull(response.root());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/hierarchies/" + hierarchyName));
    }

    @Test
    @DisplayName("Should get aspect map successfully")
    void testGetAspectMap() throws Exception
    {
        // Arrange
        String responseJson = """
            {
              "catalogId": "550e8400-e29b-41d4-a716-446655440000",
              "hierarchyName": "aspect_registry",
              "aspectDefName": "person",
              "content": {},
              "page": 0,
              "size": 10,
              "totalElements": 0,
              "totalPages": 0
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.randomUUID();
        String hierarchyName = "aspect_registry";

        // Act
        AspectMapResponse response = client.getAspectMap(catalogId, hierarchyName, 0, 10);

        // Assert
        assertNotNull(response);
        assertNotNull(response.content());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/hierarchies/" + hierarchyName));
        assertTrue(recordedRequest.getPath().contains("page=0"));
    }

    // ========== Error Handling Tests ==========

    @Test
    @DisplayName("Should throw BadRequestException for 400 errors")
    void testBadRequestException()
    {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"message\":\"Invalid request\"}"));

        // Act & Assert
        assertThrows(CheapRestBadRequestException.class,
            () -> client.listCatalogs(0, -1));
    }

    @Test
    @DisplayName("Should throw NotFoundException for 404 errors")
    void testNotFoundException()
    {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"message\":\"Not found\"}"));

        UUID catalogId = UUID.randomUUID();
        UUID aspectDefId = UUID.randomUUID();

        // Act & Assert
        assertThrows(CheapRestNotFoundException.class,
            () -> client.getAspectDef(catalogId, aspectDefId));
    }

    @Test
    @DisplayName("Should throw ServerException for 500 errors")
    void testServerException()
    {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"message\":\"Internal server error\"}"));

        UUID catalogId = UUID.randomUUID();

        // Act & Assert
        assertThrows(CheapRestServerException.class,
            () -> client.getCatalogDef(catalogId));
    }

    @Test
    @DisplayName("Should throw ClientException for other HTTP errors")
    void testGenericClientException()
    {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(403)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"message\":\"Forbidden\"}"));

        UUID catalogId = UUID.randomUUID();

        // Act & Assert
        assertThrows(CheapRestClientException.class,
            () -> client.getCatalogDef(catalogId));
    }

    // ========== Hierarchy Mutation Tests ==========

    // Entity List/Set Operations

    @Test
    @DisplayName("Should add entity IDs to list successfully")
    void testAddEntityIds() throws Exception
    {
        // Arrange
        String responseJson = """
            {
              "catalogId": "550e8400-e29b-41d4-a716-446655440000",
              "hierarchyName": "test-list",
              "operation": "add",
              "count": 3,
              "message": "Successfully added 3 entity IDs"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String hierarchyName = "test-list";
        List<UUID> entityIds = List.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );

        // Act
        EntityIdsOperationResponse response = client.addEntityIds(catalogId, hierarchyName, entityIds);

        // Assert
        assertNotNull(response);
        assertEquals(catalogId, response.catalogId());
        assertEquals(hierarchyName, response.hierarchyName());
        assertEquals("add", response.operation());
        assertEquals(3, response.count());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/entities"));
        assertTrue(recordedRequest.getPath().contains(hierarchyName));
    }

    @Test
    @DisplayName("Should remove entity IDs from list successfully")
    void testRemoveEntityIds() throws Exception
    {
        // Arrange
        String responseJson = """
            {
              "catalogId": "550e8400-e29b-41d4-a716-446655440000",
              "hierarchyName": "test-list",
              "operation": "remove",
              "count": 2,
              "message": "Successfully removed 2 entity IDs"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String hierarchyName = "test-list";
        List<UUID> entityIds = List.of(
            UUID.randomUUID(),
            UUID.randomUUID()
        );

        // Act
        EntityIdsOperationResponse response = client.removeEntityIds(catalogId, hierarchyName, entityIds);

        // Assert
        assertNotNull(response);
        assertEquals(catalogId, response.catalogId());
        assertEquals("remove", response.operation());
        assertEquals(2, response.count());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("DELETE", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/entities"));
    }

    @Test
    @DisplayName("Should throw NotFoundException when hierarchy not found for add entity IDs")
    void testAddEntityIds_HierarchyNotFound()
    {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"message\":\"Hierarchy not found\"}"));

        UUID catalogId = UUID.randomUUID();
        String hierarchyName = "nonexistent";
        List<UUID> entityIds = List.of(UUID.randomUUID());

        // Act & Assert
        assertThrows(CheapRestNotFoundException.class,
            () -> client.addEntityIds(catalogId, hierarchyName, entityIds));
    }

    @Test
    @DisplayName("Should throw BadRequestException for wrong hierarchy type")
    void testAddEntityIds_WrongHierarchyType()
    {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"message\":\"Wrong hierarchy type\"}"));

        UUID catalogId = UUID.randomUUID();
        String hierarchyName = "directory-hierarchy";
        List<UUID> entityIds = List.of(UUID.randomUUID());

        // Act & Assert
        assertThrows(CheapRestBadRequestException.class,
            () -> client.addEntityIds(catalogId, hierarchyName, entityIds));
    }

    // Entity Directory Operations

    @Test
    @DisplayName("Should add directory entries successfully")
    void testAddDirectoryEntries() throws Exception
    {
        // Arrange
        String responseJson = """
            {
              "catalogId": "550e8400-e29b-41d4-a716-446655440000",
              "hierarchyName": "test-directory",
              "operation": "add",
              "count": 2,
              "message": "Successfully added 2 directory entries"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String hierarchyName = "test-directory";
        Map<String, UUID> entries = Map.of(
            "alice", UUID.randomUUID(),
            "bob", UUID.randomUUID()
        );

        // Act
        DirectoryOperationResponse response = client.addDirectoryEntries(catalogId, hierarchyName, entries);

        // Assert
        assertNotNull(response);
        assertEquals(catalogId, response.catalogId());
        assertEquals(hierarchyName, response.hierarchyName());
        assertEquals("add", response.operation());
        assertEquals(2, response.count());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/entries"));
    }

    @Test
    @DisplayName("Should remove directory entries by names successfully")
    void testRemoveDirectoryEntriesByNames() throws Exception
    {
        // Arrange
        String responseJson = """
            {
              "catalogId": "550e8400-e29b-41d4-a716-446655440000",
              "hierarchyName": "test-directory",
              "operation": "remove",
              "count": 2,
              "message": "Successfully removed 2 directory entries by names"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String hierarchyName = "test-directory";
        List<String> names = List.of("alice", "bob");

        // Act
        DirectoryOperationResponse response = client.removeDirectoryEntriesByNames(
            catalogId, hierarchyName, names);

        // Assert
        assertNotNull(response);
        assertEquals(catalogId, response.catalogId());
        assertEquals("remove", response.operation());
        assertEquals(2, response.count());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("DELETE", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/entries"));
    }

    @Test
    @DisplayName("Should remove directory entries by entity IDs successfully")
    void testRemoveDirectoryEntriesByEntityIds() throws Exception
    {
        // Arrange
        String responseJson = """
            {
              "catalogId": "550e8400-e29b-41d4-a716-446655440000",
              "hierarchyName": "test-directory",
              "operation": "remove",
              "count": 3,
              "message": "Successfully removed 3 directory entries by entity IDs"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String hierarchyName = "test-directory";
        List<UUID> entityIds = List.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );

        // Act
        DirectoryOperationResponse response = client.removeDirectoryEntriesByEntityIds(
            catalogId, hierarchyName, entityIds);

        // Assert
        assertNotNull(response);
        assertEquals(catalogId, response.catalogId());
        assertEquals(3, response.count());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("DELETE", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/entries"));
    }

    @Test
    @DisplayName("Should throw BadRequestException when removing directory entries with both names and IDs")
    void testRemoveDirectoryEntries_BothNamesAndIds()
    {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"message\":\"Cannot specify both names and entity IDs\"}"));

        UUID catalogId = UUID.randomUUID();
        String hierarchyName = "test-directory";
        List<String> names = List.of("alice");

        // Act & Assert
        assertThrows(CheapRestBadRequestException.class,
            () -> client.removeDirectoryEntriesByNames(catalogId, hierarchyName, names));
    }

    // Entity Tree Operations

    @Test
    @DisplayName("Should add tree nodes at root level successfully")
    void testAddTreeNodes_RootLevel() throws Exception
    {
        // Arrange
        String responseJson = """
            {
              "catalogId": "550e8400-e29b-41d4-a716-446655440000",
              "hierarchyName": "test-tree",
              "operation": "add",
              "nodesAffected": 2,
              "message": "Successfully added 2 nodes"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String hierarchyName = "test-tree";
        Map<String, UUID> nodes = Map.of(
            "root1", UUID.randomUUID(),
            "root2", UUID.randomUUID()
        );

        // Act
        TreeOperationResponse response = client.addTreeNodes(
            catalogId, hierarchyName, null, nodes);

        // Assert
        assertNotNull(response);
        assertEquals(catalogId, response.catalogId());
        assertEquals(hierarchyName, response.hierarchyName());
        assertEquals("add", response.operation());
        assertEquals(2, response.nodesAffected());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/nodes"));
    }

    @Test
    @DisplayName("Should add tree nodes under parent path successfully")
    void testAddTreeNodes_WithParent() throws Exception
    {
        // Arrange
        String responseJson = """
            {
              "catalogId": "550e8400-e29b-41d4-a716-446655440000",
              "hierarchyName": "test-tree",
              "operation": "add",
              "nodesAffected": 3,
              "message": "Successfully added 3 nodes under /root/folder1"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String hierarchyName = "test-tree";
        String parentPath = "/root/folder1";
        Map<String, UUID> nodes = Map.of(
            "child1", UUID.randomUUID(),
            "child2", UUID.randomUUID(),
            "child3", UUID.randomUUID()
        );

        // Act
        TreeOperationResponse response = client.addTreeNodes(
            catalogId, hierarchyName, parentPath, nodes);

        // Assert
        assertNotNull(response);
        assertEquals(3, response.nodesAffected());
        assertTrue(response.message().contains(parentPath));

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
    }

    @Test
    @DisplayName("Should remove tree nodes successfully")
    void testRemoveTreeNodes() throws Exception
    {
        // Arrange
        String responseJson = """
            {
              "catalogId": "550e8400-e29b-41d4-a716-446655440000",
              "hierarchyName": "test-tree",
              "operation": "remove",
              "nodesAffected": 5,
              "message": "Successfully removed 2 nodes and 3 descendants"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        UUID catalogId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String hierarchyName = "test-tree";
        List<String> paths = List.of(
            "/root/folder1",
            "/root/folder2"
        );

        // Act
        TreeOperationResponse response = client.removeTreeNodes(
            catalogId, hierarchyName, paths);

        // Assert
        assertNotNull(response);
        assertEquals(catalogId, response.catalogId());
        assertEquals("remove", response.operation());
        assertEquals(5, response.nodesAffected());

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("DELETE", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/nodes"));
    }

    @Test
    @DisplayName("Should throw NotFoundException when parent node not found")
    void testAddTreeNodes_ParentNotFound()
    {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"message\":\"Parent node not found\"}"));

        UUID catalogId = UUID.randomUUID();
        String hierarchyName = "test-tree";
        String parentPath = "/nonexistent/path";
        Map<String, UUID> nodes = Map.of("child", UUID.randomUUID());

        // Act & Assert
        assertThrows(CheapRestNotFoundException.class,
            () -> client.addTreeNodes(catalogId, hierarchyName, parentPath, nodes));
    }

    @Test
    @DisplayName("Should throw ServerException for 500 errors on mutation operations")
    void testMutationOperation_ServerError()
    {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"message\":\"Internal server error\"}"));

        UUID catalogId = UUID.randomUUID();
        String hierarchyName = "test-list";
        List<UUID> entityIds = List.of(UUID.randomUUID());

        // Act & Assert
        assertThrows(CheapRestServerException.class,
            () -> client.addEntityIds(catalogId, hierarchyName, entityIds));
    }
}
