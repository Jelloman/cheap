/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.util.reflect;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating optimized lambda-based implementations of reflection operations.
 * <p>
 * This class provides the core functionality for converting traditional reflection-based
 * method calls into high-performance lambda implementations using the Java method handle
 * and lambda metafactory infrastructure. It bridges the gap between target methods and
 * the {@link GenericGetterSetter} interface by creating runtime lambda implementations
 * that avoid the overhead of traditional reflection.
 * <p>
 * <strong>Key Features:</strong>
 * <ul>
 * <li>Signature-based matching between target methods and wrapper interface methods</li>
 * <li>Automatic lambda generation for type-safe method dispatch</li>
 * <li>Support for all primitive types without boxing overhead</li>
 * <li>Exception handling with automatic wrapping of checked exceptions</li>
 * </ul>
 * <p>
 * <strong>Performance Benefits:</strong> Lambda-based implementations created by this
 * wrapper approach the performance of direct method calls after JIT optimization,
 * significantly outperforming traditional reflection while maintaining the same
 * flexibility and generic access patterns.
 * <p>
 * <strong>Thread Safety:</strong> This class is thread-safe. The static initialization
 * occurs once, and all operations are based on immutable data structures or thread-safe
 * method handle operations.
 * 
 * @see GenericGetterSetter
 * @see GetterSetterSignature
 * @see MethodDef
 * @see java.lang.invoke.LambdaMetafactory
 */
public final class ReflectionWrapper
{
    /** Method handles lookup instance for accessing methods. */
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    
    /** Cache mapping normalized signatures to their corresponding wrapper method definitions. */
    private static final Map<GetterSetterSignature, MethodDef> genericMethods = new HashMap<>();
    
    /** Method type descriptor for the GenericGetterSetter interface. */
    private static final MethodType wrapper = MethodType.methodType(GenericGetterSetter.class);

    /**
     * Functional interface for operations that may throw checked exceptions.
     * <p>
     * This interface enables clean exception handling in lambda expressions
     * where checked exceptions need to be converted to unchecked exceptions.
     * 
     * @param <T> the return type of the operation
     */
    public interface ThrowingGetter<T> {
        /**
         * Performs an operation that may throw any exception.
         * 
         * @return the result of the operation
         * @throws Throwable if the operation fails
         */
        T get() throws Throwable;
    }

    // Static initialization: build the signature mapping for all GenericGetterSetter methods
    static {
        Arrays.stream(GenericGetterSetter.class.getDeclaredMethods()).forEach(method -> {
            GetterSetterSignature methodSignature = GetterSetterSignature.fromWrapper(method);
            MethodDef metadata = new MethodDef(method);
            genericMethods.put(methodSignature, metadata);
        });
    }

    /**
     * Executes a throwing operation and converts any checked exceptions to unchecked.
     * <p>
     * This utility method provides a clean way to handle operations that may throw
     * checked exceptions in contexts where only unchecked exceptions are allowed.
     * Any checked exceptions are wrapped in a {@link RuntimeException}.
     * 
     * @param <T> the return type of the operation
     * @param getter the operation to execute
     * @return the result of the operation
     * @throws RuntimeException if the operation throws any checked exception
     * @throws RuntimeException if the operation throws any unchecked exception (re-thrown as-is)
     */
    public static <T> T getNoCheckedExceptions(ThrowingGetter<T> getter)
    {
        try {
            return getter.get();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates an optimized lambda-based wrapper for the specified method.
     * <p>
     * This is the main entry point for creating high-performance method wrappers.
     * The method signature is normalized and matched against the {@link GenericGetterSetter}
     * interface methods to determine the appropriate wrapper type. A lambda implementation
     * is then generated using {@code LambdaMetafactory} that provides near-native performance
     * after JIT optimization.
     * <p>
     * <strong>Type Safety:</strong> The generic type parameter F should match the expected
     * wrapper interface type (typically {@link GenericGetterSetter}).
     * 
     * @param <F> the type of wrapper interface to create (typically GenericGetterSetter)
     * @param method the target method to wrap
     * @return a lambda-based implementation of the wrapper interface
     * @throws RuntimeException if signature matching fails or lambda creation fails
     * @throws NullPointerException if method is null
     */
    @SuppressWarnings("unchecked")
    public static <F> F createWrapper(Method method)
    {
        try {
            return getNoCheckedExceptions(() -> (F) createCallSite(method).getTarget().invoke());
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    /**
     * Creates a lambda metafactory call site for the specified method.
     * <p>
     * This internal method performs the low-level lambda generation using
     * {@code LambdaMetafactory}. It creates a method handle for the target method
     * and matches it against the appropriate wrapper interface method signature.
     * 
     * @param method the target method to create a call site for
     * @return a call site that can generate lambda implementations
     * @throws Exception if method handle creation or lambda metafactory fails
     */
    private static CallSite createCallSite(Method method)
        throws Exception
    {
        MethodHandle methodHandle = lookup.unreflect(method);
        MethodDef def = getDef(method);
        return LambdaMetafactory.metafactory(lookup, def.getMethodName(), wrapper, def.getMethodType(), methodHandle, methodHandle.type());
    }

    /**
     * Resolves the method definition for a target method by signature matching.
     * <p>
     * This internal method normalizes the target method signature and looks up
     * the corresponding wrapper interface method definition. The signature matching
     * process ensures that the target method can be properly mapped to one of the
     * {@link GenericGetterSetter} interface methods.
     * 
     * @param method the target method to resolve a definition for
     * @return the method definition for the matching wrapper interface method
     * @throws RuntimeException if no matching signature is found in the wrapper interface
     */
    private static MethodDef getDef(Method method) {
        GetterSetterSignature signature = GetterSetterSignature.from(method);
        MethodDef def = genericMethods.get(signature);
        if (def == null) {
            throw new RuntimeException("No generic form found for method " + method.getName() + ".");
        }
        return def;
    }
}