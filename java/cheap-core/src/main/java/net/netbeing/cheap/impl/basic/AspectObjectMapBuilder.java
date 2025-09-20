package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builder implementation for creating AspectObjectMapImpl instances using the builder pattern.
 * This class provides a fluent interface for configuring and building aspects with property values.
 *
 * <p>The builder maintains state for the entity, aspect definition, and properties until
 * {@link #build()} is called, at which point it creates and returns an AspectObjectMapImpl
 * instance with all configured values.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * AspectObjectMapBuilder builder = new AspectObjectMapBuilder();
 * Aspect aspect = builder
 *     .entityId(UUID.randomUUID())
 *     .aspectDef(myAspectDef)
 *     .property("name", "John Doe")
 *     .property("age", 30)
 *     .build();
 * }</pre>
 */
public class AspectObjectMapBuilder implements AspectBuilder
{
    private Entity entity;
    private AspectDef aspectDef;
    private final Map<String, Object> properties;

    /**
     * Creates a new AspectObjectMapBuilder with empty initial state.
     */
    public AspectObjectMapBuilder()
    {
        this.properties = new LinkedHashMap<>();
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
        this.entity = Objects.requireNonNull(entity, "Entity cannot be null");
        return this;
    }

    /**
     * Sets the aspect definition that defines the structure and schema for the aspect.
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
        this.properties.put(propertyName, value);
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
        Object value = property.unsafeRead();
        this.properties.put(propertyName, value);
        return this;
    }

    /**
     * Builds and returns the configured AspectObjectMapImpl instance.
     *
     * <p>This method validates that all required components (entity and aspectDef)
     * have been set and creates the final AspectObjectMapImpl instance with all
     * configured properties.</p>
     *
     * @return the built AspectObjectMapImpl instance, never null
     * @throws IllegalStateException if entity or aspectDef are not set
     */
    @Override
    public @NotNull Aspect build()
    {
        if (entity == null) {
            throw new IllegalStateException("Entity must be set before building aspect");
        }
        if (aspectDef == null) {
            throw new IllegalStateException("AspectDef must be set before building aspect");
        }

        AspectObjectMapImpl aspect = new AspectObjectMapImpl(entity, aspectDef);

        // Add all configured properties to the aspect
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            aspect.unsafeWrite(entry.getKey(), entry.getValue());
        }

        return aspect;
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
}