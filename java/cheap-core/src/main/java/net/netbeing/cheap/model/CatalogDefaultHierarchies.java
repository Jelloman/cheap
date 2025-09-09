package net.netbeing.cheap.model;

import net.netbeing.cheap.impl.basic.HierarchyDefImpl;

/**
 * The interface Catalog default hierarchies.
 */
public interface CatalogDefaultHierarchies
{
    /**
     * The constant CATALOG_ROOT_NAME.
     */
    String CATALOG_ROOT_NAME = "hierarchies";
    /**
     * The constant CATALOG_ROOT.
     */
    HierarchyDef CATALOG_ROOT = new HierarchyDefImpl(CATALOG_ROOT_NAME, HierarchyType.HIERARCHY_DIR);

    /**
     * The constant ASPECTAGE_NAME.
     */
    String ASPECTAGE_NAME = "aspectage";
    /**
     * The constant ASPECTAGE.
     */
    HierarchyDef ASPECTAGE = new HierarchyDefImpl(ASPECTAGE_NAME, HierarchyType.ASPECT_DEF_DIR);
}
