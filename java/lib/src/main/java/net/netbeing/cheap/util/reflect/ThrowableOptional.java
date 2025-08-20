package net.netbeing.cheap.util.reflect;

public class ThrowableOptional {
    public static <T> T sneaky(SneakySupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}