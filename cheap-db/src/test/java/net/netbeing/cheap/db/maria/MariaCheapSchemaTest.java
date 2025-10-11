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

package net.netbeing.cheap.db.maria;

import ch.vorburger.exec.ManagedProcessException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import org.mariadb.jdbc.MariaDbDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

class MariaCheapSchemaTest
{
    static volatile DB db;
    static String dbName = "cheap";
    static volatile DBConfiguration dbConfiguration;
    static volatile DataSource dataSource;
    static volatile boolean dbInitialized = false;

    @BeforeAll
    static void setUp() throws SQLException, ManagedProcessException
    {
        if (!dbInitialized) {
            dbConfiguration = DBConfigurationBuilder.newBuilder().setPort(0).build();
            db = DB.newEmbeddedDB(dbConfiguration);
            db.start();
            dataSource = new MariaDbDataSource(dbConfiguration.getURL(dbName));
            dbInitialized = true;
        }

    }

    @AfterAll
    static void after() {
        if (dbInitialized) try {
            db.stop();
        } catch (ManagedProcessException e) {
            throw new AssertionError("db.stop() failed", e);
        }
    }

    @Test
    void testAllSchemaExecution() throws SQLException, ManagedProcessException
    {
        //setUp();
        db.createDB(dbName);
    }
}
