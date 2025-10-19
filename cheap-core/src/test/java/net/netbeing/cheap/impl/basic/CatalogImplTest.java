package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogSpecies;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CatalogImplTest
{
    @Test
    void def_CreateCatalogNoDef_ThenSink()
    {
        Catalog c = new CatalogImpl();

        assertNotNull(c.globalId());
        assertNull(c.upstream());

        assertEquals(CatalogSpecies.SINK, c.species());
    }

    @Test
    void upstream_CreateSinkCatalogWithUpstream_ThenIllegalStateException()
    {
        UUID upstream = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new CatalogImpl(CatalogSpecies.SINK, upstream));
    }

    @Test
    void upstream_CreateSourceCatalogWithUpstream_ThenIllegalStateException()
    {
        UUID upstream = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new CatalogImpl(CatalogSpecies.SOURCE, upstream));
    }
}