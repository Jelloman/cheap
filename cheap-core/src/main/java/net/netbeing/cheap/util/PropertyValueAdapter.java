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

package net.netbeing.cheap.util;

import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Provides coercion functions to allow a wider variety of types to be
 * assigned to Cheap Properties.</p>
 */
public class PropertyValueAdapter
{
    private TimeZone timeZone;

    public PropertyValueAdapter()
    {
        this(TimeZone.getDefault());
    }

    public PropertyValueAdapter(TimeZone timeZone)
    {
        this.timeZone = timeZone;
    }

    public TimeZone getTimeZone()
    {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone)
    {
        this.timeZone = timeZone;
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
    public Object coerce(PropertyDef propDef, Object value)
    {
        if (value == null) {
            if (!propDef.isNullable()) {
                throw new IllegalArgumentException("Property '" + propDef.name() + "' cannot be null.");
            }
            return null;
        }

        if (!propDef.isMultivalued()) {
            return coerceSingleValue(propDef.type(), value);
        }

        if (value instanceof Collection<?> coll) {
            for (Object element : coll) {
                Object coerced = coerceSingleValue(propDef.type(), element);
                if (coerced != element) {
                    List<Object> coercedList = new ArrayList<>(coll.size());
                    for (Object element2 : coll) {
                        coercedList.add(coerceSingleValue(propDef.type(), element2));
                    }
                    return coercedList;
                }
            }
            // No element of the list needed to be coerced, so return the collection as-is
            return coll;

        }

        // Handle arrays - coerce each element and return in a List
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> coercedList = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(value, i);
                coercedList.add(coerceSingleValue(propDef.type(), element));
            }
            return coercedList;
        }

        throw new IllegalArgumentException("Property '" + propDef.name() + "' is multivalued and cannot be assigned from "
            + value.getClass().getName() + ".");
    }

    /**
     * Coerces a single (non-collection) value to the type represented by this PropertyType.
     *
     * @param value the single value to coerce
     * @return the coerced value
     * @throws IllegalArgumentException if the value cannot be coerced to this type
     */
    private Object coerceSingleValue(PropertyType type, @NotNull Object value)
    {
        // If value is already the correct type, return it
        if (type.getJavaClass().isInstance(value)) {
            return value;
        }

        // Attempt type-specific coercion
        try {
            return switch (type) {
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
            throw illegalArgument(type, value, e);
        }
    }

    private static IllegalArgumentException illegalArgument(PropertyType type, Object value)
    {
        return new IllegalArgumentException("Cannot coerce value of type " + value.getClass().getName() +
                " to PropertyType " + type.name() + " (Java type: " + type.getJavaClass().getName() + ")");
    }

    private static IllegalArgumentException illegalArgument(PropertyType type, Object value, Exception cause)
    {
        return new IllegalArgumentException("Cannot coerce value of type " + value.getClass().getName() +
                " to PropertyType " + type.name() + " (Java type: " + type.getJavaClass().getName() + ")", cause);
    }

    public Long coerceToLong(@NotNull Object value)
    {
        return switch (value) {
            case Number num -> num.longValue();
            case String str -> Long.parseLong(str);
            default -> throw illegalArgument(PropertyType.Integer, value);
        };
    }

    public Double coerceToDouble(@NotNull Object value)
    {
        return switch (value) {
            case Number num -> num.doubleValue();
            case String str -> Double.parseDouble(str);
            default -> throw illegalArgument(PropertyType.Float, value);
        };
    }

    public Boolean coerceToBoolean(@NotNull Object value)
    {
        return switch (value) {
            case Boolean b -> b;
            case String str -> Boolean.parseBoolean(str);
            case Number num -> num.intValue() != 0;
            default -> throw illegalArgument(PropertyType.Boolean, value);
        };
    }

    public String coerceToString(@NotNull Object value)
    {
        return value.toString();
    }

    public BigInteger coerceToBigInteger(@NotNull Object value)
    {
        return switch (value) {
            case BigInteger bi -> bi;
            case BigDecimal bd -> bd.toBigInteger();
            case Number num -> BigInteger.valueOf(num.longValue());
            case String str -> new BigInteger(str);
            default -> throw illegalArgument(PropertyType.BigInteger, value);
        };
    }

    public BigDecimal coerceToBigDecimal(@NotNull Object value)
    {
        return switch (value) {
            case BigDecimal bd -> bd;
            case BigInteger bi -> new BigDecimal(bi);
            case Number num -> BigDecimal.valueOf(num.doubleValue());
            case String str -> new BigDecimal(str);
            default -> throw illegalArgument(PropertyType.BigDecimal, value);
        };
    }

    public ZonedDateTime coerceToZonedDateTime(@NotNull Object value)
    {
        return switch (value) {
            case ZonedDateTime zdt -> zdt;
            case String str -> ZonedDateTime.parse(str);
            case java.sql.Date date -> date.toLocalDate().atStartOfDay(timeZone.toZoneId());
            case Timestamp timestamp -> ZonedDateTime.ofInstant(timestamp.toInstant(), timeZone.toZoneId());
            case Instant instant -> ZonedDateTime.ofInstant(instant, timeZone.toZoneId());
            case java.util.Date date -> ZonedDateTime.ofInstant(date.toInstant(), timeZone.toZoneId());
            default -> throw illegalArgument(PropertyType.DateTime, value);
        };
    }

    public URI coerceToURI(@NotNull Object value)
    {
        return switch (value) {
            case URI uri -> uri;
            case String str -> URI.create(str);
            default -> throw illegalArgument(PropertyType.URI, value);
        };
    }

    public UUID coerceToUUID(@NotNull Object value)
    {
        return switch (value) {
            case UUID uuid -> uuid;
            case String str -> UUID.fromString(str);
            default -> throw illegalArgument(PropertyType.UUID, value);
        };
    }

    public byte[] coerceToByteArray(@NotNull Object value)
    {
        return switch (value) {
            case byte[] a -> a;
            case String str -> HexFormat.of().parseHex(str);
            default -> throw illegalArgument(PropertyType.BLOB, value);
        };
    }

    /**
     * Convert a value object representing a specific property type to a String.
     * This method should generally be used to write values _into_ a database, rather
     * than when reading them out.
     *
     * BLOBs are converted to hex strings using HexFormat.
     *
     * @param value value object of the given type
     * @param type property type
     * @return String
     */
    public String convertValueToString(Object value, PropertyType type)
    {
        return switch (type) {
            case DateTime -> convertToTimestamp(value).toString();
            case BLOB -> (value instanceof byte[] ba) ? HexFormat.of().formatHex(ba) : value.toString();
            default -> value.toString();
        };
    }

    public Timestamp convertToTimestamp(Object value)
    {
        return switch (value) {
            case Timestamp timestamp -> timestamp;
            case java.sql.Date date -> Timestamp.from(date.toLocalDate().atStartOfDay(timeZone.toZoneId()).toInstant());
            case Date date -> new Timestamp(date.getTime());
            case Instant instant -> Timestamp.from(instant);
            case ZonedDateTime zonedDateTime -> Timestamp.from(zonedDateTime.toInstant());
            default -> throw new IllegalStateException("Unexpected value class for DateTime: " + value.getClass());
        };
    }


}
