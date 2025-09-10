package net.netbeing.cheap.util.reflect;

/**
 * Generic interface for unified getter and setter operations across different data types.
 * <p>
 * This interface provides a common abstraction for accessing and modifying object properties
 * through reflection-based operations. It supports both object reference types and all
 * primitive types, allowing for efficient method handle-based property access without
 * the overhead of boxing/unboxing for primitive values.
 * <p>
 * The interface is designed to work with the {@link ReflectionWrapper} system to create
 * optimized lambda-based implementations that avoid the performance costs of traditional
 * reflection while maintaining type safety through method handle dispatch.
 * <p>
 * <strong>Thread Safety:</strong> Implementations are expected to be thread-safe for
 * concurrent read operations, but thread safety for write operations depends on the
 * underlying target object's thread safety characteristics.
 * 
 * @see ReflectionWrapper
 * @see GetterSetterSignature
 * @see MethodDef
 */
public interface GenericGetterSetter
{
    /**
     * Retrieves a property value from the target object.
     * <p>
     * This method provides generic access to getter methods, with the return type
     * determined by the generic type parameter. The implementation should handle
     * proper type casting and null safety.
     * 
     * @param <T> the expected return type of the property value
     * @param target the object from which to retrieve the property value
     * @return the property value cast to the specified type, may be {@code null}
     * @throws ClassCastException if the actual return type cannot be cast to T
     * @throws RuntimeException if the underlying getter method throws an exception
     */
    <T> T get(Object target);

    /**
     * Sets a property value on the target object using an Object reference.
     * <p>
     * This method handles reference type setters and provides automatic boxing
     * for primitive wrapper types.
     * 
     * @param target the object on which to set the property value
     * @param arg0 the value to set, may be {@code null} for reference types
     * @throws ClassCastException if arg0 cannot be converted to the expected parameter type
     * @throws RuntimeException if the underlying setter method throws an exception
     */
    void set(Object target, Object arg0);

    /**
     * Sets an integer property value on the target object.
     * <p>
     * This method provides direct primitive access without boxing overhead.
     * 
     * @param target the object on which to set the property value
     * @param arg0 the integer value to set
     * @throws RuntimeException if the underlying setter method throws an exception
     */
    void set(Object target, int arg0);

    /**
     * Sets a long property value on the target object.
     * <p>
     * This method provides direct primitive access without boxing overhead.
     * 
     * @param target the object on which to set the property value
     * @param arg0 the long value to set
     * @throws RuntimeException if the underlying setter method throws an exception
     */
    void set(Object target, long arg0);

    /**
     * Sets a double property value on the target object.
     * <p>
     * This method provides direct primitive access without boxing overhead.
     * 
     * @param target the object on which to set the property value
     * @param arg0 the double value to set
     * @throws RuntimeException if the underlying setter method throws an exception
     */
    void set(Object target, double arg0);

    /**
     * Sets a float property value on the target object.
     * <p>
     * This method provides direct primitive access without boxing overhead.
     * 
     * @param target the object on which to set the property value
     * @param arg0 the float value to set
     * @throws RuntimeException if the underlying setter method throws an exception
     */
    void set(Object target, float arg0);

    /**
     * Sets a boolean property value on the target object.
     * <p>
     * This method provides direct primitive access without boxing overhead.
     * 
     * @param target the object on which to set the property value
     * @param arg0 the boolean value to set
     * @throws RuntimeException if the underlying setter method throws an exception
     */
    void set(Object target, boolean arg0);

    /**
     * Sets a byte property value on the target object.
     * <p>
     * This method provides direct primitive access without boxing overhead.
     * 
     * @param target the object on which to set the property value
     * @param arg0 the byte value to set
     * @throws RuntimeException if the underlying setter method throws an exception
     */
    void set(Object target, byte arg0);

    /**
     * Sets a short property value on the target object.
     * <p>
     * This method provides direct primitive access without boxing overhead.
     * 
     * @param target the object on which to set the property value
     * @param arg0 the short value to set
     * @throws RuntimeException if the underlying setter method throws an exception
     */
    void set(Object target, short arg0);

    /**
     * Sets a character property value on the target object.
     * <p>
     * This method provides direct primitive access without boxing overhead.
     * 
     * @param target the object on which to set the property value
     * @param arg0 the character value to set
     * @throws RuntimeException if the underlying setter method throws an exception
     */
    void set(Object target, char arg0);
}
