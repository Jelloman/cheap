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

import net.netbeing.cheap.tags.model.TagApplication;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Result of a tag query containing matching elements and their associated tags.
 *
 * <p>This class provides convenient access to query results with methods for
 * retrieving element IDs, tags per element, and result metadata.</p>
 *
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * TagQueryResult result = query.execute();
 *
 * System.out.println("Found " + result.size() + " elements");
 *
 * for (UUID elementId : result.getElements()) {
 *     Collection<TagApplication> tags = result.getTagsFor(elementId);
 *     System.out.println("Element " + elementId + " has " + tags.size() + " tags");
 * }
 * }</pre>
 */
public class TagQueryResult
{
    private final Collection<UUID> elementIds;
    private final Map<UUID, Collection<TagApplication>> tagsByElement;

    /**
     * Creates a new query result.
     *
     * @param elementIds the IDs of matching elements
     * @param tagsByElement map of element IDs to their tag applications
     */
    public TagQueryResult(
        @NotNull Collection<UUID> elementIds,
        @NotNull Map<UUID, Collection<TagApplication>> tagsByElement)
    {
        this.elementIds = Collections.unmodifiableCollection(
            new ArrayList<>(Objects.requireNonNull(elementIds, "elementIds cannot be null"))
        );
        this.tagsByElement = Collections.unmodifiableMap(
            new HashMap<>(Objects.requireNonNull(tagsByElement, "tagsByElement cannot be null"))
        );
    }

    /**
     * Returns the collection of matching element IDs.
     *
     * @return unmodifiable collection of element UUIDs
     */
    @NotNull
    public Collection<UUID> getElements()
    {
        return elementIds;
    }

    /**
     * Returns all tags for a specific element.
     *
     * @param elementId the element ID
     * @return collection of tag applications for the element, or empty collection if not found
     */
    @NotNull
    public Collection<TagApplication> getTagsFor(@NotNull UUID elementId)
    {
        Objects.requireNonNull(elementId, "elementId cannot be null");
        Collection<TagApplication> tags = tagsByElement.get(elementId);
        return tags != null ? tags : Collections.emptyList();
    }

    /**
     * Returns the map of all elements to their tags.
     *
     * @return unmodifiable map of element IDs to tag application collections
     */
    @NotNull
    public Map<UUID, Collection<TagApplication>> getTagsByElement()
    {
        return tagsByElement;
    }

    /**
     * Returns the total number of matching elements.
     *
     * @return count of elements in the result
     */
    public int size()
    {
        return elementIds.size();
    }

    /**
     * Returns the total count (alias for size()).
     *
     * @return count of elements in the result
     */
    public int getTotalCount()
    {
        return size();
    }

    /**
     * Checks if the result is empty.
     *
     * @return true if no elements matched the query, false otherwise
     */
    public boolean isEmpty()
    {
        return elementIds.isEmpty();
    }

    @Override
    public String toString()
    {
        return "TagQueryResult{" +
            "elements=" + elementIds.size() +
            ", totalTags=" + tagsByElement.values().stream().mapToInt(Collection::size).sum() +
            '}';
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagQueryResult that = (TagQueryResult) o;
        // Compare element sets (order-independent)
        if (!new HashSet<>(elementIds).equals(new HashSet<>(that.elementIds))) {
            return false;
        }
        // Compare tag maps
        return tagsByElement.equals(that.tagsByElement);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(new HashSet<>(elementIds), tagsByElement);
    }
}
