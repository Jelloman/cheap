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

class TagQueryTest
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

    // ==================== Query Builder Tests ====================

    @Test
    void testQueryBuilder_ForType()
    {
        TagQuery query = new TagQuery(registry);
        query.forType(ElementType.PROPERTY);

        // Should not throw
        assertNotNull(query);
    }

    @Test
    void testQueryBuilder_WithTag()
    {
        UUID tagId = UUID.randomUUID();
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .withTag(tagId);

        assertNotNull(query);
    }

    @Test
    void testQueryBuilder_WithoutTag()
    {
        UUID tagId = UUID.randomUUID();
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .withoutTag(tagId);

        assertNotNull(query);
    }

    @Test
    void testQueryBuilder_InNamespace()
    {
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .inNamespace("cheap.core");

        assertNotNull(query);
    }

    @Test
    void testQueryBuilder_NotInNamespace()
    {
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .notInNamespace("cheap.core");

        assertNotNull(query);
    }

    @Test
    void testQueryBuilder_FromSource()
    {
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .fromSource(TagSource.EXPLICIT);

        assertNotNull(query);
    }

    @Test
    void testQueryBuilder_IncludeInheritedTags()
    {
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .includeInheritedTags(true);

        assertNotNull(query);
    }

    @Test
    void testQueryBuilder_Chaining()
    {
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .inNamespace("cheap.core")
            .fromSource(TagSource.EXPLICIT)
            .includeInheritedTags(true);

        assertNotNull(query);
    }

    // ==================== Execute Tests ====================

    @Test
    void testExecute_RequiresType()
    {
        TagQuery query = new TagQuery(registry);

        assertThrows(IllegalStateException.class, query::execute);
    }

    @Test
    void testExecute_EmptyResult()
    {
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY);

        TagQueryResult result = query.execute();
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @Test
    void testExecute_SingleTagFilter()
    {
        // Create tag definition
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "Test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        Entity tagEntity = registry.defineTag(tagDef);

        // Apply tag to elements
        UUID element1 = UUID.randomUUID();
        UUID element2 = UUID.randomUUID();
        UUID element3 = UUID.randomUUID();

        registry.applyTag(element1, ElementType.PROPERTY, tagEntity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(element2, ElementType.PROPERTY, tagEntity.globalId(), null, TagSource.EXPLICIT);

        // Query for elements with this tag
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .withTag(tagEntity.globalId());

        TagQueryResult result = query.execute();
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        assertTrue(result.getElements().contains(element1));
        assertTrue(result.getElements().contains(element2));
        assertFalse(result.getElements().contains(element3));
    }

    @Test
    void testExecute_MultipleTagFilters_AND()
    {
        // Create two tag definitions
        TagDefinition tag1Def = new TagDefinition(
            "test.namespace",
            "tag1",
            "Tag 1",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        TagDefinition tag2Def = new TagDefinition(
            "test.namespace",
            "tag2",
            "Tag 2",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        Entity tag1Entity = registry.defineTag(tag1Def);
        Entity tag2Entity = registry.defineTag(tag2Def);

        // Apply tags to elements
        UUID element1 = UUID.randomUUID();  // Has both tags
        UUID element2 = UUID.randomUUID();  // Has only tag1
        UUID element3 = UUID.randomUUID();  // Has only tag2

        registry.applyTag(element1, ElementType.PROPERTY, tag1Entity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(element1, ElementType.PROPERTY, tag2Entity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(element2, ElementType.PROPERTY, tag1Entity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(element3, ElementType.PROPERTY, tag2Entity.globalId(), null, TagSource.EXPLICIT);

        // Query for elements with BOTH tags (AND logic)
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .withTag(tag1Entity.globalId())
            .withTag(tag2Entity.globalId());

        TagQueryResult result = query.execute();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertTrue(result.getElements().contains(element1));
        assertFalse(result.getElements().contains(element2));
        assertFalse(result.getElements().contains(element3));
    }

    @Test
    void testExecute_ExcludeTag()
    {
        // Create tag definitions
        TagDefinition includeTagDef = new TagDefinition(
            "test.namespace",
            "include-tag",
            "Include tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        TagDefinition excludeTagDef = new TagDefinition(
            "test.namespace",
            "exclude-tag",
            "Exclude tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        Entity includeTagEntity = registry.defineTag(includeTagDef);
        Entity excludeTagEntity = registry.defineTag(excludeTagDef);

        // Apply tags
        UUID element1 = UUID.randomUUID();  // Has include tag only
        UUID element2 = UUID.randomUUID();  // Has both tags
        UUID element3 = UUID.randomUUID();  // Has exclude tag only

        registry.applyTag(element1, ElementType.PROPERTY, includeTagEntity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(element2, ElementType.PROPERTY, includeTagEntity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(element2, ElementType.PROPERTY, excludeTagEntity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(element3, ElementType.PROPERTY, excludeTagEntity.globalId(), null, TagSource.EXPLICIT);

        // Query: include-tag but NOT exclude-tag
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .withTag(includeTagEntity.globalId())
            .withoutTag(excludeTagEntity.globalId());

        TagQueryResult result = query.execute();
        assertEquals(1, result.size());
        assertTrue(result.getElements().contains(element1));
        assertFalse(result.getElements().contains(element2));
        assertFalse(result.getElements().contains(element3));
    }

    @Test
    void testExecute_NamespaceFilter_Include()
    {
        // Create tags in different namespaces
        TagDefinition tag1 = new TagDefinition(
            "cheap.core",
            "tag1",
            "Tag 1",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            null,
            null
        );
        TagDefinition tag2 = new TagDefinition(
            "myapp.domain",
            "tag2",
            "Tag 2",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        Entity tag1Entity = registry.defineTag(tag1);
        Entity tag2Entity = registry.defineTag(tag2);

        // Apply tags
        UUID element1 = UUID.randomUUID();
        UUID element2 = UUID.randomUUID();

        registry.applyTag(element1, ElementType.PROPERTY, tag1Entity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(element2, ElementType.PROPERTY, tag2Entity.globalId(), null, TagSource.EXPLICIT);

        // Query for cheap.core namespace only
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .inNamespace("cheap.core");

        TagQueryResult result = query.execute();
        assertTrue(result.getElements().contains(element1));
        assertFalse(result.getElements().contains(element2));
    }

    @Test
    void testExecute_NamespaceFilter_Exclude()
    {
        // Create tags in different namespaces
        TagDefinition tag1 = new TagDefinition(
            "cheap.core",
            "tag1",
            "Tag 1",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            null,
            null
        );
        TagDefinition tag2 = new TagDefinition(
            "myapp.domain",
            "tag2",
            "Tag 2",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        Entity tag1Entity = registry.defineTag(tag1);
        Entity tag2Entity = registry.defineTag(tag2);

        // Apply tags
        UUID element1 = UUID.randomUUID();
        UUID element2 = UUID.randomUUID();

        registry.applyTag(element1, ElementType.PROPERTY, tag1Entity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(element2, ElementType.PROPERTY, tag2Entity.globalId(), null, TagSource.EXPLICIT);

        // Query excluding cheap.core namespace
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .notInNamespace("cheap.core");

        TagQueryResult result = query.execute();
        assertFalse(result.getElements().contains(element1));
        assertTrue(result.getElements().contains(element2));
    }

    @Test
    void testExecute_SourceFilter()
    {
        // Create tag
        TagDefinition tagDef = new TagDefinition(
            "test.namespace",
            "test-tag",
            "Test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        Entity tagEntity = registry.defineTag(tagDef);

        // Apply with different sources
        UUID element1 = UUID.randomUUID();
        UUID element2 = UUID.randomUUID();
        UUID element3 = UUID.randomUUID();

        registry.applyTag(element1, ElementType.PROPERTY, tagEntity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(element2, ElementType.PROPERTY, tagEntity.globalId(), null, TagSource.INFERRED);
        registry.applyTag(element3, ElementType.PROPERTY, tagEntity.globalId(), null, TagSource.GENERATED);

        // Query for EXPLICIT source only
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .withTag(tagEntity.globalId())
            .fromSource(TagSource.EXPLICIT);

        TagQueryResult result = query.execute();
        assertEquals(1, result.size());
        assertTrue(result.getElements().contains(element1));
    }

    @Test
    void testExecute_ComplexQuery()
    {
        // Create multiple tags in different namespaces
        TagDefinition piiTag = new TagDefinition(
            "cheap.core",
            "pii",
            "PII data",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            null,
            null
        );
        TagDefinition encryptedTag = new TagDefinition(
            "cheap.core",
            "encrypted",
            "Encrypted",
            List.of(ElementType.PROPERTY),
            TagScope.STANDARD,
            null,
            null
        );
        TagDefinition customTag = new TagDefinition(
            "myapp.domain",
            "custom",
            "Custom",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity piiEntity = registry.defineTag(piiTag);
        Entity encryptedEntity = registry.defineTag(encryptedTag);
        Entity customEntity = registry.defineTag(customTag);

        // Create elements with various tag combinations
        UUID element1 = UUID.randomUUID();  // PII + encrypted (EXPLICIT)
        UUID element2 = UUID.randomUUID();  // PII only (EXPLICIT)
        UUID element3 = UUID.randomUUID();  // PII + encrypted + custom (EXPLICIT)
        UUID element4 = UUID.randomUUID();  // PII + encrypted (INFERRED)

        registry.applyTag(element1, ElementType.PROPERTY, piiEntity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(element1, ElementType.PROPERTY, encryptedEntity.globalId(), null, TagSource.EXPLICIT);

        registry.applyTag(element2, ElementType.PROPERTY, piiEntity.globalId(), null, TagSource.EXPLICIT);

        registry.applyTag(element3, ElementType.PROPERTY, piiEntity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(element3, ElementType.PROPERTY, encryptedEntity.globalId(), null, TagSource.EXPLICIT);
        registry.applyTag(element3, ElementType.PROPERTY, customEntity.globalId(), null, TagSource.EXPLICIT);

        registry.applyTag(element4, ElementType.PROPERTY, piiEntity.globalId(), null, TagSource.INFERRED);
        registry.applyTag(element4, ElementType.PROPERTY, encryptedEntity.globalId(), null, TagSource.INFERRED);

        // Complex query: PII AND encrypted, in cheap.core namespace only, EXPLICIT source
        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .withTag(piiEntity.globalId())
            .withTag(encryptedEntity.globalId())
            .inNamespace("cheap.core")
            .fromSource(TagSource.EXPLICIT);

        TagQueryResult result = query.execute();
        assertEquals(2, result.size());
        assertTrue(result.getElements().contains(element1));
        assertFalse(result.getElements().contains(element2));  // Missing encrypted tag
        assertTrue(result.getElements().contains(element3));   // Has all required tags
        assertFalse(result.getElements().contains(element4));  // Wrong source
    }
}
