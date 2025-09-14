package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

/**
 * CachingEntity extends LocalEntity with methods to cache Aspect references
 * directly in the Entity object (for performance reasons).
 *
 * <p>This is NOT a definitive view of all of an Entity's aspects; it is only
 * a cache. Catalogs remain the source of truth about Aspects.
 */
public interface CachingEntity extends LocalEntity
{
    /**
     * Add an Aspect to this local entity's cache. This is NOT a persistence method;
     * to persist an Aspect, add it to a Catalog.
     *
     * @return the previous Aspect entry in the cache with this AspectDef, if any
     */
    Aspect cache(@NotNull Aspect aspect);

    /**
     * Retrieves a specific aspect attached to this entity by its definition, but
     * only if it's present in the cache.
     *
     * @param def the aspect definition to look up, must not be null
     * @return the aspect instance matching the definition, or null if not found
     */
    Aspect getAspectIfPresent(@NotNull AspectDef def);

    /**
     * Retrieves a specific aspect attached to this entity by its definition.
     * If the aspect is not already referenced by this LocalEntity, attempts
     * to load the aspect from the provided Catalog.
     *
     * <p>If no aspect of the specified type is attached to this entity,
     * this method returns null.</p>
     *
     * @param def the aspect definition to look up, must not be null
     * @return the aspect instance matching the definition, or null if not found
     */
    @Override
    default Aspect getAspect(@NotNull AspectDef def)
    {
        Aspect a = getAspectIfPresent(def);
        if (a != null) {
            return a;
        }
        return LocalEntity.super.getAspect(def);
    }
}
