package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EntityLazyIdImplTest
{
    private Catalog catalog;
    private AspectDef aspectDef;
    private Aspect aspect;

    @BeforeEach
    void setUp()
    {
        catalog = new CatalogImpl();
        aspectDef = new MutableAspectDefImpl("testAspect");
        // aspect will be created as needed in tests
    }

    @Test
    void constructor_Default_CreatesWithNewLocalEntity()
    {
        EntityLazyIdImpl entity = new EntityLazyIdImpl();
        
        assertNotNull(entity);
        // GlobalId should be lazily initialized
        assertNotNull(entity.local()); // Should be immediately available
        assertInstanceOf(LocalEntityImpl.class, entity.local());
    }

    @Test
    void constructor_WithAspect_CreatesLocalEntityWithAspect()
    {
        aspect = new AspectObjectMapImpl(catalog, new EntityFullImpl(), aspectDef);
        EntityLazyIdImpl entity = new EntityLazyIdImpl(aspect);
        
        LocalEntity local = entity.local();
        assertNotNull(local);
        assertInstanceOf(LocalEntityImpl.class, local);
        // GlobalId should be lazily initialized
        
        // Verify the aspect is available in the local entity
        assertEquals(aspect, local.aspect(aspectDef));
    }

    @Test
    void constructor_WithNullLocalEntity_ThrowsException()
    {
       assertThrows(NullPointerException.class, () -> new EntityLazyIdImpl((LocalEntity) null));
    }

    @Test
    void constructor_WithNullAspect_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> new EntityLazyIdImpl((Aspect) null));
    }

    @Test
    void globalId_FirstCall_GeneratesUUID()
    {
        EntityLazyIdImpl entity = new EntityLazyIdImpl();
        
        UUID globalId = entity.globalId();
        
        assertNotNull(globalId);
        // Verify consistency
        assertSame(globalId, entity.globalId());
    }

    @Test
    void globalId_MultipleCalls_ReturnsSameUUID()
    {
        EntityLazyIdImpl entity = new EntityLazyIdImpl();
        
        UUID firstCall = entity.globalId();
        UUID secondCall = entity.globalId();
        
        assertNotNull(firstCall);
        assertSame(firstCall, secondCall);
    }

    @Test
    void globalId_ThreadSafety_InitializesOnlyOnce() throws InterruptedException
    {
        EntityLazyIdImpl entity = new EntityLazyIdImpl();
        
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        @SuppressWarnings("unchecked")
        AtomicReference<UUID>[] results = new AtomicReference[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            results[index] = new AtomicReference<>();
            new Thread(() -> {
                try {
                    startLatch.await();
                    results[index].set(entity.globalId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        startLatch.countDown();
        doneLatch.await();
        
        // All threads should get the same UUID instance
        UUID expectedUUID = results[0].get();
        assertNotNull(expectedUUID);
        for (int i = 1; i < threadCount; i++) {
            assertSame(expectedUUID, results[i].get());
        }
    }

    @Test
    void local_DefaultConstructor_ReturnsImmediatelyAvailableLocalEntity()
    {
        EntityLazyIdImpl entity = new EntityLazyIdImpl();
        
        LocalEntity local = entity.local();
        
        assertNotNull(local);
        assertInstanceOf(LocalEntityImpl.class, local);
        assertSame(entity, local.entity());
    }

    @Test
    void local_MultipleCalls_ReturnsSameInstance()
    {
        EntityLazyIdImpl entity = new EntityLazyIdImpl();
        
        LocalEntity firstCall = entity.local();
        LocalEntity secondCall = entity.local();
        
        assertNotNull(firstCall);
        assertSame(firstCall, secondCall);
    }

    @Test
    void local_WithProvidedLocalEntityWithExistingEntity_ThrowsException()
    {
        Entity tempEntity = new EntityFullImpl();
        LocalEntity providedLocal = new LocalEntityImpl(tempEntity);

        assertThrows(IllegalArgumentException.class, () -> new EntityLazyIdImpl(providedLocal));
    }

    @Test
    void local_ConstructorWithAspect_LocalEntityContainsAspect()
    {
        aspect = new AspectObjectMapImpl(catalog, new EntityFullImpl(), aspectDef);
        EntityLazyIdImpl entity = new EntityLazyIdImpl(aspect);
        
        LocalEntity local = entity.local();
        Aspect retrievedAspect = local.aspect(aspectDef);
        
        assertSame(aspect, retrievedAspect);
    }

    @Test
    void globalIdAndLocal_ConcurrentAccess_BothThreadSafe() throws InterruptedException
    {
        EntityLazyIdImpl entity = new EntityLazyIdImpl();
        
        int threadCount = 8;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        @SuppressWarnings("unchecked")
        AtomicReference<UUID>[] globalIdResults = new AtomicReference[threadCount / 2];
        @SuppressWarnings("unchecked")
        AtomicReference<LocalEntity>[] localResults = new AtomicReference[threadCount / 2];
        
        // Half threads access globalId, half access local
        for (int i = 0; i < threadCount / 2; i++) {
            final int index = i;
            globalIdResults[index] = new AtomicReference<>();
            localResults[index] = new AtomicReference<>();
            
            new Thread(() -> {
                try {
                    startLatch.await();
                    globalIdResults[index].set(entity.globalId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
            
            new Thread(() -> {
                try {
                    startLatch.await();
                    localResults[index].set(entity.local());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        startLatch.countDown();
        doneLatch.await();
        
        // Verify all threads got consistent results
        UUID expectedGlobalId = globalIdResults[0].get();
        LocalEntity expectedLocal = localResults[0].get();
        
        assertNotNull(expectedGlobalId);
        assertNotNull(expectedLocal);
        
        for (int i = 1; i < threadCount / 2; i++) {
            assertSame(expectedGlobalId, globalIdResults[i].get());
            assertSame(expectedLocal, localResults[i].get());
        }
    }

    @Test
    void lazyInitialization_UntilFirstAccess_GlobalIdRemainsNull()
    {
        EntityLazyIdImpl entity = new EntityLazyIdImpl();
        
        // Local should be immediately available
        assertNotNull(entity.local());
        // Entity is ready for lazy initialization of globalId
        assertNotNull(entity);
    }

    @Test
    void lazyInitialization_AfterGlobalIdAccess_GlobalIdInitialized()
    {
        EntityLazyIdImpl entity = new EntityLazyIdImpl();
        
        UUID globalId = entity.globalId();
        
        assertNotNull(globalId);
        // Verify consistency
        assertSame(globalId, entity.globalId());
        assertNotNull(entity.local());
    }

    @Test
    void localEntity_ReferencesCorrectParentEntity()
    {
        EntityLazyIdImpl entity = new EntityLazyIdImpl();
        
        LocalEntity local = entity.local();
        
        assertSame(entity, local.entity());
    }

    @Test
    void localEntity_WithAspectConstructor_ParentEntityReferenceCorrect()
    {
        aspect = new AspectObjectMapImpl(catalog, new EntityFullImpl(), aspectDef);
        EntityLazyIdImpl entity = new EntityLazyIdImpl(aspect);
        
        LocalEntity local = entity.local();
        
        assertSame(entity, local.entity());
    }

    @Test
    void globalId_GeneratedValues_AreUnique()
    {
        EntityLazyIdImpl entity1 = new EntityLazyIdImpl();
        EntityLazyIdImpl entity2 = new EntityLazyIdImpl();
        EntityLazyIdImpl entity3 = new EntityLazyIdImpl();
        
        UUID id1 = entity1.globalId();
        UUID id2 = entity2.globalId();
        UUID id3 = entity3.globalId();
        
        assertNotEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertNotEquals(id2, id3);
    }

    @Test
    void multipleInstances_WithDefaultConstructor_HaveDistinctLocalEntities()
    {
        EntityLazyIdImpl entity1 = new EntityLazyIdImpl();
        EntityLazyIdImpl entity2 = new EntityLazyIdImpl();
        
        LocalEntity local1 = entity1.local();
        LocalEntity local2 = entity2.local();
        
        assertNotSame(local1, local2);
        assertSame(entity1, local1.entity());
        assertSame(entity2, local2.entity());
    }

    @Test
    void constructorWithAspect_AspectEntityReferenceDifferentFromLazyEntity()
    {
        Entity originalEntity = new EntityFullImpl();
        aspect = new AspectObjectMapImpl(catalog, originalEntity, aspectDef);
        EntityLazyIdImpl lazyEntity = new EntityLazyIdImpl(aspect);
        
        // The aspect was created with originalEntity, but stored in lazyEntity's local
        assertNotSame(originalEntity, lazyEntity);
        assertSame(originalEntity, aspect.entity());
        
        // The local entity should reference the lazy entity
        LocalEntity local = lazyEntity.local();
        assertSame(lazyEntity, local.entity());
        
        // But the aspect in the local entity should be the same instance
        assertSame(aspect, local.aspect(aspectDef));
    }

    @Test
    void localEntityAspects_ThreadSafety_ConsistentAccess() throws InterruptedException
    {
        aspect = new AspectObjectMapImpl(catalog, new EntityFullImpl(), aspectDef);
        EntityLazyIdImpl entity = new EntityLazyIdImpl(aspect);
        
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
                    LocalEntity local = entity.local();
                    results[index].set(local.aspect(aspectDef));
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
            assertSame(aspect, results[i].get());
        }
    }

    @Test
    void constructor_existingEntity_throwsException()
    {
        LocalEntity providedLocal = new LocalEntityImpl(new EntityFullImpl());

        assertThrows(IllegalArgumentException.class, () -> new EntityLazyIdImpl(providedLocal));
    }
}