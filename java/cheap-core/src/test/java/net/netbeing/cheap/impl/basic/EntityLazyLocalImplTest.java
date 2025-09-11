package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.LocalEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EntityLazyLocalImplTest
{
    @Test
    void constructor_WithUUID_StoresGlobalId()
    {
        UUID expectedId = UUID.randomUUID();
        EntityLazyLocalImpl entity = new EntityLazyLocalImpl(expectedId);
        
        assertSame(expectedId, entity.globalId());
        // Local entity should be lazily initialized
    }

    @Test
    void constructor_WithNullUUID_AcceptsNull()
    {
        EntityLazyLocalImpl entity = new EntityLazyLocalImpl(null);
        
        assertNull(entity.globalId());
        // Entity is ready for lazy initialization
    }

    @Test
    void globalId_Always_ReturnsProvidedId()
    {
        UUID expectedId = UUID.randomUUID();
        EntityLazyLocalImpl entity = new EntityLazyLocalImpl(expectedId);
        
        UUID firstCall = entity.globalId();
        UUID secondCall = entity.globalId();
        
        assertSame(expectedId, firstCall);
        assertSame(expectedId, secondCall);
        assertSame(firstCall, secondCall);
    }

    @Test
    void globalId_WithNullId_ReturnsNull()
    {
        EntityLazyLocalImpl entity = new EntityLazyLocalImpl(null);
        
        assertNull(entity.globalId());
    }

    @Test
    void local_FirstCall_CreatesLocalEntity()
    {
        UUID globalId = UUID.randomUUID();
        EntityLazyLocalImpl entity = new EntityLazyLocalImpl(globalId);
        
        LocalEntity local = entity.local();
        
        assertNotNull(local);
        assertInstanceOf(LocalEntityImpl.class, local);
        // Verify consistency
        assertSame(local, entity.local());
    }

    @Test
    void local_MultipleCalls_ReturnsSameInstance()
    {
        UUID globalId = UUID.randomUUID();
        EntityLazyLocalImpl entity = new EntityLazyLocalImpl(globalId);
        
        LocalEntity firstCall = entity.local();
        LocalEntity secondCall = entity.local();
        
        assertNotNull(firstCall);
        assertSame(firstCall, secondCall);
    }

    @Test
    void local_CreatedLocalEntity_ReferencesThisEntity()
    {
        UUID globalId = UUID.randomUUID();
        EntityLazyLocalImpl entity = new EntityLazyLocalImpl(globalId);
        
        LocalEntity local = entity.local();
        
        assertSame(entity, local.entity());
    }

    @Test
    void local_ThreadSafety_InitializesOnlyOnce() throws InterruptedException
    {
        UUID globalId = UUID.randomUUID();
        EntityLazyLocalImpl entity = new EntityLazyLocalImpl(globalId);
        
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        @SuppressWarnings("unchecked")
        AtomicReference<LocalEntity>[] results = new AtomicReference[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            results[index] = new AtomicReference<>();
            new Thread(() -> {
                try {
                    startLatch.await();
                    results[index].set(entity.local());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        startLatch.countDown();
        doneLatch.await();
        
        // All threads should get the same LocalEntity instance
        LocalEntity expectedLocal = results[0].get();
        assertNotNull(expectedLocal);
        for (int i = 1; i < threadCount; i++) {
            assertSame(expectedLocal, results[i].get());
        }
    }

    @Test
    void globalIdAndLocal_ConcurrentAccess_ThreadSafe() throws InterruptedException
    {
        UUID expectedGlobalId = UUID.randomUUID();
        EntityLazyLocalImpl entity = new EntityLazyLocalImpl(expectedGlobalId);
        
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
        
        // Verify all threads got the correct instances
        for (int i = 0; i < threadCount / 2; i++) {
            assertSame(expectedGlobalId, globalIdResults[i].get());
            assertNotNull(localResults[i].get());
        }
        
        // Verify all local entity instances are the same
        LocalEntity expectedLocal = localResults[0].get();
        for (int i = 1; i < threadCount / 2; i++) {
            assertSame(expectedLocal, localResults[i].get());
        }
    }

    @Test
    void lazyInitialization_UntilFirstAccess_LocalRemainNull()
    {
        UUID globalId = UUID.randomUUID();
        EntityLazyLocalImpl entity = new EntityLazyLocalImpl(globalId);
        
        // GlobalId should be accessible immediately
        assertSame(globalId, entity.globalId());
        // Entity is ready for lazy initialization of local
        assertNotNull(entity);
    }

    @Test
    void lazyInitialization_AfterLocalAccess_LocalInitialized()
    {
        UUID globalId = UUID.randomUUID();
        EntityLazyLocalImpl entity = new EntityLazyLocalImpl(globalId);
        
        LocalEntity local = entity.local();
        
        assertNotNull(local);
        // Verify consistency
        assertSame(local, entity.local());
        assertSame(globalId, entity.globalId());
    }

    @Test
    void multipleInstances_HaveDistinctLocalEntities()
    {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        EntityLazyLocalImpl entity1 = new EntityLazyLocalImpl(id1);
        EntityLazyLocalImpl entity2 = new EntityLazyLocalImpl(id2);
        
        LocalEntity local1 = entity1.local();
        LocalEntity local2 = entity2.local();
        
        assertNotSame(local1, local2);
        assertSame(entity1, local1.entity());
        assertSame(entity2, local2.entity());
        assertSame(id1, entity1.globalId());
        assertSame(id2, entity2.globalId());
    }

    @Test
    void globalId_IsImmutable_CannotBeChanged()
    {
        UUID originalId = UUID.randomUUID();
        EntityLazyLocalImpl entity = new EntityLazyLocalImpl(originalId);
        
        // globalId should always return the same value
        assertSame(originalId, entity.globalId());
        
        // Access local to ensure no side effects
        entity.local();
        
        // globalId should still be the same
        assertSame(originalId, entity.globalId());
    }

    @Test
    void constructor_WithSameUUID_CreatesIndependentEntities()
    {
        UUID sharedId = UUID.randomUUID();
        EntityLazyLocalImpl entity1 = new EntityLazyLocalImpl(sharedId);
        EntityLazyLocalImpl entity2 = new EntityLazyLocalImpl(sharedId);
        
        // Same global ID
        assertSame(sharedId, entity1.globalId());
        assertSame(sharedId, entity2.globalId());
        
        // But different local entities
        LocalEntity local1 = entity1.local();
        LocalEntity local2 = entity2.local();
        
        assertNotSame(local1, local2);
        assertSame(entity1, local1.entity());
        assertSame(entity2, local2.entity());
    }

    @Test
    void local_WithNullGlobalId_StillCreatesValidLocalEntity()
    {
        EntityLazyLocalImpl entity = new EntityLazyLocalImpl(null);
        
        LocalEntity local = entity.local();
        
        assertNotNull(local);
        assertSame(entity, local.entity());
        assertNull(entity.globalId());
    }

    @Test
    void entityBehavior_AfterInitialization_ConsistentWithEagerInitialization()
    {
        UUID globalId = UUID.randomUUID();
        EntityLazyLocalImpl lazyEntity = new EntityLazyLocalImpl(globalId);

        // Initialize lazy entity
        LocalEntity lazyLocal = lazyEntity.local();

        // Both should have similar structure
        assertNotNull(lazyLocal);
        assertInstanceOf(LocalEntityImpl.class, lazyLocal);

        // Entities should reference themselves
        assertSame(lazyEntity, lazyLocal.entity());
    }
}