package net.netbeing.cheap.model;

import java.util.Map;

/**
 * Provides catalog-local access to an entity's aspects and properties. This interface
 * represents the catalog-specific view of an Entity, managing aspects that are
 * stored within a particular catalog context.
 * 
 * <p>LocalEntity handles the mapping between AspectDef definitions and the actual
 * Aspect instances attached to the entity. It serves as the bridge between the
 * global Entity identity and its catalog-local data storage.</p>
 * 
 * <p>Aspects attached to a LocalEntity can be retrieved, added, or modified through
 * this interface, providing the primary mechanism for data access in the CHEAP model.</p>
 */
public interface LocalEntity
{
    /**
     * Returns the global entity that this local entity represents. This provides
     * access to the global identity and cross-catalog operations.
     *
     * @return the global entity instance, never null
     */
    Entity entity();

    /**
     * Returns a mutable map of all aspects currently attached to this entity.
     * The map is keyed by AspectDef and contains the corresponding Aspect instances.
     * 
     * <p>This map can be used to add, remove, or iterate over all aspects
     * attached to the entity. Modifications to this map directly affect the
     * entity's aspect storage.</p>
     *
     * @return a mutable map of AspectDef to Aspect mappings, never null but may be empty
     */
    Map<AspectDef,Aspect> aspects();

    /**
     * Retrieves a specific aspect attached to this entity by its definition.
     * This is a convenience method equivalent to calling {@code aspects().get(def)}.
     * 
     * <p>If no aspect of the specified type is attached to this entity,
     * this method returns null.</p>
     *
     * @param def the aspect definition to look up, must not be null
     * @return the aspect instance matching the definition, or null if not found
     */
    default Aspect aspect(AspectDef def) {
        return aspects().get(def);
    }
}
