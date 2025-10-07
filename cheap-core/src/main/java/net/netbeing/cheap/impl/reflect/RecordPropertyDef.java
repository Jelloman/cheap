package net.netbeing.cheap.impl.reflect;

import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;

import java.lang.reflect.RecordComponent;
import java.util.Objects;

/**
 * A {@link PropertyDef} implementation for Java record components that provides read-only property definitions
 * derived from record component metadata through reflection.
 * 
 * <p>This class bridges Java record components to the Cheap property model by automatically inferring
 * property characteristics from the record component's type, annotations, and generic type information.
 * Record properties are inherently read-only since Java records are immutable by design.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Automatic {@link PropertyType} inference from Java types using {@link CheapReflectionUtil#typeOf}</li>
 *   <li>Nullability detection based on {@code @NotNull} annotations</li>
 *   <li>Multi-valued property support for arrays and collections</li>
 *   <li>Read-only access pattern enforcing record immutability</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * public record Person(String name, @NotNull List<String> emails, int age) {}
 * 
 * RecordComponent nameComponent = Person.class.getRecordComponents()[0];
 * RecordPropertyDef nameProp = new RecordPropertyDef(nameComponent);
 * // Results in: name="name", type=STRING, nullable=true, multivalued=false
 * 
 * RecordComponent emailsComponent = Person.class.getRecordComponents()[1]; 
 * RecordPropertyDef emailsProp = new RecordPropertyDef(emailsComponent);
 * // Results in: name="emails", type=STRING, nullable=false, multivalued=true
 * }</pre>
 * 
 * @param name the property name, derived from the record component name
 * @param type the {@link PropertyType} inferred from the component's Java type
 * @param field the underlying {@link RecordComponent} providing reflection metadata
 * @param isNullable whether the property can be null (based on annotations and primitive types)
 * @param isMultivalued whether the property represents multiple values (arrays/collections)
 * 
 * @see RecordAspectDef
 * @see RecordAspect  
 * @see CheapReflectionUtil
 * @see PropertyDef
 */
public record RecordPropertyDef(
        String name,
        PropertyType type,
        RecordComponent field,
        boolean isNullable,
        boolean isMultivalued
) implements PropertyDef
{
    /**
     * Compact constructor that validates the record property definition parameters.
     * 
     * @throws IllegalArgumentException if name is null or empty
     * @throws NullPointerException if type or field is null
     */
    public RecordPropertyDef
    {
        if (name.isEmpty()) { // implicitly tests for null also
            throw new IllegalArgumentException("Property names must have at least 1 character.");
        }
        Objects.requireNonNull(type);
        Objects.requireNonNull(field);
    }

    /**
     * Convenience constructor that automatically infers property characteristics from a record component.
     * 
     * <p>This constructor uses {@link CheapReflectionUtil} methods to automatically determine:</p>
     * <ul>
     *   <li>Property name from {@link RecordComponent#getName()}</li>
     *   <li>Property type from {@link CheapReflectionUtil#typeOf(RecordComponent)}</li>
     *   <li>Nullability from {@link CheapReflectionUtil#nullabilityOf(RecordComponent)}</li>
     *   <li>Multi-valued status from {@link CheapReflectionUtil#isMultivalued(RecordComponent)}</li>
     * </ul>
     * 
     * @param field the record component to create a property definition from
     * @throws NullPointerException if field is null
     * @throws IllegalArgumentException if the field's name is empty or type inference fails
     */
    public RecordPropertyDef(RecordComponent field)
    {
        this(field.getName(), CheapReflectionUtil.typeOf(field), field, CheapReflectionUtil.nullabilityOf(field), CheapReflectionUtil.isMultivalued(field));
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Record properties are always readable since they have accessor methods.</p>
     * 
     * @return always {@code true} for record properties
     */
    @Override
    public boolean isReadable()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Record properties are never writable since Java records are immutable.</p>
     * 
     * @return always {@code false} for record properties
     */
    @Override
    public boolean isWritable()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Record properties are never removable since record structure is fixed at compile time.</p>
     * 
     * @return always {@code false} for record properties
     */
    @Override
    public boolean isRemovable()
    {
        return false;
    }
}
