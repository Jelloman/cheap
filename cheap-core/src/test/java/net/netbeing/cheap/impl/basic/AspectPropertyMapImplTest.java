package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.Property;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AspectPropertyMapImplTest
{
    private Catalog catalog;
    private Entity entity;
    private MutableAspectDefImpl aspectDef;
    private PropertyDef propDef1;
    private PropertyDef propDef2;
    private Property property1;
    private Property property2;
    private AspectPropertyMapImpl aspect;

    @BeforeEach
    void setUp()
    {
        catalog = new CatalogImpl();
        entity = new EntityImpl();
        aspectDef = new MutableAspectDefImpl("testAspect");
        propDef1 = new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build();
        propDef2 = new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build();
        property1 = new PropertyImpl(propDef1, "test-value");
        property2 = new PropertyImpl(propDef2, 42);
        aspect = new AspectPropertyMapImpl(entity, aspectDef);
    }

    @Test
    void constructor_DefaultCapacity_CreatesEmptyAspect()
    {
        AspectPropertyMapImpl aspect = new AspectPropertyMapImpl(entity, aspectDef);
        
        assertSame(entity, aspect.entity());
        assertSame(aspectDef, aspect.def());
        assertNotNull(aspect.props);
        assertTrue(aspect.props.isEmpty());
    }

    @Test
    void constructor_WithInitialCapacity_CreatesEmptyAspect()
    {
        AspectPropertyMapImpl aspect = new AspectPropertyMapImpl(entity, aspectDef, 10);
        
        assertSame(entity, aspect.entity());
        assertSame(aspectDef, aspect.def());
        assertNotNull(aspect.props);
        assertTrue(aspect.props.isEmpty());
    }

    @Test
    void constructor_WithCapacityAndLoadFactor_CreatesEmptyAspect()
    {
        AspectPropertyMapImpl aspect = new AspectPropertyMapImpl(entity, aspectDef, 10, 0.8f);
        
        assertSame(entity, aspect.entity());
        assertSame(aspectDef, aspect.def());
        assertNotNull(aspect.props);
        assertTrue(aspect.props.isEmpty());
    }

    @Test
    void contains_PropertyNotPresent_ReturnsFalse()
    {
        assertFalse(aspect.contains("nonexistent"));
    }

    @Test
    void contains_PropertyPresent_ReturnsTrue()
    {
        aspect.put(property1);
        
        assertTrue(aspect.contains("prop1"));
    }

    @Test
    void unsafeReadObj_PropertyNotPresent_ReturnsNull()
    {
        assertNull(aspect.unsafeReadObj("nonexistent"));
    }

    @Test
    void unsafeReadObj_PropertyPresent_ReturnsPropertyValue()
    {
        aspect.put(property1);
        
        Object result = aspect.unsafeReadObj("prop1");
        assertEquals("test-value", result);
    }

    @Test
    void get_AspectNotReadable_ThrowsException()
    {
        // Create a non-readable aspect def
        aspectDef = new MutableAspectDefImpl("testAspect") {
            @Override
            public boolean isReadable() { return false; }
        };
        aspect = new AspectPropertyMapImpl(entity, aspectDef);
        aspect.put(property1);
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> aspect.get("prop1")
        );
        
        assertTrue(exception.getMessage().contains("is not readable"));
    }

    @Test
    void get_PropertyNotPresent_ThrowsException()
    {
        // Create a readable aspect def
        aspectDef = new MutableAspectDefImpl("testAspect");
        aspect = new AspectPropertyMapImpl(entity, aspectDef);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aspect.get("nonexistent")
        );
        
        assertTrue(exception.getMessage().contains("does not contain prop named"));
    }

    @Test
    void get_PropertyNotReadable_ThrowsException()
    {
        // Create a readable aspect def
        aspectDef = new MutableAspectDefImpl("testAspect");
        aspect = new AspectPropertyMapImpl(entity, aspectDef);
        PropertyDefImpl nonReadablePropDef = new PropertyDefBuilder().setName("readonly").setType(PropertyType.String).setIsReadable(false).setIsWritable(true).setIsNullable(true).setIsRemovable(true).setIsMultivalued(false).build();
        Property nonReadableProperty = new PropertyImpl(nonReadablePropDef, "value");
        aspect.put(nonReadableProperty);
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> aspect.get("readonly")
        );
        
        assertTrue(exception.getMessage().contains("is not readable"));
    }

    @Test
    void get_ValidProperty_ReturnsProperty()
    {
        // Create a readable aspect def
        aspectDef = new MutableAspectDefImpl("testAspect");
        aspect = new AspectPropertyMapImpl(entity, aspectDef);
        PropertyDefImpl readablePropDef = new PropertyDefBuilder().setName("readable").setType(PropertyType.String).setIsReadable(true).setIsWritable(true).setIsNullable(true).setIsRemovable(true).setIsMultivalued(false).build();
        Property readableProperty = new PropertyImpl(readablePropDef, "value");
        aspect.put(readableProperty);
        
        Property result = aspect.get("readable");
        
        assertSame(readableProperty, result);
    }

    @Test
    void put_AspectNotWritable_ThrowsException()
    {
        // Create a non-writable aspect def
        aspectDef = new MutableAspectDefImpl("testAspect") {
            @Override
            public boolean isWritable() { return false; }
        };
        aspect = new AspectPropertyMapImpl(entity, aspectDef);
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> aspect.put(property1)
        );
        
        assertTrue(exception.getMessage().contains("is not writable"));
    }

    @Test
    void put_NewPropertyAndNotExtensible_ThrowsException()
    {
        // Create a writable but non-extensible aspect def
        aspectDef = new MutableAspectDefImpl("testAspect") {
            @Override
            public boolean canAddProperties() { return false; }
        };
        aspect = new AspectPropertyMapImpl(entity, aspectDef);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aspect.put(property1)
        );
        
        assertTrue(exception.getMessage().contains("is not extensible"));
    }

    @Test
    void put_NewPropertyAndExtensible_AddsProperty()
    {
        // Create a writable and extensible aspect def
        aspectDef = new MutableAspectDefImpl("testAspect");
        aspect = new AspectPropertyMapImpl(entity, aspectDef);
        
        aspect.put(property1);
        
        assertTrue(aspect.contains("prop1"));
        assertSame(property1, aspect.props.get("prop1"));
    }

    @Test
    void put_ExistingPropertyNotWritable_ThrowsException()
    {
        // Create a writable aspect def
        aspectDef = new MutableAspectDefImpl("testAspect");
        aspect = new AspectPropertyMapImpl(entity, aspectDef);
        PropertyDefImpl nonWritablePropDef = new PropertyDefBuilder().setName("readonly").setType(PropertyType.String).setIsReadable(true).setIsWritable(false).setIsNullable(true).setIsRemovable(true).setIsMultivalued(false).build();
        Property existingProperty = new PropertyImpl(nonWritablePropDef, "old-value");
        aspect.props.put("readonly", existingProperty);
        
        Property newProperty = new PropertyImpl(nonWritablePropDef, "new-value");
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> aspect.put(newProperty)
        );
        
        assertTrue(exception.getMessage().contains("is marked not writable"));
    }

    @Test
    void put_ExistingPropertyDifferentDef_ThrowsException()
    {
        // Create a writable aspect def
        aspectDef = new MutableAspectDefImpl("testAspect");
        aspect = new AspectPropertyMapImpl(entity, aspectDef);
        PropertyDefImpl propDef1 = new PropertyDefBuilder().setName("prop").setType(PropertyType.String).setIsReadable(true).setIsWritable(true).setIsNullable(true).setIsRemovable(true).setIsMultivalued(false).build();
        aspectDef.add(propDef1);

        PropertyDefImpl propDef2 = new PropertyDefBuilder().setName("prop").setType(PropertyType.Integer).setIsReadable(true).setIsWritable(true).setIsNullable(true).setIsRemovable(true).setIsMultivalued(false).build();
        
        Property existingProperty = new PropertyImpl(propDef1, "value");
        aspect.props.put("prop", existingProperty);
        
        Property newProperty = new PropertyImpl(propDef2, 42);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aspect.put(newProperty)
        );
        
        assertTrue(exception.getMessage().contains("conflicts with the existing definition"));
    }

    @Test
    void put_ExistingPropertySameDef_UpdatesProperty()
    {
        // Create a writable aspect def
        aspectDef = new MutableAspectDefImpl("testAspect");
        aspect = new AspectPropertyMapImpl(entity, aspectDef);
        PropertyDefImpl writablePropDef = new PropertyDefBuilder().setName("writable").setType(PropertyType.String).setIsReadable(true).setIsWritable(true).setIsNullable(true).setIsRemovable(true).setIsMultivalued(false).build();
        
        Property existingProperty = new PropertyImpl(writablePropDef, "old-value");
        aspect.props.put("writable", existingProperty);
        
        Property newProperty = new PropertyImpl(writablePropDef, "new-value");
        aspect.put(newProperty);
        
        assertSame(newProperty, aspect.props.get("writable"));
    }

    @Test
    void unsafeAdd_NewProperty_AddsProperty()
    {
        aspect.unsafeAdd(property1);
        
        assertTrue(aspect.contains("prop1"));
        assertSame(property1, aspect.props.get("prop1"));
    }

    @Test
    void unsafeAdd_MultipleProperties_AddsAllProperties()
    {
        aspect.unsafeAdd(property1);
        aspect.unsafeAdd(property2);
        
        assertTrue(aspect.contains("prop1"));
        assertTrue(aspect.contains("prop2"));
        assertSame(property1, aspect.props.get("prop1"));
        assertSame(property2, aspect.props.get("prop2"));
    }

    @Test
    void unsafeWrite_PropertyNotPresent_ThrowsException()
    {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aspect.unsafeWrite("nonexistent", "value")
        );
        
        assertTrue(exception.getMessage().contains("does not contain prop named 'nonexistent'"));
    }

    @Test
    void unsafeWrite_PropertyPresent_UpdatesProperty()
    {
        aspect.props.put("prop1", property1);
        
        aspect.unsafeWrite("prop1", "new-value");
        
        Property result = aspect.props.get("prop1");
        assertNotSame(property1, result);
        assertEquals("new-value", result.unsafeRead());
        assertSame(propDef1, result.def());
    }

    @Test
    void unsafeRemove_PropertyPresent_RemovesProperty()
    {
        aspect.put(property1);
        assertTrue(aspect.contains("prop1"));
        
        aspect.unsafeRemove("prop1");
        
        assertFalse(aspect.contains("prop1"));
        assertNull(aspect.props.get("prop1"));
    }

    @Test
    void unsafeRemove_PropertyNotPresent_DoesNothing()
    {
        assertDoesNotThrow(() -> aspect.unsafeRemove("nonexistent"));
    }
}