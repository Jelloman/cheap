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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TagValidatorTest
{
    private CheapFactory factory;
    private Catalog catalog;
    private TagRegistry registry;
    private TagValidator validator;

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
        validator = new TagValidator(registry);
    }

    @AfterEach
    void tearDown()
    {
        validator = null;
        registry = null;
        catalog = null;
        factory = null;
    }

    // ==================== Namespace Validation Tests ====================

    @Test
    void testIsNamespaceValid_Valid()
    {
        assertTrue(validator.isNamespaceValid("cheap.core"));
        assertTrue(validator.isNamespaceValid("myapp.domain"));
        assertTrue(validator.isNamespaceValid("org.example.tags"));
        assertTrue(validator.isNamespaceValid("a.b"));
        assertTrue(validator.isNamespaceValid("test.namespace.deep"));
        assertTrue(validator.isNamespaceValid("app-name.module"));
        assertTrue(validator.isNamespaceValid("test123.namespace456"));
    }

    @Test
    void testIsNamespaceValid_Invalid()
    {
        assertFalse(validator.isNamespaceValid(null));
        assertFalse(validator.isNamespaceValid(""));
        assertFalse(validator.isNamespaceValid("nodot"));
        assertFalse(validator.isNamespaceValid("Uppercase.Namespace"));
        assertFalse(validator.isNamespaceValid(".leading.dot"));
        assertFalse(validator.isNamespaceValid("trailing.dot."));
        assertFalse(validator.isNamespaceValid("double..dot"));
        assertFalse(validator.isNamespaceValid("has space.namespace"));
        assertFalse(validator.isNamespaceValid("has_underscore.namespace"));
    }

    // ==================== Name Validation Tests ====================

    @Test
    void testIsNameValid_Valid()
    {
        assertTrue(validator.isNameValid("primary-key"));
        assertTrue(validator.isNameValid("created-timestamp"));
        assertTrue(validator.isNameValid("pii"));
        assertTrue(validator.isNameValid("a"));
        assertTrue(validator.isNameValid("test123"));
        assertTrue(validator.isNameValid("multi-word-name"));
    }

    @Test
    void testIsNameValid_Invalid()
    {
        assertFalse(validator.isNameValid(null));
        assertFalse(validator.isNameValid(""));
        assertFalse(validator.isNameValid("UpperCase"));
        assertFalse(validator.isNameValid("has space"));
        assertFalse(validator.isNameValid("has_underscore"));
        assertFalse(validator.isNameValid("has.dot"));
        assertFalse(validator.isNameValid("-leading-hyphen"));
        assertFalse(validator.isNameValid("trailing-hyphen-"));
        assertFalse(validator.isNameValid("double--hyphen"));
    }

    // ==================== Tag Definition Validation Tests ====================

    @Test
    void testValidateTagDefinition_Valid()
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

        List<String> errors = validator.validateTagDefinition(tagDef);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateTagDefinition_InvalidNamespace()
    {
        TagDefinition tagDef = new TagDefinition(
            "InvalidNamespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        List<String> errors = validator.validateTagDefinition(tagDef);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("Invalid namespace format"));
    }

    @Test
    void testValidateTagDefinition_InvalidName()
    {
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "Invalid_Name",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        List<String> errors = validator.validateTagDefinition(tagDef);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("Invalid name format"));
    }

    @Test
    void testValidateTagDefinition_MissingParentTag()
    {
        UUID nonExistentParentId = UUID.randomUUID();

        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "A test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            List.of(nonExistentParentId)
        );

        List<String> errors = validator.validateTagDefinition(tagDef);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Parent tag not found")));
    }

    @Test
    void testValidateTagDefinition_WithValidParent()
    {
        // Create parent tag
        TagDefinition parentDef = new TagDefinition(
            "test.namespace",
            "parent-tag",
            "Parent tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        Entity parentEntity = registry.defineTag(parentDef);

        // Create child tag
        TagDefinition childDef = new TagDefinition(
            "test.namespace",
            "child-tag",
            "Child tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            List.of(parentEntity.globalId())
        );

        List<String> errors = validator.validateTagDefinition(childDef);
        assertTrue(errors.isEmpty());
    }

    // ==================== Tag Application Validation Tests ====================

    @Test
    void testValidateTagApplication_Valid()
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

        List<String> errors = validator.validateTagApplication(
            tagEntity.globalId(),
            targetElementId,
            ElementType.PROPERTY
        );

        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateTagApplication_TagNotFound()
    {
        UUID nonExistentTagId = UUID.randomUUID();
        UUID targetElementId = UUID.randomUUID();

        List<String> errors = validator.validateTagApplication(
            nonExistentTagId,
            targetElementId,
            ElementType.PROPERTY
        );

        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("Tag definition not found"));
    }

    @Test
    void testValidateTagApplication_NotApplicable()
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

        List<String> errors = validator.validateTagApplication(
            tagEntity.globalId(),
            targetElementId,
            ElementType.ASPECT  // Trying to apply to ASPECT
        );

        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("cannot be applied to"));
    }

    // ==================== Circular Inheritance Detection Tests ====================

    @Test
    void testDetectCircularInheritance_NoCircle()
    {
        // Create simple parent-child chain
        TagDefinition parent = new TagDefinition(
            "test.namespace",
            "parent",
            "Parent",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        Entity parentEntity = registry.defineTag(parent);

        TagDefinition child = new TagDefinition(
            "test.namespace",
            "child",
            "Child",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            List.of(parentEntity.globalId())
        );
        Entity childEntity = registry.defineTag(child);

        assertFalse(validator.detectCircularInheritance(childEntity.globalId()));
        assertFalse(validator.detectCircularInheritance(parentEntity.globalId()));
    }

    @Test
    void testDetectCircularInheritance_DeepHierarchy()
    {
        // Create deep hierarchy: grandparent -> parent -> child
        TagDefinition grandparent = new TagDefinition(
            "test.namespace",
            "grandparent",
            "Grandparent",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        Entity grandparentEntity = registry.defineTag(grandparent);

        TagDefinition parent = new TagDefinition(
            "test.namespace",
            "parent",
            "Parent",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            List.of(grandparentEntity.globalId())
        );
        Entity parentEntity = registry.defineTag(parent);

        TagDefinition child = new TagDefinition(
            "test.namespace",
            "child",
            "Child",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            List.of(parentEntity.globalId())
        );
        Entity childEntity = registry.defineTag(child);

        assertFalse(validator.detectCircularInheritance(childEntity.globalId()));
        assertFalse(validator.detectCircularInheritance(parentEntity.globalId()));
        assertFalse(validator.detectCircularInheritance(grandparentEntity.globalId()));
    }
}
