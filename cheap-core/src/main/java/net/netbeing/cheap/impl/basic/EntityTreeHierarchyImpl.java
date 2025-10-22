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
import net.netbeing.cheap.model.EntityTreeHierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Basic implementation of an EntityTreeHierarchy that represents a tree structure
 * with string-to-entity or string-to-node mappings, corresponding to the
 * ENTITY_TREE (ET) hierarchy type in Cheap.
 * <p>
 * This implementation provides both regular nodes (which can have children) and
 * leaf nodes (which cannot have children) to build tree structures.
 *
 * @see EntityTreeHierarchy
 * @see Entity
 * @see HierarchyDef
 */
@SuppressWarnings("unused")
public class EntityTreeHierarchyImpl implements EntityTreeHierarchy
{
    /**
     * The catalog containing this hierarchy.
     */
    private final Catalog catalog;
    /**
     * The name of this hierarchy in the catalog.
     */
    private final String name;
    /**
     * The version number of this hierarchy.
     */
    private final long version;
    /**
     * The root node of this tree hierarchy.
     */
    private Node root;

    /**
     * Creates a new EntityTreeHierarchyImpl with the specified hierarchy definition and a root with a null entity.
     * Package-private for use by CatalogImpl factory methods.
     *
     * @param catalog the catalog containing this hierarchy
     * @param name    the name of this hierarchy in the catalog
     */
    protected EntityTreeHierarchyImpl(@NotNull Catalog catalog, @NotNull String name)
    {
        this(catalog, name, new NodeImpl(null), 0L);
    }

