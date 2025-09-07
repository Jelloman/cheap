package net.netbeing.cheap.impl.basic;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ImmutableAspectDefImpl extends AspectDefBase
{
    public ImmutableAspectDefImpl(@NotNull String name, @NotNull Map<String, ? extends PropertyDef> propertyDefs)
    {
        super(name, ImmutableMap.copyOf(propertyDefs));
    }

    public PropertyDef add(@NotNull PropertyDef prop)
    {
        throw new UnsupportedOperationException("Properties cannot be added to immutable AspectDef '" + name + "'.");
    }

    public PropertyDef remove(@NotNull PropertyDef prop)
    {
        throw new UnsupportedOperationException("Properties cannot be removed from immutable AspectDef '" + name + "'.");
    }

    @Override
    public boolean canAddProperties()
    {
        return false;
    }

    @Override
    public boolean canRemoveProperties()
    {
        return false;
    }
}
