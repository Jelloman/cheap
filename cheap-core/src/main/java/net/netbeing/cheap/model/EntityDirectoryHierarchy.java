package net.netbeing.cheap.model;

import java.util.Map;

/**
 * A hierarchy that maps string keys to entity references, representing the
 * ENTITY_DIR (ED) hierarchy type in the Cheap model. This hierarchy type provides
 * dictionary-like lookups of entities by string identifiers.
 *
 * <p>This hierarchy combines the standard hierarchy interface with Map functionality,
 * enabling efficient key-based access to entities through string identifiers. It is
 * analogous to a file directory (hence the name) where files are accessed by name,
 * or a database table with a string primary key.</p>
 *
 * <p>The mapping is one-to-one: each string key maps to at most one entity, though
 * multiple keys can map to the same entity if the implementation permits. The keys
 * are typically meaningful names, identifiers, or paths that provide semantic access
 * to entities.</p>
 *
 * <p>Common use cases for EntityDirectoryHierarchy include:</p>
 * <ul>
 *   <li>Named entity registries where entities are accessed by symbolic names</li>
 *   <li>Path-based entity lookups (e.g., "/users/john", "config/settings")</li>
 *   <li>String-keyed indices for quick entity retrieval</li>
 *   <li>Implementing named slots or variables in configuration systems</li>
 *   <li>Mapping external identifiers (SKUs, usernames, etc.) to internal entities</li>
 *   <li>Building simple flat namespaces for entity organization</li>
 * </ul>
 *
 * <p>For hierarchical path-based organization with parent-child relationships,
 * consider using {@link EntityTreeHierarchy} instead, which provides tree structure
 * and navigation capabilities beyond simple key-value mapping.</p>
 *
 * @see Hierarchy
 * @see EntityTreeHierarchy
 * @see HierarchyType#ENTITY_DIR
 */
public interface EntityDirectoryHierarchy extends Hierarchy, Map<String,Entity>
{
}
