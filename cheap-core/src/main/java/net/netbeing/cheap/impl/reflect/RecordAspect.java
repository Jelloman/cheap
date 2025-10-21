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
 * An {@link Aspect} implementation that provides read-only access to Java record instances
 * through the Cheap property model.
 * 
 * <p>This class wraps a Java record instance and exposes its components as Cheap properties
 * using the property definitions from a {@link RecordAspectDef}. It maintains the immutability
 * semantics of Java records by throwing {@link UnsupportedOperationException} for any write,
 * add, or remove operations.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Read-only property access through record component accessor methods</li>
 *   <li>Type-safe record instance storage with generic type parameter</li>
 *   <li>Integration with Cheap's entity and catalog system</li>
 *   <li>Efficient property value retrieval using cached method wrappers</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * public record Person(String name, int age) {}
 * 
 * Person person = new Person("John", 30);
 * RecordAspectDef aspectDef = new RecordAspectDef(Person.class);
 * RecordAspect<Person> aspect = new RecordAspect<>(catalog, aspectDef, person);
 * 
 * String name = (String) aspect.unsafeReadObj("name"); // Returns "John"
 * Integer age = (Integer) aspect.unsafeReadObj("age");  // Returns 30
 * 
 * // These operations will throw UnsupportedOperationException:
 * aspect.unsafeWrite("name", "Jane");     // IllegalStateException
 * aspect.unsafeRemove("age");             // IllegalStateException
 * }</pre>
 * 
 * <p>This implementation enforces Java record immutability by rejecting all mutation operations.
 * Property values are accessed through the record's accessor methods using efficient reflection
 * wrappers provided by the associated {@link RecordAspectDef}.</p>
 * 
 * @param <R> the specific Java record type wrapped by this aspect
 * 
 * @see RecordAspectDef
 * @see RecordPropertyDef
 * @see Aspect
 * @see CheapReflectionUtil
 */
public class RecordAspect<R extends Record> implements Aspect
{
    /** The entity that this aspect is associated with. */
    private Entity entity;
    
    /** The aspect definition describing the record's structure. */
    private final RecordAspectDef def;
    
    /** The underlying Java record instance. */
    private final R record; // NOSONAR

    /**
     * Constructs a new RecordAspect with null entity.
     *
     * @param def the aspect definition describing the rec structure
     * @param rec the Java rec instance to wrap
     * @throws NullPointerException if any parameter is null
     */
    public RecordAspect(@NotNull RecordAspectDef def, @NotNull R rec)
    {
        this.entity = null;
        this.def = def;
        this.record = rec;
    }

    /**
     * Constructs a new RecordAspect with a pre-existing entity.
     *
     * <p>This constructor is used when the entity is already known and established,
     * typically when loading existing data or when the entity ID is predetermined.</p>
     *
     * @param entity the entity that this aspect is associated with; may be null
     * @param def the aspect definition describing the record structure
     * @param rec the Java record instance to wrap
     * @throws NullPointerException if any parameter is null
     */
    public RecordAspect(Entity entity, @NotNull RecordAspectDef def, @NotNull R rec)
    {
        this.entity = entity;
        this.def = def;
        this.record = rec;
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
     * @return the record aspect definition describing this aspect's structure
     */
    @Override
    public AspectDef def()
    {
        return def;
    }

    /**
     * Returns the underlying Java record instance.
     * 
     * @return the record instance wrapped by this aspect
     */
    public R record() // NOSONAR
    {
        return record;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Reads property values from the record using the accessor methods defined by the record class.
     * The property value is retrieved through efficient reflection wrappers cached by the
     * {@link RecordAspectDef}.</p>
     * 
     * @param propName the name of the property to read
     * @return the value of the property from the record instance
     * @throws IllegalArgumentException if no property with the given name exists
     * @throws NullPointerException if propName is null
     */
    @Override
    public Object unsafeReadObj(@NotNull String propName)
    {
        GenericGetterSetter getter = def.getter(propName);
        if (getter == null) {
            throw new IllegalArgumentException("Class " + def.name() + " does not contain field '" + propName + "'.");
        }
        return getter.get(record);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Always throws {@link UnsupportedOperationException} since Java records are immutable
     * and properties cannot be modified after construction.</p>
     * 
     * @param propName the name of the property (unused)
     * @param value the value to set (unused)
     * @throws UnsupportedOperationException always, since record properties cannot be modified
     */
    @Override
    public void unsafeWrite(@NotNull String propName, Object value)
    {
        throw new UnsupportedOperationException("Property '" + propName + "' cannot be set in Record class with immutable AspectDef '" + def.name() + "'.");
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Always throws {@link UnsupportedOperationException} since Java records have a fixed
     * set of components that cannot be extended at runtime.</p>
     * 
     * @param prop the property to add (unused)
     * @throws UnsupportedOperationException always, since record properties cannot be added
     */
    @Override
    public void unsafeAdd(@NotNull Property prop)
    {
        throw new UnsupportedOperationException("Property '" + prop.def().name() + "' cannot be added to Record class with immutable AspectDef '" + def.name() + "'.");
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Always throws {@link UnsupportedOperationException} since Java records have a fixed
     * set of components that cannot be removed at runtime.</p>
     * 
     * @param propName the name of the property to remove (unused)
     * @throws UnsupportedOperationException always, since record properties cannot be removed
     */
    @Override
    public void unsafeRemove(@NotNull String propName)
    {
        throw new UnsupportedOperationException("Property '" + propName + "' cannot be removed in Record class with immutable AspectDef '" + def.name() + "'.");
    }
}
