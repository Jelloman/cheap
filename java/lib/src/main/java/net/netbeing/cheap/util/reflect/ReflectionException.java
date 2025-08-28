package net.netbeing.cheap.util.reflect;

public class ReflectionException extends RuntimeException {

    public ReflectionException(String message) {
        super(message);
    }

    public static ReflectionException format(String msg, Object... args) {
        return new ReflectionException(String.format(msg, args));
    }
}