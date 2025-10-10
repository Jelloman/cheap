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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Catalog;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

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
public class CachingEntityMultiCatalogImpl extends LocalEntityMultiCatalogImpl
{
    /** Lazily initialized map of aspect definitions to aspects. */
    protected volatile Cache<@NotNull AspectDef, @NotNull Aspect> aspects;

    /**
     * Creates a new CachingEntityMultiCatalogImpl for the specified catalog.
     * The aspects cache will be initialized on first access.
     *
     * @param catalog the catalog this entity has its Aspects in
     */
    public CachingEntityMultiCatalogImpl(@NotNull Catalog catalog)
    {
        super(catalog);
    }

    /**
     * Creates a new CachingEntityMultiCatalogImpl with the specified global ID and catalog.
     * The aspects cache will be initialized on first access.
     *
     * @param globalId the UUID for this entity
     * @param catalog the catalog this entity has its Aspects in
     */
    public CachingEntityMultiCatalogImpl(@NotNull UUID globalId, @NotNull Catalog catalog)
    {
        super(globalId, catalog);
    }

    private void createAspectCache()
    {
        this.aspects = CacheBuilder.newBuilder()
            .initialCapacity(3)
            .weakValues()
            .build();
    }

    private void createAspectCacheIfNecessary()
    {
        if (aspects == null) {
            synchronized (this) {
                if (aspects == null) {
                    createAspectCache();
                }
            }
        }
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
        createAspectCacheIfNecessary();
        aspects.put(def, a);
        return a;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void attach(@NotNull Aspect aspect)
    {
        aspect.setEntity(this);
        createAspectCacheIfNecessary();
        aspects.put(aspect.def(), aspect);
    }

}
