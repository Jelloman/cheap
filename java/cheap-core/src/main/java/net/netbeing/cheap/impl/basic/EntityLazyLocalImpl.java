package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Entity implementation with a fixed global ID and lazy-initialized local entity.
 * This implementation provides a known global identifier while deferring local
 * entity creation until first access.
 * <p>
 * This approach is useful when you have a predetermined entity identifier but
 * want to conserve memory by only creating the local entity when needed.
 * 
 * @see Entity
 * @see LocalEntity
 * @see LocalEntityImpl
 */
public class EntityLazyLocalImpl implements Entity
{
    /** The fixed global identifier for this entity. */
    private final UUID globalId;
    
    /** Lazily initialized local entity reference. */
    private volatile LocalEntity local;

    /**
     * Creates a new EntityLazyLocalImpl with the specified global ID.
     * The local entity will be created on first access.
     * 
     * @param globalId the UUID to use as the global identifier for this entity
     */
    public EntityLazyLocalImpl(UUID globalId)
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
     * Returns the local entity interface for this entity, creating it if necessary.
     * Uses double-checked locking for thread-safe lazy initialization.
     * 
     * @return the local entity reference
     */
    @Override
    public LocalEntity local()
    {
        if (local == null) {
            synchronized (this) {
                if (local == null) {
                    local = new LocalEntityImpl(this);
                }
            }
        }
        return local;
    }
}
