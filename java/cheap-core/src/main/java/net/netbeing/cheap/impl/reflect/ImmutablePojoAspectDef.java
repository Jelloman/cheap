package net.netbeing.cheap.impl.reflect;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.impl.basic.ImmutableAspectDefImpl;
import net.netbeing.cheap.model.PropertyDef;
import org.jetbrains.annotations.NotNull;
import net.netbeing.cheap.util.reflect.GenericGetterSetter;
import net.netbeing.cheap.util.reflect.ReflectionWrapper;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Map;

/**
 * An {@link net.netbeing.cheap.model.AspectDef} implementation for immutable Plain Old Java Objects (POJOs)
 * that provides read-only access to properties through JavaBean-style getter methods.
 * 
 * <p>This class uses Java's standard introspection mechanism ({@link Introspector}) to discover JavaBean
 * properties in a POJO class and creates corresponding {@link PojoPropertyDef} instances. All properties
 * are treated as read-only regardless of whether setter methods exist in the class, enforcing immutable
 * semantics at the aspect level.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Automatic property discovery using Java Bean introspection</li>
 *   <li>Read-only property definitions ignoring setter methods</li>
 *   <li>Cached getter method wrappers for efficient property access</li>
 *   <li>Integration with CHEAP's reflection utilities for type mapping</li>
 * </ul>
 * 
 * <p>JavaBean property discovery:</p>
 * <ul>
 *   <li>Uses {@link Introspector#getBeanInfo(Class,Class)} to find properties</li>
 *   <li>Excludes {@code Object} class properties (like {@code getClass()})</li>
 *   <li>Only includes properties with valid getter methods</li>
 *   <li>Setter methods are ignored to maintain immutability</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * public class Person {
 *     private String name;
 *     private int age;
 *     
 *     public String getName() { return name; }
 *     public void setName(String name) { this.name = name; } // Setter ignored
 *     public int getAge() { return age; }
 * }
 * 
 * ImmutablePojoAspectDef aspectDef = new ImmutablePojoAspectDef(Person.class);
 * // Creates read-only property definitions for "name" and "age"
 * // Setter methods are ignored, enforcing immutability
 * }</pre>
 * 
 * <p>This class extends {@link ImmutableAspectDefImpl} and works in conjunction with
 * {@link ImmutablePojoAspect} to provide read-only access to POJO instances through
 * the CHEAP property model.</p>
 * 
 * @see ImmutablePojoAspect
 * @see PojoPropertyDef
 * @see MutablePojoAspectDef
 * @see CheapReflectionUtil
 * @see ImmutableAspectDefImpl
 */
public class ImmutablePojoAspectDef extends ImmutableAspectDefImpl
{
    /** The POJO class that this aspect definition represents. */
    private final Class<?> pojoClass;
    
    /** Cached map of property names to their reflection-based getter method wrappers. */
    private final Map<String, GenericGetterSetter> getters;

    /**
     * Constructs a new ImmutablePojoAspectDef for the specified POJO class.
     * 
     * <p>This constructor performs the following steps:</p>
     * <ol>
     *   <li>Uses {@link #propDefsFrom} to discover and create property definitions</li>
     *   <li>Initializes the parent with the class canonical name and property definitions</li>
     *   <li>Creates and caches reflection wrappers for all available getter methods</li>
     * </ol>
     * 
     * <p>Only properties with getter methods will have cached wrappers created. Properties
     * without getter methods (write-only) are included in the definitions but cannot be read.</p>
     * 
     * @param pojoClass the POJO class to create an immutable aspect definition for
     * @throws NullPointerException if pojoClass is null
     * @throws IllegalArgumentException if the class cannot be introspected or has no valid properties
     */
    public ImmutablePojoAspectDef(@NotNull Class<?> pojoClass)
    {
        super(pojoClass.getCanonicalName(), propDefsFrom(pojoClass));
        this.pojoClass = pojoClass;

        Collection<? extends PropertyDef> propDefs = propertyDefs();
        ImmutableMap.Builder<@NotNull String, @NotNull GenericGetterSetter> getterBuilder =
            ImmutableMap.builderWithExpectedSize(propDefs.size());
        for (PropertyDef prop : propDefs) {
            PojoPropertyDef pojoDef = (PojoPropertyDef) prop;
            if (pojoDef.getter() != null) {
                GenericGetterSetter getterHolder = ReflectionWrapper.createWrapper(pojoDef.getter());
                getterBuilder.put(prop.name(), getterHolder);
            }
        }
        this.getters =  getterBuilder.build();
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
     * Creates property definitions for all JavaBean properties of a POJO class.
     * 
     * <p>This static factory method uses Java's standard Bean introspection to discover
     * properties and create corresponding {@link PojoPropertyDef} instances. All properties
     * are created with immutable=true, meaning only getter methods are considered.</p>
     * 
     * <p>Introspection process:</p>
     * <ol>
     *   <li>Uses {@link Introspector#getBeanInfo(Class,Class)} excluding {@code Object} class methods</li>
     *   <li>Converts each {@link PropertyDescriptor} to a {@link PojoPropertyDef}</li>
     *   <li>Forces immutable=true to ignore setter methods</li>
     *   <li>Returns an immutable map of property names to definitions</li>
     * </ol>
     * 
     * @param pojoClass the POJO class to analyze for JavaBean properties
     * @return an immutable map from property names to their corresponding property definitions
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
            PropertyDef def = PojoPropertyDef.fromPropertyDescriptor(prop, true);
            propDefs.put(prop.getName(), def);
        }
        return propDefs.build();
    }

}
