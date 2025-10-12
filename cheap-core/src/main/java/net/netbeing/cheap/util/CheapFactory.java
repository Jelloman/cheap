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

package net.netbeing.cheap.util;

import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Factory class providing instance-based factory methods for creating instances of all
 * concrete implementation classes in the net.netbeing.cheap.impl.basic package.
 * <p>
 * All factory methods return interface types from net.netbeing.cheap.model
 * rather than concrete implementation types, promoting loose coupling and
 * implementation hiding.
 * <p>
 * This factory can be configured with a default LocalEntityType to control
 * which implementation is used when creating LocalEntity instances.
 * <p>
 * This factory simplifies object creation and provides a clean API for
 * instantiating Cheap model objects without directly depending on implementation classes.
 */
public class CheapFactory
{
    /** The default LocalEntity type to create when not explicitly specified. */
    private final LocalEntityType defaultLocalEntityType;
    private final Class<? extends AspectBuilder> aspectBuilderClass;
    private final Map<String, AspectDef> aspectDefs = new HashMap<>();
    private final Map<String, HierarchyDef> hierarchyDefs = new HashMap<>();
    private final Map<UUID, Entity> entities = new HashMap<>();
    private final PropertyValueAdapter propertyAdapter = new PropertyValueAdapter();
    private Catalog catalog = null;
    private TimeZone timeZone;

    /**
     * Creates a new CheapFactory with the defaults of LocalEntityType.SINGLE_CATALOG
     * and AspectObjectMapBuilder.class.
     */
    public CheapFactory()
    {
        this(null, null);
    }

    /**
     * Creates a new CheapFactory with the specified default LocalEntity type and
     * aspectBuilderClass. If null, these will default to LocalEntityType.SINGLE_CATALOG
     * and AspectObjectMapBuilder.class.
     *
     * @param defaultLocalEntityType the default type of LocalEntity to create
     * @param aspectBuilderClass the default aspectBuilderClass
     */
    public CheapFactory(LocalEntityType defaultLocalEntityType, Class<? extends AspectBuilder> aspectBuilderClass)
    {
        this.defaultLocalEntityType = defaultLocalEntityType != null ? defaultLocalEntityType : LocalEntityType.SINGLE_CATALOG;
        this.aspectBuilderClass = aspectBuilderClass != null ? aspectBuilderClass : AspectObjectMapBuilder.class;
    }

    public TimeZone getTimeZone()
    {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone)
    {
        this.timeZone = timeZone;
        propertyAdapter.setTimeZone(timeZone);
    }

    /**
     * Returns the default LocalEntity type configured for this factory.
     *
     * @return the default LocalEntity type
     */
    public @NotNull LocalEntityType getDefaultLocalEntityType()
    {
        return defaultLocalEntityType;
    }

    /**
     * Returns the default AspectBuilder type configured for this factory.
     *
     * @return the default AspectBuilder type
     */
    public Class<? extends AspectBuilder> getAspectBuilderClass()
    {
        return aspectBuilderClass;
    }

