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

package net.netbeing.cheap.rag.extractor.visitor;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import net.netbeing.cheap.rag.extractor.JavaExtractorConfig;
import net.netbeing.cheap.rag.extractor.model.ArtifactType;
import net.netbeing.cheap.rag.extractor.model.MetadataArtifact;
import net.netbeing.cheap.rag.extractor.model.SourceType;
import net.netbeing.cheap.rag.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor for extracting interface declarations from Java code.
 */
public class InterfaceExtractorVisitor extends VoidVisitorAdapter<List<MetadataArtifact>>
{
    private final String sourceFile;
    private final JavaExtractorConfig config;

    public InterfaceExtractorVisitor(String sourceFile, JavaExtractorConfig config)
    {
        this.sourceFile = sourceFile;
        this.config = config;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration declaration, List<MetadataArtifact> artifacts)
    {
        // Only process interfaces
        if (!declaration.isInterface()) {
            super.visit(declaration, artifacts);
            return;
        }

        // Skip private interfaces if configured
        if (config.isPublicOnly() && declaration.isPrivate()) {
            super.visit(declaration, artifacts);
            return;
        }

        String interfaceName = declaration.getNameAsString();
        String qualifiedName = declaration.getFullyQualifiedName().orElse(interfaceName);
        String packageName = getPackageName(qualifiedName, interfaceName);

        // Extract documentation
        String documentation = declaration.getJavadoc()
                .map(javadoc -> javadoc.toText())
                .orElse(null);

        // Build embedding text
        StringBuilder embeddingText = new StringBuilder();
        embeddingText.append("Java interface: ").append(qualifiedName);
        if (documentation != null) {
            embeddingText.append("\n\n").append(documentation);
        }

        // Extract extends
        List<String> extendsTypes = new ArrayList<>();
        declaration.getExtendedTypes().forEach(type -> extendsTypes.add(type.getNameAsString()));

        if (!extendsTypes.isEmpty()) {
            embeddingText.append("\nExtends: ").append(String.join(", ", extendsTypes));
        }

        // Generate ID
        String id = IdGenerator.generateId("java", ArtifactType.INTERFACE, qualifiedName);

        // Build artifact
        MetadataArtifact artifact = MetadataArtifact.builder()
                .id(id)
                .type(ArtifactType.INTERFACE)
                .name(interfaceName)
                .language("java")
                .sourceType(SourceType.CODE)
                .module(packageName)
                .sourceFile(sourceFile)
                .sourceLine(declaration.getBegin().map(pos -> pos.line).orElse(null))
                .documentation(documentation)
                .qualifiedName(qualifiedName)
                .embeddingText(embeddingText.toString())
                .build();

        artifacts.add(artifact);

        // Continue visiting nested interfaces
        super.visit(declaration, artifacts);
    }

    private String getPackageName(String qualifiedName, String interfaceName)
    {
        if (qualifiedName.contains(".")) {
            return qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
        }
        return null;
    }
}
