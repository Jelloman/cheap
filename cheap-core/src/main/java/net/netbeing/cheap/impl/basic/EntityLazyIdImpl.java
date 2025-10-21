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
 * Entity implementation with a lazily-initialized global ID.
 *
 * @see Entity
 */
public class EntityLazyIdImpl implements Entity
{
    /** Lazily initialized global identifier. */
    private volatile UUID globalId;
    
    /**
     * Returns the globally unique identifier for this entity, creating it if necessary.
     * Uses double-checked locking for thread-safe lazy initialization.
     * 
     * @return the UUID identifying this entity globally
     */
    @Override
    public @NotNull UUID globalId()
    {
        if (globalId == null) {
            synchronized (this) {
                if (globalId == null) {
                    globalId = UUID.randomUUID();
                }
            }
        }
        return globalId;
    }

    /**
     * Compare to another entity. This implementation is final and only compares global IDs.
     * This will force the generation of the global ID.
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
        return Objects.equals(this.globalId(), entity.globalId());
    }

    /**
     * Generate this object's hash code. This implementation is final and only uses global ID.
     * This will force the generation of the global ID.
     *
     * @return hashCode of the globalId
     */
    @Override
    public int hashCode()
    {
        return Objects.hashCode(this.globalId());
    }
}
