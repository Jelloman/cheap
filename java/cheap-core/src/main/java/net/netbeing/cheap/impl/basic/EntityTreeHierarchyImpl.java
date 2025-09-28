package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.EntityTreeHierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
public class EntityTreeHierarchyImpl implements EntityTreeHierarchy
{
    /**
     * Implementation of a tree node that can have child nodes.
     * This node type extends HashMap to provide string-to-node mappings.
     */
    public static class NodeImpl extends LinkedHashMap<String, Node> implements Node
    {
        /** The entity value stored at this node. */
        private final Entity value;
        
        /** The parent node, or null for root nodes. */
        private final Node parent;

        /**
         * Creates a new NodeImpl with the specified entity value and no parent.
         * 
         * @param value the entity value to store at this node
         */
        public NodeImpl(Entity value)
        {
            this(value, null);
        }

        /**
         * Creates a new NodeImpl with the specified entity value and parent.
         * 
         * @param value the entity value to store at this node
         * @param parent the parent node, or null for root nodes
         */
        public NodeImpl(Entity value, Node parent)
        {
            this.value = value;
            this.parent = parent;
        }

        /**
         * Returns whether this node is a leaf node.
         * 
         * @return {@code true} only if this node has no children
         */
        @Override
        public boolean isLeaf()
        {
            return isEmpty();
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
    }

    /**
     * Implementation of a leaf tree node that cannot have child nodes.
     * This node type extends AbstractMap but provides an empty entry set.
     */
    public static class LeafNodeImpl extends AbstractMap<String, Node> implements Node
    {
        /** The entity value stored at this leaf node. */
        private final Entity value;
        
        /** The parent node, or null for root leaf nodes. */
        private final Node parent;

        /**
         * Creates a new LeafNodeImpl with the specified entity value and no parent.
         * 
         * @param value the entity value to store at this leaf node
         */
        public LeafNodeImpl(Entity value)
        {
            this(value, null);
        }

        /**
         * Creates a new LeafNodeImpl with the specified entity value and parent.
         * 
         * @param value the entity value to store at this leaf node
         * @param parent the parent node, or null for root leaf nodes
         */
        public LeafNodeImpl(Entity value, Node parent)
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
         * Returns an empty set of entries as leaf nodes cannot have children.
         * 
         * @return an empty set
         */
        @Override
        public @NotNull Set<Entry<String, Node>> entrySet()
        {
            return Collections.emptySet();
        }
    }

    /** The catalog containing this hierarchy. */
    private final Catalog catalog;

    /** The hierarchy definition describing this entity tree. */
    private final HierarchyDef def;

    /** The version number of this hierarchy. */
    private final long version;

    /** The root node of this tree hierarchy. */
    private Node root;

    /**
     * Creates a new EntityTreeHierarchyImpl with the specified hierarchy definition and a root with a null entity.
     *
     * @param def the hierarchy definition for this entity tree
     */
    public EntityTreeHierarchyImpl(@NotNull Catalog catalog, @NotNull HierarchyDef def)
    {
        this(catalog, def, new NodeImpl(null), 0L);
    }

    /**
     * Creates a new EntityTreeHierarchyImpl with the specified hierarchy definition and root entity.
     *
     * @param def the hierarchy definition for this entity tree
     * @param rootEntity the entity to use as the root of the tree
     */
    public EntityTreeHierarchyImpl(@NotNull Catalog catalog, @NotNull HierarchyDef def, Entity rootEntity)
    {
        this(catalog, def, new NodeImpl(rootEntity), 0L);
    }

    /**
     * Creates a new EntityTreeHierarchyImpl with the specified hierarchy definition and root node.
     *
     * @param def the hierarchy definition for this entity tree
     * @param rootNode the node to use as the root of the tree
     */
    public EntityTreeHierarchyImpl(@NotNull Catalog catalog, @NotNull HierarchyDef def, Node rootNode)
    {
        this(catalog, def, rootNode, 0L);
    }

    /**
     * Creates a new EntityTreeHierarchyImpl with the specified hierarchy definition, root node, and version.
     *
     * @param catalog the catalog containing this hierarchy
     * @param def the hierarchy definition for this entity tree
     * @param rootNode the node to use as the root of the tree
     * @param version the version number of this hierarchy
     */
    public EntityTreeHierarchyImpl(@NotNull Catalog catalog, @NotNull HierarchyDef def, Node rootNode, long version)
    {
        this.catalog = catalog;
        this.def = def;
        this.version = version;
        this.root = rootNode;
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
     * Returns the hierarchy definition for this entity tree.
     * 
     * @return the hierarchy definition describing this entity tree's structure
     */
    @Override
    public @NotNull HierarchyDef def()
    {
        return def;
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
}
