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
import net.netbeing.cheap.model.AspectBuilder;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Builder implementation for creating MutablePojoAspect instances using the builder pattern.
 * This class provides a fluent interface for configuring and building mutable POJO aspects
 * with property values.
 *
 * <p>The builder maintains state for the entity, aspect definition, POJO class, and properties until
 * {@link #build()} is called, at which point it creates and returns a MutablePojoAspect
 * instance with all configured values.</p>
 *
 * <p>Unlike {@link net.netbeing.cheap.impl.basic.AspectObjectMapBuilder}, this builder creates
 * POJO instances and wraps them in aspects that provide read-write access to JavaBean properties
 * through reflection-based getter and setter methods.</p>
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
 * MutablePojoAspectBuilder<Person> builder = new MutablePojoAspectBuilder<>(Person.class);
 * MutablePojoAspect<Person> aspect = builder
 *     .entity(myEntity)
 *     .property("name", "John Doe")
 *     .property("age", 30)
 *     .build();
 * }</pre>
 *
 * @param <P> the specific POJO type that this builder creates aspects for
 */
public class MutablePojoAspectBuilder<P> implements AspectBuilder
{
    private final Class<P> pojoClass;
    private AspectDef aspectDef;
    private MutablePojoAspect<P> aspect;

    /**
     * Creates a new MutablePojoAspectBuilder for the specified POJO class.
     *
     * <p>This constructor automatically creates a {@link MutablePojoAspectDef} for the
     * provided class, which will be used to define the structure and property access
     * for aspects created by this builder.</p>
     *
     * @param pojoClass the POJO class that this builder will create aspects for, must not be null
     * @throws IllegalArgumentException if pojoClass is null
     */
    public MutablePojoAspectBuilder(@NotNull Class<P> pojoClass)
    {
        this.pojoClass = Objects.requireNonNull(pojoClass, "POJO class cannot be null");
        this.aspectDef = new MutablePojoAspectDef(pojoClass);
        constructAspect();
    }

    /**
     * Sets the entity for the aspect being built.
     *
     * @param entity the entity this aspect will be attached to, must not be null
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if entity is null
     */
    @Override
    public AspectBuilder entity(@NotNull Entity entity)
    {
        aspect.setEntity(Objects.requireNonNull(entity, "Entity cannot be null"));
        return this;
    }

    /**
     * Sets the aspect definition that defines the structure and schema for the aspect.
     *
     * <p>Note: This method is provided for interface compliance, but typically the
     * aspect definition is automatically created from the POJO class in the constructor.
     * If a custom aspect definition is provided, it should be compatible with the
     * POJO class specified in the constructor.</p>
     *
     * @param aspectDef the aspect definition to use, must not be null
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if aspectDef is null
     */
    @Override
    public AspectBuilder aspectDef(@NotNull AspectDef aspectDef)
    {
        this.aspectDef = Objects.requireNonNull(aspectDef, "AspectDef cannot be null");
        return this;
    }

    /**
     * Adds a property with the specified name and value to the aspect being built.
     *
     * @param propertyName the name of the property, must not be null
     * @param value the value of the property, may be null depending on the property definition
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if propertyName is null
     */
    @Override
    public AspectBuilder property(@NotNull String propertyName, Object value)
    {
        Objects.requireNonNull(propertyName, "Property name cannot be null");
        this.aspect.write(propertyName, value);
        return this;
    }

    /**
     * Adds a property to the aspect being built.
     *
     * @param property the property to add, must not be null
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if property is null
     */
    @Override
    public AspectBuilder property(@NotNull Property property)
    {
        Objects.requireNonNull(property, "Property cannot be null");
        String propertyName = property.def().name();
        Object value = property.read();
        this.aspect.write(propertyName, value);
        return this;
    }

    /**
     * Construct a fresh Aspect.
     */
    protected void constructAspect()
    {
        P pojoInstance;
        try {
            pojoInstance = pojoClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + pojoClass.getName() +
                ". Ensure the class has a public no-argument constructor.", e);
        }

        MutablePojoAspectDef mutAspectDef;
        if (aspectDef instanceof MutablePojoAspectDef mutPojoAspectDef &&
            mutPojoAspectDef.getPojoClass().equals(pojoClass)) {
            mutAspectDef = mutPojoAspectDef;
        } else {
            // Fallback to creating a new one for the POJO class
            mutAspectDef = new MutablePojoAspectDef(pojoClass);
        }

        // Create the mutable POJO aspect
        this.aspect = new MutablePojoAspect<>(null, mutAspectDef, pojoInstance);
    }

    /**
     * Builds and returns the configured MutablePojoAspect instance.
     *
     * <p>This method performs the following steps:</p>
     * <ol>
     *   <li>Validates that all required components (entity and aspectDef) have been set</li>
     *   <li>Creates a new instance of the POJO class using its no-argument constructor</li>
     *   <li>Creates a MutablePojoAspect wrapping the POJO instance</li>
     *   <li>Sets all configured properties on the aspect using their setter methods</li>
     * </ol>
     *
     * @return the built MutablePojoAspect instance, never null
     * @throws IllegalStateException if entity or aspectDef are not set
     * @throws RuntimeException if the POJO class cannot be instantiated (wraps various reflection exceptions)
     */
    @Override
    public @NotNull Aspect build()
    {
        if (aspect.entity() == null) {
            throw new IllegalStateException("Entity must be set before building aspect");
        }
        return aspect;
    }

    /**
     * Resets this builder to its initial state, clearing all configured values.
     * This allows the builder to be reused for creating multiple aspects.
     *
     * <p>Note: The POJO class and its associated aspect definition are not reset,
     * as they are fundamental to the builder's identity.</p>
     *
     * @return this builder instance for method chaining
     */
    @Override
    public AspectBuilder reset()
    {
        constructAspect();
        return this;
    }

    /**
     * Returns the POJO class that this builder creates aspects for.
     *
     * @return the POJO class specified in the constructor
     */
    public Class<P> getPojoClass()
    {
        return pojoClass;
    }
}