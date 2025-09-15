package net.netbeing.cheap.impl.basic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Basic implementation of a LocalEntity that only has Aspects in a single Catalog.
 *
 * @see LocalEntity
 * @see EntityImpl
 */
public class LocalEntityOneCatalogImpl extends EntityImpl implements LocalEntity, Iterable<Catalog>
{
    private final Catalog catalog;

    /**
     * Creates a new LocalEntityImpl for the specified entity.
     * The aspects map will be initialized on first access.
     *
     * @param catalog the catalog this entity has its Aspects in
     */
    public LocalEntityOneCatalogImpl(@NotNull Catalog catalog)
    {
        Objects.requireNonNull(catalog, "Catalog may not be null in LocalEntityOneCatalogImpl.");
        this.catalog = catalog;
    }

    /**
     * Return the set of Catalogs that this entity has Aspects in, which is actually
     * this object since it also implements the {@literal Iterable<Catalog>} interface.
     *
     * @return this object, which is an Iterable of Catalogs
     */
    @Override
    public Iterable<Catalog> catalogs()
    {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Aspect getAspect(@NotNull AspectDef def)
    {
        return getAspect(def, this.catalog);
    }

    /**
     * Returns an iterator that returns this object's Catalog once.
     * @return a single-element iterator
     */
    @Override
    public @NotNull Iterator<Catalog> iterator()
    {
        return new Iterator<>()
        {
            boolean hasNext = true;
            @Override
            public boolean hasNext()
            {
                return hasNext;
            }

            @Override
            public Catalog next()
            {
                if (hasNext) {
                    hasNext = false;
                    return catalog;
                }
                throw new NoSuchElementException();
            }
        };
    }
}
