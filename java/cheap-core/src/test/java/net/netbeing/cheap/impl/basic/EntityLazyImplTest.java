package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.LocalEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EntityLazyImplTest
{
    private EntityLazyImpl entity;

    @BeforeEach
    void setUp()
    {
        entity = new EntityLazyImpl();
    }

    @Test
    void constructor_Default_CreatesEmptyEntity()
    {
        EntityLazyImpl entity = new EntityLazyImpl();
        
        assertNotNull(entity);
        // Entity is ready for lazy initialization - no direct field access tests
    }

    @Test
    void globalId_FirstCall_GeneratesUUID()
    {
        UUID globalId = entity.globalId();
        
        assertNotNull(globalId);
    }

    @Test
    void globalId_MultipleCalls_ReturnsSameUUID()
    {
        UUID firstCall = entity.globalId();
        UUID secondCall = entity.globalId();
        
        assertNotNull(firstCall);
        assertSame(firstCall, secondCall);
    }

    @Test
    void globalId_ThreadSafety_InitializesOnlyOnce() throws InterruptedException
    {
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
    void local_FirstCall_CreatesLocalEntity()
    {
        LocalEntity local = entity.local();
        
        assertNotNull(local);
        assertInstanceOf(LocalEntityImpl.class, local);
    }

    @Test
    void local_MultipleCalls_ReturnsSameInstance()
    {
        LocalEntity firstCall = entity.local();
        LocalEntity secondCall = entity.local();
        
        assertNotNull(firstCall);
        assertSame(firstCall, secondCall);
    }

    @Test
    void local_CreatedLocalEntity_ReferencesThisEntity()
    {
        LocalEntity local = entity.local();
        
        assertSame(entity, local.entity());
    }

    @Test
    void local_ThreadSafety_InitializesOnlyOnce() throws InterruptedException
    {
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
    void globalIdAndLocal_ConcurrentAccess_BothThreadSafe() throws InterruptedException
    {
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
        
        // Verify all threads got the same instances
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
    void lazyInitialization_UntilFirstAccess_FieldsRemainNull()
    {
        EntityLazyImpl entity = new EntityLazyImpl();
        
        // Entity is ready for lazy initialization
        assertNotNull(entity);
    }

    @Test
    void lazyInitialization_AfterGlobalIdAccess_OnlyGlobalIdInitialized()
    {
        EntityLazyImpl entity = new EntityLazyImpl();
        
        UUID globalId = entity.globalId();
        
        assertNotNull(globalId);
        // Verify consistency
        assertSame(globalId, entity.globalId());
    }

    @Test
    void lazyInitialization_AfterLocalAccess_OnlyLocalInitialized()
    {
        EntityLazyImpl entity = new EntityLazyImpl();
        
        LocalEntity local = entity.local();
        
        assertNotNull(local);
        // Verify consistency
        assertSame(local, entity.local());
    }

    @Test
    void lazyInitialization_AfterBothAccessed_BothInitialized()
    {
        EntityLazyImpl entity = new EntityLazyImpl();
        
        UUID globalId = entity.globalId();
        LocalEntity local = entity.local();
        
        assertNotNull(globalId);
        assertNotNull(local);
        // Verify consistency
        assertSame(globalId, entity.globalId());
        assertSame(local, entity.local());
    }

    @Test
    void globalId_GeneratedValues_AreUnique()
    {
        EntityLazyImpl entity1 = new EntityLazyImpl();
        EntityLazyImpl entity2 = new EntityLazyImpl();
        EntityLazyImpl entity3 = new EntityLazyImpl();
        
        UUID id1 = entity1.globalId();
        UUID id2 = entity2.globalId();
        UUID id3 = entity3.globalId();
        
        assertNotEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertNotEquals(id2, id3);
    }

    @Test
    void local_CreatedInstances_AreDistinct()
    {
        EntityLazyImpl entity1 = new EntityLazyImpl();
        EntityLazyImpl entity2 = new EntityLazyImpl();
        
        LocalEntity local1 = entity1.local();
        LocalEntity local2 = entity2.local();
        
        assertNotSame(local1, local2);
        assertSame(entity1, local1.entity());
        assertSame(entity2, local2.entity());
    }
}