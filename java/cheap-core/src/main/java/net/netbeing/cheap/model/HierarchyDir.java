package net.netbeing.cheap.model;

import java.util.Map;

/**
 * The interface Hierarchy dir.
 */
public interface HierarchyDir extends Hierarchy, Map<String, Hierarchy>
{
    default Hierarchy add(Hierarchy hierarchy) {
        return put(hierarchy.def().name(), hierarchy);
    }
}
