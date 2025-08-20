package net.netbeing.cheap.util.reflect;

public interface AbstractSignature
{
    Class<?> getReturnType();
    int getParameterCount();
    Class<?>[] getParameterTypes();
}
