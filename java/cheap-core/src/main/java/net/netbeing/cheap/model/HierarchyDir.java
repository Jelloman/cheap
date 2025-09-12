package net.netbeing.cheap.model;

import java.util.Map;

/**
 * A directory hierarchy that maps string names to sub-hierarchies.
 * This interface provides a hierarchical organization structure where hierarchies
 * can be nested within other hierarchies, similar to a filesystem directory structure.
 * 
 * <p>HierarchyDir combines the standard hierarchy interface with Map functionality,
 * allowing efficient storage and retrieval of sub-hierarchies by their names.
 * This enables the creation of complex, nested data structures within the CHEAP model.</p>
 */
public interface HierarchyDir extends Hierarchy, Map<String, Hierarchy>
{
    /**
     * Convenience method to add a hierarchy to this directory.
     * The hierarchy will be stored under the name specified by its definition.
     * This method automatically extracts the name from the hierarchy's definition
     * and uses it as the key in the underlying map.
     * 
     * @param hierarchy the hierarchy to add to this directory
     * @return the previous hierarchy associated with the same name, or null if none existed
     */
    default Hierarchy add(Hierarchy hierarchy) {
        return put(hierarchy.def().name(), hierarchy);
    }
}
