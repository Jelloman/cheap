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

package net.netbeing.cheap.rest.service;

import net.netbeing.cheap.db.CheapDao;
import net.netbeing.cheap.db.sqlite.SqliteAdapter;
import net.netbeing.cheap.db.sqlite.SqliteCheapSchema;
import net.netbeing.cheap.db.sqlite.SqliteDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base class for service tests that provides common SQLite database setup.
 * Uses a temporary file-based SQLite database for testing to avoid shared cache issues.
 */
public abstract class BaseServiceTest
{
    private static final Logger logger = LoggerFactory.getLogger(BaseServiceTest.class);

    protected static DataSource dataSource;
    protected static Connection connection;
    protected static CheapDao dao;
    protected static CheapFactory factory;
    protected static Path tempDbFile;

    // Services - to be initialized by subclasses
    protected static CatalogService catalogService;
    protected static AspectDefService aspectDefService;
    protected static AspectService aspectService;
    protected static HierarchyService hierarchyService;

    @BeforeAll
    static void setUpAll() throws SQLException, IOException
    {
        logger.info("start setUpAll()");

        // Create a temporary file-based SQLite database
        tempDbFile = Files.createTempFile("cheap-test-", ".db");

        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + tempDbFile.toAbsolutePath());
        dataSource = ds;

        // Get a connection for schema initialization
        connection = ds.getConnection();

        // Initialize factory and DAO
        factory = new CheapFactory();
        SqliteAdapter adapter = new SqliteAdapter(dataSource, factory);
        dao = new SqliteDao(adapter);

        // Initialize schema
        initializeSchema();
        
        // Initialize services
        catalogService = new CatalogService(dao, factory, dataSource);
        catalogService.setService(catalogService); // Inject self-reference for transactional methods
        aspectDefService = new AspectDefService(catalogService, dao);
        aspectService = new AspectService(dao, catalogService, factory);
        hierarchyService = new HierarchyService(dao, catalogService, factory);
        hierarchyService.setService(hierarchyService);

        // Close the connection
        if (connection != null && !connection.isClosed()) {
            connection.close();
            connection = null;
        }

        logger.info("end setUpAll()");
    }

    @BeforeEach
    void setUpBase() throws SQLException
    {
        connection = dataSource.getConnection();
    }

    @AfterEach
    void tearDownBase() throws SQLException
    {
        SqliteCheapSchema schema = new SqliteCheapSchema();
        schema.executeTruncateSchemaDdl(connection);

        // Close the connection
        if (connection != null && !connection.isClosed()) {
            connection.close();
            connection = null;
        }
    }

    @AfterAll
    static void tearDownAll() throws SQLException, IOException
    {
        // Double-check closing the connection
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }

        // Clean up references
        connection = null;
        dataSource = null;
        dao = null;
        factory = null;
        catalogService = null;
        aspectDefService = null;
        aspectService = null;
        hierarchyService = null;

        // Delete the temporary database file
        if (tempDbFile != null && Files.exists(tempDbFile)) {
            Files.delete(tempDbFile);
        }
        tempDbFile = null;
    }

    private static void initializeSchema() throws SQLException
    {
        // Use SqliteCheapSchema to execute DDL
        SqliteCheapSchema schema = new SqliteCheapSchema();
        schema.executeMainSchemaDdl(connection);
        schema.executeAuditSchemaDdl(connection);
    }
}
