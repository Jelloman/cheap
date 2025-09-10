package net.netbeing.cheap.util.reflect;

public interface GenericGetterSetter
{
    <T> T get(Object target);
    void set(Object target, Object arg0);
    void set(Object target, int arg0);
    void set(Object target, long arg0);
    void set(Object target, double arg0);
    void set(Object target, float arg0);
    void set(Object target, boolean arg0);
    void set(Object target, byte arg0);
    void set(Object target, short arg0);
    void set(Object target, char arg0);
}
