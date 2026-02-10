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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TagDefinitionTest
{
    @Test
    void testConstruct_Valid()
    {
        TagDefinition tag = new TagDefinition(
            "cheap.core",
            "primary-key",
            "Primary identifier for entity",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            List.of("pk", "primary_key"),
            List.of(UUID.randomUUID())
        );

        assertNotNull(tag);
        assertEquals("cheap.core", tag.getNamespace());
        assertEquals("primary-key", tag.getName());
        assertEquals("Primary identifier for entity", tag.getDescription());
        assertEquals(1, tag.getAppliesTo().size());
        assertTrue(tag.getAppliesTo().contains(ElementType.PROPERTY));
        assertEquals(TagScope.STANDARD, tag.getScope());
        assertEquals(2, tag.getAliases().size());
        assertEquals(1, tag.getParentTagIds().size());
    }

    @Test
    void testConstruct_MinimalValid()
    {
        TagDefinition tag = new TagDefinition(
            "myapp.domain",
            "custom-tag",
            "A custom tag",
            List.of(ElementType.ENTITY),
            TagScope.CUSTOM,
            null,  // no aliases
            null   // no parent tags
        );

        assertNotNull(tag);
        assertEquals("myapp.domain", tag.getNamespace());
        assertEquals("custom-tag", tag.getName());
        assertEquals(1, tag.getAppliesTo().size());
        assertTrue(tag.getAliases().isEmpty());
        assertTrue(tag.getParentTagIds().isEmpty());
    }

    @Test
    void testConstruct_MultipleElementTypes()
    {
        TagDefinition tag = new TagDefinition(
            "cheap.core",
            "required",
            "Marks element as required",
            List.of(ElementType.PROPERTY, ElementType.ASPECT),
            TagScope.STANDARD,
            null,
            null
        );

        assertEquals(2, tag.getAppliesTo().size());
        assertTrue(tag.getAppliesTo().contains(ElementType.PROPERTY));
        assertTrue(tag.getAppliesTo().contains(ElementType.ASPECT));
    }

    @Test
    void testConstruct_NullNamespace()
    {
        assertThrows(NullPointerException.class, () ->
            new TagDefinition(
                null,
                "tag",
                "Description",
                List.of(ElementType.PROPERTY),
                TagScope.CUSTOM,
                null,
                null
            )
        );
    }

    @Test
    void testConstruct_NullName()
    {
        assertThrows(NullPointerException.class, () ->
            new TagDefinition(
                "namespace",
                null,
                "Description",
                List.of(ElementType.PROPERTY),
                TagScope.CUSTOM,
                null,
                null
            )
        );
    }

    @Test
    void testConstruct_NullDescription()
    {
        assertThrows(NullPointerException.class, () ->
            new TagDefinition(
                "namespace",
                "tag",
                null,
                List.of(ElementType.PROPERTY),
                TagScope.CUSTOM,
                null,
                null
            )
        );
    }

    @Test
    void testConstruct_NullAppliesTo()
    {
        assertThrows(NullPointerException.class, () ->
            new TagDefinition(
                "namespace",
                "tag",
                "Description",
                null,
                TagScope.CUSTOM,
                null,
                null
            )
        );
    }

    @Test
    void testConstruct_EmptyAppliesTo()
    {
        assertThrows(IllegalArgumentException.class, () ->
            new TagDefinition(
                "namespace",
                "tag",
                "Description",
                List.of(),
                TagScope.CUSTOM,
                null,
                null
            )
        );
    }

    @Test
    void testConstruct_NullScope()
    {
        assertThrows(NullPointerException.class, () ->
            new TagDefinition(
                "namespace",
                "tag",
                "Description",
                List.of(ElementType.PROPERTY),
                null,
                null,
                null
            )
        );
    }

    @Test
    void testGetFullName()
    {
        TagDefinition tag = new TagDefinition(
            "cheap.core",
            "primary-key",
            "Primary identifier",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            null,
            null
        );

        assertEquals("cheap.core.primary-key", tag.getFullName());
    }

    @Test
    void testIsApplicableTo()
    {
        TagDefinition tag = new TagDefinition(
            "cheap.core",
            "required",
            "Required field",
            List.of(ElementType.PROPERTY, ElementType.ASPECT),
            TagScope.STANDARD,
            null,
            null
        );

        assertTrue(tag.isApplicableTo(ElementType.PROPERTY));
        assertTrue(tag.isApplicableTo(ElementType.ASPECT));
        assertFalse(tag.isApplicableTo(ElementType.ENTITY));
        assertFalse(tag.isApplicableTo(ElementType.HIERARCHY));
        assertFalse(tag.isApplicableTo(ElementType.CATALOG));
        assertFalse(tag.isApplicableTo(null));
    }

    @Test
    void testEquals_SameInstance()
    {
        TagDefinition tag = new TagDefinition(
            "namespace",
            "tag",
            "Description",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        assertEquals(tag, tag);
    }

    @Test
    void testEquals_SameValues()
    {
        TagDefinition tag1 = new TagDefinition(
            "namespace",
            "tag",
            "Description",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            List.of("alias1"),
            List.of(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        );

        TagDefinition tag2 = new TagDefinition(
            "namespace",
            "tag",
            "Description",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            List.of("alias1"),
            List.of(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        );

        assertEquals(tag1, tag2);
        assertEquals(tag1.hashCode(), tag2.hashCode());
    }

    @Test
    void testEquals_DifferentValues()
    {
        TagDefinition tag1 = new TagDefinition(
            "namespace",
            "tag1",
            "Description",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        TagDefinition tag2 = new TagDefinition(
            "namespace",
            "tag2",
            "Description",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        assertNotEquals(tag1, tag2);
    }

    @Test
    void testEquals_Null()
    {
        TagDefinition tag = new TagDefinition(
            "namespace",
            "tag",
            "Description",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        assertNotEquals(null, tag);
    }

    @Test
    void testEquals_DifferentClass()
    {
        TagDefinition tag = new TagDefinition(
            "namespace",
            "tag",
            "Description",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        assertNotEquals(tag, "not a tag definition");
    }

    @Test
    void testToString()
    {
        TagDefinition tag = new TagDefinition(
            "cheap.core",
            "primary-key",
            "Primary identifier",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            null,
            null
        );

        String str = tag.toString();
        assertNotNull(str);
        assertTrue(str.contains("cheap.core"));
        assertTrue(str.contains("primary-key"));
        assertTrue(str.contains("Primary identifier"));
    }

    @Test
    void testImmutability_AppliesTo()
    {
        TagDefinition tag = new TagDefinition(
            "namespace",
            "tag",
            "Description",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        List<ElementType> appliesTo = tag.getAppliesTo();
        assertThrows(UnsupportedOperationException.class, () -> appliesTo.add(ElementType.ASPECT));
    }

    @Test
    void testImmutability_Aliases()
    {
        TagDefinition tag = new TagDefinition(
            "namespace",
            "tag",
            "Description",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            List.of("alias1"),
            null
        );

        List<String> aliases = tag.getAliases();
        assertThrows(UnsupportedOperationException.class, () -> aliases.add("alias2"));
    }

    @Test
    void testImmutability_ParentTagIds()
    {
        UUID parentId = UUID.randomUUID();
        TagDefinition tag = new TagDefinition(
            "namespace",
            "tag",
            "Description",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            List.of(parentId)
        );

        List<UUID> parentTagIds = tag.getParentTagIds();
        assertThrows(UnsupportedOperationException.class, () -> parentTagIds.add(UUID.randomUUID()));
    }
}
