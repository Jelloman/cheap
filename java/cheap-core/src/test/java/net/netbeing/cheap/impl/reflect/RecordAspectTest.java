package net.netbeing.cheap.impl.reflect;

import net.netbeing.cheap.impl.basic.BasicEntityImpl;
import net.netbeing.cheap.impl.basic.CatalogDefImpl;
import net.netbeing.cheap.impl.basic.CatalogImpl;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogType;
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
    final Catalog testCatalog = new CatalogImpl(new CatalogDefImpl(CatalogType.ROOT));

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
    void put()
    {
    }

    @Test
    void unsafeWrite()
    {
    }

    @Test
    void unsafeRemove()
    {
    }
}