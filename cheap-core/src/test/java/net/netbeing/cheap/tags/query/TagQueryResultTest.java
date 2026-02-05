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
import net.netbeing.cheap.tags.model.TagSource;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TagQueryResultTest
{
    @Test
    void testConstructor()
    {
        UUID element1 = UUID.randomUUID();
        UUID element2 = UUID.randomUUID();

        Collection<UUID> elementIds = List.of(element1, element2);
        Map<UUID, Collection<TagApplication>> tagsByElement = new HashMap<>();

        TagQueryResult result = new TagQueryResult(elementIds, tagsByElement);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetElements()
    {
        UUID element1 = UUID.randomUUID();
        UUID element2 = UUID.randomUUID();

        Collection<UUID> elementIds = List.of(element1, element2);
        Map<UUID, Collection<TagApplication>> tagsByElement = new HashMap<>();

        TagQueryResult result = new TagQueryResult(elementIds, tagsByElement);

        Collection<UUID> elements = result.getElements();
        assertNotNull(elements);
        assertEquals(2, elements.size());
        assertTrue(elements.contains(element1));
        assertTrue(elements.contains(element2));
    }

    @Test
    void testGetElements_Immutable()
    {
        UUID element1 = UUID.randomUUID();
        Collection<UUID> elementIds = List.of(element1);
        Map<UUID, Collection<TagApplication>> tagsByElement = new HashMap<>();

        TagQueryResult result = new TagQueryResult(elementIds, tagsByElement);
        Collection<UUID> elements = result.getElements();

        assertThrows(UnsupportedOperationException.class, () -> elements.add(UUID.randomUUID()));
    }

    @Test
    void testGetTagsFor()
    {
        UUID element1 = UUID.randomUUID();
        UUID tagDefId = UUID.randomUUID();

        TagApplication tagApp = new TagApplication(
            tagDefId,
            element1,
            ElementType.PROPERTY,
            null,
            TagSource.EXPLICIT,
            ZonedDateTime.now(),
            null
        );

        Map<UUID, Collection<TagApplication>> tagsByElement = new HashMap<>();
        tagsByElement.put(element1, List.of(tagApp));

        TagQueryResult result = new TagQueryResult(List.of(element1), tagsByElement);

        Collection<TagApplication> tags = result.getTagsFor(element1);
        assertNotNull(tags);
        assertEquals(1, tags.size());
        assertTrue(tags.contains(tagApp));
    }

    @Test
    void testGetTagsFor_NotFound()
    {
        UUID element1 = UUID.randomUUID();
        UUID element2 = UUID.randomUUID();

        Map<UUID, Collection<TagApplication>> tagsByElement = new HashMap<>();

        TagQueryResult result = new TagQueryResult(List.of(element1), tagsByElement);

        Collection<TagApplication> tags = result.getTagsFor(element2);
        assertNotNull(tags);
        assertTrue(tags.isEmpty());
    }

    @Test
    void testGetTagsByElement()
    {
        UUID element1 = UUID.randomUUID();
        UUID element2 = UUID.randomUUID();
        UUID tagDefId = UUID.randomUUID();

        TagApplication tagApp1 = new TagApplication(
            tagDefId,
            element1,
            ElementType.PROPERTY,
            null,
            TagSource.EXPLICIT,
            ZonedDateTime.now(),
            null
        );
        TagApplication tagApp2 = new TagApplication(
            tagDefId,
            element2,
            ElementType.PROPERTY,
            null,
            TagSource.EXPLICIT,
            ZonedDateTime.now(),
            null
        );

        Map<UUID, Collection<TagApplication>> tagsByElement = new HashMap<>();
        tagsByElement.put(element1, List.of(tagApp1));
        tagsByElement.put(element2, List.of(tagApp2));

        TagQueryResult result = new TagQueryResult(List.of(element1, element2), tagsByElement);

        Map<UUID, Collection<TagApplication>> allTags = result.getTagsByElement();
        assertNotNull(allTags);
        assertEquals(2, allTags.size());
        assertTrue(allTags.containsKey(element1));
        assertTrue(allTags.containsKey(element2));
    }

    @Test
    void testGetTagsByElement_Immutable()
    {
        UUID element1 = UUID.randomUUID();
        Map<UUID, Collection<TagApplication>> tagsByElement = new HashMap<>();

        TagQueryResult result = new TagQueryResult(List.of(element1), tagsByElement);
        Map<UUID, Collection<TagApplication>> allTags = result.getTagsByElement();

        assertThrows(UnsupportedOperationException.class, () ->
            allTags.put(UUID.randomUUID(), Collections.emptyList()));
    }

    @Test
    void testSize()
    {
        UUID element1 = UUID.randomUUID();
        UUID element2 = UUID.randomUUID();
        UUID element3 = UUID.randomUUID();

        Collection<UUID> elementIds = List.of(element1, element2, element3);
        Map<UUID, Collection<TagApplication>> tagsByElement = new HashMap<>();

        TagQueryResult result = new TagQueryResult(elementIds, tagsByElement);

        assertEquals(3, result.size());
    }

    @Test
    void testGetTotalCount()
    {
        UUID element1 = UUID.randomUUID();
        UUID element2 = UUID.randomUUID();

        Collection<UUID> elementIds = List.of(element1, element2);
        Map<UUID, Collection<TagApplication>> tagsByElement = new HashMap<>();

        TagQueryResult result = new TagQueryResult(elementIds, tagsByElement);

        assertEquals(2, result.getTotalCount());
        assertEquals(result.size(), result.getTotalCount());
    }

    @Test
    void testIsEmpty_True()
    {
        TagQueryResult result = new TagQueryResult(Collections.emptyList(), Collections.emptyMap());

        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @Test
    void testIsEmpty_False()
    {
        UUID element1 = UUID.randomUUID();

        TagQueryResult result = new TagQueryResult(List.of(element1), Collections.emptyMap());

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testToString()
    {
        UUID element1 = UUID.randomUUID();
        UUID tagDefId = UUID.randomUUID();

        TagApplication tagApp = new TagApplication(
            tagDefId,
            element1,
            ElementType.PROPERTY,
            null,
            TagSource.EXPLICIT,
            ZonedDateTime.now(),
            null
        );

        Map<UUID, Collection<TagApplication>> tagsByElement = new HashMap<>();
        tagsByElement.put(element1, List.of(tagApp));

        TagQueryResult result = new TagQueryResult(List.of(element1), tagsByElement);

        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("elements=1"));
        assertTrue(str.contains("totalTags=1"));
    }

    @Test
    void testEquals_SameObject()
    {
        UUID element1 = UUID.randomUUID();
        TagQueryResult result = new TagQueryResult(List.of(element1), Collections.emptyMap());

        assertEquals(result, result);
    }

    @Test
    void testEquals_EqualObjects()
    {
        UUID element1 = UUID.randomUUID();
        UUID tagDefId = UUID.randomUUID();

        TagApplication tagApp = new TagApplication(
            tagDefId,
            element1,
            ElementType.PROPERTY,
            null,
            TagSource.EXPLICIT,
            ZonedDateTime.now(),
            null
        );

        Map<UUID, Collection<TagApplication>> tagsByElement1 = new HashMap<>();
        tagsByElement1.put(element1, List.of(tagApp));

        Map<UUID, Collection<TagApplication>> tagsByElement2 = new HashMap<>();
        tagsByElement2.put(element1, List.of(tagApp));

        TagQueryResult result1 = new TagQueryResult(List.of(element1), tagsByElement1);
        TagQueryResult result2 = new TagQueryResult(List.of(element1), tagsByElement2);

        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void testEquals_DifferentElements()
    {
        UUID element1 = UUID.randomUUID();
        UUID element2 = UUID.randomUUID();

        TagQueryResult result1 = new TagQueryResult(List.of(element1), Collections.emptyMap());
        TagQueryResult result2 = new TagQueryResult(List.of(element2), Collections.emptyMap());

        assertNotEquals(result1, result2);
    }

    @Test
    void testEquals_DifferentTags()
    {
        UUID element1 = UUID.randomUUID();
        UUID tagDefId1 = UUID.randomUUID();
        UUID tagDefId2 = UUID.randomUUID();

        TagApplication tagApp1 = new TagApplication(
            tagDefId1,
            element1,
            ElementType.PROPERTY,
            null,
            TagSource.EXPLICIT,
            ZonedDateTime.now(),
            null
        );
        TagApplication tagApp2 = new TagApplication(
            tagDefId2,
            element1,
            ElementType.PROPERTY,
            null,
            TagSource.EXPLICIT,
            ZonedDateTime.now(),
            null
        );

        Map<UUID, Collection<TagApplication>> tagsByElement1 = new HashMap<>();
        tagsByElement1.put(element1, List.of(tagApp1));

        Map<UUID, Collection<TagApplication>> tagsByElement2 = new HashMap<>();
        tagsByElement2.put(element1, List.of(tagApp2));

        TagQueryResult result1 = new TagQueryResult(List.of(element1), tagsByElement1);
        TagQueryResult result2 = new TagQueryResult(List.of(element1), tagsByElement2);

        assertNotEquals(result1, result2);
    }

    @Test
    void testEquals_Null()
    {
        UUID element1 = UUID.randomUUID();
        TagQueryResult result = new TagQueryResult(List.of(element1), Collections.emptyMap());

        assertNotEquals(null, result);
    }

    @Test
    void testEquals_DifferentClass()
    {
        UUID element1 = UUID.randomUUID();
        TagQueryResult result = new TagQueryResult(List.of(element1), Collections.emptyMap());

        assertNotEquals(result, "not a TagQueryResult");
    }
}
