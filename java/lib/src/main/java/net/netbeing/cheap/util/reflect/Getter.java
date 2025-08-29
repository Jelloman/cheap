package net.netbeing.cheap.util.reflect;

import org.jetbrains.annotations.Nullable;

import java.lang.invoke.*;
import java.lang.reflect.Method;

public class Getter
{
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private final CallSite callSite = null;

    @FunctionalInterface
    public interface IGetter<T, R>
    {
        @Nullable
        R get( T object );
    }

    public Getter(Method method) throws IllegalAccessException
    {
        final MethodHandle methodHandle = lookup.unreflect(method);
        final Class<?> returnType = method.getReturnType();
        final MethodType methodType = MethodType.methodType(returnType);
/*
        callSite = LambdaMetafactory.metafactory(lookup, method.getName(), methodType,

        final MethodHandle target = caller.findVirtual( clazz, methodName, methodType );
        final MethodType type = target.type();
        final CallSite site = LambdaMetafactory.metafactory(
            lookup,
            "get",
            MethodType.methodType( IGetter.class ),
            type.erase(),
            target,
            type );

        final MethodHandle factory = site.getTarget();
        //return (IGetter<T, R>) factory.invoke();
 */


    }

}
