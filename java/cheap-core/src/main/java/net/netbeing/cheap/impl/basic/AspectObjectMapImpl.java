package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Basic implementation of an Aspect that stores properties in a LinkedHashMap.
 * This implementation provides efficient access to aspect properties by name
 * while maintaining insertion order.
 * <p>
 * The underlying storage uses a {@link LinkedHashMap} to preserve the order
 * in which properties are added to the aspect.
 * 
 * @see AspectBaseImpl
 * @see Aspect
 */
public class AspectObjectMapImpl extends AspectBaseImpl
{
    /** Internal map storing property names to property objects. */
    protected Map<String, Object> props;

    /**
     * Creates a new AspectObjectMapImpl with default capacity.
     * 
     * @param catalog the catalog this aspect belongs to
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     */
    public AspectObjectMapImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def)
    {
        super(catalog, entity, def);
        this.props = new LinkedHashMap<>();
    }

    /**
     * Creates a new AspectObjectMapImpl with specified initial capacity.
     * 
     * @param catalog the catalog this aspect belongs to
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @param initialCapacity the initial capacity of the internal map
     */
    public AspectObjectMapImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def, int initialCapacity)
    {
        super(catalog, entity, def);
        this.props = new LinkedHashMap<>(initialCapacity);
    }

    /**
     * Creates a new AspectObjectMapImpl with specified initial capacity and load factor.
     * 
     * @param catalog the catalog this aspect belongs to
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @param initialCapacity the initial capacity of the internal map
     * @param loadFactor the load factor of the internal map
     */
    public AspectObjectMapImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def, int initialCapacity, float loadFactor)
    {
        super(catalog, entity, def);
        this.props = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    /**
     * Checks if this aspect contains a property with the given name.
     * 
     * @param propName the name of the property to check for
     * @return {@code true} if the property exists, {@code false} otherwise
     */
    @Override
    public boolean contains(@NotNull String propName)
    {
        return props.containsKey(propName);
    }

    /**
     * Reads a property value without type safety checks.
     * 
     * @param propName the name of the property to read
     * @return the property object, or {@code null} if not found
     */
    @Override
    public Object unsafeReadObj(@NotNull String propName)
    {
        return props.get(propName);
    }

    /**
     * Adds a property to this aspect without validation.
     * 
     * @param prop the property to add
     */
    @Override
    public void unsafeAdd(@NotNull Property prop)
    {
        props.put(prop.def().name(), prop);
    }

    /**
     * Writes a property value without type safety checks.
     * 
     * @param propName the name of the property to write
     * @param value the value to write
     * @throws IllegalArgumentException if the property name is not defined in this aspect
     */
    @Override
    public void unsafeWrite(@NotNull String propName, Object value)
    {
        if (!props.containsKey(propName)) {
            throw new IllegalArgumentException("Aspect '" + def().name() + "' does not contain prop named '" + propName + "'");
        }
        PropertyDef propDef = def.propertyDef(propName);
        if (propDef == null) {
            throw new IllegalArgumentException("Aspect '" + def.name() + "' does not contain prop named '" + propName + "'.");
        }

        Property newProp = new PropertyImpl(propDef, value);
        props.put(propName, newProp);
    }

    /**
     * Removes a property from this aspect without validation.
     * 
     * @param propName the name of the property to remove
     */
    @Override
    public void unsafeRemove(@NotNull String propName)
    {
        props.remove(propName);
    }
}
