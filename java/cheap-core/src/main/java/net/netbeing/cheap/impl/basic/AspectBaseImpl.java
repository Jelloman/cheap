package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for Aspect implementations providing common functionality.
 * This class manages the basic relationships between an aspect, its entity, catalog,
 * and aspect definition.
 * <p>
 * Subclasses must implement the specific property storage and access mechanisms
 * while this base class handles the fundamental aspect metadata.
 * 
 * @see Aspect
 * @see AspectDef
 * @see Entity
 * @see Catalog
 */
public abstract class AspectBaseImpl implements Aspect
{
    /** The catalog this aspect belongs to. */
    protected Catalog catalog;
    
    /** The entity this aspect is attached to. */
    protected Entity entity;
    
    /** The aspect definition describing this aspect's structure. */
    protected AspectDef def;

    /**
     * Creates a new AspectBaseImpl with the specified catalog, entity, and aspect definition.
     * 
     * @param catalog the catalog this aspect belongs to
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     */
    public AspectBaseImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def)
    {
        this.catalog = catalog;
        this.entity = entity;
        this.def = def;
    }

    /**
     * Creates a new AspectBaseImpl with the specified catalog, entity, aspect definition,
     * and initial capacity hint for subclass implementations.
     * 
     * @param catalog the catalog this aspect belongs to
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @param initialCapacity initial capacity hint for subclass storage implementations
     */
    public AspectBaseImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def, int initialCapacity)
    {
        this.catalog = catalog;
        this.entity = entity;
        this.def = def;
    }

    /**
     * Creates a new AspectBaseImpl with the specified catalog, entity, aspect definition,
     * initial capacity, and load factor hints for subclass implementations.
     * 
     * @param catalog the catalog this aspect belongs to
     * @param entity the entity this aspect is attached to
     * @param def the aspect definition describing this aspect's structure
     * @param initialCapacity initial capacity hint for subclass storage implementations
     * @param loadFactor load factor hint for subclass storage implementations
     */
    public AspectBaseImpl(@NotNull Catalog catalog, @NotNull Entity entity, AspectDef def, int initialCapacity, float loadFactor)
    {
        this.catalog = catalog;
        this.entity = entity;
        this.def = def;
    }

    /**
     * Returns the catalog this aspect belongs to.
     * 
     * @return the catalog containing this aspect
     */
    @Override
    public Catalog catalog()
    {
        return catalog;
    }

    /**
     * Returns the entity this aspect is attached to.
     * 
     * @return the entity owning this aspect
     */
    @Override
    public Entity entity()
    {
        return entity;
    }

    /**
     * Returns the aspect definition describing this aspect's structure.
     * 
     * @return the aspect definition for this aspect
     */
    @Override
    public AspectDef def()
    {
        return def;
    }
}
