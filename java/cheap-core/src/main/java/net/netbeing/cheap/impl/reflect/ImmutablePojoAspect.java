package net.netbeing.cheap.impl.reflect;

import net.netbeing.cheap.model.*;
import org.jetbrains.annotations.NotNull;
import net.netbeing.cheap.util.reflect.GenericGetterSetter;

/**
 * An {@link Aspect} implementation that provides read-only access to Plain Old Java Objects (POJOs)
 * through the CHEAP property model.
 * 
 * <p>This class wraps a POJO instance and exposes its JavaBean properties as CHEAP properties
 * using the property definitions from an {@link ImmutablePojoAspectDef}. It enforces immutability
 * by providing read-only access to properties and throwing {@link UnsupportedOperationException}
 * for any write, add, or remove operations, regardless of whether the underlying POJO has setter methods.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Read-only property access through JavaBean getter methods</li>
 *   <li>Type-safe POJO instance storage with generic type parameter</li>
 *   <li>Integration with CHEAP's entity and catalog system</li>
 *   <li>Efficient property value retrieval using cached method wrappers</li>
 *   <li>Immutability enforcement at the aspect level</li>
 * </ul>
 * 
 * <p>Property access behavior:</p>
 * <ul>
 *   <li>Properties are accessed through cached {@link GenericGetterSetter} wrappers</li>
 *   <li>Only properties with getter methods can be read</li>
 *   <li>Missing properties or properties without getters throw {@link IllegalArgumentException}</li>
 *   <li>All mutation operations throw {@link UnsupportedOperationException}</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * public class Person {
 *     private String name;
 *     private int age;
 *     
 *     public String getName() { return name; }
 *     public void setName(String name) { this.name = name; } // Available but ignored
 *     public int getAge() { return age; }
 * }
 * 
 * Person person = new Person();
 * person.setName("John"); // Set via regular Java method
 * 
 * ImmutablePojoAspectDef aspectDef = new ImmutablePojoAspectDef(Person.class);
 * ImmutablePojoAspect<Person> aspect = new ImmutablePojoAspect<>(catalog, entity, aspectDef, person);
 * 
 * String name = (String) aspect.unsafeReadObj("name"); // Returns "John"
 * 
 * // These operations will throw UnsupportedOperationException:
 * aspect.unsafeWrite("name", "Jane");  // Cannot write
 * aspect.unsafeRemove("age");          // Cannot remove
 * }</pre>
 * 
 * <p>This implementation works in conjunction with {@link ImmutablePojoAspectDef} to provide
 * a read-only view of POJO instances through the CHEAP property model, making it suitable
 * for scenarios where data immutability needs to be enforced at the aspect level regardless
 * of the underlying object's mutability capabilities.</p>
 * 
 * @param <P> the specific POJO type wrapped by this aspect
 * 
 * @see ImmutablePojoAspectDef
 * @see PojoPropertyDef
 * @see MutablePojoAspect
 * @see Aspect
 * @see CheapReflectionUtil
 */
public class ImmutablePojoAspect<P> implements Aspect
{
    /** The catalog that this aspect belongs to. */
    private final Catalog catalog;
    
    /** The entity that this aspect is associated with. */
    private final Entity entity;
    
    /** The aspect definition describing the POJO's structure and providing method access. */
    private final ImmutablePojoAspectDef def;
    
    /** The underlying POJO instance. */
    private final P object;

    /**
     * Constructs a new ImmutablePojoAspect wrapping the specified POJO instance.
     * 
     * <p>This constructor creates an immutable aspect that provides read-only access
     * to the POJO's properties through the property definitions and cached method
     * wrappers provided by the aspect definition.</p>
     * 
     * @param catalog the catalog that this aspect belongs to
     * @param entity the entity that this aspect is associated with
     * @param def the immutable aspect definition describing the POJO structure
     * @param object the POJO instance to wrap
     * @param <P> the type of the POJO instance
     * @throws NullPointerException if any parameter is null
     */
    public ImmutablePojoAspect(@NotNull Catalog catalog, @NotNull Entity entity, @NotNull ImmutablePojoAspectDef def, @NotNull P object)
    {
        this.catalog = catalog;
        this.entity = entity;
        this.def = def;
        this.object = object;

    }

    /**
     * {@inheritDoc}
     * 
     * @return the catalog that this aspect belongs to
     */
    @Override
    public Catalog catalog()
    {
        return catalog;
    }

    /**
     * {@inheritDoc}
     * 
     * @return the entity that this aspect is associated with
     */
    @Override
    public Entity entity()
    {
        return entity;
    }

    /**
     * {@inheritDoc}
     * 
     * @return the immutable POJO aspect definition describing this aspect's structure
     */
    @Override
    public AspectDef def()
    {
        return def;
    }

    /**
     * Returns the underlying POJO instance.
     * 
     * @return the POJO instance wrapped by this aspect
     */
    public P object()
    {
        return object;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Reads property values from the POJO using the getter methods defined by the POJO class.
     * The property value is retrieved through efficient reflection wrappers cached by the
     * {@link ImmutablePojoAspectDef}.</p>
     * 
     * @param propName the name of the property to read
     * @return the value of the property from the POJO instance
     * @throws IllegalArgumentException if no property with the given name exists or if the property has no getter method
     * @throws NullPointerException if propName is null
     */
    @Override
    public Object unsafeReadObj(@NotNull String propName)
    {
        GenericGetterSetter getter = def.getter(propName);
        if (getter == null) {
            throw new IllegalArgumentException("Class " + def.name() + " does not contain field '" + propName + "'.");
        }
        return getter.get(object);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Always throws {@link UnsupportedOperationException} since this aspect enforces
     * immutability regardless of whether the underlying POJO has setter methods.</p>
     * 
     * @param propName the name of the property (unused)
     * @param value the value to set (unused)
     * @throws UnsupportedOperationException always, since immutable aspects cannot be modified
     */
    @Override
    public void unsafeWrite(@NotNull String propName, Object value)
    {
        throw new UnsupportedOperationException("Property '" + propName + "' cannot be set in Java class with immutable AspectDef '" + def.name() + "'.");
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Always throws {@link UnsupportedOperationException} since POJOs have a fixed
     * set of properties defined by their class structure, and immutable aspects cannot be modified.</p>
     * 
     * @param prop the property to add (unused)
     * @throws UnsupportedOperationException always, since POJO properties cannot be added dynamically
     */
    @Override
    public void unsafeAdd(@NotNull Property prop)
    {
        throw new UnsupportedOperationException("Property '" + prop.def().name() + "' cannot be added to Java class with AspectDef '" + def.name() + "'.");
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Always throws {@link UnsupportedOperationException} since POJOs have a fixed
     * set of properties defined by their class structure, and immutable aspects cannot be modified.</p>
     * 
     * @param propName the name of the property to remove (unused)
     * @throws UnsupportedOperationException always, since POJO properties cannot be removed dynamically
     */
    @Override
    public void unsafeRemove(@NotNull String propName)
    {
        throw new UnsupportedOperationException("Property '" + propName + "' cannot be removed from Java class with AspectDef '" + def.name() + "'.");
    }
}
