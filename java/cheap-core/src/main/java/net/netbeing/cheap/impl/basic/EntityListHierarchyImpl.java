package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.EntityListHierarchy;

import java.util.ArrayList;

public class EntityListHierarchyImpl extends ArrayList<Entity> implements EntityListHierarchy
{
    private final HierarchyDef def;

    public EntityListHierarchyImpl(HierarchyDef def)
    {
        this.def = def;
    }

    @Override
    public HierarchyDef def()
    {
        return def;
    }
}
