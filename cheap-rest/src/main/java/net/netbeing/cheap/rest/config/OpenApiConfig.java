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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Cheap REST API.
 * Configured for Spring WebFlux reactive endpoints.
 */
@Configuration
public class OpenApiConfig
{
    @Bean
    public OpenAPI cheapRestOpenAPI()
    {
        return new OpenAPI()
            .info(new Info()
                .title("Cheap REST API")
                .description("Reactive REST API for the Cheap data caching system using Spring WebFlux. " +
                    "Provides non-blocking endpoints for creating and querying catalogs, AspectDefs, " +
                    "hierarchies, and aspects. All endpoints return Mono or Flux for reactive processing.")
                .version("0.1")
                .contact(new Contact()
                    .name("Cheap Project")
                    .url("https://github.com/Jelloman/cheap"))
                .license(new License()
                    .name("Apache License 2.0")
                    .url("http://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Development server (WebFlux/Netty)")
            ));
    }
}
