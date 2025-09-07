package net.netbeing.cheap.impl.reflect;

import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings("ReassignedVariable")
public record PojoPropertyDef(
        String name,
        PropertyType type,
        Method getter,
        Method setter,
        boolean isReadable,
        boolean isWritable,
        boolean isNullable,
        boolean isMultivalued
) implements PropertyDef
{
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

        return new PojoPropertyDef(name, type, getter, null, true, false, nullable, multivalued);
    }

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

        return new PojoPropertyDef(name, type, null, setter, false, true, nullable, multivalued);
    }

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

        return new PojoPropertyDef(name, type, getter, setter, true, true, nullable, multivalued);
    }

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

    @Override
    public boolean isRemovable()
    {
        return false;
    }
}
