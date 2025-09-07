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

    final Entity testEntity = new BasicEntityImpl();
    final Catalog testCatalog = new CatalogImpl(new CatalogDefImpl(CatalogType.ROOT));

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
        mutablePojoAspect = new MutablePojoAspect<>(testCatalog, testEntity, def, pojo1);
    }

    @Test
    void read()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testCatalog, testEntity, def, pojo1);

        String value = mutablePojoAspect.readAs("string", String.class);

        assertEquals("foo", value);
        assertEquals(pojo1.getString(), value);
    }

    @Test
    void get()
    {
        mutablePojoAspect = new MutablePojoAspect<>(testCatalog, testEntity, def, pojo1);

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
        mutablePojoAspect = new MutablePojoAspect<>(testCatalog, testEntity, def, pojo2);

        Property prop = mutablePojoAspect.get("string");
        assertEquals("bar", prop.read());

        Property newProp = new PropertyImpl(prop.def(), "baz");
        mutablePojoAspect.put(newProp);

        Property prop2 = mutablePojoAspect.get("string");
        assertEquals("baz", prop2.read());

    }

}