package net.netbeing.cheap.model;

import net.netbeing.cheap.impl.basic.PropertyImpl;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an aspect attached to an entity, serving as the "A" in the CHEAP acronym
 * (Catalog, Hierarchy, Entity, Aspect, Property). An Aspect is a collection of related
 * properties that describe a particular facet or characteristic of an entity.
 * 
 * <p>Aspects are analogous to rows in database terminology or file attributes in a
 * file system context. Each aspect has a definition that specifies its structure
 * and the properties it can contain.</p>
 * 
 * <p>This interface provides both safe (type-checked) and unsafe (unchecked) methods
 * for reading and writing property values. Safe methods perform validation against
 * the aspect definition, while unsafe methods bypass validation for performance
 * or when working with dynamic schemas.</p>
 * 
 * <p>Property access is controlled by the aspect definition's security model,
 * including readability, writability, and nullability constraints.</p>
 */
public interface Aspect
{
    /**
     * Returns the aspect definition that describes this aspect's structure,
     * including the properties it contains and their types.
     * 
     * <p>The aspect definition serves as the schema for this aspect instance,
     * defining what properties are available and their access permissions.</p>
     *
     * @return the aspect definition for this aspect, never null
     */
    AspectDef def();

    /**
     * Returns the entity that owns this aspect.
     *
     * @return the entity that owns this aspect, never null
     */
    Entity entity();

    /**
     * Set the entity that owns this aspect. If the entity is already set
     * and this is not flagged as transferable by its AspectDef, an
     * Exception will be thrown.
     *
     * @param entity the entity to attach this aspect to, never null
     */
    void setEntity(@NotNull Entity entity);

    /**
     * Reads a property value without performing validation against the aspect definition.
     * This method bypasses all security checks and type validation for maximum performance.
     * 
     * <p>Use with caution - this method can return unexpected types or throw runtime
     * exceptions if the property doesn't exist or has an incompatible type.</p>
     *
     * @param propName the name of the property to read, must not be null
     * @return the property value as an Object, may be null
     */
    Object unsafeReadObj(@NotNull String propName);

    /**
     * Returns a flag indicating whether this aspect may be transferred between entities.
     * Defaults to false. If it's false, Cheap will not allow changes to its owning entity.
     *
     * @return whether this aspect may be transferred between entities
     */
    default boolean isTransferable() {
        return false;
    }

    /**
     * Writes a property value without performing validation against the aspect definition.
     * This method bypasses all security checks, type validation, and nullability constraints.
     * 
     * <p>Use with caution - this method can corrupt data if used with incompatible
     * types or violate business rules defined by the aspect definition.</p>
     *
     * @param propName the name of the property to write, must not be null
     * @param value    the value to write, may be null
     */
    void unsafeWrite(@NotNull String propName, Object value);

    /**
     * Adds a new property to this aspect without validation. This method bypasses
     * security checks and assumes the aspect definition allows property addition.
     * 
     * <p>Use with caution - this method can violate aspect definition constraints
     * and should only be used when performance is critical and safety is ensured elsewhere.</p>
     *
     * @param prop the property to add, must not be null
     */
    void unsafeAdd(@NotNull Property prop);

    /**
     * Removes a property from this aspect without validation. This method bypasses
     * security checks and assumes the aspect definition allows property removal.
     * 
     * <p>Use with caution - this method can violate aspect definition constraints
     * and should only be used when performance is critical and safety is ensured elsewhere.</p>
     *
     * @param propName the name of the property to remove, must not be null
     */
    void unsafeRemove(@NotNull String propName);

    /**
     * Checks whether this aspect contains a property with the specified name.
     * A property is considered present if its value is not null.
     *
     * @param propName the name of the property to check for, must not be null
     * @return true if the property exists and is not null, false otherwise
     */
    default boolean contains(@NotNull String propName)
    {
        return unsafeReadObj(propName) != null;
    }

    /**
     * Reads a property value with type casting but without validation against
     * the aspect definition. This provides a balance between performance and usability.
     * 
     * <p>This method performs validation checks but casts the result to the requested
     * type without verifying type compatibility, which may result in ClassCastException.</p>
     *
     * @param <T>      the expected type of the property value
     * @param propName the name of the property to read, must not be null
     * @return the property value cast to type T, may be null
     * @throws ClassCastException if the value cannot be cast to the requested type
     */
    @SuppressWarnings("unchecked")
    default <T> T uncheckedRead(@NotNull String propName)
    {
        //noinspection unchecked
        return (T) readObj(propName);
    }

    /**
     * Reads a property value with type casting and no validation against the
     * aspect definition. This is the fastest read method but provides no safety guarantees.
     * 
     * <p>This method bypasses all validation and simply casts the raw value,
     * which may result in ClassCastException or other runtime errors.</p>
     *
     * @param <T>      the expected type of the property value
     * @param propName the name of the property to read, must not be null
     * @return the property value cast to type T, may be null
     * @throws ClassCastException if the value cannot be cast to the requested type
     */
    @SuppressWarnings("unchecked")
    default <T> T unsafeRead(@NotNull String propName)
    {
        //noinspection unchecked
        return (T) unsafeReadObj(propName);
    }

