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

import java.util.Map;
import java.util.Objects;

/**
 * Builder implementation for creating ImmutablePojoAspect instances using the builder pattern.
 * This class provides a fluent interface for configuring and building immutable POJO aspects
 * with property values.
 *
 * <p>This builder extends {@link AspectBuilderBase} to inherit common builder functionality
 * while providing specific logic for creating {@link ImmutablePojoAspect} instances. The builder
 * creates immutable POJO aspects that provide read-only access to JavaBean properties through
 * reflection-based getter methods.</p>
 *
 * <p>Unlike mutable POJO aspects, the properties set through this builder are used to initialize
 * the underlying POJO instance via reflection on setter methods during construction, but the
 * resulting aspect enforces immutability by throwing {@link UnsupportedOperationException} for
 * any subsequent write operations.</p>
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
 * ImmutablePojoAspectBuilder<Person> builder = new ImmutablePojoAspectBuilder<>(Person.class);
 * ImmutablePojoAspect<Person> aspect = builder
 *     .entity(myEntity)
 *     .property("name", "John Doe")
 *     .property("age", 30)
 *     .build();
 *
 * // Reading works
 * String name = (String) aspect.unsafeReadObj("name"); // Returns "John Doe"
 *
 * // Writing throws UnsupportedOperationException
 * aspect.unsafeWrite("name", "Jane"); // Throws exception
 * }</pre>
 *
 * @param <P> the specific POJO type that this builder creates aspects for
 */
public class ImmutablePojoAspectBuilder<P> extends AspectBuilderBase
{
    private final Class<P> pojoClass;

    /**
     * Creates a new ImmutablePojoAspectBuilder for the specified POJO class.
     *
     * <p>This constructor automatically creates an {@link ImmutablePojoAspectDef} for the
     * provided class, which will be used to define the structure and property access
     * for aspects created by this builder.</p>
     *
     * @param pojoClass the POJO class that this builder will create aspects for, must not be null
     * @throws NullPointerException if pojoClass is null
     */
    public ImmutablePojoAspectBuilder(@NotNull Class<P> pojoClass)
    {
        super();
        this.pojoClass = Objects.requireNonNull(pojoClass, "POJO class cannot be null");
        // Set the default aspect definition for this POJO class
        super.aspectDef(new ImmutablePojoAspectDef(pojoClass));
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

    /**
     * Creates and returns an ImmutablePojoAspect instance with the configured entity,
     * aspect definition, and properties.
     *
     * <p>This method performs the following steps:</p>
     * <ol>
     *   <li>Creates a new instance of the POJO class using its no-argument constructor</li>
     *   <li>Sets all configured properties on the POJO instance using reflection to find setter methods</li>
     *   <li>Creates an ImmutablePojoAspect wrapping the initialized POJO instance</li>
     *   <li>Returns the immutable aspect that enforces read-only access</li>
     * </ol>
     *
     * @return the created ImmutablePojoAspect instance, never null
     * @throws RuntimeException if the POJO class cannot be instantiated or if setter methods cannot be invoked
     */
    @Override
    protected @NotNull Aspect createAspect()
    {
        // Create a new instance of the POJO class
        P pojoInstance;
        try {
            pojoInstance = pojoClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + pojoClass.getName() +
                ". Ensure the class has a public no-argument constructor.", e);
        }

        // Ensure we're using an ImmutablePojoAspectDef for the correct POJO class
        ImmutablePojoAspectDef immutableAspectDef;
        AspectDef aspectDef = getAspectDef();
        if (aspectDef instanceof ImmutablePojoAspectDef immPojoAspectDef &&
            immPojoAspectDef.getPojoClass().equals(pojoClass)) {
            immutableAspectDef = immPojoAspectDef;
        } else {
            // Fallback to creating a new one for the POJO class
            immutableAspectDef = new ImmutablePojoAspectDef(pojoClass);
        }

        // Set all configured properties on the POJO instance using reflection
        // We'll use a temporary MutablePojoAspectDef to access setter methods during initialization
        Map<String, Object> properties = getProperties();
        if (!properties.isEmpty()) {
            try {
                MutablePojoAspectDef tempMutableDef = new MutablePojoAspectDef(pojoClass);
                MutablePojoAspect<P> tempAspect = new MutablePojoAspect<>(getEntity(), tempMutableDef, pojoInstance);

                // Apply all properties to initialize the POJO
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    tempAspect.write(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize POJO properties for " + pojoClass.getName(), e);
            }
        }

        // Create the immutable POJO aspect
        return new ImmutablePojoAspect<>(getEntity(), immutableAspectDef, pojoInstance);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides the base implementation to ensure that the ImmutablePojoAspectDef for this
     * builder's POJO class is restored after clearing the state, so the builder can
     * be reused without requiring the aspectDef to be set again.</p>
     */
    @Override
    public AspectBuilder reset()
    {
        super.reset();
        // Restore the default aspect definition for this POJO class
        super.aspectDef(new ImmutablePojoAspectDef(pojoClass));
        return this;
    }
}