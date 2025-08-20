package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BasicEntityImpl implements Entity
{
    private final UUID globalId;
    private LocalEntity local;

    public BasicEntityImpl()
    {
        this(UUID.randomUUID(), null);
    }

    public BasicEntityImpl(UUID globalId)
    {
        this(globalId, null);
    }

    public BasicEntityImpl(UUID globalId, LocalEntity local)
    {
        this.globalId = globalId;
        this.local = local;
    }

    @Override
    public @NotNull UUID globalId()
    {
        return globalId;
    }

    @Override
    public LocalEntity local()
    {
        return local;
    }

    public void setLocal(LocalEntity local)
    {
        this.local = local;
    }
}
