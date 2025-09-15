package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Implementation of a LocalEntity that only has Aspects in a single Catalog.
 *
 * @see LocalEntity
 * @see EntityImpl
 */
public class LocalEntityOneCatalogImpl extends EntityImpl implements LocalEntity, Iterable<Catalog>
{
    protected Catalog catalog;

    /**
     * Creates a new LocalEntity for the specified catalog.
     *
     * @param catalog the catalog this entity has its Aspects in
     */
    public LocalEntityOneCatalogImpl(@NotNull Catalog catalog)
    {
        Objects.requireNonNull(catalog, "Catalog may not be null in LocalEntityOneCatalogImpl.");
        this.catalog = catalog;
    }

    /**
     * Creates a new LocalEntity for the specified catalog.
     *
     * @param globalId the id of this catalog-as-entity
     * @param catalog the catalog this entity has its Aspects in
     */
    public LocalEntityOneCatalogImpl(@NotNull UUID globalId, @NotNull Catalog catalog)
    {
        super(globalId);
        Objects.requireNonNull(catalog, "Catalog may not be null in LocalEntityOneCatalogImpl.");
        this.catalog = catalog;
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
