package net.netbeing.cheap.util.reflect;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class Getter
{
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private final CallSite callSite;

    public Getter(Method method) throws IllegalAccessException
    {
        MethodHandle methodHandle = lookup.unreflect(method);
        callSite = LambdaMetafactory.metafactory(lookup, method.getName(), method.
    }
}
