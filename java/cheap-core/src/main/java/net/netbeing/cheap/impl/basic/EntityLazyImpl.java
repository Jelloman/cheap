package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Lazy-initialized Entity implementation that creates identifiers and local entities on-demand.
 * This implementation uses double-checked locking to ensure thread-safe lazy initialization
 * of both the global identifier and local entity reference.
 * <p>
 * This approach conserves memory by only creating objects when they are first accessed,
 * making it suitable for entities that may not be fully utilized.
 * 
 * @see Entity
 * @see LocalEntity
 * @see LocalEntityImpl
 */
public class EntityLazyImpl implements Entity
{
    /** Lazily initialized global identifier. */
    private volatile UUID globalId;
    
    /** Lazily initialized local entity reference. */
    private volatile LocalEntity local;

    /**
     * Creates a new EntityLazyImpl with deferred initialization.
     * Both globalId and local entity will be created on first access.
     */
    public EntityLazyImpl()
    {
    }

    /**
     * Returns the globally unique identifier for this entity, creating it if necessary.
     * Uses double-checked locking for thread-safe lazy initialization.
     * 
     * @return the UUID identifying this entity globally
     */
    @Override
    public @NotNull UUID globalId()
    {
        if (globalId == null) {
            synchronized (this) {
                if (globalId == null) {
                    globalId = UUID.randomUUID();
                }
            }
        }
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
