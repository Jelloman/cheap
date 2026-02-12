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

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Types of code artifacts that can be extracted.
 * Values must match Python MetadataArtifact schema exactly.
 */
public enum ArtifactType
{
    CLASS("class"),
    INTERFACE("interface"),
    ENUM("enum"),
    METHOD("method"),
    FUNCTION("function"),
    FIELD("field"),
    TABLE("table"),
    COLUMN("column");

    private final String value;

    ArtifactType(String value)
    {
        this.value = value;
    }

    @JsonValue
    public String getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return value;
    }
}
