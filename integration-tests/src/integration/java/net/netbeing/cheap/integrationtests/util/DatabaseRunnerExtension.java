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

package net.netbeing.cheap.integrationtests.util;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class DatabaseRunnerExtension implements BeforeAllCallback, AutoCloseable
{
    private static DB mariaDB;
    private static DBConfiguration dbConfig;

    public static DB getMariaDB()
    {
        return mariaDB;
    }

    public static DBConfiguration getDbConfig()
    {
        return dbConfig;
    }


    private static final ReentrantLock WAIT_FOR_IT = new ReentrantLock();
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception
    {
        // Even though we are using an AtomicBoolean, we also use a lock to guarantee
        // that all other threads actually wait for the thread creating the DB to complete
        // its work. The DB creation takes some time, and without this lock the other tests
        // will race on before the DB spins up.
        WAIT_FOR_IT.lock();
        try {
            if (STARTED.compareAndSet(false, true)) {
                // This is necessary to trigger our close() method.
                context.getRoot().getStore(GLOBAL).put("any unique name", this);

                // Create embedded MariaDB instance
                dbConfig = DBConfigurationBuilder.newBuilder()
                    .setPort(0)
                    .addArg("--innodb-lock-wait-timeout=300")  // Increase from default 50s to 300s
                    .build();

                mariaDB = DB.newEmbeddedDB(dbConfig);
                mariaDB.start();
            }
        } finally {
            // free the access
            WAIT_FOR_IT.unlock();
        }
    }

    @Override
    public void close() throws ManagedProcessException
    {
        // Stop embedded MariaDB
        mariaDB.stop();
    }
}



