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

package net.netbeing.cheap.model;

import net.netbeing.cheap.util.CheapHasher;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A CatalogDef defines the structure and properties of a catalog.
 * This is a purely informational class.
 *
 * @see AspectDef
 * @see HierarchyDef
 */
public interface CatalogDef
{
    /**
     * Returns a read-only collection of the aspect definitions that are typically found
     * in a catalog with this definition. Catalogs flagged as "strict" will only contain
     * these Aspects; otherwise they may contain additional types of Aspects.
     *
     * @return collection of aspect definitions
     */
    @NotNull Iterable<AspectDef> aspectDefs();

    /**
     * Returns  a read-only collection of hierarchy definitions that are typically found
     * in a catalog with this definition. Catalogs flagged as "strict" will only contain
     * these Hierarchies; otherwise they may contain additional Hierarchies.
     *
     * @return collection of hierarchy definitions
     */
    @NotNull Iterable<HierarchyDef> hierarchyDefs();

    /**
     * Retrieves a hierarchy definition by name.
     *
     * @param name the name of the hierarchy definition to retrieve
     * @return the hierarchy definition with the given name, or {@code null} if not found
     */
    HierarchyDef hierarchyDef(String name);

    /**
     * Retrieves an aspect definition by name.
     *
     * @param name the name of the aspect definition to retrieve
     * @return the aspect definition with the given name, or {@code null} if not found
     */
    AspectDef aspectDef(String name);

    /**
     * Generate a Cheap-specific FNV-1a hash of this CatalogDef.
     * This hash should be consistent across all Cheap implementations.
     *
     * <P>Implementations of this interface should probably cache the result of this
     * default method for improved performance.</P>
     *
     * @return a 64-bit hash value
     */
    default long hash()
    {
        CheapHasher hasher = new CheapHasher();
        for (AspectDef aDef : aspectDefs()) {
            hasher.update(aDef.hash());
        }
        for (HierarchyDef hDef : hierarchyDefs()) {
            hasher.update(hDef.hash());
        }
        return hasher.getHash();
    }

}
