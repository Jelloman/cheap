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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * Base class for HTTP integration tests using WebTestClient.
 * Provides utilities for loading JSON test files and common test setup.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public abstract class BaseControllerHttpTest
{
    @Autowired
    protected WebTestClient webTestClient;

    protected ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp()
    {
        // Database schema is automatically initialized by Spring SQL init
    }

    @AfterEach
    void tearDown()
    {
        // Test cleanup
    }

    /**
     * Loads a JSON file from test/resources/http-tests directory.
     *
     * @param relativePath path relative to http-tests directory (e.g., "catalog/create-catalog-request.json")
     * @return JSON content as a string
     */
    protected String loadJson(String relativePath) throws IOException
    {
        Path path = Paths.get("src/test/resources/http-tests", relativePath);
        return Files.readString(path);
    }
}
