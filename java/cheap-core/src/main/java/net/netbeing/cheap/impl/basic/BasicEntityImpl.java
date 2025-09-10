package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Basic implementation of an Entity that supports both global identification
 * and optional local entity functionality.
 * <p>
 * Unlike {@link EntityBasicImpl}, this implementation supports setting and
 * managing a local entity reference, making it suitable for entities that
 * need local catalog-specific functionality.
 * 
 * @see Entity
 * @see LocalEntity
 * @see EntityBasicImpl
 */
public class BasicEntityImpl implements Entity
{
    /** The globally unique identifier for this entity. */
    private final UUID globalId;
    
    /** The local entity reference, or null if not set. */
    private LocalEntity local;

    /**
     * Creates a new BasicEntityImpl with a randomly generated UUID and no local entity.
     */
    public BasicEntityImpl()
    {
        this(UUID.randomUUID(), null);
    }

    /**
     * Creates a new BasicEntityImpl with the specified global ID and no local entity.
     * 
     * @param globalId the UUID to use as the global identifier for this entity
     */
    public BasicEntityImpl(UUID globalId)
    {
        this(globalId, null);
    }

    /**
     * Creates a new BasicEntityImpl with the specified global ID and local entity.
     * 
     * @param globalId the UUID to use as the global identifier for this entity
     * @param local the local entity reference, or null if none
     */
    public BasicEntityImpl(UUID globalId, LocalEntity local)
    {
        this.globalId = globalId;
        this.local = local;
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
     * 
     * @return the local entity reference, or {@code null} if not set
     */
    @Override
    public LocalEntity local()
    {
        return local;
    }

    /**
     * Sets the local entity reference for this entity.
     * 
     * @param local the local entity reference to set, or null to clear
     */
    public void setLocal(LocalEntity local)
    {
        this.local = local;
    }
}
