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

package net.netbeing.cheap.rag.extractor;

import net.netbeing.cheap.rag.extractor.model.ArtifactType;
import net.netbeing.cheap.rag.extractor.model.MetadataArtifact;
import net.netbeing.cheap.rag.extractor.model.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaExtractorTest
{
    @TempDir
    Path tempDir;

    @Test
    void testExtractSimpleClass() throws IOException
    {
        // Create a simple Java class file
        String javaCode = """
                package com.example;

                /**
                 * A simple test class.
                 */
                public class SimpleClass {
                    private String field1;

                    public void method1() {
                    }
                }
                """;

        Path javaFile = tempDir.resolve("SimpleClass.java");
        Files.writeString(javaFile, javaCode);

        // Extract artifacts
        JavaExtractor extractor = new JavaExtractor();
        List<MetadataArtifact> artifacts = extractor.extractFromFile(javaFile);

        // Should extract: 1 class, 1 field, 1 method = 3 artifacts
        assertEquals(3, artifacts.size());

        // Find the class artifact
        MetadataArtifact classArtifact = artifacts.stream()
                .filter(a -> a.getType() == ArtifactType.CLASS)
                .findFirst()
                .orElseThrow();

        assertEquals("SimpleClass", classArtifact.getName());
        assertEquals("java", classArtifact.getLanguage());
        assertEquals(SourceType.CODE, classArtifact.getSourceType());
        assertEquals("com.example", classArtifact.getModule());
        assertNotNull(classArtifact.getDocumentation());
        assertTrue(classArtifact.getDocumentation().contains("simple test class"));
        assertTrue(classArtifact.getId().startsWith("java_class_"));
    }

    @Test
    void testExtractInterface() throws IOException
    {
        String javaCode = """
                package com.example;

                /**
                 * A test interface.
                 */
                public interface TestInterface {
                    void doSomething();
                }
                """;

        Path javaFile = tempDir.resolve("TestInterface.java");
        Files.writeString(javaFile, javaCode);

        JavaExtractor extractor = new JavaExtractor();
        List<MetadataArtifact> artifacts = extractor.extractFromFile(javaFile);

        // Should extract: 1 interface, 1 method = 2 artifacts
        assertTrue(artifacts.size() >= 1);

        MetadataArtifact interfaceArtifact = artifacts.stream()
                .filter(a -> a.getType() == ArtifactType.INTERFACE)
                .findFirst()
                .orElseThrow();

        assertEquals("TestInterface", interfaceArtifact.getName());
        assertEquals(ArtifactType.INTERFACE, interfaceArtifact.getType());
        assertTrue(interfaceArtifact.getId().startsWith("java_interface_"));
    }

    @Test
    void testExtractEnum() throws IOException
    {
        String javaCode = """
                package com.example;

                /**
                 * A test enum.
                 */
                public enum Color {
                    RED, GREEN, BLUE
                }
                """;

        Path javaFile = tempDir.resolve("Color.java");
        Files.writeString(javaFile, javaCode);

        JavaExtractor extractor = new JavaExtractor();
        List<MetadataArtifact> artifacts = extractor.extractFromFile(javaFile);

        MetadataArtifact enumArtifact = artifacts.stream()
                .filter(a -> a.getType() == ArtifactType.ENUM)
                .findFirst()
                .orElseThrow();

        assertEquals("Color", enumArtifact.getName());
        assertEquals(ArtifactType.ENUM, enumArtifact.getType());
        assertTrue(enumArtifact.getEmbeddingText().contains("RED"));
        assertTrue(enumArtifact.getId().startsWith("java_enum_"));
    }

    @Test
    void testPublicOnlyConfig() throws IOException
    {
        String javaCode = """
                package com.example;

                public class TestClass {
                    public String publicField;
                    private String privateField;

                    public void publicMethod() {}
                    private void privateMethod() {}
                }
                """;

        Path javaFile = tempDir.resolve("TestClass.java");
        Files.writeString(javaFile, javaCode);

        // Extract with publicOnly = true
        JavaExtractorConfig config = JavaExtractorConfig.builder()
                .publicOnly(true)
                .build();

        JavaExtractor extractor = new JavaExtractor(config);
        List<MetadataArtifact> artifacts = extractor.extractFromFile(javaFile);

        // Should only extract public members
        // Class (public), publicField, publicMethod = 3 artifacts
        assertEquals(3, artifacts.size());

        // Verify no private members
        boolean hasPrivate = artifacts.stream()
                .anyMatch(a -> a.getName().contains("private"));
        assertFalse(hasPrivate);
    }

    @Test
    void testExtractFromDirectory() throws IOException
    {
        // Create multiple Java files
        String class1 = """
                package com.example;
                public class Class1 {}
                """;

        String class2 = """
                package com.example;
                public class Class2 {}
                """;

        Files.writeString(tempDir.resolve("Class1.java"), class1);
        Files.writeString(tempDir.resolve("Class2.java"), class2);

        JavaExtractor extractor = new JavaExtractor();
        List<MetadataArtifact> artifacts = extractor.extractFromDirectory(tempDir);

        // Should extract artifacts from both files
        assertTrue(artifacts.size() >= 2);

        long class1Count = artifacts.stream()
                .filter(a -> a.getName().equals("Class1"))
                .count();
        long class2Count = artifacts.stream()
                .filter(a -> a.getName().equals("Class2"))
                .count();

        assertEquals(1, class1Count);
        assertEquals(1, class2Count);
    }
}
