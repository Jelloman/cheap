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

package net.netbeing.cheap.tags.validation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.netbeing.cheap.tags.model.ElementType;
import net.netbeing.cheap.tags.model.TagApplication;
import net.netbeing.cheap.tags.model.TagDefinition;
import net.netbeing.cheap.tags.registry.TagRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Detects semantic conflicts between tags applied to the same element.
 *
 * <p>This class identifies mutually exclusive or contradictory tags that
 * should not be applied together. For example:</p>
 * <ul>
 *   <li>immutable + modified-timestamp (immutable data shouldn't track modifications)</li>
 *   <li>required + nullable (a field can't be both required and nullable)</li>
 *   <li>encrypted + masked (redundant privacy controls)</li>
 * </ul>
 *
 * <p>Conflict rules are defined as static mappings and can be extended
 * in future versions to support custom conflict detection logic.</p>
 */
public class TagConflictDetector
{
    /**
     * Static map of conflicting tag pairs.
     * Key: tag full name, Value: set of conflicting tag full names
     */
    private static final Map<String, Set<String>> CONFLICTING_TAGS = ImmutableMap.<String, Set<String>>builder()
        // Immutability conflicts
        .put("cheap.core.immutable", ImmutableSet.of(
            "cheap.core.modified-timestamp",
            "cheap.core.version-number"
        ))
        .put("cheap.core.modified-timestamp", ImmutableSet.of(
            "cheap.core.immutable"
        ))
        .put("cheap.core.version-number", ImmutableSet.of(
            "cheap.core.immutable"
        ))

        // Nullability conflicts
        .put("cheap.core.required", ImmutableSet.of(
            "cheap.core.nullable"
        ))
        .put("cheap.core.nullable", ImmutableSet.of(
            "cheap.core.required"
        ))

        // Privacy conflicts (redundant or contradictory)
        .put("cheap.core.encrypted", ImmutableSet.of(
            "cheap.core.masked"
        ))
        .put("cheap.core.masked", ImmutableSet.of(
            "cheap.core.encrypted"
        ))

        // Lifecycle conflicts
        .put("cheap.core.soft-delete-flag", ImmutableSet.of(
            "cheap.core.immutable"
        ))
        .put("cheap.core.archived-flag", ImmutableSet.of(
            "cheap.core.immutable"
        ))

        // Key conflicts
        .put("cheap.core.primary-key", ImmutableSet.of(
            "cheap.core.nullable"
        ))
        .put("cheap.core.foreign-key", ImmutableSet.of(
            "cheap.core.primary-key"  // A field shouldn't be both PK and FK
        ))

        .build();

    private final TagRegistry registry;

    /**
     * Constructs a new TagConflictDetector.
     *
     * @param registry the tag registry to use for lookups
     */
    public TagConflictDetector(@NotNull TagRegistry registry)
    {
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
    }

    /**
     * Detects conflicts among tags applied to an element.
     *
     * <p>This method checks all tags on the element (including inherited tags)
     * against the conflict rules and returns descriptions of any conflicts found.</p>
     *
     * @param elementId UUID of the element
     * @param type type of the element
     * @return list of conflict descriptions (empty if no conflicts)
     */
    @NotNull
    public List<String> detectConflicts(@NotNull UUID elementId, @NotNull ElementType type)
    {
        Objects.requireNonNull(elementId, "elementId cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        List<String> conflicts = new ArrayList<>();

        // Get all tags on the element
        Collection<TagApplication> applications = registry.getTagsForElement(elementId, type);

        // Build set of all tag full names (including inherited tags)
        Set<String> tagFullNames = new HashSet<>();
        Map<String, TagDefinition> tagsByName = new HashMap<>();

        for (TagApplication app : applications) {
            TagDefinition tagDef = registry.getTagDefinition(app.getTagDefinitionId());
            if (tagDef != null) {
                tagFullNames.add(tagDef.getFullName());
                tagsByName.put(tagDef.getFullName(), tagDef);

                // Include inherited tags
                Collection<UUID> ancestors = registry.getAllAncestorTags(app.getTagDefinitionId());
                for (UUID ancestorId : ancestors) {
                    TagDefinition ancestorTag = registry.getTagDefinition(ancestorId);
                    if (ancestorTag != null) {
                        tagFullNames.add(ancestorTag.getFullName());
                        tagsByName.put(ancestorTag.getFullName(), ancestorTag);
                    }
                }
            }
        }

        // Check for conflicts
        for (String tagName : tagFullNames) {
            Set<String> conflictingTags = getConflictingTags(tagName);
            for (String conflictingTag : conflictingTags) {
                if (tagFullNames.contains(conflictingTag)) {
                    conflicts.add(
                        "Conflicting tags: '" + tagName + "' conflicts with '" + conflictingTag + "'"
                    );
                }
            }
        }

        return conflicts;
    }

    /**
     * Detects if applying a specific tag would create conflicts with existing tags.
     *
     * <p>This is useful for validation before applying a tag.</p>
     *
     * @param elementId UUID of the element
     * @param type type of the element
     * @param tagDefId UUID of the tag definition to check
     * @return list of conflict descriptions (empty if no conflicts)
     */
    @NotNull
    public List<String> detectConflictsForNewTag(
        @NotNull UUID elementId,
        @NotNull ElementType type,
        @NotNull UUID tagDefId)
    {
        Objects.requireNonNull(elementId, "elementId cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(tagDefId, "tagDefId cannot be null");

        List<String> conflicts = new ArrayList<>();

        TagDefinition newTagDef = registry.getTagDefinition(tagDefId);
        if (newTagDef == null) {
            return conflicts;
        }

        // Get existing tags on element
        Collection<TagApplication> existingApps = registry.getTagsForElement(elementId, type);

        // Build set of existing tag names (including inherited)
        Set<String> existingTagNames = new HashSet<>();
        for (TagApplication app : existingApps) {
            TagDefinition tagDef = registry.getTagDefinition(app.getTagDefinitionId());
            if (tagDef != null) {
                existingTagNames.add(tagDef.getFullName());

                // Include inherited tags
                Collection<UUID> ancestors = registry.getAllAncestorTags(app.getTagDefinitionId());
                for (UUID ancestorId : ancestors) {
                    TagDefinition ancestorTag = registry.getTagDefinition(ancestorId);
                    if (ancestorTag != null) {
                        existingTagNames.add(ancestorTag.getFullName());
                    }
                }
            }
        }

        // Check if new tag conflicts with any existing tag
        String newTagName = newTagDef.getFullName();
        Set<String> conflictingTags = getConflictingTags(newTagName);
        for (String conflictingTag : conflictingTags) {
            if (existingTagNames.contains(conflictingTag)) {
                conflicts.add(
                    "Cannot apply tag '" + newTagName + "': conflicts with existing tag '" + conflictingTag + "'"
                );
            }
        }

        // Also check inherited tags of the new tag
        Collection<UUID> newTagAncestors = registry.getAllAncestorTags(tagDefId);
        for (UUID ancestorId : newTagAncestors) {
            TagDefinition ancestorTag = registry.getTagDefinition(ancestorId);
            if (ancestorTag != null) {
                String ancestorName = ancestorTag.getFullName();
                Set<String> ancestorConflicts = getConflictingTags(ancestorName);
                for (String conflictingTag : ancestorConflicts) {
                    if (existingTagNames.contains(conflictingTag)) {
                        conflicts.add(
                            "Cannot apply tag '" + newTagName + "': inherited tag '" + ancestorName +
                            "' conflicts with existing tag '" + conflictingTag + "'"
                        );
                    }
                }
            }
        }

        return conflicts;
    }

    /**
     * Gets the set of tags that conflict with the specified tag.
     *
     * @param tagFullName the full name of the tag (namespace.name)
     * @return set of conflicting tag full names (never null, but may be empty)
     */
    @NotNull
    public Set<String> getConflictingTags(@NotNull String tagFullName)
    {
        Objects.requireNonNull(tagFullName, "tagFullName cannot be null");

        Set<String> conflicts = CONFLICTING_TAGS.get(tagFullName);
        return conflicts != null ? conflicts : Collections.emptySet();
    }

    /**
     * Checks if two tags conflict with each other.
     *
     * @param tag1FullName full name of first tag
     * @param tag2FullName full name of second tag
     * @return true if the tags conflict, false otherwise
     */
    public boolean tagsConflict(@NotNull String tag1FullName, @NotNull String tag2FullName)
    {
        Objects.requireNonNull(tag1FullName, "tag1FullName cannot be null");
        Objects.requireNonNull(tag2FullName, "tag2FullName cannot be null");

        Set<String> tag1Conflicts = getConflictingTags(tag1FullName);
        return tag1Conflicts.contains(tag2FullName);
    }

    /**
     * Returns all defined conflict rules.
     *
     * @return immutable map of all conflict rules
     */
    @NotNull
    public Map<String, Set<String>> getAllConflictRules()
    {
        return CONFLICTING_TAGS;
    }
}
