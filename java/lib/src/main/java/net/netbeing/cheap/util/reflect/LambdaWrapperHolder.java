package net.netbeing.cheap.util.reflect;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class LambdaWrapperHolder {

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final Map<MethodSignature, LambdaMetadata> invokers = new HashMap<>();

    static {
        Arrays.stream(LambdaWrapper.class.getDeclaredMethods()).forEach(method -> {
            MethodSignature methodSignature = MethodSignature.fromWrapper(method);
            LambdaMetadata metadata = new LambdaMetadata(method);
            invokers.put(methodSignature, metadata);
        });
    }

    public static <T> T sneaky(SneakySupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    public static <F> F createWrapper(Method method) {
        try {
            return sneaky(() -> (F) createCallSite(method).getTarget().invoke());
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private static CallSite createCallSite(Method method)
        throws Exception
    {
        MethodHandle methodHandle = lookup.unreflect(method);
        LambdaMetadata lambdaMetadata = getMetadata(method);
        return LambdaMetafactory.metafactory(lookup, lambdaMetadata.getMethodName(),
            lambdaMetadata.getDeclaringInterfaceType(),
            lambdaMetadata.getMethodType(),
            methodHandle, methodHandle.type()
        );
    }

    private static LambdaMetadata getMetadata(Method method) {
        MethodSignature signature = MethodSignature.from(method);

        if (!invokers.containsKey(signature)) {
            throw ReflectionException.format("No wrappers found for method %s", method);
        }
        return invokers.get(signature);
    }
}