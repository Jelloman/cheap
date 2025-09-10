package net.netbeing.cheap.model;

import net.netbeing.cheap.impl.basic.HierarchyDefImpl;

/**
 * The interface Catalog default hierarchies.
 */
public interface CatalogDefaultHierarchies
{
    String CATALOG_ROOT_NAME = "hierarchies";
    HierarchyDef CATALOG_ROOT = new HierarchyDefImpl(CATALOG_ROOT_NAME, HierarchyType.HIERARCHY_DIR);

    String ASPECTAGE_NAME = "aspectage";
    HierarchyDef ASPECTAGE = new HierarchyDefImpl(ASPECTAGE_NAME, HierarchyType.ASPECT_DEF_DIR);
}
