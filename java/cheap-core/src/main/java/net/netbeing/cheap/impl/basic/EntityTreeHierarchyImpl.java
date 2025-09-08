package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.EntityTreeHierarchy;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EntityTreeHierarchyImpl implements EntityTreeHierarchy
{
    public static class NodeImpl extends HashMap<String, Node> implements Node
    {
        private final Entity value;
        private final Node parent;

        public NodeImpl(Entity value)
        {
            this(value, null);
        }

        public NodeImpl(Entity value, Node parent)
        {
            this.value = value;
            this.parent = parent;
        }

        @Override
        public boolean isLeaf()
        {
            return false;
        }

        @Override
        public Node getParent()
        {
            return parent;
        }

        @Override
        public Entity value()
        {
            return value;
        }
    }

    public static class LeafNodeImpl extends AbstractMap<String, Node> implements Node
    {
        private final Entity value;
        private final Node parent;

        public LeafNodeImpl(Entity value)
        {
            this(value, null);
        }

        public LeafNodeImpl(Entity value, Node parent)
        {
            this.value = value;
            this.parent = parent;
        }

        @Override
        public boolean isLeaf()
        {
            return true;
        }

        @Override
        public Node getParent()
        {
            return parent;
        }

        @Override
        public Entity value()
        {
            return value;
        }

        @Override
        public @NotNull Set<Entry<String, Node>> entrySet()
        {
            return Collections.emptySet();
        }
    }

    private final HierarchyDef def;
    private final NodeImpl root;

    public EntityTreeHierarchyImpl(HierarchyDef def, Entity rootEntity)
    {
        this.def = def;
        this.root = new NodeImpl(rootEntity);
    }

    @Override
    public HierarchyDef def()
    {
        return def;
    }

    public Node root()
    {
        return root;
    }
}
