package net.netbeing.cheap.util;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the CheapFactory class.
 * Tests factory methods for creating instances of all Cheap model objects.
 */
class CheapFactoryTest
{
    private final CheapFactory factory = new CheapFactory();

    @Test
    void testCreateCatalog()
    {
        // Test default catalog creation
        Catalog catalog = factory.createCatalog();
        assertNotNull(catalog);
        assertEquals(CatalogSpecies.SINK, catalog.species());
        assertNull(catalog.upstream());

        
        // Test catalog with species and upstream
        UUID upstreamCatalog = UUID.randomUUID();
        Catalog mirrorCatalog = factory.createCatalog(CatalogSpecies.MIRROR, upstreamCatalog);
        assertNotNull(mirrorCatalog);
        assertEquals(CatalogSpecies.MIRROR, mirrorCatalog.species());
        assertEquals(upstreamCatalog, mirrorCatalog.upstream());
        
        // Test full catalog configuration
        UUID catalogId = UUID.randomUUID();
        CatalogDef catalogDef = factory.createCatalogDef();
        Catalog fullCatalog = factory.createCatalog(catalogId, CatalogSpecies.CACHE, upstreamCatalog);
        assertNotNull(fullCatalog);
        assertEquals(catalogId, fullCatalog.globalId());
        assertEquals(CatalogSpecies.CACHE, fullCatalog.species());
        assertEquals(upstreamCatalog, fullCatalog.upstream());

    }

    @Test
    void testCreateCatalogDef()
    {
        // Test default catalog definition
        CatalogDef catalogDef = factory.createCatalogDef();
        assertNotNull(catalogDef);
        
        // Test copy constructor
        CatalogDef copiedDef = factory.createCatalogDef(catalogDef);
        assertNotNull(copiedDef);
        assertNotSame(catalogDef, copiedDef);
    }

    @Test
    void testCreateEntity()
    {
        // Test default entity creation
        Entity entity1 = factory.createEntity();
        assertNotNull(entity1);
        assertNotNull(entity1.globalId());
        
        Entity entity2 = factory.createEntity();
        assertNotEquals(entity1.globalId(), entity2.globalId());
        
        // Test entity with specified UUID
        UUID specificId = UUID.randomUUID();
        Entity entityWithId = factory.createEntity(specificId);
        assertNotNull(entityWithId);
        assertEquals(specificId, entityWithId.globalId());
        
        // Test lazy entity
        Entity lazyEntity = factory.createLazyEntity();
        assertNotNull(lazyEntity);
        assertNotNull(lazyEntity.globalId());
    }

    @Test
    void testCreateLocalEntity()
    {
        Catalog catalog = factory.createCatalog();
        
        // Test local entity creation
        LocalEntity localEntity = factory.createLocalEntity(catalog);
        assertNotNull(localEntity);
        // Check if catalog is in the iterable
        boolean foundCatalog = false;
        for (Catalog c : localEntity.catalogs()) {
            if (c == catalog) {
                foundCatalog = true;
                break;
            }
        }
        assertTrue(foundCatalog);
        
        // Test local entity with specific ID
        UUID entityId = UUID.randomUUID();
        LocalEntity localEntityWithId = factory.createLocalEntity(entityId, catalog);
        assertNotNull(localEntityWithId);
        assertEquals(entityId, localEntityWithId.globalId());
        foundCatalog = false;
        for (Catalog c : localEntityWithId.catalogs()) {
            if (c == catalog) {
                foundCatalog = true;
                break;
            }
        }
        assertTrue(foundCatalog);
        
        // Test multi-catalog entity
        LocalEntity multiEntity = factory.createMultiCatalogEntity(catalog);
        assertNotNull(multiEntity);
        foundCatalog = false;
        for (Catalog c : multiEntity.catalogs()) {
            if (c == catalog) {
                foundCatalog = true;
                break;
            }
        }
        assertTrue(foundCatalog);
        
        // Test caching entities
        LocalEntity cachingEntity = factory.createCachingEntity(catalog);
        assertNotNull(cachingEntity);
        foundCatalog = false;
        for (Catalog c : cachingEntity.catalogs()) {
            if (c == catalog) {
                foundCatalog = true;
                break;
            }
        }
        assertTrue(foundCatalog);
        
        LocalEntity cachingMultiEntity = factory.createCachingMultiCatalogEntity(catalog);
        assertNotNull(cachingMultiEntity);
        foundCatalog = false;
        for (Catalog c : cachingMultiEntity.catalogs()) {
            if (c == catalog) {
                foundCatalog = true;
                break;
            }
        }
        assertTrue(foundCatalog);
    }

