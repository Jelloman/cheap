package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import org.jetbrains.annotations.NotNull;

import java.util.WeakHashMap;

/**
 * A specialized WeakHashMap for storing aspects by their definitions.
 * This implementation allows aspects to be garbage collected when they
 * are no longer referenced elsewhere, helping with memory management
 * in the CHEAP system.
 * <p>
 * The map uses aspect definitions as keys and aspects as values.
 * When an aspect definition is no longer strongly referenced, the
 * corresponding entry will be automatically removed from the map.
 * 
 * @see WeakHashMap
 * @see Aspect
 * @see AspectDef
 */
public class WeakAspectMap extends WeakHashMap<AspectDef, Aspect>
{
    /**
     * Creates a new WeakAspectMap with the specified initial capacity and load factor.
     * 
     * @param initialCapacity the initial capacity of the map
     * @param loadFactor the load factor of the map
     */
    public WeakAspectMap(int initialCapacity, float loadFactor)
    {
        super(initialCapacity, loadFactor);
    }

    /**
     * Creates a new WeakAspectMap with the specified initial capacity and default load factor.
     * 
     * @param initialCapacity the initial capacity of the map
     */
    public WeakAspectMap(int initialCapacity)
    {
        super(initialCapacity);
    }

    /**
     * Creates a new WeakAspectMap with default initial capacity and load factor.
     */
    public WeakAspectMap()
    {
    }

    /**
     * Adds an aspect to this map using its definition as the key.
     * 
     * @param aspect the aspect to add to this map
     */
    public void add(@NotNull Aspect aspect)
    {
        put(aspect.def(), aspect);
    }
}
