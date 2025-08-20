package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;

import java.util.HashMap;

public class AspectMapHierarchyImpl extends HashMap<Entity, Aspect> implements AspectMapHierarchy
{
    private final HierarchyDef def;
    private final AspectDef aspectDef;

    public AspectMapHierarchyImpl(HierarchyDef def, AspectDef aspectDef)
    {
        this.def = def;
        this.aspectDef = aspectDef;
    }

    @Override
    public AspectDef aspectDef()
    {
        return aspectDef;
    }

    @Override
    public HierarchyDef def()
    {
        return def;
    }
}
