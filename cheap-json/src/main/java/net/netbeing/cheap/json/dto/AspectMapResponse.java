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
import net.netbeing.cheap.model.Aspect;

import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for AspectMap hierarchy contents.
 */
public record AspectMapResponse(
    @JsonProperty("catalogId") UUID catalogId,
    @JsonProperty("hierarchyName") String hierarchyName,
    @JsonProperty("aspectDefName") String aspectDefName,
    @JsonProperty("content") Map<UUID, Aspect> content,
    @JsonProperty("page") int page,
    @JsonProperty("size") int size,
    @JsonProperty("totalElements") long totalElements,
    @JsonProperty("totalPages") int totalPages
)
{
}
