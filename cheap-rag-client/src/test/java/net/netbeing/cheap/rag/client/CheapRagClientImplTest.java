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

package net.netbeing.cheap.rag.client;

import net.netbeing.cheap.rag.client.dto.IndexStatusResponse;
import net.netbeing.cheap.rag.client.dto.QueryRequest;
import net.netbeing.cheap.rag.client.dto.QueryResponse;
import net.netbeing.cheap.rag.client.exception.CheapRagBadRequestException;
import net.netbeing.cheap.rag.client.exception.CheapRagNotFoundException;
import net.netbeing.cheap.rag.client.exception.CheapRagServerException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheapRagClientImplTest
{
    private MockWebServer mockWebServer;
    private CheapRagClient client;

    @BeforeEach
    void setup() throws IOException
    {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        client = new CheapRagClientImpl(baseUrl);
    }

    @AfterEach
    void teardown() throws IOException
    {
        mockWebServer.shutdown();
    }

    @Test
    void testQueryWithSimpleString() throws IOException
    {
        // Load test fixture
        String responseBody = loadTestResource("query-response.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        // Execute query
        QueryResponse response = client.query("What is CheapFactory?");

        // Verify response
        assertNotNull(response);
        assertEquals("CheapFactory is the main factory class for creating Cheap objects.",
                response.getAnswer());
        assertEquals("What is CheapFactory?", response.getQuery());
        assertEquals(1, response.getCitations().size());
        assertEquals("java_class_abc123", response.getCitations().get(0).getArtifactId());
        assertEquals(1, response.getSources().size());
        assertEquals(0.92, response.getConfidence());
    }

    @Test
    void testQueryWithFilters() throws IOException
    {
        String responseBody = loadTestResource("query-response.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        // Build request with filters
        Map<String, Object> filters = FilterBuilder.create()
                .language("java")
                .type("class")
                .module("cheap-core")
                .build();

        QueryRequest request = QueryRequest.builder()
                .query("What is CheapFactory?")
                .topK(5)
                .filters(filters)
                .temperature(0.7)
                .build();

        // Execute query
        QueryResponse response = client.query(request);

        assertNotNull(response);
        assertEquals("CheapFactory is the main factory class for creating Cheap objects.",
                response.getAnswer());
    }

    @Test
    void testGetIndexStatus() throws IOException
    {
        String responseBody = loadTestResource("index-status-response.json");

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        IndexStatusResponse response = client.getIndexStatus();

        assertNotNull(response);
        assertEquals("cheap_metadata", response.getCollectionName());
        assertEquals(150, response.getTotalArtifacts());
        assertTrue(response.getIndexed());
        assertNotNull(response.getArtifactCountsByType());
        assertEquals(50, response.getArtifactCountsByType().get("class"));
    }

    @Test
    void testHealthCheck()
    {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"status\":\"healthy\"}")
                .addHeader("Content-Type", "application/json"));

        Map<String, Object> response = client.healthCheck();

        assertNotNull(response);
        assertEquals("healthy", response.get("status"));
    }

    @Test
    void testNotFoundError()
    {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("Resource not found"));

        assertThrows(CheapRagNotFoundException.class, () -> {
            client.getIndexStatus();
        });
    }

    @Test
    void testBadRequestError()
    {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("Invalid query parameters"));

        assertThrows(CheapRagBadRequestException.class, () -> {
            client.query("test");
        });
    }

    @Test
    void testServerError()
    {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal server error"));

        assertThrows(CheapRagServerException.class, () -> {
            client.query("test");
        });
    }

    @Test
    void testFilterBuilder()
    {
        Map<String, Object> filters = FilterBuilder.create()
                .language("java")
                .type("interface")
                .module("cheap-core")
                .tags("core", "api")
                .primaryKey(true)
                .custom("nullable", false)
                .build();

        assertEquals("java", filters.get("language"));
        assertEquals("interface", filters.get("type"));
        assertEquals("cheap-core", filters.get("module"));
        assertTrue(filters.containsKey("tags"));
        assertEquals(true, filters.get("primary_key"));
        assertEquals(false, filters.get("nullable"));
    }

    private String loadTestResource(String filename) throws IOException
    {
        String path = "src/test/resources/http-tests/" + filename;
        return Files.readString(Paths.get(path));
    }
}
