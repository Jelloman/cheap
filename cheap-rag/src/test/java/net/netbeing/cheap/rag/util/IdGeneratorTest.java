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

package net.netbeing.cheap.rag.util;

import net.netbeing.cheap.rag.extractor.model.ArtifactType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdGeneratorTest
{
    @Test
    void testGenerateIdFormat()
    {
        String id = IdGenerator.generateId("java", ArtifactType.CLASS, "com.example.MyClass");

        // Should have format: java_class_{16-char-hash}
        assertTrue(id.startsWith("java_class_"));
        assertEquals(27, id.length()); // "java_class_" (11) + hash (16) = 27
    }

    @Test
    void testGenerateIdStability()
    {
        // Same input should always produce same ID
        String id1 = IdGenerator.generateId("java", ArtifactType.CLASS, "com.example.MyClass");
        String id2 = IdGenerator.generateId("java", ArtifactType.CLASS, "com.example.MyClass");

        assertEquals(id1, id2);
    }

    @Test
    void testGenerateIdUniqueness()
    {
        // Different inputs should produce different IDs
        String id1 = IdGenerator.generateId("java", ArtifactType.CLASS, "com.example.MyClass");
        String id2 = IdGenerator.generateId("java", ArtifactType.CLASS, "com.example.OtherClass");

        assertNotEquals(id1, id2);
    }

    @Test
    void testGenerateIdForDifferentTypes()
    {
        String classId = IdGenerator.generateId("java", ArtifactType.CLASS, "com.example.MyClass");
        String interfaceId = IdGenerator.generateId("java", ArtifactType.INTERFACE, "com.example.MyClass");

        // Same name but different type should produce different IDs
        assertNotEquals(classId, interfaceId);
        assertTrue(classId.startsWith("java_class_"));
        assertTrue(interfaceId.startsWith("java_interface_"));
    }

    @Test
    void testGenerateIdForMethod()
    {
        String id = IdGenerator.generateId("java", ArtifactType.METHOD, "com.example.MyClass.myMethod");

        assertTrue(id.startsWith("java_method_"));
    }
}
