package net.netbeing.cheap.util;

import com.google.common.collect.ImmutableList;
import net.netbeing.cheap.impl.basic.PropertyDefImpl;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the PropertyValueAdapter class.
 * Tests value coercion for all property types with various input types.
 */
class PropertyValueAdapterTest
{
    private final PropertyValueAdapter adapter = new PropertyValueAdapter();
    private final PropertyValueAdapter utcAdapter = new PropertyValueAdapter(TimeZone.getTimeZone("UTC"));

    // Test Long coercion
    @Test
    void testCoerceToLong_fromNumber()
    {
        assertEquals(42L, adapter.coerceToLong(42));
        assertEquals(42L, adapter.coerceToLong(42L));
        assertEquals(42L, adapter.coerceToLong(42.9));
        assertEquals(42L, adapter.coerceToLong(42.9f));
        assertEquals(42L, adapter.coerceToLong(new BigInteger("42")));
    }

    @Test
    void testCoerceToLong_fromString()
    {
        assertEquals(42L, adapter.coerceToLong("42"));
        assertEquals(-123L, adapter.coerceToLong("-123"));
    }

    @Test
    void testCoerceToLong_invalidInput()
    {
        assertThrows(IllegalArgumentException.class, () -> adapter.coerceToLong("not a number"));
        assertThrows(IllegalArgumentException.class, () -> adapter.coerceToLong(true));
    }

    // Test Double coercion
    @Test
    void testCoerceToDouble_fromNumber()
    {
        assertEquals(42.5, adapter.coerceToDouble(42.5));
        assertEquals(42.0, adapter.coerceToDouble(42));
        assertEquals(42.0, adapter.coerceToDouble(42L));
        assertEquals(42.5, adapter.coerceToDouble(42.5f));
    }

    @Test
    void testCoerceToDouble_fromString()
    {
        assertEquals(42.5, adapter.coerceToDouble("42.5"));
        assertEquals(-123.456, adapter.coerceToDouble("-123.456"));
    }

    @Test
    void testCoerceToDouble_invalidInput()
    {
        assertThrows(IllegalArgumentException.class, () -> adapter.coerceToDouble("not a number"));
        assertThrows(IllegalArgumentException.class, () -> adapter.coerceToDouble(true));
    }

    // Test Boolean coercion
    @Test
    void testCoerceToBoolean_fromString()
    {
        assertEquals(Boolean.TRUE, adapter.coerceToBoolean("true"));
        assertEquals(Boolean.FALSE, adapter.coerceToBoolean("false"));
        assertEquals(Boolean.FALSE, adapter.coerceToBoolean("anything else"));
    }

    @Test
    void testCoerceToBoolean_fromNumber()
    {
        assertEquals(Boolean.TRUE, adapter.coerceToBoolean(1));
        assertEquals(Boolean.FALSE, adapter.coerceToBoolean(0));
        assertEquals(Boolean.TRUE, adapter.coerceToBoolean(42));
        assertEquals(Boolean.TRUE, adapter.coerceToBoolean(-1));
    }

    @Test
    void testCoerceToBoolean_invalidInput()
    {
        assertThrows(IllegalArgumentException.class, () -> adapter.coerceToBoolean(UUID.randomUUID()));
    }

    // Test String coercion
    @Test
    void testCoerceToString()
    {
        assertEquals("hello", adapter.coerceToString("hello"));
        assertEquals("42", adapter.coerceToString(42));
        assertEquals("true", adapter.coerceToString(true));
        assertEquals("42.5", adapter.coerceToString(42.5));
    }

    // Test BigInteger coercion
    @Test
    void testCoerceToBigInteger_fromNumber()
    {
        assertEquals(new BigInteger("42"), adapter.coerceToBigInteger(42));
        assertEquals(new BigInteger("42"), adapter.coerceToBigInteger(42L));
        assertEquals(new BigInteger("42"), adapter.coerceToBigInteger(new BigDecimal("42.9")));
    }

