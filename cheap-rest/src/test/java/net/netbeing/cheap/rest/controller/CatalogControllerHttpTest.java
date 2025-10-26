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
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP integration tests for CatalogController using JSON files.
 */
class CatalogControllerHttpTest extends BaseControllerHttpTest
{
    @Test
    void testCreateCatalog() throws Exception
    {
        // Load request JSON
        String requestJson = loadJson("catalog/create-catalog-request.json");

        // Send POST request and verify response
        String responseJson = webTestClient.post()
            .uri("/api/catalogs")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Load expected response template
        String expectedTemplate = loadJson("catalog/create-catalog-response.json");

        // Parse and verify structure (ignoring dynamic fields like catalogId)
        JsonNode actualNode = objectMapper.readTree(responseJson);
        JsonNode expectedNode = objectMapper.readTree(expectedTemplate);

        // Verify static fields
        assertThat(actualNode.get("message").asText())
            .isEqualTo(expectedNode.get("message").asText());

        // Verify dynamic fields exist and have correct format
        assertThat(actualNode.has("catalogId")).isTrue();
        assertThat(actualNode.get("catalogId").asText()).matches(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        );
    }

    @Test
    void testListCatalogs() throws Exception
    {
        // First create a catalog
        String createRequest = loadJson("catalog/create-catalog-request.json");
        webTestClient.post()
            .uri("/api/catalogs")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated();

        // Then list catalogs
        String responseJson = webTestClient.get()
            .uri("/api/catalogs?page=0&size=20")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response structure
        JsonNode responseNode = objectMapper.readTree(responseJson);

        assertThat(responseNode.has("content")).isTrue();
        assertThat(responseNode.has("page")).isTrue();
        assertThat(responseNode.has("size")).isTrue();
        assertThat(responseNode.has("totalElements")).isTrue();
        assertThat(responseNode.has("totalPages")).isTrue();

        // Verify pagination values
        assertThat(responseNode.get("page").asInt()).isEqualTo(0);
        assertThat(responseNode.get("size").asInt()).isEqualTo(20);
        assertThat(responseNode.get("totalElements").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testGetCatalog() throws Exception
    {
        // Create a catalog
        String createRequest = loadJson("catalog/create-catalog-request.json");
        String createResponse = webTestClient.post()
            .uri("/api/catalogs")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Extract catalog ID
        String catalogId = objectMapper.readTree(createResponse).get("catalogId").asText();

        // Get the catalog
        // Note: WebFlux treats CatalogDef as streaming due to its Iterable methods
        String responseJson = webTestClient.get()
            .uri("/api/catalogs/" + catalogId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Load expected response and compare structure
        String expectedJson = loadJson("catalog/get-catalog-response.json");

        JsonNode actualNode = objectMapper.readTree(responseJson);
        JsonNode expectedNode = objectMapper.readTree(expectedJson);

        // Verify structure matches (compare hierarchyDefs and aspectDefs arrays)
        assertThat(actualNode.has("hierarchyDefs")).isTrue();
        assertThat(actualNode.has("aspectDefs")).isTrue();

        // Verify at least one hierarchy def
        assertThat(actualNode.get("hierarchyDefs").isArray()).isTrue();
        assertThat(actualNode.get("hierarchyDefs").size()).isGreaterThanOrEqualTo(1);

        // Verify at least one aspect def
        assertThat(actualNode.get("aspectDefs").isArray()).isTrue();
        assertThat(actualNode.get("aspectDefs").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testCreateCatalogWithInvalidData() throws Exception
    {
        // Load invalid request
        String invalidRequest = loadJson("catalog/invalid-catalog-request.json");

        // Send request and expect 400 Bad Request
        webTestClient.post()
            .uri("/api/catalogs")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void testGetNonExistentCatalog() throws Exception
    {
        // Try to get a catalog that doesn't exist
        String fakeId = "00000000-0000-0000-0000-000000000000";

        webTestClient.get()
            .uri("/api/catalogs/" + fakeId)
            .exchange()
            .expectStatus().isNotFound();
    }
}
