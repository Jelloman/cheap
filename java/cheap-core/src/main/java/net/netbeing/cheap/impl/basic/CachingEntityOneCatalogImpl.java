package net.netbeing.cheap.impl.basic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Implementation of LocalEntity that only has Aspects in a single Catalog, and
 * caches Aspects within this instance for faster lookup.
 *
 * <p>The {@link #getAspect(AspectDef, Catalog) getAspect} method will return a valid
 * response if it is passed a different Catalog, but it will only cache the Aspect
 * if it resides in the configured Catalog.</p>
 *
 * @see LocalEntityOneCatalogImpl
 */
public class CachingEntityOneCatalogImpl extends LocalEntityOneCatalogImpl
{
    /** Lazily initialized map of aspect definitions to aspects. */
    protected volatile Cache<@NotNull AspectDef, @NotNull Aspect> aspects;

    /**
     * Creates a new CachingEntityOneCatalogImpl for the specified catalog.
     * The aspects cache will be initialized on first access.
     *
     * @param catalog the catalog this entity has its Aspects in
     */
    public CachingEntityOneCatalogImpl(@NotNull Catalog catalog)
    {
        super(catalog);
    }

    private void createAspectCache()
    {
        this.aspects = CacheBuilder.newBuilder()
            .initialCapacity(3)
            .weakValues()
            .build();
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
    public Aspect getAspect(@NotNull AspectDef def)
    {
        Aspect a = getAspectIfPresent(def);
        if (a != null) {
            return a;
        }
        a = super.getAspect(def);
        if (a == null) {
            return null;
        }
        if (aspects == null) {
            synchronized (this) {
                if (aspects == null) {
                    createAspectCache();
                }
            }
        }
        aspects.put(def, a);
        return a;
    }
}
