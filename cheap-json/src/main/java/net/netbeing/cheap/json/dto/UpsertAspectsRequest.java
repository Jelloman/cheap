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

package net.netbeing.cheap.json.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for upserting aspects.
 */
public record UpsertAspectsRequest(
    @JsonProperty("aspects") List<AspectData> aspects
)
{
    /**
     * Aspect data for a single entity.
     */
    public record AspectData(
        @JsonProperty("entityId") UUID entityId,
        @JsonProperty("properties") Map<String, Object> properties
    )
    {
    }
}
