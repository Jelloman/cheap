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

/**
 * Defines the metadata and characteristics of a hierarchy within the Cheap data model.
 * A hierarchy definition specifies the type, name, and mutability constraints of
 * a hierarchy instance.
 * 
 * <p>In the Cheap model, hierarchies provide the structural organization for entities
 * and aspects within a catalog. The HierarchyDef serves as the schema definition
 * that determines how a hierarchy behaves and what operations are permitted on it.</p>
 */
public interface HierarchyDef
{
    /**
     * Returns the unique name identifier for this hierarchy definition.
     * 
     * @return the hierarchy name, never null
     */
    String name();

    /**
     * Returns the type of hierarchy this definition describes.
     * The type determines the structure and behavior of hierarchy instances
     * created from this definition.
     * 
     * @return the hierarchy type, never null
     */
    HierarchyType type();

    /**
     * Generate a Cheap-specific FNV-1a hash of this HierarchyDef.
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
        hasher.update(name());
        hasher.update(type().typeCode());
        return hasher.getHash();
    }

}
