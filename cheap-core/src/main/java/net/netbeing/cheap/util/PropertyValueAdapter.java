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

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
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

    PropertyValueAdapter()
    {
        this(TimeZone.getDefault());
    }

    PropertyValueAdapter(TimeZone timeZone)
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
    private Object coerceSingleValue(PropertyType type, Object value)
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


    public Long coerceToLong(Object value)
    {
        if (value instanceof Number num) {
            return num.longValue();
        }
        if (value instanceof String str) {
            return Long.parseLong(str);
        }
        throw illegalArgument(PropertyType.Integer, value);
    }

    public Double coerceToDouble(Object value)
    {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        if (value instanceof String str) {
            return Double.parseDouble(str);
        }
        throw illegalArgument(PropertyType.Float, value);
    }

    public Boolean coerceToBoolean(Object value)
    {
        if (value instanceof String str) {
            return java.lang.Boolean.parseBoolean(str);
        }
        if (value instanceof Number num) {
            return num.intValue() != 0;
        }
        throw illegalArgument(PropertyType.Boolean, value);
    }

    public String coerceToString(Object value)
    {
        return value.toString();
    }

    public BigInteger coerceToBigInteger(Object value)
    {
        if (value instanceof BigDecimal bd) {
            return bd.toBigInteger();
        }
        if (value instanceof Number num) {
            return BigInteger.valueOf(num.longValue());
        }
        if (value instanceof String str) {
            return new BigInteger(str);
        }
        throw illegalArgument(PropertyType.BigInteger, value);
    }

    public BigDecimal coerceToBigDecimal(Object value)
    {
        if (value instanceof BigInteger bi) {
            return new BigDecimal(bi);
        }
        if (value instanceof Number num) {
            return BigDecimal.valueOf(num.doubleValue());
        }
        if (value instanceof String str) {
            return new BigDecimal(str);
        }
        throw illegalArgument(PropertyType.BigDecimal, value);
    }

    public ZonedDateTime coerceToZonedDateTime(Object value)
    {
        if (value instanceof String str) {
            return ZonedDateTime.parse(str);
        }
        if (value instanceof Timestamp timestamp) {
            return ZonedDateTime.ofInstant(timestamp.toInstant(), timeZone.toZoneId());
        }
        if (value instanceof Instant instant) {
            return ZonedDateTime.ofInstant(instant, timeZone.toZoneId());
        }
        throw illegalArgument(PropertyType.DateTime, value);
    }

    public URI coerceToURI(Object value)
    {
        if (value instanceof String str) {
            return java.net.URI.create(str);
        }
        throw illegalArgument(PropertyType.URI, value);
    }

    public UUID coerceToUUID(Object value)
    {
        if (value instanceof String str) {
            return UUID.fromString(str);
        }
        throw illegalArgument(PropertyType.UUID, value);
    }

    public byte[] coerceToByteArray(Object value)
    {
        if (value instanceof String str) {
            return str.getBytes();
        }
        throw illegalArgument(PropertyType.BLOB, value);
    }
}
