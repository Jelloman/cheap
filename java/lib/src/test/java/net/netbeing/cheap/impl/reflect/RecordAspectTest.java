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
        String string,
        int integerPrimitive,
        Integer integer,
        char charPrimitive,
        Character character,
        UUID uuid,
        URI uri,
        LocalDateTime localDateTime
    ) {}

    final Entity testEntity = new BasicEntityImpl();
    final Catalog testCatalog = new CatalogImpl(new CatalogDefImpl(CatalogType.ROOT));

    final TestRecord testRecord2 = new TestRecord("bar", -1, -2, 'A', 'B', UUID.randomUUID(), URI.create("http://www.example.com/"), LocalDateTime.now().plusMinutes(1));

    RecordAspectDef def;
    TestRecord record;
    RecordAspect<TestRecord> recordAspect;

    @BeforeEach
    void setUp()
    {
        def = new RecordAspectDef(TestRecord.class);
    }

    @AfterEach
    void tearDown()
    {
        def = null;
        record = null;
        recordAspect = null;
    }

    @Test
    void construct()
    {
        record = new TestRecord("foo", 1, 2, 'a', 'b', UUID.randomUUID(), URI.create("http://example.com/"), LocalDateTime.now());
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record);
    }

    @Test
    void read()
    {
        record = new TestRecord("foo", 1, 2, 'a', 'b', UUID.randomUUID(), URI.create("http://example.com/"), LocalDateTime.now());
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record);

        String value = recordAspect.readAs("string", String.class);

        assertEquals("foo", value);
        assertEquals(record.string, value);
    }

    @Test
    void get()
    {
        record = new TestRecord("foo", 1, 2, 'a', 'b', UUID.randomUUID(), URI.create("http://example.com/"), LocalDateTime.now());
        recordAspect = new RecordAspect<>(testCatalog, testEntity, def, record);

        Property prop = recordAspect.get("string");

        assertEquals("foo", prop.read());
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