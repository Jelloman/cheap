package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * The interface Entity.
 */
public interface Entity
{
    /**
     * Global id uuid.
     *
     * @return the uuid
     */
    @NotNull UUID globalId();

    /**
     * Local local entity.
     *
     * @return the local entity
     */
    LocalEntity local();
}
