package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectBuilder;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.Property;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AspectObjectMapBuilderTest
{
    private AspectObjectMapBuilder builder;
    private Entity entity;
    private AspectDef aspectDef;
    private PropertyDef propDef1;
    private PropertyDef propDef2;
    private Property property1;
    private Property property2;
    private UUID entityId;

    @BeforeEach
    void setUp()
    {
        builder = new AspectObjectMapBuilder();
        entityId = UUID.randomUUID();
        entity = new EntityImpl(entityId);
        aspectDef = new MutableAspectDefImpl("testAspect");
        propDef1 = new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build();
        propDef2 = new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build();
        property1 = new PropertyImpl(propDef1, "test-value");
        property2 = new PropertyImpl(propDef2, 42);

        ((MutableAspectDefImpl) aspectDef).add(propDef1);
        ((MutableAspectDefImpl) aspectDef).add(propDef2);
    }

    @Test
    void constructor_CreatesEmptyBuilder()
    {
        AspectObjectMapBuilder builder = new AspectObjectMapBuilder();

        assertNotNull(builder);
    }

    @Test
    void entity_SetsEntity()
    {
        AspectBuilder result = builder.entity(entity);

        assertSame(builder, result);
    }

    @Test
    void entity_NullEntity_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> builder.entity(null));
    }

    @Test
    void aspectDef_SetsAspectDef()
    {
        AspectBuilder result = builder.aspectDef(aspectDef);

        assertSame(builder, result);
    }

    @Test
    void aspectDef_NullAspectDef_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> builder.aspectDef(null));
    }

    @Test
    void property_WithNameAndValue_AddsProperty()
    {
        AspectBuilder result = builder.property("testProp", "testValue");

        assertSame(builder, result);
    }

    @Test
    void property_NullPropertyName_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> builder.property(null, "value"));
    }

    @Test
    void property_WithPropertyObject_AddsProperty()
    {
        AspectBuilder result = builder.property(property1);

        assertSame(builder, result);
    }

    @Test
    void property_NullProperty_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> builder.property((Property) null));
    }

    @Test
    void build_WithRequiredFields_CreatesAspectObjectMapImpl()
    {
        Aspect result = builder
            .entity(entity)
            .aspectDef(aspectDef)
            .property("prop1", "test-value")
            .property("prop2", 42)
            .build();

        assertNotNull(result);
        assertInstanceOf(AspectObjectMapImpl.class, result);
        assertSame(entity, result.entity());
        assertSame(aspectDef, result.def());
        assertEquals("test-value", result.readObj("prop1"));
        assertEquals(42, result.readObj("prop2"));
    }

    @Test
    void build_WithPropertyObject_CreatesAspectObjectMapImpl()
    {
        Aspect result = builder
            .entity(entity)
            .aspectDef(aspectDef)
            .property(property1)
            .property(property2)
            .build();

        assertNotNull(result);
        assertInstanceOf(AspectObjectMapImpl.class, result);
        assertEquals("test-value", result.readObj("prop1"));
        assertEquals(42, result.readObj("prop2"));
    }

    @Test
    void build_NoEntity_ThrowsException()
    {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            builder.aspectDef(aspectDef).build());

        assertEquals("Entity must be set before building aspect", exception.getMessage());
    }

    @Test
    void build_NoAspectDef_ThrowsException()
    {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            builder.entity(entity).build());

        assertEquals("AspectDef must be set before building aspect", exception.getMessage());
    }

    @Test
    void reset_ClearsAllState()
    {
        builder
            .entity(entity)
            .aspectDef(aspectDef)
            .property("prop1", "test-value");

        AspectBuilder result = builder.reset();

        assertSame(builder, result);

        // Should throw exception since state is cleared
        assertThrows(IllegalStateException.class, () -> builder.build());
    }

    @Test
    void reset_AllowsReuse()
    {
        // Build first aspect
        Aspect aspect1 = builder
            .entity(entity)
            .aspectDef(aspectDef)
            .property("prop1", "value1")
            .build();

        // Reset and build second aspect
        Entity entity2 = new EntityImpl();
        Aspect aspect2 = builder
            .reset()
            .entity(entity2)
            .aspectDef(aspectDef)
            .property("prop1", "value2")
            .build();

        assertNotSame(aspect1, aspect2);
        assertEquals("value1", aspect1.readObj("prop1"));
        assertEquals("value2", aspect2.readObj("prop1"));
        assertNotSame(aspect1.entity(), aspect2.entity());
    }

    @Test
    void fluentInterface_ChainsMethods()
    {
        Aspect result = builder
            .entity(entity)
            .aspectDef(aspectDef)
            .property("prop1", "test-value")
            .property(property2)
            .build();

        assertNotNull(result);
        assertEquals("test-value", result.readObj("prop1"));
        assertEquals(42, result.readObj("prop2"));
    }
}