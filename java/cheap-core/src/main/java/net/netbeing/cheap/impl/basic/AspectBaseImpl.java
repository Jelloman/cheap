package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Abstract base class for Aspect implementations providing common functionality.
 * This class manages the basic relationships between an aspect, its entity
 * and aspect definition.
 * <p>
 * Subclasses must implement the specific property storage and access mechanisms
 * while this base class handles the fundamental aspect metadata.
 * 
 * @see Aspect
 * @see AspectDef
 * @see Entity
 */
public abstract class AspectBaseImpl implements Aspect
{
    /** The entity this aspect is attached to. */
    protected Entity entity;
    
    /** The aspect definition describing this aspect's structure. */
    protected final AspectDef def;

    /**
     * Creates a new AspectBaseImpl with the specified entity and aspect definition.
     *
     * @param entity the entity this aspect is attached to, may be null
     * @param def the aspect definition describing this aspect's structure
     */
    public AspectBaseImpl(Entity entity, @NotNull AspectDef def)
    {
        Objects.requireNonNull(def, "Aspect may not have a null AspectDef.");
        this.entity = entity;
        this.def = def;
    }

    /**
     * Returns the entity this aspect is attached to.
     * 
     * @return the entity owning this aspect
     */
    @Override
    public Entity entity()
    {
        return entity;
    }

    /**
     * Set the entity that owns this aspect. If the entity is already set
     * and this is not flagged as transferable, an Exception will be thrown.
     *
     * @param entity the entity to attach this aspect to, never null
     * @throws IllegalStateException if the aspect is non-transferable and already attached to another entity
     */
    @Override
    public void setEntity(@NotNull Entity entity)
    {
        Objects.requireNonNull(entity, "Aspects may not be assigned a null entity.");
        if (this.entity != null && this.entity != entity && !isTransferable()) {
            throw new IllegalStateException("An Aspect flagged as non-transferable may not be reassigned to a different entity.");
        }
        this.entity = entity;
    }

    /**
     * Returns the aspect definition describing this aspect's structure.
     * 
     * @return the aspect definition for this aspect
     */
    @Override
    public AspectDef def()
    {
        return def;
    }
}
