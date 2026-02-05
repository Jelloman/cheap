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

package net.netbeing.cheap.tags.registry;

import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.tags.model.ElementType;
import net.netbeing.cheap.tags.model.TagApplication;
import net.netbeing.cheap.tags.model.TagDefinition;
import net.netbeing.cheap.tags.model.TagSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Central registry for managing tag definitions and applications in the CHEAP framework.
 *
 * <p>The TagRegistry provides a complete API for:</p>
 * <ul>
 *   <li>Defining and managing tag definitions</li>
 *   <li>Applying and removing tags from CHEAP elements</li>
 *   <li>Querying tags by element, name, or namespace</li>
 *   <li>Validating tag applications</li>
 *   <li>Managing tag inheritance hierarchies</li>
 *   <li>Initializing standard cheap.core tags</li>
 * </ul>
 *
 * <p>All tag data is stored in CHEAP hierarchies within the provided catalog, using the
 * following storage structure:</p>
 * <ul>
 *   <li><b>tag_definitions</b> - AspectMapHierarchy storing all tag definitions</li>
 *   <li><b>tag_applications_*</b> - AspectMapHierarchies for tag applications (one per ElementType)</li>
 *   <li><b>tag_index_by_name</b> - EntityDirectoryHierarchy for fast name lookups</li>
 *   <li><b>tags_by_element</b> - EntityDirectoryHierarchy for fast element lookups</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create registry
 * Catalog catalog = factory.createCatalog(...);
 * TagRegistry registry = TagRegistry.create(catalog, factory);
 *
 * // Initialize standard tags
 * registry.initializeStandardTags();
 *
 * // Define custom tag
 * TagDefinition invoiceTag = new TagDefinition(...);
 * Entity tagEntity = registry.defineTag(invoiceTag);
 *
 * // Apply tag to element
 * UUID propertyId = ...;
 * Entity application = registry.applyTag(
 *     propertyId,
 *     ElementType.PROPERTY,
 *     tagEntity.globalId(),
 *     Map.of("format", "INV-{year}-{seq}"),
 *     TagSource.EXPLICIT
 * );
 *
 * // Query tags
 * Collection<TagApplication> tags = registry.getTagsForElement(propertyId, ElementType.PROPERTY);
 * }</pre>
 *
 * @see TagDefinition
 * @see TagApplication
 * @see TagRegistryImpl
 */
public interface TagRegistry
{
    /**
     * Creates a new TagRegistry instance for the specified catalog.
     *
     * <p>This factory method initializes all required hierarchies and indices
     * in the catalog. If the hierarchies already exist, they will be reused.</p>
     *
     * @param catalog the catalog to store tag data in
     * @param factory the factory to use for creating CHEAP objects
     * @return a new TagRegistry instance
     * @throws NullPointerException if catalog or factory is null
     */
    @NotNull
    static TagRegistry create(@NotNull Catalog catalog, @NotNull CheapFactory factory)
    {
        return new TagRegistryImpl(catalog, factory);
    }

    /**
     * Returns the catalog that this registry stores tag data in.
     *
     * @return the catalog (never null)
     */
    @NotNull
    Catalog catalog();

    // ==================== Tag Definition Management ====================

    /**
     * Defines a new tag in the registry.
     *
     * <p>This creates an entity for the tag definition, attaches a TagDefinitionAspect,
     * and stores it in the tag_definitions hierarchy. The tag is also indexed by name
     * for fast lookup.</p>
     *
     * <p>Tag definitions are validated before being stored. Validation includes:</p>
     * <ul>
     *   <li>Namespace and name format validation</li>
     *   <li>Duplicate name detection</li>
     *   <li>Parent tag existence verification</li>
     *   <li>Circular inheritance detection</li>
     * </ul>
     *
     * @param definition the tag definition to create
     * @return the entity containing the tag definition
     * @throws NullPointerException if definition is null
     * @throws IllegalArgumentException if validation fails
     */
    @NotNull
    Entity defineTag(@NotNull TagDefinition definition);

    /**
     * Retrieves a tag definition by its entity UUID.
     *
     * @param tagEntityId the UUID of the tag definition entity
     * @return the tag definition, or null if not found
     * @throws NullPointerException if tagEntityId is null
     */
    @Nullable
    TagDefinition getTagDefinition(@NotNull UUID tagEntityId);

