/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.impl.reflect;

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.Property;
import net.netbeing.cheap.util.reflect.GenericGetterSetter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An {@link Aspect} implementation that provides full read-write access to Plain Old Java Objects (POJOs)
 * through the Cheap property model.
 * 
 * <p>This class wraps a POJO instance and exposes its JavaBean properties as Cheap properties
 * using the property definitions from a {@link MutablePojoAspectDef}. Unlike {@link ImmutablePojoAspect},
 * this implementation supports both reading and writing properties through their respective
 * getter and setter methods when available.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Full read-write property access through JavaBean getter and setter methods</li>
 *   <li>Type-safe POJO instance storage with generic type parameter</li>
 *   <li>Integration with Cheap's entity and catalog system</li>
 *   <li>Efficient property value access using cached method wrappers</li>
 *   <li>Intelligent primitive type handling for proper boxing/unboxing</li>
 * </ul>
 * 
 * <p>Property access behavior:</p>
 * <ul>
 *   <li>Read operations use cached {@link GenericGetterSetter} getter wrappers</li>
 *   <li>Write operations use cached {@link GenericGetterSetter} setter wrappers</li>
 *   <li>Properties without getters cannot be read (throw {@link IllegalArgumentException})</li>
 *   <li>Properties without setters cannot be written (throw {@link IllegalArgumentException})</li>
 *   <li>Primitive properties receive special handling to ensure proper type conversion</li>
 * </ul>
 * 
 * <p>Primitive type handling:</p>
 * <p>When writing to properties marked as Java primitives ({@link PojoPropertyDef#isJavaPrimitive()}),
 * the implementation performs explicit type casting to ensure proper unboxing and avoid
 * {@link ClassCastException} during reflection calls. Supported primitive types include:</p>
 * <ul>
 *   <li>int, long, double, float, boolean, byte, short, char</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * public class Person {
 *     private String name;
 *     private int age;
 *     
 *     public String getName() { return name; }
 *     public void setName(String name) { this.name = name; }
 *     public int getAge() { return age; }
 *     public void setAge(int age) { this.age = age; }
 * }
 * 
 * Person person = new Person();
 * MutablePojoAspectDef aspectDef = new MutablePojoAspectDef(Person.class);
 * MutablePojoAspect<Person> aspect = new MutablePojoAspect<>(catalog, entity, aspectDef, person);
 * 
 * // Reading properties
 * String name = (String) aspect.unsafeReadObj("name");
 * Integer age = (Integer) aspect.unsafeReadObj("age");
 * 
 * // Writing properties  
 * aspect.unsafeWrite("name", "John");
 * aspect.unsafeWrite("age", 30); // Properly handles int primitive
 * 
 * // These operations will throw UnsupportedOperationException:
 * aspect.unsafeAdd(someProperty);    // Cannot add new properties
 * aspect.unsafeRemove("name");       // Cannot remove properties
 * }</pre>
 * 
 * <p>This implementation works in conjunction with {@link MutablePojoAspectDef} to provide
 * full read-write access to POJO instances through the Cheap property model, making it suitable
 * for scenarios where data needs to be both read from and written to through a consistent interface.</p>
 * 
 * @param <P> the specific POJO type wrapped by this aspect
 * 
 * @see MutablePojoAspectDef
 * @see PojoPropertyDef
 * @see ImmutablePojoAspect
 * @see Aspect
 * @see CheapReflectionUtil
 */
public class MutablePojoAspect<P> implements Aspect
{
    /** The entity that this aspect is associated with. */
    private Entity entity;
    
    /** The aspect definition describing the POJO's structure and providing method access. */
    private final MutablePojoAspectDef def;
    
    /** The underlying POJO instance. */
    private final P object;

    /**
     * Constructs a new MutablePojoAspect wrapping the specified POJO instance.
     * 
     * <p>This constructor creates a mutable aspect that provides full read-write access
     * to the POJO's properties through the property definitions and cached method
     * wrappers provided by the aspect definition.</p>
     * 
     * @param entity the entity that this aspect is associated with; may be null
     * @param def the mutable aspect definition describing the POJO structure
     * @param object the POJO instance to wrap
     * @throws NullPointerException if any parameter is null
     */
    public MutablePojoAspect(Entity entity, @NotNull MutablePojoAspectDef def, @NotNull P object)
    {
        this.entity = entity;
        this.def = def;
        this.object = object;
    }

    /**
     * {@inheritDoc}
     * 
     * @return the entity that this aspect is associated with
     */
    @Override
    public Entity entity()
    {
        return entity;
    }

    /**
     * Set the entity that owns this aspect. If the entity is already set
     * and this is not flagged as transferable, an Exception will be thrown.
     *
     * @param entity the entity to attach this aspect to, never null
     * @throws IllegalStateException if the aspect is non-transferable and already attached to another entity
     */
    @Override
    public void setEntity(@NotNull Entity entity)
    {
        Objects.requireNonNull(entity, "Aspects may not be assigned a null entity.");
        if (this.entity != null && this.entity != entity && !isTransferable()) {
            throw new IllegalStateException("An Aspect flagged as non-transferable may not be reassigned to a different entity.");
        }
        this.entity = entity;
    }

    /**
     * {@inheritDoc}
     * 
     * @return the mutable POJO aspect definition describing this aspect's structure
     */
    @Override
    public AspectDef def()
    {
        return def;
    }

    /**
     * Returns the underlying POJO instance.
     * 
     * @return the POJO instance wrapped by this aspect
     */
    public P object()
    {
        return object;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Reads property values from the POJO using the getter methods defined by the POJO class.
     * The property value is retrieved through efficient reflection wrappers cached by the
     * {@link MutablePojoAspectDef}.</p>
     * 
     * @param propName the name of the property to read
     * @return the value of the property from the POJO instance
     * @throws IllegalArgumentException if no property with the given name exists or if the property has no getter method
     * @throws NullPointerException if propName is null
     */
    @Override
    public Object unsafeReadObj(@NotNull String propName)
    {
        GenericGetterSetter getter = def.getter(propName);
        if (getter == null) {
            throw new IllegalArgumentException("Class " + def.name() + " does not contain field '" + propName + "'.");
        }
        return getter.get(object);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Writes property values to the POJO using the setter methods defined by the POJO class.
     * The method includes intelligent primitive type handling to ensure proper unboxing and
     * avoid {@link ClassCastException} during reflection calls.</p>
     * 
     * <p>For properties marked as Java primitives ({@link PojoPropertyDef#isJavaPrimitive()}),
     * the value is explicitly cast to the appropriate primitive type before passing to the setter.
     * This ensures proper handling of boxed primitive values and prevents reflection errors.</p>
     * 
     * @param propName the name of the property to write
     * @param value the value to set on the property
     * @throws IllegalArgumentException if no property with the given name exists, if the property has no setter method,
     *         or if a primitive property receives a value of an unexpected type
     * @throws IllegalStateException if a property is marked as primitive but the value is not a recognized primitive type
     * @throws NullPointerException if propName is null
     */
    @Override
    public void unsafeWrite(@NotNull String propName, Object value)
    {
        GenericGetterSetter setter = def.setter(propName);
        if (setter == null) {
            throw new IllegalArgumentException("Class " + def.name() + " does not contain field '" + propName + "'.");
        }
        PojoPropertyDef propDef = (PojoPropertyDef) def.propertyDef(propName);
        if (propDef.isJavaPrimitive()) {
            // Handle primitive types by calling the appropriate setter method
            switch (value) {
                case Integer i -> setter.set(object, (int) value);
                case Long l -> setter.set(object, (long) value);
                case Double v -> setter.set(object, (double) value);
                case Float v -> setter.set(object, (float) value);
                case Boolean b -> setter.set(object, (boolean) value);
                case Byte b -> setter.set(object, (byte) value);
                case Short i -> setter.set(object, (short) value);
                case Character c -> setter.set(object, (char) value);
                default -> throw new IllegalStateException("Property '" + propName + "' is flagged as primitive but is not of a primitive type.");
            }
        } else {
            // For non-primitive types and null values
            setter.set(object, value);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Always throws {@link UnsupportedOperationException} since POJOs have a fixed
     * set of properties defined by their class structure and cannot be extended dynamically.</p>
     * 
     * @param prop the property to add (unused)
     * @throws UnsupportedOperationException always, since POJO properties cannot be added dynamically
     */
    @Override
    public void unsafeAdd(@NotNull Property prop)
    {
        throw new UnsupportedOperationException("Property '" + prop.def().name() + "' cannot be added to Java class with AspectDef '" + def.name() + "'.");
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Always throws {@link UnsupportedOperationException} since POJOs have a fixed
     * set of properties defined by their class structure and cannot be reduced dynamically.</p>
     * 
     * @param propName the name of the property to remove (unused)
     * @throws UnsupportedOperationException always, since POJO properties cannot be removed dynamically
     */
    @Override
    public void unsafeRemove(@NotNull String propName)
    {
        throw new UnsupportedOperationException("Property '" + propName + "' cannot be removed from Java class with AspectDef '" + def.name() + "'.");
    }
}
