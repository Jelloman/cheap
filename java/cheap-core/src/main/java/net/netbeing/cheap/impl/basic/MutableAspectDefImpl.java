package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class MutableAspectDefImpl extends AspectDefBase
{
    public MutableAspectDefImpl(@NotNull String name)
    {
        super(name);
    }

    public MutableAspectDefImpl(@NotNull String name, @NotNull Map<String, PropertyDef> propertyDefs)
    {
        super(name, propertyDefs);
    }

    public PropertyDef add(@NotNull PropertyDef prop)
    {
        return propertyDefs.put(prop.name(), prop);
    }

    public PropertyDef remove(@NotNull PropertyDef prop)
    {
        return propertyDefs.remove(prop.name());
    }

}
