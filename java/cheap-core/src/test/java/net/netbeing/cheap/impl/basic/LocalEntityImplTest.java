package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LocalEntityImplTest
{
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
        assertNull(localEntity.aspects); // Lazy initialization
    }

    @Test
    void constructor_WithEntityAndAspect_CreatesLocalEntityWithAspect()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity, aspect1);
        
        assertSame(entity, localEntity.entity());
        assertNotNull(localEntity.aspects);
        assertEquals(aspect1, localEntity.aspects.get(aspectDef1));
    }

    @Test
    void constructor_WithNullEntity_AcceptsNull()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(null);
        
        assertNull(localEntity.entity());
    }

    @Test
    void constructor_WithEntityAndNullAspect_ThrowsException()
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
    void aspects_ThreadSafety_InitializesOnlyOnce() throws InterruptedException
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        @SuppressWarnings("unchecked")
        AtomicReference<Map<AspectDef, Aspect>>[] results = new AtomicReference[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            results[index] = new AtomicReference<>();
            new Thread(() -> {
                try {
                    startLatch.await();
                    results[index].set(localEntity.aspects());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        startLatch.countDown();
        doneLatch.await();
        
        // All threads should get the same map instance
        Map<AspectDef, Aspect> expectedMap = results[0].get();
        for (int i = 1; i < threadCount; i++) {
            assertSame(expectedMap, results[i].get());
        }
    }

    @Test
    void aspect_WithNullDef_ReturnsNull()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        
        Aspect result = localEntity.aspect(null);
        
        assertNull(result);
    }

    @Test
    void aspect_WithNonExistentDef_ReturnsNull()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        
        Aspect result = localEntity.aspect(aspectDef1);
        
        assertNull(result);
    }

    @Test
    void aspect_WithExistingDef_ReturnsAspect()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        localEntity.aspects().put(aspectDef1, aspect1);
        
        Aspect result = localEntity.aspect(aspectDef1);
        
        assertSame(aspect1, result);
    }

    @Test
    void aspect_BeforeAspectsInitialized_InitializesAndReturnsNull()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        assertNull(localEntity.aspects); // Not yet initialized
        
        Aspect result = localEntity.aspect(aspectDef1);
        
        assertNull(result);
        assertNotNull(localEntity.aspects); // Should be initialized now
    }

    @Test
    void aspect_AfterAspectsInitialized_UsesExistingMap()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        Map<AspectDef, Aspect> aspectsMap = localEntity.aspects();
        aspectsMap.put(aspectDef1, aspect1);
        
        Aspect result = localEntity.aspect(aspectDef1);
        
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
        assertSame(aspect1, localEntity.aspect(aspectDef1));
        assertSame(aspect2, localEntity.aspect(aspectDef2));
    }

    @Test
    void constructor_WithInitialAspect_SetsUpMapCorrectly()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity, aspect1);
        
        assertNotNull(localEntity.aspects);
        assertEquals(1, localEntity.aspects.size());
        assertSame(aspect1, localEntity.aspects.get(aspectDef1));
        assertSame(aspect1, localEntity.aspect(aspectDef1));
    }

    @Test
    void aspects_LazyInitialization_CreatesWeakAspectMap()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        
        Map<AspectDef, Aspect> aspectsMap = localEntity.aspects();
        
        assertInstanceOf(WeakAspectMap.class, aspectsMap);
    }

    @Test
    void aspects_PreInitialized_ReturnsWeakAspectMap()
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity, aspect1);
        
        Map<AspectDef, Aspect> aspectsMap = localEntity.aspects();
        
        assertInstanceOf(WeakAspectMap.class, aspectsMap);
    }

    @Test
    void aspect_ConcurrentAccess_ThreadSafe() throws InterruptedException
    {
        LocalEntityImpl localEntity = new LocalEntityImpl(entity);
        localEntity.aspects().put(aspectDef1, aspect1);
        
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        @SuppressWarnings("unchecked")
        AtomicReference<Aspect>[] results = new AtomicReference[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            results[index] = new AtomicReference<>();
            new Thread(() -> {
                try {
                    startLatch.await();
                    results[index].set(localEntity.aspect(aspectDef1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        startLatch.countDown();
        doneLatch.await();
        
        // All threads should get the same aspect
        for (int i = 0; i < threadCount; i++) {
            assertSame(aspect1, results[i].get());
        }
    }
}