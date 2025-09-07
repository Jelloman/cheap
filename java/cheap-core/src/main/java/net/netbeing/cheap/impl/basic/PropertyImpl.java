package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Property;
import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class PropertyImpl implements Property
{
    private final PropertyDef def;
    private final Object val;

    PropertyImpl(@NotNull PropertyDef def)
    {
        this.def = def;
        this.val = null;
    }

    public PropertyImpl(@NotNull PropertyDef def, Object val)
    {
        this.def = def;
        this.val = val;
    }

    @Override
    public final PropertyDef def()
    {
        return def;
    }

    @Contract(pure = true)
    @Override
    public Object unsafeRead()
    {
        return val;
    }

}
