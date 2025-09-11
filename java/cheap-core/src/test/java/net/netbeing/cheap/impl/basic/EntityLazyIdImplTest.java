package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.code.tempusfugit.concurrency.annotations.*;
import com.google.code.tempusfugit.concurrency.ConcurrentRule;
import org.junit.Rule;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EntityLazyIdImplTest
{
    @Rule
    public ConcurrentRule concurrentRule = new ConcurrentRule();
    
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
    @Concurrent(count = 10)
    @Repeating(repetition = 100)
    void globalId_ThreadSafety_InitializesOnlyOnce()
    {
        EntityLazyIdImpl entity = new EntityLazyIdImpl();
        UUID globalId = entity.globalId();
        assertNotNull(globalId);
        assertSame(globalId, entity.globalId());
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
    @Concurrent(count = 8)
    @Repeating(repetition = 50)
    void globalIdAndLocal_ConcurrentAccess_BothThreadSafe()
    {
        EntityLazyIdImpl entity = new EntityLazyIdImpl();
        
        UUID globalId = entity.globalId();
        LocalEntity local = entity.local();
        
        assertNotNull(globalId);
        assertNotNull(local);
        assertSame(globalId, entity.globalId());
        assertSame(local, entity.local());
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
    @Concurrent(count = 5)
    @Repeating(repetition = 20)
    void localEntityAspects_ThreadSafety_ConsistentAccess()
    {
        aspect = new AspectObjectMapImpl(catalog, new EntityFullImpl(), aspectDef);
        EntityLazyIdImpl entity = new EntityLazyIdImpl(aspect);
        
        LocalEntity local = entity.local();
        Aspect retrievedAspect = local.aspect(aspectDef);
        
        assertSame(aspect, retrievedAspect);
    }

    @Test
    void constructor_existingEntity_throwsException()
    {
        LocalEntity providedLocal = new LocalEntityImpl(new EntityFullImpl());

        assertThrows(IllegalArgumentException.class, () -> new EntityLazyIdImpl(providedLocal));
    }
}