    @Test
    void testCreateHierarchyDef()
    {
        // Test hierarchy definition
        HierarchyDef hierarchyDef = factory.createHierarchyDef("testHierarchy", HierarchyType.ENTITY_SET);
        assertNotNull(hierarchyDef);
        assertEquals("testHierarchy", hierarchyDef.name());
        assertEquals(HierarchyType.ENTITY_SET, hierarchyDef.type());


        // Test hierarchy definition with different type
        HierarchyDef defaultModifiableDef = factory.createHierarchyDef("testHierarchy2", HierarchyType.ENTITY_LIST);
        assertNotNull(defaultModifiableDef);
        assertEquals("testHierarchy2", defaultModifiableDef.name());
        assertEquals(HierarchyType.ENTITY_LIST, defaultModifiableDef.type());

    }

    @Test
    void testCreateEntityHierarchies()
    {
        Catalog catalog = factory.createCatalog();

        // Test entity directory hierarchy
        EntityDirectoryHierarchy entityDir = factory.createEntityDirectoryHierarchy(catalog, "testEntityDir");
        assertNotNull(entityDir);
        assertEquals("testEntityDir", entityDir.name());
        assertEquals(HierarchyType.ENTITY_DIR, entityDir.type());

        // Test entity list hierarchy
        EntityListHierarchy entityList = factory.createEntityListHierarchy(catalog, "testEntityList");
        assertNotNull(entityList);
        assertEquals("testEntityList", entityList.name());
        assertEquals(HierarchyType.ENTITY_LIST, entityList.type());

        // Test entity set hierarchy
        EntitySetHierarchy entitySet = factory.createEntitySetHierarchy(catalog, "testEntitySet");
        assertNotNull(entitySet);
        assertEquals("testEntitySet", entitySet.name());
        assertEquals(HierarchyType.ENTITY_SET, entitySet.type());

        // Test entity tree hierarchy
        Entity rootEntity = factory.createEntity();
        EntityTreeHierarchy entityTree = factory.createEntityTreeHierarchy(catalog, "testEntityTree", rootEntity);
        assertNotNull(entityTree);
        assertEquals("testEntityTree", entityTree.name());
        assertEquals(HierarchyType.ENTITY_TREE, entityTree.type());
        assertEquals(rootEntity, entityTree.root().value());
    }

    @Test
    void testCreateAspectMapHierarchy()
    {
        Catalog catalog = factory.createCatalog();

        // Create aspect definition for the hierarchy
        Map<String, PropertyDef> propertyDefs = new LinkedHashMap<>();
        propertyDefs.put("name", factory.createPropertyDef("name", PropertyType.String));
        AspectDef aspectDef = factory.createMutableAspectDef("testAspect", propertyDefs);

        // Test aspect map hierarchy with auto-generated hierarchy def
        AspectMapHierarchy aspectMap = factory.createAspectMapHierarchy(catalog, aspectDef);
        assertNotNull(aspectMap);
        assertEquals(aspectDef.name(), aspectMap.name());
        assertEquals(HierarchyType.ASPECT_MAP, aspectMap.type());
        assertEquals(aspectDef, aspectMap.aspectDef());
    }

    @Test
    void testCreateAspectDef()
    {
        // Test mutable aspect definition
        AspectDef mutableAspectDef = factory.createMutableAspectDef("testMutableAspect");
        assertNotNull(mutableAspectDef);
        assertEquals("testMutableAspect", mutableAspectDef.name());
        assertTrue(mutableAspectDef.canAddProperties());
        assertTrue(mutableAspectDef.canRemoveProperties());
        
        // Test mutable aspect definition with property definitions
        Map<String, PropertyDef> propertyDefs = new LinkedHashMap<>();
        propertyDefs.put("name", factory.createPropertyDef("name", PropertyType.String));
        propertyDefs.put("age", factory.createPropertyDef("age", PropertyType.Integer));
        AspectDef mutableWithProps = factory.createMutableAspectDef("testWithProps", propertyDefs);
        assertNotNull(mutableWithProps);
        assertEquals("testWithProps", mutableWithProps.name());
        assertEquals(2, mutableWithProps.propertyDefs().size());
        assertTrue(mutableWithProps.canAddProperties());
        assertTrue(mutableWithProps.canRemoveProperties());
        
        // Test immutable aspect definition
        AspectDef immutableAspectDef = factory.createImmutableAspectDef("testImmutableAspect", propertyDefs);
        assertNotNull(immutableAspectDef);
        assertEquals("testImmutableAspect", immutableAspectDef.name());
        assertEquals(2, immutableAspectDef.propertyDefs().size());
        assertFalse(immutableAspectDef.canAddProperties());
        assertFalse(immutableAspectDef.canRemoveProperties());
    }

