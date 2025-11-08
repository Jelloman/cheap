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
import net.netbeing.cheap.impl.basic.CheapFactory;
import org.mariadb.jdbc.MariaDbDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.TimeZone;

class MariaDbTestDb
{
    final DataSource dataSource;
    final MariaDbDao mariaDbDao;
    final CheapFactory factory;
    final String dbName;
    final MariaDbAdapter adapter;
    final boolean useForeignKeys;

    MariaDbTestDb(String dbName, boolean useForeignKeys) throws ManagedProcessException, SQLException
    {
        this.dbName = dbName;
        this.useForeignKeys = useForeignKeys;

        // Create database
        DatabaseRunnerExtension.getMariaDB().createDB(dbName);

        // Create data source
        MariaDbDataSource ds = new MariaDbDataSource();
        String url = DatabaseRunnerExtension.getDbConfig().getURL(dbName);
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

    void initializeCheapSchema() throws SQLException
    {
        // Use MariaDbCheapSchema to execute DDL
        MariaDbCheapSchema schema = new MariaDbCheapSchema();
        schema.executeMainSchemaDdl(dataSource);
        if (useForeignKeys) {
            schema.executeForeignKeysDdl(dataSource);
        }
        schema.executeAuditSchemaDdl(dataSource);
    }

    void truncateAllTables() throws SQLException
    {
        // Use MariaDbCheapSchema to execute DDL
        MariaDbCheapSchema schema = new MariaDbCheapSchema();
        schema.executeTruncateSchemaDdl(dataSource);
    }

}
