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

package net.netbeing.cheap.tags.model;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Defines a tag type that can be applied to CHEAP elements.
 *
 * <p>A tag definition specifies:</p>
 * <ul>
 *   <li>A qualified name ({@code namespace.name}) that uniquely identifies the tag</li>
 *   <li>Which element types the tag can be applied to</li>
 *   <li>Whether it's a standard or custom tag</li>
 *   <li>Optional parent tags for inheritance hierarchies</li>
 *   <li>Optional aliases for alternative names</li>
 * </ul>
 *
 * <p>Tag definitions are immutable value objects. Once created, their properties
 * cannot be changed. To modify a tag definition, create a new one with the
 * desired properties.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * TagDefinition invoiceTag = new TagDefinition(
 *     "myapp.domain",                          // namespace
 *     "invoice-number",                        // name
 *     "Identifies invoice documents uniquely", // description
 *     List.of(ElementType.PROPERTY),          // appliesTo
 *     TagScope.CUSTOM,                        // scope
 *     List.of("invoice-id"),                  // aliases
 *     List.of(primaryKeyTagId, immutableTagId) // parentTagIds
 * );
 * }</pre>
 *
 * @see TagApplication
 * @see ElementType
 * @see TagScope
 */
public class TagDefinition
{
    private final String namespace;
    private final String name;
    private final String description;
    private final List<ElementType> appliesTo;
    private final TagScope scope;
    private final List<String> aliases;
    private final List<UUID> parentTagIds;

    /**
     * Constructs a new immutable TagDefinition.
     *
     * @param namespace the namespace for this tag (e.g., "cheap.core", "myapp.domain")
     * @param name the tag name within the namespace (e.g., "primary-key")
     * @param description human and LLM-readable explanation of the tag's purpose
     * @param appliesTo list of element types this tag can be applied to (must not be empty)
     * @param scope whether this is a STANDARD or CUSTOM tag
     * @param aliases alternative names for this tag (may be null or empty)
     * @param parentTagIds UUIDs of parent tag entities for inheritance (may be null or empty)
     * @throws NullPointerException if namespace, name, description, appliesTo, or scope is null
     * @throws IllegalArgumentException if appliesTo is empty
     */
    public TagDefinition(
        @NotNull String namespace,
        @NotNull String name,
        @NotNull String description,
        @NotNull List<ElementType> appliesTo,
        @NotNull TagScope scope,
        @Nullable List<String> aliases,
        @Nullable List<UUID> parentTagIds)
    {
        this.namespace = Objects.requireNonNull(namespace, "namespace cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.appliesTo = ImmutableList.copyOf(Objects.requireNonNull(appliesTo, "appliesTo cannot be null"));
        this.scope = Objects.requireNonNull(scope, "scope cannot be null");
        this.aliases = aliases != null ? ImmutableList.copyOf(aliases) : ImmutableList.of();
        this.parentTagIds = parentTagIds != null ? ImmutableList.copyOf(parentTagIds) : ImmutableList.of();

        if (this.appliesTo.isEmpty()) {
            throw new IllegalArgumentException("appliesTo must contain at least one ElementType");
        }
    }

    /**
     * Returns the namespace of this tag.
     *
     * <p>Namespaces provide hierarchical organization and prevent naming conflicts.
     * Standard tags use the {@code cheap.core} namespace, while custom tags should
     * use application-specific namespaces like {@code myapp.domain}.</p>
     *
     * @return the tag namespace (never null)
     */
    @NotNull
    public String getNamespace()
    {
        return namespace;
    }

    /**
     * Returns the name of this tag within its namespace.
     *
     * <p>Names should be lowercase with hyphens for word separation
     * (e.g., "primary-key", "created-timestamp").</p>
     *
     * @return the tag name (never null)
     */
    @NotNull
    public String getName()
    {
        return name;
    }

    /**
     * Returns the full qualified name of this tag.
     *
     * <p>The full name is constructed as {@code namespace.name} and uniquely
     * identifies the tag across all namespaces.</p>
     *
     * @return the full qualified tag name (e.g., "cheap.core.primary-key")
     */
    @NotNull
    public String getFullName()
    {
        return namespace + "." + name;
    }

    /**
     * Returns the human-readable description of this tag's purpose and semantics.
     *
     * <p>Descriptions should clearly explain what the tag means and how it should
     * be used. They are intended for both human developers and LLMs.</p>
     *
     * @return the tag description (never null)
     */
    @NotNull
    public String getDescription()
    {
        return description;
    }

    /**
     * Returns the list of element types this tag can be applied to.
     *
     * <p>A tag may be applicable to one or more element types. For example,
     * a "required" tag might apply to both PROPERTY and ASPECT types.</p>
     *
     * @return immutable list of applicable element types (never null or empty)
     */
    @NotNull
    public List<ElementType> getAppliesTo()
    {
        return appliesTo;
    }

    /**
     * Returns the scope of this tag (STANDARD or CUSTOM).
     *
     * @return the tag scope (never null)
     */
    @NotNull
    public TagScope getScope()
    {
        return scope;
    }

    /**
     * Returns the list of alternative names for this tag.
     *
     * <p>Aliases provide alternative ways to reference the tag, useful for
     * backward compatibility or alternative naming conventions.</p>
     *
     * @return immutable list of aliases (never null, but may be empty)
     */
    @NotNull
    public List<String> getAliases()
    {
        return aliases;
    }

    /**
     * Returns the list of parent tag entity UUIDs for tag inheritance.
     *
     * <p>Parent tags allow tags to inherit properties and semantics. For example,
     * an "invoice-number" tag might inherit from both "primary-key" and "immutable"
     * tags.</p>
     *
     * @return immutable list of parent tag entity UUIDs (never null, but may be empty)
     */
    @NotNull
    public List<UUID> getParentTagIds()
    {
        return parentTagIds;
    }

    /**
     * Checks if this tag can be applied to the specified element type.
     *
     * @param elementType the element type to check
     * @return true if this tag can be applied to the element type, false otherwise
     */
    public boolean isApplicableTo(@Nullable ElementType elementType)
    {
        return elementType != null && appliesTo.contains(elementType);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagDefinition that = (TagDefinition) o;
        return Objects.equals(namespace, that.namespace) &&
               Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(appliesTo, that.appliesTo) &&
               scope == that.scope &&
               Objects.equals(aliases, that.aliases) &&
               Objects.equals(parentTagIds, that.parentTagIds);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(namespace, name, description, appliesTo, scope, aliases, parentTagIds);
    }

    @Override
    public String toString()
    {
        return "TagDefinition{" +
               "namespace='" + namespace + '\'' +
               ", name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", appliesTo=" + appliesTo +
               ", scope=" + scope +
               ", aliases=" + aliases +
               ", parentTagIds=" + parentTagIds +
               '}';
    }
}