    /**
     * Retrieves a tag definition by its qualified name.
     *
     * <p>The qualified name is in the format "namespace.name" (e.g., "cheap.core.primary-key").</p>
     *
     * @param namespace the tag namespace
     * @param name the tag name
     * @return the tag definition, or null if not found
     * @throws NullPointerException if namespace or name is null
     */
    @Nullable
    TagDefinition getTagDefinitionByName(@NotNull String namespace, @NotNull String name);

    /**
     * Retrieves all tag definitions in the registry.
     *
     * @return collection of all tag definitions (never null, but may be empty)
     */
    @NotNull
    Collection<TagDefinition> getAllTagDefinitions();

    /**
     * Retrieves all tag definitions in the specified namespace.
     *
     * <p>This includes tags in the exact namespace and any sub-namespaces.
     * For example, namespace "cheap.core" will match "cheap.core.primary-key".</p>
     *
     * @param namespace the namespace to filter by
     * @return collection of matching tag definitions (never null, but may be empty)
     * @throws NullPointerException if namespace is null
     */
    @NotNull
    Collection<TagDefinition> getTagDefinitionsByNamespace(@NotNull String namespace);

    // ==================== Tag Application ====================

    /**
     * Applies a tag to a CHEAP element.
     *
     * <p>This creates an entity for the tag application, attaches a TagApplicationAspect,
     * and stores it in the appropriate tag_applications_* hierarchy. The application is
     * also indexed by element ID for fast lookup.</p>
     *
     * <p>Tag applications are validated before being stored. Validation includes:</p>
     * <ul>
     *   <li>Tag definition existence</li>
     *   <li>Tag applicability to target element type</li>
     *   <li>Conflict detection with existing tags</li>
     * </ul>
     *
     * <p>This operation is idempotent. If the same tag is already applied to the element
     * with the same metadata, the existing application entity is returned.</p>
     *
     * @param targetElementId UUID of the element to tag
     * @param targetType type of the element being tagged
     * @param tagDefinitionId UUID of the tag definition entity
     * @param metadata optional tag-specific metadata (may be null)
     * @param source how the tag is being applied
     * @return the entity containing the tag application
     * @throws NullPointerException if targetElementId, targetType, tagDefinitionId, or source is null
     * @throws IllegalArgumentException if validation fails
     */
    @NotNull
    Entity applyTag(
        @NotNull UUID targetElementId,
        @NotNull ElementType targetType,
        @NotNull UUID tagDefinitionId,
        @Nullable Map<String, Object> metadata,
        @NotNull TagSource source
    );

    /**
     * Removes a tag application from an element.
     *
     * <p>This removes the tag application entity from the appropriate hierarchy
     * and updates the element index.</p>
     *
     * @param tagApplicationId UUID of the tag application entity to remove
     * @throws NullPointerException if tagApplicationId is null
     */
    void removeTag(@NotNull UUID tagApplicationId);

    // ==================== Tag Queries ====================

    /**
     * Retrieves all tags applied to a specific element.
     *
     * @param elementId UUID of the element
     * @param type type of the element
     * @return collection of tag applications (never null, but may be empty)
     * @throws NullPointerException if elementId or type is null
     */
    @NotNull
    Collection<TagApplication> getTagsForElement(@NotNull UUID elementId, @NotNull ElementType type);

    /**
     * Retrieves all elements that have a specific tag applied.
     *
     * @param tagDefinitionId UUID of the tag definition
     * @param type type of elements to return (filters by target type)
     * @return collection of element UUIDs (never null, but may be empty)
     * @throws NullPointerException if tagDefinitionId or type is null
     */
    @NotNull
    Collection<UUID> getElementsByTag(@NotNull UUID tagDefinitionId, @NotNull ElementType type);

    /**
     * Retrieves all elements that have a specific tag applied (by tag name).
     *
     * @param namespace the tag namespace
     * @param name the tag name
     * @param type type of elements to return
     * @return collection of element UUIDs (never null, but may be empty)
     * @throws NullPointerException if namespace, name, or type is null
     */
    @NotNull
    Collection<UUID> getElementsByTagName(
        @NotNull String namespace,
        @NotNull String name,
        @NotNull ElementType type
    );

