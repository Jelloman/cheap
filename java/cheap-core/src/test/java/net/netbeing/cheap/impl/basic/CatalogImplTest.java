package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.AspectDefDirHierarchy;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogType;
import net.netbeing.cheap.model.HierarchyDir;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogImplTest
{
    @Test
    void def_CreateCatalogNoDef_ThenRoot()
    {
        Catalog c = new CatalogImpl();
        CatalogDef def = c.def();

        assertNotNull(def);

        assertNotNull(def.globalId());

        assertEquals(CatalogType.ROOT, def.type());
    }

    @Test
    void upstream_CreateMirrorCatalogWithUpstream_ThenIllegalStateException()
    {
        Catalog upstream = new CatalogImpl();
        CatalogDef def = new CatalogDefImpl(CatalogType.ROOT);
        Throwable exception = assertThrows(IllegalStateException.class, () -> new CatalogImpl(def, upstream));
    }

    @Test
    void hierarchies()
    {
        Catalog catalog = new CatalogImpl();
        
        // Check that hierarchies() returns a HierarchyDir
        assertNotNull(catalog.hierarchies());

        // Verify that the hierarchies map contains the expected entries
        assertTrue(catalog.hierarchies().containsKey("hierarchies"));
        assertTrue(catalog.hierarchies().containsKey("aspectage"));

        // Check that the default hierarchies are available under expected keys
        assertNotNull(catalog.hierarchy("hierarchies"));
        assertNotNull(catalog.hierarchy("aspectage"));
        
        // Check that the hierarchies implement the expected interfaces
        assertInstanceOf(HierarchyDir.class, catalog.hierarchy("hierarchies"));
        assertInstanceOf(AspectDefDirHierarchy.class, catalog.hierarchy("aspectage"));
        
        // Verify that the hierarchies map returns the same instances
        assertSame(catalog.hierarchies(), catalog.hierarchy("hierarchies"));
        assertSame(catalog.hierarchies().get("aspectage"), catalog.hierarchy("aspectage"));
    }
}