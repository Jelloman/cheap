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
 * HTTP integration tests for AspectDefController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class AspectDefControllerHttpTest extends BaseControllerHttpTest
{
    private String catalogId;

    @BeforeEach
    void setupCatalog() throws Exception
    {
        // Create a catalog for testing AspectDef operations
        String createRequest = loadJson("catalog/create-catalog-request.json");
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
    void testCreateAspectDef() throws Exception
    {
        // Load request JSON
        String requestJson = loadJson("aspectdef/create-aspectdef-request.json");

        // Send POST request and verify response
        String responseJson = webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/aspect-defs")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Parse and verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);

        // Verify structure
        assertThat(responseNode.has("aspectDefId")).isTrue();
        assertThat(responseNode.has("aspectDefName")).isTrue();
        assertThat(responseNode.has("message")).isTrue();

        // Verify UUID format
        assertThat(responseNode.get("aspectDefId").asText()).matches(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        );

        // Verify name matches
        assertThat(responseNode.get("aspectDefName").asText()).isEqualTo("com.example.ProductAspect");
    }

    @Test
    void testListAspectDefs() throws Exception
    {
        // First create an AspectDef
        String createRequest = loadJson("aspectdef/create-aspectdef-request.json");
        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/aspect-defs")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated();

        // Then list AspectDefs
        String responseJson = webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/aspect-defs?page=0&size=20")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response structure
        JsonNode responseNode = objectMapper.readTree(responseJson);

        assertThat(responseNode.has("catalogId")).isTrue();
        assertThat(responseNode.has("aspectDefs")).isTrue();
        assertThat(responseNode.has("page")).isTrue();
        assertThat(responseNode.has("size")).isTrue();
        assertThat(responseNode.has("totalElements")).isTrue();
        assertThat(responseNode.has("totalPages")).isTrue();

        // Verify pagination values
        assertThat(responseNode.get("page").asInt()).isEqualTo(0);
        assertThat(responseNode.get("size").asInt()).isEqualTo(20);
        assertThat(responseNode.get("totalElements").asLong()).isGreaterThanOrEqualTo(2); // person from catalog + ProductAspect
    }

    @Test
    void testGetAspectDefById() throws Exception
    {
        // Create an AspectDef
        String createRequest = loadJson("aspectdef/create-aspectdef-request.json");
        String createResponse = webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/aspect-defs")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Extract AspectDef ID
        String aspectDefId = objectMapper.readTree(createResponse).get("aspectDefId").asText();

        // Get the AspectDef by ID
        String responseJson = webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/aspect-defs/" + aspectDefId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response structure
        JsonNode responseNode = objectMapper.readTree(responseJson);

        assertThat(responseNode.has("name")).isTrue();
        assertThat(responseNode.has("globalId")).isTrue();
        assertThat(responseNode.has("propertyDefs")).isTrue();

        assertThat(responseNode.get("name").asText()).isEqualTo("com.example.ProductAspect");
        assertThat(responseNode.get("globalId").asText()).isEqualTo(aspectDefId);
    }

    @Test
    void testGetAspectDefByName() throws Exception
    {
        // Create an AspectDef
        String createRequest = loadJson("aspectdef/create-aspectdef-request.json");
        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/aspect-defs")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated();

        // Get the AspectDef by name
        String responseJson = webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/aspect-defs/com.example.ProductAspect")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Verify response structure
        JsonNode responseNode = objectMapper.readTree(responseJson);

        assertThat(responseNode.has("name")).isTrue();
        assertThat(responseNode.has("globalId")).isTrue();
        assertThat(responseNode.has("propertyDefs")).isTrue();

        assertThat(responseNode.get("name").asText()).isEqualTo("com.example.ProductAspect");
    }

    @Test
    void testGetNonExistentAspectDef() throws Exception
    {
        // Try to get an AspectDef that doesn't exist
        String fakeId = "00000000-0000-0000-0000-000000000000";

        webTestClient.get()
            .uri("/api/catalog/" + catalogId + "/aspect-defs/" + fakeId)
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void testCreateDuplicateAspectDef() throws Exception
    {
        // Create the first AspectDef
        String createRequest = loadJson("aspectdef/create-aspectdef-request.json");
        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/aspect-defs")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated();

        // Try to create another with the same name - should fail
        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/aspect-defs")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().is4xxClientError();
    }
}
