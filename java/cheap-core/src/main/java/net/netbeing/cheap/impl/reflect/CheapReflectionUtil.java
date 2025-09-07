package net.netbeing.cheap.impl.reflect;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.model.PropertyType;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.CharBuffer;
import java.time.temporal.TemporalAccessor;
import java.util.*;

final class CheapReflectionUtil
{
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
            .put(UUID.class, PropertyType.UUID)
            .put(URI.class, PropertyType.URI)
            .put(URL.class, PropertyType.URI)
            .build();

    public static void assertGetter(@NotNull Method getter)
    {
        if (getter.getReturnType() == Void.TYPE) {
            throw new IllegalArgumentException("Method '" + getter.getName() + "' is not a getter because it returns void.");
        }
        if (getter.getParameterCount() > 0) {
            throw new IllegalArgumentException("Method '" + getter.getName() + "' is not a getter because it takes parameters.");
        }
    }

    public static void assertSetter(@NotNull Method setter)
    {
        if (setter.getParameterCount() != 1) {
            throw new IllegalArgumentException("Method '" + setter.getName() + "' is not a setter because it doesn't take 1 parameter.");
        }
    }

    public static void assertGetterSetter(@NotNull Method getter, @NotNull Method setter)
    {
        assertGetter(getter);
        assertSetter(setter);

        if (!getter.getReturnType().isAssignableFrom(setter.getParameterTypes()[0])) {
            throw new IllegalArgumentException("Methods '" + getter.getName() + "' and '" + setter.getName() +
                    "' are not a valid getter/setter pair because the types are incompatible.");
        }
    }

    public static boolean nullabilityOf(@NotNull RecordComponent field)
    {
        return nullabilityOf(field.getType(), field.getAnnotations());
    }

    public static boolean nullabilityOfGetter(@NotNull Method getter)
    {
        return nullabilityOf(getter.getReturnType(), getter.getAnnotatedReturnType().getAnnotations());
    }

    public static boolean nullabilityOfSetter(@NotNull Method setter)
    {
        return nullabilityOf(setter.getParameterTypes()[0], setter.getParameterAnnotations()[0]);
    }

    public static boolean nullabilityOfGetterSetter(@NotNull Method getter, @NotNull Method setter)
    {
        // if either the getter return or the setter param are marked non-null, then so is the property.
        return nullabilityOfGetter(getter) && nullabilityOfSetter(setter);
    }

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

    public static boolean isMultivalued(@NotNull Class<?> klass, Type genericType)
    {
        // Fields that are arrays or collections are considered "multivalued" properties of the component type.
        return klass.isArray() || genericType instanceof GenericArrayType || getCollectionComponentType(klass, genericType) != null;
    }

    public static boolean isMultivalued(RecordComponent field)
    {
        return isMultivalued(field.getType(), field.getGenericType());
    }

    public static boolean isMultivaluedGetter(Method getter)
    {
        return isMultivalued(getter.getReturnType(), getter.getGenericReturnType());
    }

    public static boolean isMultivaluedSetter(Method setter)
    {
        return isMultivalued(setter.getParameterTypes()[0], setter.getGenericParameterTypes()[0]);
    }

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

    private static Class<?> getCollectionComponentType(RecordComponent field)
    {
        return getCollectionComponentType(field.getType(), field.getGenericType());
    }

    private static Class<?> getCollectionComponentTypeGetter(Method getter)
    {
        return getCollectionComponentType(getter.getReturnType(), getter.getGenericReturnType());
    }

    private static Class<?> getCollectionComponentTypeSetter(Method setter)
    {
        return getCollectionComponentType(setter.getParameterTypes()[0], setter.getGenericParameterTypes()[0]);
    }

    public static PropertyType typeOf(RecordComponent field)
    {
        return typeOf(field.getType(), field.getGenericType());
    }

    public static PropertyType typeOfGetter(Method getter)
    {
        return typeOf(getter.getReturnType(), getter.getGenericReturnType());
    }

    public static PropertyType typeOfSetter(Method setter)
    {
        return typeOf(setter.getParameterTypes()[0], setter.getGenericParameterTypes()[0]);
    }

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

    private CheapReflectionUtil() {}
}
