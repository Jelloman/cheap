package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Entity implementation with a lazily-initialized global ID.
 *
 * @see Entity
 */
public class EntityLazyIdImpl implements Entity
{
    /** Lazily initialized global identifier. */
    private volatile UUID globalId;
    
    /**
     * Creates a new EntityLazyIdImpl with a new local entity.
     * The global ID will be created on first access.
     */
    public EntityLazyIdImpl()
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
}
