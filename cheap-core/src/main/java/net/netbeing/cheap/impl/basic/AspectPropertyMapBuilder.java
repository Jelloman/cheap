package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builder implementation for creating AspectPropertyMapImpl instances using the builder pattern.
 * This class provides a fluent interface for configuring and building aspects with Property objects.
 *
 * <p>This builder enforces strict validation against the AspectDef, ensuring that all properties
 * are defined in the aspect definition and that property values are compatible with their types.
 * PropertyDef objects are only taken from the aspectDef, never constructed.</p>
 *
 * <p>The builder maintains state for the entity, aspect definition, and properties until
 * {@link #build()} is called, at which point it creates and returns an AspectPropertyMapImpl
 * instance with all configured values.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * AspectPropertyMapBuilder builder = new AspectPropertyMapBuilder();
 * Aspect aspect = builder
 *     .entity(entity)
 *     .aspectDef(myAspectDef)
 *     .property("name", "John Doe")
 *     .property("age", 30)
 *     .build();
 * }</pre>
 */
public class AspectPropertyMapBuilder implements AspectBuilder
{
    private Entity entity;
    private AspectDef aspectDef;
    private final Map<String, Property> properties;

    /**
     * Creates a new AspectPropertyMapBuilder with empty initial state.
     */
    public AspectPropertyMapBuilder()
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
        if (!properties.isEmpty()) {
            for (Property property : properties.values()) {
                validateProperty(property);
            }
        }
        return this;
    }

    /**
     * Adds a property with the specified name and value to the aspect being built.
     * This method validates that the property name exists in the AspectDef and that
     * the value is compatible with the property's type and constraints.
     *
     * @param propertyName the name of the property, must not be null
     * @param value the value of the property, may be null depending on the property definition
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if propertyName is null, not defined in aspectDef, or value is invalid
     * @throws IllegalStateException if aspectDef has not been set
     */
    @Override
    public AspectBuilder property(@NotNull String propertyName, Object value)
    {
        Objects.requireNonNull(propertyName, "Property name cannot be null");

        if (aspectDef == null) {
            throw new IllegalStateException("AspectDef must be set before adding properties by name and value.");
        }

        PropertyDef propertyDef = aspectDef.propertyDef(propertyName);
        if (propertyDef == null) {
            throw new IllegalArgumentException("Property '" + propertyName + "' is not defined in AspectDef '" + aspectDef.name() + "'");
        }
        PropertyImpl property = new PropertyImpl(propertyDef, value);

        validateProperty(property);

        this.properties.put(propertyName, property);
        return this;
    }

    /**
     * Adds a property to the aspect being built.
     * This method validates that the property's definition exists in the AspectDef
     * and matches the expected definition.
     *
     * @param property the property to add, must not be null
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if property is null, not defined in aspectDef, or definition doesn't match
     * @throws IllegalStateException if aspectDef has not been set
     */
    @Override
    public AspectBuilder property(@NotNull Property property)
    {
        Objects.requireNonNull(property, "Property cannot be null");

        if (aspectDef != null) {
            validateProperty(property);
        }

        this.properties.put(property.def().name(), property);
        return this;
    }

    /**
     * Validate a property. The AspectDef MUST be set before calling this method.
     *
     * @param property the property to validate
     */
    private void validateProperty(Property property)
    {
        if (aspectDef == null) {
            throw new IllegalStateException("Cannot call validateProperty() before the AspectDef is set.");
        }
        String propName = property.def().name();
        PropertyDef expectedPropertyDef = aspectDef.propertyDef(propName);

        if (expectedPropertyDef == null) {
            if (!aspectDef.canAddProperties()) {
                throw new IllegalArgumentException("AspectDef '" + aspectDef.name() + "' does not define property '"
                    + propName + "' and does not allow adding Properties.");
            }
        } else if (expectedPropertyDef != property.def() && !expectedPropertyDef.fullyEquals(property.def())) {
            throw new IllegalArgumentException("Property definition for '" + propName + "' does not match the definition in AspectDef '" + aspectDef.name() + "'");
        }
        // Validate value, throwing exceptions on failure
        property.def().validatePropertyValue(property.read(), true);
    }

    /**
     * Builds and returns the configured AspectPropertyMapImpl instance.
     *
     * <p>This method validates that all required components (entity and aspectDef)
     * have been set and creates the final AspectPropertyMapImpl instance with all
     * configured properties.</p>
     *
     * @return the built AspectPropertyMapImpl instance, never null
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

        AspectPropertyMapImpl aspect = new AspectPropertyMapImpl(entity, aspectDef);

        // Add all configured properties to the aspect
        for (Property property : properties.values()) {
            // Use unsafe because validation was performed when the property was added to this builder
            aspect.unsafeAdd(property);
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