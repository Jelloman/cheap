package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract base class for AspectDef implementations providing common functionality.
 * This class manages the basic structure of an aspect definition including its
 * name and property definitions.
 * <p>
 * Subclasses should implement the mutability methods (canAddProperties, canRemoveProperties)
 * and any modification operations (add, remove) as appropriate.
 * 
 * @see AspectDef
 * @see ImmutableAspectDefImpl
 * @see MutableAspectDefImpl
 * @see PropertyDef
 */
public abstract class AspectDefBase implements AspectDef
{
    /** The name of this aspect definition. */
    final String name;
    
    /** Map of property names to property definitions. */
    final Map<String, PropertyDef> propertyDefs;

    /**
     * Creates a new AspectDefBase with the specified name and empty property definitions.
     * 
     * @param name the name of this aspect definition
     */
    protected AspectDefBase(@NotNull String name)
    {
        this(name, new LinkedHashMap<>());
    }

    /**
     * Creates a new AspectDefBase with the specified name and property definitions.
     * 
     * @param name the name of this aspect definition
     * @param propertyDefs the map of property names to property definitions
     */
    protected AspectDefBase(@NotNull String name, @NotNull Map<String, PropertyDef> propertyDefs)
    {
        Objects.requireNonNull(name, "AspectDefs must have a non-null name.");
        Objects.requireNonNull(propertyDefs, "Provided property defs cannot be null.");

        this.name = name;
        this.propertyDefs = propertyDefs;
    }

    /**
     * Returns the name of this aspect definition.
     * 
     * @return the aspect definition name
     */
    @Override
    public String name()
    {
        return name;
    }

    /**
     * Returns a collection of all property definitions in this aspect.
     * 
     * @return collection of property definitions
     */
    @Override
    public Collection<? extends PropertyDef> propertyDefs()
    {
        return propertyDefs.values();
    }

    /**
     * Retrieves a property definition by name.
     * 
     * @param propName the name of the property definition to retrieve
     * @return the property definition with the given name, or {@code null} if not found
     */
    @Override
    public PropertyDef propertyDef(@NotNull String propName)
    {
        return propertyDefs.get(propName);
    }
}
