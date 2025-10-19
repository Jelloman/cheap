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

package net.netbeing.cheap.impl.reflect;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.model.PropertyType;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.CharBuffer;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class providing reflection-based operations and type mapping functionality for the Cheap reflection package.
 * 
 * <p>This class serves as the core utility for all reflection-based implementations in the Cheap system.
 * It provides essential functionality for:</p>
 * <ul>
 *   <li>Java type to {@link PropertyType} mapping</li>
 *   <li>Getter and setter method validation</li>
 *   <li>Nullability inference from annotations</li>
 *   <li>Multi-valued property detection (arrays and collections)</li>
 *   <li>Type analysis for generic types and parameterized types</li>
 * </ul>
 * 
 * <p><strong>Type Mapping:</strong></p>
 * <p>The class maintains a comprehensive mapping from Java types to Cheap {@link PropertyType} values,
 * including support for:</p>
 * <ul>
 *   <li>Primitive types and their boxed equivalents</li>
 *   <li>String and character sequence types</li>
 *   <li>Numeric types (integers, floats, BigInteger)</li>
 *   <li>Date and time types (Date, Calendar, TemporalAccessor)</li>
 *   <li>URI and URL types</li>
 *   <li>UUID type</li>
 *   <li>Fallback to STRING for unrecognized types</li>
 * </ul>
 * 
 * <p><strong>Nullability Detection:</strong></p>
 * <p>Nullability is inferred using the following rules:</p>
 * <ul>
 *   <li>Java primitive types are never nullable</li>
 *   <li>Types annotated with {@code @NotNull} (any library) are not nullable</li>
 *   <li>All other reference types are assumed nullable</li>
 *   <li>For getter/setter pairs, property is non-null if either method is marked non-null</li>
 * </ul>
 * 
 * <p><strong>Multi-valued Detection:</strong></p>
 * <p>Properties are considered multi-valued if their type is:</p>
 * <ul>
 *   <li>An array type ({@code T[]})</li>
 *   <li>A parameterized Collection type ({@code Collection<T>})</li>
 *   <li>A generic array type ({@code GenericArrayType})</li>
 * </ul>
 * 
 * <p><strong>Method Validation:</strong></p>
 * <p>The utility provides validation for JavaBean conventions:</p>
 * <ul>
 *   <li>Getters must return non-void and take no parameters</li>
 *   <li>Setters must take exactly one parameter</li>
 *   <li>Getter/setter pairs must have compatible types</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Type mapping
 * PropertyType stringType = CheapReflectionUtil.typeOf(String.class, String.class);
 * PropertyType listType = CheapReflectionUtil.typeOf(List.class, 
 *     new ParameterizedTypeImpl(List.class, String.class));
 * 
 * // Method validation  
 * Method getter = MyClass.class.getMethod("getName");
 * Method setter = MyClass.class.getMethod("setName", String.class);
 * CheapReflectionUtil.assertGetterSetter(getter, setter); // Validates compatibility
 * 
 * // Nullability detection
 * boolean nullable = CheapReflectionUtil.nullabilityOfGetterSetter(getter, setter);
 * 
 * // Multi-valued detection
 * boolean multivalued = CheapReflectionUtil.isMultivaluedGetter(getter);
 * }</pre>
 * 
 * <p>This class is package-private and thread-safe. All methods are static and designed for
 * use by other classes in the reflection package.</p>
 * 
 * @see PropertyType
 * @see RecordPropertyDef
 * @see PojoPropertyDef
 */
