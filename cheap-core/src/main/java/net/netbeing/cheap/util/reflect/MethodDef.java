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

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * Encapsulates method metadata required for lambda metafactory operations.
 * <p>
 * This class captures essential method characteristics needed for creating lambda-based
 * method implementations through {@code LambdaMetafactory}. It stores the method name
 * and a {@link MethodType} representation that can be used for dynamic method handle
 * creation and invocation.
 * <p>
 * The class serves as a bridge between reflection-based method introspection and
 * the method handle/lambda metafactory infrastructure, enabling efficient runtime
 * creation of optimized method implementations.
 * <p>
 * <strong>Thread Safety:</strong> This class is immutable and thread-safe after construction.
 * <p>
 * <strong>Usage:</strong> Typically used in conjunction with {@link ReflectionWrapper}
 * to create {@link GenericGetterSetter} implementations from target methods.
 * 
 * @see ReflectionWrapper
 * @see GenericGetterSetter
 * @see GetterSetterSignature
 * @see MethodType
 * @see java.lang.invoke.LambdaMetafactory
 */
public class MethodDef
{
    private final String methodName;
    private final MethodType methodType;

    /**
     * Creates a method definition from a reflection Method instance.
     * <p>
     * This constructor extracts the method name and creates a {@link MethodType}
     * representation that captures the method's return type and parameter types.
     * The resulting MethodType can be used for lambda metafactory operations.
     * 
     * @param method the method to create a definition for
     * @throws NullPointerException if method is null
     */
    public MethodDef(Method method) {
        this.methodName = method.getName();
        this.methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
    }

    /**
     * Returns the name of the method.
     * 
     * @return the method name, never null
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns the method type descriptor.
     * <p>
     * The returned {@link MethodType} describes the method's signature including
     * return type and parameter types, and can be used for method handle operations
     * and lambda metafactory creation.
     * 
     * @return the method type descriptor, never null
     */
    public MethodType getMethodType() {
        return methodType;
    }
}
