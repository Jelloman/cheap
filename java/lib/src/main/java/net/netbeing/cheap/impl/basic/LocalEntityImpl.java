package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;

import java.util.Map;

public class LocalEntityImpl implements LocalEntity
{
    protected final Entity entity;
    protected volatile WeakAspectMap aspects;

    public LocalEntityImpl(Entity entity)
    {
        this.entity = entity;
    }

    public LocalEntityImpl(Entity entity, Aspect initialAspect)
    {
        this.entity = entity;
        this.aspects = new WeakAspectMap(3);
        this.aspects.add(initialAspect);
    }

    @Override
    public Entity entity()
    {
        return entity;
    }

    public Map<AspectDef,Aspect> aspects() {
        if (aspects == null) {
            synchronized(this) {
                if (aspects == null) {
                    aspects = new WeakAspectMap(2);
                }
            }
        }
        return aspects;
    }

    public Aspect aspect(AspectDef def) {
        if (aspects != null) {
            return aspects.get(def);
        }
        return aspects().get(def);
    }

}
