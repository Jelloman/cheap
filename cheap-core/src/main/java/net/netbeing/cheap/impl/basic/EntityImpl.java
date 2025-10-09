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

import net.netbeing.cheap.model.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Basic implementation of an Entity reference with the bare minimum functionality
 * (storing and providing a global ID).
 *
 * @see Entity
 */
public class EntityImpl implements Entity
{
    /** The globally unique identifier for this entity. */
    private final UUID globalId;

    /**
     * Creates a new EntityBasicImpl with a randomly generated UUID.
     */
    public EntityImpl()
    {
        this(UUID.randomUUID());
    }

    /**
     * Creates a new EntityBasicImpl with the specified global ID.
     * 
     * @param globalId the UUID to use as the global identifier for this entity
     */
    public EntityImpl(UUID globalId)
    {
        Objects.requireNonNull(globalId, "EntityBasicImpl may not have a null UUID.");
        this.globalId = globalId;
    }

    /**
     * Returns the globally unique identifier for this entity.
     * 
     * @return the UUID identifying this entity globally
     */
    @Override
    public @NotNull UUID globalId()
    {
        return globalId;
    }

    /**
     * Compare to another entity. This implementation is final and only compares global IDs.
     *
     * @param o the object with which to compare.
     * @return true if o is an Entity and has the same globalId
     */
    @Override
    public final boolean equals(Object o)
    {
        if (!(o instanceof Entity entity)) {
            return false;
        }
        return Objects.equals(globalId, entity.globalId());
    }

    /**
     * Generate this object's hash code. This implementation is final and only uses global ID.
     *
     * @return hashCode of the globalId
     */
    @Override
    public final int hashCode()
    {
        return Objects.hashCode(globalId);
    }
}
