package net.netbeing.cheap.model;

public interface AspectDefDirHierarchy extends Hierarchy
{
    AspectDef add(AspectDef def);

    AspectDef get(String name);
}
