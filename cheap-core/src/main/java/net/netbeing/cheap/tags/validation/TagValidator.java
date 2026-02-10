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

import net.netbeing.cheap.tags.model.ElementType;
import net.netbeing.cheap.tags.model.TagDefinition;
import net.netbeing.cheap.tags.registry.TagRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Validates tag definitions and applications.
 *
 * <p>This class provides comprehensive validation for:</p>
 * <ul>
 *   <li>Tag definition format (namespace, name)</li>
 *   <li>Tag application rules (applicability, existence)</li>
 *   <li>Tag inheritance (parent existence, circular references)</li>
 * </ul>
 */
public class TagValidator
{
    private final TagRegistry registry;

    /**
     * Constructs a new TagValidator.
     *
     * @param registry the tag registry to validate against
     */
    public TagValidator(@NotNull TagRegistry registry)
    {
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
    }

    /**
     * Validates a tag definition before it is stored.
     *
     * @param definition the tag definition to validate
     * @return list of validation error messages (empty if valid)
     */
    @NotNull
    public List<String> validateTagDefinition(@NotNull TagDefinition definition)
    {
        Objects.requireNonNull(definition, "definition cannot be null");

        List<String> errors = new ArrayList<>();

        // Validate namespace format
        if (!isNamespaceValid(definition.getNamespace())) {
            errors.add("Invalid namespace format: " + definition.getNamespace() +
                      " (must be lowercase with dots, e.g., 'cheap.core' or 'myapp.domain')");
        }

        // Validate name format
        if (!isNameValid(definition.getName())) {
            errors.add("Invalid name format: " + definition.getName() +
                      " (must be lowercase with hyphens, e.g., 'primary-key' or 'created-timestamp')");
        }

        // Validate appliesTo is not empty
        if (definition.getAppliesTo().isEmpty()) {
            errors.add("Tag must apply to at least one element type");
        }

        // Validate parent tags exist
        for (UUID parentTagId : definition.getParentTagIds()) {
            TagDefinition parentTag = registry.getTagDefinition(parentTagId);
            if (parentTag == null) {
                errors.add("Parent tag not found: " + parentTagId);
            }
        }

        // Detect circular inheritance (if this tag has parents)
        if (!definition.getParentTagIds().isEmpty()) {
            // We can't fully detect circular inheritance until the tag is created,
            // but we can check if any parent already has this tag's name
            String fullName = definition.getFullName();
            for (UUID parentTagId : definition.getParentTagIds()) {
                if (wouldCreateCircularInheritance(parentTagId, fullName, definition.getParentTagIds())) {
                    errors.add("Circular inheritance detected: tag would inherit from itself");
                    break;
                }
            }
        }

        return errors;
    }

    /**
     * Validates a tag application before it is applied.
     *
     * @param tagDefId UUID of the tag definition
     * @param targetId UUID of the target element
     * @param targetType type of the target element
     * @return list of validation error messages (empty if valid)
     */
    @NotNull
    public List<String> validateTagApplication(
        @NotNull UUID tagDefId,
        @NotNull UUID targetId,
        @NotNull ElementType targetType)
    {
        Objects.requireNonNull(tagDefId, "tagDefId cannot be null");
        Objects.requireNonNull(targetId, "targetId cannot be null");
        Objects.requireNonNull(targetType, "targetType cannot be null");

        List<String> errors = new ArrayList<>();

        // Check tag definition exists
        TagDefinition tagDef = registry.getTagDefinition(tagDefId);
        if (tagDef == null) {
            errors.add("Tag definition not found: " + tagDefId);
            return errors;
        }

        // Check tag is applicable to target type
        if (!tagDef.isApplicableTo(targetType)) {
            errors.add("Tag " + tagDef.getFullName() + " cannot be applied to " + targetType +
                      " (applicable to: " + tagDef.getAppliesTo() + ")");
        }

        return errors;
    }

