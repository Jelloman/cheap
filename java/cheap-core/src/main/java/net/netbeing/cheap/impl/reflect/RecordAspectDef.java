package net.netbeing.cheap.impl.reflect;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.impl.basic.ImmutableAspectDefImpl;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.util.reflect.GenericGetterSetter;
import net.netbeing.cheap.util.reflect.ReflectionWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.Map;

/**
 * An {@link net.netbeing.cheap.model.AspectDef} implementation for Java record types that automatically
 * generates CHEAP aspect definitions from record class metadata through reflection.
 * 
 * <p>This class bridges Java record types to the CHEAP model by introspecting record components
 * and creating corresponding {@link RecordPropertyDef} instances. It provides read-only access
 * to record data through generated property definitions and cached accessor methods.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Automatic aspect definition generation from {@link RecordComponent} metadata</li>
 *   <li>Immutable property definitions reflecting Java record immutability</li>
 *   <li>Cached method resolution for efficient property access</li>
 *   <li>Seamless integration with CHEAP's reflection utilities</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * public record Person(String name, int age, List<String> emails) {}
 * 
 * RecordAspectDef aspectDef = new RecordAspectDef(Person.class);
 * // Creates an aspect definition with properties: name (STRING), age (INTEGER), emails (STRING, multivalued)
 * 
 * Collection<PropertyDef> properties = aspectDef.propertyDefs();
 * // Returns read-only property definitions for all record components
 * }</pre>
 * 
 * <p>This class extends {@link ImmutableAspectDefImpl} to enforce read-only semantics consistent
 * with Java record immutability. Property definitions are generated using {@link #propDefsFrom(Class)}
 * which creates {@link RecordPropertyDef} instances for each record component.</p>
 * 
 * @see RecordPropertyDef
 * @see RecordAspect
 * @see CheapReflectionUtil
 * @see ImmutableAspectDefImpl
 */
public class RecordAspectDef extends ImmutableAspectDefImpl
{
    /** The Java record class that this aspect definition represents. */
    private final Class<? extends Record> recordClass;
    
    /** Cached map of property names to their reflection-based accessor methods. */
    private final Map<String, GenericGetterSetter> methods;

    /**
     * Constructs a new RecordAspectDef for the specified record class.
     * 
     * <p>This constructor introspects the record class to automatically generate property definitions
     * for all record components. The aspect name is derived from the record's canonical name, and
     * property definitions are created using {@link #propDefsFrom(Class)}.</p>
     * 
     * @param recordClass the Java record class to create an aspect definition for
     * @throws NullPointerException if recordClass is null
     * @throws IllegalArgumentException if recordClass is not a valid record type
     */
    public RecordAspectDef(@NotNull Class<? extends Record> recordClass)
    {
        super(recordClass.getCanonicalName(), propDefsFrom(recordClass));
        this.recordClass = recordClass;
        this.methods = buildMethodMap();
    }

    /**
     * Builds an immutable map of property names to their corresponding accessor method wrappers.
     * 
     * <p>This method creates {@link GenericGetterSetter} wrappers for each record component's
     * accessor method to enable efficient property value retrieval. The wrappers are created
     * using {@link ReflectionWrapper#createWrapper(Method)} for optimized reflection access.</p>
     * 
     * @return an immutable map from property names to method wrappers
     * @throws IllegalStateException if property definitions contain non-record properties
     */
    protected @NotNull @Unmodifiable ImmutableMap<@NotNull String, @NotNull GenericGetterSetter> buildMethodMap()
    {
        Collection<? extends PropertyDef> propDefs = propertyDefs();
        ImmutableMap.Builder<@NotNull String, @NotNull GenericGetterSetter> builder = ImmutableMap.builderWithExpectedSize(propDefs.size());
        for (PropertyDef propDef : propDefs) {
            RecordPropertyDef recDef = (RecordPropertyDef) propDef;
            RecordComponent comp = recDef.field();
            Method method = comp.getAccessor();
            GenericGetterSetter getterHolder = ReflectionWrapper.createWrapper(method);
            builder.put(comp.getName(), getterHolder);
        }
        return builder.build();
    }

    /**
     * Retrieves the reflection wrapper for the accessor method of the specified property.
     * 
     * @param propName the name of the property to get the accessor for
     * @return the {@link GenericGetterSetter} wrapper for the property's accessor method,
     *         or {@code null} if no such property exists
     * @throws NullPointerException if propName is null
     */
    protected GenericGetterSetter getter(@NotNull String propName)
    {
        return methods.get(propName);
    }

    /**
     * Returns the Java record class that this aspect definition represents.
     * 
     * @return the record class used to generate this aspect definition
     */
    public Class<? extends Record> getRecordClass()
    {
        return recordClass;
    }

    /**
     * Creates property definitions for all components of a Java record class.
     * 
     * <p>This static factory method introspects the record class using {@link Class#getRecordComponents()}
     * and creates a {@link RecordPropertyDef} for each component. The resulting map provides
     * immediate access to property definitions by name.</p>
     * 
     * @param klass the Java record class to analyze
     * @return an immutable map from property names to their corresponding property definitions
     * @throws NullPointerException if klass is null
     * @throws IllegalArgumentException if klass is not a record type or has invalid components
     * @see RecordPropertyDef#RecordPropertyDef(RecordComponent)
     */
    public static ImmutableMap<@NotNull String, @NotNull PropertyDef> propDefsFrom(@NotNull Class<? extends Record> klass)
    {
        RecordComponent[] fields = klass.getRecordComponents();
        ImmutableMap.Builder<@NotNull String, @NotNull PropertyDef> propDefs = ImmutableMap.builderWithExpectedSize(fields.length);

        for (var field : fields) {
            PropertyDef def = new RecordPropertyDef(field);
            propDefs.put(field.getName(), def);
        }

        return propDefs.build();
    }

}
