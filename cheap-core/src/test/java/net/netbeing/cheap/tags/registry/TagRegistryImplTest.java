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
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.tags.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TagRegistryImplTest
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

    // ==================== Construction Tests ====================

    @Test
    void testCreate()
    {
        assertNotNull(registry);
        assertEquals(catalog, registry.catalog());
    }

    @Test
    void testCreate_NullCatalog()
    {
        assertThrows(NullPointerException.class, () ->
            TagRegistry.create(null, factory)
        );
    }

    @Test
    void testCreate_NullFactory()
    {
        assertThrows(NullPointerException.class, () ->
            TagRegistry.create(catalog, null)
        );
    }

    // ==================== Tag Definition Tests ====================

    @Test
    void testDefineTag()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity entity = registry.defineTag(tagDef);
        assertNotNull(entity);
        assertNotNull(entity.globalId());
    }

    @Test
    void testDefineTag_DuplicateName()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        registry.defineTag(tagDef);

        assertThrows(IllegalArgumentException.class, () ->
            registry.defineTag(tagDef)
        );
    }

    @Test
    void testDefineTag_InvalidNamespace()
    {
        TagDefinition tagDef = new TagDefinition(
            "InvalidNamespace",  // uppercase not allowed
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        assertThrows(IllegalArgumentException.class, () ->
            registry.defineTag(tagDef)
        );
    }

    @Test
    void testDefineTag_InvalidName()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "Invalid_Name",  // underscore not allowed
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        assertThrows(IllegalArgumentException.class, () ->
            registry.defineTag(tagDef)
        );
    }

    @Test
    void testGetTagDefinition()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity entity = registry.defineTag(tagDef);
        TagDefinition retrieved = registry.getTagDefinition(entity.globalId());

        assertNotNull(retrieved);
        assertEquals(tagDef.getNamespace(), retrieved.getNamespace());
        assertEquals(tagDef.getName(), retrieved.getName());
        assertEquals(tagDef.getDescription(), retrieved.getDescription());
    }

    @Test
    void testGetTagDefinition_NotFound()
    {
        TagDefinition retrieved = registry.getTagDefinition(UUID.randomUUID());
        assertNull(retrieved);
    }

    @Test
    void testGetTagDefinitionByName()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        registry.defineTag(tagDef);
        TagDefinition retrieved = registry.getTagDefinitionByName("test.namespace", "test-tag");

        assertNotNull(retrieved);
        assertEquals(tagDef.getNamespace(), retrieved.getNamespace());
        assertEquals(tagDef.getName(), retrieved.getName());
    }

    @Test
    void testGetTagDefinitionByName_NotFound()
    {
        TagDefinition retrieved = registry.getTagDefinitionByName("nonexistent.namespace", "nonexistent-tag");
        assertNull(retrieved);
    }

    @Test
    void testGetAllTagDefinitions()
    {
        TagDefinition tag1 = new TagDefinition(
            "test.namespace",
            "tag1",
            "First tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        TagDefinition tag2 = new TagDefinition(
            "test.namespace",
            "tag2",
            "Second tag",
            List.of(ElementType.ASPECT),
            TagScope.CUSTOM,
            null,
            null
        );

        registry.defineTag(tag1);
        registry.defineTag(tag2);

        Collection<TagDefinition> all = registry.getAllTagDefinitions();
        assertEquals(2, all.size());
    }

    @Test
    void testGetTagDefinitionsByNamespace()
    {
        TagDefinition tag1 = new TagDefinition(
            "test.namespace",
            "tag1",
            "First tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        TagDefinition tag2 = new TagDefinition(
            "other.namespace",
            "tag2",
            "Second tag",
            List.of(ElementType.ASPECT),
            TagScope.CUSTOM,
            null,
            null
        );

        registry.defineTag(tag1);
        registry.defineTag(tag2);

        Collection<TagDefinition> testTags = registry.getTagDefinitionsByNamespace("test.namespace");
        assertEquals(1, testTags.size());
        assertEquals("tag1", testTags.iterator().next().getName());
    }

    // ==================== Tag Application Tests ====================

    @Test
    void testApplyTag()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity tagEntity = registry.defineTag(tagDef);
        UUID targetElementId = UUID.randomUUID();

        Entity appEntity = registry.applyTag(
            targetElementId,
            ElementType.PROPERTY,
            tagEntity.globalId(),
            Map.of("key", "value"),
            TagSource.EXPLICIT
        );

        assertNotNull(appEntity);
        assertNotNull(appEntity.globalId());
    }

    @Test
    void testApplyTag_Idempotent()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity tagEntity = registry.defineTag(tagDef);
        UUID targetElementId = UUID.randomUUID();

        Entity app1 = registry.applyTag(
            targetElementId,
            ElementType.PROPERTY,
            tagEntity.globalId(),
            null,
            TagSource.EXPLICIT
        );

        Entity app2 = registry.applyTag(
            targetElementId,
            ElementType.PROPERTY,
            tagEntity.globalId(),
            null,
            TagSource.EXPLICIT
        );

        assertEquals(app1.globalId(), app2.globalId());
    }

    @Test
    void testApplyTag_NotApplicable()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),  // Only applicable to PROPERTY
            TagScope.CUSTOM,
            null,
            null
        );

        Entity tagEntity = registry.defineTag(tagDef);
        UUID targetElementId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
            registry.applyTag(
                targetElementId,
                ElementType.ASPECT,  // Trying to apply to ASPECT
                tagEntity.globalId(),
                null,
                TagSource.EXPLICIT
            )
        );
    }

    @Test
    void testApplyTag_TagNotFound()
    {
        UUID targetElementId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
            registry.applyTag(
                targetElementId,
                ElementType.PROPERTY,
                UUID.randomUUID(),  // Non-existent tag
                null,
                TagSource.EXPLICIT
            )
        );
    }

    @Test
    void testRemoveTag()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity tagEntity = registry.defineTag(tagDef);
        UUID targetElementId = UUID.randomUUID();

        Entity appEntity = registry.applyTag(
            targetElementId,
            ElementType.PROPERTY,
            tagEntity.globalId(),
            null,
            TagSource.EXPLICIT
        );

        assertTrue(registry.hasTag(targetElementId, ElementType.PROPERTY, tagEntity.globalId()));

        registry.removeTag(appEntity.globalId());

        assertFalse(registry.hasTag(targetElementId, ElementType.PROPERTY, tagEntity.globalId()));
    }

    // ==================== Tag Query Tests ====================

    @Test
    void testGetTagsForElement()
    {
        TagDefinition tag1 = new TagDefinition(
            "test.namespace",
            "tag1",
            "First tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        TagDefinition tag2 = new TagDefinition(
            "test.namespace",
            "tag2",
            "Second tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity tagEntity1 = registry.defineTag(tag1);
        Entity tagEntity2 = registry.defineTag(tag2);
        UUID targetElementId = UUID.randomUUID();

        registry.applyTag(targetElementId, ElementType.PROPERTY, tagEntity1.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(targetElementId, ElementType.PROPERTY, tagEntity2.globalId(), null, TagSource.EXPLICIT);

        Collection<TagApplication> tags = registry.getTagsForElement(targetElementId, ElementType.PROPERTY);
        assertEquals(2, tags.size());
    }

    @Test
    void testGetElementsByTag()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity tagEntity = registry.defineTag(tagDef);
        UUID elem1 = UUID.randomUUID();
        UUID elem2 = UUID.randomUUID();

        registry.applyTag(elem1, ElementType.PROPERTY, tagEntity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(elem2, ElementType.PROPERTY, tagEntity.globalId(), null, TagSource.EXPLICIT);

        Collection<UUID> elements = registry.getElementsByTag(tagEntity.globalId(), ElementType.PROPERTY);
        assertEquals(2, elements.size());
        assertTrue(elements.contains(elem1));
        assertTrue(elements.contains(elem2));
    }

    @Test
    void testGetElementsByTagName()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity tagEntity = registry.defineTag(tagDef);
        UUID elem1 = UUID.randomUUID();

        registry.applyTag(elem1, ElementType.PROPERTY, tagEntity.globalId(), null, TagSource.EXPLICIT);

        Collection<UUID> elements = registry.getElementsByTagName("test.namespace", "test-tag", ElementType.PROPERTY);
        assertEquals(1, elements.size());
        assertTrue(elements.contains(elem1));
    }

    @Test
    void testHasTag()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity tagEntity = registry.defineTag(tagDef);
        UUID targetElementId = UUID.randomUUID();

        assertFalse(registry.hasTag(targetElementId, ElementType.PROPERTY, tagEntity.globalId()));

        registry.applyTag(targetElementId, ElementType.PROPERTY, tagEntity.globalId(), null, TagSource.EXPLICIT);

        assertTrue(registry.hasTag(targetElementId, ElementType.PROPERTY, tagEntity.globalId()));
    }

    // ==================== Tag Validation Tests ====================

    @Test
    void testIsTagApplicable()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY, ElementType.ASPECT),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity tagEntity = registry.defineTag(tagDef);

        assertTrue(registry.isTagApplicable(tagEntity.globalId(), ElementType.PROPERTY));
        assertTrue(registry.isTagApplicable(tagEntity.globalId(), ElementType.ASPECT));
        assertFalse(registry.isTagApplicable(tagEntity.globalId(), ElementType.ENTITY));
    }

    @Test
    void testValidateTagApplication()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity tagEntity = registry.defineTag(tagDef);
        UUID targetElementId = UUID.randomUUID();

        Collection<String> errors = registry.validateTagApplication(
            tagEntity.globalId(),
            targetElementId,
            ElementType.PROPERTY
        );
        assertTrue(errors.isEmpty());

        errors = registry.validateTagApplication(
            tagEntity.globalId(),
            targetElementId,
            ElementType.ASPECT
        );
        assertFalse(errors.isEmpty());
    }

    // ==================== Tag Inheritance Tests ====================

    @Test
    void testGetParentTags()
    {
        TagDefinition parent = new TagDefinition(
            "test.namespace",
            "parent-tag",
            "Parent tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity parentEntity = registry.defineTag(parent);

        TagDefinition child = new TagDefinition(
            "test.namespace",
            "child-tag",
            "Child tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            List.of(parentEntity.globalId())
        );

        Entity childEntity = registry.defineTag(child);

        Collection<UUID> parents = registry.getParentTags(childEntity.globalId());
        assertEquals(1, parents.size());
        assertTrue(parents.contains(parentEntity.globalId()));
    }

    @Test
    void testGetAllAncestorTags()
    {
        TagDefinition grandparent = new TagDefinition(
            "test.namespace",
            "grandparent-tag",
            "Grandparent tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity grandparentEntity = registry.defineTag(grandparent);

        TagDefinition parent = new TagDefinition(
            "test.namespace",
            "parent-tag",
            "Parent tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            List.of(grandparentEntity.globalId())
        );

        Entity parentEntity = registry.defineTag(parent);

        TagDefinition child = new TagDefinition(
            "test.namespace",
            "child-tag",
            "Child tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            List.of(parentEntity.globalId())
        );

        Entity childEntity = registry.defineTag(child);

        Collection<UUID> ancestors = registry.getAllAncestorTags(childEntity.globalId());
        assertEquals(2, ancestors.size());
        assertTrue(ancestors.contains(parentEntity.globalId()));
        assertTrue(ancestors.contains(grandparentEntity.globalId()));
    }

    @Test
    void testGetChildTags()
    {
        TagDefinition parent = new TagDefinition(
            "test.namespace",
            "parent-tag",
            "Parent tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity parentEntity = registry.defineTag(parent);

        TagDefinition child1 = new TagDefinition(
            "test.namespace",
            "child1-tag",
            "First child tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            List.of(parentEntity.globalId())
        );

        TagDefinition child2 = new TagDefinition(
            "test.namespace",
            "child2-tag",
            "Second child tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            List.of(parentEntity.globalId())
        );

        Entity child1Entity = registry.defineTag(child1);
        Entity child2Entity = registry.defineTag(child2);

        Collection<UUID> children = registry.getChildTags(parentEntity.globalId());
        assertEquals(2, children.size());
        assertTrue(children.contains(child1Entity.globalId()));
        assertTrue(children.contains(child2Entity.globalId()));
    }

    @Test
    void testInheritsFrom()
    {
        TagDefinition parent = new TagDefinition(
            "test.namespace",
            "parent-tag",
            "Parent tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity parentEntity = registry.defineTag(parent);

        TagDefinition child = new TagDefinition(
            "test.namespace",
            "child-tag",
            "Child tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            List.of(parentEntity.globalId())
        );

        Entity childEntity = registry.defineTag(child);

        assertTrue(registry.inheritsFrom(childEntity.globalId(), parentEntity.globalId()));
        assertFalse(registry.inheritsFrom(parentEntity.globalId(), childEntity.globalId()));
    }

    // ==================== Standard Tags Tests ====================

    @Test
    void testGetStandardTags()
    {
        Collection<TagDefinition> standardTags = registry.getStandardTags();
        assertNotNull(standardTags);
        // Should be empty until we implement Phase 5
        assertTrue(standardTags.isEmpty());
    }
}