final class CheapReflectionUtil
{
    /**
     * Immutable mapping from Java types to their corresponding Cheap {@link PropertyType} values.
     * 
     * <p>This map provides efficient type resolution for commonly used Java types. The mapping includes:</p>
     * <ul>
     *   <li><strong>Boolean types:</strong> boolean, Boolean → PropertyType.Boolean</li>
     *   <li><strong>Floating point types:</strong> float, Float, double, Double → PropertyType.Float</li>
     *   <li><strong>Integer types:</strong> byte, Byte, short, Short, int, Integer, long, Long → PropertyType.Integer</li>
     *   <li><strong>String types:</strong> char, Character, String, StringBuffer, StringBuilder, CharSequence, CharBuffer → PropertyType.String</li>
     *   <li><strong>Special types:</strong> BigInteger → PropertyType.BigInteger, UUID → PropertyType.UUID</li>
     *   <li><strong>URI types:</strong> URI, URL → PropertyType.URI</li>
     * </ul>
     * 
     * <p>Types not found in this map are handled by additional logic in {@link #typeOf(Class, Type)}.</p>
     */
    private static final Map<Class<?>, PropertyType> CLASS_PROPERTY_TYPE_MAP = ImmutableMap.<Class<?>, PropertyType>builder()
            .put(Boolean.TYPE, PropertyType.Boolean)
            .put(Boolean.class, PropertyType.Boolean)
            .put(Float.TYPE, PropertyType.Float)
            .put(Float.class, PropertyType.Float)
            .put(Double.TYPE, PropertyType.Float)
            .put(Double.class, PropertyType.Float)
            .put(Byte.TYPE, PropertyType.Integer)
            .put(Byte.class, PropertyType.Integer)
            .put(Short.TYPE, PropertyType.Integer)
            .put(Short.class, PropertyType.Integer)
            .put(Integer.TYPE, PropertyType.Integer)
            .put(Integer.class, PropertyType.Integer)
            .put(Long.TYPE, PropertyType.Integer)
            .put(Long.class, PropertyType.Integer)
            .put(Character.TYPE, PropertyType.String)
            .put(Character.class, PropertyType.String)
            .put(String.class, PropertyType.String)
            .put(StringBuffer.class, PropertyType.String)
            .put(StringBuilder.class, PropertyType.String)
            .put(CharSequence.class, PropertyType.String)
            .put(CharBuffer.class, PropertyType.String)
            .put(BigInteger.class, PropertyType.BigInteger)
            .put(BigDecimal.class, PropertyType.BigDecimal)
            .put(UUID.class, PropertyType.UUID)
            .put(URI.class, PropertyType.URI)
            .put(URL.class, PropertyType.URI)
            .build();

    /**
     * Validates that a method conforms to JavaBean getter conventions.
     * 
     * <p>A valid getter method must:</p>
     * <ul>
     *   <li>Return a non-void type</li>
     *   <li>Take no parameters</li>
     * </ul>
     * 
     * @param getter the method to validate as a getter
     * @throws IllegalArgumentException if the method does not meet getter requirements
     * @throws NullPointerException if getter is null
     */
    public static void assertGetter(@NotNull Method getter)
    {
        if (getter.getReturnType() == Void.TYPE) {
            throw new IllegalArgumentException("Method '" + getter.getName() + "' is not a getter because it returns void.");
        }
        if (getter.getParameterCount() > 0) {
            throw new IllegalArgumentException("Method '" + getter.getName() + "' is not a getter because it takes parameters.");
        }
    }

    /**
     * Validates that a method conforms to JavaBean setter conventions.
     * 
     * <p>A valid setter method must:</p>
     * <ul>
     *   <li>Take exactly one parameter</li>
     * </ul>
     * 
     * @param setter the method to validate as a setter
     * @throws IllegalArgumentException if the method does not meet setter requirements
     * @throws NullPointerException if setter is null
     */
    public static void assertSetter(@NotNull Method setter)
    {
        if (setter.getParameterCount() != 1) {
            throw new IllegalArgumentException("Method '" + setter.getName() + "' is not a setter because it doesn't take 1 parameter.");
        }
    }

