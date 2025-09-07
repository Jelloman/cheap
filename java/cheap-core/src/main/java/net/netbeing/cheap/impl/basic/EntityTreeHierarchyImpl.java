package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.EntityTreeHierarchy;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EntityTreeHierarchyImpl implements EntityTreeHierarchy
{
    public class NodeImpl extends HashMap<String, Node> implements Node
    {
        @Override
        public boolean isLeaf()
        {
            return false;
        }

        @Override
        public Entity value()
        {
            return null;
        }
    }

    public static class LeafNodeImpl extends AbstractMap<String, Node> implements Node
    {
        private final Entity value;

        public LeafNodeImpl(Entity value)
        {
            this.value = value;
        }

        @Override
        public boolean isLeaf()
        {
            return true;
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

    public EntityTreeHierarchyImpl(HierarchyDef def)
    {
        this.def = def;
        this.root = new NodeImpl();
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
