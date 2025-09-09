package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents an entity in the CHEAP data model. An Entity is the core unit of data
 * storage and identification within a Catalog, serving as the "E" in the CHEAP acronym
 * (Catalog, Hierarchy, Entity, Aspect, Property).
 * 
 * <p>Each Entity has a globally unique identifier and can have multiple Aspects attached
 * to it. Entities are analogous to primary keys in database terminology or files in a
 * file system context.</p>
 * 
 * <p>Entities exist within Hierarchies and can be referenced across different Hierarchies
 * within the same Catalog or across different Catalogs through their global ID.</p>
 */
public interface Entity
{
    /**
     * Returns the globally unique identifier for this entity. This UUID is used to
     * reference the entity across different catalogs and hierarchies.
     * 
     * <p>The global ID is immutable once assigned and must be unique across all
     * entities in all catalogs within the CHEAP system.</p>
     *
     * @return the globally unique UUID for this entity, never null
     */
    @NotNull UUID globalId();

    /**
     * Returns the local entity interface which provides access to this entity's
     * aspects and catalog-local operations.
     * 
     * <p>The local entity provides methods to access and manipulate aspects
     * that are attached to this entity within the current catalog context.</p>
     *
     * @return the local entity interface for catalog-specific operations, never null
     */
    LocalEntity local();
}
