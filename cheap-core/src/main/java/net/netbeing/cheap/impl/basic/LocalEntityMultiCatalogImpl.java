/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.impl.basic;

import com.google.common.collect.Iterables;
import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
     * Creates a new LocalEntityMultiCatalogImpl with the specified global ID and catalog.
     * The aspects map will be initialized on first access.
     *
     * @param globalId the UUID for this entity
     * @param catalog the catalog this entity has its Aspects in
     */
    public LocalEntityMultiCatalogImpl(@NotNull UUID globalId, @NotNull Catalog catalog)
    {
        super(globalId);
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
