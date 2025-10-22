package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.HierarchyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class AspectMapHierarchyImplTest
{
    private AspectDef aspectDef;
    private AspectMapHierarchyImpl hierarchy;
    private Entity entity1;
    private Entity entity2;
    private Aspect aspect1;
    private Aspect aspect2;
    private Catalog catalog;

    @BeforeEach
    void setUp()
    {
        catalog = new CatalogImpl();
        String uniqueName = "test" + System.nanoTime();
        aspectDef = new MutableAspectDefImpl(uniqueName);
        hierarchy = (AspectMapHierarchyImpl) catalog.createAspectMap(aspectDef, 0L);

        entity1 = new EntityImpl();
        entity2 = new EntityImpl();
        aspect1 = new AspectObjectMapImpl(entity1, aspectDef);
        aspect2 = new AspectObjectMapImpl(entity2, aspectDef);
    }

    @Test
    void constructor_ValidParameters_CreatesHierarchy()
    {
        assertEquals(aspectDef.name(), hierarchy.name());
        assertEquals(HierarchyType.ASPECT_MAP, hierarchy.type());
        assertSame(aspectDef, hierarchy.aspectDef());
        assertTrue(hierarchy.isEmpty());
    }

    @Test
    void put_NewEntityAspectPair_AddsMapping()
    {
        Aspect result = hierarchy.put(entity1, aspect1);
        
        assertNull(result);
        assertEquals(1, hierarchy.size());
        assertSame(aspect1, hierarchy.get(entity1));
        assertTrue(hierarchy.containsKey(entity1));
        assertTrue(hierarchy.containsValue(aspect1));
    }

    @Test
    void get_ExistingEntity_ReturnsAspect()
    {
        hierarchy.put(entity1, aspect1);

        assertSame(aspect1, hierarchy.get(entity1));
    }

    @Test
    void put_ExistingEntity_ReplacesAspect()
    {
        hierarchy.put(entity1, aspect1);
        assertEquals(1, hierarchy.size());

        Aspect result = hierarchy.put(entity1, aspect2);
        
        assertSame(aspect1, result);
        assertEquals(1, hierarchy.size());
        assertSame(aspect2, hierarchy.get(entity1));
        assertFalse(hierarchy.containsValue(aspect1));
        assertTrue(hierarchy.containsValue(aspect2));
    }

    @Test
    void put_MultipleEntities_AddsAllMappings()
    {
        hierarchy.put(entity1, aspect1);
        hierarchy.put(entity2, aspect2);
        
        assertEquals(2, hierarchy.size());
        assertSame(aspect1, hierarchy.get(entity1));
        assertSame(aspect2, hierarchy.get(entity2));
    }

    @Test
    void remove_ExistingEntity_RemovesMapping()
    {
        hierarchy.put(entity1, aspect1);
        
        Aspect result = hierarchy.remove(entity1);
        
        assertSame(aspect1, result);
        assertEquals(0, hierarchy.size());
    }

    @Test
    void values_PopulatedHierarchy_ReturnsAllAspects()
    {
        hierarchy.put(entity1, aspect1);
        hierarchy.put(entity2, aspect2);
        
        Collection<Aspect> values = hierarchy.values();
        
        assertEquals(2, values.size());
        assertTrue(values.contains(aspect1));
        assertTrue(values.contains(aspect2));
    }
}