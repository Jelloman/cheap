package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;

import java.util.Map;
import java.util.UUID;

/**
 * Full-featured Entity implementation that combines both Entity and LocalEntity functionality.
 * This implementation extends EntityBasicImpl and implements LocalEntity to provide
 * a complete entity with both global identification and local aspect management.
 * <p>
 * This class is suitable for entities that need to manage aspects within a catalog
 * context, using lazy initialization and weak references for memory efficiency.
 * 
 * @see EntityBasicImpl
 * @see LocalEntity
 * @see WeakAspectMap
 * @see Aspect
 */
public class EntityFullImpl extends EntityBasicImpl implements LocalEntity
{
    /** Lazily initialized map of aspect definitions to aspects. */
    protected volatile WeakAspectMap aspects;

    /**
     * Creates a new EntityFullImpl with a randomly generated UUID.
     * The aspects map will be initialized on first access.
     */
    public EntityFullImpl()
    {
    }

    /**
     * Creates a new EntityFullImpl with the specified global ID.
     * The aspects map will be initialized on first access.
     * 
     * @param globalId the UUID to use as the global identifier for this entity
     */
    public EntityFullImpl(UUID globalId)
    {
        super(globalId);
    }

    /**
     * Creates a new EntityFullImpl with the specified global ID and initial aspect.
     * 
     * @param globalId the UUID to use as the global identifier for this entity
     * @param initialAspect the initial aspect to store for this entity
     */
    public EntityFullImpl(UUID globalId, Aspect initialAspect)
    {
        super(globalId);
        this.aspects = new WeakAspectMap(3);
        this.aspects.add(initialAspect);
    }

    /**
     * Returns the local entity interface for this entity.
     * Since this implementation is itself a LocalEntity, it returns itself.
     * 
     * @return this entity as a LocalEntity
     */
    @Override
    public LocalEntity local()
    {
        return this;
    }

    /**
     * Returns the entity interface for this local entity.
     * Since this implementation is itself an Entity, it returns itself.
     * 
     * @return this entity as an Entity
     */
    @Override
    public Entity entity()
    {
        return this;
    }

    /**
     * Returns the map of aspect definitions to aspects for this entity.
     * Uses lazy initialization with double-checked locking for thread safety.
     * 
     * @return the aspects map, initialized if necessary
     */
    public Map<AspectDef,Aspect> aspects() {
        if (aspects == null) {
            synchronized(this) {
                if (aspects == null) {
                    aspects = new WeakAspectMap(2);
                }
            }
        }
        return aspects;
    }

    /**
     * Retrieves an aspect by its definition.
     * 
     * @param def the aspect definition to look up
     * @return the aspect for the given definition, or {@code null} if not found
     */
    public Aspect aspect(AspectDef def) {
        if (aspects != null) {
            return aspects.get(def);
        }
        return aspects().get(def);
    }

}
