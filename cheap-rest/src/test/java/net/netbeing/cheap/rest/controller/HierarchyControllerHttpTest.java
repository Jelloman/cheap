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

package net.netbeing.cheap.rest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import net.netbeing.cheap.rest.TestStartEndLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP integration tests for HierarchyController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(TestStartEndLogger.class)
class HierarchyControllerHttpTest extends BaseControllerHttpTest
{
    private String catalogId;

    @BeforeEach
    void setupCatalog() throws Exception
    {
        // Create a catalog with various hierarchy types
        String createRequest = loadJson("hierarchy/create-catalog-with-hierarchies.json");
        String createResponse = webTestClient.post()
            .uri("/api/catalog")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        catalogId = objectMapper.readTree(createResponse).get("catalogId").asText();
    }

    @Test
    void testGetEntityListHierarchy() throws Exception
    {
        // Get the entity list hierarchy
        String responseJson = webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/hierarchies/people?page=0&size=20")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response structure
        JsonNode responseNode = objectMapper.readTree(responseJson);

        assertThat(responseNode.has("catalogId")).isTrue();
        assertThat(responseNode.has("hierarchyName")).isTrue();
        assertThat(responseNode.has("content")).isTrue();
        assertThat(responseNode.has("page")).isTrue();
        assertThat(responseNode.has("size")).isTrue();
        assertThat(responseNode.has("totalElements")).isTrue();
        assertThat(responseNode.has("totalPages")).isTrue();

        // Verify hierarchy name
        assertThat(responseNode.get("hierarchyName").asText()).isEqualTo("people");

        // Verify pagination
        assertThat(responseNode.get("page").asInt()).isZero();
        assertThat(responseNode.get("size").asInt()).isEqualTo(20);
    }

    @Test
    void testGetEntitySetHierarchy() throws Exception
    {
        // Get the entity set hierarchy
        String responseJson = webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/hierarchies/users?page=0&size=20")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response structure
        JsonNode responseNode = objectMapper.readTree(responseJson);

        assertThat(responseNode.has("catalogId")).isTrue();
        assertThat(responseNode.has("hierarchyName")).isTrue();
        assertThat(responseNode.has("content")).isTrue();
        assertThat(responseNode.has("page")).isTrue();
        assertThat(responseNode.has("size")).isTrue();
        assertThat(responseNode.has("totalElements")).isTrue();
        assertThat(responseNode.has("totalPages")).isTrue();

        // Verify hierarchy name
        assertThat(responseNode.get("hierarchyName").asText()).isEqualTo("users");
    }

    @Test
    void testGetEntityDirectoryHierarchy() throws Exception
    {
        // Get the entity directory hierarchy
        String responseJson = webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/hierarchies/documents?page=0&size=20")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response structure
        JsonNode responseNode = objectMapper.readTree(responseJson);

        assertThat(responseNode.has("catalogId")).isTrue();
        assertThat(responseNode.has("hierarchyName")).isTrue();
        assertThat(responseNode.has("content")).isTrue();
        assertThat(responseNode.has("page")).isTrue();
        assertThat(responseNode.has("size")).isTrue();
        assertThat(responseNode.has("totalElements")).isTrue();
        assertThat(responseNode.has("totalPages")).isTrue();

        // Verify hierarchy name
        assertThat(responseNode.get("hierarchyName").asText()).isEqualTo("documents");

        // Content should be a map for directory
        JsonNode content = responseNode.get("content");
        assertThat(content.isObject()).isTrue();
    }

    @Test
    void testGetEntityTreeHierarchy() throws Exception
    {
        // Get the entity tree hierarchy
        String responseJson = webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/hierarchies/categories")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response structure
        JsonNode responseNode = objectMapper.readTree(responseJson);

        assertThat(responseNode.has("catalogId")).isTrue();
        assertThat(responseNode.has("hierarchyName")).isTrue();
        assertThat(responseNode.has("root")).isTrue();

        // Verify hierarchy name
        assertThat(responseNode.get("hierarchyName").asText()).isEqualTo("categories");

        // Root should exist
        JsonNode root = responseNode.get("root");
        assertThat(root).isNotNull();
    }

