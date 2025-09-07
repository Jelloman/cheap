package net.netbeing.cheap.model;

import java.util.Map;

public interface EntityTreeHierarchy extends Hierarchy
{
    interface Node extends Map<String, Node>
    {
        default boolean isLeaf()
        {
            return value() != null;
        }

        Entity value();
    }

    Node root();
}
