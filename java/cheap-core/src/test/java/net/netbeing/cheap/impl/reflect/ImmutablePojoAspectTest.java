package net.netbeing.cheap.impl.reflect;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.netbeing.cheap.impl.basic.BasicEntityImpl;
import net.netbeing.cheap.impl.basic.CatalogDefImpl;
import net.netbeing.cheap.impl.basic.CatalogImpl;
import net.netbeing.cheap.impl.basic.PropertyImpl;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ImmutablePojoAspectTest
{
    @Data @AllArgsConstructor
    public static class TestClass
    {
        private int integerPrimitive;
        private char charPrimitive;
        private boolean booleanPrimitive;
        private byte bytePrimitive;
        private short shortPrimitive;
        private long longPrimitive;
        private float floatPrimitive;
        private double doublePrimitive;
        private String string;
        private Integer integer;
        private Character character;
        private UUID uuid;
        private URI uri;
        private LocalDateTime localDateTime;
    }

    private static TestClass pojo1;
    private static TestClass pojo2;

    final Entity testEntity = new BasicEntityImpl();
    final Catalog testCatalog = new CatalogImpl(new CatalogDefImpl(CatalogType.ROOT));

    ImmutablePojoAspectDef def;
    ImmutablePojoAspect<TestClass> immutablePojoAspect;

    @BeforeEach
    void setUp()
    {
        def = new ImmutablePojoAspectDef(TestClass.class);
        pojo1 = new TestClass(1, 'a', true, (byte) 10, (short) 100, 1000L, 10.5f, 100.25, "foo", 2, 'b', UUID.randomUUID(), URI.create("http://example.com/"), LocalDateTime.now());
        pojo2 = new TestClass(3, 'c', false, (byte) 30, (short) 300, 3000L, 30.5f, 300.75, "bar", 4, 'd', UUID.randomUUID(), URI.create("http://example.com/bar"), LocalDateTime.now().minusDays(1));
    }

    @AfterEach
    void tearDown()
    {
        def = null;
        pojo1 = null;
        pojo2 = null;
        immutablePojoAspect = null;
    }

    @Test
    void construct()
    {
        immutablePojoAspect = new ImmutablePojoAspect<>(testCatalog, testEntity, def, pojo1);
    }

    @Test
    void read()
    {
        immutablePojoAspect = new ImmutablePojoAspect<>(testCatalog, testEntity, def, pojo1);

        String value = immutablePojoAspect.readAs("string", String.class);

        assertEquals("foo", value);
        assertEquals(pojo1.getString(), value);
    }

    @Test
    void get()
    {
        immutablePojoAspect = new ImmutablePojoAspect<>(testCatalog, testEntity, def, pojo1);

        Property prop = immutablePojoAspect.get("string");
        assertEquals("foo", prop.read());

        Property booleanProp = immutablePojoAspect.get("booleanPrimitive");
        assertEquals(true, booleanProp.read());

        Property intProp = immutablePojoAspect.get("integerPrimitive");
        assertEquals(1, intProp.read());

        Property charProp = immutablePojoAspect.get("charPrimitive");
        assertEquals('a', charProp.read());
        
        Property byteProp = immutablePojoAspect.get("bytePrimitive");
        assertEquals((byte) 10, byteProp.read());
        
        Property shortProp = immutablePojoAspect.get("shortPrimitive");
        assertEquals((short) 100, shortProp.read());
        
        Property longProp = immutablePojoAspect.get("longPrimitive");
        assertEquals(1000L, longProp.read());
        
        Property floatProp = immutablePojoAspect.get("floatPrimitive");
        assertEquals(10.5f, floatProp.read());
        
        Property doubleProp = immutablePojoAspect.get("doublePrimitive");
        assertEquals(100.25, doubleProp.read());
    }

    @Test
    void put()
    {
        immutablePojoAspect = new ImmutablePojoAspect<>(testCatalog, testEntity, def, pojo2);

        Property prop = immutablePojoAspect.get("string");
        assertEquals("bar", prop.read());

        Property newProp = new PropertyImpl(prop.def(), "baz");
        Throwable exception = assertThrows(UnsupportedOperationException.class, () -> immutablePojoAspect.put(newProp));
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