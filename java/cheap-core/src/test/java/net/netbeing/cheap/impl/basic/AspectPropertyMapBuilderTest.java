package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AspectPropertyMapBuilderTest
{
    private AspectPropertyMapBuilder builder;
    private Entity entity;
    private AspectDef aspectDef;
    private PropertyDef stringPropDef;
    private PropertyDef intPropDef;
    private PropertyDef nullablePropDef;
    private Property stringProperty;
    private Property intProperty;
    private UUID entityId;

    @BeforeEach
    void setUp()
    {
        builder = new AspectPropertyMapBuilder();
        entityId = UUID.randomUUID();
        entity = new EntityImpl(entityId);
        aspectDef = new MutableAspectDefImpl("testAspect");

        // Create property definitions
        stringPropDef = new PropertyDefImpl("stringProp", PropertyType.String, true, true, false, true, false);
        intPropDef = new PropertyDefImpl("intProp", PropertyType.Integer, true, true, false, true, false);
        nullablePropDef = new PropertyDefImpl("nullableProp", PropertyType.String, true, true, true, true, false);

        // Add properties to aspect definition
        ((MutableAspectDefImpl) aspectDef).add(stringPropDef);
        ((MutableAspectDefImpl) aspectDef).add(intPropDef);
        ((MutableAspectDefImpl) aspectDef).add(nullablePropDef);

        // Create property instances
        stringProperty = new PropertyImpl(stringPropDef, "test-value");
        intProperty = new PropertyImpl(intPropDef, 42L);
    }

    @Test
    void constructor_CreatesEmptyBuilder()
    {
        AspectPropertyMapBuilder builder = new AspectPropertyMapBuilder();

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
    void property_WithValidNameAndValue_AddsProperty()
    {
        builder.aspectDef(aspectDef);

        AspectBuilder result = builder.property("stringProp", "test-value");

        assertSame(builder, result);
    }

    @Test
    void property_NullPropertyName_ThrowsException()
    {
        builder.aspectDef(aspectDef);

        assertThrows(NullPointerException.class, () -> builder.property(null, "value"));
    }

    @Test
    void property_NoAspectDefSet_ThrowsException()
    {
        assertThrows(IllegalStateException.class, () -> builder.property("stringProp", "value"));
    }

    @Test
    void property_UndefinedPropertyName_ThrowsException()
    {
        builder.aspectDef(aspectDef);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> builder.property("undefinedProp", "value"));

        assertEquals("Property 'undefinedProp' is not defined in AspectDef 'testAspect'", exception.getMessage());
    }

    @Test
    void property_WrongType_ThrowsException()
    {
        builder.aspectDef(aspectDef);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> builder.property("intProp", "not-an-integer"));

        assertEquals("Property 'intProp' expects type Long but got String", exception.getMessage());
    }

    @Test
    void property_NullValueForNonNullableProperty_ThrowsException()
    {
        builder.aspectDef(aspectDef);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> builder.property("stringProp", null));

        assertEquals("Property 'stringProp' does not allow null values", exception.getMessage());
    }

    @Test
    void property_NullValueForNullableProperty_Succeeds()
    {
        builder.aspectDef(aspectDef);

        AspectBuilder result = builder.property("nullableProp", null);

        assertSame(builder, result);
    }

    @Test
    void property_WithPropertyObject_AddsProperty()
    {
        builder.aspectDef(aspectDef);

        AspectBuilder result = builder.property(stringProperty);

        assertSame(builder, result);
    }

    @Test
    void property_NullProperty_ThrowsException()
    {
        builder.aspectDef(aspectDef);

        assertThrows(NullPointerException.class, () -> builder.property((Property) null));
    }

    @Test
    void property_PropertyObjectNoAspectDefSet_Succeeds()
    {
        builder.property(stringProperty);
    }

    @Test
    void property_PropertyObjectUndefinedInAspectDef_ThrowsException()
    {
        AspectDef emptyAspectDef = new ImmutableAspectDefImpl("emptyAspect", Map.of(intPropDef.name(), intPropDef));
        builder.aspectDef(emptyAspectDef);

        assertThrows(IllegalArgumentException.class, () -> builder.property(stringProperty));
    }

    @Test
    void property_PropertyObjectMismatchedDefinition_ThrowsException()
    {
        PropertyDef differentDef = new PropertyDefImpl("stringProp", PropertyType.String, false, true, false, true, false);
        Property propertyWithDifferentDef = new PropertyImpl(differentDef, "value");
        builder.aspectDef(aspectDef);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> builder.property(propertyWithDifferentDef));

        assertEquals("Property definition for 'stringProp' does not match the definition in AspectDef 'testAspect'", exception.getMessage());
    }

    @Test
    void build_WithRequiredFields_CreatesAspectPropertyMapImpl()
    {
        Aspect result = builder
            .entity(entity)
            .aspectDef(aspectDef)
            .property("stringProp", "test-value")
            .property("intProp", 42L)
            .build();

        assertNotNull(result);
        assertInstanceOf(AspectPropertyMapImpl.class, result);
        assertSame(entity, result.entity());
        assertSame(aspectDef, result.def());
        assertEquals("test-value", result.unsafeReadObj("stringProp"));
        assertEquals(42L, result.unsafeReadObj("intProp"));
    }

    @Test
    void build_WithEntity_CreatesAspectPropertyMapImpl()
    {
        Aspect result = builder
            .entity(entity)
            .aspectDef(aspectDef)
            .property("stringProp", "test-value")
            .build();

        assertNotNull(result);
        assertInstanceOf(AspectPropertyMapImpl.class, result);
        assertEquals(entityId, result.entity().globalId());
        assertSame(aspectDef, result.def());
        assertEquals("test-value", result.unsafeReadObj("stringProp"));
    }

    @Test
    void build_WithPropertyObjects_CreatesAspectPropertyMapImpl()
    {
        Aspect result = builder
            .entity(entity)
            .aspectDef(aspectDef)
            .property(stringProperty)
            .property(intProperty)
            .build();

        assertNotNull(result);
        assertInstanceOf(AspectPropertyMapImpl.class, result);
        assertEquals("test-value", result.unsafeReadObj("stringProp"));
        assertEquals(42L, result.unsafeReadObj("intProp"));
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
    void build_WithNullableProperty_CreatesAspectPropertyMapImpl()
    {
        Aspect result = builder
            .entity(entity)
            .aspectDef(aspectDef)
            .property("nullableProp", null)
            .build();

        assertNotNull(result);
        assertNull(result.unsafeReadObj("nullableProp"));
    }

    @Test
    void reset_ClearsAllState()
    {
        builder
            .entity(entity)
            .aspectDef(aspectDef)
            .property("stringProp", "test-value");

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
            .property("stringProp", "value1")
            .build();

        // Reset and build second aspect
        Entity entity2 = new EntityImpl();
        Aspect aspect2 = builder
            .reset()
            .entity(entity2)
            .aspectDef(aspectDef)
            .property("stringProp", "value2")
            .build();

        assertNotSame(aspect1, aspect2);
        assertEquals("value1", aspect1.unsafeReadObj("stringProp"));
        assertEquals("value2", aspect2.unsafeReadObj("stringProp"));
        assertNotSame(aspect1.entity(), aspect2.entity());
    }

    @Test
    void fluentInterface_ChainsMethods()
    {
        Aspect result = builder
            .entity(entity)
            .aspectDef(aspectDef)
            .property("stringProp", "test-value")
            .property(intProperty)
            .build();

        assertNotNull(result);
        assertEquals("test-value", result.unsafeReadObj("stringProp"));
        assertEquals(42L, result.unsafeReadObj("intProp"));
    }

    @Test
    void property_MixedPropertyAndNameValueCalls_AllWork()
    {
        Aspect result = builder
            .entity(entity)
            .aspectDef(aspectDef)
            .property("stringProp", "from-name-value")
            .property(intProperty)
            .property("nullableProp", null)
            .build();

        assertNotNull(result);
        assertEquals("from-name-value", result.unsafeReadObj("stringProp"));
        assertEquals(42L, result.unsafeReadObj("intProp"));
        assertNull(result.unsafeReadObj("nullableProp"));
    }

    @Test
    void property_OverwritePropertyValue_UsesLatestValue()
    {
        Aspect result = builder
            .entity(entity)
            .aspectDef(aspectDef)
            .property("stringProp", "first-value")
            .property("stringProp", "second-value")
            .build();

        assertNotNull(result);
        assertEquals("second-value", result.unsafeReadObj("stringProp"));
    }
}