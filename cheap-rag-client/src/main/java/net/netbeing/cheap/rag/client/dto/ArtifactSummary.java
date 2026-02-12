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

/**
 * Summary of a metadata artifact from the RAG index.
 */
@Value
@Builder
@Jacksonized
public class ArtifactSummary
{
    @NotNull
    @JsonProperty("id")
    String id;

    @NotNull
    @JsonProperty("type")
    String type;

    @NotNull
    @JsonProperty("name")
    String name;

    @Nullable
    @JsonProperty("language")
    String language;

    @Nullable
    @JsonProperty("module")
    String module;

    @Nullable
    @JsonProperty("source_file")
    String sourceFile;

    @Nullable
    @JsonProperty("source_line")
    Integer sourceLine;
}
