package net.netbeing.cheap.impl.basic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

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
    protected volatile Cache<@NotNull AspectDef, @NotNull Aspect> aspects;

    /**
     * Creates a new LocalEntityImpl for the specified entity.
     * The aspects map will be initialized on first access.
     * 
     * @param entity the entity this local entity represents
     */
    public LocalEntityImpl(@NotNull Entity entity)
    {
        Objects.requireNonNull(entity, "Entity may not be null in LocalEntity.");
        this.entity = entity;
    }

    /**
     * Creates a new LocalEntityImpl for the specified entity with an initial aspect.
     * 
     * @param entity the entity this local entity represents
     * @param initialAspect the initial aspect to store for this entity
     */
    public LocalEntityImpl(@NotNull Entity entity, @NotNull Aspect initialAspect)
    {
        Objects.requireNonNull(entity, "Entity may not be null in LocalEntity.");
        Objects.requireNonNull(initialAspect, "Initial Aspect may not be null in LocalEntity.");
        this.entity = entity;
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
     * Returns the entity this local entity represents.
     * 
     * @return the underlying entity
     */
    @Override
    public @NotNull Entity entity()
    {
        return entity;
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
    @Override
    public Aspect getAspectIfPresent(@NotNull AspectDef def)
    {
        if (aspects != null) {
            return aspects.getIfPresent(def);
        }
        return null;
    }

}
