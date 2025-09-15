package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AspectObjectMapImplTest
{
    private Catalog catalog;
    private Entity entity;
    private AspectDef aspectDef;
    private PropertyDef propDef1;
    private PropertyDef propDef2;
    private Property property1;
    private Property property2;
    private AspectObjectMapImpl aspect;

    @BeforeEach
    void setUp()
    {
        catalog = new CatalogImpl();
        entity = new EntityImpl();
        aspectDef = new MutableAspectDefImpl("testAspect");
        propDef1 = new PropertyDefImpl("prop1", PropertyType.String);
        propDef2 = new PropertyDefImpl("prop2", PropertyType.Integer);
        property1 = new PropertyImpl(propDef1, "test-value");
        property2 = new PropertyImpl(propDef2, 42);
        aspect = new AspectObjectMapImpl(entity, aspectDef);
    }

    @Test
    void constructor_DefaultCapacity_CreatesEmptyAspect()
    {
        AspectObjectMapImpl aspect = new AspectObjectMapImpl(entity, aspectDef);
        
        assertSame(entity, aspect.entity());
        assertSame(aspectDef, aspect.def());
        assertNotNull(aspect.props);
        assertTrue(aspect.props.isEmpty());
    }

    @Test
    void constructor_WithInitialCapacity_CreatesEmptyAspect()
    {
        AspectObjectMapImpl aspect = new AspectObjectMapImpl(entity, aspectDef, 10);
        
        assertSame(entity, aspect.entity());
        assertSame(aspectDef, aspect.def());
        assertNotNull(aspect.props);
        assertTrue(aspect.props.isEmpty());
    }

    @Test
    void constructor_WithCapacityAndLoadFactor_CreatesEmptyAspect()
    {
        AspectObjectMapImpl aspect = new AspectObjectMapImpl(entity, aspectDef, 10, 0.8f);
        
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
        aspect.unsafeAdd(property1);
        
        assertTrue(aspect.contains("prop1"));
    }

    @Test
    void unsafeReadObj_PropertyNotPresent_ReturnsNull()
    {
        assertNull(aspect.unsafeReadObj("nonexistent"));
    }

    @Test
    void unsafeReadObj_PropertyPresent_ReturnsPropertyObject()
    {
        aspect.unsafeAdd(property1);
        
        Object result = aspect.unsafeReadObj("prop1");
        assertSame(property1, result);
    }

    @Test
    void unsafeAdd_NewProperty_AddsProperty()
    {
        aspect.unsafeAdd(property1);
        
        assertTrue(aspect.contains("prop1"));
        assertSame(property1, aspect.unsafeReadObj("prop1"));
    }

    @Test
    void unsafeAdd_MultipleProperties_AddsAllProperties()
    {
        aspect.unsafeAdd(property1);
        aspect.unsafeAdd(property2);
        
        assertTrue(aspect.contains("prop1"));
        assertTrue(aspect.contains("prop2"));
        assertSame(property1, aspect.unsafeReadObj("prop1"));
        assertSame(property2, aspect.unsafeReadObj("prop2"));
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
        MutableAspectDefImpl mutableDef = (MutableAspectDefImpl) aspectDef;
        mutableDef.add(propDef1);
        aspect.props.put("prop1", "old-value");
        
        aspect.unsafeWrite("prop1", "new-value");
        
        Object result = aspect.unsafeReadObj("prop1");
        assertInstanceOf(Property.class, result);
        Property prop = (Property) result;
        assertEquals("new-value", prop.unsafeRead());
    }

    @Test
    void unsafeWrite_AspectDefMissingProperty_ThrowsException()
    {
        aspect.props.put("prop1", property1);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aspect.unsafeWrite("prop1", "new-value")
        );
        
        assertTrue(exception.getMessage().contains("does not contain prop named 'prop1'"));
    }

    @Test
    void unsafeRemove_PropertyPresent_RemovesProperty()
    {
        aspect.unsafeAdd(property1);
        assertTrue(aspect.contains("prop1"));
        
        aspect.unsafeRemove("prop1");
        
        assertFalse(aspect.contains("prop1"));
        assertNull(aspect.unsafeReadObj("prop1"));
    }

    @Test
    void unsafeRemove_PropertyNotPresent_DoesNothing()
    {
        assertDoesNotThrow(() -> aspect.unsafeRemove("nonexistent"));
    }
}