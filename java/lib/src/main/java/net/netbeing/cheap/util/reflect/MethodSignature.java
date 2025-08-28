package net.netbeing.cheap.util.reflect;


import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

public class MethodSignature
{
    private final Class<?> rType;

    private final Integer pCount;

    private final Class<?> declaringClass;

    public MethodSignature(Class<?> declaringClass, Class<?> rType, Integer pCount) {
        this.declaringClass = declaringClass;
        this.rType = rType;
        this.pCount = pCount;
    }

    public static MethodSignature fromWrapper(Method method) {
        return new MethodSignature(
            method.getDeclaringClass(), getReturnType(method), method.getParameterCount());
    }

    public static MethodSignature from(Method method) {
        Class<?> rType = getReturnType(method);
        int pCount = method.getParameterCount() + (Modifier.isStatic(method.getModifiers()) ? 0 : 1);
        return new MethodSignature(method.getDeclaringClass(), rType, pCount);
    }

    public static Class<?> getReturnType(Method method) {
        return method.getReturnType() == void.class ? void.class : Object.class;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getReturnType(), getParameterCount());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodSignature that = (MethodSignature) o;
        return getReturnType().equals(that.getReturnType())
            && getParameterCount() == that.getParameterCount();
    }

    @Override
    public String toString() {
        return "MethodSignature{" + "rType=" + rType + ", pCount=" + pCount + ", declaringClass="
            + declaringClass +
            '}';
    }

    public Class<?> getReturnType() {
        return rType;
    }

    public int getParameterCount() {
        return pCount;
    }

}