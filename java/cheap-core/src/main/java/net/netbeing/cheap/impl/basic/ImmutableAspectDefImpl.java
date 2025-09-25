package net.netbeing.cheap.impl.basic;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Immutable implementation of an AspectDef that prevents modification after creation.
 * This implementation uses Guava's ImmutableMap to ensure the property definitions
 * cannot be changed after the aspect definition is constructed.
 * <p>
 * All attempts to add or remove properties will result in an UnsupportedOperationException.
 * This class is suitable for aspect definitions that should remain fixed throughout
 * the application lifecycle.
 * 
 * @see AspectDefBase
 * @see MutableAspectDefImpl
 * @see PropertyDef
 */
public class ImmutableAspectDefImpl extends AspectDefBase
{
    /**
     * Creates a new ImmutableAspectDefImpl with the specified name and property definitions.
     * The property definitions map is copied into an ImmutableMap to prevent modification.
     * 
     * @param name the name of this aspect definition
     * @param propertyDefs the map of property names to property definitions
     */
    public ImmutableAspectDefImpl(@NotNull String name, @NotNull Map<String, ? extends PropertyDef> propertyDefs)
    {
        super(name, ImmutableMap.copyOf(propertyDefs));
        if (propertyDefs.isEmpty()) {
            throw new IllegalArgumentException("An AspectDef must contain at least one property.");
        }
    }

    /**
     * Attempts to add a property definition to this immutable aspect definition.
     * 
     * @param prop the property definition to add
     * @return never returns normally
     * @throws UnsupportedOperationException always, as this aspect definition is immutable
     */
    public PropertyDef add(@NotNull PropertyDef prop)
    {
        throw new UnsupportedOperationException("Properties cannot be added to immutable AspectDef '" + name + "'.");
    }

    /**
     * Attempts to remove a property definition from this immutable aspect definition.
     * 
     * @param prop the property definition to remove
     * @return never returns normally
     * @throws UnsupportedOperationException always, as this aspect definition is immutable
     */
    public PropertyDef remove(@NotNull PropertyDef prop)
    {
        throw new UnsupportedOperationException("Properties cannot be removed from immutable AspectDef '" + name + "'.");
    }
}
