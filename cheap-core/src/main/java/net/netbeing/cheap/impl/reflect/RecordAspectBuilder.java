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

import net.netbeing.cheap.impl.basic.AspectBuilderBase;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectBuilder;
import net.netbeing.cheap.model.AspectDef;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builder implementation for creating RecordAspect instances using the builder pattern.
 * This class provides a fluent interface for configuring and building record aspects
 * with property values that correspond to record components.
 *
 * <p>This builder extends {@link AspectBuilderBase} to inherit common builder functionality
 * while providing specific logic for creating {@link RecordAspect} instances. Since Java
 * records are immutable by design, the builder creates record instances by calling the
 * canonical constructor with the configured property values, then wraps the record in
 * an immutable aspect.</p>
 *
 * <p>The builder automatically discovers the record's components and their types, then
 * matches configured properties to constructor parameters by name and position. Properties
 * must be set for all record components that don't have default values.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public record Person(String name, int age, boolean active) {}
 *
 * RecordAspectBuilder<Person> builder = new RecordAspectBuilder<>(Person.class);
 * RecordAspect<Person> aspect = builder
 *     .entity(myEntity)
 *     .property("name", "John Doe")
 *     .property("age", 30)
 *     .property("active", true)
 *     .build();
 *
 * // Reading works
 * String name = (String) aspect.unsafeReadObj("name"); // Returns "John Doe"
 *
 * // Writing throws UnsupportedOperationException (records are immutable)
 * aspect.unsafeWrite("name", "Jane"); // Throws exception
 * }</pre>
 *
 * @param <R> the specific Java record type that this builder creates aspects for
 */
public class RecordAspectBuilder<R extends Record> extends AspectBuilderBase
{
    private final Class<R> recordClass;

    /**
     * Creates a new RecordAspectBuilder for the specified record class.
     *
     * <p>This constructor automatically creates a {@link RecordAspectDef} for the
     * provided record class, which will be used to define the structure and property access
     * for aspects created by this builder.</p>
     *
     * @param recordClass the record class that this builder will create aspects for, must not be null
     * @throws NullPointerException if recordClass is null
     * @throws IllegalArgumentException if the provided class is not a record class
     */
    public RecordAspectBuilder(@NotNull Class<R> recordClass)
    {
        super();
        this.recordClass = Objects.requireNonNull(recordClass, "Record class cannot be null");

        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException("Class " + recordClass.getName() + " is not a record class");
        }

        // Set the default aspect definition for this record class
        super.aspectDef(new RecordAspectDef(recordClass));
    }

    /**
     * Returns the record class that this builder creates aspects for.
     *
     * @return the record class specified in the constructor
     */
    public Class<R> getRecordClass()
    {
        return recordClass;
    }

    /**
     * Creates and returns a RecordAspect instance with the configured entity,
     * aspect definition, and properties.
     *
     * <p>This method performs the following steps:</p>
     * <ol>
     *   <li>Discovers the record's canonical constructor</li>
     *   <li>Maps configured properties to constructor parameters by component name</li>
     *   <li>Creates a new record instance using the canonical constructor</li>
     *   <li>Creates a RecordAspect wrapping the record instance</li>
     *   <li>Returns the immutable aspect that enforces read-only access</li>
     * </ol>
     *
     * @return the created RecordAspect instance, never null
     * @throws RuntimeException if the record cannot be instantiated or if required properties are missing
     */
    @Override
    protected @NotNull Aspect createAspect()
    {
        // Ensure we're using a RecordAspectDef for the correct record class
        RecordAspectDef recordAspectDef;
        AspectDef aspectDef = getAspectDef();
        if (aspectDef instanceof RecordAspectDef recAspectDef &&
            recAspectDef.getRecordClass().equals(recordClass)) {
            recordAspectDef = recAspectDef;
        } else {
            // Fallback to creating a new one for the record class
            recordAspectDef = new RecordAspectDef(recordClass);
        }

        // Get the record components to understand the constructor parameters
        RecordComponent[] components = recordClass.getRecordComponents();

        // Create the parameter array for the canonical constructor
        List<Object> constructorArgs = new ArrayList<>(components.length);
        Map<String, Object> properties = getProperties();

        for (RecordComponent component : components) {
            String componentName = component.getName();
            Object value = properties.get(componentName);

            // Handle missing values - could be null or missing entirely
            if (value == null && !properties.containsKey(componentName)) {
                // For primitive types, we need to provide a default value
                Class<?> componentType = component.getType();
                if (componentType.isPrimitive()) {
                    value = getDefaultPrimitiveValue(componentType);
                }
                // For reference types, null is acceptable
            }

            constructorArgs.add(value);
        }

        // Create the record instance using the canonical constructor
        R recordInstance;
        try {
            // Get the canonical constructor (all record components as parameters)
            Class<?>[] parameterTypes = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                parameterTypes[i] = components[i].getType();
            }

            Constructor<R> constructor = recordClass.getDeclaredConstructor(parameterTypes);
            recordInstance = constructor.newInstance(constructorArgs.toArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of record " + recordClass.getName() +
                ". Ensure all required properties are provided.", e);
        }

        // Create the record aspect
        return new RecordAspect<>(getEntity(), recordAspectDef, recordInstance);
    }

    /**
     * Returns the default value for a primitive type.
     *
     * @param primitiveType the primitive type to get the default value for
     * @return the default value for the primitive type
     * @throws IllegalArgumentException if the type is not a recognized primitive type
     */
    private Object getDefaultPrimitiveValue(Class<?> primitiveType)
    {
        if (primitiveType == boolean.class) return false;
        if (primitiveType == byte.class) return (byte) 0;
        if (primitiveType == short.class) return (short) 0;
        if (primitiveType == int.class) return 0;
        if (primitiveType == long.class) return 0L;
        if (primitiveType == float.class) return 0.0f;
        if (primitiveType == double.class) return 0.0;
        if (primitiveType == char.class) return '\0';

        throw new IllegalArgumentException("Unknown primitive type: " + primitiveType.getName());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides the base implementation to ensure that the RecordAspectDef for this
     * builder's record class is restored after clearing the state, so the builder can
     * be reused without requiring the aspectDef to be set again.</p>
     */
    @Override
    public AspectBuilder reset()
    {
        super.reset();
        // Restore the default aspect definition for this record class
        super.aspectDef(new RecordAspectDef(recordClass));
        return this;
    }
}