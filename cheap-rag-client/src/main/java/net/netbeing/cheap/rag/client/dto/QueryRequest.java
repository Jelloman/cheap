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

package net.netbeing.cheap.rag.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Request payload for RAG query endpoint.
 * Maps to Python cheap-rag QueryRequest with snake_case JSON fields.
 */
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryRequest
{
    /**
     * The natural language query (required)
     */
    @NotNull
    @JsonProperty("query")
    String query;

    /**
     * Number of top results to retrieve (1-50, optional)
     */
    @Nullable
    @JsonProperty("top_k")
    Integer topK;

    /**
     * Minimum similarity score threshold (0.0-1.0, optional)
     */
    @Nullable
    @JsonProperty("similarity_threshold")
    Double similarityThreshold;

    /**
     * Metadata filters (optional)
     * Supported keys: language, type, source_type, module, tags, table_name,
     * column_type, primary_key, and custom fields
     */
    @Nullable
    @JsonProperty("filters")
    Map<String, Object> filters;

    /**
     * LLM temperature for generation (0.0-2.0, optional)
     */
    @Nullable
    @JsonProperty("temperature")
    Double temperature;

    /**
     * Maximum tokens for LLM response (100-4096, optional)
     */
    @Nullable
    @JsonProperty("max_tokens")
    Integer maxTokens;
}