    /**
     * Checks if an element has a specific tag applied.
     *
     * @param elementId UUID of the element
     * @param type type of the element
     * @param tagDefinitionId UUID of the tag definition
     * @return true if the tag is applied to the element, false otherwise
     * @throws NullPointerException if any parameter is null
     */
    boolean hasTag(
        @NotNull UUID elementId,
        @NotNull ElementType type,
        @NotNull UUID tagDefinitionId
    );

    // ==================== Tag Validation ====================

    /**
     * Checks if a tag can be applied to a specific element type.
     *
     * @param tagDefinitionId UUID of the tag definition
     * @param targetType the element type to check
     * @return true if the tag can be applied, false otherwise
     * @throws NullPointerException if tagDefinitionId or targetType is null
     */
    boolean isTagApplicable(@NotNull UUID tagDefinitionId, @NotNull ElementType targetType);

    /**
     * Validates a tag application before it is applied.
     *
     * <p>This performs all validation checks including:</p>
     * <ul>
     *   <li>Tag definition existence</li>
     *   <li>Tag applicability to target type</li>
     *   <li>Conflict detection with existing tags</li>
     * </ul>
     *
     * @param tagDefinitionId UUID of the tag definition
     * @param targetElementId UUID of the target element
     * @param targetType type of the target element
     * @return collection of validation error messages (empty if valid)
     * @throws NullPointerException if any parameter is null
     */
    @NotNull
    Collection<String> validateTagApplication(
        @NotNull UUID tagDefinitionId,
        @NotNull UUID targetElementId,
        @NotNull ElementType targetType
    );

    // ==================== Tag Inheritance ====================

    /**
     * Retrieves the direct parent tags of a tag definition.
     *
     * @param tagDefinitionId UUID of the tag definition
     * @return collection of parent tag definition UUIDs (never null, but may be empty)
     * @throws NullPointerException if tagDefinitionId is null
     */
    @NotNull
    Collection<UUID> getParentTags(@NotNull UUID tagDefinitionId);

    /**
     * Retrieves all ancestor tags of a tag definition (transitive closure).
     *
     * <p>This includes direct parents, grandparents, and so on up the inheritance chain.</p>
     *
     * @param tagDefinitionId UUID of the tag definition
     * @return collection of ancestor tag definition UUIDs (never null, but may be empty)
     * @throws NullPointerException if tagDefinitionId is null
     */
    @NotNull
    Collection<UUID> getAllAncestorTags(@NotNull UUID tagDefinitionId);

    /**
     * Retrieves the direct child tags of a tag definition.
     *
     * @param tagDefinitionId UUID of the tag definition
     * @return collection of child tag definition UUIDs (never null, but may be empty)
     * @throws NullPointerException if tagDefinitionId is null
     */
    @NotNull
    Collection<UUID> getChildTags(@NotNull UUID tagDefinitionId);

    /**
     * Checks if a tag inherits from another tag.
     *
     * @param childTagId UUID of the potential child tag
     * @param parentTagId UUID of the potential parent tag
     * @return true if childTag inherits from parentTag (directly or transitively), false otherwise
     * @throws NullPointerException if any parameter is null
     */
    boolean inheritsFrom(@NotNull UUID childTagId, @NotNull UUID parentTagId);

    // ==================== Standard Tags ====================

    /**
     * Initializes all standard cheap.core tags in the registry.
     *
     * <p>This operation is idempotent. Tags that already exist will not be duplicated.</p>
     *
     * <p>Standard tags include:</p>
     * <ul>
     *   <li>Identity and Keys (6 tags)</li>
     *   <li>Temporal and Versioning (7 tags)</li>
     *   <li>Lifecycle and State (5 tags)</li>
     *   <li>Relationships (5 tags)</li>
     *   <li>Data Semantics (6 tags)</li>
     *   <li>Validation and Constraints (6 tags)</li>
     *   <li>Security and Privacy (6 tags)</li>
     *   <li>Business Domain (8 tags)</li>
     *   <li>Technical Behavior (5 tags)</li>
     * </ul>
     */
    void initializeStandardTags();

    /**
     * Retrieves all standard cheap.core tags.
     *
     * @return collection of standard tag definitions (never null, but may be empty)
     */
    @NotNull
    Collection<TagDefinition> getStandardTags();
}
