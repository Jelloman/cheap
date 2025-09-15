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

    /**
     * Compare to another entity. This implementation is final and only compares global IDs.
     * This will force the generation of the global ID.
     *
     * @param o the object with which to compare.
     * @return true if o is an Entity and has the same globalId
     */
    @Override
    public final boolean equals(Object o)
    {
        if (!(o instanceof Entity entity)) {
            return false;
        }
        return Objects.equals(this.globalId(), entity.globalId());
    }

    /**
     * Generate this object's hash code. This implementation is final and only uses global ID.
     * This will force the generation of the global ID.
     *
     * @return hashCode of the globalId
     */
    @Override
    public int hashCode()
    {
        return Objects.hashCode(this.globalId());
    }
}
