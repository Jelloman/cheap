package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

/**
 * LocalEntity keeps track of one or more Catalog(s) that contain its Aspects.
 * LocalEntity references are therefore sufficient to access Aspects, without
 * needing a Catalog reference.
 */
public interface LocalEntity extends Entity
{
    /**
     * Return the set of Catalogs that this entity has Aspects in.
     *
     * @return an Iterable of Catalogs (which commonly will only have one element)
     */
    Iterable<Catalog> catalogs();

    /**
     * Retrieves a specific aspect attached to this entity by its definition.
     *
     * <p>If no aspect of the specified type is attached to this entity in any
     * of its catalogs, this method returns null.</p>
     *
     * <p>The default implementation calls the {@link #getAspect(AspectDef, Catalog) getAspect}
     * method with each of the catalogs returned by the {@link #catalogs() catalogs} method,
     * in order, and returns the first match.</p>
     *
     * @param def the aspect definition to look up, must not be null
     * @return the aspect instance matching the definition, or null if not found
     */
    default Aspect getAspect(@NotNull AspectDef def)
    {
        for (Catalog cat : catalogs()) {
            Aspect a = getAspect(def, cat);
            if (a != null) {
                return a;
            }
        }
        return null;
    }

}
