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

package net.netbeing.cheap.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for aspect upsert operation.
 */
public record UpsertAspectsResponse(
    @JsonProperty("catalogId") UUID catalogId,
    @JsonProperty("aspectDefName") String aspectDefName,
    @JsonProperty("results") List<AspectResult> results,
    @JsonProperty("successCount") int successCount,
    @JsonProperty("failureCount") int failureCount
)
{
    /**
     * Result for a single aspect upsert.
     */
    public record AspectResult(
        @JsonProperty("entityId") UUID entityId,
        @JsonProperty("success") boolean success,
        @JsonProperty("created") boolean created,
        @JsonProperty("message") String message
    )
    {
    }
}