    @Test
    void testCoerceToBigInteger_fromString()
    {
        assertEquals(new BigInteger("12345678901234567890"),
                     adapter.coerceToBigInteger("12345678901234567890"));
    }

    @Test
    void testCoerceToBigInteger_invalidInput()
    {
        assertThrows(IllegalArgumentException.class, () -> adapter.coerceToBigInteger("not a number"));
        assertThrows(IllegalArgumentException.class, () -> adapter.coerceToBigInteger(true));
    }

    // Test BigDecimal coercion
    @Test
    void testCoerceToBigDecimal_fromNumber()
    {
        assertEquals(new BigDecimal("42.5"), adapter.coerceToBigDecimal(42.5));
        assertEquals(new BigDecimal("42.0"), adapter.coerceToBigDecimal(42));
        assertEquals(new BigDecimal("42"), adapter.coerceToBigDecimal(new BigInteger("42")));
    }

    @Test
    void testCoerceToBigDecimal_fromString()
    {
        assertEquals(new BigDecimal("123.456"), adapter.coerceToBigDecimal("123.456"));
    }

    @Test
    void testCoerceToBigDecimal_invalidInput()
    {
        assertThrows(IllegalArgumentException.class, () -> adapter.coerceToBigDecimal("not a number"));
        assertThrows(IllegalArgumentException.class, () -> adapter.coerceToBigDecimal(true));
    }

    // Test ZonedDateTime coercion
    @Test
    void testCoerceToZonedDateTime_fromString()
    {
        ZonedDateTime expected = ZonedDateTime.parse("2025-01-15T10:30:00Z");
        assertEquals(expected, adapter.coerceToZonedDateTime("2025-01-15T10:30:00Z"));
    }

    @Test
    void testCoerceToZonedDateTime_fromTimestamp()
    {
        Instant instant = Instant.parse("2025-01-15T10:30:00Z");
        Timestamp timestamp = Timestamp.from(instant);

        ZonedDateTime result = utcAdapter.coerceToZonedDateTime(timestamp);
        assertEquals(instant, result.toInstant());
    }

    @Test
    void testCoerceToZonedDateTime_fromInstant()
    {
        Instant instant = Instant.parse("2025-01-15T10:30:00Z");

        ZonedDateTime result = utcAdapter.coerceToZonedDateTime(instant);
        assertEquals(instant, result.toInstant());
        assertEquals(ZoneId.of("UTC"), result.getZone());
    }

    @Test
    void testCoerceToZonedDateTime_respectsTimeZone()
    {
        TimeZone estTimeZone = TimeZone.getTimeZone("America/New_York");
        PropertyValueAdapter estAdapter = new PropertyValueAdapter(estTimeZone);

        Instant instant = Instant.parse("2025-01-15T10:30:00Z");
        ZonedDateTime result = estAdapter.coerceToZonedDateTime(instant);

        assertEquals(instant, result.toInstant());
        assertEquals(estTimeZone.toZoneId(), result.getZone());
    }

    @Test
    void testCoerceToZonedDateTime_invalidInput()
    {
        // When called directly, ZonedDateTime.parse throws DateTimeParseException
        // When called through coerce() with PropertyDef, it gets wrapped in IllegalArgumentException
        PropertyDef dateProp = new PropertyDefImpl("date", PropertyType.DateTime);
        assertThrows(IllegalArgumentException.class, () -> adapter.coerce(dateProp, "not a date"));
        assertThrows(IllegalArgumentException.class, () -> adapter.coerce(dateProp, 42));
    }

    // Test URI coercion
    @Test
    void testCoerceToURI_fromString()
    {
        URI expected = URI.create("https://example.com/path");
        assertEquals(expected, adapter.coerceToURI("https://example.com/path"));
    }

    @Test
    void testCoerceToURI_invalidInput()
    {
        assertThrows(IllegalArgumentException.class, () -> adapter.coerceToURI(42));
    }

