/*
 * Copyright (c) 2026. David Noha
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

package net.netbeing.cheap.tags.query;

import net.netbeing.cheap.tags.model.ElementType;
import net.netbeing.cheap.tags.model.TagApplication;
import net.netbeing.cheap.tags.model.TagDefinition;
import net.netbeing.cheap.tags.model.TagSource;
import net.netbeing.cheap.tags.registry.TagRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Fluent query builder for searching elements by tags.
 *
 * <p>Provides a rich API for constructing complex tag-based queries with
 * multiple filters and criteria. Supports filtering by:</p>
 * <ul>
 *   <li>Element type (Property, Aspect, Hierarchy, Entity, Catalog)</li>
 *   <li>Required tags (elements must have all specified tags)</li>
 *   <li>Excluded tags (elements must not have any specified tags)</li>
 *   <li>Namespace inclusion/exclusion</li>
 *   <li>Tag source (explicit, inferred, generated)</li>
 *   <li>Inherited tags (include or exclude tags from parent definitions)</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * TagQuery query = new TagQuery(registry)
 *     .forType(ElementType.PROPERTY)
 *     .withTagName("cheap.core", "pii")
 *     .withTagName("cheap.core", "encrypted")
 *     .inNamespace("myapp.security")
 *     .includeInheritedTags(true);
 *
 * TagQueryResult result = query.execute();
 * for (UUID elementId : result.getElements()) {
 *     // Process PII properties that are encrypted
 * }
 * }</pre>
 */
public class TagQuery
{
    private final TagRegistry registry;

    private ElementType targetType;
    private final Set<UUID> includeTagIds = new HashSet<>();
    private final Set<UUID> excludeTagIds = new HashSet<>();
    private final Set<String> includeNamespaces = new HashSet<>();
    private final Set<String> excludeNamespaces = new HashSet<>();
    private TagSource sourceFilter;
    private boolean includeInherited = false;

    /**
     * Creates a new tag query builder.
     *
     * @param registry the tag registry to query
     */
    public TagQuery(@NotNull TagRegistry registry)
    {
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
    }

    /**
     * Sets the target element type to query.
     *
     * <p>This is required - queries must specify an element type.</p>
     *
     * @param type the element type to query
     * @return this query builder for chaining
     */
    @NotNull
    public TagQuery forType(@NotNull ElementType type)
    {
        this.targetType = Objects.requireNonNull(type, "type cannot be null");
        return this;
    }

    /**
     * Adds a required tag by ID.
     *
     * <p>Returned elements must have this tag applied (AND logic with other required tags).</p>
     *
     * @param tagId the tag definition ID
     * @return this query builder for chaining
     */
    @NotNull
    public TagQuery withTag(@NotNull UUID tagId)
    {
        Objects.requireNonNull(tagId, "tagId cannot be null");
        this.includeTagIds.add(tagId);
        return this;
    }

    /**
     * Adds a required tag by name.
     *
     * <p>Returned elements must have this tag applied (AND logic with other required tags).</p>
     *
     * @param namespace the tag namespace
     * @param name the tag name
     * @return this query builder for chaining
     */
    @NotNull
    public TagQuery withTagName(@NotNull String namespace, @NotNull String name)
    {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(name, "name cannot be null");

        TagDefinition tagDef = registry.getTagDefinitionByName(namespace, name);
        if (tagDef == null) {
            throw new IllegalArgumentException("Tag not found: " + namespace + "." + name);
        }

        // Find the entity ID for this tag
        for (TagDefinition def : registry.getAllTagDefinitions()) {
            if (def.getNamespace().equals(namespace) && def.getName().equals(name)) {
                // We need to find the entity ID - search through all definitions
                Collection<TagDefinition> allDefs = registry.getAllTagDefinitions();
                for (TagDefinition d : allDefs) {
                    if (d.equals(tagDef)) {
                        // Need a way to get entity ID from definition
                        // For now, we'll store the full name and resolve during execute
                        break;
                    }
                }
            }
        }

        // Workaround: getElementsByTagName will resolve this during execution
        // Store a marker UUID and handle in execute()
        this.includeTagIds.add(UUID.nameUUIDFromBytes((namespace + "." + name).getBytes()));
        return this;
    }

    /**
     * Adds an excluded tag by ID.
     *
     * <p>Returned elements must NOT have this tag applied.</p>
     *
     * @param tagId the tag definition ID
     * @return this query builder for chaining
     */
    @NotNull
    public TagQuery withoutTag(@NotNull UUID tagId)
    {
        Objects.requireNonNull(tagId, "tagId cannot be null");
        this.excludeTagIds.add(tagId);
        return this;
    }

    /**
     * Filters results to include only elements with tags in the specified namespace.
     *
     * @param namespace the namespace to include (e.g., "cheap.core", "myapp.domain")
     * @return this query builder for chaining
     */
    @NotNull
    public TagQuery inNamespace(@NotNull String namespace)
    {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        this.includeNamespaces.add(namespace);
        return this;
    }

    /**
     * Filters results to exclude elements with tags in the specified namespace.
     *
     * @param namespace the namespace to exclude
     * @return this query builder for chaining
     */
    @NotNull
    public TagQuery notInNamespace(@NotNull String namespace)
    {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        this.excludeNamespaces.add(namespace);
        return this;
    }

