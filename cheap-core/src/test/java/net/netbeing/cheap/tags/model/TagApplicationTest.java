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

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TagApplicationTest
{
    private final UUID tagDefId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID targetId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final ZonedDateTime timestamp = ZonedDateTime.now();

    @Test
    void testConstruct_Valid()
    {
        TagApplication app = new TagApplication(
            tagDefId,
            targetId,
            ElementType.PROPERTY,
            Map.of("format", "INV-{year}-{seq}", "maxLength", 50),
            TagSource.EXPLICIT,
            timestamp,
            "john.doe@example.com"
        );

        assertNotNull(app);
        assertEquals(tagDefId, app.getTagDefinitionId());
        assertEquals(targetId, app.getTargetElementId());
        assertEquals(ElementType.PROPERTY, app.getTargetType());
        assertEquals(2, app.getMetadata().size());
        assertEquals("INV-{year}-{seq}", app.getMetadata().get("format"));
        assertEquals(50, app.getMetadata().get("maxLength"));
        assertEquals(TagSource.EXPLICIT, app.getSource());
        assertEquals(timestamp, app.getAppliedAt());
        assertEquals("john.doe@example.com", app.getAppliedBy());
    }

    @Test
    void testConstruct_MinimalValid()
    {
        TagApplication app = new TagApplication(
            tagDefId,
            targetId,
            ElementType.ENTITY,
            null,  // no metadata
            TagSource.INFERRED,
            timestamp,
            null   // no appliedBy
        );

        assertNotNull(app);
        assertEquals(tagDefId, app.getTagDefinitionId());
        assertEquals(targetId, app.getTargetElementId());
        assertEquals(ElementType.ENTITY, app.getTargetType());
        assertTrue(app.getMetadata().isEmpty());
        assertEquals(TagSource.INFERRED, app.getSource());
        assertEquals(timestamp, app.getAppliedAt());
        assertNull(app.getAppliedBy());
    }

    @Test
    void testConstruct_AllElementTypes()
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

            assertEquals(type, app.getTargetType());
        }
    }

    @Test
    void testConstruct_AllSources()
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

            assertEquals(source, app.getSource());
        }
    }

    @Test
    void testConstruct_NullTagDefinitionId()
    {
        assertThrows(NullPointerException.class, () ->
            new TagApplication(
                null,
                targetId,
                ElementType.PROPERTY,
                null,
                TagSource.EXPLICIT,
                timestamp,
                null
            )
        );
    }

    @Test
    void testConstruct_NullTargetElementId()
    {
        assertThrows(NullPointerException.class, () ->
            new TagApplication(
                tagDefId,
                null,
                ElementType.PROPERTY,
                null,
                TagSource.EXPLICIT,
                timestamp,
                null
            )
        );
    }

    @Test
    void testConstruct_NullTargetType()
    {
        assertThrows(NullPointerException.class, () ->
            new TagApplication(
                tagDefId,
                targetId,
                null,
                null,
                TagSource.EXPLICIT,
                timestamp,
                null
            )
        );
    }

    @Test
    void testConstruct_NullSource()
    {
        assertThrows(NullPointerException.class, () ->
            new TagApplication(
                tagDefId,
                targetId,
                ElementType.PROPERTY,
                null,
                null,
                timestamp,
                null
            )
        );
    }

    @Test
    void testConstruct_NullAppliedAt()
    {
        assertThrows(NullPointerException.class, () ->
            new TagApplication(
                tagDefId,
                targetId,
                ElementType.PROPERTY,
                null,
                TagSource.EXPLICIT,
                null,
                null
            )
        );
    }

    @Test
    void testEquals_SameInstance()
    {
        TagApplication app = new TagApplication(
            tagDefId,
            targetId,
            ElementType.PROPERTY,
            null,
            TagSource.EXPLICIT,
            timestamp,
            null
        );

        assertEquals(app, app);
    }

    @Test
    void testEquals_SameValues()
    {
        TagApplication app1 = new TagApplication(
            tagDefId,
            targetId,
            ElementType.PROPERTY,
            Map.of("key", "value"),
            TagSource.EXPLICIT,
            timestamp,
            "user@example.com"
        );

        TagApplication app2 = new TagApplication(
            tagDefId,
            targetId,
            ElementType.PROPERTY,
            Map.of("key", "value"),
            TagSource.EXPLICIT,
            timestamp,
            "user@example.com"
        );

        assertEquals(app1, app2);
        assertEquals(app1.hashCode(), app2.hashCode());
    }

    @Test
    void testEquals_DifferentValues()
    {
        TagApplication app1 = new TagApplication(
            tagDefId,
            targetId,
            ElementType.PROPERTY,
            null,
            TagSource.EXPLICIT,
            timestamp,
            null
        );

        TagApplication app2 = new TagApplication(
            UUID.randomUUID(),  // different tag def
            targetId,
            ElementType.PROPERTY,
            null,
            TagSource.EXPLICIT,
            timestamp,
            null
        );

        assertNotEquals(app1, app2);
    }

    @Test
    void testEquals_Null()
    {
        TagApplication app = new TagApplication(
            tagDefId,
            targetId,
            ElementType.PROPERTY,
            null,
            TagSource.EXPLICIT,
            timestamp,
            null
        );

        assertNotEquals(null, app);
    }

    @Test
    void testEquals_DifferentClass()
    {
        TagApplication app = new TagApplication(
            tagDefId,
            targetId,
            ElementType.PROPERTY,
            null,
            TagSource.EXPLICIT,
            timestamp,
            null
        );

        assertNotEquals(app, "not a tag application");
    }

    @Test
    void testToString()
    {
        TagApplication app = new TagApplication(
            tagDefId,
            targetId,
            ElementType.PROPERTY,
            Map.of("key", "value"),
            TagSource.EXPLICIT,
            timestamp,
            "user@example.com"
        );

        String str = app.toString();
        assertNotNull(str);
        assertTrue(str.contains(tagDefId.toString()));
        assertTrue(str.contains(targetId.toString()));
        assertTrue(str.contains("PROPERTY"));
        assertTrue(str.contains("EXPLICIT"));
    }

    @Test
    void testImmutability_Metadata()
    {
        TagApplication app = new TagApplication(
            tagDefId,
            targetId,
            ElementType.PROPERTY,
            Map.of("key", "value"),
            TagSource.EXPLICIT,
            timestamp,
            null
        );

        Map<String, Object> metadata = app.getMetadata();
        assertThrows(UnsupportedOperationException.class, () -> metadata.put("newKey", "newValue"));
    }

    @Test
    void testMetadata_EmptyWhenNull()
    {
        TagApplication app = new TagApplication(
            tagDefId,
            targetId,
            ElementType.PROPERTY,
            null,
            TagSource.EXPLICIT,
            timestamp,
            null
        );

        assertNotNull(app.getMetadata());
        assertTrue(app.getMetadata().isEmpty());
    }

    @Test
    void testMetadata_ComplexValues()
    {
        Map<String, Object> metadata = Map.of(
            "string", "value",
            "number", 42,
            "boolean", true,
            "list", java.util.List.of("a", "b", "c")
        );

        TagApplication app = new TagApplication(
            tagDefId,
            targetId,
            ElementType.PROPERTY,
            metadata,
            TagSource.EXPLICIT,
            timestamp,
            null
        );

        assertEquals(4, app.getMetadata().size());
        assertEquals("value", app.getMetadata().get("string"));
        assertEquals(42, app.getMetadata().get("number"));
        assertEquals(true, app.getMetadata().get("boolean"));
        assertEquals(java.util.List.of("a", "b", "c"), app.getMetadata().get("list"));
    }
}
