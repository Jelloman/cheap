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

package net.netbeing.cheap.db.sqlite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.netbeing.cheap.json.jackson.deserialize.CheapJacksonDeserializer;
import net.netbeing.cheap.json.jackson.serialize.CheapJacksonSerializer;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.impl.basic.CheapFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SqliteJsonRoundtripTest
{
    DataSource dataSource;
    Connection connection;
    SqliteDao sqliteDao;
    SqliteAdapter adapter;
    CheapFactory factory;

    @BeforeEach
    void setUp() throws SQLException
    {
        // Create an in-memory SQLite database with shared cache
        // Using shared cache mode allows multiple connections to access the same in-memory database
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:file::memory:?cache=shared");
        this.dataSource = ds;
        // Keep connection open to prevent in-memory database deletion
        this.connection = ds.getConnection();

        // Initialize factory and DAO
        factory = new CheapFactory();
        adapter = new SqliteAdapter(dataSource, factory);
        sqliteDao = new SqliteDao(adapter);

        // Initialize schema
        initializeSchema();
    }

    @AfterEach
    void tearDown() throws SQLException
    {
        // Close the connection (which will delete the in-memory database)
        if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
        }
        this.connection = null;
        this.dataSource = null;
        this.sqliteDao = null;
        this.factory = null;
    }

    private void initializeSchema() throws SQLException
    {
        // Use SqliteCheapSchema to execute DDL
        SqliteCheapSchema schema = new SqliteCheapSchema();
        schema.executeMainSchemaDdl(connection);
        schema.executeAuditSchemaDdl(connection);
    }

    @Test
    void testFullCatalogJsonRoundtrip() throws SQLException, IOException, URISyntaxException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        CheapJacksonDeserializer deserializer = new CheapJacksonDeserializer(factory);

        // Load the original JSON
        String originalJson = loadResourceFile("/jackson/full-catalog.json");
        JsonNode originalJsonNode = objectMapper.readTree(originalJson);

        // Deserialize to Catalog
        Catalog catalog = deserializer.fromJson(originalJson);
        assertNotNull(catalog);

        // Save to database
        sqliteDao.saveCatalog(catalog);

        // Load back from database
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalog.globalId());
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