    /**
     * Creates a new EntityTreeHierarchyImpl with the specified hierarchy definition and root node.
     * Public for use by CheapFactory.
     *
     * @param catalog  the catalog containing this hierarchy
     * @param name     the name of this hierarchy in the catalog
     * @param rootNode the node to use as the root of the tree
     */
    protected EntityTreeHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, Node rootNode)
    {
        this(catalog, name, rootNode, 0L);
    }

    /**
     * Creates a new EntityTreeHierarchyImpl with the specified hierarchy definition, root node, and version.
     * Public for use by CheapFactory.
     *
     * @param catalog  the catalog containing this hierarchy
     * @param name     the name of this hierarchy in the catalog
     * @param rootNode the node to use as the root of the tree
     * @param version  the version number of this hierarchy
     */
    protected EntityTreeHierarchyImpl(@NotNull Catalog catalog, @NotNull String name, Node rootNode, long version)
    {
        this.catalog = catalog;
        this.name = name;
        this.version = version;
        this.root = rootNode;
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
        return HierarchyType.ENTITY_TREE;
    }

    /**
     * Returns the root node of this tree hierarchy.
     *
     * @return the root node of the tree
     */
    @Override
    public @NotNull Node root()
    {
        return root;
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

    /**
     * Set a new root node.
     *
     * @param newRoot the new root
     */
    public void setRoot(@NotNull Node newRoot)
    {
        this.root = newRoot;
    }

    /**
     * Implementation of a tree node that can have child nodes.
     * This node type uses composition with an internal map to provide string-to-node mappings.
     */
    protected static class NodeImpl implements Node
    {
        /**
         * The parent node, or null for root nodes.
         */
        private final Node parent;
        /**
         * The internal map storing child nodes.
         */
        private final Map<String, Node> children;
        /**
         * The entity value stored at this node.
         */
        private Entity value;

        /**
         * Creates a new NodeImpl with the specified entity value and no parent.
         *
         * @param value the entity value to store at this node
         */
        protected NodeImpl(Entity value)
        {
            this(value, null);
        }

        /**
         * Creates a new NodeImpl with the specified entity value and parent.
         *
         * @param value  the entity value to store at this node
         * @param parent the parent node, or null for root nodes
         */
        public NodeImpl(Entity value, Node parent)
        {
            this.value = value;
            this.parent = parent;
            this.children = new LinkedHashMap<>();
        }

        /**
         * Returns whether this node is a leaf node.
         *
         * @return {@code true} only if this node has no children
         */
        @Override
        public boolean isLeaf()
        {
            return children.isEmpty();
        }

        /**
         * Returns the parent node of this node.
         *
         * @return the parent node, or {@code null} if this is a root node
         */
        @Override
        public Node getParent()
        {
            return parent;
        }

        /**
         * Returns the entity value stored at this node.
         *
         * @return the entity value at this node
         */
        @Override
        public Entity value()
        {
            return value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValue(Entity entity)
        {
            value = entity;
        }

        // Map interface delegation methods

        @Override
        public int size()
        {
            return children.size();
        }

        @Override
        public boolean isEmpty()
        {
            return children.isEmpty();
        }

        @Override
        public boolean containsKey(Object key)
        {
            return children.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value)
        {
            return children.containsValue(value);
        }

        @Override
        public Node get(Object key)
        {
            return children.get(key);
        }

        @Override
        public Node put(String key, Node value)
        {
            return children.put(key, value);
        }

        @Override
        public Node remove(Object key)
        {
            return children.remove(key);
        }

        @Override
        public void putAll(@NotNull Map<? extends String, ? extends Node> m)
        {
            children.putAll(m);
        }

        @Override
        public void clear()
        {
            children.clear();
        }

        @Override
        public @NotNull Set<String> keySet()
        {
            return children.keySet();
        }

        @Override
        public @NotNull Collection<Node> values()
        {
            return children.values();
        }

        @Override
        public @NotNull Set<Entry<String, Node>> entrySet()
        {
            return children.entrySet();
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        @Override
        public Node getOrDefault(Object key, Node defaultValue)
        {
            return children.getOrDefault(key, defaultValue);
        }

        @Override
        public void forEach(BiConsumer<? super String, ? super Node> action)
        {
            children.forEach(action);
        }

        @Override
        public void replaceAll(BiFunction<? super String, ? super Node, ? extends Node> function)
        {
            children.replaceAll(function);
        }

        @Override
        public Node putIfAbsent(String key, Node value)
        {
            return children.putIfAbsent(key, value);
        }

        @Override
        public boolean remove(Object key, Object value)
        {
            return children.remove(key, value);
        }

        @Override
        public boolean replace(String key, Node oldValue, Node newValue)
        {
            return children.replace(key, oldValue, newValue);
        }

        @Override
        public Node replace(String key, Node value)
        {
            return children.replace(key, value);
        }

        @Override
        public Node computeIfAbsent(String key, @NotNull Function<? super String, ? extends Node> mappingFunction)
        {
            return children.computeIfAbsent(key, mappingFunction);
        }

        @Override
        public Node computeIfPresent(String key, @NotNull BiFunction<? super String, ? super Node, ? extends Node> remappingFunction)
        {
            return children.computeIfPresent(key, remappingFunction);
        }

        @Override
        public Node compute(String key, @NotNull BiFunction<? super String, ? super Node, ? extends Node> remappingFunction)
        {
            return children.compute(key, remappingFunction);
        }

        @Override
        public Node merge(String key, @NotNull Node value,
                          @NotNull BiFunction<? super Node, ? super Node, ? extends Node> remappingFunction)
        {
            return children.merge(key, value, remappingFunction);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof NodeImpl node)) return false;
            return children.equals(node.children);
        }

        @Override
        public int hashCode()
        {
            return children.hashCode();
        }
    }

    /**
     * Implementation of a leaf tree node that cannot have child nodes.
     * This node type implements the Map interface by extending AbstractMap and
     * providing an empty entry set to indicate no children are allowed.
     */
    protected static class LeafNodeImpl extends AbstractMap<String, Node> implements Node
    {
        /**
         * The parent node, or null for root leaf nodes.
         */
        private final Node parent;
        /**
         * The entity value stored at this leaf node.
         */
        private Entity value;

        /**
         * Creates a new LeafNodeImpl with the specified entity value and no parent.
         *
         * @param value the entity value to store at this leaf node
         */
        protected LeafNodeImpl(Entity value)
        {
            this(value, null);
        }

        /**
         * Creates a new LeafNodeImpl with the specified entity value and parent.
         *
         * @param value  the entity value to store at this leaf node
         * @param parent the parent node, or null for root leaf nodes
         */
        protected LeafNodeImpl(Entity value, Node parent)
        {
            this.value = value;
            this.parent = parent;
        }

        /**
         * Returns whether this node is a leaf node.
         *
         * @return {@code true} as this implementation cannot have children
         */
        @Override
        public boolean isLeaf()
        {
            return true;
        }

        /**
         * Returns the parent node of this leaf node.
         *
         * @return the parent node, or {@code null} if this is a root leaf node
         */
        @Override
        public Node getParent()
        {
            return parent;
        }

        /**
         * Returns the entity value stored at this leaf node.
         *
         * @return the entity value at this leaf node
         */
        @Override
        public Entity value()
        {
            return value;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void setValue(Entity entity)
        {
            value = entity;
        }

        /**
         * Returns an empty set of entries as leaf nodes cannot have children.
         *
         * @return an empty set
         */
        @Override
        public @NotNull Set<Entry<String, Node>> entrySet()
        {
            return Collections.emptySet();
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof LeafNodeImpl that)) return false;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(value);
        }
    }
}
