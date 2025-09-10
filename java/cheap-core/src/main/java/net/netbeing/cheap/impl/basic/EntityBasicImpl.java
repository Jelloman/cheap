package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Basic implementation of an Entity with minimal functionality.
 * This implementation provides a globally unique identifier for the entity
 * but does not support local entity functionality.
 * <p>
 * Each entity is identified by a UUID that is either generated automatically
 * or provided during construction.
 * 
 * @see Entity
 * @see LocalEntity
 */
public abstract class EntityBasicImpl implements Entity
{
    /** The globally unique identifier for this entity. */
    private final UUID globalId;

    /**
     * Creates a new EntityBasicImpl with a randomly generated UUID.
     */
    public EntityBasicImpl()
    {
        this(UUID.randomUUID());
    }

    /**
     * Creates a new EntityBasicImpl with the specified global ID.
     * 
     * @param globalId the UUID to use as the global identifier for this entity
     */
    public EntityBasicImpl(UUID globalId)
    {
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

    /**
     * Returns the local entity interface for this entity.
     * This implementation returns {@code null} as it does not support local entity functionality.
     * 
     * @return {@code null} as this implementation does not provide local entity features
     */
    @Override
    public LocalEntity local()
    {
        return null;
    }
}
