package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;

import java.util.Map;

/**
 * Basic implementation of a LocalEntity that manages aspects for an entity
 * within a specific catalog context.
 * <p>
 * This implementation uses a WeakAspectMap for lazy initialization and
 * thread-safe aspect storage. The aspects map is initialized on first access
 * to conserve memory for entities that may not have local aspects.
 * 
 * @see LocalEntity
 * @see Entity
 * @see WeakAspectMap
 * @see Aspect
 */
public class LocalEntityImpl implements LocalEntity
{
    /** The entity this local entity represents. */
    protected final Entity entity;
    
    /** Lazily initialized map of aspect definitions to aspects. */
    protected volatile WeakAspectMap aspects;

    /**
     * Creates a new LocalEntityImpl for the specified entity.
     * The aspects map will be initialized on first access.
     * 
     * @param entity the entity this local entity represents
     */
    public LocalEntityImpl(Entity entity)
    {
        this.entity = entity;
    }

    /**
     * Creates a new LocalEntityImpl for the specified entity with an initial aspect.
     * 
     * @param entity the entity this local entity represents
     * @param initialAspect the initial aspect to store for this entity
     */
    public LocalEntityImpl(Entity entity, Aspect initialAspect)
    {
        this.entity = entity;
        this.aspects = new WeakAspectMap(3);
        this.aspects.add(initialAspect);
    }

    /**
     * Returns the entity this local entity represents.
     * 
     * @return the underlying entity
     */
    @Override
    public Entity entity()
    {
        return entity;
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
