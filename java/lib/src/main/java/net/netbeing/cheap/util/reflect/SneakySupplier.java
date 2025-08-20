package net.netbeing.cheap.util.reflect;

public interface SneakySupplier<T> {

    T get() throws Throwable;

}
