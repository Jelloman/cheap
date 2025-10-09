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

import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * A {@link PropertyDef} implementation for Plain Old Java Objects (POJOs) that provides property definitions
 * derived from JavaBean-style getter and setter methods through reflection.
 * 
 * <p>This class bridges POJO properties to the Cheap property model by analyzing getter and setter method
 * pairs and inferring property characteristics from method signatures, return types, and annotations.
 * Unlike {@link RecordPropertyDef}, POJO properties can be read-only, write-only, or read-write depending
 * on the availability of getter and setter methods.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Support for read-only (getter only), write-only (setter only), and read-write properties</li>
 *   <li>Automatic property name derivation from method names (removing "get"/"set"/"is" prefixes)</li>
 *   <li>Property type inference using {@link CheapReflectionUtil} methods</li>
 *   <li>Nullability analysis based on {@code @NotNull} annotations on methods</li>
 *   <li>Multi-valued property detection for arrays and collections</li>
 *   <li>Java primitive type tracking for optimization</li>
 * </ul>
 * 
 * <p>Method name conventions:</p>
 * <ul>
 *   <li>{@code getFoo()} or {@code isFoo()} for boolean → property name "foo"</li>
 *   <li>{@code setFoo(T value)} → property name "foo"</li>
 *   <li>Getter/setter pairs must have compatible types</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // For a class with: String getName() and void setName(String name)
 * Method getter = MyClass.class.getMethod("getName");
 * Method setter = MyClass.class.getMethod("setName", String.class);
 * PojoPropertyDef def = PojoPropertyDef.fromGetterSetter(getter, setter);
 * // Results in: name="name", type=STRING, readable=true, writable=true
 * 
 * // For read-only property: List<String> getEmails()
 * Method emailsGetter = MyClass.class.getMethod("getEmails");
 * PojoPropertyDef def = PojoPropertyDef.fromGetterOnly(emailsGetter);
 * // Results in: name="emails", type=STRING, multivalued=true, readable=true, writable=false
 * }</pre>
 * 
 * @param name the property name derived from getter/setter method names
 * @param type the {@link PropertyType} inferred from method return/parameter types
 * @param getter the getter method, or {@code null} for write-only properties
 * @param setter the setter method, or {@code null} for read-only properties
 * @param isJavaPrimitive whether the property type is a Java primitive (affects nullability and boxing)
 * @param isReadable whether the property can be read (has a getter method)
 * @param isWritable whether the property can be written (has a setter method)
 * @param isNullable whether the property can be null (based on annotations and primitive types)
 * @param isMultivalued whether the property represents multiple values (arrays/collections)
 * 
 * @see ImmutablePojoAspectDef
 * @see MutablePojoAspectDef
 * @see CheapReflectionUtil
 * @see PropertyDef
 */