    /**
     * Validates that a pair of methods form a compatible getter/setter pair.
     * 
     * <p>This method validates that:</p>
     * <ul>
     *   <li>The getter meets getter requirements (via {@link #assertGetter})</li>
     *   <li>The setter meets setter requirements (via {@link #assertSetter})</li>
     *   <li>The getter return type is compatible with the setter parameter type</li>
     * </ul>
     * 
     * <p>Type compatibility is determined using {@link Class#isAssignableFrom(Class)},
     * allowing for inheritance and polymorphic relationships.</p>
     * 
     * @param getter the getter method to validate
     * @param setter the setter method to validate
     * @throws IllegalArgumentException if either method fails validation or if the types are incompatible
     * @throws NullPointerException if either getter or setter is null
     */
    public static void assertGetterSetter(@NotNull Method getter, @NotNull Method setter)
    {
        assertGetter(getter);
        assertSetter(setter);

        if (!getter.getReturnType().isAssignableFrom(setter.getParameterTypes()[0])) {
            throw new IllegalArgumentException("Methods '" + getter.getName() + "' and '" + setter.getName() +
                    "' are not a valid getter/setter pair because the types are incompatible.");
        }
    }

    /**
     * Determines whether a record component can be null based on its type and annotations.
     * 
     * <p>This method delegates to {@link #nullabilityOf(Class, Annotation[])} using the
     * component's declared type and annotations.</p>
     * 
     * @param field the record component to analyze for nullability
     * @return {@code true} if the component can be null, {@code false} if it cannot be null
     * @throws NullPointerException if field is null
     * @see #nullabilityOf(Class, Annotation[])
     */
    public static boolean nullabilityOf(@NotNull RecordComponent field)
    {
        return nullabilityOf(field.getType(), field.getAnnotations());
    }

    /**
     * Determines whether a getter method's return value can be null based on its return type and annotations.
     * 
     * <p>This method analyzes the getter's return type and any annotations on the return type
     * to determine nullability.</p>
     * 
     * @param getter the getter method to analyze for return nullability
     * @return {@code true} if the getter's return value can be null, {@code false} if it cannot be null
     * @throws NullPointerException if getter is null
     * @see #nullabilityOf(Class, Annotation[])
     */
    public static boolean nullabilityOfGetter(@NotNull Method getter)
    {
        return nullabilityOf(getter.getReturnType(), getter.getAnnotatedReturnType().getAnnotations());
    }

    /**
     * Determines whether a setter method's parameter can be null based on its parameter type and annotations.
     * 
     * <p>This method analyzes the setter's first (and only) parameter type and any annotations
     * on that parameter to determine nullability.</p>
     * 
     * @param setter the setter method to analyze for parameter nullability
     * @return {@code true} if the setter's parameter can be null, {@code false} if it cannot be null
     * @throws NullPointerException if setter is null
     * @throws IndexOutOfBoundsException if setter has no parameters
     * @see #nullabilityOf(Class, Annotation[])
     */
    public static boolean nullabilityOfSetter(@NotNull Method setter)
    {
        return nullabilityOf(setter.getParameterTypes()[0], setter.getParameterAnnotations()[0]);
    }

    /**
     * Determines whether a property represented by a getter/setter pair can be null.
     * 
     * <p>This method uses a conservative approach: if either the getter return type or the
     * setter parameter type is marked as non-null, then the entire property is considered non-null.
     * This ensures that the property meets the strongest nullability constraint from either accessor.</p>
     * 
     * <p>The logic is: property is nullable if BOTH getter AND setter allow null values.</p>
     * 
     * @param getter the getter method of the property
     * @param setter the setter method of the property
     * @return {@code true} if the property can be null (both methods allow null), 
     *         {@code false} if the property cannot be null (either method prohibits null)
     * @throws NullPointerException if either getter or setter is null
     * @see #nullabilityOfGetter(Method)
     * @see #nullabilityOfSetter(Method)
     */
    public static boolean nullabilityOfGetterSetter(@NotNull Method getter, @NotNull Method setter)
    {
        // if either the getter return or the setter param are marked non-null, then so is the property.
        return nullabilityOfGetter(getter) && nullabilityOfSetter(setter);
    }

