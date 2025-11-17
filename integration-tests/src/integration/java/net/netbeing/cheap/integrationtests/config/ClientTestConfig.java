package net.netbeing.cheap.integrationtests.config;

import net.netbeing.cheap.rest.client.CheapRestClient;
import net.netbeing.cheap.rest.client.CheapRestClientImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Client-only Spring Boot configuration for integration tests.
 * Provides CheapRestClient beans configured for each database server.
 * NO database dependencies or cheap-rest dependencies.
 */
@Configuration
public class ClientTestConfig
{
    /**
     * REST client for PostgreSQL server (port 8081).
     */
    @Bean("postgresClient")
    public CheapRestClient postgresRestClient()
    {
        return new CheapRestClientImpl("http://localhost:8081");
    }

    /**
     * REST client for SQLite server (port 8082).
     */
    @Bean("sqliteClient")
    public CheapRestClient sqliteRestClient()
    {
        return new CheapRestClientImpl("http://localhost:8082");
    }

    /**
     * REST client for MariaDB server (port 8083).
     */
    @Bean("mariadbClient")
    public CheapRestClient mariadbRestClient()
    {
        return new CheapRestClientImpl("http://localhost:8083");
    }
}
