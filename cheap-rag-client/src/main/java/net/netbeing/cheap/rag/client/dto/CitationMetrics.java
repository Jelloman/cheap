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

/**
 * Metrics about citation accuracy and coverage.
 */
@Value
@Builder
@Jacksonized
public class CitationMetrics
{
    @NotNull
    @JsonProperty("total_citations")
    Integer totalCitations;

    @NotNull
    @JsonProperty("verified_citations")
    Integer verifiedCitations;

    @NotNull
    @JsonProperty("coverage_percentage")
    Double coveragePercentage;
}
