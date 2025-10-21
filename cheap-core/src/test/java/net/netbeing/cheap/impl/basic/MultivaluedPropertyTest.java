package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.MutableAspectDef;
import net.netbeing.cheap.model.Property;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import net.netbeing.cheap.util.CheapFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for multivalued properties in the Cheap system.
 * Tests reading and writing properties that hold multiple values (List&lt;T&gt;).
 */
class MultivaluedPropertyTest
{
    private final CheapFactory factory = new CheapFactory();

    @Test
    void testMultivaluedStringProperty_CreateAndRead()
    {
        PropertyDef propDef = factory.createPropertyDef("tags", PropertyType.String,
            true, true, true, true, true);

        assertTrue(propDef.isMultivalued());
        assertEquals(PropertyType.String, propDef.type());

        List<String> tags = List.of("java", "cheap", "data");
        Property property = factory.createProperty(propDef, tags);

        assertNotNull(property);
        assertEquals(propDef, property.def());

        Object value = property.unsafeRead();
        assertInstanceOf(List.class, value);

        @SuppressWarnings("unchecked")
        List<String> readTags = (List<String>) value;
        assertEquals(3, readTags.size());
        assertEquals("java", readTags.get(0));
        assertEquals("cheap", readTags.get(1));
        assertEquals("data", readTags.get(2));
    }

    @Test
    void testMultivaluedIntegerProperty_CreateAndRead()
    {
        PropertyDef propDef = factory.createPropertyDef("scores", PropertyType.Integer,
            true, true, true, true, true);

        assertTrue(propDef.isMultivalued());
        assertEquals(PropertyType.Integer, propDef.type());

        List<Long> scores = List.of(100L, 95L, 87L, 92L);
        Property property = factory.createProperty(propDef, scores);

        assertNotNull(property);

        @SuppressWarnings("unchecked")
        List<Long> readScores = property.unsafeReadAs();
        assertEquals(4, readScores.size());
        assertEquals(100L, readScores.get(0));
        assertEquals(95L, readScores.get(1));
        assertEquals(87L, readScores.get(2));
        assertEquals(92L, readScores.get(3));
    }

    @Test
    void testMultivaluedBooleanProperty_CreateAndRead()
    {
        PropertyDef propDef = factory.createPropertyDef("flags", PropertyType.Boolean,
            true, true, true, true, true);

        assertTrue(propDef.isMultivalued());

        List<Boolean> flags = List.of(true, false, true, true);
        Property property = factory.createProperty(propDef, flags);

        @SuppressWarnings("unchecked")
        List<Boolean> readFlags = property.readAs(List.class);
        assertEquals(4, readFlags.size());
        assertTrue(readFlags.get(0));
        assertFalse(readFlags.get(1));
        assertTrue(readFlags.get(2));
        assertTrue(readFlags.get(3));
    }

    @Test
    void testMultivaluedFloatProperty_CreateAndRead()
    {
        PropertyDef propDef = factory.createPropertyDef("temperatures", PropertyType.Float,
            true, true, true, true, true);

        List<Double> temps = List.of(98.6, 99.1, 97.8, 98.2);
        Property property = factory.createProperty(propDef, temps);

        @SuppressWarnings("unchecked")
        List<Double> readTemps = (List<Double>) property.read();
        assertEquals(4, readTemps.size());
        assertEquals(98.6, readTemps.get(0), 0.01);
        assertEquals(99.1, readTemps.get(1), 0.01);
    }

    @Test
    void testMultivaluedUUIDProperty_CreateAndRead()
    {
        PropertyDef propDef = factory.createPropertyDef("identifiers", PropertyType.UUID,
            true, true, true, true, true);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<UUID> ids = List.of(id1, id2);

        Property property = factory.createProperty(propDef, ids);

        @SuppressWarnings("unchecked")
        List<UUID> readIds = (List<UUID>) property.read();
        assertEquals(2, readIds.size());
        assertEquals(id1, readIds.get(0));
        assertEquals(id2, readIds.get(1));
    }

    @Test
    void testMultivaluedURIProperty_CreateAndRead() throws Exception
    {
        PropertyDef propDef = factory.createPropertyDef("links", PropertyType.URI,
            true, true, true, true, true);

        URI uri1 = new URI("https://example.com");
        URI uri2 = new URI("https://test.com");
        List<URI> uris = List.of(uri1, uri2);

        Property property = factory.createProperty(propDef, uris);

        @SuppressWarnings("unchecked")
        List<URI> readUris = (List<URI>) property.read();
        assertEquals(2, readUris.size());
        assertEquals(uri1, readUris.get(0));
        assertEquals(uri2, readUris.get(1));
    }

