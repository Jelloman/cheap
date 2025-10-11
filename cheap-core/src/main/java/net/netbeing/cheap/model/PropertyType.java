/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.model;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Defines the supported data types for properties in the Cheap model.
 * Each property type specifies storage characteristics, validation rules,
 * and the corresponding Java class used for value representation.
 * 
 * <p>Property types range from basic primitives to complex streaming types,
 * providing comprehensive data storage capabilities while maintaining
 * type safety and efficient serialization.</p>
 */
public enum PropertyType
{
    /**
     * 64-bit signed integer values. Uses Java Long class for representation
     * to ensure full range support and consistency across platforms.
     */
    Integer("INT", Long.class),
    
    /**
     * 64-bit floating-point values (double precision). Provides standard
     * IEEE 754 double-precision floating-point arithmetic.
     */
    Float("FLT", Double.class),
    
    /**
     * Boolean values supporting true, false, or null states. The null state
     * allows for three-valued logic in data storage and queries.
     */
    Boolean("BLN", Boolean.class),
    
    /**
     * String values with length limited to 8192 characters, processed atomically.
     * Suitable for short text fields, identifiers, and labels where size
     * limits ensure efficient storage and retrieval.
     */
    String("STR", String.class),
    
    /**
     * Text values with unlimited length, processed atomically. Suitable for
     * large text content, documents, and descriptions where size flexibility
     * is more important than storage efficiency.
     */
    Text("TXT", String.class),

    /**
     * Arbitrary precision integer values with unlimited size. Stored as
     * strings to avoid platform-specific size limitations and ensure
     * exact precision for mathematical operations.
     */
    BigInteger("BGI", BigInteger.class),

    /**
     * Arbitrary precision floating-point values with unlimited size. Stored
     * as strings to avoid platform-specific size limitations and ensure
     * exact precision for mathematical operations.
     */
    BigDecimal("BGF", BigDecimal.class),

    /**
     * Date and time values stored as ISO-8601 formatted strings. This ensures
     * timezone information is preserved and provides human-readable storage
     * with standardized parsing support.
     */
    DateTime("DAT", ZonedDateTime.class),
    
    /**
     * Uniform Resource Identifier values following RFC 3986 specification.
     * Stored as strings with application-level conversion to/from URI/URL objects
     * to maintain flexibility in handling various URI schemes.
     */
    URI("URI", URI.class),
    
    /**
     * Universally Unique Identifier values following RFC 4122 specification.
     * Stored as strings with application-level conversion to/from UUID objects
     * to ensure consistent representation across different systems.
     */
    UUID("UID", UUID.class),
    
    /**
     * Character Large Object (CLOB) for streaming text data. Represented by
     * a String.
     */
    CLOB("CLB", String.class),
    
    /**
     * Binary Large Object (BLOB) for streaming binary data. Represented by
     * a byte array.
     */
    BLOB("BLB", byte[].class)
    ;

    /**
     * Container for CLOB stream operations, providing both input (Reader) and
     * output (Writer) stream access for character-based large object data.
     * 
     * @param reader stream for reading character data from the CLOB
     * @param writer stream for writing character data to the CLOB
     */
    public record ReaderWriter(Reader reader, Writer writer) {}

    /**
     * Container for BLOB stream operations, providing both input (InputStream) and
     * output (OutputStream) stream access for binary large object data.
     * 
     * @param input stream for reading binary data from the BLOB
     * @param output stream for writing binary data to the BLOB
     */
    public record InputOutput(InputStream input, OutputStream output) {}

    private final String typeCode;
    private final Class<?> javaClass;

    PropertyType(String typeCode, Class<?> javaClass)
    {
        this.typeCode = typeCode;
        this.javaClass = javaClass;
    }

    /**
     * Returns the short string code that identifies this property type.
     * These codes are used for serialization, storage, and compact representation.
     *
     * @return the three-character type code for this property type, never null
     */
    public String typeCode()
    {
        return typeCode;
    }

    /**
     * Returns the Java class used to represent values of this property type.
     * This class is used for type checking and serialization operations.
     *
     * @return the Java class that represents this property type's values, never null
     */
    public Class<?> getJavaClass()
    {
        return javaClass;
    }

    public String toString()
    {
        return typeCode;
    }