    /**
     * Returns the default AspectBuilder type configured for this factory.
     *
     * @return the default AspectBuilder type
     */
    public AspectBuilder createAspectBuilder()
    {
        try {
            return aspectBuilderClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Catalog getCatalog()
    {
        return catalog;
    }

    public void setCatalog(Catalog catalog)
    {
        this.catalog = catalog;
    }

    /**
     * Return the AspectDef registered in this factory with the given name, or
     * null if not found.
     *
     * @param name aspectDef name
     * @return the aspectDef with that name
     */
    public AspectDef getAspectDef(@NotNull String name)
    {
        return aspectDefs.get(name);
    }

    /**
     * Register an AspectDef with this factory.
     *
     * @param aspectDef the aspectDef to register
     * @return the existing AspectDef registered under that name, if any
     */
    @SuppressWarnings("UnusedReturnValue")
    public AspectDef registerAspectDef(AspectDef aspectDef)
    {
        return aspectDefs.put(aspectDef.name(), aspectDef);
    }

    /**
     * Return the HierarchyDef registered in this factory with the given name, or
     * null if not found.
     *
     * @param name hierarchyDef name
     * @return the hierarchyDef with that name
     */
    public HierarchyDef getHierarchyDef(@NotNull String name)
    {
        return hierarchyDefs.get(name);
    }

    /**
     * Register a HierarchyDef with this factory.
     *
     * @param hierarchyDef the hierarchyDef to register
     * @return the existing HierarchyDef registered under that name, if any
     */
    @SuppressWarnings("UnusedReturnValue")
    public HierarchyDef registerHierarchyDef(HierarchyDef hierarchyDef)
    {
        return hierarchyDefs.put(hierarchyDef.name(), hierarchyDef);
    }

    /**
     * Return the Entity registered in this factory with the given id, or
     * null if not found.
     *
     * @param id entity id
     * @return the entity with that id
     */
    public Entity getEntity(@NotNull UUID id)
    {
        return entities.get(id);
    }

    /**
     * Register an Entity with this factory.
     *
     * @param entity the Entity to register
     * @return the existing Entity registered under that id, if any
     */
    public Entity registerEntity(Entity entity)
    {
        return entities.put(entity.globalId(), entity);
    }

    // ===== Catalog Factory Methods =====

    /**
     * Creates a new non-strict SINK catalog.
     *
     * @return a new Catalog instance
     */
    public @NotNull Catalog createCatalog()
    {
        return new CatalogImpl();
    }

    /**
     * Creates a new non-strict catalog with the specified species and upstream.
     *
     * @param species the catalog species
     * @param upstream the upstream catalog, or null for SOURCE/SINK catalogs
     * @return a new Catalog instance
     */
    public @NotNull Catalog createCatalog(@NotNull CatalogSpecies species, UUID upstream)
    {
        return new CatalogImpl(species, upstream);
    }

    /**
     * Creates a new catalog with full configuration.
     *
     * @param globalId the global identifier for the catalog
     * @param species the catalog species
     * @param upstream the upstream catalog, or null for SOURCE/SINK catalogs
     * @return a new Catalog instance
     */
    public @NotNull Catalog createCatalog(@NotNull UUID globalId, @NotNull CatalogSpecies species,
                                          UUID upstream)
    {
        return new CatalogImpl(globalId, species, upstream, 0L);
    }

    /**
     * Creates a new catalog with full configuration including version.
     *
     * @param globalId the global identifier for the catalog
     * @param species the catalog species
     * @param upstream the upstream catalog, or null for SOURCE/SINK catalogs
     * @param version the version number of this catalog
     * @return a new Catalog instance
     */
    public @NotNull Catalog createCatalog(@NotNull UUID globalId, @NotNull CatalogSpecies species,
                                          URI uri, UUID upstream, long version)
    {
        CatalogImpl cat = new CatalogImpl(globalId, species, upstream, version);
        cat.uri(uri);
        return cat;
    }

    /**
     * Creates a new catalog definition.
     *
     * @return a new CatalogDef instance
     */
    public @NotNull CatalogDef createCatalogDef()
    {
        return new CatalogDefImpl();
    }

    /**
     * Creates a new catalog definition by copying another.
     *
     * @param other the catalog definition to copy
     * @return a new CatalogDef instance
     */
    public @NotNull CatalogDef createCatalogDef(@NotNull CatalogDef other)
    {
        return new CatalogDefImpl(other);
    }

    /**
     * Creates a new catalog definition with specified hierarchies and aspect definitions.
     *
     * @param hierarchyDefs the hierarchy definitions to include
     * @param aspectDefs the aspect definitions to include
     * @return a new CatalogDef instance
     */
    public @NotNull CatalogDef createCatalogDef(Iterable<HierarchyDef> hierarchyDefs, 
                                                Iterable<AspectDef> aspectDefs)
    {
        return new CatalogDefImpl(hierarchyDefs, aspectDefs);
    }

    // ===== Entity Factory Methods =====

    /**
     * Creates a new entity with a random UUID.
     *
     * @return a new Entity instance
     */
    public @NotNull Entity createEntity()
    {
        return new EntityImpl();
    }

    /**
     * Creates a new entity with a random UUID.
     *
     * @return a new Entity instance
     */
    public @NotNull Entity createAndRegisterEntity()
    {
        Entity e = new EntityImpl();
        entities.put(e.globalId(),e);
        return e;
    }

    /**
     * Creates a new entity with the specified global ID.
     *
     * @param globalId the UUID for the entity
     * @return a new Entity instance
     */
    public @NotNull Entity createEntity(@NotNull UUID globalId)
    {
        return new EntityImpl(globalId);
    }

    /**
     * Find the registered entity with the specified global ID; if it's not
     * found, create and register a new one.
     *
     * @param globalId the UUID for the entity
     * @return a new Entity instance
     */
    public @NotNull Entity getOrRegisterNewEntity(@NotNull UUID globalId)
    {
        Entity e = entities.get(globalId);
        if (e != null) {
            return e;
        }
        e = new EntityImpl(globalId);
        entities.put(e.globalId(),e);
        return e;
    }

    /**
     * Creates a new entity with lazily-initialized UUID.
     *
     * @return a new Entity instance with lazy ID initialization
     */
    public @NotNull Entity createLazyEntity()
    {
        return new EntityLazyIdImpl();
    }

    /**
     * Creates a new local entity associated with a catalog using the configured LocalEntityType.
     *
     * @param catalog the catalog this entity belongs to
     * @return a new LocalEntity instance of the configured type
     */
    public @NotNull LocalEntity createLocalEntity(@NotNull Catalog catalog)
    {
        return createLocalEntity(defaultLocalEntityType, catalog);
    }

    /**
     * Creates a new local entity associated with a catalog with optional UUID using the configured LocalEntityType.
     *
     * @param globalId the UUID for the entity, or null for random UUID
     * @param catalog the catalog this entity belongs to
     * @return a new LocalEntity instance of the configured type
     */
    public @NotNull LocalEntity createLocalEntity(UUID globalId, @NotNull Catalog catalog)
    {
        return createLocalEntity(defaultLocalEntityType, globalId, catalog);
    }

    /**
     * Creates a new local entity of the specified type associated with a catalog.
     *
     * @param type the type of LocalEntity to create
     * @param catalog the catalog this entity belongs to
     * @return a new LocalEntity instance
     */
    public @NotNull LocalEntity createLocalEntity(@NotNull LocalEntityType type, @NotNull Catalog catalog)
    {
        return createLocalEntity(type, null, catalog);
    }

    /**
     * Creates a new local entity of the specified type associated with a catalog with optional UUID.
     *
     * @param type the type of LocalEntity to create
     * @param globalId the UUID for the entity, or null for random UUID
     * @param catalog the catalog this entity belongs to
     * @return a new LocalEntity instance
     */
    public @NotNull LocalEntity createLocalEntity(@NotNull LocalEntityType type, UUID globalId, @NotNull Catalog catalog)
    {
        return switch (type) {
            case SINGLE_CATALOG -> globalId != null 
                ? new LocalEntityOneCatalogImpl(globalId, catalog)
                : new LocalEntityOneCatalogImpl(catalog);
            case MULTI_CATALOG -> globalId != null
                ? new LocalEntityMultiCatalogImpl(globalId, catalog)
                : new LocalEntityMultiCatalogImpl(catalog);
            case CACHING_SINGLE_CATALOG -> globalId != null
                ? new CachingEntityOneCatalogImpl(globalId, catalog)
                : new CachingEntityOneCatalogImpl(catalog);
            case CACHING_MULTI_CATALOG -> globalId != null
                ? new CachingEntityMultiCatalogImpl(globalId, catalog)
                : new CachingEntityMultiCatalogImpl(catalog);
        };
    }

    /**
     * Creates a new single-catalog local entity with specified global ID.
     *
     * @param globalId the UUID for the entity
     * @param catalog the catalog this entity belongs to
     * @return a new LocalEntity instance (single-catalog type)
     */
    public @NotNull LocalEntity createSingleCatalogEntity(@NotNull UUID globalId, @NotNull Catalog catalog)
    {
        return new LocalEntityOneCatalogImpl(globalId, catalog);
    }

    /**
     * Creates a new single-catalog local entity with random global ID.
     *
     * @param catalog the catalog this entity belongs to
     * @return a new LocalEntity instance (single-catalog type)
     */
    public @NotNull LocalEntity createSingleCatalogEntity(@NotNull Catalog catalog)
    {
        return new LocalEntityOneCatalogImpl(catalog);
    }

    /**
     * Creates a new local entity that can belong to multiple catalogs.
     *
     * @param catalog the initial catalog this entity belongs to
     * @return a new LocalEntity instance
     */
    public @NotNull LocalEntity createMultiCatalogEntity(@NotNull Catalog catalog)
    {
        return new LocalEntityMultiCatalogImpl(catalog);
    }

    /**
     * Creates a new local entity that can belong to multiple catalogs with specified global ID.
     *
     * @param globalId the UUID for the entity
     * @param catalog the initial catalog this entity belongs to
     * @return a new LocalEntity instance
     */
    public @NotNull LocalEntity createMultiCatalogEntity(@NotNull UUID globalId, @NotNull Catalog catalog)
    {
        return new LocalEntityMultiCatalogImpl(globalId, catalog);
    }

    /**
     * Creates a new caching local entity associated with a catalog.
     *
     * @param catalog the catalog this entity belongs to
     * @return a new LocalEntity instance with caching
     */
    public @NotNull LocalEntity createCachingEntity(@NotNull Catalog catalog)
    {
        return new CachingEntityOneCatalogImpl(catalog);
    }

    /**
     * Creates a new caching local entity associated with a catalog with specified global ID.
     *
     * @param globalId the UUID for the entity
     * @param catalog the catalog this entity belongs to
     * @return a new LocalEntity instance with caching
     */
    public @NotNull LocalEntity createCachingEntity(@NotNull UUID globalId, @NotNull Catalog catalog)
    {
        return new CachingEntityOneCatalogImpl(globalId, catalog);
    }

    /**
     * Creates a new caching local entity that can belong to multiple catalogs.
     *
     * @param catalog the initial catalog this entity belongs to
     * @return a new LocalEntity instance with caching
     */
    public @NotNull LocalEntity createCachingMultiCatalogEntity(@NotNull Catalog catalog)
    {
        return new CachingEntityMultiCatalogImpl(catalog);
    }

    /**
     * Creates a new caching local entity that can belong to multiple catalogs with specified global ID.
     *
     * @param globalId the UUID for the entity
     * @param catalog the initial catalog this entity belongs to
     * @return a new LocalEntity instance with caching
     */
    public @NotNull LocalEntity createCachingMultiCatalogEntity(@NotNull UUID globalId, @NotNull Catalog catalog)
    {
        return new CachingEntityMultiCatalogImpl(globalId, catalog);
    }

    // ===== Hierarchy Factory Methods =====

    /**
     * Creates a new modifiable hierarchy definition.
     *
     * @param name the name of the hierarchy
     * @param type the type of hierarchy
     * @return a new HierarchyDef instance
     */
    public @NotNull HierarchyDef createHierarchyDef(@NotNull String name, @NotNull HierarchyType type)
    {
        return new HierarchyDefImpl(name, type);
    }

    /**
     * Creates a new entity directory hierarchy.
     *
     * @param name the name of this hierarchy in the catalog
     * @return a new EntityDirectoryHierarchy instance
     */
    public @NotNull EntityDirectoryHierarchy createEntityDirectoryHierarchy(@NotNull String name)
    {
        return createEntityDirectoryHierarchy(catalog, name);
    }

    /**
     * Creates a new entity directory hierarchy.
     *
     * @param catalog the owning catalog
     * @param name the name of this hierarchy in the catalog
     * @return a new EntityDirectoryHierarchy instance
     */
    public @NotNull EntityDirectoryHierarchy createEntityDirectoryHierarchy(@NotNull Catalog catalog, @NotNull String name)
    {
        return new EntityDirectoryHierarchyImpl(catalog, name);
    }

    /**
     * Creates a new entity directory hierarchy with version.
     *
     * @param catalog the owning catalog
     * @param name the name of this hierarchy in the catalog
     * @param version the version number of this hierarchy
     * @return a new EntityDirectoryHierarchy instance
     */
    public @NotNull EntityDirectoryHierarchy createEntityDirectoryHierarchy(@NotNull Catalog catalog, @NotNull String name, long version)
    {
        return new EntityDirectoryHierarchyImpl(catalog, name, version);
    }

    /**
     * Creates a new entity list hierarchy.
     *
     * @param name the name of this hierarchy in the catalog
     * @return a new EntityListHierarchy instance
     */
    public @NotNull EntityListHierarchy createEntityListHierarchy(@NotNull String name)
    {
        return new EntityListHierarchyImpl(catalog, name);
    }

    /**
     * Creates a new entity list hierarchy.
     *
     * @param name the name of this hierarchy in the catalog
     * @return a new EntityListHierarchy instance
     */
    public @NotNull EntityListHierarchy createEntityListHierarchy(@NotNull Catalog catalog, @NotNull String name)
    {
        return new EntityListHierarchyImpl(catalog, name);
    }

    /**
     * Creates a new entity list hierarchy with version.
     *
     * @param catalog the owning catalog
     * @param name the name of this hierarchy in the catalog
     * @param version the version number of this hierarchy
     * @return a new EntityListHierarchy instance
     */
    public @NotNull EntityListHierarchy createEntityListHierarchy(@NotNull Catalog catalog, @NotNull String name, long version)
    {
        return new EntityListHierarchyImpl(catalog, name, version);
    }

    /**
     * Creates a new entity list hierarchy with initial capacity and version.
     *
     * @param catalog the owning catalog
     * @param name the name of this hierarchy in the catalog
     * @param initialCapacity the initial capacity for the list
     * @param version the version number of this hierarchy
     * @return a new EntityListHierarchy instance
     */
    public @NotNull EntityListHierarchy createEntityListHierarchy(@NotNull Catalog catalog, @NotNull String name, int initialCapacity, long version)
    {
        return new EntityListHierarchyImpl(catalog, name, initialCapacity, version);
    }

    /**
     * Creates a new entity set hierarchy.
     *
     * @param name the name of this hierarchy in the catalog
     * @return a new EntitySetHierarchy instance
     */
    public @NotNull EntitySetHierarchy createEntitySetHierarchy(@NotNull String name)
    {
        return new EntitySetHierarchyImpl(catalog, name);
    }

    /**
     * Creates a new entity set hierarchy.
     *
     * @param name the name of this hierarchy in the catalog
     * @return a new EntitySetHierarchy instance
     */
    public @NotNull EntitySetHierarchy createEntitySetHierarchy(@NotNull Catalog catalog, @NotNull String name)
    {
        return new EntitySetHierarchyImpl(catalog, name);
    }

    /**
     * Creates a new entity set hierarchy with version.
     *
     * @param catalog the owning catalog
     * @param name the name of this hierarchy in the catalog
     * @param version the version number of this hierarchy
     * @return a new EntitySetHierarchy instance
     */
    public @NotNull EntitySetHierarchy createEntitySetHierarchy(@NotNull Catalog catalog, @NotNull String name, long version)
    {
        return new EntitySetHierarchyImpl(catalog, name, version);
    }

    /**
     * Creates a new entity set hierarchy with initial capacity and version.
     *
     * @param catalog the owning catalog
     * @param name the name of this hierarchy in the catalog
     * @param initialCapacity the initial capacity for the set
     * @param version the version number of this hierarchy
     * @return a new EntitySetHierarchy instance
     */
    public @NotNull EntitySetHierarchy createEntitySetHierarchy(@NotNull Catalog catalog, @NotNull String name, int initialCapacity, long version)
    {
        return new EntitySetHierarchyImpl(catalog, name, initialCapacity, version);
    }

    /**
     * Creates a new entity tree hierarchy.
     *
     * @param name the name of this hierarchy in the catalog
     * @param rootEntity the entity to use as the root of the tree
     * @return a new EntityTreeHierarchy instance
     */
    public @NotNull EntityTreeHierarchy createEntityTreeHierarchy(@NotNull String name, @NotNull Entity rootEntity)
    {
        return new EntityTreeHierarchyImpl(catalog, name, rootEntity);
    }

    /**
     * Creates a new entity tree hierarchy.
     *
     * @param name the name of this hierarchy in the catalog
     * @param rootEntity the entity to use as the root of the tree
     * @return a new EntityTreeHierarchy instance
     */
    public @NotNull EntityTreeHierarchy createEntityTreeHierarchy(@NotNull Catalog catalog, @NotNull String name,
                                                                  @NotNull Entity rootEntity)
    {
        return new EntityTreeHierarchyImpl(catalog, name, rootEntity);
    }

    /**
     * Creates a new entity tree hierarchy.
     *
     * @param name the name of this hierarchy in the catalog
     * @param rootNode the node to use as the root of the tree
     * @return a new EntityTreeHierarchy instance
     */
    public @NotNull EntityTreeHierarchy createEntityTreeHierarchy(@NotNull String name,
                                                                  @NotNull EntityTreeHierarchy.Node rootNode)
    {
        return new EntityTreeHierarchyImpl(catalog, name, rootNode);
    }

    /**
     * Creates a new entity tree hierarchy.
     *
     * @param name the name of this hierarchy in the catalog
     * @param rootNode the node to use as the root of the tree
     * @return a new EntityTreeHierarchy instance
     */
    public @NotNull EntityTreeHierarchy createEntityTreeHierarchy(@NotNull Catalog catalog, @NotNull String name,
                                                                  @NotNull EntityTreeHierarchy.Node rootNode)
    {
        return new EntityTreeHierarchyImpl(catalog, name, rootNode);
    }

    /**
     * Creates a new entity tree hierarchy with version.
     *
     * @param catalog the owning catalog
     * @param name the name of this hierarchy in the catalog
     * @param rootNode the node to use as the root of the tree
     * @param version the version number of this hierarchy
     * @return a new EntityTreeHierarchy instance
     */
    public @NotNull EntityTreeHierarchy createEntityTreeHierarchy(@NotNull Catalog catalog, @NotNull String name,
                                                                  @NotNull EntityTreeHierarchy.Node rootNode, long version)
    {
        return new EntityTreeHierarchyImpl(catalog, name, rootNode, version);
    }

    /**
     * Creates a new aspect map hierarchy.
     *
     * @param aspectDef the aspect definition for aspects in this hierarchy
     * @return a new AspectMapHierarchy instance
     */
    public @NotNull AspectMapHierarchy createAspectMapHierarchy(@NotNull AspectDef aspectDef)
    {
        return new AspectMapHierarchyImpl(catalog, aspectDef);
    }

    /**
     * Creates a new aspect map hierarchy.
     *
     * @param aspectDef the aspect definition for aspects in this hierarchy
     * @return a new AspectMapHierarchy instance
     */
    public @NotNull AspectMapHierarchy createAspectMapHierarchy(@NotNull Catalog catalog, @NotNull AspectDef aspectDef)
    {
        return new AspectMapHierarchyImpl(catalog, aspectDef);
    }

    /**
     * Creates a new aspect map hierarchy with custom hierarchy definition.
     *
     * @param name the name of this hierarchy in the catalog
     * @param aspectDef the aspect definition for aspects in this hierarchy
     * @return a new AspectMapHierarchy instance
     */
    public @NotNull AspectMapHierarchy createAspectMapHierarchy(@NotNull String name, @NotNull AspectDef aspectDef)
    {
        return new AspectMapHierarchyImpl(catalog, aspectDef);
    }

    /**
     * Creates a new aspect map hierarchy with version.
     *
     * @param catalog the owning catalog
     * @param aspectDef the aspect definition for aspects in this hierarchy
     * @param version the version number of this hierarchy
     * @return a new AspectMapHierarchy instance
     */
    public @NotNull AspectMapHierarchy createAspectMapHierarchy(@NotNull Catalog catalog, @NotNull AspectDef aspectDef, long version)
    {
        return new AspectMapHierarchyImpl(catalog, aspectDef, version);
    }

    // ===== Aspect Definition Factory Methods =====

    /**
     * Creates a new mutable aspect definition.
     *
     * @param name the name of this aspect definition
     * @return a new mutable AspectDef instance
     */
    public @NotNull MutableAspectDef createMutableAspectDef(@NotNull String name)
    {
        return new MutableAspectDefImpl(name);
    }

    /**
     * Creates a new mutable aspect definition with property definitions.
     *
     * @param name the name of this aspect definition
     * @param propertyDefs the map of property names to property definitions
     * @return a new mutable AspectDef instance
     */
    public @NotNull MutableAspectDef createMutableAspectDef(@NotNull String name,
                                                           @NotNull Map<String, PropertyDef> propertyDefs)
    {
        return new MutableAspectDefImpl(name, propertyDefs);
    }

    /**
     * Creates a new immutable aspect definition.
     *
     * @param name the name of this aspect definition
     * @param propertyDefs the map of property names to property definitions
     * @return a new immutable AspectDef instance
     */
    public @NotNull AspectDef createImmutableAspectDef(@NotNull String name,
                                                       @NotNull Map<String, ? extends PropertyDef> propertyDefs)
    {
        return new ImmutableAspectDefImpl(name, propertyDefs);
    }

    /**
     * Creates a new full aspect definition with explicit control over all boolean flags.
     *
     * @param name the name of this aspect definition
     * @param globalId the global ID for this aspect definition
     * @param propertyDefs the map of property names to property definitions
     * @param isReadable whether this aspect definition is readable
     * @param isWritable whether this aspect definition is writable
     * @param canAddProperties whether properties can be added
     * @param canRemoveProperties whether properties can be removed
     * @return a new full AspectDef instance
     */
    public @NotNull MutableAspectDef createFullAspectDef(@NotNull String name,
                                                          @NotNull UUID globalId,
                                                          @NotNull Map<String, PropertyDef> propertyDefs,
                                                          boolean isReadable,
                                                          boolean isWritable,
                                                          boolean canAddProperties,
                                                          boolean canRemoveProperties)
    {
        return new FullAspectDefImpl(name, globalId, propertyDefs, isReadable, isWritable,
            canAddProperties, canRemoveProperties);
    }

    // ===== Property Factory Methods =====

    /**
     * Creates a new property definition.
     *
     * @param name the name of the property
     * @param type the data type of the property
     * @param defaultValue the default value for the property
     * @param hasDefaultValue whether the property has a default value
     * @param isReadable whether the property can be read
     * @param isWritable whether the property can be written
     * @param isNullable whether the property accepts null values
     * @param isRemovable whether the property can be removed
     * @param isMultivalued whether the property can hold multiple values
     * @return a new PropertyDef instance
     */
    public @NotNull PropertyDef createPropertyDef(@NotNull String name, @NotNull PropertyType type,
                                                        Object defaultValue, boolean hasDefaultValue,
                                                        boolean isReadable, boolean isWritable,
                                                        boolean isNullable, boolean isRemovable,
                                                        boolean isMultivalued)
    {
        return new PropertyDefImpl(name, type, defaultValue, hasDefaultValue, isReadable, isWritable, 
                                  isNullable, isRemovable, isMultivalued);
    }

    /**
     * Creates a new property definition with default settings.
     *
     * @param name the name of the property
     * @param type the data type of the property
     * @return a new PropertyDef instance
     */
    public @NotNull PropertyDef createPropertyDef(@NotNull String name, @NotNull PropertyType type)
    {
        return new PropertyDefImpl(name, type);
    }

    /**
     * Creates a new property definition with specified accessibility.
     *
     * @param name the name of the property
     * @param type the data type of the property
     * @param isReadable whether the property can be read
     * @param isWritable whether the property can be written
     * @param isNullable whether the property accepts null values
     * @param isRemovable whether the property can be removed
     * @param isMultivalued whether the property can hold multiple values
     * @return a new PropertyDef instance
     */
    public @NotNull PropertyDef createPropertyDef(@NotNull String name, @NotNull PropertyType type,
                                                        boolean isReadable, boolean isWritable,
                                                        boolean isNullable, boolean isRemovable,
                                                        boolean isMultivalued)
    {
        return new PropertyDefImpl(name, type, isReadable, isWritable, isNullable, isRemovable, isMultivalued);
    }

    /**
     * Creates a new read-only property definition.
     *
     * @param name the name of the property
     * @param type the data type of the property
     * @param isNullable whether the property accepts null values
     * @param isRemovable whether the property can be removed
     * @return a new read-only PropertyDef instance
     */
    public @NotNull PropertyDef createReadOnlyPropertyDef(@NotNull String name, @NotNull PropertyType type,
                                                                boolean isNullable, boolean isRemovable)
    {
        return PropertyDefImpl.readOnly(name, type, isNullable, isRemovable);
    }

    /**
     * Creates a new property with the specified value.
     * The value will be coerced to the type specified in the PropertyDef if necessary.
     *
     * @param def the property definition for this property
     * @param value the value to store in this property
     * @return a new Property instance
     * @throws IllegalArgumentException if the value cannot be coerced to the required type
     */
    public @NotNull Property createProperty(@NotNull PropertyDef def, Object value)
    {
        Object coercedValue = propertyAdapter.coerce(def, value);
        return new PropertyImpl(def, coercedValue);
    }


    // ===== EntityTreeHierarchy Node Factory Methods =====

    /**
     * Creates a new non-leaf EntityTreeHierarchy.Node that can have children.
     *
     * @param value the entity value to store at this node
     * @return a new non-leaf Node instance
     */
    public @NotNull EntityTreeHierarchy.Node createTreeNode(Entity value)
    {
        return new EntityTreeHierarchyImpl.NodeImpl(value);
    }

    /**
     * Creates a new non-leaf EntityTreeHierarchy.Node with the specified parent.
     *
     * @param value the entity value to store at this node
     * @param parent the parent node
     * @return a new non-leaf Node instance
     */
    public @NotNull EntityTreeHierarchy.Node createTreeNode(Entity value, EntityTreeHierarchy.Node parent)
    {
        return new EntityTreeHierarchyImpl.NodeImpl(value, parent);
    }

    /**
     * Creates a new leaf EntityTreeHierarchy.Node that cannot have children.
     *
     * @param value the entity value to store at this leaf node
     * @return a new leaf Node instance
     */
    public @NotNull EntityTreeHierarchy.Node createTreeLeafNode(Entity value)
    {
        return new EntityTreeHierarchyImpl.LeafNodeImpl(value);
    }

    /**
     * Creates a new leaf EntityTreeHierarchy.Node with the specified parent.
     *
     * @param value the entity value to store at this leaf node
     * @param parent the parent node
     * @return a new leaf Node instance
     */
    public @NotNull EntityTreeHierarchy.Node createTreeLeafNode(Entity value, EntityTreeHierarchy.Node parent)
    {
        return new EntityTreeHierarchyImpl.LeafNodeImpl(value, parent);
    }

    // ===== Aspect Factory Methods =====

    /**
     * Creates a new aspect with object-based property storage.
     *
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @return a new Aspect instance
     */
    public @NotNull Aspect createObjectMapAspect(Entity entity, @NotNull AspectDef def)
    {
        return new AspectObjectMapImpl(entity, def);
    }

    /**
     * Creates a new aspect with object-based property storage and performance tuning.
     *
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @param initialCapacity the initial capacity for the internal map
     * @return a new Aspect instance
     */
    public @NotNull Aspect createObjectMapAspect(Entity entity, @NotNull AspectDef def, int initialCapacity)
    {
        return new AspectObjectMapImpl(entity, def, initialCapacity);
    }

    /**
     * Creates a new aspect with object-based property storage and full performance tuning.
     *
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @param initialCapacity the initial capacity for the internal map
     * @param loadFactor the load factor for the internal map
     * @return a new Aspect instance
     */
    public @NotNull Aspect createObjectMapAspect(Entity entity, @NotNull AspectDef def, 
                                                       int initialCapacity, float loadFactor)
    {
        return new AspectObjectMapImpl(entity, def, initialCapacity, loadFactor);
    }

    /**
     * Creates a new aspect with Property-based storage.
     *
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @return a new Aspect instance
     */
    public @NotNull Aspect createPropertyMapAspect(Entity entity, @NotNull AspectDef def)
    {
        return new AspectPropertyMapImpl(entity, def);
    }

    /**
     * Creates a new aspect with Property-based storage and performance tuning.
     *
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @param initialCapacity the initial capacity for the internal map
     * @return a new Aspect instance
     */
    public @NotNull Aspect createPropertyMapAspect(Entity entity, @NotNull AspectDef def, int initialCapacity)
    {
        return new AspectPropertyMapImpl(entity, def, initialCapacity);
    }

    /**
     * Creates a new aspect with Property-based storage and full performance tuning.
     *
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @param initialCapacity the initial capacity for the internal map
     * @param loadFactor the load factor for the internal map
     * @return a new Aspect instance
     */
    public @NotNull Aspect createPropertyMapAspect(Entity entity, @NotNull AspectDef def, 
                                                         int initialCapacity, float loadFactor)
    {
        return new AspectPropertyMapImpl(entity, def, initialCapacity, loadFactor);
    }
}