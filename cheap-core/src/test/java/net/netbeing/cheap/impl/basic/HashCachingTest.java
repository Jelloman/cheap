package net.netbeing.cheap.impl.basic;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that hash caching is working correctly for PropertyDef and AspectDef implementations.
 */
class HashCachingTest
{
    @Test
    void propertyDefImpl_CachesHashValue_MultipleCallsReturnSameValue()
    {
        PropertyDefImpl propDef = new PropertyDefImpl("testProp", PropertyType.String);

        long hash1 = propDef.hash();
        long hash2 = propDef.hash();
        long hash3 = propDef.hash();

        assertEquals(hash1, hash2, "Second hash call should return cached value");
        assertEquals(hash1, hash3, "Third hash call should return cached value");
        assertNotEquals(0, hash1, "Hash value should not be zero");
    }

    @Test
    void immutableAspectDefImpl_CachesHashValue_MultipleCallsReturnSameValue()
    {
        Map<String, PropertyDef> propertyDefs = ImmutableMap.of(
            "prop1", new PropertyDefImpl("prop1", PropertyType.String),
            "prop2", new PropertyDefImpl("prop2", PropertyType.Integer)
        );

        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);

        long hash1 = aspectDef.hash();
        long hash2 = aspectDef.hash();
        long hash3 = aspectDef.hash();

        assertEquals(hash1, hash2, "Second hash call should return cached value");
        assertEquals(hash1, hash3, "Third hash call should return cached value");
        assertNotEquals(0, hash1, "Hash value should not be zero");
    }

    @Test
    void mutableAspectDefImpl_InvalidatesCacheOnAdd()
    {
        Map<String, PropertyDef> propertyDefs = new LinkedHashMap<>();
        propertyDefs.put("prop1", new PropertyDefImpl("prop1", PropertyType.String));

        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect", propertyDefs);

        long hashBeforeAdd = aspectDef.hash();

        // Add a property - should invalidate cache
        aspectDef.add(new PropertyDefImpl("prop2", PropertyType.Integer));

        long hashAfterAdd = aspectDef.hash();

        assertNotEquals(hashBeforeAdd, hashAfterAdd,
            "Hash should change after adding a property");
        assertNotEquals(0, hashBeforeAdd, "Initial hash should not be zero");
        assertNotEquals(0, hashAfterAdd, "Hash after add should not be zero");
    }

    @Test
    void mutableAspectDefImpl_InvalidatesCacheOnRemove()
    {
        Map<String, PropertyDef> propertyDefs = new LinkedHashMap<>();
        PropertyDef prop1 = new PropertyDefImpl("prop1", PropertyType.String);
        PropertyDef prop2 = new PropertyDefImpl("prop2", PropertyType.Integer);
        propertyDefs.put("prop1", prop1);
        propertyDefs.put("prop2", prop2);

        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect", propertyDefs);

        long hashBeforeRemove = aspectDef.hash();

        // Remove a property - should invalidate cache
        aspectDef.remove(prop2);

        long hashAfterRemove = aspectDef.hash();

        assertNotEquals(hashBeforeRemove, hashAfterRemove,
            "Hash should change after removing a property");
        assertNotEquals(0, hashBeforeRemove, "Initial hash should not be zero");
        assertNotEquals(0, hashAfterRemove, "Hash after remove should not be zero");
    }

    @Test
    void fullAspectDefImpl_InvalidatesCacheOnAdd_WhenAllowed()
    {
        Map<String, PropertyDef> propertyDefs = new LinkedHashMap<>();
        propertyDefs.put("prop1", new PropertyDefImpl("prop1", PropertyType.String));

        FullAspectDefImpl aspectDef = new FullAspectDefImpl(
            "testAspect",
            java.util.UUID.randomUUID(),
            propertyDefs,
            true,  // isReadable
            true,  // isWritable
            true,  // canAddProperties
            false  // canRemoveProperties
        );

        long hashBeforeAdd = aspectDef.hash();

        // Add a property - should invalidate cache
        aspectDef.add(new PropertyDefImpl("prop2", PropertyType.Integer));

        long hashAfterAdd = aspectDef.hash();

        assertNotEquals(hashBeforeAdd, hashAfterAdd,
            "Hash should change after adding a property");
        assertNotEquals(0, hashBeforeAdd, "Initial hash should not be zero");
        assertNotEquals(0, hashAfterAdd, "Hash after add should not be zero");
    }

    @Test
    void fullAspectDefImpl_InvalidatesCacheOnRemove_WhenAllowed()
    {
        Map<String, PropertyDef> propertyDefs = new LinkedHashMap<>();
        PropertyDef prop1 = new PropertyDefImpl("prop1", PropertyType.String);
        PropertyDef prop2 = new PropertyDefImpl("prop2", PropertyType.Integer);
        propertyDefs.put("prop1", prop1);
        propertyDefs.put("prop2", prop2);

        FullAspectDefImpl aspectDef = new FullAspectDefImpl(
            "testAspect",
            java.util.UUID.randomUUID(),
            propertyDefs,
            true,  // isReadable
            true,  // isWritable
            false, // canAddProperties
            true   // canRemoveProperties
        );

        long hashBeforeRemove = aspectDef.hash();

        // Remove a property - should invalidate cache
        aspectDef.remove(prop2);

        long hashAfterRemove = aspectDef.hash();

        assertNotEquals(hashBeforeRemove, hashAfterRemove,
            "Hash should change after removing a property");
        assertNotEquals(0, hashBeforeRemove, "Initial hash should not be zero");
        assertNotEquals(0, hashAfterRemove, "Hash after remove should not be zero");
    }

    @Test
    void mutableAspectDefImpl_CachesHashValue_AfterMultipleModifications()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect");

        // Add properties
        aspectDef.add(new PropertyDefImpl("prop1", PropertyType.String));
        aspectDef.add(new PropertyDefImpl("prop2", PropertyType.Integer));

        // Get hash (should be computed and cached)
        long hash1 = aspectDef.hash();
        long hash2 = aspectDef.hash();
        long hash3 = aspectDef.hash();

        // All should be the same (cached)
        assertEquals(hash1, hash2, "Hash should be cached after computation");
        assertEquals(hash1, hash3, "Hash should remain cached");

        // Modify again
        aspectDef.add(new PropertyDefImpl("prop3", PropertyType.Boolean));

        long hash4 = aspectDef.hash();
        long hash5 = aspectDef.hash();

        // Hash should have changed
        assertNotEquals(hash1, hash4, "Hash should change after modification");
        // But new hash should be cached
        assertEquals(hash4, hash5, "New hash should be cached");
    }
}
