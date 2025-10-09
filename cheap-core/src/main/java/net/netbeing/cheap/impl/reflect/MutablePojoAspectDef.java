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
import net.netbeing.cheap.impl.basic.MutableAspectDefImpl;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.util.reflect.GenericGetterSetter;
import net.netbeing.cheap.util.reflect.ReflectionWrapper;
import org.jetbrains.annotations.NotNull;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Map;

/**
 * An {@link net.netbeing.cheap.model.AspectDef} implementation for mutable Plain Old Java Objects (POJOs)
 * that provides full read-write access to properties through JavaBean-style getter and setter methods.
 * Unlike {@link MutableAspectDefImpl}, this implementation does NOT allow addition or removal of properties.
 * 
 * <p>This class uses Java's standard introspection mechanism ({@link Introspector}) to discover JavaBean
 * properties in a POJO class and creates corresponding {@link PojoPropertyDef} instances. Unlike
 * {@link ImmutablePojoAspectDef}, this class preserves both getter and setter methods, allowing
 * full mutation capabilities for properties that have both accessor types.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Automatic property discovery using Java Bean introspection</li>
 *   <li>Full read-write property definitions respecting both getters and setters</li>
 *   <li>Cached getter and setter method wrappers for efficient property access</li>
 *   <li>Support for read-only, write-only, and read-write properties</li>
 *   <li>Integration with Cheap's reflection utilities for type mapping</li>
 * </ul>
 * 
 * <p>Property access patterns:</p>
 * <ul>
 *   <li>Properties with only getters → read-only</li>
 *   <li>Properties with only setters → write-only</li>
 *   <li>Properties with both getters and setters → read-write</li>
 *   <li>All discovered properties are included regardless of accessibility</li>
 * </ul>
 * 
 * <p>JavaBean property discovery:</p>
 * <ul>
 *   <li>Uses {@link Introspector#getBeanInfo(Class,Class)} to find properties</li>
 *   <li>Excludes {@code Object} class properties (like {@code getClass()})</li>
 *   <li>Includes properties with getter methods, setter methods, or both</li>
 *   <li>Preserves mutability information for each property</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * public class Person {
 *     private String name;
 *     private int age;
 *     private List<String> emails;
 *     
 *     public String getName() { return name; }
 *     public void setName(String name) { this.name = name; }
 *     public int getAge() { return age; }
 *     // No setter for age - read-only property
 *     public void setEmails(List<String> emails) { this.emails = emails; }
 *     // No getter for emails - write-only property
 * }
 * 
 * MutablePojoAspectDef aspectDef = new MutablePojoAspectDef(Person.class);
 * // Creates property definitions for:
 * // - "name": read-write (has both getter and setter)
 * // - "age": read-only (has getter only)
 * // - "emails": write-only (has setter only)
 * }</pre>
 * 
 * <p>This class extends {@link MutableAspectDefImpl} and works in conjunction with
 * {@link MutablePojoAspect} to provide full read-write access to POJO instances through
 * the Cheap property model.</p>
 * 
 * @see MutablePojoAspect
 * @see PojoPropertyDef
 * @see ImmutablePojoAspectDef
 * @see CheapReflectionUtil
 * @see MutableAspectDefImpl
 */
public class MutablePojoAspectDef extends MutableAspectDefImpl
{
    /** The POJO class that this aspect definition represents. */
    private final Class<?> pojoClass;
    
    /** Cached map of property names to their reflection-based getter method wrappers. */
    private final Map<String, GenericGetterSetter> getters;
    
    /** Cached map of property names to their reflection-based setter method wrappers. */
    private final Map<String, GenericGetterSetter> setters;

    /**
     * Constructs a new MutablePojoAspectDef for the specified POJO class.
     * 
     * <p>This constructor performs the following steps:</p>
     * <ol>
     *   <li>Uses {@link #propDefsFrom} to discover and create mutable property definitions</li>
     *   <li>Initializes the parent with the class canonical name and property definitions</li>
     *   <li>Creates and caches reflection wrappers for all available getter and setter methods</li>
     * </ol>
     * 
     * <p>Separate maps are maintained for getters and setters, allowing properties to be
     * read-only (getter only), write-only (setter only), or read-write (both).</p>
     * 
     * @param pojoClass the POJO class to create a mutable aspect definition for
     * @throws NullPointerException if pojoClass is null
     * @throws IllegalArgumentException if the class cannot be introspected or has no valid properties
     */
    public MutablePojoAspectDef(@NotNull Class<?> pojoClass)
    {
        super(pojoClass.getCanonicalName(), propDefsFrom(pojoClass));
        this.pojoClass = pojoClass;

        Collection<? extends PropertyDef> propDefs = propertyDefs();
        ImmutableMap.Builder<@NotNull String, @NotNull GenericGetterSetter> getterBuilder = ImmutableMap.builderWithExpectedSize(propDefs.size());
        ImmutableMap.Builder<@NotNull String, @NotNull GenericGetterSetter> setterBuilder = ImmutableMap.builderWithExpectedSize(propDefs.size());
        for (PropertyDef prop : propDefs) {
            PojoPropertyDef pojoDef = (PojoPropertyDef) prop;
            if (pojoDef.getter() != null) {
                GenericGetterSetter getterHolder = ReflectionWrapper.createWrapper(pojoDef.getter());
                getterBuilder.put(prop.name(), getterHolder);
            }
            if (pojoDef.setter() != null) {
                GenericGetterSetter setterHolder = ReflectionWrapper.createWrapper(pojoDef.setter());
                setterBuilder.put(prop.name(), setterHolder);
            }
        }
        this.getters =  getterBuilder.build();
        this.setters =  setterBuilder.build();
    }

