package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MutableAspectDefImplTest
{
    private PropertyDef propDef1;
    private PropertyDef propDef2;
    private PropertyDef propDef3;
    private Map<String, PropertyDef> propertyDefs;

    @BeforeEach
    void setUp()
    {
        propDef1 = new PropertyDefImpl("prop1", PropertyType.String);
        propDef2 = new PropertyDefImpl("prop2", PropertyType.Integer);
        propDef3 = new PropertyDefImpl("prop3", PropertyType.Boolean);
        
        propertyDefs = new HashMap<>();
        propertyDefs.put("prop1", propDef1);
        propertyDefs.put("prop2", propDef2);
    }

    @Test
    void constructor_WithNameOnly_CreatesEmptyMutableAspectDef()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect");
        
        assertEquals("testAspect", aspectDef.name());
        assertTrue(aspectDef.propertyDefs().isEmpty());
        assertTrue(aspectDef.canAddProperties());
        assertTrue(aspectDef.canRemoveProperties());
    }

    @Test
    void constructor_WithNameAndProperties_CreatesMutableAspectDef()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect", propertyDefs);
        
        assertEquals("testAspect", aspectDef.name());
        assertEquals(2, aspectDef.propertyDefs().size());
        assertSame(propDef1, aspectDef.propertyDef("prop1"));
        assertSame(propDef2, aspectDef.propertyDef("prop2"));
        assertTrue(aspectDef.canAddProperties());
        assertTrue(aspectDef.canRemoveProperties());
    }

    @Test
    void constructor_WithNullName_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> new MutableAspectDefImpl(null));
    }

    @Test
    void constructor_WithNullPropertyMap_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> new MutableAspectDefImpl("testAspect", null));
    }

    @Test
    void add_NewProperty_AddsProperty()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect");
        
        PropertyDef result = aspectDef.add(propDef1);
        
        assertNull(result);
        assertEquals(1, aspectDef.propertyDefs().size());
        assertSame(propDef1, aspectDef.propertyDef("prop1"));
    }

    @Test
    void add_ExistingPropertyName_ReturnsOldProperty()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect", propertyDefs);
        PropertyDef newProp = new PropertyDefImpl("prop1", PropertyType.Boolean);
        
        PropertyDef result = aspectDef.add(newProp);
        
        assertSame(propDef1, result);
        assertEquals(2, aspectDef.propertyDefs().size());
        assertSame(newProp, aspectDef.propertyDef("prop1"));
    }

    @Test
    void add_MultipleProperties_AddsAllProperties()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect");
        
        aspectDef.add(propDef1);
        aspectDef.add(propDef2);
        aspectDef.add(propDef3);
        
        assertEquals(3, aspectDef.propertyDefs().size());
        assertSame(propDef1, aspectDef.propertyDef("prop1"));
        assertSame(propDef2, aspectDef.propertyDef("prop2"));
        assertSame(propDef3, aspectDef.propertyDef("prop3"));
    }

    @Test
    void add_NullProperty_ThrowsException()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect");
        
        assertThrows(NullPointerException.class, () -> aspectDef.add(null));
    }

    @Test
    void remove_ExistingProperty_RemovesProperty()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect", propertyDefs);
        
        PropertyDef result = aspectDef.remove(propDef1);
        
        assertSame(propDef1, result);
        assertEquals(1, aspectDef.propertyDefs().size());
        assertNull(aspectDef.propertyDef("prop1"));
        assertSame(propDef2, aspectDef.propertyDef("prop2"));
    }

    @Test
    void remove_NonExistentProperty_ReturnsNull()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect");
        
        PropertyDef result = aspectDef.remove(propDef1);
        
        assertNull(result);
        assertTrue(aspectDef.propertyDefs().isEmpty());
    }

    @Test
    void remove_PropertyWithSameName_RemovesCorrectProperty()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect", propertyDefs);
        PropertyDef differentPropSameName = new PropertyDefImpl("prop1", PropertyType.Float);
        
        PropertyDef result = aspectDef.remove(differentPropSameName);
        
        assertSame(propDef1, result); // Returns the original property that was removed
        assertEquals(1, aspectDef.propertyDefs().size());
        assertNull(aspectDef.propertyDef("prop1"));
    }

    @Test
    void remove_NullProperty_ThrowsException()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect");
        
        assertThrows(NullPointerException.class, () -> aspectDef.remove(null));
    }

    @Test
    void canAddProperties_Always_ReturnsTrue()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect");
        
        assertTrue(aspectDef.canAddProperties());
        
        // Should still return true after adding properties
        aspectDef.add(propDef1);
        assertTrue(aspectDef.canAddProperties());
    }

    @Test
    void canRemoveProperties_Always_ReturnsTrue()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect", propertyDefs);
        
        assertTrue(aspectDef.canRemoveProperties());
        
        // Should still return true after removing properties
        aspectDef.remove(propDef1);
        assertTrue(aspectDef.canRemoveProperties());
        
        // Should still return true even when empty
        aspectDef.remove(propDef2);
        assertTrue(aspectDef.canRemoveProperties());
    }

    @Test
    void name_AfterConstruction_ReturnsProvidedName()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("myAspect");
        
        assertEquals("myAspect", aspectDef.name());
    }

    @Test
    void propertyDefs_IsMutable_CanAddPropertiesViaAdd()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect");
        Collection<? extends PropertyDef> returnedCollection = aspectDef.propertyDefs();
        
        // Should be able to add properties via the add method
        assertDoesNotThrow(() -> aspectDef.add(propDef3));
        
        assertEquals(1, aspectDef.propertyDefs().size());
        assertSame(propDef3, aspectDef.propertyDef("prop3"));
    }

    @Test
    void propertyDefs_MultipleCallsSameInstance_ReturnsSameMap()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect");
        
        Collection<? extends PropertyDef> first = aspectDef.propertyDefs();
        Collection<? extends PropertyDef> second = aspectDef.propertyDefs();
        
        assertSame(first, second);
    }

    @Test
    void addAndRemove_ChainedOperations_WorksCorrectly()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect");
        
        // Add properties
        assertNull(aspectDef.add(propDef1));
        assertNull(aspectDef.add(propDef2));
        assertEquals(2, aspectDef.propertyDefs().size());
        
        // Remove one property
        assertSame(propDef1, aspectDef.remove(propDef1));
        assertEquals(1, aspectDef.propertyDefs().size());
        
        // Add it back
        assertNull(aspectDef.add(propDef1));
        assertEquals(2, aspectDef.propertyDefs().size());
        
        // Remove all
        assertSame(propDef1, aspectDef.remove(propDef1));
        assertSame(propDef2, aspectDef.remove(propDef2));
        assertTrue(aspectDef.propertyDefs().isEmpty());
    }

    @Test
    void constructor_WithSpecialCharactersInName_HandlesCorrectly()
    {
        String specialName = "aspect-with_special.chars@123";
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl(specialName);
        
        assertEquals(specialName, aspectDef.name());
    }

    @Test
    void constructor_WithEmptyName_HandlesCorrectly()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("");
        
        assertEquals("", aspectDef.name());
    }

    @Test
    void constructor_WithWhitespaceName_HandlesCorrectly()
    {
        String whitespaceName = "  aspect with spaces  ";
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl(whitespaceName);
        
        assertEquals(whitespaceName, aspectDef.name());
    }

    @Test
    void constructor_WithInitialProperties_DoesNotRetainReference()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect", propertyDefs);
        
        // Modify original map
        propertyDefs.put("prop3", propDef3);
        
        // AspectDef should be affected (since it uses the same map)
        assertEquals(3, aspectDef.propertyDefs().size());
        assertSame(propDef3, aspectDef.propertyDef("prop3"));
    }

    @Test
    void add_PropertyWithSpecialName_HandlesCorrectly()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect");
        PropertyDef specialProp = new PropertyDefImpl("_special_", PropertyType.String); // Special but valid name
        
        PropertyDef result = aspectDef.add(specialProp);
        
        assertNull(result);
        assertEquals(1, aspectDef.propertyDefs().size());
        assertSame(specialProp, aspectDef.propertyDef("_special_"));
    }

    @Test
    void remove_PropertyWithSpecialName_HandlesCorrectly()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("testAspect");
        PropertyDef specialProp = new PropertyDefImpl("_special_", PropertyType.String); // Special but valid name
        aspectDef.add(specialProp);
        
        PropertyDef result = aspectDef.remove(specialProp);
        
        assertSame(specialProp, result);
        assertTrue(aspectDef.propertyDefs().isEmpty());
    }

    @Test
    void addAndRemove_LargeNumberOfProperties_HandlesCorrectly()
    {
        MutableAspectDefImpl aspectDef = new MutableAspectDefImpl("largeAspect");
        
        // Add many properties
        PropertyDef[] props = new PropertyDef[1000];
        for (int i = 0; i < 1000; i++) {
            props[i] = new PropertyDefImpl("prop" + i, PropertyType.String);
            aspectDef.add(props[i]);
        }
        
        assertEquals(1000, aspectDef.propertyDefs().size());
        
        // Remove half of them
        for (int i = 0; i < 500; i++) {
            assertSame(props[i], aspectDef.remove(props[i]));
        }
        
        assertEquals(500, aspectDef.propertyDefs().size());
        
        // Verify remaining properties
        for (int i = 500; i < 1000; i++) {
            assertSame(props[i], aspectDef.propertyDef("prop" + i));
        }
    }
}