    @Test
    void testCreatePropertyDef()
    {
        // Test property definition with all parameters
        PropertyDef fullPropertyDef = factory.createPropertyDef(
            "testProp", PropertyType.String, "default", true, true, true, true, true, false);
        assertNotNull(fullPropertyDef);
        assertEquals("testProp", fullPropertyDef.name());
        assertEquals(PropertyType.String, fullPropertyDef.type());
        assertEquals("default", fullPropertyDef.defaultValue());
        assertTrue(fullPropertyDef.hasDefaultValue());
        assertTrue(fullPropertyDef.isReadable());
        assertTrue(fullPropertyDef.isWritable());
        assertTrue(fullPropertyDef.isNullable());
        assertTrue(fullPropertyDef.isRemovable());
        assertFalse(fullPropertyDef.isMultivalued());
        
        // Test property definition with default settings
        PropertyDef defaultPropertyDef = factory.createPropertyDef("defaultProp", PropertyType.Integer);
        assertNotNull(defaultPropertyDef);
        assertEquals("defaultProp", defaultPropertyDef.name());
        assertEquals(PropertyType.Integer, defaultPropertyDef.type());
        
        // Test property definition with specified accessibility
        PropertyDef accessPropertyDef = factory.createPropertyDef(
            "accessProp", PropertyType.Boolean, true, false, false, true, true);
        assertNotNull(accessPropertyDef);
        assertEquals("accessProp", accessPropertyDef.name());
        assertTrue(accessPropertyDef.isReadable());
        assertFalse(accessPropertyDef.isWritable());
        assertFalse(accessPropertyDef.isNullable());
        assertTrue(accessPropertyDef.isRemovable());
        assertTrue(accessPropertyDef.isMultivalued());
        
        // Test read-only property definition
        PropertyDef readOnlyDef = factory.createReadOnlyPropertyDef("readOnlyProp", PropertyType.String, true, false);
        assertNotNull(readOnlyDef);
        assertEquals("readOnlyProp", readOnlyDef.name());
        assertTrue(readOnlyDef.isReadable());
        assertFalse(readOnlyDef.isWritable());
        assertTrue(readOnlyDef.isNullable());
        assertFalse(readOnlyDef.isRemovable());
    }

    @Test
    void testCreateProperty()
    {
        PropertyDef propertyDef = factory.createPropertyDef("testProp", PropertyType.String);
        
        // Test property creation with value
        Property property = factory.createProperty(propertyDef, "test value");
        assertNotNull(property);
        assertEquals(propertyDef, property.def());
        assertEquals("test value", property.read());
    }

