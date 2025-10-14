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

package net.netbeing.cheap.db;

import net.netbeing.cheap.model.AspectDef;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps an AspectDef to a database table, with property-to-column mappings.
 * <p>
 * This class enables aspects to be stored in and loaded from custom database tables
 * rather than the standard aspect/property_value tables.
 * </p>
 *
 * <h2>Table Structure Patterns</h2>
 * <p>
 * The mapping supports four different table structure patterns based on the presence
 * of catalog_id and entity_id columns:
 * </p>
 * <ul>
 *   <li><b>No IDs (hasCatalogId=false, hasEntityId=false):</b>
 *       Generic lookup table with no primary key. Table is truncated before each save.
 *       Entity IDs are generated on load.</li>
 *   <li><b>Catalog ID only (hasCatalogId=true, hasEntityId=false):</b>
 *       Catalog-scoped table with no primary key. Rows for the catalog are deleted before save.
 *       Entity IDs are generated on load.</li>
 *   <li><b>Entity ID only (hasCatalogId=false, hasEntityId=true):</b>
 *       Entity-scoped table with PRIMARY KEY (entity_id). Standard behavior with
 *       INSERT...ON CONFLICT. Entity IDs are preserved.</li>
 *   <li><b>Both IDs (hasCatalogId=true, hasEntityId=true):</b>
 *       Catalog+Entity scoped table with PRIMARY KEY (catalog_id, entity_id).
 *       Entity IDs are preserved, queries filtered by catalog.</li>
 * </ul>
 *
 * <h2>Important Notes</h2>
 * <ul>
 *   <li>The primary key is NEVER catalog_id alone</li>
 *   <li>When hasEntityId=false, entity IDs are generated on each load and not preserved</li>
 *   <li>This means entity references across hierarchies will not work for tables without entity_id</li>
 * </ul>
 */
public class AspectTableMapping
{
    private final AspectDef aspectDef;
    private final String tableName;
    private final Map<String, String> propertyToColumnMap;
    private final boolean hasCatalogId;
    private final boolean hasEntityId;

    /**
     * Creates a new AspectTableMapping with default flags (no catalog_id, has entity_id).
     * This maintains backward compatibility with the original behavior.
     *
     * @param aspectDef the AspectDef to map
     * @param tableName the name of the database table
     * @param propertyToColumnMap map from property names to column names
     */
    public AspectTableMapping(
        @NotNull AspectDef aspectDef,
        @NotNull String tableName,
        @NotNull Map<String, String> propertyToColumnMap)
    {
        this(aspectDef, tableName, propertyToColumnMap, false, true);
    }

    /**
     * Creates a new AspectTableMapping with explicit control over table structure.
     *
     * @param aspectDef the AspectDef to map
     * @param tableName the name of the database table
     * @param propertyToColumnMap map from property names to column names
     * @param hasCatalogId whether the table has a catalog_id column
     * @param hasEntityId whether the table has an entity_id column
     */
    public AspectTableMapping(
        @NotNull AspectDef aspectDef,
        @NotNull String tableName,
        @NotNull Map<String, String> propertyToColumnMap,
        boolean hasCatalogId,
        boolean hasEntityId)
    {
        this.aspectDef = aspectDef;
        this.tableName = tableName;
        this.propertyToColumnMap = new LinkedHashMap<>(propertyToColumnMap);
        this.hasCatalogId = hasCatalogId;
        this.hasEntityId = hasEntityId;
    }

    /**
     * Gets the AspectDef.
     *
     * @return the AspectDef
     */
    @NotNull
    public AspectDef aspectDef()
    {
        return aspectDef;
    }

    /**
     * Gets the table name.
     *
     * @return the table name
     */
    @NotNull
    public String tableName()
    {
        return tableName;
    }

    /**
     * Gets the property-to-column mapping.
     *
     * @return unmodifiable map from property names to column names
     */
    @NotNull
    public Map<String, String> propertyToColumnMap()
    {
        return Collections.unmodifiableMap(propertyToColumnMap);
    }

    /**
     * Returns whether the table has a catalog_id column.
     *
     * @return true if the table has a catalog_id column
     */
    public boolean hasCatalogId()
    {
        return hasCatalogId;
    }

    /**
     * Returns whether the table has an entity_id column.
     *
     * @return true if the table has an entity_id column
     */
    public boolean hasEntityId()
    {
        return hasEntityId;
    }
}