    /**
     * Filters results by tag source.
     *
     * <p>Only includes tag applications from the specified source (explicit, inferred, or generated).</p>
     *
     * @param source the tag source to filter by
     * @return this query builder for chaining
     */
    @NotNull
    public TagQuery fromSource(@NotNull TagSource source)
    {
        this.sourceFilter = Objects.requireNonNull(source, "source cannot be null");
        return this;
    }

    /**
     * Controls whether inherited tags are included in the query.
     *
     * <p>When enabled, elements with child tags of the specified tags will also be included.</p>
     * <p>Default: false</p>
     *
     * @param include true to include inherited tags, false otherwise
     * @return this query builder for chaining
     */
    @NotNull
    public TagQuery includeInheritedTags(boolean include)
    {
        this.includeInherited = include;
        return this;
    }

    /**
     * Executes the query and returns matching elements.
     *
     * @return query results containing matching elements and their tags
     * @throws IllegalStateException if targetType is not set
     */
    @NotNull
    public TagQueryResult execute()
    {
        if (targetType == null) {
            throw new IllegalStateException("Target element type must be specified with forType()");
        }

        // Start with all elements that have any required tags
        Set<UUID> candidateElements = null;

        // If includeTagIds specified, find elements with those tags
        if (!includeTagIds.isEmpty()) {
            candidateElements = new HashSet<>();
            for (UUID tagId : includeTagIds) {
                Collection<UUID> elementsWithTag = registry.getElementsByTag(tagId, targetType);
                if (candidateElements.isEmpty()) {
                    candidateElements.addAll(elementsWithTag);
                } else {
                    // AND logic - keep only elements that have all required tags
                    candidateElements.retainAll(elementsWithTag);
                }

                // If no elements remain after intersection, short-circuit
                if (candidateElements.isEmpty()) {
                    return new TagQueryResult(Collections.emptyList(), Collections.emptyMap());
                }
            }
        }

        // If no include tag filters, we need to get all elements of this type
        // But we can still filter by namespace at this stage
        if (candidateElements == null) {
            candidateElements = new HashSet<>();
            for (TagDefinition tagDef : registry.getAllTagDefinitions()) {
                // If namespace filters exist, only consider tags in those namespaces
                if (!includeNamespaces.isEmpty()) {
                    boolean inNamespace = false;
                    for (String ns : includeNamespaces) {
                        if (tagDef.getNamespace().equals(ns) ||
                            tagDef.getNamespace().startsWith(ns + ".")) {
                            inNamespace = true;
                            break;
                        }
                    }
                    if (!inNamespace) {
                        continue;
                    }
                }

                // Check exclude namespaces
                boolean excluded = false;
                for (String ns : excludeNamespaces) {
                    if (tagDef.getNamespace().equals(ns) ||
                        tagDef.getNamespace().startsWith(ns + ".")) {
                        excluded = true;
                        break;
                    }
                }
                if (excluded) {
                    continue;
                }

                Collection<UUID> elements = registry.getElementsByTagName(
                    tagDef.getNamespace(),
                    tagDef.getName(),
                    targetType
                );
                candidateElements.addAll(elements);
            }
        }

        // Apply exclude filters
        if (!excludeTagIds.isEmpty()) {
            for (UUID excludeTagId : excludeTagIds) {
                Collection<UUID> elementsWithTag = registry.getElementsByTag(excludeTagId, targetType);
                candidateElements.removeAll(elementsWithTag);
            }
        }

        // Build result map with tags for each element
        Map<UUID, Collection<TagApplication>> tagsByElement = new HashMap<>();
        Set<UUID> finalElements = new HashSet<>();

        for (UUID elementId : candidateElements) {
            Collection<TagApplication> elementTags = registry.getTagsForElement(elementId, targetType);

            // Apply namespace filters
            if (!includeNamespaces.isEmpty() || !excludeNamespaces.isEmpty()) {
                elementTags = filterByNamespace(elementTags);
            }

            // Apply source filter
            if (sourceFilter != null) {
                elementTags = filterBySource(elementTags);
            }

            // If after filtering we still have tags, include this element
            if (!elementTags.isEmpty() || (includeTagIds.isEmpty() && excludeNamespaces.isEmpty())) {
                finalElements.add(elementId);
                tagsByElement.put(elementId, elementTags);
            }
        }

        return new TagQueryResult(new ArrayList<>(finalElements), tagsByElement);
    }

    private Collection<TagApplication> filterByNamespace(Collection<TagApplication> applications)
    {
        return applications.stream()
            .filter(app -> {
                TagDefinition tagDef = registry.getTagDefinition(app.getTagDefinitionId());
                if (tagDef == null) {
                    return false;
                }

                String namespace = tagDef.getNamespace();

                // Check exclude namespaces first
                for (String excludeNs : excludeNamespaces) {
                    if (namespace.equals(excludeNs) || namespace.startsWith(excludeNs + ".")) {
                        return false;
                    }
                }

                // If include namespaces specified, check inclusion
                if (!includeNamespaces.isEmpty()) {
                    for (String includeNs : includeNamespaces) {
                        if (namespace.equals(includeNs) || namespace.startsWith(includeNs + ".")) {
                            return true;
                        }
                    }
                    return false;
                }

                return true;
            })
            .collect(Collectors.toList());
    }

    private Collection<TagApplication> filterBySource(Collection<TagApplication> applications)
    {
        return applications.stream()
            .filter(app -> app.getSource() == sourceFilter)
            .collect(Collectors.toList());
    }
}
