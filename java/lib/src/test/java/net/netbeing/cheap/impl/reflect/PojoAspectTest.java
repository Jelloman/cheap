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

public class PojoAspectTest
{
    @Data @AllArgsConstructor
    public static class TestClass
    {
        private String string;
        private int integerPrimitive;
        private Integer integer;
        private char charPrimitive;
        private Character character;
        private UUID uuid;
        private URI uri;
        private LocalDateTime localDateTime;
    }

    final Entity testEntity = new BasicEntityImpl();
    final Catalog testCatalog = new CatalogImpl(new CatalogDefImpl(CatalogType.ROOT));

    PojoAspectDef def;
    TestClass pojo;
    PojoAspect<TestClass> pojoAspect;

    @BeforeEach
    void setUp()
    {
        def = new PojoAspectDef(TestClass.class);
    }

    @AfterEach
    void tearDown()
    {
        def = null;
        pojo = null;
        pojoAspect = null;
    }

    @Test
    void construct()
    {
        pojo = new TestClass("foo", 1, 2, 'a', 'b', UUID.randomUUID(), URI.create("http://example.com/"), LocalDateTime.now());
        pojoAspect = new PojoAspect<>(testCatalog, testEntity, def, pojo);
    }

    @Test
    void read()
    {
        pojo = new TestClass("foo", 1, 2, 'a', 'b', UUID.randomUUID(), URI.create("http://example.com/"), LocalDateTime.now());
        pojoAspect = new PojoAspect<>(testCatalog, testEntity, def, pojo);

        String value = pojoAspect.readAs("string", String.class);

        assertEquals("foo", value);
        assertEquals(pojo.getString(), value);
    }

    @Test
    void get()
    {
        pojo = new TestClass("foo", 1, 2, 'a', 'b', UUID.randomUUID(), URI.create("http://example.com/"), LocalDateTime.now());
        pojoAspect = new PojoAspect<>(testCatalog, testEntity, def, pojo);

        Property prop = pojoAspect.get("string");

        assertEquals("foo", prop.read());
    }

    @Test
    void put()
    {
        pojo = new TestClass("bar", 3, 4, 'c', 'd', UUID.randomUUID(), URI.create("http://example.com/foo/"), LocalDateTime.now().minusDays(1));
        pojoAspect = new PojoAspect<>(testCatalog, testEntity, def, pojo);

        Property prop = pojoAspect.get("string");
        assertEquals("bar", prop.read());

        //pojo.setString();
        Property newProp = new PropertyImpl(prop.def(), "baz");
        pojoAspect.put(newProp);

        Property prop2 = pojoAspect.get("string");
        assertEquals("baz", prop2.read());

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