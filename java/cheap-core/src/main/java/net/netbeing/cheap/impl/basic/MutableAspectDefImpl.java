package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Mutable implementation of an AspectDef that allows modification after creation.
 * This implementation allows adding and removing property definitions dynamically,
 * making it suitable for aspect definitions that need to evolve during runtime.
 * <p>
 * Unlike {@link ImmutableAspectDefImpl}, this class supports modification operations
 * and maintains a mutable internal map of property definitions.
 * 
 * @see AspectDefBase
 * @see ImmutableAspectDefImpl
 * @see PropertyDef
 */
public class MutableAspectDefImpl extends AspectDefBase
{
    /**
     * Creates a new MutableAspectDefImpl with the specified name and empty property definitions.
     * 
     * @param name the name of this aspect definition
     */
    public MutableAspectDefImpl(@NotNull String name)
    {
        super(name);
    }

    /**
     * Creates a new MutableAspectDefImpl with the specified name and property definitions.
     * 
     * @param name the name of this aspect definition
     * @param propertyDefs the initial map of property names to property definitions
     */
    public MutableAspectDefImpl(@NotNull String name, @NotNull Map<String, PropertyDef> propertyDefs)
    {
        super(name, propertyDefs);
    }

    /**
     * Adds a property definition to this mutable aspect definition.
     * 
     * @param prop the property definition to add
     * @return the previous property definition with the same name, or {@code null} if none existed
     */
    public PropertyDef add(@NotNull PropertyDef prop)
    {
        return propertyDefs.put(prop.name(), prop);
    }

    /**
     * Removes a property definition from this mutable aspect definition.
     * 
     * @param prop the property definition to remove
     * @return the removed property definition, or {@code null} if it wasn't present
     */
    public PropertyDef remove(@NotNull PropertyDef prop)
    {
        return propertyDefs.remove(prop.name());
    }

}
