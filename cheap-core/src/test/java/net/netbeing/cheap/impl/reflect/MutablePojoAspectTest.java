package net.netbeing.cheap.impl.reflect;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.netbeing.cheap.impl.basic.EntityImpl;
import net.netbeing.cheap.impl.basic.PropertyImpl;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MutablePojoAspectTest
{
    @Data @AllArgsConstructor
    public static class TestClass
    {
        private int integerPrimitive;
        private char charPrimitive;
        private boolean booleanPrimitive;
        private String string;
        private Integer integer;
        private Character character;
        private UUID uuid;
        private URI uri;
        private LocalDateTime localDateTime;
    }

    private static TestClass pojo1;
    private static TestClass pojo2;

    final Entity testEntity = new EntityImpl();

    MutablePojoAspectDef def;
    MutablePojoAspect<TestClass> mutablePojoAspect;

    @BeforeEach
    void setUp()
    {
        def = new MutablePojoAspectDef(TestClass.class);
        pojo1 = new TestClass(1, 'a', true, "foo", 2, 'b', UUID.randomUUID(), URI.create("http://example.com/"), LocalDateTime.now());
        pojo2 = new TestClass(3, 'c', false, "bar", 4, 'd', UUID.randomUUID(), URI.create("http://example.com/foo"), LocalDateTime.now().minusDays(1));
    }

    @AfterEach
    void tearDown()
    {
        def = null;
        pojo1 = null;
        pojo2 = null;
        mutablePojoAspect = null;
    }

    @Test
    void construct()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
    }

    @Test
    void read()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);

        String value = mutablePojoAspect.readAs("string", String.class);

        assertEquals("foo", value);
        assertEquals(pojo1.getString(), value);
    }

    @Test
    void get()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);

        Property prop = mutablePojoAspect.get("string");
        assertEquals("foo", prop.read());

        Property intProp = mutablePojoAspect.get("integerPrimitive");
        assertEquals(1, intProp.read());
        
        Property charProp = mutablePojoAspect.get("charPrimitive");
        assertEquals('a', charProp.read());
    }

    @Test
    void put()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo2);

        Property prop = mutablePojoAspect.get("string");
        assertEquals("bar", prop.read());

        Property newProp = new PropertyImpl(prop.def(), "baz");
        mutablePojoAspect.put(newProp);

        Property prop2 = mutablePojoAspect.get("string");
        assertEquals("baz", prop2.read());

    }

    @Test
    void object()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        
        assertSame(pojo1, mutablePojoAspect.object());
    }

    @Test
    void entity()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        
        assertSame(testEntity, mutablePojoAspect.entity());
    }

    @Test
    void def()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        
        assertSame(def, mutablePojoAspect.def());
    }

    @Test
    void contains()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        
        assertTrue(mutablePojoAspect.contains("string"));
        assertTrue(mutablePojoAspect.contains("integerPrimitive"));
        assertTrue(mutablePojoAspect.contains("uuid"));
        
        // For non-existent fields, contains() will throw an exception because unsafeReadObj() throws
        assertThrows(IllegalArgumentException.class, 
            () -> mutablePojoAspect.contains("nonExistentField"));
    }

    @Test
    void unsafeReadObj()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        
        assertEquals("foo", mutablePojoAspect.unsafeReadObj("string"));
        assertEquals(1, mutablePojoAspect.unsafeReadObj("integerPrimitive"));
        assertEquals('a', mutablePojoAspect.unsafeReadObj("charPrimitive"));
        assertEquals(true, mutablePojoAspect.unsafeReadObj("booleanPrimitive"));
        assertEquals(2, mutablePojoAspect.unsafeReadObj("integer"));
        assertEquals('b', mutablePojoAspect.unsafeReadObj("character"));
        assertNotNull(mutablePojoAspect.unsafeReadObj("uuid"));
        assertNotNull(mutablePojoAspect.unsafeReadObj("uri"));
        assertNotNull(mutablePojoAspect.unsafeReadObj("localDateTime"));
    }

    @Test
    void unsafeReadObj_NonExistentProperty_ThrowsException()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> mutablePojoAspect.unsafeReadObj("nonExistentField")
        );
        
        assertTrue(exception.getMessage().contains("does not contain field 'nonExistentField'"));
    }

    @Test
    void unsafeWrite()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);

        assertEquals("foo", mutablePojoAspect.unsafeReadObj("string"));
        assertEquals(2, mutablePojoAspect.unsafeReadObj("integer"));

        mutablePojoAspect.unsafeWrite("string", "updated");
        mutablePojoAspect.unsafeWrite("integer", 7);

        assertEquals("updated", mutablePojoAspect.unsafeReadObj("string"));
        assertEquals("updated", pojo1.getString()); // Verify the underlying object was modified
        assertEquals(7, mutablePojoAspect.unsafeReadObj("integer"));
        assertEquals(7, pojo1.getInteger()); // Verify the underlying object was modified

    }

    @Test
    void unsafeWrite_PrimitiveTypes()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        
        mutablePojoAspect.unsafeWrite("integerPrimitive", 999);
        assertEquals(999, mutablePojoAspect.unsafeReadObj("integerPrimitive"));
        assertEquals(999, pojo1.getIntegerPrimitive());
        
        mutablePojoAspect.unsafeWrite("charPrimitive", 'z');
        assertEquals('z', mutablePojoAspect.unsafeReadObj("charPrimitive"));
        assertEquals('z', pojo1.getCharPrimitive());
        
        mutablePojoAspect.unsafeWrite("booleanPrimitive", false);
        assertEquals(false, mutablePojoAspect.unsafeReadObj("booleanPrimitive"));
        assertFalse(pojo1.isBooleanPrimitive());
    }

    @Test
    void unsafeWrite_NonExistentProperty_ThrowsException()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> mutablePojoAspect.unsafeWrite("nonExistentField", "value")
        );
        
        assertTrue(exception.getMessage().contains("does not contain field 'nonExistentField'"));
    }

    @Test
    void unsafeAdd_ThrowsUnsupportedOperation()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        Property prop = mutablePojoAspect.get("string");
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> mutablePojoAspect.unsafeAdd(prop)
        );
        
        assertTrue(exception.getMessage().contains("cannot be added to Java class"));
    }

    @Test
    void unsafeRemove_ThrowsUnsupportedOperation()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> mutablePojoAspect.unsafeRemove("string")
        );
        
        assertTrue(exception.getMessage().contains("cannot be removed from Java class"));
    }

    @Test
    void write_ThroughAspectInterface()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        
        assertEquals("foo", mutablePojoAspect.readAs("string", String.class));
        
        mutablePojoAspect.write("string", "newValue");
        
        assertEquals("newValue", mutablePojoAspect.readAs("string", String.class));
        assertEquals("newValue", pojo1.getString());
    }

    @Test
    void readAs_TypedReading()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        
        String stringValue = mutablePojoAspect.readAs("string", String.class);
        assertEquals("foo", stringValue);
        
        Integer intValue = mutablePojoAspect.readAs("integerPrimitive", Integer.class);
        assertEquals(1, intValue);
        
        Character charValue = mutablePojoAspect.readAs("charPrimitive", Character.class);
        assertEquals('a', charValue);
        
        Boolean boolValue = mutablePojoAspect.readAs("booleanPrimitive", Boolean.class);
        assertTrue(boolValue);
    }

    @Test
    void unsafeRead_GenericMethod()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        
        String stringValue = mutablePojoAspect.unsafeRead("string");
        assertEquals("foo", stringValue);
        
        Integer intValue = mutablePojoAspect.unsafeRead("integerPrimitive");
        assertEquals(1, intValue);
        
        Boolean boolValue = mutablePojoAspect.unsafeRead("booleanPrimitive");
        assertTrue(boolValue);
    }

    @Test
    void readObj_WithNullValue()
    {
        TestClass pojoWithNull = new TestClass(1, 'a', true, null, null, null, null, null, null);
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojoWithNull);
        
        assertNull(mutablePojoAspect.readObj("string"));
        assertNull(mutablePojoAspect.readObj("integer"));
        assertNull(mutablePojoAspect.readObj("character"));
        assertNull(mutablePojoAspect.readObj("uuid"));
        assertNull(mutablePojoAspect.readObj("uri"));
        assertNull(mutablePojoAspect.readObj("localDateTime"));
        
        // Test that contains() returns false for null values
        assertFalse(mutablePojoAspect.contains("string"));
        assertFalse(mutablePojoAspect.contains("integer"));
    }

    @Test
    void writeNullValue()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        
        assertNotNull(mutablePojoAspect.readObj("string"));
        
        mutablePojoAspect.unsafeWrite("string", null);
        
        assertNull(mutablePojoAspect.readObj("string"));
        assertNull(pojo1.getString());
    }

    @Test
    void uncheckedRead_GenericMethod()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testEntity, def, pojo1);
        
        String stringValue = mutablePojoAspect.uncheckedRead("string");
        assertEquals("foo", stringValue);
        
        Character charValue = mutablePojoAspect.uncheckedRead("character");
        assertEquals('b', charValue);
    }

}