package net.netbeing.cheap.impl.reflect;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.impl.basic.EntityImpl;
import net.netbeing.cheap.impl.basic.PropertyDefImpl;
import net.netbeing.cheap.impl.basic.PropertyImpl;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ImmutablePojoAspectBuilderTest
{
    private ImmutablePojoAspectBuilder<TestPojo> builder;
    private Entity entity;
    private UUID entityId;

    // Test POJO class for testing
    public static class TestPojo
    {
        private String name;
        private int age;
        private boolean active;
        private Double salary;

        public TestPojo() {} // Required no-arg constructor

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        public Double getSalary() { return salary; }
        public void setSalary(Double salary) { this.salary = salary; }
    }

    // Test POJO class without no-arg constructor
    public static class InvalidPojo
    {
        private String name;

        public InvalidPojo(String name) { this.name = name; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // Test POJO class that is not a record
    public static class NonRecordClass
    {
        private String value;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    @BeforeEach
    void setUp()
    {
        builder = new ImmutablePojoAspectBuilder<>(TestPojo.class);
        entityId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        entity = new EntityImpl(entityId);
    }

    @Test
    void constructor_CreatesBuilderWithValidClass()
    {
        ImmutablePojoAspectBuilder<TestPojo> newBuilder = new ImmutablePojoAspectBuilder<>(TestPojo.class);

        assertNotNull(newBuilder);
        assertEquals(TestPojo.class, newBuilder.getPojoClass());
    }

    @Test
    void constructor_NullClass_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> new ImmutablePojoAspectBuilder<>(null));
    }

    @Test
    void getPojoClass_ReturnsCorrectClass()
    {
        assertEquals(TestPojo.class, builder.getPojoClass());
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
        ImmutablePojoAspectDef customAspectDef = new ImmutablePojoAspectDef(TestPojo.class);
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
    void build_WithRequiredFields_CreatesImmutablePojoAspect()
    {
        Aspect result = builder
            .entity(entity)
            .property("name", "John Doe")
            .property("age", 30)
            .property("active", true)
            .property("salary", 50000.0)
            .build();

        assertNotNull(result);
        assertInstanceOf(ImmutablePojoAspect.class, result);
        assertSame(entity, result.entity());

        // Verify property values were set during construction
        assertEquals("John Doe", result.readObj("name"));
        assertEquals(30, result.readObj("age"));
        assertEquals(true, result.readObj("active"));
        assertEquals(50000.0, result.readObj("salary"));
    }

    @Test
    void build_WithPropertyObject_CreatesImmutablePojoAspect()
    {
        PropertyDef propDef = new PropertyDefImpl("name", PropertyType.String);
        Property property = new PropertyImpl(propDef, "Jane Smith");

        Aspect result = builder
            .entity(entity)
            .property(property)
            .property("age", 25)
            .build();

        assertNotNull(result);
        assertInstanceOf(ImmutablePojoAspect.class, result);
        assertEquals("Jane Smith", result.readObj("name"));
        assertEquals(25, result.readObj("age"));
    }

    @Test
    void build_NoEntity_ThrowsException()
    {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            builder.property("name", "test").build());

        assertEquals("Entity must be set before building aspect", exception.getMessage());
    }

    @Test
    void build_NoAspectDef_UsesDefaultAspectDef()
    {
        // AspectDef is automatically created in constructor, so this should work
        Aspect result = builder
            .entity(entity)
            .property("name", "Test Name")
            .build();

        assertNotNull(result);
        assertInstanceOf(ImmutablePojoAspect.class, result);
        assertEquals("Test Name", result.readObj("name"));
    }

    @Test
    void build_InvalidPojoClass_ThrowsException()
    {
        ImmutablePojoAspectBuilder<InvalidPojo> invalidBuilder =
            new ImmutablePojoAspectBuilder<>(InvalidPojo.class);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            invalidBuilder
                .entity(entity)
                .property("name", "test")
                .build());

        assertTrue(exception.getMessage().contains("Failed to create instance of"));
        assertTrue(exception.getMessage().contains("no-argument constructor"));
    }

    @Test
    void build_VerifiesImmutableBehavior()
    {
        Aspect result = builder
            .entity(entity)
            .property("name", "Test")
            .property("age", 35)
            .build();

        assertNotNull(result);
        assertInstanceOf(ImmutablePojoAspect.class, result);

        // Verify initial values
        assertEquals("Test", result.readObj("name"));
        assertEquals(35, result.readObj("age"));

        // Verify immutability - writes should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> result.unsafeWrite("name", "Modified"));
        assertThrows(UnsupportedOperationException.class, () -> result.unsafeWrite("age", 40));

        // Values should remain unchanged
        assertEquals("Test", result.readObj("name"));
        assertEquals(35, result.readObj("age"));
    }

    @Test
    void build_WithPrimitiveTypes_HandlesProperlyBoxedValues()
    {
        Aspect result = builder
            .entity(entity)
            .property("age", Integer.valueOf(42))
            .property("active", Boolean.valueOf(true))
            .build();

        assertEquals(42, result.readObj("age"));
        assertEquals(true, result.readObj("active"));
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
            .build();

        // Reset and build second aspect
        Entity entity2 = new EntityImpl();
        Aspect aspect2 = builder
            .reset()
            .entity(entity2)
            .property("name", "Jane")
            .property("age", 30)
            .build();

        assertNotSame(aspect1, aspect2);
        assertEquals("John", aspect1.readObj("name"));
        assertEquals("Jane", aspect2.readObj("name"));
        assertEquals(25, aspect1.readObj("age"));
        assertEquals(30, aspect2.readObj("age"));
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
            .build();

        assertNotNull(result);
        assertEquals("prop-value", result.readObj("name")); // Should be the value from the property object
        assertEquals(40, result.readObj("age"));
    }

    @Test
    void build_WithImmutablePojoAspectDefType_UsesProvidedDefinition()
    {
        ImmutablePojoAspectDef customDef = new ImmutablePojoAspectDef(TestPojo.class);

        Aspect result = builder
            .entity(entity)
            .aspectDef(customDef)
            .property("name", "Custom Def Test")
            .build();

        assertNotNull(result);
        assertEquals("Custom Def Test", result.readObj("name"));
        // Verify the custom definition is being used
        assertSame(customDef, result.def());
    }

    @Test
    void build_WithNonImmutablePojoAspectDef_FallsBackToDefaultDefinition()
    {
        // Use a different type of AspectDef - this one won't match our TestPojo
        AspectDef genericDef = new ImmutablePojoAspectDef(String.class); // Different class

        Aspect result = builder
            .entity(entity)
            .aspectDef(genericDef)
            .property("name", "Fallback Test") // TestPojo has this property
            .build();

        assertNotNull(result);
        assertEquals("Fallback Test", result.readObj("name"));
        // Should have fallen back to creating a new definition for TestPojo.class
        assertNotSame(genericDef, result.def());
        assertTrue(result.def() instanceof ImmutablePojoAspectDef);
        // Verify it's using TestPojo definition, not String definition
        assertEquals(TestPojo.class.getCanonicalName(), result.def().name());
    }

    @Test
    void build_WithNoPropertiesSet_CreatesEmptyInitializedPojo()
    {
        Aspect result = builder
            .entity(entity)
            .build();

        assertNotNull(result);
        assertInstanceOf(ImmutablePojoAspect.class, result);

        // Should be able to read properties (they'll have default values)
        assertNull(result.readObj("name")); // String defaults to null
        assertEquals(0, result.readObj("age")); // int defaults to 0
        assertEquals(false, result.readObj("active")); // boolean defaults to false
        assertNull(result.readObj("salary")); // Double defaults to null
    }

    @Test
    void build_VerifyUnderlyingPojoInstance()
    {
        Aspect aspectResult = builder
            .entity(entity)
            .property("name", "Test")
            .property("age", 35)
            .build();

        assertInstanceOf(ImmutablePojoAspect.class, aspectResult);
        @SuppressWarnings("unchecked")
        ImmutablePojoAspect<TestPojo> result = (ImmutablePojoAspect<TestPojo>) aspectResult;

        assertNotNull(result.object());
        assertInstanceOf(TestPojo.class, result.object());

        // Verify the underlying POJO was properly initialized
        TestPojo pojo = result.object();
        assertEquals("Test", pojo.getName());
        assertEquals(35, pojo.getAge());
    }

    @Test
    void build_ImmutabilityEnforcedAtAspectLevel()
    {
        Aspect result = builder
            .entity(entity)
            .property("name", "Initial")
            .build();

        @SuppressWarnings("unchecked")
        ImmutablePojoAspect<TestPojo> immutableAspect = (ImmutablePojoAspect<TestPojo>) result;

        // The underlying POJO might have setters, but the aspect should prevent their use
        TestPojo pojo = immutableAspect.object();
        assertEquals("Initial", pojo.getName());

        // Even though the POJO has setters, the aspect should throw on write attempts
        assertThrows(UnsupportedOperationException.class, () -> result.unsafeWrite("name", "Modified"));

        // The POJO value should remain unchanged through the aspect
        assertEquals("Initial", result.readObj("name"));
        assertEquals("Initial", pojo.getName());
    }
}