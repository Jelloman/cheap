package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Entity implementation with a fixed local entity and lazy-initialized global ID.
 * This implementation provides an immediate local entity reference while deferring
 * global identifier creation until first access.
 * <p>
 * This approach is useful when you need local entity functionality immediately
 * but want to defer the cost of global ID generation until it's actually needed.
 * 
 * @see Entity
 * @see LocalEntity
 * @see LocalEntityImpl
 */
public class EntityLazyIdImpl implements Entity
{
    /** Lazily initialized global identifier. */
    private volatile UUID globalId;
    
    /** The fixed local entity reference. */
    private final LocalEntity local;

    /**
     * Creates a new EntityLazyIdImpl with a new local entity.
     * The global ID will be created on first access.
     */
    public EntityLazyIdImpl()
    {
        this.local = new LocalEntityImpl(this);
    }

    /**
     * Creates a new EntityLazyIdImpl with the specified local entity.
     * The global ID will be created on first access.
     * 
     * @param local the local entity to use for this entity
     */
    public EntityLazyIdImpl(LocalEntity local)
    {
        this.local = local;
    }

    /**
     * Creates a new EntityLazyIdImpl with a new local entity containing the initial aspect.
     * The global ID will be created on first access.
     * 
     * @param aspect the initial aspect to store in the local entity
     */
    public EntityLazyIdImpl(Aspect aspect)
    {
        this.local = new LocalEntityImpl(this, aspect);
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
     * Returns the local entity interface for this entity.
     * 
     * @return the local entity reference
     */
    @Override
    public LocalEntity local()
    {
        return local;
    }
}
