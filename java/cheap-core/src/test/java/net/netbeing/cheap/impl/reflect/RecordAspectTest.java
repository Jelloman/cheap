package net.netbeing.cheap.impl.reflect;

import net.netbeing.cheap.impl.basic.BasicEntityImpl;
import net.netbeing.cheap.impl.basic.CatalogDefImpl;
import net.netbeing.cheap.impl.basic.CatalogImpl;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RecordAspectTest
{
    public record TestRecord(
        int integerPrimitive,
        char charPrimitive,
        boolean booleanPrimitive,
        byte bytePrimitive,
        short shortPrimitive,
        long longPrimitive,
        float floatPrimitive,
        double doublePrimitive,
        String string,
        Integer integer,
        Character character,
        UUID uuid,
        URI uri,
        LocalDateTime localDateTime
    ) {}

    private static TestRecord record1;
    private static TestRecord record2;

    final Entity testEntity = new BasicEntityImpl();
    final Catalog testCatalog = new CatalogImpl();

    RecordAspectDef def;
    RecordAspect<TestRecord> recordAspect;

    @BeforeEach
    void setUp()
    {
        def = new RecordAspectDef(TestRecord.class);
        record1 = new TestRecord(1, 'a', true, (byte) 10, (short) 100, 1000L, 10.5f, 100.25, "foo", 2, 'b', UUID.randomUUID(), URI.create("http://example.com/"), LocalDateTime.now());
        record2 = new TestRecord(3, 'c', false, (byte) 30, (short) 300, 3000L, 30.5f, 300.75, "bar", 4, 'd', UUID.randomUUID(), URI.create("http://example.com/bar"), LocalDateTime.now().minusDays(1));
    }

    @AfterEach
    void tearDown()
    {
        def = null;
        record1 = null;
        record2 = null;
        recordAspect = null;
    }

    @Test
    void construct()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
    }

    @Test
    void read()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);

        String value = recordAspect.readAs("string", String.class);

        assertEquals("foo", value);
        assertEquals(record1.string, value);
    }

    @Test
    void get()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);

        Property prop = recordAspect.get("string");
        assertEquals("foo", prop.read());

        // Test primitive types that should work
        Property intProp = recordAspect.get("integerPrimitive");
        assertEquals(1, intProp.read());
        
        Property charProp = recordAspect.get("charPrimitive");
        assertEquals('a', charProp.read());
        
        Property byteProp = recordAspect.get("bytePrimitive");
        assertEquals((byte) 10, byteProp.read());
        
        Property shortProp = recordAspect.get("shortPrimitive");
        assertEquals((short) 100, shortProp.read());
        
        Property longProp = recordAspect.get("longPrimitive");
        assertEquals(1000L, longProp.read());
        
        Property floatProp = recordAspect.get("floatPrimitive");
        assertEquals(10.5f, floatProp.read());
        
        Property doubleProp = recordAspect.get("doublePrimitive");
        assertEquals(100.25, doubleProp.read());
    }

    @Test
    void construct_WithEntity()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        
        assertSame(testCatalog, recordAspect.catalog());
        assertSame(testEntity, recordAspect.entity());
        assertSame(def, recordAspect.def());
        assertSame(record1, recordAspect.record());
    }

    @Test
    void construct_WithoutEntity()
    {
        recordAspect = new RecordAspect<>(testCatalog, def, record1);
        
        assertSame(testCatalog, recordAspect.catalog());
        assertNotNull(recordAspect.entity()); // Should create EntityLazyIdImpl
        assertSame(def, recordAspect.def());
        assertSame(record1, recordAspect.record());
    }

    @Test
    void record()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        
        assertSame(record1, recordAspect.record());
    }

    @Test
    void catalog()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        
        assertSame(testCatalog, recordAspect.catalog());
    }

    @Test
    void entity()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        
        assertSame(testEntity, recordAspect.entity());
    }

    @Test
    void def()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        
        assertSame(def, recordAspect.def());
    }

    @Test
    void contains()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        
        assertTrue(recordAspect.contains("string"));
        assertTrue(recordAspect.contains("integerPrimitive"));
        assertTrue(recordAspect.contains("uuid"));
        
        // For non-existent fields, contains() will throw an exception because unsafeReadObj() throws
        assertThrows(IllegalArgumentException.class, 
            () -> recordAspect.contains("nonExistentField"));
    }

    @Test
    void unsafeReadObj()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        
        assertEquals("foo", recordAspect.unsafeReadObj("string"));
        assertEquals(1, recordAspect.unsafeReadObj("integerPrimitive"));
        assertEquals('a', recordAspect.unsafeReadObj("charPrimitive"));
        assertEquals(true, recordAspect.unsafeReadObj("booleanPrimitive"));
        assertEquals((byte) 10, recordAspect.unsafeReadObj("bytePrimitive"));
        assertEquals((short) 100, recordAspect.unsafeReadObj("shortPrimitive"));
        assertEquals(1000L, recordAspect.unsafeReadObj("longPrimitive"));
        assertEquals(10.5f, recordAspect.unsafeReadObj("floatPrimitive"));
        assertEquals(100.25, recordAspect.unsafeReadObj("doublePrimitive"));
        assertEquals(2, recordAspect.unsafeReadObj("integer"));
        assertEquals('b', recordAspect.unsafeReadObj("character"));
        assertNotNull(recordAspect.unsafeReadObj("uuid"));
        assertNotNull(recordAspect.unsafeReadObj("uri"));
        assertNotNull(recordAspect.unsafeReadObj("localDateTime"));
    }

    @Test
    void unsafeReadObj_NonExistentProperty_ThrowsException()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> recordAspect.unsafeReadObj("nonExistentField")
        );
        
        assertTrue(exception.getMessage().contains("does not contain field 'nonExistentField'"));
    }

    @Test
    void unsafeWrite_ThrowsUnsupportedOperation()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> recordAspect.unsafeWrite("string", "newValue")
        );
        
        assertTrue(exception.getMessage().contains("cannot be set in Record class"));
    }

    @Test
    void unsafeAdd_ThrowsUnsupportedOperation()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        Property prop = recordAspect.get("string");
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> recordAspect.unsafeAdd(prop)
        );
        
        assertTrue(exception.getMessage().contains("cannot be added to Record class"));
    }

    @Test
    void unsafeRemove_ThrowsUnsupportedOperation()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> recordAspect.unsafeRemove("string")
        );
        
        assertTrue(exception.getMessage().contains("cannot be removed in Record class"));
    }

    @Test
    void get_AllPrimitiveTypes()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);

        Property booleanProp = recordAspect.get("booleanPrimitive");
        assertEquals(true, booleanProp.read());

        Property intProp = recordAspect.get("integerPrimitive");
        assertEquals(1, intProp.read());
        
        Property charProp = recordAspect.get("charPrimitive");
        assertEquals('a', charProp.read());
        
        Property byteProp = recordAspect.get("bytePrimitive");
        assertEquals((byte) 10, byteProp.read());
        
        Property shortProp = recordAspect.get("shortPrimitive");
        assertEquals((short) 100, shortProp.read());
        
        Property longProp = recordAspect.get("longPrimitive");
        assertEquals(1000L, longProp.read());
        
        Property floatProp = recordAspect.get("floatPrimitive");
        assertEquals(10.5f, floatProp.read());
        
        Property doubleProp = recordAspect.get("doublePrimitive");
        assertEquals(100.25, doubleProp.read());
        
        Property stringProp = recordAspect.get("string");
        assertEquals("foo", stringProp.read());
        
        Property integerProp = recordAspect.get("integer");
        assertEquals(2, integerProp.read());
        
        Property characterProp = recordAspect.get("character");
        assertEquals('b', characterProp.read());
    }

    @Test
    void readAs_TypedReading()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        
        String stringValue = recordAspect.readAs("string", String.class);
        assertEquals("foo", stringValue);
        
        Integer intValue = recordAspect.readAs("integerPrimitive", Integer.class);
        assertEquals(1, intValue);
        
        Character charValue = recordAspect.readAs("charPrimitive", Character.class);
        assertEquals('a', charValue);
        
        Boolean boolValue = recordAspect.readAs("booleanPrimitive", Boolean.class);
        assertTrue(boolValue);
        
        Byte byteValue = recordAspect.readAs("bytePrimitive", Byte.class);
        assertEquals((byte) 10, byteValue);
        
        Short shortValue = recordAspect.readAs("shortPrimitive", Short.class);
        assertEquals((short) 100, shortValue);
        
        Long longValue = recordAspect.readAs("longPrimitive", Long.class);
        assertEquals(1000L, longValue);
        
        Float floatValue = recordAspect.readAs("floatPrimitive", Float.class);
        assertEquals(10.5f, floatValue);
        
        Double doubleValue = recordAspect.readAs("doublePrimitive", Double.class);
        assertEquals(100.25, doubleValue);
    }

    @Test
    void unsafeRead_GenericMethod()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        
        String stringValue = recordAspect.unsafeRead("string");
        assertEquals("foo", stringValue);
        
        Integer intValue = recordAspect.unsafeRead("integerPrimitive");
        assertEquals(1, intValue);
        
        Boolean boolValue = recordAspect.unsafeRead("booleanPrimitive");
        assertTrue(boolValue);
    }

    @Test
    void readObj_WithNullValue()
    {
        TestRecord recordWithNull = new TestRecord(1, 'a', true, (byte) 10, (short) 100, 1000L, 10.5f, 100.25, null, null, null, null, null, null);
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, recordWithNull);
        
        assertNull(recordAspect.unsafeReadObj("string"));
        assertNull(recordAspect.unsafeReadObj("integer"));
        assertNull(recordAspect.unsafeReadObj("character"));
        assertNull(recordAspect.unsafeReadObj("uuid"));
        assertNull(recordAspect.unsafeReadObj("uri"));
        assertNull(recordAspect.unsafeReadObj("localDateTime"));
        
        // Test that contains() returns false for null values
        assertFalse(recordAspect.contains("string"));
        assertFalse(recordAspect.contains("integer"));
    }

    @Test
    void uncheckedRead_GenericMethod()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        
        String stringValue = recordAspect.uncheckedRead("string");
        assertEquals("foo", stringValue);
        
        Character charValue = recordAspect.uncheckedRead("character");
        assertEquals('b', charValue);
    }

    @Test 
    void put_ThrowsUnsupportedOperation()
    {
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record1);
        Property prop = recordAspect.get("string");
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> recordAspect.put(prop)
        );
        
        assertTrue(exception.getMessage().contains("is not writable"));
    }

}