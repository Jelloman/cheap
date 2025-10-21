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

import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.EntityDirectoryHierarchy;
import net.netbeing.cheap.model.HierarchyDef;
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
 * Basic implementation of an EntityDirectoryHierarchy using a LinkedHashMap.
 * This hierarchy type represents a string-to-entity mapping, corresponding
 * to the ENTITY_DIR (ED) hierarchy type in Cheap.
 * <p>
 * This implementation uses composition with an internal LinkedHashMap to provide
 * efficient name-based entity lookup while implementing the EntityDirectoryHierarchy
 * interface.
 *
 * @see EntityDirectoryHierarchy
 * @see Entity
 * @see HierarchyDef
 */
public class EntityDirectoryHierarchyImpl implements EntityDirectoryHierarchy
{
    /** The catalog containing this hierarchy. */
    private final Catalog catalog;

    /** The name of this hierarchy in the catalog. */
    private final String name;

    /** The version number of this hierarchy. */
    private final long version;

    /** The internal map storing string-to-entity mappings. */
    private final Map<String, Entity> entities;

    /**
     * Creates a new EntityDirectoryHierarchyImpl with the specified hierarchy definition.
     *
     * @param catalog the owning catalog
     * @param name the name of this hierarchy in the catalog
     */
    public EntityDirectoryHierarchyImpl(@NotNull Catalog catalog, @NotNull String name)
    {
        this(catalog, name, 0L);
    }

    /**
     * Creates a new EntityDirectoryHierarchyImpl with the specified hierarchy definition and version.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     * @param version the version number of this hierarchy
     */
    public EntityDirectoryHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, long version)
    {
        this.catalog = catalog;
        this.name = name;
        this.version = version;
        this.entities = new LinkedHashMap<>();
        catalog.addHierarchy(this);
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
        return HierarchyType.ENTITY_DIR;
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
        return entities.size();
    }

    @Override
    public boolean isEmpty()
    {
        return entities.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return entities.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return entities.containsValue(value);
    }

    @Override
    public Entity get(Object key)
    {
        return entities.get(key);
    }

    @Override
    public Entity put(String key, Entity value)
    {
        return entities.put(key, value);
    }

    @Override
    public Entity remove(Object key)
    {
        return entities.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends Entity> m)
    {
        entities.putAll(m);
    }

    @Override
    public void clear()
    {
        entities.clear();
    }

    @Override
    public @NotNull Set<String> keySet()
    {
        return entities.keySet();
    }

    @Override
    public @NotNull Collection<Entity> values()
    {
        return entities.values();
    }

    @Override
    public @NotNull Set<Entry<String, Entity>> entrySet()
    {
        return entities.entrySet();
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public Entity getOrDefault(Object key, Entity defaultValue)
    {
        return entities.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Entity> action)
    {
        entities.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super Entity, ? extends Entity> function)
    {
        entities.replaceAll(function);
    }

    @Override
    public Entity putIfAbsent(String key, Entity value)
    {
        return entities.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value)
    {
        return entities.remove(key, value);
    }

    @Override
    public boolean replace(String key, Entity oldValue, Entity newValue)
    {
        return entities.replace(key, oldValue, newValue);
    }

    @Override
    public Entity replace(String key, Entity value)
    {
        return entities.replace(key, value);
    }

    @Override
    public Entity computeIfAbsent(String key, @NotNull Function<? super String, ? extends Entity> mappingFunction)
    {
        return entities.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public Entity computeIfPresent(String key, @NotNull BiFunction<? super String, ? super Entity, ? extends Entity> remappingFunction)
    {
        return entities.computeIfPresent(key, remappingFunction);
    }

    @Override
    public Entity compute(String key, @NotNull BiFunction<? super String, ? super Entity, ? extends Entity> remappingFunction)
    {
        return entities.compute(key, remappingFunction);
    }

    @Override
    public Entity merge(String key, @NotNull Entity value, @NotNull BiFunction<? super Entity, ? super Entity, ? extends Entity> remappingFunction)
    {
        return entities.merge(key, value, remappingFunction);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof EntityDirectoryHierarchyImpl)) return false;
        EntityDirectoryHierarchyImpl that = (EntityDirectoryHierarchyImpl) o;
        return entities.equals(that.entities);
    }

    @Override
    public int hashCode()
    {
        return entities.hashCode();
    }
}
