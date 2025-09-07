package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.EntitySetHierarchy;

import java.util.HashSet;

public class EntitySetHierarchyImpl extends HashSet<Entity> implements EntitySetHierarchy
{
    private final HierarchyDef def;

    public EntitySetHierarchyImpl(HierarchyDef def)
    {
        this.def = def;
    }

    @Override
    public HierarchyDef def()
    {
        return def;
    }
}
