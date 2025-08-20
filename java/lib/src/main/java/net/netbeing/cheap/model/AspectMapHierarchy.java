package net.netbeing.cheap.model;

import java.util.Map;

public interface AspectMapHierarchy extends Hierarchy, Map<Entity,Aspect>
{
    AspectDef aspectDef();

    default Aspect add(Aspect a) {
        return put(a.entity(), a);
    }
}
