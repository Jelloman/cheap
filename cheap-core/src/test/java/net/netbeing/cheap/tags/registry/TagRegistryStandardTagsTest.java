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
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.tags.model.TagDefinition;
import net.netbeing.cheap.tags.model.TagScope;
import net.netbeing.cheap.tags.standard.StandardTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for standard tags integration with TagRegistry.
 */
class TagRegistryStandardTagsTest
{
    private CheapFactory factory;
    private Catalog catalog;
    private TagRegistry registry;

    @BeforeEach
    void setUp()
    {
        factory = new CheapFactory();
        catalog = factory.createCatalog(
            UUID.randomUUID(),
            CatalogSpecies.SINK,
            URI.create("mem://test-catalog"),
            null,
            0L
        );
        registry = TagRegistry.create(catalog, factory);
    }

    @AfterEach
    void tearDown()
    {
        registry = null;
        catalog = null;
        factory = null;
    }

    // ==================== Initialize Standard Tags Tests ====================

    @Test
    void testInitializeStandardTags_CreatesAllTags()
    {
        registry.initializeStandardTags();

        Collection<TagDefinition> standardTags = registry.getStandardTags();
        assertNotNull(standardTags);
        assertEquals(StandardTags.getStandardTagCount(), standardTags.size());
    }

    @Test
    void testInitializeStandardTags_AllTagsRetrievable()
    {
        registry.initializeStandardTags();

        // Try to retrieve each standard tag by name
        for (TagDefinition expectedTag : StandardTags.allStandardTags()) {
            TagDefinition retrievedTag = registry.getTagDefinitionByName(
                expectedTag.getNamespace(),
                expectedTag.getName()
            );

            assertNotNull(retrievedTag,
                "Tag not found: " + expectedTag.getFullName());
            assertEquals(expectedTag.getNamespace(), retrievedTag.getNamespace());
            assertEquals(expectedTag.getName(), retrievedTag.getName());
            assertEquals(expectedTag.getDescription(), retrievedTag.getDescription());
        }
    }

    @Test
    void testInitializeStandardTags_Idempotent()
    {
        // Initialize once
        registry.initializeStandardTags();
        Collection<TagDefinition> tags1 = registry.getStandardTags();

        // Initialize again
        registry.initializeStandardTags();
        Collection<TagDefinition> tags2 = registry.getStandardTags();

        // Should have same count (no duplicates)
        assertEquals(tags1.size(), tags2.size());
        assertEquals(StandardTags.getStandardTagCount(), tags2.size());
    }

    @Test
    void testInitializeStandardTags_BeforeAndAfter()
    {
        // Before initialization
        Collection<TagDefinition> tagsBefore = registry.getStandardTags();
        assertTrue(tagsBefore.isEmpty());

        // After initialization
        registry.initializeStandardTags();
        Collection<TagDefinition> tagsAfter = registry.getStandardTags();
        assertFalse(tagsAfter.isEmpty());
        assertEquals(StandardTags.getStandardTagCount(), tagsAfter.size());
    }

    // ==================== Get Standard Tags Tests ====================

    @Test
    void testGetStandardTags_BeforeInitialization()
    {
        Collection<TagDefinition> standardTags = registry.getStandardTags();
        assertNotNull(standardTags);
        assertTrue(standardTags.isEmpty());
    }

    @Test
    void testGetStandardTags_AfterInitialization()
    {
        registry.initializeStandardTags();

        Collection<TagDefinition> standardTags = registry.getStandardTags();
        assertNotNull(standardTags);
        assertFalse(standardTags.isEmpty());
        assertEquals(StandardTags.getStandardTagCount(), standardTags.size());
    }

    @Test
    void testGetStandardTags_AllHaveCheapCoreNamespace()
    {
        registry.initializeStandardTags();

        Collection<TagDefinition> standardTags = registry.getStandardTags();
        for (TagDefinition tag : standardTags) {
            assertTrue(tag.getNamespace().equals("cheap.core") ||
                      tag.getNamespace().startsWith("cheap.core."),
                "Tag " + tag.getFullName() + " not in cheap.core namespace");
        }
    }

    @Test
    void testGetStandardTags_AllStandardScope()
    {
        registry.initializeStandardTags();

        Collection<TagDefinition> standardTags = registry.getStandardTags();
        for (TagDefinition tag : standardTags) {
            assertEquals(TagScope.STANDARD, tag.getScope(),
                "Tag " + tag.getFullName() + " is not STANDARD scope");
        }
    }

    // ==================== Specific Tag Retrieval Tests ====================

