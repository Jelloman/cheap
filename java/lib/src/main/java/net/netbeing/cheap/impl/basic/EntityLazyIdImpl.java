package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EntityLazyIdImpl implements Entity
{
    private volatile UUID globalId;
    private final LocalEntity local;

    public EntityLazyIdImpl(LocalEntity local)
    {
        this.local = local;
    }

    public EntityLazyIdImpl(Aspect aspect)
    {
        this.local = new LocalEntityImpl(this, aspect);
    }

    @Override
    public @NotNull UUID globalId()
    {
        if (globalId == null) {
            synchronized (this) {
                if (globalId == null) {
                    globalId = UUID.randomUUID();
                }
            }
        }
        return globalId;
    }

    @Override
    public LocalEntity local()
    {
        return local;
    }
}
