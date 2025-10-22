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

package net.netbeing.cheap.rest.config;

import net.netbeing.cheap.db.CheapDao;
import net.netbeing.cheap.db.mariadb.MariaDbAdapter;
import net.netbeing.cheap.db.mariadb.MariaDbDao;
import net.netbeing.cheap.db.postgres.PostgresAdapter;
import net.netbeing.cheap.db.postgres.PostgresDao;
import net.netbeing.cheap.db.sqlite.SqliteAdapter;
import net.netbeing.cheap.db.sqlite.SqliteDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuration class for creating Cheap DAO beans based on the selected database type.
 * 
 * Uses Spring profiles and conditional bean creation to instantiate the correct
 * DAO implementation (PostgreSQL, SQLite, or MariaDB) based on the
 * {@code cheap.database.type} property.
 * 
 * This configuration also creates a shared CheapFactory bean used by all DAOs
 * for consistent object creation and entity registry management.
 */
@Configuration
public class CheapDaoConfig
{
    private static final Logger logger = LoggerFactory.getLogger(CheapDaoConfig.class);

    /**
     * Creates a CheapFactory bean for use across the application.
     * 
     * @return a new CheapFactory instance
     */
    @Bean
    public CheapFactory cheapFactory()
    {
        logger.info("Creating CheapFactory bean");
        return new CheapFactory();
    }

    /**
     * PostgreSQL-specific configuration.
     */
    @Configuration
    @ConditionalOnProperty(name = "cheap.database.type", havingValue = "postgres")
    static class PostgresConfig
    {
        private static final Logger logger = LoggerFactory.getLogger(PostgresConfig.class);

        /**
         * Creates a PostgresDao bean when PostgreSQL is the selected database type.
         * 
         * @param dataSource the Spring-managed DataSource
         * @param factory the CheapFactory for object creation
         * @return a configured PostgresDao instance
         */
        @Bean
        public CheapDao cheapDao(DataSource dataSource, CheapFactory factory)
        {
            logger.info("Creating PostgresDao bean");
            PostgresAdapter adapter = new PostgresAdapter(dataSource, factory);
            return new PostgresDao(adapter);
        }
    }

    /**
     * SQLite-specific configuration.
     */
    @Configuration
    @ConditionalOnProperty(name = "cheap.database.type", havingValue = "sqlite")
    static class SqliteConfig
    {
        private static final Logger logger = LoggerFactory.getLogger(SqliteConfig.class);

        /**
         * Creates a SqliteDao bean when SQLite is the selected database type.
         * 
         * @param dataSource the Spring-managed DataSource
         * @param factory the CheapFactory for object creation
         * @return a configured SqliteDao instance
         */
        @Bean
        public CheapDao cheapDao(DataSource dataSource, CheapFactory factory)
        {
            logger.info("Creating SqliteDao bean");
            SqliteAdapter adapter = new SqliteAdapter(dataSource, factory);
            return new SqliteDao(adapter);
        }
    }

    /**
     * MariaDB-specific configuration.
     */
    @Configuration
    @ConditionalOnProperty(name = "cheap.database.type", havingValue = "mariadb")
    static class MariaDbConfig
    {
        private static final Logger logger = LoggerFactory.getLogger(MariaDbConfig.class);

        /**
         * Creates a MariaDbDao bean when MariaDB is the selected database type.
         * 
         * @param dataSource the Spring-managed DataSource
         * @param factory the CheapFactory for object creation
         * @return a configured MariaDbDao instance
         */
        @Bean
        public CheapDao cheapDao(DataSource dataSource, CheapFactory factory)
        {
            logger.info("Creating MariaDbDao bean");
            MariaDbAdapter adapter = new MariaDbAdapter(dataSource, factory);
            return new MariaDbDao(adapter);
        }
    }
}
