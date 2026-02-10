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

import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.tags.model.*;
import net.netbeing.cheap.tags.registry.TagRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TagConflictDetectorTest
{
    private CheapFactory factory;
    private Catalog catalog;
    private TagRegistry registry;
    private TagConflictDetector conflictDetector;

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
        conflictDetector = new TagConflictDetector(registry);
    }

    @AfterEach
    void tearDown()
    {
        conflictDetector = null;
        registry = null;
        catalog = null;
        factory = null;
    }

    // ==================== Conflict Rules Tests ====================

    @Test
    void testGetConflictingTags()
    {
        Set<String> immutableConflicts = conflictDetector.getConflictingTags("cheap.core.immutable");
        assertFalse(immutableConflicts.isEmpty());
        assertTrue(immutableConflicts.contains("cheap.core.modified-timestamp"));

        Set<String> requiredConflicts = conflictDetector.getConflictingTags("cheap.core.required");
        assertTrue(requiredConflicts.contains("cheap.core.nullable"));
    }

    @Test
    void testGetConflictingTags_NoConflicts()
    {
        Set<String> noConflicts = conflictDetector.getConflictingTags("nonexistent.tag");
        assertTrue(noConflicts.isEmpty());
    }

    @Test
    void testTagsConflict()
    {
        assertTrue(conflictDetector.tagsConflict("cheap.core.immutable", "cheap.core.modified-timestamp"));
        assertTrue(conflictDetector.tagsConflict("cheap.core.required", "cheap.core.nullable"));
        assertTrue(conflictDetector.tagsConflict("cheap.core.encrypted", "cheap.core.masked"));

        assertFalse(conflictDetector.tagsConflict("cheap.core.immutable", "cheap.core.required"));
        assertFalse(conflictDetector.tagsConflict("cheap.core.primary-key", "cheap.core.immutable"));
    }

    @Test
    void testGetAllConflictRules()
    {
        var rules = conflictDetector.getAllConflictRules();
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        assertTrue(rules.containsKey("cheap.core.immutable"));
        assertTrue(rules.containsKey("cheap.core.required"));
    }

    // ==================== Detect Conflicts Tests ====================

    @Test
    void testDetectConflicts_NoConflicts()
    {
        // Create compatible tags
        TagDefinition tag1 = new TagDefinition(
            "test.namespace",
            "tag1",
            "Tag 1",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        TagDefinition tag2 = new TagDefinition(
            "test.namespace",
            "tag2",
            "Tag 2",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity tag1Entity = registry.defineTag(tag1);
        Entity tag2Entity = registry.defineTag(tag2);

        UUID elementId = UUID.randomUUID();
        registry.applyTag(elementId, ElementType.PROPERTY, tag1Entity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(elementId, ElementType.PROPERTY, tag2Entity.globalId(), null, TagSource.EXPLICIT);

        List<String> conflicts = conflictDetector.detectConflicts(elementId, ElementType.PROPERTY);
        assertTrue(conflicts.isEmpty());
    }

    @Test
    void testDetectConflicts_WithConflict()
    {
        // Create conflicting standard tags
        TagDefinition immutableTag = new TagDefinition(
            "cheap.core",
            "immutable",
            "Immutable data",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            null,
            null
        );
        TagDefinition modifiedTag = new TagDefinition(
            "cheap.core",
            "modified-timestamp",
            "Modified timestamp",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            null,
            null
        );

        Entity immutableEntity = registry.defineTag(immutableTag);
        Entity modifiedEntity = registry.defineTag(modifiedTag);

        UUID elementId = UUID.randomUUID();
        registry.applyTag(elementId, ElementType.PROPERTY, immutableEntity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(elementId, ElementType.PROPERTY, modifiedEntity.globalId(), null, TagSource.EXPLICIT);

        List<String> conflicts = conflictDetector.detectConflicts(elementId, ElementType.PROPERTY);
        assertFalse(conflicts.isEmpty());
        assertTrue(conflicts.stream().anyMatch(c -> c.contains("immutable") && c.contains("modified-timestamp")));
    }

    @Test
    void testDetectConflicts_MultipleConflicts()
    {
        // Create tags with multiple conflicts
        TagDefinition requiredTag = new TagDefinition(
            "cheap.core",
            "required",
            "Required field",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            null,
            null
        );
        TagDefinition nullableTag = new TagDefinition(
            "cheap.core",
            "nullable",
            "Nullable field",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            null,
            null
        );

        Entity requiredEntity = registry.defineTag(requiredTag);
        Entity nullableEntity = registry.defineTag(nullableTag);

        UUID elementId = UUID.randomUUID();
        registry.applyTag(elementId, ElementType.PROPERTY, requiredEntity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(elementId, ElementType.PROPERTY, nullableEntity.globalId(), null, TagSource.EXPLICIT);

        List<String> conflicts = conflictDetector.detectConflicts(elementId, ElementType.PROPERTY);
        assertFalse(conflicts.isEmpty());
    }

    // ==================== Detect Conflicts For New Tag Tests ====================

    @Test
    void testDetectConflictsForNewTag_NoConflict()
    {
        TagDefinition tag1 = new TagDefinition(
            "test.namespace",
            "tag1",
            "Tag 1",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        TagDefinition tag2 = new TagDefinition(
            "test.namespace",
            "tag2",
            "Tag 2",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity tag1Entity = registry.defineTag(tag1);
        Entity tag2Entity = registry.defineTag(tag2);

        UUID elementId = UUID.randomUUID();
        registry.applyTag(elementId, ElementType.PROPERTY, tag1Entity.globalId(), null, TagSource.EXPLICIT);

        List<String> conflicts = conflictDetector.detectConflictsForNewTag(
            elementId,
            ElementType.PROPERTY,
            tag2Entity.globalId()
        );

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void testDetectConflictsForNewTag_WithConflict()
    {
        TagDefinition immutableTag = new TagDefinition(
            "cheap.core",
            "immutable",
            "Immutable data",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            null,
            null
        );
        TagDefinition modifiedTag = new TagDefinition(
            "cheap.core",
            "modified-timestamp",
            "Modified timestamp",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            null,
            null
        );

        Entity immutableEntity = registry.defineTag(immutableTag);
        Entity modifiedEntity = registry.defineTag(modifiedTag);

        UUID elementId = UUID.randomUUID();
        registry.applyTag(elementId, ElementType.PROPERTY, immutableEntity.globalId(), null, TagSource.EXPLICIT);

        // Try to apply conflicting tag
        List<String> conflicts = conflictDetector.detectConflictsForNewTag(
            elementId,
            ElementType.PROPERTY,
            modifiedEntity.globalId()
        );

        assertFalse(conflicts.isEmpty());
        assertTrue(conflicts.get(0).contains("Cannot apply tag"));
        assertTrue(conflicts.get(0).contains("conflicts with"));
    }

    @Test
    void testDetectConflictsForNewTag_InheritedConflict()
    {
        // Create parent tag that conflicts with something
        TagDefinition immutableTag = new TagDefinition(
            "cheap.core",
            "immutable",
            "Immutable data",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            null,
            null
        );
        Entity immutableEntity = registry.defineTag(immutableTag);

        // Create child tag that inherits from immutable
        TagDefinition childTag = new TagDefinition(
            "test.namespace",
            "child-immutable",
            "Child of immutable",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            List.of(immutableEntity.globalId())
        );
        Entity childEntity = registry.defineTag(childTag);

        // Create modified-timestamp tag
        TagDefinition modifiedTag = new TagDefinition(
            "cheap.core",
            "modified-timestamp",
            "Modified timestamp",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            null,
            null
        );
        Entity modifiedEntity = registry.defineTag(modifiedTag);

        // Apply modified-timestamp to element
        UUID elementId = UUID.randomUUID();
        registry.applyTag(elementId, ElementType.PROPERTY, modifiedEntity.globalId(), null, TagSource.EXPLICIT);

        // Try to apply child tag (which inherits from immutable, which conflicts with modified-timestamp)
        List<String> conflicts = conflictDetector.detectConflictsForNewTag(
            elementId,
            ElementType.PROPERTY,
            childEntity.globalId()
        );

        assertFalse(conflicts.isEmpty());
        assertTrue(conflicts.stream().anyMatch(c -> c.contains("inherited tag")));
    }

    @Test
    void testDetectConflictsForNewTag_EmptyElement()
    {
        TagDefinition tag = new TagDefinition(
            "test.namespace",
            "tag",
            "Tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        Entity tagEntity = registry.defineTag(tag);

        UUID elementId = UUID.randomUUID();

        // No tags on element yet, so no conflicts
        List<String> conflicts = conflictDetector.detectConflictsForNewTag(
            elementId,
            ElementType.PROPERTY,
            tagEntity.globalId()
        );

        assertTrue(conflicts.isEmpty());
    }
}
