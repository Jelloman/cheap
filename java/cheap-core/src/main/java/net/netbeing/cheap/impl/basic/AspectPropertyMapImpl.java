package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of an Aspect that stores properties as Property objects in a HashMap.
 * This implementation provides type-safe property access and validation while maintaining
 * insertion order.
 * <p>
 * Unlike {@link AspectObjectMapImpl}, this implementation stores actual Property objects
 * rather than raw values, providing better type safety and validation capabilities.
 * 
 * @see AspectBaseImpl
 * @see Aspect
 * @see Property
 * @see AspectObjectMapImpl
 */
public class AspectPropertyMapImpl extends AspectBaseImpl
{
    /** Internal map storing property names to Property objects. */
    protected Map<String, Property> props;

    /**
     * Creates a new AspectPropertyMapImpl with default capacity.
     * 
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     */
    public AspectPropertyMapImpl(Entity entity, AspectDef def)
    {
        super(entity, def);
        this.props = new LinkedHashMap<>();
    }

    /**
     * Creates a new AspectPropertyMapImpl with specified initial capacity.
     * 
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @param initialCapacity the initial capacity of the internal map
     */
    public AspectPropertyMapImpl(Entity entity, AspectDef def, int initialCapacity)
    {
        super(entity, def);
        this.props = new LinkedHashMap<>(initialCapacity);
    }

    /**
     * Creates a new AspectPropertyMapImpl with specified initial capacity and load factor.
     * 
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @param initialCapacity the initial capacity of the internal map
     * @param loadFactor the load factor of the internal map
     */
    public AspectPropertyMapImpl(Entity entity, AspectDef def, int initialCapacity, float loadFactor)
    {
        super(entity, def);
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
        if (props.containsKey(propName)) {
            return true;
        }
        PropertyDef propDef = def.propertyDef(propName);
        return propDef != null && propDef.hasDefaultValue();
    }

    /**
     * Reads a property value without type safety checks.
     * 
     * @param propName the name of the property to read
     * @return the property value, or {@code null} if the property doesn't exist
     */
    @Override
    public Object unsafeReadObj(@NotNull String propName)
    {
        if (props.containsKey(propName)) {
            return props.get(propName).unsafeRead();
        }
        PropertyDef propDef = def.propertyDef(propName);
        return (propDef != null && propDef.hasDefaultValue()) ? propDef.defaultValue() : null;
    }

    /**
     * Retrieves a property by name with full validation and access control.
     * 
     * @param propName the name of the property to retrieve
     * @return the Property object
     * @throws UnsupportedOperationException if the aspect or property is not readable
     * @throws IllegalArgumentException if the property doesn't exist in this aspect
     */
    @Override
    public Property get(@NotNull String propName)
    {
        AspectDef def = def();
        String name = def.name();
        if (!def.isReadable()) {
            throw new UnsupportedOperationException("Aspect '" + name + "' is not readable.");
        }
        Property prop = props.get(propName);
        if (prop != null) {
            if (!prop.def().isReadable()) {
                throw new UnsupportedOperationException("Property '" + propName + "' is not readable.");
            }
            return prop;
        }
        PropertyDef propDef = def.propertyDef(propName);
        if (propDef == null || !propDef.hasDefaultValue()) {
            throw new IllegalArgumentException("Aspect '" + name + "' does not contain prop named '"+ propName + "'.");
        }
        return new PropertyImpl(propDef, propDef.defaultValue());
    }

    /**
     * Stores a property in this aspect with full validation and access control.
     * 
     * @param prop the property to store
     * @throws UnsupportedOperationException if the aspect or property is not writable
     * @throws IllegalArgumentException if the property cannot be added to this aspect
     * @throws ClassCastException if the property definition doesn't match an existing property
     */
    @Override
    public void put(@NotNull Property prop)
    {
        AspectDef def = def();
        String aspectName = def.name();
        if (!def.isWritable()) {
            throw new UnsupportedOperationException("Aspect '" + aspectName + "' is not writable.");
        }
        String propName = prop.def().name();
        PropertyDef stdPropDef = def.propertyDef(propName);
        if (stdPropDef == null) {
            if (!def.canAddProperties()) {
                throw new IllegalArgumentException("Aspect '" + aspectName + "' does not contain prop named '" + propName + "' and is not extensible.");
            }
            if (!prop.def().isWritable()) {
                throw new UnsupportedOperationException("Provided property '" + propName + "' is marked not writable.");
            }
        } else {
            if (!stdPropDef.isWritable()) {
                throw new UnsupportedOperationException("Property '" + propName + "' is not writable.");
            }
            if (!stdPropDef.equals(prop.def())) {
                throw new IllegalArgumentException("Provided definition of '" + propName + "' conflicts with the existing definition.");
            }
        }
        props.put(propName, prop);
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
        AspectDef def = def();
        PropertyDef stdPropDef = def.propertyDef(propName);
        if (stdPropDef != null) {
            // ignore and replace any current prop
            props.put(propName, new PropertyImpl(stdPropDef, value));
        } else {
            Property prop = props.get(propName);
            if (prop == null) {
                throw new IllegalArgumentException("Aspect '" + def().name() + "' does not contain prop named '" + propName + "'");
            }
            props.put(propName, new PropertyImpl(prop.def(), value));
        }
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