    /**
     * Validates a namespace format.
     *
     * <p>Valid namespace format:</p>
     * <ul>
     *   <li>Lowercase letters, numbers, dots, and hyphens only</li>
     *   <li>Must contain at least one dot</li>
     *   <li>Cannot start or end with dot or hyphen</li>
     *   <li>Examples: "cheap.core", "myapp.domain", "org.example.tags"</li>
     * </ul>
     *
     * @param namespace the namespace to validate
     * @return true if valid, false otherwise
     */
    public boolean isNamespaceValid(@NotNull String namespace)
    {
        if (namespace == null || namespace.isEmpty()) {
            return false;
        }

        // Must contain at least one dot and use lowercase letters, numbers, dots, and hyphens
        // Pattern: lowercase/number segments separated by dots or hyphens
        return namespace.matches("^[a-z0-9]+([.-][a-z0-9]+)+$");
    }

    /**
     * Validates a tag name format.
     *
     * <p>Valid name format:</p>
     * <ul>
     *   <li>Lowercase letters, numbers, and hyphens only</li>
     *   <li>Cannot start or end with hyphen</li>
     *   <li>Examples: "primary-key", "created-timestamp", "pii"</li>
     * </ul>
     *
     * @param name the name to validate
     * @return true if valid, false otherwise
     */
    public boolean isNameValid(@NotNull String name)
    {
        if (name == null || name.isEmpty()) {
            return false;
        }

        // Must use lowercase letters, numbers, and hyphens (no leading/trailing hyphens)
        return name.matches("^[a-z0-9]+(-[a-z0-9]+)*$");
    }

    /**
     * Detects if creating a tag with the given properties would create circular inheritance.
     *
     * <p>This method checks if any parent in the inheritance chain already references
     * a tag with the same full name, or if adding these parents would create a cycle.</p>
     *
     * @param parentTagId the parent tag ID to check
     * @param childFullName the full name of the child tag being created
     * @param allParentIds all parent IDs of the child tag
     * @return true if circular inheritance would be created, false otherwise
     */
    private boolean wouldCreateCircularInheritance(
        UUID parentTagId,
        String childFullName,
        List<UUID> allParentIds)
    {
        TagDefinition parentTag = registry.getTagDefinition(parentTagId);
        if (parentTag == null) {
            return false;
        }

        // Check if parent has the same name (shouldn't happen, but check anyway)
        if (parentTag.getFullName().equals(childFullName)) {
            return true;
        }

        // Check if any ancestor of the parent is in the child's parent list
        // (this would create a cycle)
        Collection<UUID> ancestorIds = registry.getAllAncestorTags(parentTagId);
        for (UUID ancestorId : ancestorIds) {
            if (allParentIds.contains(ancestorId)) {
                // This would create a cycle: child -> parent -> ancestor -> parent (again)
                return true;
            }
        }

        return false;
    }

    /**
     * Detects if a tag definition has circular inheritance.
     *
     * <p>This method traverses the parent tag chain to detect if the tag
     * appears in its own ancestry.</p>
     *
     * @param tagId the tag ID to check
     * @return true if circular inheritance exists, false otherwise
     */
    public boolean detectCircularInheritance(@NotNull UUID tagId)
    {
        Objects.requireNonNull(tagId, "tagId cannot be null");

        Set<UUID> visited = new HashSet<>();
        return detectCircularInheritanceRecursive(tagId, visited);
    }

    /**
     * Recursively detects circular inheritance.
     *
     * @param tagId the current tag ID
     * @param visited set of already visited tag IDs
     * @return true if circular inheritance detected, false otherwise
     */
    private boolean detectCircularInheritanceRecursive(UUID tagId, Set<UUID> visited)
    {
        if (visited.contains(tagId)) {
            return true;  // Circular reference detected
        }

        visited.add(tagId);

        Collection<UUID> parents = registry.getParentTags(tagId);
        for (UUID parentId : parents) {
            if (detectCircularInheritanceRecursive(parentId, visited)) {
                return true;
            }
        }

        visited.remove(tagId);  // Backtrack
        return false;
    }
}
