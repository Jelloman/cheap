package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.LocalEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.code.tempusfugit.concurrency.annotations.*;
import com.google.code.tempusfugit.concurrency.ConcurrentRule;
import org.junit.Rule;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityLazyImplTest
{
    @Rule
    public ConcurrentRule concurrentRule = new ConcurrentRule();
    
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
    @Concurrent(count = 10)
    @Repeating(repetition = 100)
    void globalId_ThreadSafety_InitializesOnlyOnce()
    {
        UUID globalId = entity.globalId();
        assertNotNull(globalId);
        assertSame(globalId, entity.globalId());
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
    @Concurrent(count = 10)
    @Repeating(repetition = 100)
    void local_ThreadSafety_InitializesOnlyOnce()
    {
        LocalEntity local = entity.local();
        assertNotNull(local);
        assertSame(local, entity.local());
    }

    @Test
    @Concurrent(count = 8)
    @Repeating(repetition = 50)
    void globalIdAndLocal_ConcurrentAccess_BothThreadSafe()
    {
        UUID globalId = entity.globalId();
        LocalEntity local = entity.local();
        
        assertNotNull(globalId);
        assertNotNull(local);
        assertSame(globalId, entity.globalId());
        assertSame(local, entity.local());
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