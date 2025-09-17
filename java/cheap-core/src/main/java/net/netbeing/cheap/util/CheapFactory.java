package net.netbeing.cheap.util;

import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Factory class providing static factory methods for creating instances of all
 * concrete implementation classes in the net.netbeing.cheap.impl.basic package.
 * <p>
 * All factory methods return interface types from net.netbeing.cheap.model
 * rather than concrete implementation types, promoting loose coupling and
 * implementation hiding.
 * <p>
 * This factory simplifies object creation and provides a clean API for
 * instantiating CHEAP model objects without directly depending on implementation classes.
 */
public final class CheapFactory
{
    private CheapFactory()
    {
        // Utility class - prevent instantiation
    }

    // ===== Catalog Factory Methods =====

    /**
     * Creates a new non-strict SINK catalog.
     *
     * @return a new Catalog instance
     */
    public static @NotNull Catalog createCatalog()
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
    public static @NotNull Catalog createCatalog(@NotNull CatalogSpecies species, Catalog upstream)
    {
        return new CatalogImpl(species, upstream);
    }

    /**
     * Creates a new catalog with full configuration.
     *
     * @param globalId the global identifier for the catalog
     * @param species the catalog species
     * @param def the catalog definition, or null to auto-create
     * @param upstream the upstream catalog, or null for SOURCE/SINK catalogs
     * @param strict whether the catalog is strict
     * @return a new Catalog instance
     */
    public static @NotNull Catalog createCatalog(@NotNull UUID globalId, @NotNull CatalogSpecies species, 
                                                CatalogDef def, Catalog upstream, boolean strict)
    {
        return new CatalogImpl(globalId, species, def, upstream, strict);
    }

    /**
     * Creates a new catalog definition.
     *
     * @return a new CatalogDef instance
     */
    public static @NotNull CatalogDef createCatalogDef()
    {
        return new CatalogDefImpl();
    }

