package net.netbeing.cheap.impl.basic;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ImmutableAspectDefImplTest
{
    private PropertyDef propDef1;
    private PropertyDef propDef2;
    private PropertyDef propDef3;
    private Map<String, PropertyDef> propertyDefs;

    @BeforeEach
    void setUp()
    {
        propDef1 = new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build();
        propDef2 = new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build();
        propDef3 = new PropertyDefBuilder().setName("prop3").setType(PropertyType.Boolean).build();
        
        propertyDefs = new LinkedHashMap<>();
        propertyDefs.put("prop1", propDef1);
        propertyDefs.put("prop2", propDef2);
    }

    @Test
    void constructor_ValidNameAndProperties_CreatesImmutableAspectDef()
    {
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);
        
        assertEquals("testAspect", aspectDef.name());
        assertEquals(2, aspectDef.propertyDefs().size());
        assertSame(propDef1, aspectDef.propertyDef("prop1"));
        assertSame(propDef2, aspectDef.propertyDef("prop2"));
    }

    @Test
    void constructor_WithNullName_ThrowsNullPointerException()
    {
        assertThrows(NullPointerException.class, () -> new ImmutableAspectDefImpl(null, propertyDefs));
    }

    @Test
    void constructor_WithNullPropertyMap_ThrowsNullPointerException()
    {
        assertThrows(NullPointerException.class, () -> new ImmutableAspectDefImpl("testAspect", null));
    }

    @Test
    void constructor_CopiesPropertyMap_DoesNotRetainReference()
    {
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);
        
        // Modify original map
        propertyDefs.put("prop3", propDef3);
        
        // AspectDef should not be affected
        assertEquals(2, aspectDef.propertyDefs().size());
        assertNull(aspectDef.propertyDef("prop3"));
    }

    @Test
    void propertyDefs_ReturnsImmutableCollection_CannotBeModified()
    {
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);
        Collection<? extends PropertyDef> returnedCollection = aspectDef.propertyDefs();
        
        // The returned collection should be immutable - verify its size and contents
        assertEquals(2, returnedCollection.size());
        assertTrue(returnedCollection.contains(propDef1));
        assertTrue(returnedCollection.contains(propDef2));
        
        // Test immutability via casting to Collection<PropertyDef> for the operations
        @SuppressWarnings("unchecked")
        Collection<PropertyDef> mutableRef = (Collection<PropertyDef>) returnedCollection;
        assertThrows(UnsupportedOperationException.class, () -> mutableRef.add(propDef3));
        assertThrows(UnsupportedOperationException.class, () -> mutableRef.remove(propDef1));
        assertThrows(UnsupportedOperationException.class, () -> mutableRef.clear());
    }

    @Test
    void add_AnyProperty_ThrowsUnsupportedOperationException()
    {
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> aspectDef.add(propDef3)
        );
        
        assertTrue(exception.getMessage().contains("Properties cannot be added to immutable AspectDef 'testAspect'"));
    }

    @Test
    void add_NullProperty_ThrowsUnsupportedOperationException()
    {
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);
        
        assertThrows(UnsupportedOperationException.class, () -> aspectDef.add(null));
    }

    @Test
    void remove_ExistingProperty_ThrowsUnsupportedOperationException()
    {
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> aspectDef.remove(propDef1)
        );
        
        assertTrue(exception.getMessage().contains("Properties cannot be removed from immutable AspectDef 'testAspect'"));
    }

    @Test
    void remove_NonExistentProperty_ThrowsUnsupportedOperationException()
    {
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);
        
        assertThrows(UnsupportedOperationException.class, () -> aspectDef.remove(propDef3));
    }

    @Test
    void remove_NullProperty_ThrowsUnsupportedOperationException()
    {
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);
        
        assertThrows(UnsupportedOperationException.class, () -> aspectDef.remove(null));
    }

    @Test
    void canAddProperties_Always_ReturnsFalse()
    {
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);
        
        assertFalse(aspectDef.canAddProperties());
    }

    @Test
    void canRemoveProperties_Always_ReturnsFalse()
    {
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);
        
        assertFalse(aspectDef.canRemoveProperties());
    }

    @Test
    void name_AfterConstruction_ReturnsProvidedName()
    {
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("myAspect", propertyDefs);
        
        assertEquals("myAspect", aspectDef.name());
    }

    @Test
    void propertyDefs_MultipleCallsSameInstance_ReturnsSameMap()
    {
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);
        
        Collection<? extends PropertyDef> first = aspectDef.propertyDefs();
        Collection<? extends PropertyDef> second = aspectDef.propertyDefs();
        
        assertSame(first, second);
    }

    @Test
    void constructor_WithSpecialCharactersInName_HandlesCorrectly()
    {
        String specialName = "aspect-with_special.chars@123";
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl(specialName, propertyDefs);
        
        assertEquals(specialName, aspectDef.name());
    }

    @Test
    void constructor_WithEmptyName_HandlesCorrectly()
    {
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("", propertyDefs);
        
        assertEquals("", aspectDef.name());
    }

    @Test
    void constructor_WithWhitespaceName_HandlesCorrectly()
    {
        String whitespaceName = "  aspect with spaces  ";
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl(whitespaceName, propertyDefs);
        
        assertEquals(whitespaceName, aspectDef.name());
    }

    @Test
    void propertyDefs_WithDuplicatePropertyNames_LastOneWins()
    {
        Map<String, PropertyDef> duplicateMap = new LinkedHashMap<>();
        PropertyDef originalProp = new PropertyDefBuilder().setName("sameName").setType(PropertyType.String).build();
        PropertyDef duplicateProp = new PropertyDefBuilder().setName("sameName").setType(PropertyType.Integer).build();
        
        duplicateMap.put("sameName", originalProp);
        duplicateMap.put("sameName", duplicateProp); // This will overwrite
        
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", duplicateMap);
        
        assertSame(duplicateProp, aspectDef.propertyDef("sameName"));
        assertEquals(PropertyType.Integer, aspectDef.propertyDef("sameName").type());
    }

    @Test
    void constructor_LargePropertyMap_HandlesCorrectly()
    {
        Map<String, PropertyDef> largeMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            PropertyDef prop = new PropertyDefBuilder().setName("prop" + i).setType(PropertyType.String).build();
            largeMap.put("prop" + i, prop);
        }
        
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("largeAspect", largeMap);
        
        assertEquals("largeAspect", aspectDef.name());
        assertEquals(1000, aspectDef.propertyDefs().size());
        
        // Verify a few random properties
        assertNotNull(aspectDef.propertyDef("prop0"));
        assertNotNull(aspectDef.propertyDef("prop500"));
        assertNotNull(aspectDef.propertyDef("prop999"));
    }

    @Test
    void error_messages_ContainAspectName()
    {
        String aspectName = "mySpecialAspect";
        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl(aspectName, propertyDefs);
        
        try {
            aspectDef.add(propDef3);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains(aspectName));
        }
        
        try {
            aspectDef.remove(propDef1);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains(aspectName));
        }
    }


    @Test
    void fullyEquals_IdenticalInstancesConstructedSeparately_ReturnsTrue()
    {
        UUID globalId = UUID.randomUUID();
        Map<String, PropertyDef> propertyDefs1 = ImmutableMap.of(
            "prop1", new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build(),
            "prop2", new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build()
        );
        Map<String, PropertyDef> propertyDefs2 = ImmutableMap.of(
            "prop1", new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build(),
            "prop2", new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build()
        );

        ImmutableAspectDefImpl aspectDef1 = new ImmutableAspectDefImpl("testAspect", globalId, propertyDefs1);
        ImmutableAspectDefImpl aspectDef2 = new ImmutableAspectDefImpl("testAspect", globalId, propertyDefs2);

        assertTrue(aspectDef1.fullyEquals(aspectDef2));
        assertTrue(aspectDef2.fullyEquals(aspectDef1));
    }

    @Test
    void fullyEquals_SameInstance_ReturnsTrue()
    {
        Map<String, PropertyDef> propertyDefs = ImmutableMap.of(
            "prop1", new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build(),
            "prop2", new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build()
        );

        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);

        assertTrue(aspectDef.fullyEquals(aspectDef));
    }

    @Test
    void fullyEquals_DifferentNames_ReturnsFalse()
    {
        Map<String, PropertyDef> propertyDefs = ImmutableMap.of(
            "prop1", new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build(),
            "prop2", new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build()
        );

        ImmutableAspectDefImpl aspectDef1 = new ImmutableAspectDefImpl("testAspect1", propertyDefs);
        ImmutableAspectDefImpl aspectDef2 = new ImmutableAspectDefImpl("testAspect2", propertyDefs);

        assertFalse(aspectDef1.fullyEquals(aspectDef2));
    }

    @Test
    void hash_IdenticalInstancesConstructedSeparately_ReturnsSameHash()
    {
        UUID globalId = UUID.randomUUID();
        Map<String, PropertyDef> propertyDefs1 = ImmutableMap.of(
            "prop1", new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build(),
            "prop2", new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build()
        );
        Map<String, PropertyDef> propertyDefs2 = ImmutableMap.of(
            "prop1", new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build(),
            "prop2", new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build()
        );

        ImmutableAspectDefImpl aspectDef1 = new ImmutableAspectDefImpl("testAspect", globalId, propertyDefs1);
        ImmutableAspectDefImpl aspectDef2 = new ImmutableAspectDefImpl("testAspect", globalId, propertyDefs2);

        assertEquals(aspectDef1.hash(), aspectDef2.hash());
    }

    @Test
    void hash_SameInstance_ReturnsSameHash()
    {
        Map<String, PropertyDef> propertyDefs = ImmutableMap.of(
            "prop1", new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build(),
            "prop2", new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build()
        );

        ImmutableAspectDefImpl aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);

        assertEquals(aspectDef.hash(), aspectDef.hash());
    }

    @Test
    void hash_DifferentPropertyOrder_ReturnsDifferentHash()
    {
        UUID globalId = UUID.randomUUID();
        Map<String, PropertyDef> propertyDefs1 = new LinkedHashMap<>();
        propertyDefs1.put("prop1", new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build());
        propertyDefs1.put("prop2", new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build());

        Map<String, PropertyDef> propertyDefs2 = new LinkedHashMap<>();
        propertyDefs2.put("prop2", new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build());
        propertyDefs2.put("prop1", new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build());

        ImmutableAspectDefImpl aspectDef1 = new ImmutableAspectDefImpl("testAspect", globalId, propertyDefs1);
        ImmutableAspectDefImpl aspectDef2 = new ImmutableAspectDefImpl("testAspect", globalId, propertyDefs2);

        // Property order affects hash value, so hashes should be different
        assertNotEquals(aspectDef1.hash(), aspectDef2.hash());
    }
}