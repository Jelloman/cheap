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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base class for service tests that provides common SQLite database setup.
 * Uses an in-memory SQLite database with shared cache for testing.
 */
public abstract class BaseServiceTest
{
    protected DataSource dataSource;
    protected Connection connection;
    protected CheapDao dao;
    protected CheapFactory factory;
    
    // Services - to be initialized by subclasses
    protected CatalogService catalogService;
    protected AspectDefService aspectDefService;
    protected AspectService aspectService;
    protected HierarchyService hierarchyService;

    @BeforeEach
    void setUpBase() throws SQLException
    {
        // Create an in-memory SQLite database with shared cache
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:file::memory:?cache=shared");
        this.dataSource = ds;
        
        // Keep connection open to prevent in-memory database deletion
        this.connection = ds.getConnection();

        // Initialize factory and DAO
        factory = new CheapFactory();
        SqliteAdapter adapter = new SqliteAdapter(dataSource, factory);
        dao = new SqliteDao(adapter);

        // Initialize schema
        initializeSchema();
        
        // Initialize services
        catalogService = new CatalogService(dao, factory, dataSource);
        catalogService.setService(catalogService); // Inject self-reference for transactional methods
        aspectDefService = new AspectDefService(dao, factory);
        aspectService = new AspectService(dao, factory);
        hierarchyService = new HierarchyService(dao, factory);
    }

    @AfterEach
    void tearDownBase() throws SQLException
    {
        // Close the connection (which will delete the in-memory database)
        if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
        }
        this.connection = null;
        this.dataSource = null;
        this.dao = null;
        this.factory = null;
        this.catalogService = null;
        this.aspectDefService = null;
        this.aspectService = null;
        this.hierarchyService = null;
    }

    private void initializeSchema() throws SQLException
    {
        // Use SqliteCheapSchema to execute DDL
        SqliteCheapSchema schema = new SqliteCheapSchema();
        schema.executeMainSchemaDdl(connection);
        schema.executeAuditSchemaDdl(connection);
    }
}
