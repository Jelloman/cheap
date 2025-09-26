package net.netbeing.cheap.model;

/**
 * Defines the metadata and characteristics of a hierarchy within the Cheap data model.
 * A hierarchy definition specifies the type, name, and mutability constraints of
 * a hierarchy instance.
 * 
 * <p>In the Cheap model, hierarchies provide the structural organization for entities
 * and aspects within a catalog. The HierarchyDef serves as the schema definition
 * that determines how a hierarchy behaves and what operations are permitted on it.</p>
 */
public interface HierarchyDef
{
    /**
     * Returns the unique name identifier for this hierarchy definition.
     * 
     * @return the hierarchy name, never null
     */
    String name();

    /**
     * Returns the type of hierarchy this definition describes.
     * The type determines the structure and behavior of hierarchy instances
     * created from this definition.
     * 
     * @return the hierarchy type, never null
     */
    HierarchyType type();

    /**
     * Determines whether hierarchies of this type can be modified after creation.
     * A modifiable hierarchy allows addition, removal, and modification of its contents.
     * 
     * @return true if the hierarchy can be modified, false otherwise; defaults to true
     */
    default boolean isModifiable()
    {
        return true;
    }

    /**
     * Tests whether this is equal to another HierarchyDef. This alternate form
     * allows equality between different HierarchyDef implementations without overriding
     * Object.equals() in every implementation.
     *
     * @param other HierarchyDef to compare to
     * @return true if this is equivalent to the other HierarchyDef
     */
    default boolean fullyEquals(HierarchyDef other)
    {
        return other.isModifiable() == isModifiable() &&
            other.name().equals(name()) &&
            other.type().equals(type());
    }
}