@SuppressWarnings("ReassignedVariable")
public record PojoPropertyDef(
        String name,
        PropertyType type,
        Method getter,
        Method setter,
        boolean isJavaPrimitive,
        boolean isReadable,
        boolean isWritable,
        boolean isNullable,
        boolean isMultivalued
) implements PropertyDef
{
    /**
     * Compact constructor that validates the POJO property definition parameters.
     * 
     * <p>Ensures that at least one of getter or setter is provided, and that all required
     * fields are properly initialized.</p>
     * 
     * @throws IllegalArgumentException if name is null or empty
     * @throws NullPointerException if type is null, or if both getter and setter are null
     */
    public PojoPropertyDef
    {
        if (name.isEmpty()) { // implicitly tests for null also
            throw new IllegalArgumentException("Property names must have at least 1 character.");
        }
        Objects.requireNonNull(type);
        if (getter == null) {
            Objects.requireNonNull(setter, "PojoPropertyDef requires a getter and/or a setter method.");
        }
    }

    /**
     * Creates a read-only property definition from a getter method.
     * 
     * <p>This factory method analyzes the getter method to automatically derive:</p>
     * <ul>
     *   <li>Property name by removing "get" or "is" prefix and lowercasing the first letter</li>
     *   <li>Property type from the method's return type</li>
     *   <li>Nullability from method annotations and return type</li>
     *   <li>Multi-valued status for arrays and collections</li>
     *   <li>Primitive type status for boxing optimization</li>
     * </ul>
     * 
     * <p>Method name examples:</p>
     * <ul>
     *   <li>{@code getName()} → property name "name"</li>
     *   <li>{@code isActive()} → property name "active"</li>
     *   <li>{@code value()} → property name "value" (no prefix to remove)</li>
     * </ul>
     * 
     * @param getter the getter method to analyze
     * @return a read-only property definition based on the getter method
     * @throws NullPointerException if getter is null
     * @throws IllegalArgumentException if getter is not a valid getter method
     * @see CheapReflectionUtil#assertGetter(Method)
     */
    public static PojoPropertyDef fromGetterOnly(Method getter)
    {
        String name = getter.getName();
        // Remove leading get/is and lower-case next character. Without leading "get" we leave case alone.
        if (name.startsWith("get")) {
            char[] c = name.toCharArray();
            c[3] = Character.toLowerCase(c[3]);
            name = new String(c, 3, c.length - 3);
        } else if (name.startsWith("is")) {
            char[] c = name.toCharArray();
            c[2] = Character.toLowerCase(c[2]);
            name = new String(c, 2, c.length - 2);
        }
        PropertyType type = CheapReflectionUtil.typeOfGetter(getter);
        boolean nullable = CheapReflectionUtil.nullabilityOfGetter(getter);
        boolean multivalued = CheapReflectionUtil.isMultivaluedGetter(getter);
        boolean isPrimitive = getter.getReturnType().isPrimitive();

        return new PojoPropertyDef(name, type, getter, null, isPrimitive, true, false, nullable, multivalued);
    }

    /**
     * Creates a write-only property definition from a setter method.
     * 
     * <p>This factory method analyzes the setter method to automatically derive:</p>
     * <ul>
     *   <li>Property name by removing "set" prefix and lowercasing the first letter</li>
     *   <li>Property type from the method's parameter type</li>
     *   <li>Nullability from method parameter annotations</li>
     *   <li>Multi-valued status for arrays and collections</li>
     *   <li>Primitive type status for boxing optimization</li>
     * </ul>
     * 
     * <p>Method name examples:</p>
     * <ul>
     *   <li>{@code setName(String name)} → property name "name"</li>
     *   <li>{@code name(String value)} → property name "name" (no prefix to remove)</li>
     * </ul>
     * 
     * @param setter the setter method to analyze
     * @return a write-only property definition based on the setter method
     * @throws NullPointerException if setter is null
     * @throws IllegalArgumentException if setter is not a valid setter method
     * @see CheapReflectionUtil#assertSetter(Method)
     */
    public static PojoPropertyDef fromSetterOnly(Method setter)
    {
        String name = setter.getName();
        if (name.startsWith("set")) {
            name = name.substring(3);
        }
        // Remove leading "set" and lower-case next character. Without leading "set" leave case alone.
        if (name.startsWith("set")) {
            char[] c = name.toCharArray();
            c[3] = Character.toLowerCase(c[3]);
            name = new String(c, 3, c.length - 3);
        }
        PropertyType type = CheapReflectionUtil.typeOfSetter(setter);
        boolean nullable = CheapReflectionUtil.nullabilityOfSetter(setter);
        boolean multivalued = CheapReflectionUtil.isMultivaluedSetter(setter);
        boolean isPrimitive = setter.getParameterTypes()[0].isPrimitive();

        return new PojoPropertyDef(name, type, null, setter, isPrimitive, false, true, nullable, multivalued);
    }

    /**
     * Creates a read-write property definition from a getter/setter method pair.
     * 
     * <p>This factory method analyzes both methods to create a complete property definition.
     * The property name is derived from the getter method, and type compatibility between
     * the getter return type and setter parameter type is assumed to have been validated.</p>
     * 
     * <p>Property characteristics are derived as follows:</p>
     * <ul>
     *   <li>Property name from getter method name (removing "get"/"is" prefix)</li>
     *   <li>Property type from getter return type</li>
     *   <li>Nullability combines both getter and setter annotations (property is non-null if either is marked non-null)</li>
     *   <li>Multi-valued status from getter return type</li>
     *   <li>Primitive type status from getter return type</li>
     * </ul>
     * 
     * @param getter the getter method for reading the property
     * @param setter the setter method for writing the property
     * @return a read-write property definition based on both methods
     * @throws NullPointerException if either getter or setter is null
     * @throws IllegalArgumentException if the methods don't form a valid getter/setter pair
     * @see CheapReflectionUtil#assertGetterSetter(Method, Method)
     */
    public static PojoPropertyDef fromGetterSetter(Method getter, Method setter)
    {
        String name = getter.getName();
        // Remove leading get and lower-case next character. Without leading "get" we leave case alone.
        if (name.startsWith("get")) {
            char[] c = name.toCharArray();
            c[3] = Character.toLowerCase(c[3]);
            name = new String(c, 3, c.length - 3);
        } else if (name.startsWith("is")) {
            char[] c = name.toCharArray();
            c[2] = Character.toLowerCase(c[2]);
            name = new String(c, 2, c.length - 2);
        }
        PropertyType type = CheapReflectionUtil.typeOfGetter(getter);
        boolean nullable = CheapReflectionUtil.nullabilityOfGetterSetter(getter, setter);
        boolean multivalued = CheapReflectionUtil.isMultivaluedGetter(getter);
        boolean isPrimitive = getter.getReturnType().isPrimitive();

        return new PojoPropertyDef(name, type, getter, setter, isPrimitive, true, true, nullable, multivalued);
    }

    /**
     * Creates a property definition from a JavaBean {@link PropertyDescriptor}.
     * 
     * <p>This factory method provides integration with Java's standard introspection mechanism.
     * It analyzes the property descriptor to determine available methods and creates the
     * appropriate property definition based on method availability and mutability settings.</p>
     * 
     * <p>Method selection logic:</p>
     * <ul>
     *   <li>If immutable=true or no setter available → creates read-only property using {@link #fromGetterOnly}</li>
     *   <li>If no getter available → creates write-only property using {@link #fromSetterOnly}</li>
     *   <li>If both methods available and immutable=false → creates read-write property using {@link #fromGetterSetter}</li>
     * </ul>
     * 
     * @param prop the PropertyDescriptor from Java Bean introspection
     * @param immutable whether to treat the property as immutable (ignore setter even if available)
     * @return a property definition based on the descriptor's methods and mutability setting
     * @throws NullPointerException if prop is null
     * @throws IllegalArgumentException if the descriptor has no valid methods
     */
    public static PojoPropertyDef fromPropertyDescriptor(PropertyDescriptor prop, boolean immutable)
    {
        Method getter = prop.getReadMethod();
        Method setter = prop.getWriteMethod();
        if (immutable || setter == null) {
            return fromGetterOnly(getter);
        } else if (getter == null) {
            return fromSetterOnly(setter);
        } else {
            return fromGetterSetter(getter, setter);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>POJO properties are never removable since property structure is defined by the class methods
     * and cannot be modified at runtime.</p>
     * 
     * @return always {@code false} for POJO properties
     */
    @Override
    public boolean isRemovable()
    {
        return false;
    }
}
