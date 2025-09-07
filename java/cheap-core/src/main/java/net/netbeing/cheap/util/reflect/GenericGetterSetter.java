package net.netbeing.cheap.util.reflect;

public interface GenericGetterSetter
{
    <T> T get(Object caller);
    void set(Object caller, Object arg0);
    
    // Primitive setters
    void set(Object caller, int arg0);
    void set(Object caller, long arg0);
    void set(Object caller, double arg0);
    void set(Object caller, float arg0);
    void set(Object caller, boolean arg0);
    void set(Object caller, byte arg0);
    void set(Object caller, short arg0);
    void set(Object caller, char arg0);
}
