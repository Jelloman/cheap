/*
 * Copyright (c) 2025. David Noha
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
import net.netbeing.cheap.tags.model.TagApplication;
import net.netbeing.cheap.tags.model.TagSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TagApplicationAspectTest
{
    private static TagApplication tagApp;
    private final Entity testEntity = new EntityImpl();
    private final Catalog testCatalog = new CatalogImpl();
    private final UUID tagDefId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID targetId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final ZonedDateTime timestamp = ZonedDateTime.now();

    private ImmutablePojoAspectDef aspectDef;
    private TagApplicationAspect tagAppAspect;

    @BeforeEach
    void setUp()
    {
        aspectDef = TagApplicationAspect.aspectDef();
        tagApp = new TagApplication(
            tagDefId,
            targetId,
            ElementType.PROPERTY,
            Map.of("format", "INV-{year}-{seq}", "maxLength", 50),
            TagSource.EXPLICIT,
            timestamp,
            "john.doe@example.com"
        );
    }

    @AfterEach
    void tearDown()
    {
        tagApp = null;
        tagAppAspect = null;
    }

    @Test
    void testAspectDef()
    {
        ImmutablePojoAspectDef def = TagApplicationAspect.aspectDef();
        assertNotNull(def);
        assertEquals("net.netbeing.cheap.tags.model.TagApplication", def.name());

        // Verify expected properties exist
        assertNotNull(def.propertyDef("tagDefinitionId"));
        assertNotNull(def.propertyDef("targetElementId"));
        assertNotNull(def.propertyDef("targetType"));
        assertNotNull(def.propertyDef("metadata"));
        assertNotNull(def.propertyDef("source"));
        assertNotNull(def.propertyDef("appliedAt"));
        assertNotNull(def.propertyDef("appliedBy"));
    }

    @Test
    void testConstruct()
    {
        assertDoesNotThrow(() -> new TagApplicationAspect(testEntity, tagApp));
    }

    @Test
    void testEntity()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);
        assertEquals(testEntity, tagAppAspect.entity());
    }

    @Test
    void testDef()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);
        assertEquals(aspectDef, tagAppAspect.def());
    }

    @Test
    void testObject()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);
        assertEquals(tagApp, tagAppAspect.object());
    }

    @Test
    void testGetTagDefinitionId()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);
        assertEquals(tagDefId, tagAppAspect.getTagDefinitionId());
    }

    @Test
    void testGetTargetElementId()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);
        assertEquals(targetId, tagAppAspect.getTargetElementId());
    }

    @Test
    void testGetTargetType()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);
        assertEquals(ElementType.PROPERTY, tagAppAspect.getTargetType());
    }

    @Test
    void testGetMetadata()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);
        Map<String, Object> metadata = tagAppAspect.getMetadata();
        assertEquals(2, metadata.size());
        assertEquals("INV-{year}-{seq}", metadata.get("format"));
        assertEquals(50, metadata.get("maxLength"));
    }

    @Test
    void testGetSource()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);
        assertEquals(TagSource.EXPLICIT, tagAppAspect.getSource());
    }

    @Test
    void testGetAppliedAt()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);
        assertEquals(timestamp, tagAppAspect.getAppliedAt());
    }

    @Test
    void testGetAppliedBy()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);
        assertEquals("john.doe@example.com", tagAppAspect.getAppliedBy());
    }

    @Test
    void testGetAppliedBy_Null()
    {
        TagApplication appWithNullAppliedBy = new TagApplication(
            tagDefId,
            targetId,
            ElementType.PROPERTY,
            null,
            TagSource.INFERRED,
            timestamp,
            null  // no appliedBy
        );

        tagAppAspect = new TagApplicationAspect(testEntity, appWithNullAppliedBy);
        assertNull(tagAppAspect.getAppliedBy());
    }

    @Test
    void testReadPropertyViaAspectInterface()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);

        // Access properties through Aspect interface
        Property tagDefIdProp = tagAppAspect.get("tagDefinitionId");
        assertEquals(tagDefId, tagDefIdProp.read());

        Property targetIdProp = tagAppAspect.get("targetElementId");
        assertEquals(targetId, targetIdProp.read());

        Property typeProp = tagAppAspect.get("targetType");
        assertEquals(ElementType.PROPERTY, typeProp.read());

        Property sourceProp = tagAppAspect.get("source");
        assertEquals(TagSource.EXPLICIT, sourceProp.read());
    }

    @Test
    void testUnsafeReadObj()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);

        assertEquals(tagDefId, tagAppAspect.unsafeReadObj("tagDefinitionId"));
        assertEquals(targetId, tagAppAspect.unsafeReadObj("targetElementId"));
        assertEquals(ElementType.PROPERTY, tagAppAspect.unsafeReadObj("targetType"));
        assertEquals(TagSource.EXPLICIT, tagAppAspect.unsafeReadObj("source"));
        assertEquals(timestamp, tagAppAspect.unsafeReadObj("appliedAt"));
        assertEquals("john.doe@example.com", tagAppAspect.unsafeReadObj("appliedBy"));
    }

    @Test
    void testImmutability_CannotWrite()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);
        assertThrows(UnsupportedOperationException.class, () ->
            tagAppAspect.unsafeWrite("source", TagSource.INFERRED)
        );
    }

    @Test
    void testImmutability_CannotAdd()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);
        net.netbeing.cheap.model.PropertyDef propDef = aspectDef.propertyDef("source");
        Property newProp = new net.netbeing.cheap.impl.basic.PropertyImpl(propDef, TagSource.INFERRED);
        assertThrows(UnsupportedOperationException.class, () ->
            tagAppAspect.unsafeAdd(newProp)
        );
    }

    @Test
    void testImmutability_CannotRemove()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);
        assertThrows(UnsupportedOperationException.class, () ->
            tagAppAspect.unsafeRemove("source")
        );
    }

    @Test
    void testAsAspect()
    {
        tagAppAspect = new TagApplicationAspect(testEntity, tagApp);

        // Verify it can be used as an Aspect
        Aspect aspect = tagAppAspect;
        assertEquals(testEntity, aspect.entity());
        assertEquals(aspectDef, aspect.def());
    }

    @Test
    void testWithDifferentElementTypes()
    {
        for (ElementType type : ElementType.values()) {
            TagApplication app = new TagApplication(
                tagDefId,
                targetId,
                type,
                null,
                TagSource.EXPLICIT,
                timestamp,
                null
            );

            tagAppAspect = new TagApplicationAspect(testEntity, app);
            assertEquals(type, tagAppAspect.getTargetType());
        }
    }

    @Test
    void testWithDifferentSources()
    {
        for (TagSource source : TagSource.values()) {
            TagApplication app = new TagApplication(
                tagDefId,
                targetId,
                ElementType.PROPERTY,
                null,
                source,
                timestamp,
                null
            );

            tagAppAspect = new TagApplicationAspect(testEntity, app);
            assertEquals(source, tagAppAspect.getSource());
        }
    }
}
