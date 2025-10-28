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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP integration tests for HierarchyController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
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
}
