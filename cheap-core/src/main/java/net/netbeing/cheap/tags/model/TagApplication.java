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

package net.netbeing.cheap.tags.model;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Records the application of a tag to a specific CHEAP element.
 *
 * <p>A tag application captures:</p>
 * <ul>
 *   <li>Which tag definition is being applied</li>
 *   <li>Which element is being tagged</li>
 *   <li>When and how the tag was applied</li>
 *   <li>Optional tag-specific metadata</li>
 * </ul>
 *
 * <p>Tag applications are immutable value objects. Once created, their properties
 * cannot be changed. To modify a tag application, remove it and create a new one
 * with the desired properties.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * TagApplication application = new TagApplication(
 *     invoiceTagId,                           // tagDefinitionId
 *     propertyId,                             // targetElementId
 *     ElementType.PROPERTY,                   // targetType
 *     Map.of("format", "INV-{year}-{seq}"),  // metadata
 *     TagSource.EXPLICIT,                     // source
 *     ZonedDateTime.now(),                    // appliedAt
 *     "john.doe@example.com"                  // appliedBy
 * );
 * }</pre>
 *
 * @see TagDefinition
 * @see ElementType
 * @see TagSource
 */
public class TagApplication
{
    private final UUID tagDefinitionId;
    private final UUID targetElementId;
    private final ElementType targetType;
    private final Map<String, Object> metadata;
    private final TagSource source;
    private final ZonedDateTime appliedAt;
    private final String appliedBy;

    /**
     * Constructs a new immutable TagApplication.
     *
     * @param tagDefinitionId UUID of the tag definition entity being applied
     * @param targetElementId UUID of the element being tagged
     * @param targetType type of the element being tagged
     * @param metadata optional tag-specific attributes (may be null)
     * @param source how the tag was applied (EXPLICIT, INFERRED, or GENERATED)
     * @param appliedAt timestamp when the tag was applied
     * @param appliedBy identifier of the user or system that applied the tag (may be null)
     * @throws NullPointerException if tagDefinitionId, targetElementId, targetType, source, or appliedAt is null
     */
    public TagApplication(
        @NotNull UUID tagDefinitionId,
        @NotNull UUID targetElementId,
        @NotNull ElementType targetType,
        @Nullable Map<String, Object> metadata,
        @NotNull TagSource source,
        @NotNull ZonedDateTime appliedAt,
        @Nullable String appliedBy)
    {
        this.tagDefinitionId = Objects.requireNonNull(tagDefinitionId, "tagDefinitionId cannot be null");
        this.targetElementId = Objects.requireNonNull(targetElementId, "targetElementId cannot be null");
        this.targetType = Objects.requireNonNull(targetType, "targetType cannot be null");
        this.metadata = metadata != null ? ImmutableMap.copyOf(metadata) : ImmutableMap.of();
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.appliedAt = Objects.requireNonNull(appliedAt, "appliedAt cannot be null");
        this.appliedBy = appliedBy;
    }

    /**
     * Returns the UUID of the tag definition entity being applied.
     *
     * <p>This references the entity in the tag_definitions hierarchy that
     * contains the TagDefinition aspect.</p>
     *
     * @return the tag definition entity UUID (never null)
     */
    @NotNull
    public UUID getTagDefinitionId()
    {
        return tagDefinitionId;
    }

    /**
     * Returns the UUID of the element being tagged.
     *
     * <p>This is the global ID of the CHEAP element (Property, Aspect, Entity,
     * Hierarchy, or Catalog) that this tag is attached to.</p>
     *
     * @return the target element UUID (never null)
     */
    @NotNull
    public UUID getTargetElementId()
    {
        return targetElementId;
    }

    /**
     * Returns the type of the element being tagged.
     *
     * @return the target element type (never null)
     */
    @NotNull
    public ElementType getTargetType()
    {
        return targetType;
    }

    /**
     * Returns the optional tag-specific metadata.
     *
     * <p>Metadata provides additional context or configuration for the tag
     * application. For example, a "format" tag might include a format pattern,
     * or a "range-bounded" tag might include min/max values.</p>
     *
     * @return immutable map of metadata (never null, but may be empty)
     */
    @NotNull
    public Map<String, Object> getMetadata()
    {
        return metadata;
    }

    /**
     * Returns how the tag was applied.
     *
     * @return the tag source (never null)
     * @see TagSource
     */
    @NotNull
    public TagSource getSource()
    {
        return source;
    }

    /**
     * Returns the timestamp when the tag was applied.
     *
     * @return the application timestamp (never null)
     */
    @NotNull
    public ZonedDateTime getAppliedAt()
    {
        return appliedAt;
    }

    /**
     * Returns the identifier of the user or system that applied the tag.
     *
     * <p>This might be a username, email address, or system identifier.
     * It may be null if the source is unknown or not tracked.</p>
     *
     * @return the applier identifier (may be null)
     */
    @Nullable
    public String getAppliedBy()
    {
        return appliedBy;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagApplication that = (TagApplication) o;
        return Objects.equals(tagDefinitionId, that.tagDefinitionId) &&
               Objects.equals(targetElementId, that.targetElementId) &&
               targetType == that.targetType &&
               Objects.equals(metadata, that.metadata) &&
               source == that.source &&
               Objects.equals(appliedAt, that.appliedAt) &&
               Objects.equals(appliedBy, that.appliedBy);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(tagDefinitionId, targetElementId, targetType, metadata, source, appliedAt, appliedBy);
    }

    @Override
    public String toString()
    {
        return "TagApplication{" +
               "tagDefinitionId=" + tagDefinitionId +
               ", targetElementId=" + targetElementId +
               ", targetType=" + targetType +
               ", metadata=" + metadata +
               ", source=" + source +
               ", appliedAt=" + appliedAt +
               ", appliedBy='" + appliedBy + '\'' +
               '}';
    }
}
