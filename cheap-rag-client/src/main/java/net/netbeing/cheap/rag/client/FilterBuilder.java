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

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for constructing metadata filters for RAG queries.
 * <p>
 * Example usage:
 * <pre>
 * Map&lt;String, Object&gt; filters = FilterBuilder.create()
 *     .language("java")
 *     .type("interface")
 *     .tags("core", "cheap")
 *     .custom("nullable", false)
 *     .build();
 * </pre>
 */
public class FilterBuilder
{
    private final Map<String, Object> filters;

    private FilterBuilder()
    {
        this.filters = new HashMap<>();
    }

    /**
     * Creates a new FilterBuilder instance.
     */
    @NotNull
    public static FilterBuilder create()
    {
        return new FilterBuilder();
    }

    /**
     * Filter by programming language.
     */
    @NotNull
    public FilterBuilder language(@NotNull String language)
    {
        filters.put("language", language);
        return this;
    }

    /**
     * Filter by artifact type (e.g., "class", "interface", "function").
     */
    @NotNull
    public FilterBuilder type(@NotNull String type)
    {
        filters.put("type", type);
        return this;
    }

    /**
     * Filter by source type (e.g., "code", "database").
     */
    @NotNull
    public FilterBuilder sourceType(@NotNull String sourceType)
    {
        filters.put("source_type", sourceType);
        return this;
    }

    /**
     * Filter by module/package name.
     */
    @NotNull
    public FilterBuilder module(@NotNull String module)
    {
        filters.put("module", module);
        return this;
    }

    /**
     * Filter by tags (multiple values).
     */
    @NotNull
    public FilterBuilder tags(@NotNull String... tags)
    {
        filters.put("tags", List.of(tags));
        return this;
    }

    /**
     * Filter by database table name.
     */
    @NotNull
    public FilterBuilder tableName(@NotNull String tableName)
    {
        filters.put("table_name", tableName);
        return this;
    }

    /**
     * Filter by database column type.
     */
    @NotNull
    public FilterBuilder columnType(@NotNull String columnType)
    {
        filters.put("column_type", columnType);
        return this;
    }

    /**
     * Filter by primary key status.
     */
    @NotNull
    public FilterBuilder primaryKey(boolean primaryKey)
    {
        filters.put("primary_key", primaryKey);
        return this;
    }

    /**
     * Add a custom filter field.
     */
    @NotNull
    public FilterBuilder custom(@NotNull String key, @NotNull Object value)
    {
        filters.put(key, value);
        return this;
    }

    /**
     * Builds and returns the filters map.
     */
    @NotNull
    public Map<String, Object> build()
    {
        return new HashMap<>(filters);
    }
}
