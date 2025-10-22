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
import net.netbeing.cheap.model.EntitySetHierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Basic implementation of an EntitySetHierarchy using a LinkedHashSet.
 * This hierarchy type represents a possibly-ordered collection of unique entities
 * corresponding to the ENTITY_SET (ES) hierarchy type in Cheap.
 * <p>
 * This implementation uses composition with an internal LinkedHashSet to provide
 * efficient entity membership testing and duplicate prevention while implementing
 * the EntitySetHierarchy interface.
 *
 * @see EntitySetHierarchy
 * @see Entity
 * @see HierarchyDef
 */
@SuppressWarnings("unused")
public class EntitySetHierarchyImpl implements EntitySetHierarchy
{
    /** The catalog containing this hierarchy. */
    private final Catalog catalog;

    /** The name of this hierarchy in the catalog. */
    private final String name;

    /** The version number of this hierarchy. */
    private final long version;

    /** The internal set storing unique entities. */
    private final Set<Entity> entities;

    /**
     * Creates a new EntitySetHierarchyImpl with the specified hierarchy definition.
     * Package-private for use by CatalogImpl factory methods.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     */
    protected EntitySetHierarchyImpl(@NotNull Catalog catalog, @NotNull String name)
    {
        this(catalog, name, 0L);
    }

    /**
     * Creates a new EntitySetHierarchyImpl with the specified hierarchy definition and
     * initial capacity. Public for use by CheapFactory.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     * @param initialCapacity initial capacity of set
     */
    protected EntitySetHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, int initialCapacity)
    {
        this(catalog, name, initialCapacity, 0L);
    }

    /**
     * Creates a new EntitySetHierarchyImpl with the specified hierarchy definition and version.
     * Public for use by CheapFactory.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     * @param version the version number of this hierarchy
     */
    protected EntitySetHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, long version)
    {
        this.catalog = catalog;
        this.name = name;
        this.version = version;
        this.entities = new LinkedHashSet<>();
    }

    /**
     * Creates a new EntitySetHierarchyImpl with the specified hierarchy definition,
     * initial capacity, and version. Public for use by CheapFactory.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     * @param initialCapacity initial capacity of set
     * @param version the version number of this hierarchy
     */
    protected EntitySetHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, int initialCapacity, long version)
    {
        this.catalog = catalog;
        this.name = name;
        this.version = version;
        this.entities = LinkedHashSet.newLinkedHashSet(initialCapacity);
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
        return HierarchyType.ENTITY_SET;
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

    // Set interface delegation methods

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
    public boolean contains(Object o)
    {
        return entities.contains(o);
    }

    @Override
    public @NotNull Iterator<Entity> iterator()
    {
        return entities.iterator();
    }

    @Override
    public Object @NotNull [] toArray()
    {
        return entities.toArray();
    }

    @Override
    public <T> T @NotNull [] toArray(T @NotNull [] a)
    {
        return entities.toArray(a);
    }

    @Override
    public boolean add(Entity entity)
    {
        return entities.add(entity);
    }

    @Override
    public boolean remove(Object o)
    {
        return entities.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c)
    {
        return entities.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Entity> c)
    {
        return entities.addAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c)
    {
        return entities.retainAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c)
    {
        return entities.removeAll(c);
    }

    @Override
    public void clear()
    {
        entities.clear();
    }

    @Override
    public @NotNull Spliterator<Entity> spliterator()
    {
        return entities.spliterator();
    }

    @Override
    public <T> T[] toArray(@NotNull IntFunction<T[]> generator)
    {
        return entities.toArray(generator);
    }

    @Override
    public boolean removeIf(@NotNull Predicate<? super Entity> filter)
    {
        return entities.removeIf(filter);
    }

    @Override
    public @NotNull Stream<Entity> stream()
    {
        return entities.stream();
    }

    @Override
    public @NotNull Stream<Entity> parallelStream()
    {
        return entities.parallelStream();
    }

    @Override
    public void forEach(Consumer<? super Entity> action)
    {
        entities.forEach(action);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof EntitySetHierarchyImpl that)) return false;
        return entities.equals(that.entities);
    }

    @Override
    public int hashCode()
    {
        return entities.hashCode();
    }
}
