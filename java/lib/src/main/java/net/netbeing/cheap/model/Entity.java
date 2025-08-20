package net.netbeing.cheap.model;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface Entity
{
    @NotNull UUID globalId();

    LocalEntity local();
}