    @Test
    void testRetrieveSpecificTag_PrimaryKey()
    {
        registry.initializeStandardTags();

        TagDefinition tag = registry.getTagDefinitionByName("cheap.core", "primary-key");
        assertNotNull(tag);
        assertEquals("cheap.core", tag.getNamespace());
        assertEquals("primary-key", tag.getName());
        assertEquals(TagScope.STANDARD, tag.getScope());
    }

    @Test
    void testRetrieveSpecificTag_Pii()
    {
        registry.initializeStandardTags();

        TagDefinition tag = registry.getTagDefinitionByName("cheap.core", "pii");
        assertNotNull(tag);
        assertEquals("cheap.core", tag.getNamespace());
        assertEquals("pii", tag.getName());
        assertTrue(tag.getDescription().contains("Personally Identifiable Information"));
    }

    @Test
    void testRetrieveSpecificTag_CreatedTimestamp()
    {
        registry.initializeStandardTags();

        TagDefinition tag = registry.getTagDefinitionByName("cheap.core", "created-timestamp");
        assertNotNull(tag);
        assertEquals("cheap.core.created-timestamp", tag.getFullName());
    }

    @Test
    void testRetrieveSpecificTag_Immutable()
    {
        registry.initializeStandardTags();

        TagDefinition tag = registry.getTagDefinitionByName("cheap.core", "immutable");
        assertNotNull(tag);
        assertEquals("cheap.core.immutable", tag.getFullName());
    }

    // ==================== Mix Standard and Custom Tags Tests ====================

    @Test
    void testMixStandardAndCustomTags()
    {
        // Initialize standard tags
        registry.initializeStandardTags();

        // Define a custom tag
        TagDefinition customTag = new TagDefinition(
            "myapp.domain",
            "custom-tag",
            "Custom tag",
            java.util.List.of(net.netbeing.cheap.tags.model.ElementType.PROPERTY),
            net.netbeing.cheap.tags.model.TagScope.CUSTOM,
            null,
            null
        );
        registry.defineTag(customTag);

        // Get all tags
        Collection<TagDefinition> allTags = registry.getAllTagDefinitions();
        assertEquals(StandardTags.getStandardTagCount() + 1, allTags.size());

        // Get only standard tags
        Collection<TagDefinition> standardTags = registry.getStandardTags();
        assertEquals(StandardTags.getStandardTagCount(), standardTags.size());

        // Verify custom tag not in standard tags
        boolean customInStandard = standardTags.stream()
            .anyMatch(t -> t.getFullName().equals(customTag.getFullName()));
        assertFalse(customInStandard);
    }

    @Test
    void testGetStandardTags_DoesNotIncludeCustomTags()
    {
        // Define custom tags in different namespaces
        TagDefinition customTag1 = new TagDefinition(
            "myapp.domain",
            "tag1",
            "Custom tag 1",
            java.util.List.of(net.netbeing.cheap.tags.model.ElementType.PROPERTY),
            net.netbeing.cheap.tags.model.TagScope.CUSTOM,
            null,
            null
        );
        TagDefinition customTag2 = new TagDefinition(
            "other.namespace",
            "tag2",
            "Custom tag 2",
            java.util.List.of(net.netbeing.cheap.tags.model.ElementType.PROPERTY),
            net.netbeing.cheap.tags.model.TagScope.CUSTOM,
            null,
            null
        );

        registry.defineTag(customTag1);
        registry.defineTag(customTag2);
        registry.initializeStandardTags();

        // Get standard tags
        Collection<TagDefinition> standardTags = registry.getStandardTags();

        // Verify only cheap.core namespace
        for (TagDefinition tag : standardTags) {
            assertTrue(tag.getNamespace().startsWith("cheap.core"));
        }

        // Verify standard tags count unchanged
        assertEquals(StandardTags.getStandardTagCount(), standardTags.size());
    }

    // ==================== Integration Tests ====================

    @Test
    void testApplyStandardTag_PrimaryKey()
    {
        registry.initializeStandardTags();

        TagDefinition pkTag = registry.getTagDefinitionByName("cheap.core", "primary-key");
        assertNotNull(pkTag);

        UUID elementId = UUID.randomUUID();

        // This should work if we can get the tag entity ID
        // For now, just verify we can retrieve the tag
        assertNotNull(pkTag);
    }

    @Test
    void testAllStandardTagsHaveValidDefinitions()
    {
        registry.initializeStandardTags();

        Collection<TagDefinition> standardTags = registry.getStandardTags();

        for (TagDefinition tag : standardTags) {
            // Validate each tag
            assertNotNull(tag.getNamespace());
            assertNotNull(tag.getName());
            assertNotNull(tag.getDescription());
            assertFalse(tag.getDescription().trim().isEmpty());
            assertFalse(tag.getAppliesTo().isEmpty());
            assertNotNull(tag.getScope());
            assertEquals(TagScope.STANDARD, tag.getScope());
        }
    }
}