    @Test
    void testCreateAspects()
    {
        Entity entity = factory.createEntity();
        Map<String, PropertyDef> propertyDefs = new LinkedHashMap<>();
        propertyDefs.put("name", factory.createPropertyDef("name", PropertyType.String));
        AspectDef aspectDef = factory.createMutableAspectDef("testAspect", propertyDefs);
        
        // Test object map aspect creation
        Aspect objectMapAspect = factory.createObjectMapAspect(entity, aspectDef);
        assertNotNull(objectMapAspect);
        assertEquals(entity, objectMapAspect.entity());
        assertEquals(aspectDef, objectMapAspect.def());
        
        // Test object map aspect with initial capacity
        Aspect tunedObjectMapAspect = factory.createObjectMapAspect(entity, aspectDef, 32);
        assertNotNull(tunedObjectMapAspect);
        assertEquals(entity, tunedObjectMapAspect.entity());
        assertEquals(aspectDef, tunedObjectMapAspect.def());
        
        // Test object map aspect with full performance tuning
        Aspect fullTunedObjectMapAspect = factory.createObjectMapAspect(entity, aspectDef, 32, 0.9f);
        assertNotNull(fullTunedObjectMapAspect);
        assertEquals(entity, fullTunedObjectMapAspect.entity());
        assertEquals(aspectDef, fullTunedObjectMapAspect.def());
        
        // Test property map aspect creation
        Aspect propertyMapAspect = factory.createPropertyMapAspect(entity, aspectDef);
        assertNotNull(propertyMapAspect);
        assertEquals(entity, propertyMapAspect.entity());
        assertEquals(aspectDef, propertyMapAspect.def());
        
        // Test property map aspect with initial capacity
        Aspect tunedPropertyMapAspect = factory.createPropertyMapAspect(entity, aspectDef, 16);
        assertNotNull(tunedPropertyMapAspect);
        assertEquals(entity, tunedPropertyMapAspect.entity());
        assertEquals(aspectDef, tunedPropertyMapAspect.def());
        
        // Test property map aspect with full performance tuning
        Aspect fullTunedPropertyMapAspect = factory.createPropertyMapAspect(entity, aspectDef, 16, 0.7f);
        assertNotNull(fullTunedPropertyMapAspect);
        assertEquals(entity, fullTunedPropertyMapAspect.entity());
        assertEquals(aspectDef, fullTunedPropertyMapAspect.def());
    }

    @Test
    void testFactoryConfiguration()
    {
        // Test default factory configuration
        assertEquals(LocalEntityType.SINGLE_CATALOG, factory.getDefaultLocalEntityType());
        
        // Test factory with custom configuration
        CheapFactory customFactory = new CheapFactory(LocalEntityType.MULTI_CATALOG, null);
        assertEquals(LocalEntityType.MULTI_CATALOG, customFactory.getDefaultLocalEntityType());

        // Test null configuration (both parameters null should use defaults)
        CheapFactory nullFactory = new CheapFactory(null, null);
        assertEquals(LocalEntityType.SINGLE_CATALOG, nullFactory.getDefaultLocalEntityType());
    }

    @Test
    void testLocalEntityTypeBasedCreation()
    {
        Catalog catalog = factory.createCatalog();
        
        // Test default factory creates SINGLE_CATALOG type
        LocalEntity defaultEntity = factory.createLocalEntity(catalog);
        assertNotNull(defaultEntity);
        // Verify it's the correct implementation type by checking behavior
        assertTrue(defaultEntity instanceof net.netbeing.cheap.impl.basic.LocalEntityOneCatalogImpl);
        
        // Test different LocalEntityType configurations
        CheapFactory singleFactory = new CheapFactory(LocalEntityType.SINGLE_CATALOG, null);
        LocalEntity singleEntity = singleFactory.createLocalEntity(catalog);
        assertNotNull(singleEntity);
        assertTrue(singleEntity instanceof net.netbeing.cheap.impl.basic.LocalEntityOneCatalogImpl);

        CheapFactory multiFactory = new CheapFactory(LocalEntityType.MULTI_CATALOG, null);
        LocalEntity multiEntity = multiFactory.createLocalEntity(catalog);
        assertNotNull(multiEntity);
        assertTrue(multiEntity instanceof net.netbeing.cheap.impl.basic.LocalEntityMultiCatalogImpl);

        CheapFactory cachingSingleFactory = new CheapFactory(LocalEntityType.CACHING_SINGLE_CATALOG, null);
        LocalEntity cachingSingleEntity = cachingSingleFactory.createLocalEntity(catalog);
        assertNotNull(cachingSingleEntity);
        assertTrue(cachingSingleEntity instanceof net.netbeing.cheap.impl.basic.CachingEntityOneCatalogImpl);

        CheapFactory cachingMultiFactory = new CheapFactory(LocalEntityType.CACHING_MULTI_CATALOG, null);
        LocalEntity cachingMultiEntity = cachingMultiFactory.createLocalEntity(catalog);
        assertNotNull(cachingMultiEntity);
        assertTrue(cachingMultiEntity instanceof net.netbeing.cheap.impl.basic.CachingEntityMultiCatalogImpl);
    }

