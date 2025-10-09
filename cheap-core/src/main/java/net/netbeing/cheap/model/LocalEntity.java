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

import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;

/**
 * LocalEntity keeps track of one or more Catalog(s) that contain its Aspects.
 * LocalEntity references are therefore sufficient to access Aspects, without
 * needing a Catalog reference.
 */
public interface LocalEntity extends Entity
{
    /**
     * Return the set of Catalogs that this entity has Aspects in.
     *
     * @return an Iterable of Catalogs (which commonly will only have one element)
     */
    Iterable<Catalog> catalogs();

    /**
     * Retrieves a specific aspect attached to this entity by its definition.
     *
     * <p>If no aspect of the specified type is attached to this entity in any
     * of its catalogs, this method returns null.</p>
     *
     * <p>The default implementation calls the {@link #getAspect(AspectDef, Catalog) getAspect}
     * method with each of the catalogs returned by the {@link #catalogs() catalogs} method,
     * in order, and returns the first match.</p>
     *
     * @param def the aspect definition to look up, must not be null
     * @return the aspect instance matching the definition, or null if not found
     */
    default Aspect getAspect(@NotNull AspectDef def)
    {
        for (Catalog cat : catalogs()) {
            Aspect a = getAspect(def, cat);
            if (a != null) {
                return a;
            }
        }
        return null;
    }

    /**
     * Adds an aspect to this entity. If the aspect already has an entity specified
     * and is flagged as non-transferable, an exception will be thrown.
     *
     * <p>The default implementation iterates through the catalogs returned by
     * the {@link #catalogs() catalogs} method and inserts the Aspect into the
     * first Catalog that contains the matching AspectDef. If no catalog is found,
     * an exception is thrown.</p>
     *
     * @param aspect the aspect to attach to the entity
     */
    default void add(@NotNull Aspect aspect)
    {
        if (aspect.entity() != null && aspect.entity() != null && !aspect.isTransferable()) {
            throw new IllegalStateException("An Aspect flagged as non-transferable may not be reassigned to a different entity.");
        }
        for (Catalog cat : catalogs()) {
            AspectMapHierarchy aspectMap = cat.aspects(aspect.def());
            if (aspectMap != null) {
                aspect.setEntity(this);
                aspectMap.add(aspect);
                return;
            }
        }
        throw new NoSuchElementException("No Catalog was found to store the " + aspect.def().name() + " aspect in.");
    }

}
