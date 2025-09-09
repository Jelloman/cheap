package net.netbeing.cheap.model;

import java.util.Map;

/**
 * The interface Local entity.
 */
public interface LocalEntity
{
    /**
     * Entity entity.
     *
     * @return the entity
     */
    Entity entity();

    /**
     * Aspects map.
     *
     * @return the map
     */
    Map<AspectDef,Aspect> aspects();

    /**
     * Aspect aspect.
     *
     * @param def the def
     * @return the aspect
     */
    default Aspect aspect(AspectDef def) {
        return aspects().get(def);
    }
}
