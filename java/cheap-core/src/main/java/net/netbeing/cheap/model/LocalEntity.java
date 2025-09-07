package net.netbeing.cheap.model;

import java.util.Map;

public interface LocalEntity
{
    Entity entity();

    Map<AspectDef,Aspect> aspects();

    default Aspect aspect(AspectDef def) {
        return aspects().get(def);
    }
}
