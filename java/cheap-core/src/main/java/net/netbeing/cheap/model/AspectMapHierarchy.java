package net.netbeing.cheap.model;

import java.util.Map;

/**
 * The interface Aspect map hierarchy.
 */
public interface AspectMapHierarchy extends Hierarchy, Map<Entity,Aspect>
{
    /**
     * Aspect def aspect def.
     *
     * @return the aspect def
     */
    AspectDef aspectDef();

    /**
     * Add aspect.
     *
     * @param a the a
     * @return the aspect
     */
    default Aspect add(Aspect a) {
        return put(a.entity(), a);
    }
}
