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

package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.HierarchyDef;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Basic implementation of a CatalogDef that defines the structure and properties
 * of a catalog in the Cheap data caching system.
 * <p>
 * This implementation automatically includes the default catalog hierarchies (catalog root and aspectage).
 * 
 * @see CatalogDef
 * @see CatalogSpecies
 * @see HierarchyDef
 */
public class CatalogDefImpl implements CatalogDef
{
    /** Map of hierarchy names to their definitions. */
    private final Map<String,HierarchyDef> hierarchyDefs = new LinkedHashMap<>();

    /** Map of hierarchy names to their definitions. */
    private final Map<String,AspectDef> aspectDefs = new LinkedHashMap<>();

    /**
     * Creates a new CatalogDefImpl with no HierarchyDefs or AspectDefs.
     */
    public CatalogDefImpl()
    {
    }

    /**
     * Creates a new CatalogDefImpl as a copy of another CatalogDef.
     *
     * @param other a CatalogDef
     */
    public CatalogDefImpl(@NotNull CatalogDef other)
    {
        this(other.hierarchyDefs(), other.aspectDefs());
    }

    /**
     * Creates a new CatalogDefImpl with copies of the provided hierarchy defs and/or aspect defs.
     * If the default hierarchies are not included, they will also be added.
     *
     * @param hierarchyDefs the hierarchyDefs to copy
     * @param aspectDefs the aspectDefs to copy
     */
    public CatalogDefImpl(Iterable<HierarchyDef> hierarchyDefs, Iterable<AspectDef> aspectDefs)
    {
        if (hierarchyDefs != null) {
            for (HierarchyDef hDef : hierarchyDefs) {
                this.hierarchyDefs.put(hDef.name(), hDef);
            }
        }
        if (aspectDefs != null) {
            for (AspectDef aDef : aspectDefs) {
                this.aspectDefs.put(aDef.name(), aDef);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Iterable<AspectDef> aspectDefs()
    {
        return Collections.unmodifiableCollection(aspectDefs.values());
    }

    /**
     * Returns an unmodifiable collection of all hierarchy defs in this catalog def.
     * 
     * @return collection of hierarchy definitions
     */
    @Override
    public @NotNull Iterable<HierarchyDef> hierarchyDefs()
    {
        return Collections.unmodifiableCollection(hierarchyDefs.values());
    }

    /**
     * Retrieves a hierarchy definition by name.
     * 
     * @param name the name of the hierarchy definition to retrieve
     * @return the hierarchy definition with the given name, or {@code null} if not found
     */
    @Override
    public HierarchyDef hierarchyDef(String name)
    {
        return hierarchyDefs.get(name);
    }

    /**
     * Retrieves an aspect definition by name.
     *
     * @param name the name of the aspect definition to retrieve
     * @return the aspect definition with the given name, or {@code null} if not found
     */
    @Override
    public AspectDef aspectDef(String name)
    {
        return aspectDefs.get(name);
    }

}