    @Test
    void testGetHierarchyPagination() throws Exception
    {
        // Test pagination with different page sizes
        String response1 = webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/hierarchies/people?page=0&size=5")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        JsonNode node1 = objectMapper.readTree(response1);
        assertThat(node1.get("size").asInt()).isEqualTo(5);

        // Test second page
        String response2 = webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/hierarchies/people?page=1&size=5")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        JsonNode node2 = objectMapper.readTree(response2);
        assertThat(node2.get("page").asInt()).isEqualTo(1);
    }

    @Test
    void testGetNonExistentHierarchy()
    {
        webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/hierarchies/nonexistent")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void testGetHierarchyWithInvalidPageSize()
    {
        // Page size exceeds maximum (100)
        webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/hierarchies/people?page=0&size=200")
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void testGetHierarchyFromNonExistentCatalog()
    {
        String fakeId = "00000000-0000-0000-0000-000000000000";

        webTestClient.get()
            .uri("/api/catalog/" + fakeId + "/hierarchies/people")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void testCreateEntityListHierarchy() throws Exception
    {
        // Load the JSON request for creating an entity list hierarchy
        String createRequest = loadJson("hierarchy/create-hierarchy.json");

        // Create the hierarchy
        String responseJson = webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);
        assertThat(responseNode.has("hierarchyName")).isTrue();
        assertThat(responseNode.has("message")).isTrue();
        assertThat(responseNode.get("hierarchyName").asText()).isEqualTo("projects");
        assertThat(responseNode.get("message").asText()).isEqualTo("Hierarchy created successfully");

        // Verify we can retrieve the created hierarchy
        String getResponse = webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/hierarchies/projects?page=0&size=20")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        JsonNode getNode = objectMapper.readTree(getResponse);
        assertThat(getNode.get("hierarchyName").asText()).isEqualTo("projects");
    }

    @Test
    void testCreateEntitySetHierarchy() throws Exception
    {
        // Load the JSON request for creating an entity set hierarchy
        String createRequest = loadJson("hierarchy/create-hierarchy-set.json");

        // Create the hierarchy
        String responseJson = webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);
        assertThat(responseNode.get("hierarchyName").asText()).isEqualTo("users");
        assertThat(responseNode.get("message").asText()).isEqualTo("Hierarchy created successfully");

        // Verify we can retrieve the created hierarchy
        String getResponse = webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/hierarchies/users?page=0&size=20")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        JsonNode getNode = objectMapper.readTree(getResponse);
        assertThat(getNode.get("hierarchyName").asText()).isEqualTo("users");
    }

    @Test
    void testCreateEntityTreeHierarchy() throws Exception
    {
        // Load the JSON request for creating an entity tree hierarchy
        String createRequest = loadJson("hierarchy/create-hierarchy-tree.json");

        // Create the hierarchy
        String responseJson = webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);
        assertThat(responseNode.get("hierarchyName").asText()).isEqualTo("categories");
        assertThat(responseNode.get("message").asText()).isEqualTo("Hierarchy created successfully");

        // Verify we can retrieve the created hierarchy
        String getResponse = webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/hierarchies/categories")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        JsonNode getNode = objectMapper.readTree(getResponse);
        assertThat(getNode.get("hierarchyName").asText()).isEqualTo("categories");
    }

    @Test
    void testCreateEntityDirectoryHierarchy() throws Exception
    {
        // Load the JSON request for creating an entity directory hierarchy
        String createRequest = loadJson("hierarchy/create-hierarchy-directory.json");

        // Create the hierarchy
        String responseJson = webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);
        assertThat(responseNode.get("hierarchyName").asText()).isEqualTo("documents");
        assertThat(responseNode.get("message").asText()).isEqualTo("Hierarchy created successfully");

