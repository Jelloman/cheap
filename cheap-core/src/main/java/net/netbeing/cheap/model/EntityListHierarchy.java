package net.netbeing.cheap.model;

import java.util.List;

/**
 * A hierarchy that maintains an ordered list of entity references, representing the
 * ENTITY_LIST (EL) hierarchy type in the Cheap model. This hierarchy type allows
 * duplicate entity references and preserves insertion order.
 *
 * <p>This hierarchy combines the standard hierarchy interface with List functionality,
 * providing indexed access to entities and supporting operations like sequential
 * traversal, positional insertion, and duplicate entries. It is analogous to a
 * database table with no primary key constraint or a file manifest that can contain
 * multiple references to the same file.</p>
 *
 * <p>Use cases for EntityListHierarchy include:</p>
 * <ul>
 *   <li>Maintaining processing queues or work lists where entities may appear multiple times</li>
 *   <li>Recording event sequences or audit trails where the same entity may be logged repeatedly</li>
 *   <li>Preserving ordered collections where position matters (e.g., playlist, ranked lists)</li>
 *   <li>Modeling many-to-many relationships with ordering and multiplicity</li>
 * </ul>
 *
 * <p>The list can contain null elements if the underlying implementation permits,
 * though this is generally discouraged to maintain referential integrity.</p>
 *
 * @see Hierarchy
 * @see EntitySetHierarchy
 * @see HierarchyType#ENTITY_LIST
 */
public interface EntityListHierarchy extends Hierarchy, List<Entity>
{
}
