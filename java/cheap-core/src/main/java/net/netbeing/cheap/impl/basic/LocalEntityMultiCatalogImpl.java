package net.netbeing.cheap.impl.basic;

import com.google.common.collect.Iterables;
import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Implementation of a LocalEntity that has Aspects in multiple Catalogs.
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

    /**
     * Attach the given aspect to this entity, then add it to the specified catalog.
     * Note that the set of Aspect types stored in a catalog cannot be implicitly
     * extended; to add a new type of entity to a non-strict catalog, call its
     * {@link Catalog#extend(AspectDef) extend} method.
     *
     * <p>This method must invoke the {@link Aspect#setEntity(Entity) setEntity}
     * method on the Aspect.</p>
     *
     * @param aspect the aspect to attach
     */
    @Override
    public void attachAndSave(@NotNull Aspect aspect, @NotNull Catalog catalog)
    {
        super.attachAndSave(aspect, catalog);
        catalogs.add(catalog);
    }

}
