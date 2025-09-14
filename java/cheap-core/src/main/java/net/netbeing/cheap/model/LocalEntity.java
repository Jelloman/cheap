package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

/**
 * Provides convenience access to a cache of an entity's aspects. This is NOT a
 * definitive view of all of an Entity's aspects; it is only a cache. The Catalog
 * remains the source of truth about Aspects.
 */
public interface LocalEntity
{
    /**
     * Returns the global entity that this local entity represents.
     *
     * @return the global entity instance, never null
     */
    @NotNull Entity entity();

    /**
     * Add an Aspect to this local entity's cache. This is NOT a persistence method;
     * to persist an Aspect, add it to a Catalog.
     *
     * @return the previous aspect with this AspectDef, if any
     */
    Aspect cache(@NotNull Aspect aspect);

    /**
     * Retrieves a specific aspect attached to this entity by its definition.
     * This is a convenience method equivalent to calling {@code aspects().get(def)}.
     *
     * <p>If no aspect of the specified type is attached to this entity,
     * this method returns null.</p>
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
    default Aspect getAspect(@NotNull AspectDef def, @NotNull Catalog cat)
    {
        Aspect a = getAspectIfPresent(def);
        if (a != null) {
            return a;
        }
        a = cat.aspects(def).get(entity());
        if (a != null) {
            cache(a);
        }
        return a;
    }
}
