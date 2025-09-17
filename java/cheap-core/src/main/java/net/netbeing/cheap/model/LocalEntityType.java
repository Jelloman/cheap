package net.netbeing.cheap.model;

/**
 * Enumeration of the different types of LocalEntity implementations available
 * in the CHEAP system. Each type corresponds to a specific implementation class
 * with different performance characteristics and catalog management strategies.
 * 
 * <p>This enum is used to configure factory methods to create the appropriate
 * type of LocalEntity based on application needs.</p>
 * 
 * @see LocalEntity
 */
public enum LocalEntityType
{
    /**
     * Basic single-catalog entity implementation.
     * Corresponds to LocalEntityOneCatalogImpl.
     * <p>
     * This is the simplest implementation for entities that belong to a single catalog.
     * It provides basic functionality without caching or multi-catalog support.
     * </p>
     */
    SINGLE_CATALOG,

    /**
     * Multi-catalog entity implementation.
     * Corresponds to LocalEntityMultiCatalogImpl.
     * <p>
     * This implementation allows entities to belong to multiple catalogs simultaneously.
     * It maintains a set of catalogs and can search across all of them for aspects.
     * </p>
     */
    MULTI_CATALOG,

    /**
     * Caching single-catalog entity implementation.
     * Corresponds to CachingEntityOneCatalogImpl.
     * <p>
     * This implementation extends the single-catalog functionality with aspect caching
     * for improved performance when aspects are frequently accessed.
     * </p>
     */
    CACHING_SINGLE_CATALOG,

    /**
     * Caching multi-catalog entity implementation.
     * Corresponds to CachingEntityMultiCatalogImpl.
     * <p>
     * This implementation combines multi-catalog support with aspect caching,
     * providing the most feature-rich LocalEntity implementation.
     * </p>
     */
    CACHING_MULTI_CATALOG
}