    /**
     * Reads a property value with full validation against the aspect definition.
     * This is the safest read method, performing all security and existence checks.
     * 
     * <p>This method verifies that the aspect is readable, the property exists,
     * and the property is readable before returning the value.</p>
     *
     * @param propName the name of the property to read, must not be null
     * @return the property value as an Object, may be null
     * @throws UnsupportedOperationException if the aspect or property is not readable
     * @throws IllegalArgumentException if the property doesn't exist in this aspect
     */
    default Object readObj(@NotNull String propName)
    {
        AspectDef def = def();
        if (!def.isReadable()) {
            throw new UnsupportedOperationException("Aspect '" + def.name() + "' is not readable.");
        }
        PropertyDef propDef = def.propertyDef(propName);
        if (propDef == null) {
            throw new IllegalArgumentException("Aspect '" + def.name() + "' does not contain prop named '" + propName + "'.");
        }
        if (!propDef.isReadable()) {
            throw new UnsupportedOperationException("Property '" + propName + "' in Aspect '" + def.name() + "' is not readable.");
        }
        return unsafeReadObj(propName);
    }

    default <T> T readAs(@NotNull String propName, @NotNull Class<T> type)
    {
        AspectDef def = def();
        PropertyDef propDef = def.propertyDef(propName);
        if (propDef == null) {
            throw new IllegalArgumentException("Aspect '" + def.name() + "' does not contain prop named '" + propName + "'.");
        }
        Object objVal = unsafeReadObj(propName);
        return type.cast(objVal);
    }

    default Property get(@NotNull String propName)
    {
        AspectDef def = def();
        String name = def.name();
        if (!def.isReadable()) {
            throw new UnsupportedOperationException("Aspect '" + name + "' is not readable.");
        }
        PropertyDef propDef = def.propertyDef(propName);
        if (propDef == null) {
            throw new IllegalArgumentException("Aspect '" + name + "' does not contain prop named '" + propName + "'.");
        }
        if (!propDef.isReadable()) {
            throw new UnsupportedOperationException("Property '" + propName + "' in Aspect '" + name + "' is not readable.");
        }
        return new PropertyImpl(propDef, unsafeReadObj(propName));
    }

    default void put(@NotNull Property prop)
    {
        AspectDef def = def();
        String name = def.name();
        String propName = prop.def().name();
        PropertyDef propDef = def.propertyDef(propName);
        if (propDef == null) {
            if (!def.canAddProperties()) {
                throw new IllegalArgumentException("Aspect '" + name + "' does not support adding properties.");
            }
            unsafeAdd(prop);
        } else {
            if (!def.isWritable()) {
                throw new UnsupportedOperationException("Aspect '" + name + "' is not writable.");
            }
            if (!propDef.isWritable()) {
                throw new UnsupportedOperationException("Property '" + propName + "' in Aspect '" + name + "' is not writable.");
            }
            unsafeWrite(propName, prop.unsafeRead());
        }
    }

    default void remove(@NotNull Property prop)
    {
        AspectDef def = def();
        String name = def.name();
        if (!def.canRemoveProperties()) {
            throw new UnsupportedOperationException("Aspect '" + name + "' does not support property removal.");
        }
        PropertyDef propDef = prop.def();
        String propName = propDef.name();
        Property currProp = get(propName);
        if (currProp == null) {
            throw new IllegalArgumentException("Aspect '" + name + "' does not contain a property named '" + propName + "'.");
        }
        PropertyDef currDef = currProp.def();
        if (currDef != propDef) { //TODO: use Entities.equal after writing it
            throw new ClassCastException("PropertyDef '" + propName + "' is not equal to existing PropertyDef '" + currDef.name() + "' in Aspect '" + name + "'.");
        }
        if (!currDef.isRemovable()) {
            throw new UnsupportedOperationException("Property '" + propName + "' in Aspect '" + name + "' is not removable.");
        }
        // TODO: Should value be checked for equality also? It may seem more "correct" but what's the benefit? It also prevents removal of unreadable properties.
        unsafeRemove(propName);
    }

    default void write(@NotNull String propName, Object value)
    {
        AspectDef def = def();
        String name = def.name();
        if (!def.isWritable()) {
            throw new UnsupportedOperationException("Aspect '" + name + "' is not writable.");
        }
        Property currProp = get(propName);
        if (currProp == null) {
            throw new IllegalArgumentException("Aspect '" + name + "' does not contain prop named '" + propName + "'.");
        }
        PropertyDef currDef = currProp.def();
        if (!currDef.isWritable()) {
            throw new UnsupportedOperationException("Property '" + propName + "' in Aspect '" + name + "' is not writable.");
        }
        if (value == null && !currDef.isNullable()) {
            throw new NullPointerException("Property '" + propName + "' in Aspect '" + name + "' is not nullable.");
        }
        unsafeWrite(propName, value);
    }

    default void putAll(@NotNull Iterable<Property> properties)
    {
        for (Property prop : properties) {
            put(prop);
        }
    }

    default void unsafeWriteAll(@NotNull Iterable<Property> properties)
    {
        for (Property prop : properties) {
            PropertyDef def = prop.def();
            unsafeWrite(def.name(), prop.unsafeRead());
        }
    }
}
