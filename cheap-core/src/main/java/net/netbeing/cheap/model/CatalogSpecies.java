package net.netbeing.cheap.model;

/**
 * A Species is the fundamental type of a catalog in the Cheap data model.
 * The species determines the data source relationship and caching behavior
 * of the catalog within the overall system architecture.
 * 
 * <p>All catalogs in Cheap are caches or working copies, but they differ in
 * their relationship to upstream data sources and their write-through behavior.</p>
 */
public enum CatalogSpecies
{
    /**
     * A Source catalog represents a read-only cache of an external data source.
     */
    SOURCE,

    /**
     * A Sink catalog represents a read-write working copy of an external data source.
     */
    SINK,

    /**
     * A Mirror catalog provides a cached read-only view of another catalog.
     */
    MIRROR,

    /**
     * A Cache catalog provides a write-though view of another catalog.
     * (Writes may be buffered.)
     */
    CACHE,

    /**
     * A Clone catalog is a working copy of another catalog. Reads and writes to and from
     * the upstream catalog are manually invoked.
     */
    CLONE,

    /**
     * A Fork catalog is a transient copy of another catalog, severed from the original.
     * Forks should usually be converted into a Sink, i.e., a permanent copy, AKA "Save As".
     */
    FORK
}
