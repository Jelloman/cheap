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

package net.netbeing.cheap.tags.aspect;

import net.netbeing.cheap.impl.basic.CatalogImpl;
import net.netbeing.cheap.impl.basic.EntityImpl;
import net.netbeing.cheap.impl.reflect.ImmutablePojoAspectDef;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.Property;
import net.netbeing.cheap.tags.model.ElementType;
import net.netbeing.cheap.tags.model.TagDefinition;
import net.netbeing.cheap.tags.model.TagScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TagDefinitionAspectTest
{
    private static TagDefinition tagDef;
    private final Entity testEntity = new EntityImpl();
    private final Catalog testCatalog = new CatalogImpl();
    private ImmutablePojoAspectDef aspectDef;
    private TagDefinitionAspect tagDefAspect;

    @BeforeEach
    void setUp()
    {
        aspectDef = TagDefinitionAspect.aspectDef();
        tagDef = new TagDefinition(
            "cheap.core",
            "primary-key",
            "Primary identifier for entity",
            List.of(ElementType.PROPERTY, ElementType.ENTITY),
            TagScope.STANDARD,
            List.of("pk", "primary_key"),
            List.of(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        );
    }

    @AfterEach
    void tearDown()
    {
        tagDef = null;
        tagDefAspect = null;
    }

    @Test
    void testAspectDef()
    {
        ImmutablePojoAspectDef def = TagDefinitionAspect.aspectDef();
        assertNotNull(def);
        assertEquals("net.netbeing.cheap.tags.model.TagDefinition", def.name());

        // Verify expected properties exist
        assertNotNull(def.propertyDef("namespace"));
        assertNotNull(def.propertyDef("name"));
        assertNotNull(def.propertyDef("description"));
        assertNotNull(def.propertyDef("appliesTo"));
        assertNotNull(def.propertyDef("scope"));
        assertNotNull(def.propertyDef("aliases"));
        assertNotNull(def.propertyDef("parentTagIds"));
        assertNotNull(def.propertyDef("fullName"));
    }

    @Test
    void testConstruct()
    {
        assertDoesNotThrow(() -> new TagDefinitionAspect(testEntity, tagDef));
    }

    @Test
    void testEntity()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        assertEquals(testEntity, tagDefAspect.entity());
    }

    @Test
    void testDef()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        assertEquals(aspectDef, tagDefAspect.def());
    }

    @Test
    void testObject()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        assertEquals(tagDef, tagDefAspect.object());
    }

    @Test
    void testGetNamespace()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        assertEquals("cheap.core", tagDefAspect.getNamespace());
    }

    @Test
    void testGetName()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        assertEquals("primary-key", tagDefAspect.getName());
    }

    @Test
    void testGetFullName()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        assertEquals("cheap.core.primary-key", tagDefAspect.getFullName());
    }

    @Test
    void testGetDescription()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        assertEquals("Primary identifier for entity", tagDefAspect.getDescription());
    }

    @Test
    void testGetAppliesTo()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        List<ElementType> appliesTo = tagDefAspect.getAppliesTo();
        assertEquals(2, appliesTo.size());
        assertTrue(appliesTo.contains(ElementType.PROPERTY));
        assertTrue(appliesTo.contains(ElementType.ENTITY));
    }

    @Test
    void testGetScope()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        assertEquals(TagScope.STANDARD, tagDefAspect.getScope());
    }

    @Test
    void testGetAliases()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        List<String> aliases = tagDefAspect.getAliases();
        assertEquals(2, aliases.size());
        assertTrue(aliases.contains("pk"));
        assertTrue(aliases.contains("primary_key"));
    }

    @Test
    void testGetParentTagIds()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        List<UUID> parentTagIds = tagDefAspect.getParentTagIds();
        assertEquals(1, parentTagIds.size());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), parentTagIds.get(0));
    }

    @Test
    void testIsApplicableTo()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        assertTrue(tagDefAspect.isApplicableTo(ElementType.PROPERTY));
        assertTrue(tagDefAspect.isApplicableTo(ElementType.ENTITY));
        assertFalse(tagDefAspect.isApplicableTo(ElementType.ASPECT));
        assertFalse(tagDefAspect.isApplicableTo(ElementType.HIERARCHY));
        assertFalse(tagDefAspect.isApplicableTo(ElementType.CATALOG));
    }

    @Test
    void testReadPropertyViaAspectInterface()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);

        // Access properties through Aspect interface
        Property namespaceProp = tagDefAspect.get("namespace");
        assertEquals("cheap.core", namespaceProp.read());

        Property nameProp = tagDefAspect.get("name");
        assertEquals("primary-key", nameProp.read());

        Property scopeProp = tagDefAspect.get("scope");
        assertEquals(TagScope.STANDARD, scopeProp.read());
    }

    @Test
    void testUnsafeReadObj()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);

        assertEquals("cheap.core", tagDefAspect.unsafeReadObj("namespace"));
        assertEquals("primary-key", tagDefAspect.unsafeReadObj("name"));
        assertEquals("cheap.core.primary-key", tagDefAspect.unsafeReadObj("fullName"));
        assertEquals(TagScope.STANDARD, tagDefAspect.unsafeReadObj("scope"));
    }

    @Test
    void testImmutability_CannotWrite()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        assertThrows(UnsupportedOperationException.class, () ->
            tagDefAspect.unsafeWrite("namespace", "new.namespace")
        );
    }

    @Test
    void testImmutability_CannotAdd()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        net.netbeing.cheap.model.PropertyDef propDef = aspectDef.propertyDef("namespace");
        Property newProp = new net.netbeing.cheap.impl.basic.PropertyImpl(propDef, "test-value");
        assertThrows(UnsupportedOperationException.class, () ->
            tagDefAspect.unsafeAdd(newProp)
        );
    }

    @Test
    void testImmutability_CannotRemove()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);
        assertThrows(UnsupportedOperationException.class, () ->
            tagDefAspect.unsafeRemove("namespace")
        );
    }

    @Test
    void testAsAspect()
    {
        tagDefAspect = new TagDefinitionAspect(testEntity, tagDef);

        // Verify it can be used as an Aspect
        Aspect aspect = tagDefAspect;
        assertEquals(testEntity, aspect.entity());
        assertEquals(aspectDef, aspect.def());
    }
}