    /**
     * Attempts to coerce the given value to the type represented by this PropertyType.
     * If the value is already of the correct type, it is returned as-is.
     * If the value is a List or array (for multivalued properties), each element is coerced.
     * If the value can be converted to the target type, the converted value is returned.
     * If conversion is not possible, an IllegalArgumentException is thrown.
     *
     * @param value the value to coerce to this property type
     * @return the coerced value
     * @throws IllegalArgumentException if the value cannot be coerced to this type
     */
    public Object coerce(Object value)
    {
        switch (value) {
            case null -> {
                return null;
            }

            // Handle byte[] specially - it's the native type for BLOB, not a multivalued property
            case byte[] bytes -> {
                return coerceSingleValue(value);
            }

            // Handle collections (for multivalued properties) - coerce each element
            case List<?> list -> {
                List<Object> coercedList = new ArrayList<>(list.size());
                for (Object element : list) {
                    coercedList.add(coerceSingleValue(element));
                }
                return coercedList;
            }
            default -> {
            }
        }

        // Handle arrays (except byte[]) - coerce each element
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> coercedList = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(value, i);
                coercedList.add(coerceSingleValue(element));
            }
            return coercedList;
        }

        // Single value
        return coerceSingleValue(value);
    }

    /**
     * Coerces a single (non-collection) value to the type represented by this PropertyType.
     *
     * @param value the single value to coerce
     * @return the coerced value
     * @throws IllegalArgumentException if the value cannot be coerced to this type
     */
    private Object coerceSingleValue(Object value)
    {
        if (value == null) {
            return null;
        }

        // If value is already the correct type, return it
        if (javaClass.isInstance(value)) {
            return value;
        }

        // Attempt type-specific coercion
        try {
            return switch (this) {
                case Integer -> coerceToLong(value);
                case Float -> coerceToDouble(value);
                case Boolean -> coerceToBoolean(value);
                case String, Text, CLOB -> coerceToString(value);
                case BigInteger -> coerceToBigInteger(value);
                case BigDecimal -> coerceToBigDecimal(value);
                case DateTime -> coerceToZonedDateTime(value);
                case URI -> coerceToURI(value);
                case UUID -> coerceToUUID(value);
                case BLOB -> coerceToByteArray(value);
            };
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Cannot coerce value of type " + value.getClass().getName() +
                " to " + this.name() + " (Java type: " + javaClass.getName() + ")", e);
        }
    }

    private static Long coerceToLong(Object value)
    {
        if (value instanceof Number num) {
            return num.longValue();
        }
        if (value instanceof java.lang.String str) {
            return Long.parseLong(str);
        }
        throw new IllegalArgumentException("Cannot coerce to Long");
    }

    private static Double coerceToDouble(Object value)
    {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        if (value instanceof java.lang.String str) {
            return Double.parseDouble(str);
        }
        throw new IllegalArgumentException("Cannot coerce to Double");
    }

    private static java.lang.Boolean coerceToBoolean(Object value)
    {
        if (value instanceof java.lang.String str) {
            return java.lang.Boolean.parseBoolean(str);
        }
        if (value instanceof Number num) {
            return num.intValue() != 0;
        }
        throw new IllegalArgumentException("Cannot coerce to Boolean");
    }

    private static java.lang.String coerceToString(Object value)
    {
        return value.toString();
    }

    private static BigInteger coerceToBigInteger(Object value)
    {
        if (value instanceof BigDecimal bd) {
            return bd.toBigInteger();
        }
        if (value instanceof Number num) {
            return java.math.BigInteger.valueOf(num.longValue());
        }
        if (value instanceof java.lang.String str) {
            return new BigInteger(str);
        }
        throw new IllegalArgumentException("Cannot coerce to BigInteger");
    }

    private static BigDecimal coerceToBigDecimal(Object value)
    {
        if (value instanceof BigInteger bi) {
            return new BigDecimal(bi);
        }
        if (value instanceof Number num) {
            return java.math.BigDecimal.valueOf(num.doubleValue());
        }
        if (value instanceof java.lang.String str) {
            return new BigDecimal(str);
        }
        throw new IllegalArgumentException("Cannot coerce to BigDecimal");
    }

    private static ZonedDateTime coerceToZonedDateTime(Object value)
    {
        if (value instanceof java.lang.String str) {
            return ZonedDateTime.parse(str);
        }
        if (value instanceof Timestamp timestamp) {
            return ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault());
        }
        if (value instanceof Instant instant) {
            return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        }
        throw new IllegalArgumentException("Cannot coerce to ZonedDateTime");
    }

    private static java.net.URI coerceToURI(Object value)
    {
        if (value instanceof java.lang.String str) {
            return java.net.URI.create(str);
        }
        throw new IllegalArgumentException("Cannot coerce to URI");
    }

    private static UUID coerceToUUID(Object value)
    {
        if (value instanceof java.lang.String str) {
            return java.util.UUID.fromString(str);
        }
        throw new IllegalArgumentException("Cannot coerce to UUID");
    }

    private static byte[] coerceToByteArray(Object value)
    {
        if (value instanceof java.lang.String str) {
            return str.getBytes();
        }
        throw new IllegalArgumentException("Cannot coerce to byte array");
    }
}
