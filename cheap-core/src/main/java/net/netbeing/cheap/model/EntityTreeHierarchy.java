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

package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Represents a tree-structured hierarchy of entities where nodes can have named children
 * and leaf nodes represent actual entities. This is the "ET" (Entity Tree) hierarchy type
 * in the Cheap model.
 * 
 * <p>Tree hierarchies are useful for representing file systems, organizational structures,
 * taxonomies, or any data that has a natural parent-child relationship with named paths
 * to leaf entities.</p>
 * 
 * <p>The tree structure allows for efficient navigation and querying of hierarchical data,
 * with each node potentially containing an entity and supporting named child lookups.</p>
 */
public interface EntityTreeHierarchy extends Hierarchy
{
    /**
     * Represents a node in the entity tree hierarchy. Each node can contain an entity
     * and act as a container for named child nodes, implementing the Map interface for
     * efficient child lookup by name.
     * 
     * <p>Nodes can be either leaf nodes (containing entities with no children) or
     * internal nodes (which may contain entities and have child nodes). The tree
     * structure is navigable in both directions through parent-child relationships.</p>
     */
    interface Node extends Map<String, Node>
    {
        /**
         * Determines whether this node is a leaf node (has no children) or an internal
         * node that can contain child nodes.
         * 
         * <p>Leaf nodes typically represent terminal entities in the hierarchy, while
         * internal nodes represent containers or directories that can hold other nodes.</p>
         *
         * @return true if this node cannot have children, false if it can contain child nodes
         */
        boolean isLeaf();

        /**
         * Returns the parent node of this node in the tree hierarchy, or null if this
         * is the root node.
         * 
         * <p>The parent relationship allows for upward navigation in the tree structure
         * and is used to maintain tree integrity during modifications.</p>
         *
         * @return the parent node, or null if this is the root node
         */
        Node getParent();

        /**
         * Returns the entity associated with this node. Not all nodes are required to
         * have an associated entity - some nodes may exist purely as containers for
         * organizing child nodes.
         *
         * <p>When present, the entity provides access to the data and aspects stored
         * at this location in the tree hierarchy.</p>
         *
         * @return the entity associated with this node, or null if no entity is attached
         */
        Entity value();

        /**
         * Sets the entity associated with this node. Not all nodes are required to
         * have an associated entity - some nodes may exist purely as containers for
         * organizing child nodes.
         *
         * @param entity the entity associated with this node, or null if no entity is attached
         */
        void setValue(Entity entity);
    }

    /**
     * Returns the root node of this tree hierarchy. The root node serves as the entry
     * point for navigating the entire tree structure.
     * 
     * <p>The root node has no parent and provides access to all child nodes through
     * the tree traversal methods. All paths in the tree hierarchy begin from this root.</p>
     *
     * @return the root node of the tree, never null
     */
    @NotNull Node root();

}
