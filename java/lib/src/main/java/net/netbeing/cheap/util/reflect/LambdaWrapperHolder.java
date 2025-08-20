package net.netbeing.cheap.util.reflect;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum LambdaWrapperHolder {

    DEFAULT;

    private final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private final Map<AbstractSignature, LambdaMetadata> invokers = new HashMap<>();

    LambdaWrapperHolder() {
        Arrays.stream(LambdaWrapper.class.getDeclaredMethods()).forEach(this::addInvoker);
    }

    private void addInvoker(Method method) {
        MethodSignature methodSignature = MethodSignature.fromWrapper(method);
        LambdaMetadata metadata = new LambdaMetadata(method);
        invokers.put(methodSignature, metadata);
    }


    @SuppressWarnings("unchecked")
    public <F> WrapperHolder<F> createWrapper(Method method) {
        return new WrapperHolder<>(
            ThrowableOptional.sneaky(
                () -> (F) createCallSite(method).getTarget().invoke()),
            LambdaWrapper.class
        );
    }

    private CallSite createCallSite(Method method)
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

    private LambdaMetadata getMetadata(Method method) {
        AbstractSignature signature = MethodSignature.from(method);

        if (!invokers.containsKey(signature)) {
            throw ReflectionException.format("No wrappers found for method %s", method);
        }
        return invokers.get(signature);
    }
}