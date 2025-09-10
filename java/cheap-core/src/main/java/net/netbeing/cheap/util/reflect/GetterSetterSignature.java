package net.netbeing.cheap.util.reflect;


import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public record GetterSetterSignature(
    Class<?> declaringClass,
    Class<?> returnType,
    int paramCount,
    Class<?>[] paramTypes
)
{
    public GetterSetterSignature(Class<?> declaringClass, Class<?> returnType, int paramCount, Class<?>[] paramTypes)
    {
        this.declaringClass = declaringClass;
        this.returnType = returnType;
        this.paramCount = paramCount;
        this.paramTypes = paramTypes != null ? paramTypes.clone() : new Class<?>[0];
    }

    public static GetterSetterSignature fromWrapper(Method method)
    {
        return new GetterSetterSignature(method.getDeclaringClass(), getReturnType(method), method.getParameterCount(), method.getParameterTypes());
    }

    public static GetterSetterSignature from(Method method)
    {
        Class<?> returnType = getReturnType(method);
        // For target method signature, we need to include the receiver as first parameter
        Class<?>[] originalParams = method.getParameterTypes();
        Class<?>[] paramTypes = new Class<?>[originalParams.length + 1];
        paramTypes[0] = Object.class; // receiver type

        // Convert parameter types for signature matching (primitives stay as-is, reference types become Object)
        for (int i = 0; i < originalParams.length; i++) {
            Class<?> paramType = originalParams[i];
            if (paramType.isPrimitive()) {
                paramTypes[i + 1] = paramType; // Keep primitives as-is for overload resolution
            } else {
                paramTypes[i + 1] = Object.class; // Convert reference types to Object
            }
        }

        return new GetterSetterSignature(method.getDeclaringClass(), returnType, method.getParameterCount() + 1, paramTypes);
    }

    public static Class<?> getReturnType(Method method)
    {
        Class<?> returnType = method.getReturnType();
        // For signature matching, convert all non-void types to Object for get methods
        // and keep void for set methods
        if (returnType == void.class) {
            return void.class;
        }
        return Object.class;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(returnType(), paramCount(), Arrays.hashCode(paramTypes));
        // Note: declaringClass is NOT included in hash for signature matching
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        GetterSetterSignature otherSig = (GetterSetterSignature) other;
        return returnType().equals(otherSig.returnType()) && paramCount() == otherSig.paramCount() && Arrays.equals(paramTypes, otherSig.paramTypes);
        // Note: declaringClass is NOT compared for signature matching
    }
}