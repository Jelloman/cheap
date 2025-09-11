package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Hierarchy;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyDir;

import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic implementation of a HierarchyDir that stores hierarchies in a LinkedHashMap.
 * This implementation provides a directory-like structure for managing hierarchies
 * by string names while maintaining insertion order.
 * <p>
 * This class extends ConcurrentHashMap to provide efficient hierarchy lookup and
 * thread-safety while implementing the HierarchyDir interface.
 * 
 * @see HierarchyDir
 * @see Hierarchy
 * @see HierarchyDef
 */
public class HierarchyDirImpl extends ConcurrentHashMap<String, Hierarchy> implements HierarchyDir
{
    /** The hierarchy definition for this directory hierarchy. */
    private final HierarchyDef def;

    /**
     * Creates a new HierarchyDirImpl with the specified definition.
     * 
     * @param def the hierarchy definition for this directory
     */
    public HierarchyDirImpl(HierarchyDef def)
    {
        this.def = def;
    }

    /**
     * Creates a new HierarchyDirImpl with the specified definition, initial capacity, and load factor.
     * 
     * @param def the hierarchy definition for this directory
     * @param initialCapacity the initial capacity of the underlying map
     * @param loadFactor the load factor of the underlying map
     */
    public HierarchyDirImpl(HierarchyDef def, int initialCapacity, float loadFactor)
    {
        super(initialCapacity, loadFactor);
        this.def = def;
    }

    /**
     * Returns the hierarchy definition for this directory.
     * 
     * @return the hierarchy definition describing this directory's structure
     */
    @Override
    public HierarchyDef def()
    {
        return def;
    }
}
