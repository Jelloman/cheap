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

package net.netbeing.cheap.tags.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ElementTypeTest
{
    @Test
    void testEnumValues()
    {
        assertEquals(5, ElementType.values().length);
        assertNotNull(ElementType.PROPERTY);
        assertNotNull(ElementType.ASPECT);
        assertNotNull(ElementType.HIERARCHY);
        assertNotNull(ElementType.ENTITY);
        assertNotNull(ElementType.CATALOG);
    }

    @Test
    void testFromString_Valid()
    {
        assertEquals(ElementType.PROPERTY, ElementType.fromString("property"));
        assertEquals(ElementType.PROPERTY, ElementType.fromString("PROPERTY"));
        assertEquals(ElementType.PROPERTY, ElementType.fromString("Property"));

        assertEquals(ElementType.ASPECT, ElementType.fromString("aspect"));
        assertEquals(ElementType.ASPECT, ElementType.fromString("ASPECT"));

        assertEquals(ElementType.HIERARCHY, ElementType.fromString("hierarchy"));
        assertEquals(ElementType.ENTITY, ElementType.fromString("entity"));
        assertEquals(ElementType.CATALOG, ElementType.fromString("catalog"));
    }

    @Test
    void testFromString_Invalid()
    {
        assertNull(ElementType.fromString(null));
        assertNull(ElementType.fromString(""));
        assertNull(ElementType.fromString("invalid"));
        assertNull(ElementType.fromString("prop"));
        assertNull(ElementType.fromString("properties"));
    }

    @Test
    void testToLowerString()
    {
        assertEquals("property", ElementType.PROPERTY.toLowerString());
        assertEquals("aspect", ElementType.ASPECT.toLowerString());
        assertEquals("hierarchy", ElementType.HIERARCHY.toLowerString());
        assertEquals("entity", ElementType.ENTITY.toLowerString());
        assertEquals("catalog", ElementType.CATALOG.toLowerString());
    }

    @Test
    void testEnumOrdering()
    {
        // Verify enum ordering from finest to coarsest grain
        ElementType[] values = ElementType.values();
        assertEquals(ElementType.PROPERTY, values[0]);
        assertEquals(ElementType.ASPECT, values[1]);
        assertEquals(ElementType.HIERARCHY, values[2]);
        assertEquals(ElementType.ENTITY, values[3]);
        assertEquals(ElementType.CATALOG, values[4]);
    }
}
