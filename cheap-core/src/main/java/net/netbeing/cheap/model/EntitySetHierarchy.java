/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.model;

import java.util.Set;

/**
 * A hierarchy that maintains a possibly-ordered set of unique entity references,
 * representing the ENTITY_SET (ES) hierarchy type in the Cheap model. This hierarchy
 * type ensures that each entity appears at most once in the collection.
 *
 * <p>This hierarchy combines the standard hierarchy interface with Set functionality,
 * providing efficient membership testing and automatic duplicate elimination. The set
 * may be ordered (e.g., using a LinkedHashSet implementation) or unordered (e.g., using
 * a HashSet implementation), depending on the specific implementation chosen.</p>
 *
 * <p>Unlike {@link EntityListHierarchy}, this hierarchy enforces uniqueness of entity
 * references, making it analogous to a database table with a primary key constraint or
 * a collection of unique file references. This makes EntitySetHierarchy ideal for:</p>
 * <ul>
 *   <li>Maintaining collections of unique entities without duplicates</li>
 *   <li>Implementing tag or category memberships where each entity belongs once</li>
 *   <li>Managing entity pools or registries requiring fast membership checks</li>
 *   <li>Representing one-to-many relationships where uniqueness is required</li>
 *   <li>Building indices or lookup structures for entities</li>
 * </ul>
 *
 * <p>The uniqueness constraint is based on entity equality, which in the Cheap model
 * is determined solely by the entity's global ID. This means two Entity instances with
 * the same UUID are considered equal and only one will be retained in the set.</p>
 *
 * @see Hierarchy
 * @see EntityListHierarchy
 * @see HierarchyType#ENTITY_SET
 */
public interface EntitySetHierarchy extends Hierarchy, Set<Entity>
{
}