    /**
     * Determines whether a type can be null based on its class and annotations.
     * 
     * <p>This is the core nullability detection logic used by all other nullability methods.
     * The determination follows these rules:</p>
     * <ol>
     *   <li>Primitive types are never nullable (return {@code false})</li>
     *   <li>Types annotated with any {@code @NotNull} annotation are not nullable (return {@code false})</li>
     *   <li>All other reference types are assumed nullable (return {@code true})</li>
     * </ol>
     * 
     * <p><strong>Annotation Detection:</strong></p>
     * <p>The method detects {@code @NotNull} annotations by checking the simple class name,
     * making it compatible with NotNull annotations from multiple libraries (JetBrains, javax.validation,
     * FindBugs, etc.) without requiring specific dependencies.</p>
     * 
     * @param type the Java class to check for nullability
     * @param annotations the annotations to analyze, or null if no annotations
     * @return {@code true} if the type can be null, {@code false} if it cannot be null
     * @throws NullPointerException if type is null
     */
    public static boolean nullabilityOf(@NotNull Class<?> type, Annotation[] annotations)
    {
        if (type.isPrimitive()) {
            return false;
        }
        if (annotations != null) {
            for (var annotation : annotations) {
                Class<?> annotationType = annotation.annotationType();
                String simpleName = annotationType.getSimpleName();
                if (annotationType.getSimpleName().equals("NotNull")) { // this catches annotations from multiples libraries
                    return false;
                }
            }
        }
        //TODO: can possibly infer non-nullability based on collection types of multivalued props.
        return true;
    }

    /**
     * Determines whether a type represents a multi-valued property (array or collection).
     * 
     * <p>A type is considered multi-valued if it is:</p>
     * <ul>
     *   <li>An array type ({@code T[]})</li>
     *   <li>A generic array type ({@code GenericArrayType})</li>
     *   <li>A Collection type with valid generic parameters</li>
     * </ul>
     * 
     * <p>This method handles both raw classes and their generic type information to make
     * accurate determinations about multi-valued properties.</p>
     * 
     * @param klass the raw class type to check
     * @param genericType the generic type information, which may be more specific than the raw class
     * @return {@code true} if the type represents multiple values, {@code false} for single values
     * @throws NullPointerException if klass is null
     */
    public static boolean isMultivalued(@NotNull Class<?> klass, Type genericType)
    {
        // Fields that are arrays or collections are considered "multivalued" properties of the component type.
        return klass.isArray() || genericType instanceof GenericArrayType || getCollectionComponentType(klass, genericType) != null;
    }

    /**
     * Determines whether a record component represents a multi-valued property.
     * 
     * <p>This method delegates to {@link #isMultivalued(Class, Type)} using the component's
     * declared type and generic type information.</p>
     * 
     * @param field the record component to analyze
     * @return {@code true} if the component represents multiple values, {@code false} for single values
     * @throws NullPointerException if field is null
     * @see #isMultivalued(Class, Type)
     */
    public static boolean isMultivalued(RecordComponent field)
    {
        return isMultivalued(field.getType(), field.getGenericType());
    }

    /**
     * Determines whether a getter method returns a multi-valued property.
     * 
     * <p>This method analyzes the getter's return type and generic return type to determine
     * if it represents multiple values.</p>
     * 
     * @param getter the getter method to analyze
     * @return {@code true} if the getter returns multiple values, {@code false} for single values
     * @throws NullPointerException if getter is null
     * @see #isMultivalued(Class, Type)
     */
    public static boolean isMultivaluedGetter(Method getter)
    {
        return isMultivalued(getter.getReturnType(), getter.getGenericReturnType());
    }

