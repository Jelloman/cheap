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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Response from RAG query endpoint.
 * Maps to Python cheap-rag QueryResponse with snake_case JSON fields.
 */
@Value
@Builder
@Jacksonized
public class QueryResponse
{
    @NotNull
    @JsonProperty("answer")
    String answer;

    @NotNull
    @JsonProperty("query")
    String query;

    @NotNull
    @JsonProperty("citations")
    List<CitationInfo> citations;

    @NotNull
    @JsonProperty("sources")
    List<ArtifactSummary> sources;

    @NotNull
    @JsonProperty("search_metadata")
    SearchMetadata searchMetadata;

    @NotNull
    @JsonProperty("generation_metadata")
    GenerationMetadata generationMetadata;

    @NotNull
    @JsonProperty("citation_metrics")
    CitationMetrics citationMetrics;

    @NotNull
    @JsonProperty("timestamp")
    String timestamp;

    @NotNull
    @JsonProperty("total_time_ms")
    Double totalTimeMs;

    @Nullable
    @JsonProperty("confidence")
    Double confidence;

    @Nullable
    @JsonProperty("warnings")
    List<String> warnings;
}
