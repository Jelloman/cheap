package net.netbeing.cheap.util;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the CheapFactory class.
 * Tests factory methods for creating instances of all CHEAP model objects.
 */
class CheapFactoryTest
{
    @Test
    void testCreateCatalog()
    {
        // Test default catalog creation
        Catalog catalog = CheapFactory.createCatalog();
        assertNotNull(catalog);
        assertEquals(CatalogSpecies.SINK, catalog.species());
        assertNull(catalog.upstream());
        assertFalse(catalog.isStrict());
        
        // Test catalog with species and upstream
        Catalog upstreamCatalog = CheapFactory.createCatalog();
        Catalog mirrorCatalog = CheapFactory.createCatalog(CatalogSpecies.MIRROR, upstreamCatalog);
        assertNotNull(mirrorCatalog);
        assertEquals(CatalogSpecies.MIRROR, mirrorCatalog.species());
        assertEquals(upstreamCatalog, mirrorCatalog.upstream());
        
        // Test full catalog configuration
        UUID catalogId = UUID.randomUUID();
        CatalogDef catalogDef = CheapFactory.createCatalogDef();
        Catalog fullCatalog = CheapFactory.createCatalog(catalogId, CatalogSpecies.CACHE, catalogDef, upstreamCatalog, true);
        assertNotNull(fullCatalog);
        assertEquals(catalogId, fullCatalog.globalId());
        assertEquals(CatalogSpecies.CACHE, fullCatalog.species());
        assertEquals(catalogDef, fullCatalog.def());
        assertEquals(upstreamCatalog, fullCatalog.upstream());
        assertTrue(fullCatalog.isStrict());
    }

    @Test
    void testCreateCatalogDef()
    {
        // Test default catalog definition
        CatalogDef catalogDef = CheapFactory.createCatalogDef();
        assertNotNull(catalogDef);
        
        // Test copy constructor
        CatalogDef copiedDef = CheapFactory.createCatalogDef(catalogDef);
        assertNotNull(copiedDef);
        assertNotSame(catalogDef, copiedDef);
    }

    @Test
    void testCreateEntity()
    {
        // Test default entity creation
        Entity entity1 = CheapFactory.createEntity();
        assertNotNull(entity1);
        assertNotNull(entity1.globalId());
        
        Entity entity2 = CheapFactory.createEntity();
        assertNotEquals(entity1.globalId(), entity2.globalId());
        
        // Test entity with specified UUID
        UUID specificId = UUID.randomUUID();
        Entity entityWithId = CheapFactory.createEntity(specificId);
        assertNotNull(entityWithId);
        assertEquals(specificId, entityWithId.globalId());
        
        // Test lazy entity
        Entity lazyEntity = CheapFactory.createLazyEntity();
        assertNotNull(lazyEntity);
        assertNotNull(lazyEntity.globalId());
    }

    @Test
    void testCreateLocalEntity()
    {
        Catalog catalog = CheapFactory.createCatalog();
        
        // Test local entity creation
        LocalEntity localEntity = CheapFactory.createLocalEntity(catalog);
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
        LocalEntity localEntityWithId = CheapFactory.createLocalEntity(entityId, catalog);
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
        LocalEntity multiEntity = CheapFactory.createMultiCatalogEntity(catalog);
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
        LocalEntity cachingEntity = CheapFactory.createCachingEntity(catalog);
        assertNotNull(cachingEntity);
        foundCatalog = false;
        for (Catalog c : cachingEntity.catalogs()) {
            if (c == catalog) {
                foundCatalog = true;
                break;
            }
        }
        assertTrue(foundCatalog);
        
        LocalEntity cachingMultiEntity = CheapFactory.createCachingMultiCatalogEntity(catalog);
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
        // Test hierarchy definition with all parameters
        HierarchyDef hierarchyDef = CheapFactory.createHierarchyDef("testHierarchy", HierarchyType.ENTITY_SET, true);
        assertNotNull(hierarchyDef);
        assertEquals("testHierarchy", hierarchyDef.name());
        assertEquals(HierarchyType.ENTITY_SET, hierarchyDef.type());
        assertTrue(hierarchyDef.isModifiable());
        
        // Test hierarchy definition with default modifiable=true
        HierarchyDef defaultModifiableDef = CheapFactory.createHierarchyDef("testHierarchy2", HierarchyType.ENTITY_LIST);
        assertNotNull(defaultModifiableDef);
        assertEquals("testHierarchy2", defaultModifiableDef.name());
        assertEquals(HierarchyType.ENTITY_LIST, defaultModifiableDef.type());
        assertTrue(defaultModifiableDef.isModifiable());
    }

