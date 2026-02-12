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

import net.netbeing.cheap.rag.client.dto.IndexStatusResponse;
import net.netbeing.cheap.rag.client.dto.QueryRequest;
import net.netbeing.cheap.rag.client.dto.QueryResponse;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Client interface for the CHEAP RAG REST API.
 * Provides methods for querying the RAG system and retrieving index status.
 */
public interface CheapRagClient
{
    /**
     * Performs a simple RAG query with default parameters.
     *
     * @param query The natural language query
     * @return The query response with answer, citations, and metadata
     */
    @NotNull
    QueryResponse query(@NotNull String query);

    /**
     * Performs a RAG query with full control over parameters.
     *
     * @param request The query request with filters and parameters
     * @return The query response with answer, citations, and metadata
     */
    @NotNull
    QueryResponse query(@NotNull QueryRequest request);

    /**
     * Performs a RAG query asynchronously (non-blocking).
     *
     * @param request The query request
     * @return A Mono that emits the query response
     */
    @NotNull
    Mono<QueryResponse> queryAsync(@NotNull QueryRequest request);

    /**
     * Retrieves the current index status and statistics.
     *
     * @return Index status with artifact counts and metadata
     */
    @NotNull
    IndexStatusResponse getIndexStatus();

    /**
     * Performs a health check on the RAG service.
     *
     * @return Health check response map
     */
    @NotNull
    Map<String, Object> healthCheck();

    /**
     * Triggers a rebuild of the vector index.
     * NOTE: This is a placeholder for Phase 1.
     *
     * @return Rebuild status response
     */
    @NotNull
    Map<String, Object> rebuildIndex();
}
