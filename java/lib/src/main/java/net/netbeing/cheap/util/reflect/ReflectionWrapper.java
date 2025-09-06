package net.netbeing.cheap.util.reflect;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class ReflectionWrapper
{
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final Map<GetterSetterSignature, MethodDef> genericMethods = new HashMap<>();
    private static final MethodType wrapper = MethodType.methodType(GenericGetterSetter.class);

    public interface Getter<T> {
        T get() throws Throwable;
    }

    static {
        Arrays.stream(GenericGetterSetter.class.getDeclaredMethods()).forEach(method -> {
            GetterSetterSignature methodSignature = GetterSetterSignature.fromWrapper(method);
            MethodDef metadata = new MethodDef(method);
            genericMethods.put(methodSignature, metadata);
        });
    }

    public static <T> T getNoCheckedExceptions(Getter<T> getter)
    {
        try {
            return getter.get();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @SuppressWarnings("unchecked")
    public static <F> F createWrapper(Method method)
    {
        try {
            return getNoCheckedExceptions(() -> (F) createCallSite(method).getTarget().invoke());
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private static CallSite createCallSite(Method method)
        throws Exception
    {
        MethodHandle methodHandle = lookup.unreflect(method);
        MethodDef def = getDef(method);
        return LambdaMetafactory.metafactory(lookup, def.getMethodName(), wrapper, def.getMethodType(), methodHandle, methodHandle.type());
    }

    private static MethodDef getDef(Method method) {
        GetterSetterSignature signature = GetterSetterSignature.from(method);
        MethodDef def = genericMethods.get(signature);
        if (def == null) {
            throw new RuntimeException("No generic form found for method " + method.getName() + ".");
        }
        return def;
    }
}