    /**
     * Creates a new catalog definition by copying another.
     *
     * @param other the catalog definition to copy
     * @return a new CatalogDef instance
     */
    public static @NotNull CatalogDef createCatalogDef(@NotNull CatalogDef other)
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
    public static @NotNull CatalogDef createCatalogDef(Iterable<HierarchyDef> hierarchyDefs, 
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
    public static @NotNull Entity createEntity()
    {
        return new EntityImpl();
    }

    /**
     * Creates a new entity with the specified global ID.
     *
     * @param globalId the UUID for the entity
     * @return a new Entity instance
     */
    public static @NotNull Entity createEntity(@NotNull UUID globalId)
    {
        return new EntityImpl(globalId);
    }

    /**
     * Creates a new entity with lazily-initialized UUID.
     *
     * @return a new Entity instance with lazy ID initialization
     */
    public static @NotNull Entity createLazyEntity()
    {
        return new EntityLazyIdImpl();
    }

    /**
     * Creates a new local entity associated with a catalog.
     *
     * @param catalog the catalog this entity belongs to
     * @return a new LocalEntity instance
     */
    public static @NotNull LocalEntity createLocalEntity(Catalog catalog)
    {
        return new LocalEntityOneCatalogImpl(catalog);
    }

    /**
     * Creates a new local entity with specified global ID and catalog.
     *
     * @param globalId the UUID for the entity
     * @param catalog the catalog this entity belongs to
     * @return a new LocalEntity instance
     */
    public static @NotNull LocalEntity createLocalEntity(@NotNull UUID globalId, Catalog catalog)
    {
        return new LocalEntityOneCatalogImpl(globalId, catalog);
    }

    /**
     * Creates a new local entity that can belong to multiple catalogs.
     *
     * @param catalog the initial catalog this entity belongs to
     * @return a new LocalEntity instance
     */
    public static @NotNull LocalEntity createMultiCatalogEntity(@NotNull Catalog catalog)
    {
        return new LocalEntityMultiCatalogImpl(catalog);
    }

    /**
     * Creates a new caching local entity associated with a catalog.
     *
     * @param catalog the catalog this entity belongs to
     * @return a new LocalEntity instance with caching
     */
    public static @NotNull LocalEntity createCachingEntity(@NotNull Catalog catalog)
    {
        return new CachingEntityOneCatalogImpl(catalog);
    }

    /**
     * Creates a new caching local entity that can belong to multiple catalogs.
     *
     * @param catalog the initial catalog this entity belongs to
     * @return a new LocalEntity instance with caching
     */
    public static @NotNull LocalEntity createCachingMultiCatalogEntity(@NotNull Catalog catalog)
    {
        return new CachingEntityMultiCatalogImpl(catalog);
    }

    // ===== Hierarchy Factory Methods =====

    /**
     * Creates a new hierarchy definition.
     *
     * @param name the name of the hierarchy
     * @param type the type of hierarchy
     * @param isModifiable whether the hierarchy contents can be modified
     * @return a new HierarchyDef instance
     */
    public static @NotNull HierarchyDef createHierarchyDef(@NotNull String name, @NotNull HierarchyType type, 
                                                          boolean isModifiable)
    {
        return new HierarchyDefImpl(name, type, isModifiable);
    }

    /**
     * Creates a new modifiable hierarchy definition.
     *
     * @param name the name of the hierarchy
     * @param type the type of hierarchy
     * @return a new HierarchyDef instance
     */
    public static @NotNull HierarchyDef createHierarchyDef(@NotNull String name, @NotNull HierarchyType type)
    {
        return new HierarchyDefImpl(name, type);
    }

    /**
     * Creates a new hierarchy directory.
     *
     * @param def the hierarchy definition for this directory
     * @return a new HierarchyDir instance
     */
    public static @NotNull HierarchyDir createHierarchyDir(@NotNull HierarchyDef def)
    {
        return new HierarchyDirImpl(def);
    }

    /**
     * Creates a new hierarchy directory with performance tuning.
     *
     * @param def the hierarchy definition for this directory
     * @param initialCapacity the initial capacity for the internal map
     * @param loadFactor the load factor for the internal map
     * @return a new HierarchyDir instance
     */
    public static @NotNull HierarchyDir createHierarchyDir(@NotNull HierarchyDef def, int initialCapacity, 
                                                          float loadFactor)
    {
        return new HierarchyDirImpl(def, initialCapacity, loadFactor);
    }

    /**
     * Creates a new entity directory hierarchy.
     *
     * @param def the hierarchy definition for this entity directory
     * @return a new EntityDirectoryHierarchy instance
     */
    public static @NotNull EntityDirectoryHierarchy createEntityDirectoryHierarchy(@NotNull HierarchyDef def)
    {
        return new EntityDirectoryHierarchyImpl(def);
    }

    /**
     * Creates a new entity list hierarchy.
     *
     * @param def the hierarchy definition for this entity list
     * @return a new EntityListHierarchy instance
     */
    public static @NotNull EntityListHierarchy createEntityListHierarchy(@NotNull HierarchyDef def)
    {
        return new EntityListHierarchyImpl(def);
    }

    /**
     * Creates a new entity set hierarchy.
     *
     * @param def the hierarchy definition for this entity set
     * @return a new EntitySetHierarchy instance
     */
    public static @NotNull EntitySetHierarchy createEntitySetHierarchy(@NotNull HierarchyDef def)
    {
        return new EntitySetHierarchyImpl(def);
    }

    /**
     * Creates a new entity tree hierarchy.
     *
     * @param def the hierarchy definition for this entity tree
     * @param rootEntity the entity to use as the root of the tree
     * @return a new EntityTreeHierarchy instance
     */
    public static @NotNull EntityTreeHierarchy createEntityTreeHierarchy(@NotNull HierarchyDef def, 
                                                                         @NotNull Entity rootEntity)
    {
        return new EntityTreeHierarchyImpl(def, rootEntity);
    }

    /**
     * Creates a new aspect map hierarchy.
     *
     * @param aspectDef the aspect definition for aspects in this hierarchy
     * @return a new AspectMapHierarchy instance
     */
    public static @NotNull AspectMapHierarchy createAspectMapHierarchy(@NotNull AspectDef aspectDef)
    {
        return new AspectMapHierarchyImpl(aspectDef);
    }

    /**
     * Creates a new aspect map hierarchy with custom hierarchy definition.
     *
     * @param def the hierarchy definition for this hierarchy
     * @param aspectDef the aspect definition for aspects in this hierarchy
     * @return a new AspectMapHierarchy instance
     */
    public static @NotNull AspectMapHierarchy createAspectMapHierarchy(@NotNull HierarchyDef def, 
                                                                      @NotNull AspectDef aspectDef)
    {
        return new AspectMapHierarchyImpl(def, aspectDef);
    }

    // ===== Aspect Definition Factory Methods =====

    /**
     * Creates a new mutable aspect definition.
     *
     * @param name the name of this aspect definition
     * @return a new mutable AspectDef instance
     */
    public static @NotNull AspectDef createMutableAspectDef(@NotNull String name)
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
    public static @NotNull AspectDef createMutableAspectDef(@NotNull String name, 
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
    public static @NotNull AspectDef createImmutableAspectDef(@NotNull String name, 
                                                             @NotNull Map<String, ? extends PropertyDef> propertyDefs)
    {
        return new ImmutableAspectDefImpl(name, propertyDefs);
    }

    /**
     * Creates a new aspect definition directory.
     *
     * @return a new AspectDefDir instance
     */
    public static @NotNull AspectDefDir createAspectDefDir()
    {
        return new AspectDefDirImpl();
    }

    /**
     * Creates a new aspect definition directory hierarchy.
     *
     * @param def the hierarchy definition for this hierarchy
     * @return a new AspectDefDirHierarchy instance
     */
    public static @NotNull AspectDefDirHierarchy createAspectDefDirHierarchy(@NotNull HierarchyDef def)
    {
        return new AspectDefDirHierarchyImpl(def);
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
    public static @NotNull PropertyDef createPropertyDef(@NotNull String name, @NotNull PropertyType type,
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
    public static @NotNull PropertyDef createPropertyDef(@NotNull String name, @NotNull PropertyType type)
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
    public static @NotNull PropertyDef createPropertyDef(@NotNull String name, @NotNull PropertyType type,
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
    public static @NotNull PropertyDef createReadOnlyPropertyDef(@NotNull String name, @NotNull PropertyType type,
                                                                boolean isNullable, boolean isRemovable)
    {
        return PropertyDefImpl.readOnly(name, type, isNullable, isRemovable);
    }

    /**
     * Creates a new property with the specified value.
     *
     * @param def the property definition for this property
     * @param value the value to store in this property
     * @return a new Property instance
     */
    public static @NotNull Property createProperty(@NotNull PropertyDef def, Object value)
    {
        return new PropertyImpl(def, value);
    }


    // ===== Aspect Factory Methods =====

    /**
     * Creates a new aspect with object-based property storage.
     *
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @return a new Aspect instance
     */
    public static @NotNull Aspect createObjectMapAspect(Entity entity, @NotNull AspectDef def)
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
    public static @NotNull Aspect createObjectMapAspect(Entity entity, @NotNull AspectDef def, int initialCapacity)
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
    public static @NotNull Aspect createObjectMapAspect(Entity entity, @NotNull AspectDef def, 
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
    public static @NotNull Aspect createPropertyMapAspect(Entity entity, @NotNull AspectDef def)
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
    public static @NotNull Aspect createPropertyMapAspect(Entity entity, @NotNull AspectDef def, int initialCapacity)
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
    public static @NotNull Aspect createPropertyMapAspect(Entity entity, @NotNull AspectDef def, 
                                                         int initialCapacity, float loadFactor)
    {
        return new AspectPropertyMapImpl(entity, def, initialCapacity, loadFactor);
    }
}