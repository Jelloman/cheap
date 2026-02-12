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

package net.netbeing.cheap.rag.extractor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Metadata artifact matching the Python cheap-rag MetadataArtifact schema exactly.
 * All JSON field names use snake_case to match Python convention.
 */
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataArtifact
{
    /**
     * Unique identifier (SHA-256 hash, format: {language}_{type}_{hash16})
     */
    @NotNull
    @JsonProperty("id")
    String id;

    /**
     * Artifact type (class, interface, method, etc.)
     */
    @NotNull
    @JsonProperty("type")
    ArtifactType type;

    /**
     * Artifact name
     */
    @NotNull
    @JsonProperty("name")
    String name;

    /**
     * Programming language
     */
    @NotNull
    @JsonProperty("language")
    String language;

    /**
     * Source type (code or database)
     */
    @NotNull
    @JsonProperty("source_type")
    SourceType sourceType;

    /**
     * Module/package name
     */
    @Nullable
    @JsonProperty("module")
    String module;

    /**
     * Source file path
     */
    @Nullable
    @JsonProperty("source_file")
    String sourceFile;

    /**
     * Line number in source file
     */
    @Nullable
    @JsonProperty("source_line")
    Integer sourceLine;

    /**
     * Documentation string (Javadoc, docstring, etc.)
     */
    @Nullable
    @JsonProperty("documentation")
    String documentation;

    /**
     * Method/function signature
     */
    @Nullable
    @JsonProperty("signature")
    String signature;

    /**
     * Full qualified name
     */
    @Nullable
    @JsonProperty("qualified_name")
    String qualifiedName;

    /**
     * Parent artifact ID (for methods, fields, etc.)
     */
    @Nullable
    @JsonProperty("parent_id")
    String parentId;

    /**
     * Tags for categorization
     */
    @Nullable
    @JsonProperty("tags")
    List<String> tags;

    /**
     * Custom metadata fields
     */
    @Nullable
    @JsonProperty("metadata")
    Map<String, Object> metadata;

    /**
     * Text for embedding (combined name, docs, signature)
     */
    @NotNull
    @JsonProperty("embedding_text")
    String embeddingText;
}