    @Test
    void testMultivaluedBigIntegerProperty_CreateAndRead()
    {
        PropertyDef propDef = factory.createPropertyDef("bigNumbers", PropertyType.BigInteger,
            true, true, true, true, true);

        BigInteger big1 = new BigInteger("12345678901234567890");
        BigInteger big2 = new BigInteger("98765432109876543210");
        List<BigInteger> bigInts = List.of(big1, big2);

        Property property = factory.createProperty(propDef, bigInts);

        @SuppressWarnings("unchecked")
        List<BigInteger> readBigInts = (List<BigInteger>) property.read();
        assertEquals(2, readBigInts.size());
        assertEquals(big1, readBigInts.get(0));
        assertEquals(big2, readBigInts.get(1));
    }

    @Test
    void testMultivaluedBigDecimalProperty_CreateAndRead()
    {
        PropertyDef propDef = factory.createPropertyDef("prices", PropertyType.BigDecimal,
            true, true, true, true, true);

        BigDecimal price1 = new BigDecimal("123.45");
        BigDecimal price2 = new BigDecimal("678.90");
        List<BigDecimal> prices = List.of(price1, price2);

        Property property = factory.createProperty(propDef, prices);

        @SuppressWarnings("unchecked")
        List<BigDecimal> readPrices = (List<BigDecimal>) property.read();
        assertEquals(2, readPrices.size());
        assertEquals(price1, readPrices.get(0));
        assertEquals(price2, readPrices.get(1));
    }

    @Test
    void testMultivaluedDateTimeProperty_CreateAndRead()
    {
        PropertyDef propDef = factory.createPropertyDef("timestamps", PropertyType.DateTime,
            true, true, true, true, true);

        ZonedDateTime time1 = ZonedDateTime.now();
        ZonedDateTime time2 = time1.plusHours(1);
        List<ZonedDateTime> times = List.of(time1, time2);

        Property property = factory.createProperty(propDef, times);

        @SuppressWarnings("unchecked")
        List<ZonedDateTime> readTimes = (List<ZonedDateTime>) property.read();
        assertEquals(2, readTimes.size());
        assertEquals(time1, readTimes.get(0));
        assertEquals(time2, readTimes.get(1));
    }

    @Test
    void testMultivaluedProperty_EmptyList()
    {
        PropertyDef propDef = factory.createPropertyDef("tags", PropertyType.String,
            true, true, true, true, true);

        List<String> emptyList = List.of();
        Property property = factory.createProperty(propDef, emptyList);

        @SuppressWarnings("unchecked")
        List<String> readList = (List<String>) property.read();
        assertNotNull(readList);
        assertTrue(readList.isEmpty());
    }

    @Test
    void testMultivaluedProperty_NullValue_Nullable()
    {
        PropertyDef propDef = factory.createPropertyDef("tags", PropertyType.String,
            true, true, true, true, true); // nullable=true

        Property property = factory.createProperty(propDef, null);

        assertNull(property.read());
    }

    @Test
    void testMultivaluedPropertyInAspect_WriteAndRead()
    {
        Entity entity = factory.createEntity();

        PropertyDef tagsDef = factory.createPropertyDef("tags", PropertyType.String,
            true, true, true, true, true);
        PropertyDef scoresDef = factory.createPropertyDef("scores", PropertyType.Integer,
            true, true, true, true, true);

        MutableAspectDef aspectDef = (MutableAspectDef) factory.createMutableAspectDef("testAspect");
        aspectDef.add(tagsDef);
        aspectDef.add(scoresDef);

        Aspect aspect = factory.createObjectMapAspect(entity, aspectDef);

        List<String> tags = List.of("test", "multivalued");
        List<Long> scores = List.of(100L, 95L);

        Property tagsProperty = factory.createProperty(tagsDef, tags);
        Property scoresProperty = factory.createProperty(scoresDef, scores);

        aspect.put(tagsProperty);
        aspect.put(scoresProperty);

        assertTrue(aspect.contains("tags"));
        assertTrue(aspect.contains("scores"));

        @SuppressWarnings("unchecked")
        List<String> readTags = (List<String>) aspect.readObj("tags");
        assertEquals(2, readTags.size());
        assertEquals("test", readTags.get(0));
        assertEquals("multivalued", readTags.get(1));

        @SuppressWarnings("unchecked")
        List<Long> readScores = (List<Long>) aspect.readObj("scores");
        assertEquals(2, readScores.size());
        assertEquals(100L, readScores.get(0));
        assertEquals(95L, readScores.get(1));
    }