    @Test
    void testLocalEntityWithSpecificType()
    {
        Catalog catalog = factory.createCatalog();
        
        // Test creating entities with specific types
        LocalEntity singleEntity = factory.createLocalEntity(LocalEntityType.SINGLE_CATALOG, catalog);
        assertNotNull(singleEntity);
        assertTrue(singleEntity instanceof net.netbeing.cheap.impl.basic.LocalEntityOneCatalogImpl);
        
        LocalEntity multiEntity = factory.createLocalEntity(LocalEntityType.MULTI_CATALOG, catalog);
        assertNotNull(multiEntity);
        assertTrue(multiEntity instanceof net.netbeing.cheap.impl.basic.LocalEntityMultiCatalogImpl);
        
        // Test with globalId for SINGLE_CATALOG (should work)
        UUID entityId = UUID.randomUUID();
        LocalEntity singleWithId = factory.createLocalEntity(LocalEntityType.SINGLE_CATALOG, entityId, catalog);
        assertNotNull(singleWithId);
        assertEquals(entityId, singleWithId.globalId());
        
        // Test with globalId for MULTI_CATALOG (should work now)
        LocalEntity multiWithId = factory.createLocalEntity(LocalEntityType.MULTI_CATALOG, entityId, catalog);
        assertNotNull(multiWithId);
        assertEquals(entityId, multiWithId.globalId());
        assertTrue(multiWithId instanceof net.netbeing.cheap.impl.basic.LocalEntityMultiCatalogImpl);
        
        // Test with globalId for CACHING types (should work now)
        UUID cachingSingleId = UUID.randomUUID();
        LocalEntity cachingSingleWithId = factory.createLocalEntity(LocalEntityType.CACHING_SINGLE_CATALOG, cachingSingleId, catalog);
        assertNotNull(cachingSingleWithId);
        assertEquals(cachingSingleId, cachingSingleWithId.globalId());
        assertTrue(cachingSingleWithId instanceof net.netbeing.cheap.impl.basic.CachingEntityOneCatalogImpl);
        
        UUID cachingMultiId = UUID.randomUUID();
        LocalEntity cachingMultiWithId = factory.createLocalEntity(LocalEntityType.CACHING_MULTI_CATALOG, cachingMultiId, catalog);
        assertNotNull(cachingMultiWithId);
        assertEquals(cachingMultiId, cachingMultiWithId.globalId());
        assertTrue(cachingMultiWithId instanceof net.netbeing.cheap.impl.basic.CachingEntityMultiCatalogImpl);
    }

    @Test
    void testExplicitLocalEntityCreationMethods()
    {
        Catalog catalog = factory.createCatalog();
        UUID entityId = UUID.randomUUID();
        
        // Test explicit single catalog entity creation
        LocalEntity singleEntity = factory.createSingleCatalogEntity(catalog);
        assertNotNull(singleEntity);
        assertTrue(singleEntity instanceof net.netbeing.cheap.impl.basic.LocalEntityOneCatalogImpl);
        
        LocalEntity singleEntityWithId = factory.createSingleCatalogEntity(entityId, catalog);
        assertNotNull(singleEntityWithId);
        assertEquals(entityId, singleEntityWithId.globalId());
        assertTrue(singleEntityWithId instanceof net.netbeing.cheap.impl.basic.LocalEntityOneCatalogImpl);
        
        // Test explicit multi-catalog entity creation
        LocalEntity multiEntity = factory.createMultiCatalogEntity(catalog);
        assertNotNull(multiEntity);
        assertTrue(multiEntity instanceof net.netbeing.cheap.impl.basic.LocalEntityMultiCatalogImpl);
        
        UUID multiEntityId = UUID.randomUUID();
        LocalEntity multiEntityWithId = factory.createMultiCatalogEntity(multiEntityId, catalog);
        assertNotNull(multiEntityWithId);
        assertEquals(multiEntityId, multiEntityWithId.globalId());
        assertTrue(multiEntityWithId instanceof net.netbeing.cheap.impl.basic.LocalEntityMultiCatalogImpl);
        
        // Test explicit caching entity creation
        LocalEntity cachingEntity = factory.createCachingEntity(catalog);
        assertNotNull(cachingEntity);
        assertTrue(cachingEntity instanceof net.netbeing.cheap.impl.basic.CachingEntityOneCatalogImpl);
        
        UUID cachingEntityId = UUID.randomUUID();
        LocalEntity cachingEntityWithId = factory.createCachingEntity(cachingEntityId, catalog);
        assertNotNull(cachingEntityWithId);
        assertEquals(cachingEntityId, cachingEntityWithId.globalId());
        assertTrue(cachingEntityWithId instanceof net.netbeing.cheap.impl.basic.CachingEntityOneCatalogImpl);
        
        LocalEntity cachingMultiEntity = factory.createCachingMultiCatalogEntity(catalog);
        assertNotNull(cachingMultiEntity);
        assertTrue(cachingMultiEntity instanceof net.netbeing.cheap.impl.basic.CachingEntityMultiCatalogImpl);
        
        UUID cachingMultiEntityId = UUID.randomUUID();
        LocalEntity cachingMultiEntityWithId = factory.createCachingMultiCatalogEntity(cachingMultiEntityId, catalog);
        assertNotNull(cachingMultiEntityWithId);
        assertEquals(cachingMultiEntityId, cachingMultiEntityWithId.globalId());
        assertTrue(cachingMultiEntityWithId instanceof net.netbeing.cheap.impl.basic.CachingEntityMultiCatalogImpl);
    }

