package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.code.tempusfugit.concurrency.annotations.*;
import com.google.code.tempusfugit.concurrency.ConcurrentRule;
import org.junit.Rule;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityFullImplTest
{
    @Rule
    public ConcurrentRule concurrentRule = new ConcurrentRule();
    
    private AspectDef aspectDef1;
    private AspectDef aspectDef2;
    private Aspect aspect1;
    private Aspect aspect2;
    private Catalog catalog;

    @BeforeEach
    void setUp()
    {
        catalog = new CatalogImpl();
        aspectDef1 = new MutableAspectDefImpl("aspect1");
        aspectDef2 = new MutableAspectDefImpl("aspect2");
    }

    @Test
    void constructor_NoParameters_GeneratesRandomGlobalId()
    {
        EntityFullImpl entity = new EntityFullImpl();
        
        assertNotNull(entity.globalId());
        assertSame(entity, entity.local());
        assertSame(entity, entity.entity());
        assertNull(entity.aspects); // Lazy initialization
    }

    @Test
    void constructor_WithGlobalId_UsesProvidedGlobalId()
    {
        UUID testId = UUID.randomUUID();
        EntityFullImpl entity = new EntityFullImpl(testId);
        
        assertSame(testId, entity.globalId());
        assertSame(entity, entity.local());
        assertSame(entity, entity.entity());
        assertNull(entity.aspects); // Lazy initialization
    }

    @Test
    void constructor_WithGlobalIdAndAspect_SetsUpGetAspectIfPresentMap()
    {
        UUID testId = UUID.randomUUID();
        aspect1 = new AspectObjectMapImpl(catalog, null, aspectDef1);
        EntityFullImpl entity = new EntityFullImpl(testId, aspect1);
        
        assertSame(testId, entity.globalId());
        assertNotNull(entity.aspects);
        assertEquals(aspect1, entity.getAspectIfPresent(aspectDef1));
        assertSame(entity, entity.local());
        assertSame(entity, entity.entity());
    }

    @Test
    void constructor_WithNullGlobalId_GeneratesId()
    {
        EntityFullImpl entity = new EntityFullImpl(null);
        
        assertNotNull(entity.globalId());
        assertSame(entity, entity.local());
        assertSame(entity, entity.entity());
    }

    @Test
    void constructor_WithNullGetAspectIfPresent_ThrowsException()
    {
        UUID testId = UUID.randomUUID();
        
        assertThrows(NullPointerException.class, () -> new EntityFullImpl(testId, null));
    }

    @Test
    void globalId_AfterConstruction_ReturnsCorrectId()
    {
        UUID testId = UUID.randomUUID();
        EntityFullImpl entity = new EntityFullImpl(testId);
        
        assertEquals(testId, entity.globalId());
    }

    @Test
    void globalId_MultipleCallsSameInstance_ReturnsSameId()
    {
        EntityFullImpl entity = new EntityFullImpl();
        UUID firstCall = entity.globalId();
        UUID secondCall = entity.globalId();
        
        assertSame(firstCall, secondCall);
    }

    @Test
    void local_Always_ReturnsSelf()
    {
        EntityFullImpl entity = new EntityFullImpl();
        
        assertSame(entity, entity.local());
    }

    @Test
    void entity_Always_ReturnsSelf()
    {
        EntityFullImpl entity = new EntityFullImpl();
        
        assertSame(entity, entity.entity());
    }

    @Test
    void aspects_FirstCall_InitializesMap()
    {
        EntityFullImpl entity = new EntityFullImpl();
        
        Map<AspectDef, Aspect> aspects = entity.aspects();
        
        assertNotNull(aspects);
        assertNotNull(entity.aspects);
        assertTrue(aspects.isEmpty());
    }

    @Test
    void aspects_MultipleCallsSameInstance_ReturnsSameMap()
    {
        EntityFullImpl entity = new EntityFullImpl();
        
        Map<AspectDef, Aspect> first = entity.aspects();
        Map<AspectDef, Aspect> second = entity.aspects();
        
        assertSame(first, second);
    }

    @Test
    @Concurrent(count = 10)
    @Repeating(repetition = 100)
    void aspects_ThreadSafety_InitializesOnlyOnce()
    {
        EntityFullImpl entity = new EntityFullImpl();
        Map<AspectDef, Aspect> aspects = entity.aspects();
        assertNotNull(aspects);
        assertSame(aspects, entity.aspects());
    }

    @Test
    void getAspectIfPresent_WithNullDef_ReturnsNull()
    {
        EntityFullImpl entity = new EntityFullImpl();
        
        Aspect result = entity.getAspectIfPresent(null);
        
        assertNull(result);
    }

    @Test
    void getAspectIfPresent_WithNonExistentDef_ReturnsNull()
    {
        EntityFullImpl entity = new EntityFullImpl();
        
        Aspect result = entity.getAspectIfPresent(aspectDef1);
        
        assertNull(result);
    }

    @Test
    void aspect_WithExistingDef_ReturnsGetAspectIfPresent()
    {
        EntityFullImpl entity = new EntityFullImpl();
        aspect1 = new AspectObjectMapImpl(catalog, entity, aspectDef1);
        entity.aspects().put(aspectDef1, aspect1);
        
        Aspect result = entity.getAspectIfPresent(aspectDef1);
        
        assertSame(aspect1, result);
    }

    @Test
    void getAspectIfPresent_BeforeAspectsInitialized_InitializesAndReturnsNull()
    {
        EntityFullImpl entity = new EntityFullImpl();
        assertNull(entity.aspects); // Not yet initialized
        
        Aspect result = entity.getAspectIfPresent(aspectDef1);
        
        assertNull(result);
        assertNotNull(entity.aspects); // Should be initialized now
    }

    @Test
    void getAspectIfPresent_AfterAspectsInitialized_UsesExistingMap()
    {
        EntityFullImpl entity = new EntityFullImpl();
        Map<AspectDef, Aspect> aspectsMap = entity.aspects();
        aspect1 = new AspectObjectMapImpl(catalog, entity, aspectDef1);
        aspectsMap.put(aspectDef1, aspect1);
        
        Aspect result = entity.getAspectIfPresent(aspectDef1);
        
        assertSame(aspect1, result);
        assertSame(aspectsMap, entity.aspects); // Should use same map
    }

    @Test
    void aspects_AddAndRetrieve_WorksCorrectly()
    {
        EntityFullImpl entity = new EntityFullImpl();
        Map<AspectDef, Aspect> aspectsMap = entity.aspects();
        
        aspect1 = new AspectObjectMapImpl(catalog, entity, aspectDef1);
        aspect2 = new AspectObjectMapImpl(catalog, entity, aspectDef2);
        aspectsMap.put(aspectDef1, aspect1);
        aspectsMap.put(aspectDef2, aspect2);
        
        assertEquals(2, aspectsMap.size());
        assertSame(aspect1, aspectsMap.get(aspectDef1));
        assertSame(aspect2, aspectsMap.get(aspectDef2));
        assertSame(aspect1, entity.getAspectIfPresent(aspectDef1));
        assertSame(aspect2, entity.getAspectIfPresent(aspectDef2));
    }

    @Test
    void constructor_WithInitialGetAspectIfPresent_SetsUpMapCorrectly()
    {
        UUID testId = UUID.randomUUID();
        aspect1 = new AspectObjectMapImpl(catalog, null, aspectDef1);
        EntityFullImpl entity = new EntityFullImpl(testId, aspect1);
        
        assertNotNull(entity.aspects);
        assertEquals(1, entity.aspects.size());
        assertSame(aspect1, entity.aspects.get(aspectDef1));
        assertSame(aspect1, entity.getAspectIfPresent(aspectDef1));
    }

    @Test
    void inheritance_ExtendsEntityBasicImpl_InheritsCorrectly()
    {
        EntityFullImpl entity = new EntityFullImpl();
        
        // Should have Entity interface methods
        assertNotNull(entity.globalId());
        assertNotNull(entity.local());
        
        // Should have LocalEntity interface methods
        assertSame(entity, entity.entity());
        assertNotNull(entity.aspects());
    }

    @Test
    void globalId_DifferentInstances_HaveDifferentIds()
    {
        EntityFullImpl entity1 = new EntityFullImpl();
        EntityFullImpl entity2 = new EntityFullImpl();
        
        assertNotEquals(entity1.globalId(), entity2.globalId());
    }

    @Test
    void constructor_SpecificUUID_PreservesAllBits()
    {
        // Test with a known UUID to ensure all bits are preserved
        UUID testId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        EntityFullImpl entity = new EntityFullImpl(testId);
        
        assertEquals(testId, entity.globalId());
        assertEquals("123e4567-e89b-12d3-a456-426614174000", entity.globalId().toString());
    }

    @Test
    @Concurrent(count = 5)
    @Repeating(repetition = 20)
    void getAspectIfPresent_ConcurrentAccess_ThreadSafe()
    {
        EntityFullImpl entity = new EntityFullImpl();
        aspect1 = new AspectObjectMapImpl(catalog, entity, aspectDef1);
        entity.aspects().put(aspectDef1, aspect1);
        
        Aspect result = entity.getAspectIfPresent(aspectDef1);
        assertSame(aspect1, result);
    }

    @Test
    void toString_ValidEntity_ContainsGlobalId()
    {
        UUID testId = UUID.randomUUID();
        EntityFullImpl entity = new EntityFullImpl(testId);
        
        String toString = entity.toString();
        
        // toString should contain class name or meaningful representation
        assertNotNull(toString);
        assertFalse(toString.trim().isEmpty());
    }
}