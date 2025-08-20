package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import org.jetbrains.annotations.NotNull;

import java.util.WeakHashMap;

public class WeakAspectMap extends WeakHashMap<AspectDef, Aspect>
{
    public WeakAspectMap(int initialCapacity, float loadFactor)
    {
        super(initialCapacity, loadFactor);
    }

    public WeakAspectMap(int initialCapacity)
    {
        super(initialCapacity);
    }

    public WeakAspectMap()
    {
    }

    public void add(@NotNull Aspect aspect)
    {
        put(aspect.def(), aspect);
    }
}
