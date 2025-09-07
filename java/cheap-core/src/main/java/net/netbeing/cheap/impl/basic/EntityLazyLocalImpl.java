package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EntityLazyLocalImpl implements Entity
{
    private final UUID globalId;
    private volatile LocalEntity local;

    public EntityLazyLocalImpl(UUID globalId)
    {
        this.globalId = globalId;
    }

    @Override
    public @NotNull UUID globalId()
    {
        return globalId;
    }

    @Override
    public LocalEntity local()
    {
        if (local == null) {
            synchronized (this) {
                if (local == null) {
                    local = new LocalEntityImpl(this);
                }
            }
        }
        return local;
    }
}