    /**
     * Determines whether a setter method accepts a multivalued property.
     * 
     * <p>This method analyzes the setter's parameter type and generic parameter type to determine
     * if it accepts multiple values.</p>
     * 
     * @param setter the setter method to analyze
     * @return {@code true} if the setter accepts multiple values, {@code false} for single values
     * @throws NullPointerException if setter is null
     * @throws IndexOutOfBoundsException if setter has no parameters
     * @see #isMultivalued(Class, Type)
     */
    public static boolean isMultivaluedSetter(Method setter)
    {
        return isMultivalued(setter.getParameterTypes()[0], setter.getGenericParameterTypes()[0]);
    }

    /**
     * Extracts the component type from a Collection's generic type information.
     * 
     * <p>This method analyzes a Collection type to determine the type of elements it contains.
     * It works by examining the generic type parameters of parameterized Collection types.</p>
     * 
     * <p>The method returns {@code null} if:</p>
     * <ul>
     *   <li>The class is not assignable to Collection</li>
     *   <li>The generic type is not parameterized</li>
     *   <li>The parameterized type doesn't have exactly one type argument</li>
     * </ul>
     * 
     * @param klass the Collection class to analyze
     * @param genericType the generic type information for the Collection
     * @return the component type of the Collection, or {@code null} if it cannot be determined
     * @throws NullPointerException if klass is null
     */
    private static Class<?> getCollectionComponentType(@NotNull Class<?> klass, Type genericType)
    {
        if (Collection.class.isAssignableFrom(klass)) {
            if (genericType instanceof ParameterizedType) {
                Type[] paramTypes = ((ParameterizedType) genericType).getActualTypeArguments();
                if (paramTypes.length == 1) {
                    return paramTypes[0].getClass();
                }
            }
        }
        return null;
    }

    /**
     * Convenience method to extract the component type from a record component's Collection type.
     * 
     * @param field the record component with a Collection type
     * @return the component type of the Collection, or {@code null} if not a Collection or type cannot be determined
     * @throws NullPointerException if field is null
     * @see #getCollectionComponentType(Class, Type)
     */
    private static Class<?> getCollectionComponentType(RecordComponent field)
    {
        return getCollectionComponentType(field.getType(), field.getGenericType());
    }

    /**
     * Convenience method to extract the component type from a getter method's Collection return type.
     * 
     * @param getter the getter method with a Collection return type
     * @return the component type of the returned Collection, or {@code null} if not a Collection or type cannot be determined
     * @throws NullPointerException if getter is null
     * @see #getCollectionComponentType(Class, Type)
     */
    private static Class<?> getCollectionComponentTypeGetter(Method getter)
    {
        return getCollectionComponentType(getter.getReturnType(), getter.getGenericReturnType());
    }

    /**
     * Convenience method to extract the component type from a setter method's Collection parameter type.
     * 
     * @param setter the setter method with a Collection parameter type
     * @return the component type of the Collection parameter, or {@code null} if not a Collection or type cannot be determined
     * @throws NullPointerException if setter is null
     * @throws IndexOutOfBoundsException if setter has no parameters
     * @see #getCollectionComponentType(Class, Type)
     */
    private static Class<?> getCollectionComponentTypeSetter(Method setter)
    {
        return getCollectionComponentType(setter.getParameterTypes()[0], setter.getGenericParameterTypes()[0]);
    }

    /**
     * Determines the Cheap PropertyType for a record component.
     * 
     * <p>This method delegates to {@link #typeOf(Class, Type)} using the component's
     * declared type and generic type information.</p>
     * 
     * @param field the record component to analyze
     * @return the corresponding PropertyType for the component
     * @throws NullPointerException if field is null
     * @see #typeOf(Class, Type)
     */
    public static PropertyType typeOf(RecordComponent field)
    {
        return typeOf(field.getType(), field.getGenericType());
    }

