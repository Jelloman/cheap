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

package net.netbeing.cheap.rag.client;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Configuration for the CHEAP RAG client.
 */
@Value
@Builder
public class CheapRagClientConfig
{
    /**
     * Base URL of the cheap-rag API (required).
     */
    @NotNull
    String baseUrl;

    /**
     * Connection timeout (default: 5 seconds).
     */
    @NotNull
    @Builder.Default
    Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * Response timeout for read operations (default: 30 seconds).
     */
    @NotNull
    @Builder.Default
    Duration responseTimeout = Duration.ofSeconds(30);

    /**
     * Maximum number of connections in the pool (default: 50).
     */
    @Builder.Default
    int maxConnections = 50;

    /**
     * Maximum idle time for connections (default: 30 seconds).
     */
    @NotNull
    @Builder.Default
    Duration maxIdleTime = Duration.ofSeconds(30);
}
