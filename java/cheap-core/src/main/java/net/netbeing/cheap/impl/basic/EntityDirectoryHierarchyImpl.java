package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.EntityDirectoryHierarchy;

import java.util.HashMap;

public class EntityDirectoryHierarchyImpl extends HashMap<String, Entity> implements EntityDirectoryHierarchy
{
    private final HierarchyDef def;

    public EntityDirectoryHierarchyImpl(HierarchyDef def)
    {
        this.def = def;
    }

    @Override
    public HierarchyDef def()
    {
        return def;
    }
}
