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

import com.google.common.collect.Maps;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
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

    private static final Map<String, PropertyType> LOOKUP = Maps.uniqueIndex(Arrays.asList(values()), PropertyType::typeCode);

    /**
     * Convert a type code string to a PropertyType.
     *
     * @param typeCode a 3-letter string code
     * @return the corresponding PropertyType
     * @throws IllegalArgumentException if the code is not recognized
     */
    public static PropertyType fromTypeCode(String typeCode)
    {
        return LOOKUP.get(typeCode.toUpperCase());
    }

    public String toString()
    {
        return typeCode;
    }
}