    @Test
    void testCreateHierarchyDir()
    {
        HierarchyDef def = CheapFactory.createHierarchyDef("testDir", HierarchyType.HIERARCHY_DIR);
        
        // Test basic hierarchy directory
        HierarchyDir hierarchyDir = CheapFactory.createHierarchyDir(def);
        assertNotNull(hierarchyDir);
        assertEquals(def, hierarchyDir.def());
        
        // Test hierarchy directory with performance tuning
        HierarchyDir tunedHierarchyDir = CheapFactory.createHierarchyDir(def, 64, 0.8f);
        assertNotNull(tunedHierarchyDir);
        assertEquals(def, tunedHierarchyDir.def());
    }

    @Test
    void testCreateEntityHierarchies()
    {
        HierarchyDef def = CheapFactory.createHierarchyDef("testEntityHierarchy", HierarchyType.ENTITY_SET);
        
        // Test entity directory hierarchy
        EntityDirectoryHierarchy entityDir = CheapFactory.createEntityDirectoryHierarchy(def);
        assertNotNull(entityDir);
        assertEquals(def, entityDir.def());
        
        // Test entity list hierarchy
        EntityListHierarchy entityList = CheapFactory.createEntityListHierarchy(def);
        assertNotNull(entityList);
        assertEquals(def, entityList.def());
        
        // Test entity set hierarchy
        EntitySetHierarchy entitySet = CheapFactory.createEntitySetHierarchy(def);
        assertNotNull(entitySet);
        assertEquals(def, entitySet.def());
        
        // Test entity tree hierarchy
        Entity rootEntity = CheapFactory.createEntity();
        EntityTreeHierarchy entityTree = CheapFactory.createEntityTreeHierarchy(def, rootEntity);
        assertNotNull(entityTree);
        assertEquals(def, entityTree.def());
        assertEquals(rootEntity, entityTree.root().value());
    }

    @Test
    void testCreateAspectMapHierarchy()
    {
        // Create aspect definition for the hierarchy
        Map<String, PropertyDef> propertyDefs = new HashMap<>();
        propertyDefs.put("name", CheapFactory.createPropertyDef("name", PropertyType.String));
        AspectDef aspectDef = CheapFactory.createMutableAspectDef("testAspect", propertyDefs);
        
        // Test aspect map hierarchy with auto-generated hierarchy def
        AspectMapHierarchy aspectMap = CheapFactory.createAspectMapHierarchy(aspectDef);
        assertNotNull(aspectMap);
        assertEquals(aspectDef, aspectMap.aspectDef());
        
        // Test aspect map hierarchy with custom hierarchy def
        HierarchyDef customDef = CheapFactory.createHierarchyDef("customAspectMap", HierarchyType.ASPECT_MAP);
        AspectMapHierarchy customAspectMap = CheapFactory.createAspectMapHierarchy(customDef, aspectDef);
        assertNotNull(customAspectMap);
        assertEquals(customDef, customAspectMap.def());
        assertEquals(aspectDef, customAspectMap.aspectDef());
    }

    @Test
    void testCreateAspectDef()
    {
        // Test mutable aspect definition
        AspectDef mutableAspectDef = CheapFactory.createMutableAspectDef("testMutableAspect");
        assertNotNull(mutableAspectDef);
        assertEquals("testMutableAspect", mutableAspectDef.name());
        assertTrue(mutableAspectDef.canAddProperties());
        assertTrue(mutableAspectDef.canRemoveProperties());
        
        // Test mutable aspect definition with property definitions
        Map<String, PropertyDef> propertyDefs = new HashMap<>();
        propertyDefs.put("name", CheapFactory.createPropertyDef("name", PropertyType.String));
        propertyDefs.put("age", CheapFactory.createPropertyDef("age", PropertyType.Integer));
        AspectDef mutableWithProps = CheapFactory.createMutableAspectDef("testWithProps", propertyDefs);
        assertNotNull(mutableWithProps);
        assertEquals("testWithProps", mutableWithProps.name());
        assertEquals(2, mutableWithProps.propertyDefs().size());
        assertTrue(mutableWithProps.canAddProperties());
        assertTrue(mutableWithProps.canRemoveProperties());
        
        // Test immutable aspect definition
        AspectDef immutableAspectDef = CheapFactory.createImmutableAspectDef("testImmutableAspect", propertyDefs);
        assertNotNull(immutableAspectDef);
        assertEquals("testImmutableAspect", immutableAspectDef.name());
        assertEquals(2, immutableAspectDef.propertyDefs().size());
        assertFalse(immutableAspectDef.canAddProperties());
        assertFalse(immutableAspectDef.canRemoveProperties());
    }

    @Test
    void testCreateAspectDefDir()
    {
        // Test aspect definition directory
        AspectDefDir aspectDefDir = CheapFactory.createAspectDefDir();
        assertNotNull(aspectDefDir);
        
        // Test aspect definition directory hierarchy
        HierarchyDef def = CheapFactory.createHierarchyDef("aspectDefDir", HierarchyType.ASPECT_DEF_DIR);
        AspectDefDirHierarchy aspectDefDirHierarchy = CheapFactory.createAspectDefDirHierarchy(def);
        assertNotNull(aspectDefDirHierarchy);
        assertEquals(def, aspectDefDirHierarchy.def());
    }

