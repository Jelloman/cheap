package net.netbeing.cheap.model;

/**
 * Defines the fundamental types of catalogs in the CHEAP data model.
 * Catalog types determine the data source relationship and caching behavior
 * of the catalog within the overall system architecture.
 * 
 * <p>All catalogs in CHEAP are caches, but they differ in their relationship
 * to external data sources and their write-through behavior.</p>
 */
public enum CatalogType
{
    /**
     * A root catalog that represents an external data source. Root catalogs
     * serve as the primary interface to external systems and define the
     * authoritative source for data within their scope.
     */
    ROOT,
    
    /**
     * A mirror catalog that provides a cached view of another catalog.
     * Mirror catalogs improve performance by maintaining local copies of
     * frequently accessed data from upstream catalogs.
     */
    MIRROR
}