    @Test
    void testMultivaluedProperty_ValidationWithList_Succeeds()
    {
        PropertyDef propDef = factory.createPropertyDef("tags", PropertyType.String,
            true, true, true, true, true);

        List<String> tags = List.of("java", "cheap");

        // Should return true because validation now correctly handles multivalued properties
        boolean isValid = propDef.validatePropertyValue(tags, false);
        assertTrue(isValid, "List value should be valid for multivalued property");
    }

    @Test
    void testMultivaluedProperty_ValidationWithExceptions_Succeeds()
    {
        PropertyDef propDef = factory.createPropertyDef("tags", PropertyType.String,
            true, true, true, true, true);

        List<String> tags = List.of("java", "cheap");

        // Should not throw exception - validation should pass
        assertDoesNotThrow(() -> propDef.validatePropertyValue(tags, true),
            "Validation should succeed for List values on multivalued properties");
    }

    @Test
    void testMultivaluedProperty_ValidationWithWrongElementType_Fails()
    {
        PropertyDef propDef = factory.createPropertyDef("numbers", PropertyType.Integer,
            true, true, true, true, true);

        // Create a list with wrong element type
        List<String> wrongTypeList = List.of("not", "numbers");

        // Should return false
        boolean isValid = propDef.validatePropertyValue(wrongTypeList, false);
        assertFalse(isValid, "Validation should fail when list elements have wrong type");

        // With exceptions enabled, should throw
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> propDef.validatePropertyValue(wrongTypeList, true)
        );

        assertTrue(exception.getMessage().contains("expects List<Long>"),
            "Exception should mention expected type: " + exception.getMessage());
    }

    @Test
    void testMultivaluedProperty_ValidationWithNonListValue_Fails()
    {
        PropertyDef propDef = factory.createPropertyDef("tags", PropertyType.String,
            true, true, true, true, true);

        // Pass a single String instead of a List
        String singleValue = "not a list";

        // Should return false
        boolean isValid = propDef.validatePropertyValue(singleValue, false);
        assertFalse(isValid, "Validation should fail when non-List value provided for multivalued property");

        // With exceptions enabled, should throw
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> propDef.validatePropertyValue(singleValue, true)
        );

        assertTrue(exception.getMessage().contains("multivalued") && exception.getMessage().contains("expects a List"),
            "Exception should mention multivalued and List: " + exception.getMessage());
    }

    @Test
    void testSingleValuedProperty_NotMultivalued()
    {
        PropertyDef propDef = factory.createPropertyDef("name", PropertyType.String,
            true, true, true, true, false); // isMultivalued = false

        assertFalse(propDef.isMultivalued());

        String name = "test";
        Property property = factory.createProperty(propDef, name);

        assertEquals("test", property.read());
        assertFalse(property.read() instanceof List, "Single-valued property should not return a List");
    }

    @Test
    void testMultivaluedProperty_ReadOnlyPropertyDef()
    {
        // Note: createReadOnlyPropertyDef doesn't support multivalued properties
        // Use the full constructor instead
        PropertyDef propDef = factory.createPropertyDef("tags", PropertyType.String,
            null, false, true, false, true, true, true);

        assertTrue(propDef.isMultivalued());
        assertTrue(propDef.isReadable());
        assertFalse(propDef.isWritable());

        List<String> tags = List.of("readonly", "tags");
        Property property = factory.createProperty(propDef, tags);

        @SuppressWarnings("unchecked")
        List<String> readTags = (List<String>) property.read();
        assertEquals(2, readTags.size());
    }

    @Test
    void testMultivaluedProperty_WithDefaultValue()
    {
        List<String> defaultTags = List.of("default");
        PropertyDef propDef = factory.createPropertyDef("tags", PropertyType.String,
            defaultTags, true, true, true, true, true, true);

        assertTrue(propDef.hasDefaultValue());
        assertTrue(propDef.isMultivalued());

        @SuppressWarnings("unchecked")
        List<String> readDefault = (List<String>) propDef.defaultValue();
        assertEquals(1, readDefault.size());
        assertEquals("default", readDefault.get(0));
    }
}
