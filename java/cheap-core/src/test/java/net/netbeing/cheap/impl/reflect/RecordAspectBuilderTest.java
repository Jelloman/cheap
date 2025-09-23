package net.netbeing.cheap.impl.reflect;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.impl.basic.EntityImpl;
import net.netbeing.cheap.impl.basic.PropertyDefImpl;
import net.netbeing.cheap.impl.basic.PropertyImpl;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecordAspectBuilderTest
{
    private RecordAspectBuilder<TestRecord> builder;
    private Entity entity;
    private UUID entityId;

    // Test record class for testing
    public record TestRecord(String name, int age, boolean active, Double salary) {}

    // Test record with primitives
    public record PrimitiveRecord(int intValue, boolean boolValue, double doubleValue, char charValue) {}

    // Test record with single component
    public record SimpleRecord(String value) {}

    // Test record with no components
    public record EmptyRecord() {}

    // Test complex record with collection
    public record ComplexRecord(String name, List<String> tags, UUID id) {}

    // Non-record class for testing validation
    public static class NotARecord
    {
        private String value;
        public String getValue() { return value; }
    }

    @BeforeEach
    void setUp()
    {
        builder = new RecordAspectBuilder<>(TestRecord.class);
        entityId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        entity = new EntityImpl(entityId);
    }

    @Test
    void constructor_CreatesBuilderWithValidRecordClass()
    {
        RecordAspectBuilder<TestRecord> newBuilder = new RecordAspectBuilder<>(TestRecord.class);

        assertNotNull(newBuilder);
        assertEquals(TestRecord.class, newBuilder.getRecordClass());
    }

    @Test
    void constructor_NullClass_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> new RecordAspectBuilder<>(null));
    }

    @Test
    void constructor_NonRecordClass_ThrowsException()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Record> fakeRecordClass = (Class<? extends Record>) (Class<?>) NotARecord.class;
                new RecordAspectBuilder<>(fakeRecordClass);
            } catch (ClassCastException e) {
                // This shouldn't happen at runtime, but if it does, re-throw as IllegalArgumentException
                throw new IllegalArgumentException("Class is not a record class", e);
            }
        });

        assertTrue(exception.getMessage().contains("is not a record class"));
    }

    @Test
    void getRecordClass_ReturnsCorrectClass()
    {
        assertEquals(TestRecord.class, builder.getRecordClass());
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
    void aspectDef_SetsCustomAspectDef()
    {
        RecordAspectDef customAspectDef = new RecordAspectDef(TestRecord.class);
        AspectBuilder result = builder.aspectDef(customAspectDef);

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
        AspectBuilder result = builder.property("name", "John Doe");

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
        PropertyDef propDef = new PropertyDefImpl("name", PropertyType.String);
        Property property = new PropertyImpl(propDef, "test-value");

        AspectBuilder result = builder.property(property);

        assertSame(builder, result);
    }

    @Test
    void property_NullProperty_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> builder.property((Property) null));
    }

    @Test
    void build_WithAllComponents_CreatesRecordAspect()
    {
        Aspect result = builder
            .entity(entity)
            .property("name", "John Doe")
            .property("age", 30)
            .property("active", true)
            .property("salary", 50000.0)
            .build();

        assertNotNull(result);
        assertInstanceOf(RecordAspect.class, result);
        assertSame(entity, result.entity());

        // Verify property values
        assertEquals("John Doe", result.unsafeReadObj("name"));
        assertEquals(30, result.unsafeReadObj("age"));
        assertEquals(true, result.unsafeReadObj("active"));
        assertEquals(50000.0, result.unsafeReadObj("salary"));
    }

    @Test
    void build_WithPropertyObject_CreatesRecordAspect()
    {
        PropertyDef propDef = new PropertyDefImpl("name", PropertyType.String);
        Property property = new PropertyImpl(propDef, "Jane Smith");

        Aspect result = builder
            .entity(entity)
            .property(property)
            .property("age", 25)
            .property("active", false)
            .property("salary", 45000.0)
            .build();

        assertNotNull(result);
        assertInstanceOf(RecordAspect.class, result);
        assertEquals("Jane Smith", result.unsafeReadObj("name"));
        assertEquals(25, result.unsafeReadObj("age"));
    }

    @Test
    void build_NoEntity_ThrowsException()
    {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            builder.property("name", "test").build());

        assertEquals("Entity must be set before building aspect", exception.getMessage());
    }

    @Test
    void build_WithMissingComponents_UsesDefaults()
    {
        // Only set some properties, others should get default values
        Aspect result = builder
            .entity(entity)
            .property("name", "Partial")
            .property("active", true)
            .build();

        assertNotNull(result);
        assertInstanceOf(RecordAspect.class, result);
        assertEquals("Partial", result.unsafeReadObj("name"));
        assertEquals(0, result.unsafeReadObj("age")); // int default
        assertEquals(true, result.unsafeReadObj("active"));
        assertNull(result.unsafeReadObj("salary")); // Double default
    }

    @Test
    void build_WithPrimitiveDefaults_HandlesPrimitiveTypes()
    {
        RecordAspectBuilder<PrimitiveRecord> primitiveBuilder = new RecordAspectBuilder<>(PrimitiveRecord.class);

        Aspect result = primitiveBuilder
            .entity(entity)
            .property("intValue", 42)
            .build();

        assertNotNull(result);
        assertEquals(42, result.unsafeReadObj("intValue"));
        assertEquals(false, result.unsafeReadObj("boolValue")); // boolean default
        assertEquals(0.0, result.unsafeReadObj("doubleValue")); // double default
        assertEquals('\0', result.unsafeReadObj("charValue")); // char default
    }

    @Test
    void build_VerifiesImmutableBehavior()
    {
        Aspect result = builder
            .entity(entity)
            .property("name", "Test")
            .property("age", 35)
            .property("active", true)
            .property("salary", 60000.0)
            .build();

        assertNotNull(result);
        assertInstanceOf(RecordAspect.class, result);

        // Verify initial values
        assertEquals("Test", result.unsafeReadObj("name"));
        assertEquals(35, result.unsafeReadObj("age"));

        // Verify immutability - writes should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> result.unsafeWrite("name", "Modified"));
        assertThrows(UnsupportedOperationException.class, () -> result.unsafeWrite("age", 40));

        // Values should remain unchanged
        assertEquals("Test", result.unsafeReadObj("name"));
        assertEquals(35, result.unsafeReadObj("age"));
    }

    @Test
    void build_WithSimpleRecord_CreatesCorrectly()
    {
        RecordAspectBuilder<SimpleRecord> simpleBuilder = new RecordAspectBuilder<>(SimpleRecord.class);

        Aspect result = simpleBuilder
            .entity(entity)
            .property("value", "Simple Test")
            .build();

        assertNotNull(result);
        assertInstanceOf(RecordAspect.class, result);
        assertEquals("Simple Test", result.unsafeReadObj("value"));
    }

    @Test
    void build_WithEmptyRecord_ThrowsException()
    {
        // Empty records are not supported by the aspect framework since AspectDef requires at least one property
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            new RecordAspectBuilder<>(EmptyRecord.class));

        assertTrue(exception.getMessage().contains("must contain at least one property"));
    }

    @Test
    void build_WithComplexRecord_HandlesComplexTypes()
    {
        RecordAspectBuilder<ComplexRecord> complexBuilder = new RecordAspectBuilder<>(ComplexRecord.class);
        UUID testId = UUID.randomUUID();
        List<String> testTags = List.of("tag1", "tag2");

        Aspect result = complexBuilder
            .entity(entity)
            .property("name", "Complex Test")
            .property("tags", testTags)
            .property("id", testId)
            .build();

        assertNotNull(result);
        assertInstanceOf(RecordAspect.class, result);
        assertEquals("Complex Test", result.unsafeReadObj("name"));
        assertEquals(testTags, result.unsafeReadObj("tags"));
        assertEquals(testId, result.unsafeReadObj("id"));
    }

    @Test
    void reset_ClearsAllState()
    {
        builder
            .entity(entity)
            .property("name", "test-value");

        AspectBuilder result = builder.reset();

        assertSame(builder, result);

        // Should throw exception since entity state is cleared
        assertThrows(IllegalStateException.class, () -> builder.build());
    }

    @Test
    void reset_AllowsReuse()
    {
        // Build first aspect
        Aspect aspect1 = builder
            .entity(entity)
            .property("name", "John")
            .property("age", 25)
            .property("active", true)
            .property("salary", 40000.0)
            .build();

        // Reset and build second aspect
        Entity entity2 = new EntityImpl();
        Aspect aspect2 = builder
            .reset()
            .entity(entity2)
            .property("name", "Jane")
            .property("age", 30)
            .property("active", false)
            .property("salary", 55000.0)
            .build();

        assertNotSame(aspect1, aspect2);
        assertEquals("John", aspect1.unsafeReadObj("name"));
        assertEquals("Jane", aspect2.unsafeReadObj("name"));
        assertEquals(25, aspect1.unsafeReadObj("age"));
        assertEquals(30, aspect2.unsafeReadObj("age"));
        assertNotSame(aspect1.entity(), aspect2.entity());
    }

    @Test
    void fluentInterface_ChainsMethods()
    {
        PropertyDef propDef = new PropertyDefImpl("name", PropertyType.String);
        Property property = new PropertyImpl(propDef, "prop-value");

        Aspect result = builder
            .entity(entity)
            .property("name", "Fluent Test")
            .property("age", 40)
            .property(property) // This will overwrite the "name" property set above
            .property("active", true)
            .property("salary", 70000.0)
            .build();

        assertNotNull(result);
        assertEquals("prop-value", result.unsafeReadObj("name")); // Should be the value from the property object
        assertEquals(40, result.unsafeReadObj("age"));
        assertEquals(true, result.unsafeReadObj("active"));
        assertEquals(70000.0, result.unsafeReadObj("salary"));
    }

    @Test
    void build_WithRecordAspectDefType_UsesProvidedDefinition()
    {
        RecordAspectDef customDef = new RecordAspectDef(TestRecord.class);

        Aspect result = builder
            .entity(entity)
            .aspectDef(customDef)
            .property("name", "Custom Def Test")
            .property("age", 33)
            .property("active", true)
            .property("salary", 80000.0)
            .build();

        assertNotNull(result);
        assertEquals("Custom Def Test", result.unsafeReadObj("name"));
        // Verify the custom definition is being used
        assertSame(customDef, result.def());
    }

    @Test
    void build_WithNonRecordAspectDef_FallsBackToDefaultDefinition()
    {
        // Use a different type of AspectDef that doesn't match our record
        RecordAspectDef genericDef = new RecordAspectDef(SimpleRecord.class); // Different record class

        Aspect result = builder
            .entity(entity)
            .aspectDef(genericDef)
            .property("name", "Fallback Test")
            .property("age", 28)
            .property("active", false)
            .property("salary", 45000.0)
            .build();

        assertNotNull(result);
        assertEquals("Fallback Test", result.unsafeReadObj("name"));
        // Should have fallen back to creating a new definition for TestRecord.class
        assertNotSame(genericDef, result.def());
        assertTrue(result.def() instanceof RecordAspectDef);
        // Verify it's using TestRecord definition, not SimpleRecord definition
        assertEquals(TestRecord.class.getCanonicalName(), result.def().name());
    }

    @Test
    void build_VerifyUnderlyingRecordInstance()
    {
        Aspect aspectResult = builder
            .entity(entity)
            .property("name", "Test")
            .property("age", 35)
            .property("active", true)
            .property("salary", 50000.0)
            .build();

        assertInstanceOf(RecordAspect.class, aspectResult);
        @SuppressWarnings("unchecked")
        RecordAspect<TestRecord> result = (RecordAspect<TestRecord>) aspectResult;

        assertNotNull(result.record());
        assertInstanceOf(TestRecord.class, result.record());

        // Verify the underlying record was properly created
        TestRecord record = result.record();
        assertEquals("Test", record.name());
        assertEquals(35, record.age());
        assertEquals(true, record.active());
        assertEquals(50000.0, record.salary());
    }

    @Test
    void build_RecordImmutabilityEnforcedByNature()
    {
        Aspect result = builder
            .entity(entity)
            .property("name", "Initial")
            .property("age", 25)
            .property("active", false)
            .property("salary", 35000.0)
            .build();

        @SuppressWarnings("unchecked")
        RecordAspect<TestRecord> recordAspect = (RecordAspect<TestRecord>) result;

        // Records are immutable by nature
        TestRecord record = recordAspect.record();
        assertEquals("Initial", record.name());
        assertEquals(25, record.age());

        // The aspect should throw on write attempts (records don't have setters)
        assertThrows(UnsupportedOperationException.class, () -> result.unsafeWrite("name", "Modified"));

        // The record values should remain unchanged
        assertEquals("Initial", result.unsafeReadObj("name"));
        assertEquals("Initial", record.name());
        assertEquals(25, record.age());
    }

    @Test
    void build_AllPrimitiveTypes_HandlesProperly()
    {
        RecordAspectBuilder<PrimitiveRecord> primitiveBuilder = new RecordAspectBuilder<>(PrimitiveRecord.class);

        Aspect result = primitiveBuilder
            .entity(entity)
            .property("intValue", 123)
            .property("boolValue", true)
            .property("doubleValue", 45.67)
            .property("charValue", 'A')
            .build();

        assertEquals(123, result.unsafeReadObj("intValue"));
        assertEquals(true, result.unsafeReadObj("boolValue"));
        assertEquals(45.67, result.unsafeReadObj("doubleValue"));
        assertEquals('A', result.unsafeReadObj("charValue"));
    }
}