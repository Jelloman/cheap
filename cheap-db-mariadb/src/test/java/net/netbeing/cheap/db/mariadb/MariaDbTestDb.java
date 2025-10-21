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

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import net.netbeing.cheap.util.CheapFactory;
import org.mariadb.jdbc.MariaDbDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.TimeZone;

class MariaDbTestDb
{
    final DataSource dataSource;
    final DB mariaDB;
    final MariaDbDao mariaDbDao;
    final CheapFactory factory;
    final DBConfiguration dbConfig;
    final String dbName;
    final MariaDbAdapter adapter;

    MariaDbTestDb(String dbName) throws ManagedProcessException, SQLException
    {
        this.dbName = dbName;

        // Create embedded MariaDB instance
        dbConfig = DBConfigurationBuilder.newBuilder().setPort(0).build();

        mariaDB = DB.newEmbeddedDB(dbConfig);
        mariaDB.start();

        // Create database
        mariaDB.createDB(dbName);

        // Create data source
        MariaDbDataSource ds = new MariaDbDataSource();
        String url = dbConfig.getURL(dbName);
        url = url + (url.indexOf('?') >= 0 ? "&" : "?") + "allowMultiQueries=true";
        ds.setUrl(url);
        ds.setUser("root");
        ds.setPassword("");
        this.dataSource = ds;

        // Initialize factory and DAO
        factory = new CheapFactory();
        adapter = new MariaDbAdapter(dataSource, factory);
        adapter.setTimeZone(TimeZone.getTimeZone("UTC"));
        mariaDbDao = new MariaDbDao(adapter);
    }

    void tearDown() throws ManagedProcessException
    {
        // Stop embedded MariaDB
        mariaDB.stop();
    }

    void initializeCheapSchema() throws SQLException
    {
        // Use MariaDbCheapSchema to execute DDL
        MariaDbCheapSchema schema = new MariaDbCheapSchema();
        schema.executeMainSchemaDdl(dataSource);
        schema.executeAuditSchemaDdl(dataSource);
    }

    void truncateAllTables() throws SQLException
    {
        // Use MariaDbCheapSchema to execute DDL
        MariaDbCheapSchema schema = new MariaDbCheapSchema();
        schema.executeTruncateSchemaDdl(dataSource);
    }

}
