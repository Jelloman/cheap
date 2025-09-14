package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogImplTest
{
    @Test
    void def_CreateCatalogNoDef_ThenSink()
    {
        Catalog c = new CatalogImpl();
        CatalogDef def = c.def();

        assertNotNull(def);
        assertNotNull(c.globalId());
        assertNull(c.upstream());
        assertFalse(c.isStrict());
        assertEquals(CatalogSpecies.SINK, c.species());
    }

    @Test
    void upstream_CreateSinkCatalogWithUpstream_ThenIllegalStateException()
    {
        Catalog upstream = new CatalogImpl();
        assertThrows(IllegalArgumentException.class, () -> new CatalogImpl(CatalogSpecies.SINK, upstream));
    }

    @Test
    void upstream_CreateSourceCatalogWithUpstream_ThenIllegalStateException()
    {
        Catalog upstream = new CatalogImpl();
        assertThrows(IllegalArgumentException.class, () -> new CatalogImpl(CatalogSpecies.SOURCE, upstream));
    }

    @Test
    void hierarchies()
    {
        Catalog catalog = new CatalogImpl();
        
        // Check that hierarchies() returns a HierarchyDir
        assertNotNull(catalog.hierarchies());

        // Verify that the hierarchies map contains the expected entries
        assertTrue(catalog.hierarchies().containsKey(CatalogDefaultHierarchies.CATALOG_ROOT_NAME));
        assertTrue(catalog.hierarchies().containsKey(CatalogDefaultHierarchies.ASPECTAGE_NAME));

        // Check that the default hierarchies are available under expected keys
        assertNotNull(catalog.hierarchy(CatalogDefaultHierarchies.CATALOG_ROOT_NAME));
        assertNotNull(catalog.hierarchy(CatalogDefaultHierarchies.ASPECTAGE_NAME));
        
        // Check that the hierarchies implement the expected interfaces
        assertInstanceOf(HierarchyDir.class, catalog.hierarchy(CatalogDefaultHierarchies.CATALOG_ROOT_NAME));
        assertInstanceOf(AspectDefDirHierarchy.class, catalog.hierarchy(CatalogDefaultHierarchies.ASPECTAGE_NAME));
        
        // Verify that the hierarchies map returns the same instances
        assertSame(catalog.hierarchies(), catalog.hierarchy(CatalogDefaultHierarchies.CATALOG_ROOT_NAME));
        assertSame(catalog.hierarchies().get(CatalogDefaultHierarchies.ASPECTAGE_NAME),
            catalog.hierarchy(CatalogDefaultHierarchies.ASPECTAGE_NAME));
    }
}