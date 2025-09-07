package net.netbeing.cheap.util.reflect;


import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public class GetterSetterSignature
{
    private final Class<?> rType;
    private final Integer pCount;
    private final Class<?> declaringClass;
    private final Class<?>[] paramTypes;

    public GetterSetterSignature(Class<?> declaringClass, Class<?> rType, Integer pCount, Class<?>[] paramTypes) {
        this.declaringClass = declaringClass;
        this.rType = rType;
        this.pCount = pCount;
        this.paramTypes = paramTypes != null ? paramTypes.clone() : new Class<?>[0];
    }

    public static GetterSetterSignature fromWrapper(Method method) {
        return new GetterSetterSignature(
            method.getDeclaringClass(), getReturnType(method), method.getParameterCount(), method.getParameterTypes());
    }

    public static GetterSetterSignature from(Method method) {
        Class<?> rType = getReturnType(method);
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
        
        return new GetterSetterSignature(method.getDeclaringClass(), rType, method.getParameterCount() + 1, paramTypes);
    }

    public static Class<?> getReturnType(Method method) {
        Class<?> returnType = method.getReturnType();
        // For signature matching, convert all non-void types to Object for get methods
        // and keep void for set methods
        if (returnType == void.class) {
            return void.class;
        }
        return Object.class;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getReturnType(), getParameterCount(), Arrays.hashCode(paramTypes));
        // Note: declaringClass is NOT included in hash for signature matching
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GetterSetterSignature that = (GetterSetterSignature) o;
        return getReturnType().equals(that.getReturnType())
            && getParameterCount() == that.getParameterCount()
            && Arrays.equals(paramTypes, that.paramTypes);
            // Note: declaringClass is NOT compared for signature matching
    }

    @Override
    public String toString() {
        return "MethodSignature{" + "rType=" + rType + ", pCount=" + pCount + ", declaringClass="
            + declaringClass + ", paramTypes=" + Arrays.toString(paramTypes) +
            '}';
    }

    public Class<?> getReturnType() {
        return rType;
    }

    public int getParameterCount() {
        return pCount;
    }
    
    public Class<?>[] getParameterTypes() {
        return paramTypes.clone();
    }
}