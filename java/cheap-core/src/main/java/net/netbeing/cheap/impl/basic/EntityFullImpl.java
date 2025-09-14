package net.netbeing.cheap.impl.basic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;
import org.jetbrains.annotations.NotNull;

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
    protected volatile Cache<@NotNull AspectDef, @NotNull Aspect> aspects;

    /**
     * Creates a new EntityFullImpl with a randomly generated UUID.
     */
    public EntityFullImpl()
    {
    }

    /**
     * Creates a new EntityFullImpl with the specified global ID.
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
        createAspectCache();
        this.aspects.put(initialAspect.def(), initialAspect);
    }

    private void createAspectCache()
    {
        this.aspects = CacheBuilder.newBuilder()
            .initialCapacity(3)
            .weakValues()
            .build();
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
    public @NotNull Entity entity()
    {
        return this;
    }

    /**
     * Add an Aspect to this local entity's cache. This is NOT a persistence method;
     * to persist an Aspect, add it to a Catalog.
     *
     * @return the previous aspect with this AspectDef, if any
     */
    @Override
    public Aspect cache(@NotNull Aspect aspect)
    {
        Aspect other = null;
        if (aspects == null) {
            synchronized(this) {
                if (aspects == null) {
                    createAspectCache();
                }
            }
        } else {
            other = aspects.getIfPresent(aspect.def());
        }
        aspects.put(aspect.def(), aspect);
        return other;
    }

    /**
     * Retrieves an aspect by its definition.
     * 
     * @param def the aspect definition to look up
     * @return the aspect for the given definition, or {@code null} if not found
     */
    public Aspect getAspectIfPresent(@NotNull AspectDef def)
    {
        if (aspects != null) {
            return aspects.getIfPresent(def);
        }
        return null;
    }

}
