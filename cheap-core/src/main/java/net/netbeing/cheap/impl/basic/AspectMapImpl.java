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
import net.netbeing.cheap.model.AspectMap;
import net.netbeing.cheap.model.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Basic implementation of an AspectMap that maps entities to aspects of a single type.
 * <p>
 * This class uses composition with an internal LinkedHashMap to provide efficient
 * entity-to-aspect lookups while implementing the {@link AspectMap} interface.
 *
 * @see AspectMap
 */
public class AspectMapImpl implements AspectMap
{
    /** The aspect definition for the aspects stored in this map. */
    private final AspectDef aspectDef;

    /** The internal map storing entity-to-aspect mappings. */
    private final Map<Entity, Aspect> aspects;

    /**
     * Creates a new AspectMapImpl to contain the given AspectDef.
     *
     * @param aspectDef the aspect definition for aspects in this map
     */
    protected AspectMapImpl(@NotNull AspectDef aspectDef)
    {
        this.aspectDef = aspectDef;
        this.aspects = new LinkedHashMap<>();
    }

    /**
     * Returns the aspect definition for aspects stored in this map.
     * 
     * @return the aspect definition
     */
    @Override
    public AspectDef aspectDef()
    {
        return aspectDef;
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
