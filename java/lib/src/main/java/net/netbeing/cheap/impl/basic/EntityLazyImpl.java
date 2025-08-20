package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EntityLazyImpl implements Entity
{
    private volatile UUID globalId;
    private volatile LocalEntity local;

    public EntityLazyImpl()
    {
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
