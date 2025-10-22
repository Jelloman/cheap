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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Configuration class for Jackson JSON serialization/deserialization.
 *
 * Configures Spring's ObjectMapper with basic settings. Cheap-specific serializers
 * and deserializers will be integrated in a later phase once the controller layer
 * is implemented.
 *
 * For now, this configuration enables pretty printing for better readability
 * during development and testing.
 */
@Configuration
public class JacksonConfig
{
    private static final Logger logger = LoggerFactory.getLogger(JacksonConfig.class);

    /**
     * Configures the ObjectMapper used by Spring for JSON conversion.
     *
     * Enables pretty printing for better readability. Cheap-specific serializers
     * will be added in a later phase.
     *
     * @return configured ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper()
    {
        logger.info("Configuring Jackson ObjectMapper");

        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
            .featuresToEnable(SerializationFeature.INDENT_OUTPUT)
            .build();

        logger.info("Jackson ObjectMapper configured successfully");
        return mapper;
    }
}
