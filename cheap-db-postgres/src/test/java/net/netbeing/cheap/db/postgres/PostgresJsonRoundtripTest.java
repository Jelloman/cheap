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

package net.netbeing.cheap.db.postgres;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.FlywayPreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;
import net.netbeing.cheap.json.jackson.deserialize.CheapJacksonDeserializer;
import net.netbeing.cheap.json.jackson.serialize.CheapJacksonSerializer;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.impl.basic.CheapFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PostgresJsonRoundtripTest
{
    @RegisterExtension
    public static PreparedDbExtension flywayDB = EmbeddedPostgresExtension.preparedDatabase(FlywayPreparer.forClasspathLocation("db/pg"));

    static volatile DataSource dataSource;
    static volatile boolean schemaInitialized = false;

    PostgresDao postgresDao;
    PostgresAdapter adapter;
    CheapFactory factory;
    ObjectMapper objectMapper;
    CheapJacksonDeserializer deserializer;

    void setupDatabase() throws SQLException, IOException, URISyntaxException
    {
        // Get the datasource (will be initialized by JUnit extension)
        dataSource = flywayDB.getTestDatabase();

        // Initialize database schema once for all tests
        if (!schemaInitialized) {
            initializeSchema();
            schemaInitialized = true;
        }

        factory = new CheapFactory();
        adapter = new PostgresAdapter(dataSource, factory);
        postgresDao = new PostgresDao(adapter);
        objectMapper = new ObjectMapper();
        deserializer = new CheapJacksonDeserializer(factory);

        // Clean up all tables before each test
        truncateAllTables();
    }

    private static void initializeSchema() throws SQLException, IOException, URISyntaxException
    {
        String mainSchemaPath = "/db/schemas/postgres/postgres-cheap.sql";
        String auditSchemaPath = "/db/schemas/postgres/postgres-cheap-audit.sql";

        String mainDdl = loadResourceFile(mainSchemaPath);
        String auditDdl = loadResourceFile(auditSchemaPath);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(mainDdl);
            statement.execute(auditDdl);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private static String loadResourceFile(String resourcePath) throws IOException, URISyntaxException
    {
        Path path = Paths.get(PostgresJsonRoundtripTest.class.getResource(resourcePath).toURI());
        return Files.readString(path);
    }

    private void truncateAllTables() throws SQLException, IOException, URISyntaxException
    {
        String truncateSql = loadResourceFile("/db/schemas/postgres/postgres-cheap-truncate.sql");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(truncateSql);
        }
    }

    @Test
    void testFullCatalogJsonRoundtrip() throws SQLException, IOException, URISyntaxException
    {
        setupDatabase();

        // Load the original JSON
        String originalJson = loadResourceFile("/jackson/full-catalog.json");
        JsonNode originalJsonNode = objectMapper.readTree(originalJson);

        // Deserialize to Catalog
        Catalog catalog = deserializer.fromJson(originalJson);
        assertNotNull(catalog);

        // Save to database
        postgresDao.saveCatalog(catalog);

        // Load back from database
        Catalog loadedCatalog = postgresDao.loadCatalog(catalog.globalId());
        assertNotNull(loadedCatalog);

        // Serialize loaded catalog to JSON
        String serializedJson = CheapJacksonSerializer.toJson(loadedCatalog);
        JsonNode serializedJsonNode = objectMapper.readTree(serializedJson);

        // Compare the two JSON nodes
        assertEquals(originalJsonNode, serializedJsonNode,
            "Serialized JSON should match original JSON after database round-trip");
    }
}
