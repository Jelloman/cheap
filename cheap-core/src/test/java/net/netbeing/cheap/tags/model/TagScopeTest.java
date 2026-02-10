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

class TagScopeTest
{
    @Test
    void testEnumValues()
    {
        assertEquals(2, TagScope.values().length);
        assertNotNull(TagScope.STANDARD);
        assertNotNull(TagScope.CUSTOM);
    }

    @Test
    void testEnumOrdering()
    {
        TagScope[] values = TagScope.values();
        assertEquals(TagScope.STANDARD, values[0]);
        assertEquals(TagScope.CUSTOM, values[1]);
    }

    @Test
    void testValueOf()
    {
        assertEquals(TagScope.STANDARD, TagScope.valueOf("STANDARD"));
        assertEquals(TagScope.CUSTOM, TagScope.valueOf("CUSTOM"));
    }
}
