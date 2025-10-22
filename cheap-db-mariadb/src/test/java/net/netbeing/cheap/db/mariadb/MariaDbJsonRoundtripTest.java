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

package net.netbeing.cheap.db.mariadb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.netbeing.cheap.json.jackson.deserialize.CheapJacksonDeserializer;
import net.netbeing.cheap.json.jackson.serialize.CheapJacksonSerializer;
import net.netbeing.cheap.model.Catalog;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MariaDbJsonRoundtripTest
{
    static final String DB_NAME = "cheap";
    static volatile MariaDbTestDb db;

    static void setupDatabase() throws Exception
    {
        if (db == null) {
            db = new MariaDbTestDb(DB_NAME);
            db.initializeCheapSchema();
        }
    }

    @AfterAll
    static void tearDown() throws Exception
    {
        if (db != null) {
            db.tearDown();
        }
    }

    @Test
    void testFullCatalogJsonRoundtrip() throws Exception
    {
        setupDatabase();

        // Clean up all tables before test
        db.truncateAllTables();

        ObjectMapper objectMapper = new ObjectMapper();
        CheapJacksonDeserializer deserializer = new CheapJacksonDeserializer(db.factory);

        // Load the original JSON
        String originalJson = loadResourceFile("/jackson/full-catalog.json");
        JsonNode originalJsonNode = objectMapper.readTree(originalJson);

        // Deserialize to Catalog
        Catalog catalog = deserializer.fromJson(originalJson);
        assertNotNull(catalog);

        // Save to database
        db.mariaDbDao.saveCatalog(catalog);

        // Load back from database
        Catalog loadedCatalog = db.mariaDbDao.loadCatalog(catalog.globalId());
        assertNotNull(loadedCatalog);

        // Serialize loaded catalog to JSON
        String serializedJson = CheapJacksonSerializer.toJson(loadedCatalog);
        JsonNode serializedJsonNode = objectMapper.readTree(serializedJson);

        // Compare the two JSON nodes
        assertEquals(originalJsonNode, serializedJsonNode,
            "Serialized JSON should match original JSON after database round-trip");
    }

    @SuppressWarnings({"DataFlowIssue", "SameParameterValue"})
    private String loadResourceFile(String resourcePath) throws IOException, URISyntaxException
    {
        Path path = Paths.get(getClass().getResource(resourcePath).toURI());
        return Files.readString(path);
    }
}
