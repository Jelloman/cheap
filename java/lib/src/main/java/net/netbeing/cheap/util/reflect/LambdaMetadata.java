package net.netbeing.cheap.util.reflect;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class LambdaMetadata {

    private final String methodName;
    private final MethodType declaringInterfaceType;
    private final MethodType methodType;

    public LambdaMetadata(Method method) {
        this.methodName = method.getName();
        this.declaringInterfaceType = MethodType.methodType(LambdaWrapper.class);
        this.methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
    }

    public String getMethodName() {
        return methodName;
    }

    public MethodType getDeclaringInterfaceType() {
        return declaringInterfaceType;
    }

    public MethodType getMethodType() {
        return methodType;
    }

    @Override
    public String toString() {
        return "LambdaMetadata{" + "methodName='" + methodName + '\'' + ", factory="
            + declaringInterfaceType +
            ", methodType=" + methodType + '}';
    }
}
