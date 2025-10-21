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

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.AspectMapHierarchy;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.model.HierarchyType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Basic implementation of an AspectMapHierarchy that maps entities to aspects.
 * This hierarchy type stores a mapping from entity IDs to aspects of a single type.
 * <p>
 * This class uses composition with an internal LinkedHashMap to provide efficient
 * entity-to-aspect lookups while implementing the {@link AspectMapHierarchy} interface.
 *
 * @see AspectMapHierarchy
 * @see Hierarchy
 */
public class AspectMapHierarchyImpl implements AspectMapHierarchy
{
    /** The catalog containing this hierarchy. */
    private final Catalog catalog;

    /** The name of this hierarchy in the catalog. */
    private final String name;

    /** The aspect definition for the aspects stored in this hierarchy. */
    private final AspectDef aspectDef;

    /** The version number of this hierarchy. */
    private final long version;

    /** The internal map storing entity-to-aspect mappings. */
    private final Map<Entity, Aspect> aspects;

    /**
     * Creates a new AspectMapHierarchyImpl to contain the given AspectDef.
     * AA new HierarchyDef will be constructed.
     *
     * @param aspectDef the aspect definition for aspects in this hierarchy
     */
    public AspectMapHierarchyImpl(@NotNull Catalog catalog, @NotNull AspectDef aspectDef)
    {
        this(catalog, aspectDef, 0L);
    }

    /**
     * Creates a new AspectMapHierarchyImpl to contain the given AspectDef with version.
     * A new HierarchyDef will be constructed.
     *
     * @param catalog the catalog containing this hierarchy
     * @param aspectDef the aspect definition for aspects in this hierarchy
     * @param version the version number of this hierarchy
     */
    public AspectMapHierarchyImpl(@NotNull Catalog catalog, @NotNull AspectDef aspectDef, long version)
    {
        this.catalog = catalog;
        this.aspectDef = aspectDef;
        this.version = version;
        this.name = aspectDef.name();
        this.aspects = new LinkedHashMap<>();
        catalog.addHierarchy(this);
    }

    /**
     * Returns the aspect definition for aspects stored in this hierarchy.
     * 
     * @return the aspect definition
     */
    @Override
    public AspectDef aspectDef()
    {
        return aspectDef;
    }

    /**
     * Returns the Catalog that owns this hierarchy.
     *
     * @return the parent catalog
     */
    @Override
    public @NotNull Catalog catalog()
    {
        return catalog;
    }

    /**
     * Returns the name of this hierarchy in the catalog.
     *
     * @return the name of the hierarchy
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
    public @NotNull HierarchyType type()
    {
        return HierarchyType.ASPECT_MAP;
    }

    /**
     * Returns the version number of this hierarchy.
     *
     * @return the version number
     */
    @Override
    public long version()
    {
        return version;
    }

    // Map interface delegation methods

    @Override
    public int size()
    {
        return aspects.size();
    }

    @Override
    public boolean isEmpty()
    {
        return aspects.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return aspects.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return aspects.containsValue(value);
    }

    @Override
    public Aspect get(Object key)
    {
        return aspects.get(key);
    }

    @Override
    public Aspect put(Entity key, Aspect value)
    {
        return aspects.put(key, value);
    }

    @Override
    public Aspect remove(Object key)
    {
        return aspects.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends Entity, ? extends Aspect> m)
    {
        aspects.putAll(m);
    }

    @Override
    public void clear()
    {
        aspects.clear();
    }

    @Override
    public @NotNull Set<Entity> keySet()
    {
        return aspects.keySet();
    }

    @Override
    public @NotNull Collection<Aspect> values()
    {
        return aspects.values();
    }

    @Override
    public @NotNull Set<Entry<Entity, Aspect>> entrySet()
    {
        return aspects.entrySet();
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public Aspect getOrDefault(Object key, Aspect defaultValue)
    {
        return aspects.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super Entity, ? super Aspect> action)
    {
        aspects.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super Entity, ? super Aspect, ? extends Aspect> function)
    {
        aspects.replaceAll(function);
    }

    @Override
    public Aspect putIfAbsent(Entity key, Aspect value)
    {
        return aspects.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value)
    {
        return aspects.remove(key, value);
    }

    @Override
    public boolean replace(Entity key, Aspect oldValue, Aspect newValue)
    {
        return aspects.replace(key, oldValue, newValue);
    }

    @Override
    public Aspect replace(Entity key, Aspect value)
    {
        return aspects.replace(key, value);
    }

    @Override
    public Aspect computeIfAbsent(Entity key, @NotNull Function<? super Entity, ? extends Aspect> mappingFunction)
    {
        return aspects.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public Aspect computeIfPresent(Entity key, @NotNull BiFunction<? super Entity, ? super Aspect, ? extends Aspect> remappingFunction)
    {
        return aspects.computeIfPresent(key, remappingFunction);
    }

    @Override
    public Aspect compute(Entity key, @NotNull BiFunction<? super Entity, ? super Aspect, ? extends Aspect> remappingFunction)
    {
        return aspects.compute(key, remappingFunction);
    }

    @Override
    public Aspect merge(Entity key, @NotNull Aspect value, @NotNull BiFunction<? super Aspect, ? super Aspect, ? extends Aspect> remappingFunction)
    {
        return aspects.merge(key, value, remappingFunction);
    }
}
