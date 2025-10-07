package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

/**
 * Builder implementation for creating AspectObjectMapImpl instances using the builder pattern.
 * This class provides a fluent interface for configuring and building aspects with property values.
 *
 * <p>This implementation extends {@link AspectBuilderBase} to inherit common builder functionality
 * while providing specific logic for creating {@link AspectObjectMapImpl} instances.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * AspectObjectMapBuilder builder = new AspectObjectMapBuilder();
 * Aspect aspect = builder
 *     .entity(myEntity)
 *     .aspectDef(myAspectDef)
 *     .property("name", "John Doe")
 *     .property("age", 30)
 *     .build();
 * }</pre>
 */
public class AspectObjectMapBuilder extends AspectBuilderBase
{
    /**
     * Creates a new AspectObjectMapBuilder with empty initial state.
     */
    public AspectObjectMapBuilder()
    {
        super();
    }

    /**
     * Creates and returns an AspectObjectMapImpl instance with the configured entity,
     * aspect definition, and properties.
     *
     * <p>This method creates a new {@link AspectObjectMapImpl} and applies all
     * configured properties to it using the {@link #applyPropertiesToAspect(Aspect)}
     * method inherited from the base class.</p>
     *
     * @return the created AspectObjectMapImpl instance, never null
     */
    @Override
    protected @NotNull Aspect createAspect()
    {
        AspectObjectMapImpl aspect = new AspectObjectMapImpl(getEntity(), getAspectDef());
        applyPropertiesToAspect(aspect);
        return aspect;
    }
}