    /**
     * Determines the Cheap PropertyType for a getter method's return type.
     * 
     * <p>This method analyzes the getter's return type and generic return type to determine
     * the appropriate Cheap PropertyType.</p>
     * 
     * @param getter the getter method to analyze
     * @return the corresponding PropertyType for the getter's return type
     * @throws NullPointerException if getter is null
     * @see #typeOf(Class, Type)
     */
    public static PropertyType typeOfGetter(Method getter)
    {
        return typeOf(getter.getReturnType(), getter.getGenericReturnType());
    }

    /**
     * Determines the Cheap PropertyType for a setter method's parameter type.
     * 
     * <p>This method analyzes the setter's parameter type and generic parameter type to determine
     * the appropriate Cheap PropertyType.</p>
     * 
     * @param setter the setter method to analyze
     * @return the corresponding PropertyType for the setter's parameter type
     * @throws NullPointerException if setter is null
     * @throws IndexOutOfBoundsException if setter has no parameters
     * @see #typeOf(Class, Type)
     */
    public static PropertyType typeOfSetter(Method setter)
    {
        return typeOf(setter.getParameterTypes()[0], setter.getGenericParameterTypes()[0]);
    }

    /**
     * Maps a Java type to the corresponding Cheap {@link PropertyType}.
     * 
     * <p>This is the core type mapping method that handles the conversion from Java's type system
     * to Cheap's property type system. The mapping process follows these steps:</p>
     * 
     * <ol>
     *   <li><strong>Multi-valued type unwrapping:</strong> For arrays and Collections, extracts the component type</li>
     *   <li><strong>Direct mapping lookup:</strong> Checks {@link #CLASS_PROPERTY_TYPE_MAP} for exact matches</li>
     *   <li><strong>Date/time type detection:</strong> Handles Date, Calendar, and TemporalAccessor types</li>
     *   <li><strong>Fallback:</strong> Defaults to PropertyType.String for unrecognized types</li>
     * </ol>
     * 
     * <p><strong>Multi-valued handling:</strong></p>
     * <p>For array and Collection types, this method extracts the component type before mapping:</p>
     * <ul>
     *   <li>Array types: uses {@link Class#getComponentType()}</li>
     *   <li>Collection types: uses {@link #getCollectionComponentType} with generic type information</li>
     *   <li>If Collection component type cannot be determined, falls back to PropertyType.String</li>
     * </ul>
     * 
     * <p><strong>Type mapping examples:</strong></p>
     * <pre>{@code
     * typeOf(String.class, String.class)           → PropertyType.String
     * typeOf(int.class, int.class)                → PropertyType.Integer
     * typeOf(List.class, List<String>)            → PropertyType.String (component type)
     * typeOf(Date.class, Date.class)              → PropertyType.DateTime
     * typeOf(CustomClass.class, CustomClass.class) → PropertyType.String (fallback)
     * }</pre>
     * 
     * @param klass the Java class to map
     * @param genericType the generic type information, used for Collections and generics
     * @return the corresponding Cheap PropertyType, never null
     * @throws NullPointerException if klass is null
     */
    public static PropertyType typeOf(Class<?> klass, Type genericType)
    {
        // Fields that are arrays or collections are considered "multivalued" properties of the component type.
        if (klass.isArray()) {
            klass = klass.getComponentType();
        } else if (Collection.class.isAssignableFrom(klass)) {
            klass = getCollectionComponentType(klass, genericType);
            if (klass == null) {
                // for some reason, the collection type parameter could not be determined, so fall back to String.
                return PropertyType.String;
            }
        }

        PropertyType mappedType = CLASS_PROPERTY_TYPE_MAP.get(klass);
        if (mappedType != null) {
            return mappedType;
        }

        if (Date.class.isAssignableFrom(klass) || Calendar.class.isAssignableFrom(klass) || TemporalAccessor.class.isAssignableFrom(klass)) {
            return PropertyType.DateTime;
        }

        // Everything else falls back to string
        return PropertyType.String;
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private CheapReflectionUtil() {}
}
