package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyDir;

import java.util.LinkedHashMap;

public class HierarchyDirImpl extends LinkedHashMap<String, Hierarchy> implements HierarchyDir
{
    private final HierarchyDef def;

    public HierarchyDirImpl(HierarchyDef def)
    {
        this.def = def;
    }

    public HierarchyDirImpl(HierarchyDef def, int initialCapacity, float loadFactor)
    {
        super(initialCapacity, loadFactor);
        this.def = def;
    }

    @Override
    public HierarchyDef def()
    {
        return def;
    }
}
