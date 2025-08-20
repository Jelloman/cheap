package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CatalogDefImplTest
{

    @Test
    void type_DefaultConstructor_ThenRoot()
    {
        CatalogDef def = new CatalogDefImpl();

        assertNotNull(def.globalId());

        assertEquals(CatalogType.MIRROR, def.type());
    }

    @Test
    void globalId()
    {
        CatalogDef def1 = new CatalogDefImpl();
        assertNotNull(def1.globalId());

        CatalogDef def2 = new CatalogDefImpl(CatalogType.MIRROR);
        assertNotNull(def2.globalId());
        assertEquals(CatalogType.MIRROR, def2.type());

        CatalogDef def3 = new CatalogDefImpl(CatalogType.ROOT);
        assertNotNull(def3.globalId());
        assertEquals(CatalogType.ROOT, def3.type());
    }

    @Test
    void hierarchyDefs()
    {
        CatalogDef def = new CatalogDefImpl();

        assertNotNull(def.hierarchyDefs());
    }

    @Test
    void hierarchyDef()
    {
    }
}