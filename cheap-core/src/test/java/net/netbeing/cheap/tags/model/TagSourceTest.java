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

import static org.junit.jupiter.api.Assertions.*;

class TagSourceTest
{
    @Test
    void testEnumValues()
    {
        assertEquals(3, TagSource.values().length);
        assertNotNull(TagSource.EXPLICIT);
        assertNotNull(TagSource.INFERRED);
        assertNotNull(TagSource.GENERATED);
    }

    @Test
    void testEnumOrdering()
    {
        TagSource[] values = TagSource.values();
        assertEquals(TagSource.EXPLICIT, values[0]);
        assertEquals(TagSource.INFERRED, values[1]);
        assertEquals(TagSource.GENERATED, values[2]);
    }

    @Test
    void testValueOf()
    {
        assertEquals(TagSource.EXPLICIT, TagSource.valueOf("EXPLICIT"));
        assertEquals(TagSource.INFERRED, TagSource.valueOf("INFERRED"));
        assertEquals(TagSource.GENERATED, TagSource.valueOf("GENERATED"));
    }
}