    @Test
    void testCreatePropertyDef()
    {
        // Test property definition with all parameters
        PropertyDef fullPropertyDef = CheapFactory.createPropertyDef(
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
        PropertyDef defaultPropertyDef = CheapFactory.createPropertyDef("defaultProp", PropertyType.Integer);
        assertNotNull(defaultPropertyDef);
        assertEquals("defaultProp", defaultPropertyDef.name());
        assertEquals(PropertyType.Integer, defaultPropertyDef.type());
        
        // Test property definition with specified accessibility
        PropertyDef accessPropertyDef = CheapFactory.createPropertyDef(
            "accessProp", PropertyType.Boolean, true, false, false, true, true);
        assertNotNull(accessPropertyDef);
        assertEquals("accessProp", accessPropertyDef.name());
        assertTrue(accessPropertyDef.isReadable());
        assertFalse(accessPropertyDef.isWritable());
        assertFalse(accessPropertyDef.isNullable());
        assertTrue(accessPropertyDef.isRemovable());
        assertTrue(accessPropertyDef.isMultivalued());
        
        // Test read-only property definition
        PropertyDef readOnlyDef = CheapFactory.createReadOnlyPropertyDef("readOnlyProp", PropertyType.String, true, false);
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
        PropertyDef propertyDef = CheapFactory.createPropertyDef("testProp", PropertyType.String);
        
        // Test property creation with value
        Property property = CheapFactory.createProperty(propertyDef, "test value");
        assertNotNull(property);
        assertEquals(propertyDef, property.def());
        assertEquals("test value", property.unsafeRead());
    }

    @Test
    void testCreateAspects()
    {
        Entity entity = CheapFactory.createEntity();
        Map<String, PropertyDef> propertyDefs = new HashMap<>();
        propertyDefs.put("name", CheapFactory.createPropertyDef("name", PropertyType.String));
        AspectDef aspectDef = CheapFactory.createMutableAspectDef("testAspect", propertyDefs);
        
        // Test object map aspect creation
        Aspect objectMapAspect = CheapFactory.createObjectMapAspect(entity, aspectDef);
        assertNotNull(objectMapAspect);
        assertEquals(entity, objectMapAspect.entity());
        assertEquals(aspectDef, objectMapAspect.def());
        
        // Test object map aspect with initial capacity
        Aspect tunedObjectMapAspect = CheapFactory.createObjectMapAspect(entity, aspectDef, 32);
        assertNotNull(tunedObjectMapAspect);
        assertEquals(entity, tunedObjectMapAspect.entity());
        assertEquals(aspectDef, tunedObjectMapAspect.def());
        
        // Test object map aspect with full performance tuning
        Aspect fullTunedObjectMapAspect = CheapFactory.createObjectMapAspect(entity, aspectDef, 32, 0.9f);
        assertNotNull(fullTunedObjectMapAspect);
        assertEquals(entity, fullTunedObjectMapAspect.entity());
        assertEquals(aspectDef, fullTunedObjectMapAspect.def());
        
        // Test property map aspect creation
        Aspect propertyMapAspect = CheapFactory.createPropertyMapAspect(entity, aspectDef);
        assertNotNull(propertyMapAspect);
        assertEquals(entity, propertyMapAspect.entity());
        assertEquals(aspectDef, propertyMapAspect.def());
        
        // Test property map aspect with initial capacity
        Aspect tunedPropertyMapAspect = CheapFactory.createPropertyMapAspect(entity, aspectDef, 16);
        assertNotNull(tunedPropertyMapAspect);
        assertEquals(entity, tunedPropertyMapAspect.entity());
        assertEquals(aspectDef, tunedPropertyMapAspect.def());
        
        // Test property map aspect with full performance tuning
        Aspect fullTunedPropertyMapAspect = CheapFactory.createPropertyMapAspect(entity, aspectDef, 16, 0.7f);
        assertNotNull(fullTunedPropertyMapAspect);
        assertEquals(entity, fullTunedPropertyMapAspect.entity());
        assertEquals(aspectDef, fullTunedPropertyMapAspect.def());
    }

    @Test
    void testFactoryClassIsUtility()
    {
        // Verify that CheapFactory cannot be instantiated
        assertThrows(Exception.class, () -> {
            CheapFactory.class.getDeclaredConstructor().newInstance();
        });
    }

    @Test
    void testNullValidation()
    {
        // Test that null parameters throw appropriate exceptions
        assertThrows(NullPointerException.class, () -> 
            CheapFactory.createCatalog(null, null));
        
        assertThrows(NullPointerException.class, () -> 
            CheapFactory.createEntity((UUID) null));
        
        assertThrows(NullPointerException.class, () -> 
            CheapFactory.createHierarchyDef(null, HierarchyType.ENTITY_SET));
        
        assertThrows(NullPointerException.class, () -> 
            CheapFactory.createPropertyDef(null, PropertyType.String));
    }
}