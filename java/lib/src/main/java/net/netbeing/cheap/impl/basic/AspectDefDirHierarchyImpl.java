package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.AspectDefDirHierarchy;
import net.netbeing.cheap.model.HierarchyDef;

import java.util.LinkedHashMap;

public class AspectDefDirHierarchyImpl implements AspectDefDirHierarchy
{
    private final LinkedHashMap<String, AspectDef> defs = new LinkedHashMap<>();

    @Override
    public AspectDef add(AspectDef def)
    {
        return defs.put(def.name(), def);
    }

    @Override
    public AspectDef get(String name)
    {
        return defs.get(name);
    }

    @Override
    public HierarchyDef def()
    {
        return null;
    }
}
