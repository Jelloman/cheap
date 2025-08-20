package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.LocalEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EntityBasicImpl implements Entity
{
    private final UUID globalId;

    public EntityBasicImpl()
    {
        this(UUID.randomUUID());
    }

    public EntityBasicImpl(UUID globalId)
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
        return null;
    }
}
