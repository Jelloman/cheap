package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents an entity in the CHEAP data model. An Entity is only a conceptual object.
 * It does not have any specific properties except for a global ID; all other properties
 * are stored in Aspects, which in turn are stored in Catalogs. Entities are not "stored"
 * anywhere, since they have no data. The global ID serves as a key to locate Aspects.
 * Entities are analogous to primary keys in database terminology.
 * 
 * <p>Each Entity has a globally unique identifier (UUID) and can have an arbitrary set
 * of Aspects attached to it - but no more than one of each Aspect type, as defined by an
 * AspectDef.</p>
 * 
 * <p>Entities are referenced by their Aspects, and also by some types of Hierarchies.</p>
 */
public interface Entity
{
    /**
     * Returns the globally unique identifier for this entity. This UUID is used to
     * reference the entity across different catalogs and hierarchies.
     * 
     * <p>The global ID is immutable once assigned. Some Entity implementations
     * will defer ID generation until requested.</p>
     *
     * @return the globally unique UUID for this entity, never null
     */
    @NotNull UUID globalId();

    /**
     * Retrieves an Aspect attached to this entity in the specified Catalog
     * using its AspectDef.
     *
     * <p>The default implementation requests the AspectMapHierarchy from the
     * given Catalog for the given AspectDef and simply returns the result of
     * its {@link AspectMapHierarchy#get(AspectDef) get} method. If no aspect
     * of the specified type is attached to this entity in the catalog, this
     * method returns null.</p>
     *
     * @param def the aspect definition to look up, must not be null
     * @param cat the catalog to look in, must not be null
     * @return the aspect instance matching the definition, or null if not found
     */
    default Aspect getAspect(@NotNull AspectDef def, @NotNull Catalog cat)
    {
        AspectMapHierarchy aspects = cat.aspects(def);
        if (aspects != null) {
            return aspects.get(this);
        }
        return null;
    }
}