    // Test UUID coercion
    @Test
    void testCoerceToUUID_fromString()
    {
        UUID expected = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertEquals(expected, adapter.coerceToUUID("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    void testCoerceToUUID_invalidInput()
    {
        assertThrows(IllegalArgumentException.class, () -> adapter.coerceToUUID("not a uuid"));
        assertThrows(IllegalArgumentException.class, () -> adapter.coerceToUUID(42));
    }

    // Test byte array coercion
    @Test
    void testCoerceToByteArray_fromString()
    {
        String input = "hello";
        assertArrayEquals(input.getBytes(), adapter.coerceToByteArray(input));
    }

    @Test
    void testCoerceToByteArray_invalidInput()
    {
        assertThrows(IllegalArgumentException.class, () -> adapter.coerceToByteArray(42));
    }

    // Test coerce method with PropertyDef
    @Test
    void testCoerce_alreadyCorrectType()
    {
        PropertyDef propDef = new PropertyDefImpl("test", PropertyType.Integer);
        Long value = 42L;

        Object result = adapter.coerce(propDef, value);
        assertSame(value, result);
    }

    @Test
    void testCoerce_coercionNeeded()
    {
        PropertyDef propDef = new PropertyDefImpl("test", PropertyType.Integer);

        Object result = adapter.coerce(propDef, 42);
        assertEquals(42L, result);
    }

    @Test
    void testCoerce_nullValue_nullable()
    {
        PropertyDef propDef = new PropertyDefImpl("test", PropertyType.String, true, true, true, true, false);

        Object result = adapter.coerce(propDef, null);
        assertNull(result);
    }

    @Test
    void testCoerce_nullValue_notNullable()
    {
        PropertyDef propDef = new PropertyDefImpl("test", PropertyType.String, true, true, false, true, false);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> adapter.coerce(propDef, null)
        );
        assertTrue(exception.getMessage().contains("cannot be null"));
    }

    // Test multivalued properties
    @Test
    void testCoerce_multivalued_fromList()
    {
        PropertyDef propDef = new PropertyDefImpl("test", PropertyType.Integer, true, true, true, true, true);
        List<Integer> input = ImmutableList.of(1, 2, 3);

        Object result = adapter.coerce(propDef, input);
        assertInstanceOf(List.class, result);

        @SuppressWarnings("unchecked")
        List<Long> resultList = (List<Long>) result;
        assertEquals(3, resultList.size());
        assertEquals(1L, resultList.get(0));
        assertEquals(2L, resultList.get(1));
        assertEquals(3L, resultList.get(2));
    }

    @Test
    void testCoerce_multivalued_fromArray()
    {
        PropertyDef propDef = new PropertyDefImpl("test", PropertyType.Integer, true, true, true, true, true);
        Integer[] input = {1, 2, 3};

        Object result = adapter.coerce(propDef, input);
        assertInstanceOf(List.class, result);

        @SuppressWarnings("unchecked")
        List<Long> resultList = (List<Long>) result;
        assertEquals(3, resultList.size());
        assertEquals(1L, resultList.get(0));
        assertEquals(2L, resultList.get(1));
        assertEquals(3L, resultList.get(2));
    }

    @Test
    void testCoerce_multivalued_listNoCoercionNeeded()
    {
        PropertyDef propDef = new PropertyDefImpl("test", PropertyType.Integer, true, true, true, true, true);
        List<Long> input = ImmutableList.of(1L, 2L, 3L);

        Object result = adapter.coerce(propDef, input);
        assertSame(input, result); // Should return same list when no coercion needed
    }

    @Test
    void testCoerce_multivalued_invalidInput()
    {
        PropertyDef propDef = new PropertyDefImpl("test", PropertyType.Integer, true, true, true, true, true);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> adapter.coerce(propDef, "not a collection")
        );
        assertTrue(exception.getMessage().contains("multivalued"));
    }

    @Test
    void testCoerce_singleValued_cannotPassList()
    {
        PropertyDef propDef = new PropertyDefImpl("test", PropertyType.Integer);
        List<Integer> input = ImmutableList.of(1, 2, 3);

        // For single-valued properties, lists are coerced to the target type
        // which will fail for Integer type
        assertThrows(IllegalArgumentException.class, () -> adapter.coerce(propDef, input));
    }

    // Test all PropertyTypes
    @Test
    void testCoerce_allPropertyTypes()
    {
        // Integer
        PropertyDef intProp = new PropertyDefImpl("int", PropertyType.Integer);
        assertEquals(42L, adapter.coerce(intProp, 42));

        // Float
        PropertyDef floatProp = new PropertyDefImpl("float", PropertyType.Float);
        assertEquals(42.5, adapter.coerce(floatProp, 42.5));

        // Boolean
        PropertyDef boolProp = new PropertyDefImpl("bool", PropertyType.Boolean);
        assertEquals(Boolean.TRUE, adapter.coerce(boolProp, "true"));

        // String
        PropertyDef stringProp = new PropertyDefImpl("string", PropertyType.String);
        assertEquals("hello", adapter.coerce(stringProp, "hello"));

        // Text
        PropertyDef textProp = new PropertyDefImpl("text", PropertyType.Text);
        assertEquals("hello", adapter.coerce(textProp, "hello"));

        // CLOB
        PropertyDef clobProp = new PropertyDefImpl("clob", PropertyType.CLOB);
        assertEquals("hello", adapter.coerce(clobProp, "hello"));

        // BigInteger
        PropertyDef bigIntProp = new PropertyDefImpl("bigint", PropertyType.BigInteger);
        assertEquals(new BigInteger("42"), adapter.coerce(bigIntProp, 42));

        // BigDecimal
        PropertyDef bigDecProp = new PropertyDefImpl("bigdec", PropertyType.BigDecimal);
        assertEquals(new BigDecimal("42.5"), adapter.coerce(bigDecProp, 42.5));

        // DateTime
        PropertyDef dateProp = new PropertyDefImpl("date", PropertyType.DateTime);
        ZonedDateTime expected = ZonedDateTime.parse("2025-01-15T10:30:00Z");
        assertEquals(expected, adapter.coerce(dateProp, "2025-01-15T10:30:00Z"));

        // URI
        PropertyDef uriProp = new PropertyDefImpl("uri", PropertyType.URI);
        assertEquals(URI.create("https://example.com"), adapter.coerce(uriProp, "https://example.com"));

        // UUID
        PropertyDef uuidProp = new PropertyDefImpl("uuid", PropertyType.UUID);
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, adapter.coerce(uuidProp, uuid.toString()));

        // BLOB
        PropertyDef blobProp = new PropertyDefImpl("blob", PropertyType.BLOB);
        assertArrayEquals("hello".getBytes(), (byte[]) adapter.coerce(blobProp, "hello"));
    }

    // Test TimeZone getter/setter
    @Test
    void testGetSetTimeZone()
    {
        PropertyValueAdapter testAdapter = new PropertyValueAdapter();
        assertNotNull(testAdapter.getTimeZone());

        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        testAdapter.setTimeZone(utcZone);
        assertEquals(utcZone, testAdapter.getTimeZone());
    }

    // Test edge cases
    @Test
    void testCoerce_emptyList()
    {
        PropertyDef propDef = new PropertyDefImpl("test", PropertyType.Integer, true, true, true, true, true);
        List<Integer> input = ImmutableList.of();

        Object result = adapter.coerce(propDef, input);
        assertInstanceOf(List.class, result);
        assertTrue(((List<?>) result).isEmpty());
    }

    @Test
    void testCoerce_primitiveArray()
    {
        PropertyDef propDef = new PropertyDefImpl("test", PropertyType.Integer, true, true, true, true, true);
        int[] input = {1, 2, 3};

        Object result = adapter.coerce(propDef, input);
        assertInstanceOf(List.class, result);

        @SuppressWarnings("unchecked")
        List<Long> resultList = (List<Long>) result;
        assertEquals(3, resultList.size());
        assertEquals(1L, resultList.getFirst());
    }
}
