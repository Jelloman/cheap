package net.netbeing.cheap.model;

import java.util.Map;

/**
 * The interface Entity tree hierarchy.
 */
public interface EntityTreeHierarchy extends Hierarchy
{
    /**
     * The interface Node.
     */
    interface Node extends Map<String, Node>
    {
        /**
         * Is leaf boolean.
         *
         * @return the boolean
         */
        boolean isLeaf();

        /**
         * Gets parent.
         *
         * @return the parent
         */
        Node getParent();

        /**
         * Value entity.
         *
         * @return the entity
         */
        Entity value();
    }

    /**
     * Root node.
     *
     * @return the node
     */
    Node root();
}
