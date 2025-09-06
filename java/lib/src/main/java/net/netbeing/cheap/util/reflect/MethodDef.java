package net.netbeing.cheap.util.reflect;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class MethodDef
{
    private final String methodName;
    private final MethodType methodType;

    public MethodDef(Method method) {
        this.methodName = method.getName();
        this.methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
    }

    public String getMethodName() {
        return methodName;
    }

    public MethodType getMethodType() {
        return methodType;
    }

    @Override
    public String toString() {
        return "LambdaMetadata{" + "methodName=" + methodName + ", methodType=" + methodType + '}';
    }
}
