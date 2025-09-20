package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A builder interface for creating Aspect instances using the builder pattern.
 * This interface allows fluent configuration of an aspect's entity, definition,
 * and property values before building the final Aspect instance.
 *
 * <p>The builder pattern provides a clean and readable way to construct aspects
 * with multiple properties, especially when dealing with complex aspect structures.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * AspectBuilder builder = someImplementation();
 * Aspect aspect = builder
 *     .entityId(UUID.randomUUID())
 *     .aspectDef(myAspectDef)
 *     .property("name", "John Doe")
 *     .property("age", 30)
 *     .build();
 * }</pre>
 */
public interface AspectBuilder
{
    /**
     * Sets the entity ID for the aspect being built.
     *
     * @param entityId the UUID of the entity this aspect will be attached to, must not be null
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if entityId is null
     */
    AspectBuilder entityId(@NotNull UUID entityId);

    /**
     * Sets the entity for the aspect being built.
     *
     * @param entity the entity this aspect will be attached to, must not be null
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if entity is null
     */
    AspectBuilder entity(@NotNull Entity entity);

    /**
     * Sets the aspect definition that defines the structure and schema for the aspect.
     *
     * @param aspectDef the aspect definition to use, must not be null
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if aspectDef is null
     */
    AspectBuilder aspectDef(@NotNull AspectDef aspectDef);

    /**
     * Adds a property with the specified name and value to the aspect being built.
     *
     * @param propertyName the name of the property, must not be null
     * @param value the value of the property, may be null depending on the property definition
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if propertyName is null
     */
    AspectBuilder property(@NotNull String propertyName, Object value);

    /**
     * Adds a property to the aspect being built.
     *
     * @param property the property to add, must not be null
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if property is null
     */
    AspectBuilder property(@NotNull Property property);

    /**
     * Builds and returns the configured Aspect instance.
     *
     * <p>This method validates that all required components (entity/entityId and aspectDef)
     * have been set and creates the final Aspect instance with all configured properties.</p>
     *
     * @return the built Aspect instance, never null
     * @throws IllegalStateException if required components are not set or if the builder is in an invalid state
     */
    @NotNull Aspect build();

    /**
     * Resets this builder to its initial state, clearing all configured values.
     * This allows the builder to be reused for creating multiple aspects.
     *
     * @return this builder instance for method chaining
     */
    AspectBuilder reset();
}