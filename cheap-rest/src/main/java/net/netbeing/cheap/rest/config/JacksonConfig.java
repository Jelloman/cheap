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
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.json.jackson.deserialize.CheapJacksonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Configuration class for Jackson JSON serialization/deserialization.
 *
 * Configures Spring WebFlux's ObjectMapper with Cheap-specific deserializers
 * for handling Cheap data model interfaces (CatalogDef, AspectDef, etc.).
 *
 * This configuration enables pretty printing and registers custom deserializers
 * that use the CheapFactory for object creation.
 */
@Configuration
public class JacksonConfig implements WebFluxConfigurer
{
    private static final Logger logger = LoggerFactory.getLogger(JacksonConfig.class);

    private final CheapFactory cheapFactory;
    private ObjectMapper configuredMapper;

    public JacksonConfig(CheapFactory cheapFactory)
    {
        this.cheapFactory = cheapFactory;
    }

    /**
     * Configures the ObjectMapper used by Spring for JSON conversion.
     *
     * Enables pretty printing and registers Cheap-specific deserializers.
     *
     * @return configured ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper()
    {
        if (configuredMapper == null) {
            logger.info("Configuring Jackson ObjectMapper with Cheap deserializers");

            configuredMapper = new ObjectMapper();
            configuredMapper.enable(SerializationFeature.INDENT_OUTPUT);

            // Register Cheap custom deserializers module
            configuredMapper.registerModule(CheapJacksonDeserializer.createCheapModule(cheapFactory));

            logger.info("Jackson ObjectMapper configured successfully with Cheap deserializers");
        }
        return configuredMapper;
    }

    /**
     * Configures WebFlux HTTP message codecs to use the custom ObjectMapper.
     *
     * @param configurer the codec configurer
     */
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer)
    {
        ObjectMapper mapper = objectMapper();
        configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(mapper));
        configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(mapper));
    }
}
