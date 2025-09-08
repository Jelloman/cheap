package net.netbeing.cheap.model;

import java.util.Map;

public interface EntityTreeHierarchy extends Hierarchy
{
    interface Node extends Map<String, Node>
    {
        boolean isLeaf();
        Node getParent();
        Entity value();
    }

    Node root();
}
