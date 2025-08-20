package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogType;
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
    }
}