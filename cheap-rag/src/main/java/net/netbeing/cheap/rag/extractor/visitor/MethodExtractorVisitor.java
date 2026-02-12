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

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import net.netbeing.cheap.rag.extractor.JavaExtractorConfig;
import net.netbeing.cheap.rag.extractor.model.ArtifactType;
import net.netbeing.cheap.rag.extractor.model.MetadataArtifact;
import net.netbeing.cheap.rag.extractor.model.SourceType;
import net.netbeing.cheap.rag.util.IdGenerator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Visitor for extracting method declarations from Java code.
 */
public class MethodExtractorVisitor extends VoidVisitorAdapter<List<MetadataArtifact>>
{
    private final String sourceFile;
    private final JavaExtractorConfig config;

    public MethodExtractorVisitor(String sourceFile, JavaExtractorConfig config)
    {
        this.sourceFile = sourceFile;
        this.config = config;
    }

    @Override
    public void visit(MethodDeclaration declaration, List<MetadataArtifact> artifacts)
    {
        // Skip methods if disabled in config
        if (!config.isExtractMethods()) {
            super.visit(declaration, artifacts);
            return;
        }

        // Skip private methods if configured
        if (config.isPublicOnly() && declaration.isPrivate()) {
            super.visit(declaration, artifacts);
            return;
        }

        String methodName = declaration.getNameAsString();
        String returnType = declaration.getTypeAsString();

        // Build signature
        String parameters = declaration.getParameters().stream()
                .map(param -> param.getTypeAsString() + " " + param.getNameAsString())
                .collect(Collectors.joining(", "));
        String signature = String.format("%s %s(%s)", returnType, methodName, parameters);

        // Try to get containing class/interface
        @SuppressWarnings("unchecked")
        String parentName = declaration.findAncestor(
                (Class<com.github.javaparser.ast.body.TypeDeclaration<?>>) (Class<?>) com.github.javaparser.ast.body.TypeDeclaration.class)
                .flatMap(type -> type.getFullyQualifiedName())
                .orElse("Unknown");

        String qualifiedName = parentName + "." + methodName;

        // Extract documentation
        String documentation = declaration.getJavadoc()
                .map(javadoc -> javadoc.toText())
                .orElse(null);

        // Build embedding text
        StringBuilder embeddingText = new StringBuilder();
        embeddingText.append("Java method: ").append(signature);
        embeddingText.append("\nIn class: ").append(parentName);
        if (documentation != null) {
            embeddingText.append("\n\n").append(documentation);
        }

        // Generate ID
        String id = IdGenerator.generateId("java", ArtifactType.METHOD, qualifiedName + ":" + parameters);

        // Build artifact
        MetadataArtifact artifact = MetadataArtifact.builder()
                .id(id)
                .type(ArtifactType.METHOD)
                .name(methodName)
                .language("java")
                .sourceType(SourceType.CODE)
                .module(getPackageName(parentName))
                .sourceFile(sourceFile)
                .sourceLine(declaration.getBegin().map(pos -> pos.line).orElse(null))
                .documentation(documentation)
                .signature(signature)
                .qualifiedName(qualifiedName)
                .embeddingText(embeddingText.toString())
                .build();

        artifacts.add(artifact);

        super.visit(declaration, artifacts);
    }

    private String getPackageName(String qualifiedName)
    {
        if (qualifiedName.contains(".")) {
            return qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
        }
        return null;
    }
}
