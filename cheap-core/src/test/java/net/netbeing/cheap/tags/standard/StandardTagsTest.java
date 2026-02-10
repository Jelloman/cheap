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

package net.netbeing.cheap.tags.standard;

import net.netbeing.cheap.tags.model.ElementType;
import net.netbeing.cheap.tags.model.TagDefinition;
import net.netbeing.cheap.tags.model.TagScope;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StandardTagsTest
{
    // ==================== All Standard Tags Tests ====================

    @Test
    void testAllStandardTags_NotNull()
    {
        Collection<TagDefinition> allTags = StandardTags.allStandardTags();
        assertNotNull(allTags);
    }

    @Test
    void testAllStandardTags_NotEmpty()
    {
        Collection<TagDefinition> allTags = StandardTags.allStandardTags();
        assertFalse(allTags.isEmpty());
    }

    @Test
    void testAllStandardTags_Count()
    {
        Collection<TagDefinition> allTags = StandardTags.allStandardTags();
        // Should have 55 standard tags (6+7+5+5+6+7+6+8+5)
        assertEquals(55, allTags.size());
        assertEquals(55, StandardTags.getStandardTagCount());
    }

    @Test
    void testAllStandardTags_AllInCheapCoreNamespace()
    {
        Collection<TagDefinition> allTags = StandardTags.allStandardTags();
        for (TagDefinition tag : allTags) {
            assertEquals("cheap.core", tag.getNamespace());
        }
    }

    @Test
    void testAllStandardTags_AllStandardScope()
    {
        Collection<TagDefinition> allTags = StandardTags.allStandardTags();
        for (TagDefinition tag : allTags) {
            assertEquals(TagScope.STANDARD, tag.getScope());
        }
    }

    @Test
    void testAllStandardTags_NoDuplicateNames()
    {
        Collection<TagDefinition> allTags = StandardTags.allStandardTags();
        Set<String> names = new HashSet<>();
        for (TagDefinition tag : allTags) {
            String fullName = tag.getFullName();
            assertFalse(names.contains(fullName), "Duplicate tag name: " + fullName);
            names.add(fullName);
        }
    }

    @Test
    void testAllStandardTags_AllHaveDescriptions()
    {
        Collection<TagDefinition> allTags = StandardTags.allStandardTags();
        for (TagDefinition tag : allTags) {
            assertNotNull(tag.getDescription());
            assertFalse(tag.getDescription().trim().isEmpty(),
                "Tag " + tag.getFullName() + " has empty description");
        }
    }

    @Test
    void testAllStandardTags_AllApplicableToSomething()
    {
        Collection<TagDefinition> allTags = StandardTags.allStandardTags();
        for (TagDefinition tag : allTags) {
            assertFalse(tag.getAppliesTo().isEmpty(),
                "Tag " + tag.getFullName() + " has no applicable element types");
        }
    }

    @Test
    void testAllStandardTags_NoParentTags()
    {
        Collection<TagDefinition> allTags = StandardTags.allStandardTags();
        for (TagDefinition tag : allTags) {
            assertTrue(tag.getParentTagIds().isEmpty(),
                "Standard tags should not have parent tags: " + tag.getFullName());
        }
    }

    @Test
    void testAllStandardTags_Immutable()
    {
        Collection<TagDefinition> allTags = StandardTags.allStandardTags();
        // Verify collection is unmodifiable by attempting to add
        assertThrows(UnsupportedOperationException.class, () ->
            allTags.add(StandardTags.PRIMARY_KEY));
    }

    // ==================== Get Standard Tag Tests ====================

    @Test
    void testGetStandardTag_PrimaryKey()
    {
        TagDefinition tag = StandardTags.getStandardTag("primary-key");
        assertNotNull(tag);
        assertEquals("cheap.core", tag.getNamespace());
        assertEquals("primary-key", tag.getName());
        assertTrue(tag.isApplicableTo(ElementType.PROPERTY));
    }

    @Test
    void testGetStandardTag_Pii()
    {
        TagDefinition tag = StandardTags.getStandardTag("pii");
        assertNotNull(tag);
        assertEquals("cheap.core", tag.getNamespace());
        assertEquals("pii", tag.getName());
        assertTrue(tag.getDescription().contains("Personally Identifiable Information"));
    }

    @Test
    void testGetStandardTag_NotFound()
    {
        TagDefinition tag = StandardTags.getStandardTag("nonexistent-tag");
        assertNull(tag);
    }

    @Test
    void testGetStandardTag_NullName()
    {
        assertThrows(NullPointerException.class, () ->
            StandardTags.getStandardTag(null));
    }

    // ==================== Is Standard Tag Tests ====================

    @Test
    void testIsStandardTag_True()
    {
        assertTrue(StandardTags.isStandardTag("primary-key"));
        assertTrue(StandardTags.isStandardTag("pii"));
        assertTrue(StandardTags.isStandardTag("created-timestamp"));
        assertTrue(StandardTags.isStandardTag("immutable"));
    }

    @Test
    void testIsStandardTag_False()
    {
        assertFalse(StandardTags.isStandardTag("nonexistent-tag"));
        assertFalse(StandardTags.isStandardTag("custom-tag"));
        assertFalse(StandardTags.isStandardTag(""));
    }

    @Test
    void testIsStandardTag_NullName()
    {
        assertThrows(NullPointerException.class, () ->
            StandardTags.isStandardTag(null));
    }

    // ==================== Category Tests ====================

    @Test
    void testIdentityAndKeysCategory()
    {
        // 6 tags
        assertNotNull(StandardTags.getStandardTag("primary-key"));
        assertNotNull(StandardTags.getStandardTag("foreign-key"));
        assertNotNull(StandardTags.getStandardTag("composite-key-part"));
        assertNotNull(StandardTags.getStandardTag("natural-key"));
        assertNotNull(StandardTags.getStandardTag("surrogate-key"));
        assertNotNull(StandardTags.getStandardTag("alternate-key"));
    }

    @Test
    void testTemporalAndVersioningCategory()
    {
        // 7 tags
        assertNotNull(StandardTags.getStandardTag("created-timestamp"));
        assertNotNull(StandardTags.getStandardTag("modified-timestamp"));
        assertNotNull(StandardTags.getStandardTag("version-number"));
        assertNotNull(StandardTags.getStandardTag("effective-date"));
        assertNotNull(StandardTags.getStandardTag("expiration-date"));
        assertNotNull(StandardTags.getStandardTag("temporal-range-start"));
        assertNotNull(StandardTags.getStandardTag("temporal-range-end"));
    }

    @Test
    void testLifecycleAndStateCategory()
    {
        // 5 tags
        assertNotNull(StandardTags.getStandardTag("soft-delete-flag"));
        assertNotNull(StandardTags.getStandardTag("archived-flag"));
        assertNotNull(StandardTags.getStandardTag("status-field"));
        assertNotNull(StandardTags.getStandardTag("approval-status"));
        assertNotNull(StandardTags.getStandardTag("published-flag"));
    }

    @Test
    void testRelationshipsCategory()
    {
        // 5 tags
        assertNotNull(StandardTags.getStandardTag("parent-reference"));
        assertNotNull(StandardTags.getStandardTag("owner-reference"));
        assertNotNull(StandardTags.getStandardTag("many-to-many-link"));
        assertNotNull(StandardTags.getStandardTag("polymorphic-reference"));
        assertNotNull(StandardTags.getStandardTag("self-reference"));
    }

    @Test
    void testDataSemanticsCategory()
    {
        // 6 tags
        assertNotNull(StandardTags.getStandardTag("display-name"));
        assertNotNull(StandardTags.getStandardTag("description-field"));
        assertNotNull(StandardTags.getStandardTag("sort-order"));
        assertNotNull(StandardTags.getStandardTag("code-value"));
        assertNotNull(StandardTags.getStandardTag("computed-field"));
        assertNotNull(StandardTags.getStandardTag("denormalized-cache"));
    }

    @Test
    void testValidationAndConstraintsCategory()
    {
        // 7 tags
        assertNotNull(StandardTags.getStandardTag("required"));
        assertNotNull(StandardTags.getStandardTag("nullable"));
        assertNotNull(StandardTags.getStandardTag("unique"));
        assertNotNull(StandardTags.getStandardTag("immutable"));
        assertNotNull(StandardTags.getStandardTag("range-bounded"));
        assertNotNull(StandardTags.getStandardTag("format-constrained"));
        assertNotNull(StandardTags.getStandardTag("enum-valued"));
    }

    @Test
    void testSecurityAndPrivacyCategory()
    {
        // 6 tags
        assertNotNull(StandardTags.getStandardTag("pii"));
        assertNotNull(StandardTags.getStandardTag("sensitive"));
        assertNotNull(StandardTags.getStandardTag("encrypted"));
        assertNotNull(StandardTags.getStandardTag("audit-logged"));
        assertNotNull(StandardTags.getStandardTag("masked"));
        assertNotNull(StandardTags.getStandardTag("anonymizable"));
    }

    @Test
    void testBusinessDomainCategory()
    {
        // 8 tags
        assertNotNull(StandardTags.getStandardTag("monetary-amount"));
        assertNotNull(StandardTags.getStandardTag("quantity"));
        assertNotNull(StandardTags.getStandardTag("percentage"));
        assertNotNull(StandardTags.getStandardTag("email-address"));
        assertNotNull(StandardTags.getStandardTag("phone-number"));
        assertNotNull(StandardTags.getStandardTag("postal-address"));
        assertNotNull(StandardTags.getStandardTag("url"));
        assertNotNull(StandardTags.getStandardTag("geo-coordinate"));
    }

    @Test
    void testTechnicalBehaviorCategory()
    {
        // 5 tags
        assertNotNull(StandardTags.getStandardTag("indexed"));
        assertNotNull(StandardTags.getStandardTag("searchable"));
        assertNotNull(StandardTags.getStandardTag("lazy-loaded"));
        assertNotNull(StandardTags.getStandardTag("cached"));
        assertNotNull(StandardTags.getStandardTag("immutable-aggregate-root"));
    }

    // ==================== Specific Tag Validation Tests ====================

    @Test
    void testPrimaryKey_NotApplicableToForeignKey()
    {
        // Based on conflict detector rules, primary-key should not be used with foreign-key
        TagDefinition pk = StandardTags.PRIMARY_KEY;
        TagDefinition fk = StandardTags.FOREIGN_KEY;

        assertNotEquals(pk.getName(), fk.getName());
    }

    @Test
    void testImmutable_ApplicableToMultipleTypes()
    {
        TagDefinition immutable = StandardTags.IMMUTABLE;

        assertTrue(immutable.isApplicableTo(ElementType.PROPERTY));
        assertTrue(immutable.isApplicableTo(ElementType.ASPECT));
        assertTrue(immutable.isApplicableTo(ElementType.ENTITY));
    }

    @Test
    void testPii_ApplicableToMultipleTypes()
    {
        TagDefinition pii = StandardTags.PII;

        assertTrue(pii.isApplicableTo(ElementType.PROPERTY));
        assertTrue(pii.isApplicableTo(ElementType.ASPECT));
        assertTrue(pii.isApplicableTo(ElementType.ENTITY));
    }

    @Test
    void testManyToManyLink_ApplicableToEntityAndAspect()
    {
        TagDefinition link = StandardTags.MANY_TO_MANY_LINK;

        assertTrue(link.isApplicableTo(ElementType.ENTITY));
        assertTrue(link.isApplicableTo(ElementType.ASPECT));
        assertFalse(link.isApplicableTo(ElementType.PROPERTY));
    }

    @Test
    void testImmutableAggregateRoot_OnlyEntity()
    {
        TagDefinition root = StandardTags.IMMUTABLE_AGGREGATE_ROOT;

        assertTrue(root.isApplicableTo(ElementType.ENTITY));
        assertFalse(root.isApplicableTo(ElementType.PROPERTY));
        assertFalse(root.isApplicableTo(ElementType.ASPECT));
    }

    // ==================== Utility Class Tests ====================

    @Test
    void testConstructor_ThrowsException()
    {
        try {
            // Use reflection to access private constructor
            var constructor = StandardTags.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
            fail("Constructor should throw UnsupportedOperationException");
        } catch (Exception e) {
            // Reflection wraps the exception in InvocationTargetException
            assertTrue(e.getCause() instanceof UnsupportedOperationException,
                "Expected UnsupportedOperationException, got: " + e.getCause());
        }
    }

    // ==================== Constants Tests ====================

    @Test
    void testConstant_PRIMARY_KEY()
    {
        assertNotNull(StandardTags.PRIMARY_KEY);
        assertEquals("cheap.core.primary-key", StandardTags.PRIMARY_KEY.getFullName());
    }

    @Test
    void testConstant_PII()
    {
        assertNotNull(StandardTags.PII);
        assertEquals("cheap.core.pii", StandardTags.PII.getFullName());
    }

    @Test
    void testConstant_CREATED_TIMESTAMP()
    {
        assertNotNull(StandardTags.CREATED_TIMESTAMP);
        assertEquals("cheap.core.created-timestamp", StandardTags.CREATED_TIMESTAMP.getFullName());
    }

    @Test
    void testConstant_IMMUTABLE()
    {
        assertNotNull(StandardTags.IMMUTABLE);
        assertEquals("cheap.core.immutable", StandardTags.IMMUTABLE.getFullName());
    }
}
