package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Basic implementation of an Entity reference with the minimum functionality.
 *
 * @see Entity
 */
public class EntityImpl implements Entity
{
    /** The globally unique identifier for this entity. */
    private final UUID globalId;

    /**
     * Creates a new EntityBasicImpl with a randomly generated UUID.
     */
    public EntityImpl()
    {
        this(UUID.randomUUID());
    }

    /**
     * Creates a new EntityBasicImpl with the specified global ID.
     * 
     * @param globalId the UUID to use as the global identifier for this entity
     */
    public EntityImpl(UUID globalId)
    {
        Objects.requireNonNull(globalId, "EntityBasicImpl may not have a null UUID.");
        this.globalId = globalId;
    }

    /**
     * Returns the globally unique identifier for this entity.
     * 
     * @return the UUID identifying this entity globally
     */
    @Override
    public @NotNull UUID globalId()
    {
        return globalId;
    }
}
