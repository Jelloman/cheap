package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.MutableAspectDef;
import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Full implementation of an AspectDef with explicit control over all four boolean flags.
 * This implementation allows specifying exactly which operations are allowed through
 * constructor parameters, providing fine-grained control over aspect definition behavior.
 * <p>
 * Unlike {@link MutableAspectDefImpl} which always allows all operations, or
 * {@link ImmutableAspectDefImpl} which allows none, this class allows any combination
 * of the four boolean flags to be set independently.
 *
 * @see AspectDefBase
 * @see MutableAspectDefImpl
 * @see ImmutableAspectDefImpl
 * @see PropertyDef
 */
public class FullAspectDefImpl extends AspectDefBase implements MutableAspectDef
{
    /** Whether this aspect definition is readable. */
    private final boolean isReadable;

    /** Whether this aspect definition is writable. */
    private final boolean isWritable;

    /** Whether properties can be added to this aspect definition. */
    private final boolean canAddProperties;

    /** Whether properties can be removed from this aspect definition. */
    private final boolean canRemoveProperties;

    /**
     * Creates a new FullAspectDefImpl with all flags explicitly specified.
     *
     * @param name the name of this aspect definition
     * @param globalId the global ID for this aspect definition
     * @param propertyDefs the map of property names to property definitions
     * @param isReadable whether this aspect definition is readable
     * @param isWritable whether this aspect definition is writable
     * @param canAddProperties whether properties can be added
     * @param canRemoveProperties whether properties can be removed
     */
    public FullAspectDefImpl(
        @NotNull String name,
        @NotNull UUID globalId,
        @NotNull Map<String, PropertyDef> propertyDefs,
        boolean isReadable,
        boolean isWritable,
        boolean canAddProperties,
        boolean canRemoveProperties)
    {
        super(name, globalId, propertyDefs);
        this.isReadable = isReadable;
        this.isWritable = isWritable;
        this.canAddProperties = canAddProperties;
        this.canRemoveProperties = canRemoveProperties;
    }

    /**
     * Returns whether this aspect definition is readable.
     *
     * @return the readable flag value
     */
    @Override
    public boolean isReadable()
    {
        return isReadable;
    }

    /**
     * Returns whether this aspect definition is writable.
     *
     * @return the writable flag value
     */
    @Override
    public boolean isWritable()
    {
        return isWritable;
    }

    /**
     * Returns whether properties can be added to this aspect definition.
     *
     * @return the canAddProperties flag value
     */
    @Override
    public boolean canAddProperties()
    {
        return canAddProperties;
    }

    /**
     * Returns whether properties can be removed from this aspect definition.
     *
     * @return the canRemoveProperties flag value
     */
    @Override
    public boolean canRemoveProperties()
    {
        return canRemoveProperties;
    }

    /**
     * Adds a property definition to this aspect definition.
     *
     * @param prop the property definition to add
     * @return the previous property definition with the same name, or {@code null} if none existed
     * @throws UnsupportedOperationException if {@link #canAddProperties()} returns {@code false}
     */
    @Override
    public PropertyDef add(@NotNull PropertyDef prop)
    {
        if (!canAddProperties) {
            throw new UnsupportedOperationException(
                "Cannot add properties to AspectDef '" + name + "' (canAddProperties=false)");
        }
        return propertyDefs.put(prop.name(), prop);
    }

    /**
     * Removes a property definition from this aspect definition.
     *
     * @param prop the property definition to remove
     * @return the removed property definition, or {@code null} if it wasn't present
     * @throws UnsupportedOperationException if {@link #canRemoveProperties()} returns {@code false}
     */
    @Override
    public PropertyDef remove(@NotNull PropertyDef prop)
    {
        if (!canRemoveProperties) {
            throw new UnsupportedOperationException(
                "Cannot remove properties from AspectDef '" + name + "' (canRemoveProperties=false)");
        }
        return propertyDefs.remove(prop.name());
    }
}