        // Verify we can retrieve the created hierarchy
        String getResponse = webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/hierarchies/documents?page=0&size=20")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        JsonNode getNode = objectMapper.readTree(getResponse);
        assertThat(getNode.get("hierarchyName").asText()).isEqualTo("documents");
    }

    // ========================================
    // Entity List/Set Mutation Operation Tests
    // ========================================

    @Test
    void testAddEntityIdsToEntityListHierarchy() throws Exception
    {
        // Load the JSON request
        String addRequest = loadJson("hierarchy/add-entity-ids-request.json");

        // Add entity IDs to the 'people' hierarchy (EntityList)
        String responseJson = webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies/people/entities")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(addRequest)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);
        assertThat(responseNode.get("catalogId").asText()).isEqualTo(catalogId);
        assertThat(responseNode.get("hierarchyName").asText()).isEqualTo("people");
        assertThat(responseNode.get("operation").asText()).isEqualTo("add");
        assertThat(responseNode.get("count").asInt()).isEqualTo(3);
        assertThat(responseNode.get("message").asText()).contains("Added 3 entity ID(s)");
    }

    @Test
    void testAddEntityIdsToEntitySetHierarchy() throws Exception
    {
        // Load the JSON request
        String addRequest = loadJson("hierarchy/add-entity-ids-request.json");

        // Add entity IDs to the 'users' hierarchy (EntitySet)
        String responseJson = webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies/users/entities")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(addRequest)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);
        assertThat(responseNode.get("count").asInt()).isEqualTo(3);
        assertThat(responseNode.get("operation").asText()).isEqualTo("add");
    }

    @Test
    void testAddEntityIdsToWrongHierarchyTypeReturns400() throws Exception
    {
        // Load the JSON request
        String addRequest = loadJson("hierarchy/add-entity-ids-request.json");

        // Try to add entity IDs to a directory (should fail)
        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies/documents/entities")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(addRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void testRemoveEntityIdsFromEntityListHierarchy() throws Exception
    {
        // First add some entities
        String addRequest = loadJson("hierarchy/add-entity-ids-request.json");
        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies/people/entities")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(addRequest)
            .exchange()
            .expectStatus().isOk();

        // Now remove one entity
        String removeRequest = loadJson("hierarchy/remove-entity-ids-request.json");
        String responseJson = webTestClient.method(HttpMethod.DELETE)
            .uri("/api/catalog/" + catalogId + "/hierarchies/people/entities")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(removeRequest)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);
        assertThat(responseNode.get("operation").asText()).isEqualTo("remove");
        assertThat(responseNode.get("count").asInt()).isEqualTo(1);
        assertThat(responseNode.get("message").asText()).contains("Removed 1 entity ID(s)");
    }

    @Test
    void testAddEntityIdsToNonexistentHierarchyReturns404() throws Exception
    {
        String addRequest = loadJson("hierarchy/add-entity-ids-request.json");

        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies/nonexistent/entities")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(addRequest)
            .exchange()
            .expectStatus().isNotFound();
    }

    // ========================================
    // Entity Directory Mutation Operation Tests
    // ========================================

    @Test
    void testAddDirectoryEntriesHierarchy() throws Exception
    {
        // Load the JSON request
        String addRequest = loadJson("hierarchy/add-directory-entries-request.json");

        // Add entries to the 'documents' hierarchy (EntityDirectory)
        String responseJson = webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies/documents/entries")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(addRequest)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);
        assertThat(responseNode.get("operation").asText()).isEqualTo("add");
        assertThat(responseNode.get("count").asInt()).isEqualTo(3);
        assertThat(responseNode.get("message").asText()).contains("Added 3 entry/entries");
    }

    @Test
    void testAddDirectoryEntriesToWrongHierarchyTypeReturns400() throws Exception
    {
        String addRequest = loadJson("hierarchy/add-directory-entries-request.json");

        // Try to add directory entries to a list (should fail)
        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies/people/entries")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(addRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void testRemoveDirectoryEntriesByNamesHierarchy() throws Exception
    {
        // First add some entries
        String addRequest = loadJson("hierarchy/add-directory-entries-request.json");
        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies/documents/entries")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(addRequest)
            .exchange()
            .expectStatus().isOk();

        // Now remove one entry by name
        String removeRequest = loadJson("hierarchy/remove-directory-entries-by-names-request.json");
        String responseJson = webTestClient.method(HttpMethod.DELETE)
            .uri("/api/catalog/" + catalogId + "/hierarchies/documents/entries")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(removeRequest)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);
        assertThat(responseNode.get("operation").asText()).isEqualTo("remove");
        assertThat(responseNode.get("count").asInt()).isEqualTo(1);
    }

    @Test
    void testRemoveDirectoryEntriesByIdsHierarchy() throws Exception
    {
        // First add some entries
        String addRequest = loadJson("hierarchy/add-directory-entries-request.json");
        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies/documents/entries")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(addRequest)
            .exchange()
            .expectStatus().isOk();

        // Now remove entries by IDs
        String removeRequest = loadJson("hierarchy/remove-directory-entries-by-ids-request.json");
        String responseJson = webTestClient.method(HttpMethod.DELETE)
            .uri("/api/catalog/" + catalogId + "/hierarchies/documents/entries")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(removeRequest)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);
        assertThat(responseNode.get("operation").asText()).isEqualTo("remove");
        assertThat(responseNode.get("count").asInt()).isEqualTo(2);
    }

    @Test
    void testRemoveDirectoryEntriesWithBothNamesAndIdsReturns400()
    {
        // Create a request with both names and IDs (invalid)
        String invalidRequest = """
        {
          "names": ["doc1"],
          "entityIds": ["550e8400-e29b-41d4-a716-446655440001"]
        }
        """;

        webTestClient.method(HttpMethod.DELETE)
            .uri("/api/catalog/" + catalogId + "/hierarchies/documents/entries")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void testRemoveDirectoryEntriesWithNeitherNamesNorIdsReturns400()
    {
        // Create a request with neither names nor IDs (invalid)
        String invalidRequest = """
        {
          "names": null,
          "entityIds": null
        }
        """;

        webTestClient.method(HttpMethod.DELETE)
            .uri("/api/catalog/" + catalogId + "/hierarchies/documents/entries")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }

    // ========================================
    // Entity Tree Mutation Operation Tests
    // ========================================

    @Test
    void testAddTreeNodesHierarchy() throws Exception
    {
        // Load the JSON request
        String addRequest = loadJson("hierarchy/add-tree-nodes-request.json");

        // Add nodes to the 'categories' hierarchy (EntityTree)
        String responseJson = webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies/categories/nodes")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(addRequest)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);
        assertThat(responseNode.get("operation").asText()).isEqualTo("add");
        assertThat(responseNode.get("nodesAffected").asInt()).isEqualTo(2);
        assertThat(responseNode.get("message").asText()).contains("Added 2 node(s)");
    }

    @Test
    void testAddTreeNodesToWrongHierarchyTypeReturns400() throws Exception
    {
        String addRequest = loadJson("hierarchy/add-tree-nodes-request.json");

        // Try to add tree nodes to a list (should fail)
        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies/people/nodes")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(addRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void testRemoveTreeNodesHierarchy() throws Exception
    {
        // First add some nodes
        String addRequest = loadJson("hierarchy/add-tree-nodes-request.json");
        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies/categories/nodes")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(addRequest)
            .exchange()
            .expectStatus().isOk();

        // Now remove one node
        String removeRequest = loadJson("hierarchy/remove-tree-nodes-request.json");
        String responseJson = webTestClient.method(HttpMethod.DELETE)
            .uri("/api/catalog/" + catalogId + "/hierarchies/categories/nodes")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(removeRequest)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);
        assertThat(responseNode.get("operation").asText()).isEqualTo("remove");
        assertThat(responseNode.get("nodesAffected").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(responseNode.get("message").asText()).contains("including descendants");
    }

    @Test
    void testAddTreeNodesToNonexistentParentReturns404()
    {
        // Create a request with a non-existent parent path
        String invalidRequest = """
        {
          "parentPath": "/nonexistent",
          "nodes": {
            "child": "750e8400-e29b-41d4-a716-446655440001"
          }
        }
        """;

        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/hierarchies/categories/nodes")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isNotFound();
    }
}