    /**
     * Returns whether properties can be added to this aspect definition.
     *
     * @return {@code false} as this aspect definition cannot be modified
     */
    @Override
    public boolean canAddProperties()
    {
        return false;
    }

    /**
     * Returns whether properties can be removed from this aspect definition.
     *
     * @return {@code false} as this aspect definition cannot be modified
     */
    @Override
    public boolean canRemoveProperties()
    {
        return false;
    }

    /**
     * Attempts to add a property definition to this immutable aspect definition.
     *
     * @param prop the property definition to add
     * @return never returns normally
     * @throws UnsupportedOperationException always, as this aspect definition cannot be modified
     */
    public PropertyDef add(@NotNull PropertyDef prop)
    {
        throw new UnsupportedOperationException("Properties cannot be added to AspectDef '" + name() + "'.");
    }

    /**
     * Attempts to remove a property definition from this immutable aspect definition.
     *
     * @param prop the property definition to remove
     * @return never returns normally
     * @throws UnsupportedOperationException always, as this aspect definition cannot be modified
     */
    public PropertyDef remove(@NotNull PropertyDef prop)
    {
        throw new UnsupportedOperationException("Properties cannot be removed from AspectDef '" + name() + "'.");
    }

    /**
     * Returns the POJO class that this aspect definition represents.
     * 
     * @return the POJO class used to generate this aspect definition
     */
    public Class<?> getPojoClass()
    {
        return pojoClass;
    }

    /**
     * Retrieves the reflection wrapper for the getter method of the specified property.
     * 
     * @param propName the name of the property to get the getter for
     * @return the {@link GenericGetterSetter} wrapper for the property's getter method,
     *         or {@code null} if no such property exists or the property has no getter
     * @throws NullPointerException if propName is null
     */
    protected GenericGetterSetter getter(@NotNull String propName)
    {
        return getters.get(propName);
    }

    /**
     * Retrieves the reflection wrapper for the setter method of the specified property.
     * 
     * @param propName the name of the property to get the setter for
     * @return the {@link GenericGetterSetter} wrapper for the property's setter method,
     *         or {@code null} if no such property exists or the property has no setter
     * @throws NullPointerException if propName is null
     */
    protected GenericGetterSetter setter(@NotNull String propName)
    {
        return setters.get(propName);
    }

    /**
     * Creates mutable property definitions for all JavaBean properties of a POJO class.
     * 
     * <p>This static factory method uses Java's standard Bean introspection to discover
     * properties and create corresponding {@link PojoPropertyDef} instances. Unlike
     * {@link ImmutablePojoAspectDef#propDefsFrom}, this method creates mutable property
     * definitions that preserve both getter and setter methods when available.</p>
     * 
     * <p>Introspection process:</p>
     * <ol>
     *   <li>Uses {@link Introspector#getBeanInfo(Class,Class)} excluding {@code Object} class methods</li>
     *   <li>Converts each {@link PropertyDescriptor} to a {@link PojoPropertyDef}</li>
     *   <li>Forces immutable=false to preserve setter methods</li>
     *   <li>Returns an immutable map of property names to definitions</li>
     * </ol>
     * 
     * @param pojoClass the POJO class to analyze for JavaBean properties
     * @return an immutable map from property names to their corresponding mutable property definitions
     * @throws NullPointerException if pojoClass is null
     * @throws IllegalArgumentException if the class cannot be introspected or wraps {@link IntrospectionException}
     * @see PojoPropertyDef#fromPropertyDescriptor(PropertyDescriptor, boolean)
     */
    public static ImmutableMap<@NotNull String, @NotNull PropertyDef> propDefsFrom(@NotNull Class<?> pojoClass)
    {
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(pojoClass, Object.class);
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException(e);
        }

        PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
        ImmutableMap.Builder<@NotNull String, @NotNull PropertyDef> propDefs = ImmutableMap.builderWithExpectedSize(props.length);
        for (PropertyDescriptor prop : props)
        {
            PropertyDef def = PojoPropertyDef.fromPropertyDescriptor(prop, false);
            propDefs.put(prop.getName(), def);
        }
        return propDefs.build();
    }

}
