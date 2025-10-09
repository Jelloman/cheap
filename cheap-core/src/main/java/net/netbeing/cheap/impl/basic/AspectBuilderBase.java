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

package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract base class providing common functionality for AspectBuilder implementations.
 * This class handles entity management, aspect definition storage, property collection,
 * and validation logic that is shared across different builder implementations.
 *
 * <p>The base class maintains state for the entity, aspect definition, and properties,
 * and provides template methods for subclasses to customize the aspect creation process
 * while reusing common validation and property management logic.</p>
 *
 * <p>Subclasses must implement {@link #createAspect()} to provide the specific logic
 * for creating the appropriate aspect type with the configured entity, aspect definition,
 * and properties.</p>
 *
 * <p>Key features provided by this base class:</p>
 * <ul>
 *   <li>Entity and aspect definition management with validation</li>
 *   <li>Property collection with both name-value and Property object support</li>
 *   <li>Standard validation for build prerequisites</li>
 *   <li>Reset functionality for builder reuse</li>
 *   <li>Fluent interface support with proper return types</li>
 * </ul>
 *
 * <p>Example subclass implementation:</p>
 * <pre>{@code
 * public class MyAspectBuilder extends AspectBuilderBase {
 *     @Override
 *     protected Aspect createAspect() {
 *         MyAspectImpl aspect = new MyAspectImpl(getEntity(), getAspectDef());
 *         applyPropertiesToAspect(aspect);
 *         return aspect;
 *     }
 * }
 * }</pre>
 */
public abstract class AspectBuilderBase implements AspectBuilder
{
    /** The entity that the aspect will be associated with. */
    private Entity entity;

    /** The aspect definition that defines the structure and schema for the aspect. */
    private AspectDef aspectDef;

    /** Map of property names to values that will be set on the created aspect. */
    private final Map<String, Object> properties;

    /**
     * Creates a new AspectBuilderBase with empty initial state.
     */
    protected AspectBuilderBase()
    {
        this.properties = new LinkedHashMap<>();
    }

    /**
     * Sets the entity for the aspect being built.
     *
     * @param entity the entity this aspect will be attached to, must not be null
     * @return this builder instance for method chaining
     * @throws NullPointerException if entity is null
     */
    @Override
    public AspectBuilder entity(@NotNull Entity entity)
    {
        this.entity = Objects.requireNonNull(entity, "Entity cannot be null");
        return this;
    }

    /**
     * Sets the aspect definition that defines the structure and schema for the aspect.
     *
     * @param aspectDef the aspect definition to use, must not be null
     * @return this builder instance for method chaining
     * @throws NullPointerException if aspectDef is null
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
     * @throws NullPointerException if propertyName is null
     */
    @Override
    public AspectBuilder property(@NotNull String propertyName, Object value)
    {
        Objects.requireNonNull(propertyName, "Property name cannot be null");
        this.properties.put(propertyName, value);
        return this;
    }

    /**
     * Adds a property to the aspect being built.
     *
     * @param property the property to add, must not be null
     * @return this builder instance for method chaining
     * @throws NullPointerException if property is null
     */
    @Override
    public AspectBuilder property(@NotNull Property property)
    {
        Objects.requireNonNull(property, "Property cannot be null");
        String propertyName = property.def().name();
        Object value = property.read();
        this.properties.put(propertyName, value);
        return this;
    }

    /**
     * Builds and returns the configured Aspect instance.
     *
     * <p>This method validates that all required components (entity and aspectDef)
     * have been set and delegates to {@link #createAspect()} for the actual aspect
     * creation logic.</p>
     *
     * @return the built Aspect instance, never null
     * @throws IllegalStateException if entity or aspectDef are not set
     */
    @Override
    public final @NotNull Aspect build()
    {
        validateBuildPrerequisites();
        return createAspect();
    }

    /**
     * Resets this builder to its initial state, clearing all configured values.
     * This allows the builder to be reused for creating multiple aspects.
     *
     * @return this builder instance for method chaining
     */
    @Override
    public AspectBuilder reset()
    {
        this.entity = null;
        this.aspectDef = null;
        this.properties.clear();
        return this;
    }

    /**
     * Validates that all required components are set before building an aspect.
     *
     * @throws IllegalStateException if entity or aspectDef are not set
     */
    protected final void validateBuildPrerequisites()
    {
        if (entity == null) {
            throw new IllegalStateException("Entity must be set before building aspect");
        }
        if (aspectDef == null) {
            throw new IllegalStateException("AspectDef must be set before building aspect");
        }
    }

    /**
     * Returns the entity that has been set for this builder.
     *
     * @return the entity, may be null if not yet set
     */
    protected final Entity getEntity()
    {
        return entity;
    }

    /**
     * Returns the aspect definition that has been set for this builder.
     *
     * @return the aspect definition, may be null if not yet set
     */
    protected final AspectDef getAspectDef()
    {
        return aspectDef;
    }

    /**
     * Returns an unmodifiable view of the properties that have been set for this builder.
     *
     * @return an unmodifiable map of property names to values
     */
    protected final Map<String, Object> getProperties()
    {
        return Map.copyOf(properties);
    }

    /**
     * Applies all configured properties to the specified aspect by calling
     * {@link Aspect#unsafeWrite(String, Object)} for each property.
     *
     * <p>This is a convenience method for subclasses that need to apply the
     * collected properties to their created aspect instances.</p>
     *
     * @param aspect the aspect to apply properties to, must not be null
     * @throws NullPointerException if aspect is null
     */
    protected final void applyPropertiesToAspect(@NotNull Aspect aspect)
    {
        Objects.requireNonNull(aspect, "Aspect cannot be null");
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            aspect.write(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Creates and returns the specific aspect type with the configured entity,
     * aspect definition, and properties.
     *
     * <p>This method is called by {@link #build()} after validation has passed.
     * Subclasses must implement this method to provide the specific logic for
     * creating their target aspect type.</p>
     *
     * <p>The entity and aspect definition are guaranteed to be non-null when
     * this method is called. Subclasses can access them via {@link #getEntity()}
     * and {@link #getAspectDef()}, and can apply properties using
     * {@link #applyPropertiesToAspect(Aspect)}.</p>
     *
     * @return the created aspect instance, must not be null
     */
    protected abstract @NotNull Aspect createAspect();
}