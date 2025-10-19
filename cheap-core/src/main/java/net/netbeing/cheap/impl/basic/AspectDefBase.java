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
import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class for AspectDef implementations providing common functionality.
 * This class manages the basic structure of an aspect definition including its
 * name and property definitions.
 * <p>
 * Subclasses should implement the mutability methods (canAddProperties, canRemoveProperties)
 * and any modification operations (add, remove) as appropriate.
 *
 * @see AspectDef
 * @see ImmutableAspectDefImpl
 * @see MutableAspectDefImpl
 * @see PropertyDef
 */
public abstract class AspectDefBase extends EntityImpl implements AspectDef
{
    /** The name of this aspect definition. */
    final String name;

    /** Map of property names to property definitions. */
    final Map<String, PropertyDef> propertyDefs;

    /** Cached hash value (0 means not yet computed). */
    private volatile long cachedHash = 0;

    /**
     * Creates a new AspectDefBase with the specified name and empty property definitions.
     *
     * @param name the name of this aspect definition
     */
    protected AspectDefBase(@NotNull String name)
    {
        this(name, UUID.randomUUID(), new LinkedHashMap<>());
    }

    /**
     * Creates a new AspectDefBase with the specified name and empty property definitions.
     *
     * @param name the name of this aspect definition
     */
    protected AspectDefBase(@NotNull String name, @NotNull UUID globalId)
    {
        this(name, globalId, new LinkedHashMap<>());
    }

    /**
     * Creates a new AspectDefBase with the specified name and property definitions.
     *
     * @param name the name of this aspect definition
     * @param propertyDefs the map of property names to property definitions
     */
    protected AspectDefBase(@NotNull String name, @NotNull Map<String, PropertyDef> propertyDefs)
    {
        this(name, UUID.randomUUID(), propertyDefs);
    }

    /**
     * Creates a new AspectDefBase with the specified name and property definitions.
     *
     * @param name the name of this aspect definition
     * @param propertyDefs the map of property names to property definitions
     */
    protected AspectDefBase(@NotNull String name, @NotNull UUID globalId, @NotNull Map<String, PropertyDef> propertyDefs)
    {
        super(globalId);
        this.name = Objects.requireNonNull(name, "AspectDefs must have a non-null name.");
        this.propertyDefs = Objects.requireNonNull(propertyDefs, "Provided property defs cannot be null.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull String name()
    {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Collection<? extends PropertyDef> propertyDefs()
    {
        return propertyDefs.values();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return propertyDefs.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyDef propertyDef(@NotNull String propName)
    {
        return propertyDefs.get(propName);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation caches the computed hash value for improved performance.
     * The cache uses a volatile long variable with 0 as the sentinel value indicating
     * "not yet computed". This is safe because the FNV-1a hash algorithm never produces
     * a hash value of 0 for any non-empty input.
     * <p>
     * Note: For mutable AspectDef implementations, the cached hash may become stale
     * if properties are added or removed. Subclasses that allow modification should
     * call {@link #invalidateHashCache()} when the aspect definition is modified.
     */
    @Override
    public long hash()
    {
        long result = cachedHash;
        if (result == 0) {
            // Compute hash using default implementation from AspectDef interface
            result = AspectDef.super.hash();
            cachedHash = result;
        }
        return result;
    }

    /**
     * Invalidates the cached hash value, forcing it to be recomputed on the next call to hash().
     * This should be called by mutable subclasses whenever the aspect definition is modified.
     */
    protected void invalidateHashCache()
    {
        cachedHash = 0;
    }
}
