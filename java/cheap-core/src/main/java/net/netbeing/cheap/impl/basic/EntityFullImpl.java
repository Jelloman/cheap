package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;

import java.util.Map;
import java.util.UUID;

public class EntityFullImpl extends EntityBasicImpl implements LocalEntity
{
    protected volatile WeakAspectMap aspects;

    public EntityFullImpl()
    {
    }

    public EntityFullImpl(UUID globalId)
    {
        super(globalId);
    }

    public EntityFullImpl(UUID globalId, Aspect initialAspect)
    {
        super(globalId);
        this.aspects = new WeakAspectMap(3);
        this.aspects.add(initialAspect);
    }

    @Override
    public LocalEntity local()
    {
        return this;
    }

    @Override
    public Entity entity()
    {
        return this;
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
