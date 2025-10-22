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
import net.netbeing.cheap.model.EntityListHierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Basic implementation of an EntityListHierarchy using an ArrayList.
 * This hierarchy type represents an ordered list of entities that may contain
 * duplicates, corresponding to the ENTITY_LIST (EL) hierarchy type in Cheap.
 * <p>
 * This implementation uses composition with an internal ArrayList to provide
 * indexed access and maintain insertion order while implementing the
 * EntityListHierarchy interface.
 *
 * @see EntityListHierarchy
 * @see Entity
 * @see HierarchyDef
 */
@SuppressWarnings("unused")
public class EntityListHierarchyImpl implements EntityListHierarchy
{
    /** The catalog containing this hierarchy. */
    private final Catalog catalog;

    /** The name of this hierarchy in the catalog. */
    private final String name;

    /** The version number of this hierarchy. */
    private final long version;

    /** The internal list storing entities. */
    private final List<Entity> entities;

    /**
     * Creates a new EntityListHierarchyImpl with the specified hierarchy definition.
     * Package-private for use by CatalogImpl factory methods.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     */
    protected EntityListHierarchyImpl(@NotNull Catalog catalog, @NotNull String name)
    {
        this(catalog, name, 0L);
    }

    /**
     * Creates a new EntityListHierarchyImpl with the specified hierarchy definition and version.
     * Public for use by CheapFactory.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     * @param version the version number of this hierarchy
     */
    protected  EntityListHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, long version)
    {
        this.catalog = catalog;
        this.name = name;
        this.version = version;
        this.entities = new ArrayList<>();
    }

    /**
     * Creates a new EntityListHierarchyImpl with the specified hierarchy definition,
     * initial capacity, and version. Public for use by CheapFactory.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name the name of this hierarchy in the catalog
     * @param initialCapacity initial capacity of list
     * @param version the version number of this hierarchy
     */
    protected  EntityListHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, int initialCapacity, long version)
    {
        this.catalog = catalog;
        this.name = name;
        this.version = version;
        this.entities = new ArrayList<>(initialCapacity);
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
        return HierarchyType.ENTITY_LIST;
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

    // List interface delegation methods

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
        return new HashSet<>(entities).containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Entity> c)
    {
        return entities.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends Entity> c)
    {
        return entities.addAll(index, c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c)
    {
        return entities.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c)
    {
        return entities.retainAll(c);
    }

    @Override
    public void clear()
    {
        entities.clear();
    }

    @Override
    public Entity get(int index)
    {
        return entities.get(index);
    }

    @Override
    public Entity set(int index, Entity element)
    {
        return entities.set(index, element);
    }

    @Override
    public void add(int index, Entity element)
    {
        entities.add(index, element);
    }

    @Override
    public Entity remove(int index)
    {
        return entities.remove(index);
    }

    @Override
    public int indexOf(Object o)
    {
        return entities.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return entities.lastIndexOf(o);
    }

    @Override
    public @NotNull ListIterator<Entity> listIterator()
    {
        return entities.listIterator();
    }

    @Override
    public @NotNull ListIterator<Entity> listIterator(int index)
    {
        return entities.listIterator(index);
    }

    @Override
    public @NotNull List<Entity> subList(int fromIndex, int toIndex)
    {
        return entities.subList(fromIndex, toIndex);
    }

    @Override
    public void replaceAll(@NotNull UnaryOperator<Entity> operator)
    {
        entities.replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super Entity> c)
    {
        entities.sort(c);
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
}