    @Test
    void testAllLocalEntityTypesWithUUID()
    {
        Catalog catalog = factory.createCatalog();
        
        // Test all LocalEntityType enum values with both random and specified UUIDs
        for (LocalEntityType type : LocalEntityType.values()) {
            // Test with random UUID (null)
            LocalEntity randomEntity = factory.createLocalEntity(type, null, catalog);
            assertNotNull(randomEntity, "Failed to create " + type + " with random UUID");
            assertNotNull(randomEntity.globalId(), type + " should have a global ID");
            
            // Test with specific UUID
            UUID specificId = UUID.randomUUID();
            LocalEntity specificEntity = factory.createLocalEntity(type, specificId, catalog);
            assertNotNull(specificEntity, "Failed to create " + type + " with specific UUID");
            assertEquals(specificId, specificEntity.globalId(), type + " should use specified UUID");
            
            // Verify different instances have different UUIDs when random
            assertNotEquals(randomEntity.globalId(), specificEntity.globalId(), 
                type + " instances should have different UUIDs");
            
            // Verify correct implementation type
            switch (type) {
                case SINGLE_CATALOG -> {
                    assertTrue(randomEntity instanceof net.netbeing.cheap.impl.basic.LocalEntityOneCatalogImpl);
                    assertTrue(specificEntity instanceof net.netbeing.cheap.impl.basic.LocalEntityOneCatalogImpl);
                }
                case MULTI_CATALOG -> {
                    assertTrue(randomEntity instanceof net.netbeing.cheap.impl.basic.LocalEntityMultiCatalogImpl);
                    assertTrue(specificEntity instanceof net.netbeing.cheap.impl.basic.LocalEntityMultiCatalogImpl);
                }
                case CACHING_SINGLE_CATALOG -> {
                    assertTrue(randomEntity instanceof net.netbeing.cheap.impl.basic.CachingEntityOneCatalogImpl);
                    assertTrue(specificEntity instanceof net.netbeing.cheap.impl.basic.CachingEntityOneCatalogImpl);
                }
                case CACHING_MULTI_CATALOG -> {
                    assertTrue(randomEntity instanceof net.netbeing.cheap.impl.basic.CachingEntityMultiCatalogImpl);
                    assertTrue(specificEntity instanceof net.netbeing.cheap.impl.basic.CachingEntityMultiCatalogImpl);
                }
            }
            
            // Verify catalog association
            boolean foundCatalog = false;
            for (Catalog c : randomEntity.catalogs()) {
                if (c == catalog) {
                    foundCatalog = true;
                    break;
                }
            }
            assertTrue(foundCatalog, type + " should be associated with the catalog");
        }
    }

    @Test
    void testNullValidation()
    {
        // Test that null parameters throw appropriate exceptions
        assertThrows(NullPointerException.class, () -> 
            factory.createCatalog(null, null));
        
        assertThrows(NullPointerException.class, () -> 
            factory.createEntity((UUID) null));
        
        assertThrows(NullPointerException.class, () -> 
            factory.createHierarchyDef(null, HierarchyType.ENTITY_SET));
        
        assertThrows(NullPointerException.class, () -> 
            factory.createPropertyDef(null, PropertyType.String));
    }
}