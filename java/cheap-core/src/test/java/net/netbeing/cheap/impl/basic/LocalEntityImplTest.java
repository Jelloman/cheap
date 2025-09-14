package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.code.tempusfugit.concurrency.annotations.*;
import com.google.code.tempusfugit.concurrency.ConcurrentRule;
import org.junit.Rule;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocalEntityImplTest
{
    @Rule
    public ConcurrentRule concurrentRule = new ConcurrentRule();
    
    private Entity entity;
    private AspectDef aspectDef1;
    private AspectDef aspectDef2;
    private Aspect aspect1;
    private Aspect aspect2;
    private Catalog catalog;

    @BeforeEach
    void setUp()
    {
        catalog = new CatalogImpl();
        entity = new EntityFullImpl();
        aspectDef1 = new MutableAspectDefImpl("aspect1");
        aspectDef2 = new MutableAspectDefImpl("aspect2");
        aspect1 = new AspectObjectMapImpl(catalog, entity, aspectDef1);
        aspect2 = new AspectObjectMapImpl(catalog, entity, aspectDef2);
    }

    @Test
    void constructor_WithEntity_CreatesLocalEntity()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        
        assertSame(entity, localEntity.entity());
    }

    @Test
    void constructor_WithEntityAndAspect_CreatesLocalEntityWithGetAspectIfPresent()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity, aspect1);
        
        assertSame(entity, localEntity.entity());
        assertEquals(aspect1, localEntity.getAspectIfPresent(aspectDef1));
    }

    @Test
    void constructor_WithNullEntity_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> new LocalEntityImpl(null));
    }

    @Test
    void constructor_WithEntityAndNullGetAspectIfPresent_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> new LocalEntityImpl(entity, null));
    }

    @Test
    void entity_Always_ReturnsProvidedEntity()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        
        assertSame(entity, localEntity.entity());
    }

    @Test
    void aspects_FirstCall_InitializesMap()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        
        Map<AspectDef, Aspect> aspects = localEntity.aspects();
        
        assertNotNull(aspects);
        assertNotNull(localEntity.aspects);
        assertTrue(aspects.isEmpty());
    }

    @Test
    void aspects_MultipleCallsSameInstance_ReturnsSameMap()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        
        Map<AspectDef, Aspect> first = localEntity.aspects();
        Map<AspectDef, Aspect> second = localEntity.aspects();
        
        assertSame(first, second);
    }

    @Test
    @Concurrent(count = 10)
    @Repeating(repetition = 100)
    void aspects_ThreadSafety_InitializesOnlyOnce()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        Map<AspectDef, Aspect> aspects = localEntity.aspects();
        assertNotNull(aspects);
        assertSame(aspects, localEntity.aspects());
    }

    @Test
    void getAspectIfPresent_WithNullDef_ReturnsNull()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        
        Aspect result = localEntity.getAspectIfPresent(null);
        
        assertNull(result);
    }

    @Test
    void getAspectIfPresent_WithNonExistentDef_ReturnsNull()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        
        Aspect result = localEntity.getAspectIfPresent(aspectDef1);
        
        assertNull(result);
    }

    @Test
    void aspect_WithExistingDef_ReturnsGetAspectIfPresent()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        localEntity.aspects().put(aspectDef1, aspect1);
        
        Aspect result = localEntity.getAspectIfPresent(aspectDef1);
        
        assertSame(aspect1, result);
    }

    @Test
    void getAspectIfPresent_BeforeAspectsInitialized_InitializesAndReturnsNull()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        assertNull(localEntity.aspects); // Not yet initialized
        
        Aspect result = localEntity.getAspectIfPresent(aspectDef1);
        
        assertNull(result);
        assertNotNull(localEntity.aspects); // Should be initialized now
    }

    @Test
    void getAspectIfPresent_AfterAspectsInitialized_UsesExistingMap()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        Map<AspectDef, Aspect> aspectsMap = localEntity.aspects();
        aspectsMap.put(aspectDef1, aspect1);
        
        Aspect result = localEntity.getAspectIfPresent(aspectDef1);
        
        assertSame(aspect1, result);
        assertSame(aspectsMap, localEntity.aspects); // Should use same map
    }

    @Test
    void aspects_AddAndRetrieve_WorksCorrectly()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        Map<AspectDef, Aspect> aspectsMap = localEntity.aspects();
        
        aspectsMap.put(aspectDef1, aspect1);
        aspectsMap.put(aspectDef2, aspect2);
        
        assertEquals(2, aspectsMap.size());
        assertSame(aspect1, aspectsMap.get(aspectDef1));
        assertSame(aspect2, aspectsMap.get(aspectDef2));
        assertSame(aspect1, localEntity.getAspectIfPresent(aspectDef1));
        assertSame(aspect2, localEntity.getAspectIfPresent(aspectDef2));
    }

    @Test
    void constructor_WithInitialGetAspectIfPresent_SetsUpMapCorrectly()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity, aspect1);
        
        assertNotNull(localEntity.aspects);
        assertEquals(1, localEntity.aspects.size());
        assertSame(aspect1, localEntity.getAspectIfPresent(aspectDef1));
    }

    @Test
    @Concurrent(count = 5)
    @Repeating(repetition = 20)
    void getAspectIfPresent_ConcurrentAccess_ThreadSafe()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        localEntity.aspects().put(aspectDef1, aspect1);
        
        Aspect result = localEntity.getAspectIfPresent(aspectDef1);
        assertSame(aspect1, result);
    }
}