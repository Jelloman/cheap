package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

class AspectDefBase implements AspectDef
{
    final String name;
    final Map<String, PropertyDef> propertyDefs;

    AspectDefBase(@NotNull String name)
    {
        this(name, new LinkedHashMap<>());
    }

    AspectDefBase(@NotNull String name, @NotNull Map<String, PropertyDef> propertyDefs)
    {
        this.name = name;
        this.propertyDefs = propertyDefs;
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public Collection<? extends PropertyDef> propertyDefs()
    {
        return propertyDefs.values();
    }

    @Override
    public PropertyDef propertyDef(@NotNull String propName)
    {
        return propertyDefs.get(propName);
    }
}
