package net.netbeing.cheap.util.reflect;


import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents the normalized signature of a getter or setter method for reflection operations.
 * <p>
 * This record encapsulates the essential characteristics of a method signature that are
 * needed for matching getter/setter pairs in the CHEAP reflection system. It performs
 * signature normalization to enable efficient matching between target methods and their
 * corresponding {@link GenericGetterSetter} interface methods.
 * <p>
 * The signature normalization process includes:
 * <ul>
 * <li>Converting all non-primitive reference types to {@code Object.class} for signature matching</li>
 * <li>Preserving primitive types for proper overload resolution</li>
 * <li>Adding the receiver object as the first parameter for target method signatures</li>
 * <li>Normalizing return types (void for setters, Object for getters)</li>
 * </ul>
 * <p>
 * <strong>Equality and Hashing:</strong> Two signatures are considered equal if they have
 * the same return type, parameter count, and parameter types. The declaring class is
 * intentionally excluded from equality comparisons to enable cross-class signature matching.
 * <p>
 * <strong>Thread Safety:</strong> This record is immutable and thread-safe.
 * 
 * @param declaringClass the class that declares the method (not used in equality)
 * @param returnType the normalized return type (void for setters, Object for getters)
 * @param paramCount the number of parameters in the method signature
 * @param paramTypes array of normalized parameter types (defensive copy maintained)
 * 
 * @see GenericGetterSetter
 * @see ReflectionWrapper
 * @see MethodDef
 */
public record GetterSetterSignature(
    Class<?> declaringClass,
    Class<?> returnType,
    int paramCount,
    Class<?>[] paramTypes
)
{
    /**
     * Creates a new signature with defensive copying of the parameter types array.
     * <p>
     * This constructor ensures immutability by creating a defensive copy of the
     * parameter types array to prevent external modification.
     * 
     * @param declaringClass the class that declares the method
     * @param returnType the normalized return type 
     * @param paramCount the number of parameters
     * @param paramTypes array of parameter types (will be defensively copied)
     */
    public GetterSetterSignature(Class<?> declaringClass, Class<?> returnType, int paramCount, Class<?>[] paramTypes)
    {
        this.declaringClass = declaringClass;
        this.returnType = returnType;
        this.paramCount = paramCount;
        this.paramTypes = paramTypes != null ? paramTypes.clone() : new Class<?>[0];
    }

    /**
     * Creates a signature from a {@link GenericGetterSetter} interface method.
     * <p>
     * This factory method is used to create signatures for the template methods
     * defined in the GenericGetterSetter interface. These signatures serve as
     * the canonical forms that target method signatures are matched against.
     * 
     * @param method a method from the GenericGetterSetter interface
     * @return a normalized signature for the wrapper method
     * @throws NullPointerException if method is null
     */
    public static GetterSetterSignature fromWrapper(Method method)
    {
        return new GetterSetterSignature(method.getDeclaringClass(), getReturnType(method), method.getParameterCount(), method.getParameterTypes());
    }

    /**
     * Creates a normalized signature from a target method for matching against wrapper methods.
     * <p>
     * This factory method transforms a target getter/setter method signature into a normalized
     * form that can be matched against the {@link GenericGetterSetter} interface methods.
     * The normalization process includes:
     * <ul>
     * <li>Adding the receiver object as the first parameter (Object.class)</li>
     * <li>Preserving primitive parameter types for proper overload resolution</li>
     * <li>Converting reference parameter types to Object.class for generic matching</li>
     * <li>Normalizing the return type using {@link #getReturnType(Method)}</li>
     * </ul>
     * 
     * @param method the target getter or setter method to normalize
     * @return a normalized signature that can match wrapper interface methods
     * @throws NullPointerException if method is null
     */
    public static GetterSetterSignature from(Method method)
    {
        Class<?> returnType = getReturnType(method);
        // For target method signature, we need to include the receiver as first parameter
        Class<?>[] originalParams = method.getParameterTypes();
        Class<?>[] paramTypes = new Class<?>[originalParams.length + 1];
        paramTypes[0] = Object.class; // receiver type

        // Convert parameter types for signature matching (primitives stay as-is, reference types become Object)
        for (int i = 0; i < originalParams.length; i++) {
            Class<?> paramType = originalParams[i];
            if (paramType.isPrimitive()) {
                paramTypes[i + 1] = paramType; // Keep primitives as-is for overload resolution
            } else {
                paramTypes[i + 1] = Object.class; // Convert reference types to Object
            }
        }

        return new GetterSetterSignature(method.getDeclaringClass(), returnType, method.getParameterCount() + 1, paramTypes);
    }

    /**
     * Normalizes a method's return type for signature matching purposes.
     * <p>
     * This utility method performs return type normalization to enable matching
     * between getter/setter methods and their corresponding wrapper interface methods.
     * The normalization rules are:
     * <ul>
     * <li>{@code void.class} remains unchanged (for setter methods)</li>
     * <li>All other return types (including primitives) become {@code Object.class} (for getter methods)</li>
     * </ul>
     * 
     * @param method the method whose return type should be normalized
     * @return {@code void.class} for void methods, {@code Object.class} for all others
     * @throws NullPointerException if method is null
     */
    public static Class<?> getReturnType(Method method)
    {
        Class<?> returnType = method.getReturnType();
        // For signature matching, convert all non-void types to Object for get methods
        // and keep void for set methods
        if (returnType == void.class) {
            return void.class;
        }
        return Object.class;
    }

    /**
     * Computes hash code based on signature characteristics only.
     * <p>
     * The hash code calculation includes return type, parameter count, and parameter types,
     * but deliberately excludes the declaring class to enable cross-class signature matching.
     * 
     * @return hash code for this signature
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(returnType(), paramCount(), Arrays.hashCode(paramTypes));
        // Note: declaringClass is NOT included in hash for signature matching
    }

    /**
     * Determines equality based on signature characteristics only.
     * <p>
     * Two signatures are considered equal if they have the same return type,
     * parameter count, and parameter types. The declaring class is deliberately
     * excluded from the comparison to enable matching methods from different
     * classes that have compatible signatures.
     * 
     * @param other the object to compare with this signature
     * @return true if the signatures match (excluding declaring class)
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        GetterSetterSignature otherSig = (GetterSetterSignature) other;
        return returnType().equals(otherSig.returnType()) && paramCount() == otherSig.paramCount() && Arrays.equals(paramTypes, otherSig.paramTypes);
        // Note: declaringClass is NOT compared for signature matching
    }
}