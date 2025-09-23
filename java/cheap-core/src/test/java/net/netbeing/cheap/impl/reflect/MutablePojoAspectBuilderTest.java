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

class MutablePojoAspectBuilderTest
{
    private MutablePojoAspectBuilder<TestPojo> builder;
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

    @BeforeEach
    void setUp()
    {
        builder = new MutablePojoAspectBuilder<>(TestPojo.class);
        entityId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        entity = new EntityImpl(entityId);
    }

    @Test
    void constructor_CreatesBuilderWithValidClass()
    {
        MutablePojoAspectBuilder<TestPojo> newBuilder = new MutablePojoAspectBuilder<>(TestPojo.class);

        assertNotNull(newBuilder);
        assertEquals(TestPojo.class, newBuilder.getPojoClass());
    }

    @Test
    void constructor_NullClass_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> new MutablePojoAspectBuilder<>(null));
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
        MutablePojoAspectDef customAspectDef = new MutablePojoAspectDef(TestPojo.class);
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
    void build_WithRequiredFields_CreatesMutablePojoAspect()
    {
        Aspect result = builder
            .entity(entity)
            .property("name", "John Doe")
            .property("age", 30)
            .property("active", true)
            .property("salary", 50000.0)
            .build();

        assertNotNull(result);
        assertInstanceOf(MutablePojoAspect.class, result);
        assertSame(entity, result.entity());

        // Verify property values
        assertEquals("John Doe", result.unsafeReadObj("name"));
        assertEquals(30, result.unsafeReadObj("age"));
        assertEquals(true, result.unsafeReadObj("active"));
        assertEquals(50000.0, result.unsafeReadObj("salary"));
    }

    @Test
    void build_WithPropertyObject_CreatesMutablePojoAspect()
    {
        PropertyDef propDef = new PropertyDefImpl("name", PropertyType.String);
        Property property = new PropertyImpl(propDef, "Jane Smith");

        Aspect result = builder
            .entity(entity)
            .property(property)
            .property("age", 25)
            .build();

        assertNotNull(result);
        assertInstanceOf(MutablePojoAspect.class, result);
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
    void build_NoAspectDef_UsesDefaultAspectDef()
    {
        // AspectDef is automatically created in constructor, so this should work
        Aspect result = builder
            .entity(entity)
            .property("name", "Test Name")
            .build();

        assertNotNull(result);
        assertInstanceOf(MutablePojoAspect.class, result);
        assertEquals("Test Name", result.unsafeReadObj("name"));
    }

    @Test
    void build_InvalidPojoClass_ThrowsException()
    {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            new MutablePojoAspectBuilder<>(InvalidPojo.class));

        assertTrue(exception.getMessage().contains("Failed to create instance of"));
        assertTrue(exception.getMessage().contains("no-argument constructor"));
    }

    @Test
    void build_VerifiesPojoInstanceCreation()
    {
        Aspect aspectResult = builder
            .entity(entity)
            .property("name", "Test")
            .property("age", 35)
            .build();

        assertInstanceOf(MutablePojoAspect.class, aspectResult);
        @SuppressWarnings("unchecked")
        MutablePojoAspect<TestPojo> result = (MutablePojoAspect<TestPojo>) aspectResult;

        assertNotNull(result.object());
        assertInstanceOf(TestPojo.class, result.object());

        // Verify the underlying POJO was properly set
        TestPojo pojo = result.object();
        assertEquals("Test", pojo.getName());
        assertEquals(35, pojo.getAge());
    }

    @Test
    void build_WithPrimitiveTypes_HandlesProperlyBoxedValues()
    {
        Aspect result = builder
            .entity(entity)
            .property("age", Integer.valueOf(42))
            .property("active", Boolean.valueOf(true))
            .build();

        assertEquals(42, result.unsafeReadObj("age"));
        assertEquals(true, result.unsafeReadObj("active"));
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
        assertEquals("John", aspect1.unsafeReadObj("name"));
        assertEquals("Jane", aspect2.unsafeReadObj("name"));
        assertEquals(25, aspect1.unsafeReadObj("age"));
        assertEquals(30, aspect2.unsafeReadObj("age"));
        assertNotSame(aspect1.entity(), aspect2.entity());
    }

    @Test
    void reset_ResetsToDefaultAspectDef()
    {
        // Set custom aspect definition
        MutablePojoAspectDef customAspectDef = new MutablePojoAspectDef(TestPojo.class);
        builder.aspectDef(customAspectDef);

        builder.reset();

        // Should create new default aspect definition after reset
        Aspect result = builder
            .entity(entity)
            .property("name", "test")
            .build();

        assertNotNull(result);
        assertNotNull(result.def());
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
        assertEquals("prop-value", result.unsafeReadObj("name")); // Should be the value from the property object
        assertEquals(40, result.unsafeReadObj("age"));
    }

    @Test
    void build_WithMutablePojoAspectDefType_UsesProvidedDefinitionAfterReset()
    {
        MutablePojoAspectDef customDef = new MutablePojoAspectDef(TestPojo.class);

        // The aspectDef is only used when constructing the aspect (constructor or reset)
        Aspect result = builder
            .entity(entity)
            .aspectDef(customDef)
            .reset() // This will rebuild the aspect using the custom definition
            .entity(entity) // Need to set entity again after reset
            .property("name", "Custom Def Test")
            .build();

        assertNotNull(result);
        assertEquals("Custom Def Test", result.unsafeReadObj("name"));
        // After reset with custom definition, it should use the custom definition
        assertSame(customDef, result.def());
    }

    @Test
    void build_WithNonMutablePojoAspectDef_FallsBackToDefaultDefinition()
    {
        // Use a different type of AspectDef - this one won't match our TestPojo
        AspectDef genericDef = new MutablePojoAspectDef(String.class); // Different class

        Aspect result = builder
            .entity(entity)
            .aspectDef(genericDef)
            .property("name", "Fallback Test") // TestPojo has this property
            .build();

        assertNotNull(result);
        assertEquals("Fallback Test", result.unsafeReadObj("name"));
        // Should have fallen back to creating a new definition for TestPojo.class
        assertNotSame(genericDef, result.def());
        assertTrue(result.def() instanceof MutablePojoAspectDef);
        // Verify it's using TestPojo definition, not String definition
        assertEquals(TestPojo.class.getCanonicalName(), result.def().name());
    }

    @Test
    void build_CanModifyCreatedAspect()
    {
        Aspect aspectResult = builder
            .entity(entity)
            .property("name", "Initial Name")
            .property("age", 20)
            .build();

        assertInstanceOf(MutablePojoAspect.class, aspectResult);
        @SuppressWarnings("unchecked")
        MutablePojoAspect<TestPojo> result = (MutablePojoAspect<TestPojo>) aspectResult;

        // Verify initial values
        assertEquals("Initial Name", result.unsafeReadObj("name"));
        assertEquals(20, result.unsafeReadObj("age"));

        // Modify through the aspect
        result.unsafeWrite("name", "Modified Name");
        result.unsafeWrite("age", 30);

        // Verify modifications
        assertEquals("Modified Name", result.unsafeReadObj("name"));
        assertEquals(30, result.unsafeReadObj("age"));

        // Verify underlying POJO was also modified
        TestPojo pojo = result.object();
        assertEquals("Modified Name", pojo.getName());
        assertEquals(30, pojo.getAge());
    }
}