package net.netbeing.cheap.impl.basic;

import com.google.common.collect.Iterables;
import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Basic implementation of a LocalEntity that only has Aspects in a single Catalog.
 *
 * @see LocalEntity
 * @see EntityImpl
 */
public class LocalEntityMultiCatalogImpl extends EntityImpl implements LocalEntity
{
    private final Set<Catalog> catalogs;

    /**
     * Creates a new LocalEntityMultiCatalogImpl for the specified Catalog.
     * The aspects map will be initialized on first access.
     *
     * @param catalog the catalog this entity has its Aspects in
     */
    public LocalEntityMultiCatalogImpl(@NotNull Catalog catalog)
    {
        Objects.requireNonNull(catalog, "Catalog may not be null in LocalEntityMultiCatalogImpl.");
        this.catalogs = new HashSet<>();
        this.catalogs.add(catalog);
    }

    /**
     * Return the set of Catalogs that this entity has Aspects in.
     *
     * @return this object, which is an Iterable of Catalogs
     */
    @Override
    public Iterable<Catalog> catalogs()
    {
        return Iterables.unmodifiableIterable(catalogs);
    }
}
