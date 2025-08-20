package net.netbeing.cheap.util.reflect;

public interface LambdaWrapper {
    <T> T get(Object caller);
    void set(Object caller, Object arg0);
}
