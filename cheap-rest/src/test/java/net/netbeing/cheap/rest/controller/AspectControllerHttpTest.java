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
 * HTTP integration tests for AspectController.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class AspectControllerHttpTest extends BaseControllerHttpTest
{
    private String catalogId;

    @BeforeEach
    void setupCatalog() throws Exception
    {
        // Create a catalog for testing Aspect operations
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
    void testUpsertAspectsCreate() throws Exception
    {
        // Load request JSON
        String requestJson = loadJson("aspect/upsert-aspects-request.json");

        // Send POST request and verify response
        String responseJson = webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/aspects/person")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Parse and verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);

        // Verify structure
        assertThat(responseNode.has("catalogId")).isTrue();
        assertThat(responseNode.has("aspectDefName")).isTrue();
        assertThat(responseNode.has("results")).isTrue();
        assertThat(responseNode.has("successCount")).isTrue();
        assertThat(responseNode.has("failureCount")).isTrue();

        // Verify success
        assertThat(responseNode.get("aspectDefName").asText()).isEqualTo("person");
        assertThat(responseNode.get("successCount").asInt()).isEqualTo(2);
        assertThat(responseNode.get("failureCount").asInt()).isZero();

        // Verify results array
        JsonNode results = responseNode.get("results");
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isEqualTo(2);

        // Verify first result
        JsonNode firstResult = results.get(0);
        assertThat(firstResult.get("success").asBoolean()).isTrue();
        assertThat(firstResult.get("created").asBoolean()).isTrue();
    }

    @Test
    void testUpsertAspectsUpdate() throws Exception
    {
        // Load request JSON
        String requestJson = loadJson("aspect/upsert-aspects-request.json");

        // First upsert - create
        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/aspects/person")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isOk();

        // Second upsert - update
        String updateJson = loadJson("aspect/update-aspects-request.json");
        String responseJson = webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/aspects/person")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateJson)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Parse and verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);

        // Verify that it was an update, not a creation
        JsonNode results = responseNode.get("results");
        JsonNode firstResult = results.get(0);
        assertThat(firstResult.get("success").asBoolean()).isTrue();
        assertThat(firstResult.get("created").asBoolean()).isFalse(); // Should be false for update
    }

    @Test
    void testQueryAspects() throws Exception
    {
        // First upsert some aspects
        String upsertJson = loadJson("aspect/upsert-aspects-request.json");
        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/aspects/person")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(upsertJson)
            .exchange()
            .expectStatus().isOk();

        // Query those aspects
        String queryJson = loadJson("aspect/query-aspects-request.json");
        String responseJson = webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/aspects/query")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(queryJson)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Parse and verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);

        // Verify structure
        assertThat(responseNode.has("catalogId")).isTrue();
        assertThat(responseNode.has("results")).isTrue();

        // Verify results contain entities
        JsonNode results = responseNode.get("results");
        assertThat(results.has("660e8400-e29b-41d4-a716-446655440001")).isTrue();
        assertThat(results.has("660e8400-e29b-41d4-a716-446655440002")).isTrue();

        // Verify aspects are present
        JsonNode entity1 = results.get("660e8400-e29b-41d4-a716-446655440001");
        assertThat(entity1.has("person")).isTrue();

        JsonNode personAspect = entity1.get("person");
        assertThat(personAspect.has("firstName")).isTrue();
        assertThat(personAspect.get("firstName").asText()).isEqualTo("Alice");
    }

    @Test
    void testQueryAspectsEmpty() throws Exception
    {
        // Query aspects that don't exist
        String queryJson = loadJson("aspect/query-aspects-empty-request.json");
        String responseJson = webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/aspects/query")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(queryJson)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // Parse and verify response
        JsonNode responseNode = objectMapper.readTree(responseJson);

        // Verify structure
        assertThat(responseNode.has("catalogId")).isTrue();
        assertThat(responseNode.has("results")).isTrue();

        // Results should be empty
        JsonNode results = responseNode.get("results");
        assertThat(results.size()).isZero();
    }

    @Test
    void testUpsertAspectsBatchSizeExceeded()
    {
        // Create a request with more than 1000 aspects (exceeds max-batch-size)
        StringBuilder jsonBuilder = new StringBuilder("{\"aspects\":[");
        for (int i = 0; i < 1001; i++) {
            if (i > 0) jsonBuilder.append(",");
            jsonBuilder.append(String.format(
                "{\"entityId\":\"660e8400-e29b-41d4-a716-%012d\",\"properties\":{\"firstName\":\"Person%d\",\"lastName\":\"Test\",\"age\":30}}",
                i, i
            ));
        }
        jsonBuilder.append("]}");
        String requestJson = jsonBuilder.toString();

        // Should fail with 400 Bad Request
        webTestClient.post()
            .uri("/api/catalog/" + catalogId + "/aspects/person")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("exceeds maximum"));
    }

    @Test
    void testUpsertAspectsNonExistentCatalog() throws Exception
    {
        String fakeId = "00000000-0000-0000-0000-000000000000";
        String requestJson = loadJson("aspect/upsert-aspects-request.json");

        webTestClient.post()
            .uri("/api/catalog/" + fakeId + "/aspects/person")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isNotFound();
    }
}
