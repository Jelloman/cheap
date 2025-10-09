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

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;
import net.netbeing.cheap.util.reflect.GenericGetterSetter;

import java.util.Objects;

/**
 * An {@link Aspect} implementation that provides read-only access to Plain Old Java Objects (POJOs)
 * through the Cheap property model.
 * 
 * <p>This class wraps a POJO instance and exposes its JavaBean properties as Cheap properties
 * using the property definitions from an {@link ImmutablePojoAspectDef}. It enforces immutability
 * by providing read-only access to properties and throwing {@link UnsupportedOperationException}
 * for any write, add, or remove operations, regardless of whether the underlying POJO has setter
 * methods.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Read-only property access through JavaBean getter methods</li>
 *   <li>Type-safe POJO instance storage with generic type parameter</li>
 *   <li>Integration with Cheap's entity and catalog system</li>
 *   <li>Efficient property value retrieval using cached method wrappers</li>
 *   <li>Immutability enforcement at the aspect level</li>
 * </ul>
 * 
 * <p>Property access behavior:</p>
 * <ul>
 *   <li>Properties are accessed through cached {@link GenericGetterSetter} wrappers</li>
 *   <li>Only properties with getter methods can be read</li>
 *   <li>Missing properties or properties without getters throw {@link IllegalArgumentException}</li>
 *   <li>All mutation operations throw {@link UnsupportedOperationException}</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * public class Person {
 *     private String name;
 *     private int age;
 *     
 *     public String getName() { return name; }
 *     public void setName(String name) { this.name = name; } // Available but ignored
 *     public int getAge() { return age; }
 * }
 * 
 * Person person = new Person();
 * person.setName("John"); // Set via regular Java method
 * 
 * ImmutablePojoAspectDef aspectDef = new ImmutablePojoAspectDef(Person.class);
 * ImmutablePojoAspect<Person> aspect = new ImmutablePojoAspect<>(entity, aspectDef, person);
 * 
 * String name = (String) aspect.unsafeReadObj("name"); // Returns "John"
 * 
 * // These operations will throw UnsupportedOperationException:
 * aspect.unsafeWrite("name", "Jane");  // Cannot write
 * aspect.unsafeRemove("age");          // Cannot remove
 * }</pre>
 * 
 * <p>This implementation works in conjunction with {@link ImmutablePojoAspectDef} to provide
 * a read-only view of POJO instances through the Cheap property model, making it suitable
 * for scenarios where data immutability needs to be enforced at the aspect level regardless
 * of the underlying object's mutability capabilities.</p>
 * 
 * @param <P> the specific POJO type wrapped by this aspect
 * 
 * @see ImmutablePojoAspectDef
 * @see PojoPropertyDef
 * @see MutablePojoAspect
 * @see Aspect
 * @see CheapReflectionUtil
 */
public class ImmutablePojoAspect<P> implements Aspect
{
    /** The entity that this aspect is associated with. */
    private final Entity entity;
    
    /** The aspect definition describing the POJO's structure and providing method access. */
    private final ImmutablePojoAspectDef def;
    
    /** The underlying POJO instance. */
    private final P object;

    /**
     * Constructs a new ImmutablePojoAspect wrapping the specified POJO instance. The entity
     * must be provided and may not be changed.
     * 
     * <p>This constructor creates an immutable aspect that provides read-only access
     * to the POJO's properties through the property definitions and cached method
     * wrappers provided by the aspect definition.</p>
     * 
     * @param entity the entity that this aspect is associated with
     * @param def the immutable aspect definition describing the POJO structure
     * @param object the POJO instance to wrap
     * @throws NullPointerException if any parameter is null
     */
    public ImmutablePojoAspect(@NotNull Entity entity, @NotNull ImmutablePojoAspectDef def, @NotNull P object)
    {
        this.entity = entity;
        this.def = def;
        this.object = object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Entity entity()
    {
        return entity;
    }

    /**
     * Set the entity that owns this aspect. Since this aspect is immutable, the
     * provided entity is compared to the current entity, and an exception is
     * thrown if they don't match. If they match, this is a no-op.
     *
     * @param entity the entity to attach this aspect to, never null
     * @throws IllegalStateException if the entity is not equal to the current entity
     */
    @Override
    public void setEntity(@NotNull Entity entity)
    {
        // This null check is not necessary, but could be helpful in debugging.
        Objects.requireNonNull(entity, "Aspects may not be assigned a null entity.");
        if (!this.entity.equals(entity)) {
            throw new IllegalStateException("An Aspect flagged as non-transferable may not be reassigned to a different entity.");
        }
    }

    /**
     * {@inheritDoc}
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
     * {@link ImmutablePojoAspectDef}.</p>
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
     * <p>Always throws {@link UnsupportedOperationException} since this aspect enforces
     * immutability regardless of whether the underlying POJO has setter methods.</p>
     * 
     * @param propName the name of the property (unused)
     * @param value the value to set (unused)
     * @throws UnsupportedOperationException always, since immutable aspects cannot be modified
     */
    @Override
    public void unsafeWrite(@NotNull String propName, Object value)
    {
        throw new UnsupportedOperationException("Property '" + propName + "' cannot be set in Java class with immutable AspectDef '" + def.name() + "'.");
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Always throws {@link UnsupportedOperationException} since POJOs have a fixed
     * set of properties defined by their class structure, and immutable aspects cannot be modified.</p>
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
     * set of properties defined by their class structure, and immutable aspects cannot be modified.</p>
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
