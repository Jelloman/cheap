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

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import net.netbeing.cheap.rag.extractor.JavaExtractorConfig;
import net.netbeing.cheap.rag.extractor.model.ArtifactType;
import net.netbeing.cheap.rag.extractor.model.MetadataArtifact;
import net.netbeing.cheap.rag.extractor.model.SourceType;
import net.netbeing.cheap.rag.util.IdGenerator;

import java.util.List;

/**
 * Visitor for extracting field declarations from Java code.
 */
public class FieldExtractorVisitor extends VoidVisitorAdapter<List<MetadataArtifact>>
{
    private final String sourceFile;
    private final JavaExtractorConfig config;

    public FieldExtractorVisitor(String sourceFile, JavaExtractorConfig config)
    {
        this.sourceFile = sourceFile;
        this.config = config;
    }

    @Override
    public void visit(FieldDeclaration declaration, List<MetadataArtifact> artifacts)
    {
        // Skip fields if disabled in config
        if (!config.isExtractFields()) {
            super.visit(declaration, artifacts);
            return;
        }

        // Skip private fields if configured
        if (config.isPublicOnly() && declaration.isPrivate()) {
            super.visit(declaration, artifacts);
            return;
        }

        // Try to get containing class/interface
        @SuppressWarnings("unchecked")
        String parentName = declaration.findAncestor(
                (Class<com.github.javaparser.ast.body.TypeDeclaration<?>>) (Class<?>) com.github.javaparser.ast.body.TypeDeclaration.class)
                .flatMap(type -> type.getFullyQualifiedName())
                .orElse("Unknown");

        // Extract documentation
        String documentation = declaration.getJavadoc()
                .map(javadoc -> javadoc.toText())
                .orElse(null);

        // A field declaration can declare multiple variables (e.g., int x, y, z)
        for (VariableDeclarator variable : declaration.getVariables()) {
            String fieldName = variable.getNameAsString();
            String fieldType = variable.getTypeAsString();
            String qualifiedName = parentName + "." + fieldName;

            // Build embedding text
            StringBuilder embeddingText = new StringBuilder();
            embeddingText.append("Java field: ").append(fieldType).append(" ").append(fieldName);
            embeddingText.append("\nIn class: ").append(parentName);
            if (documentation != null) {
                embeddingText.append("\n\n").append(documentation);
            }

            // Generate ID
            String id = IdGenerator.generateId("java", ArtifactType.FIELD, qualifiedName);

            // Build artifact
            MetadataArtifact artifact = MetadataArtifact.builder()
                    .id(id)
                    .type(ArtifactType.FIELD)
                    .name(fieldName)
                    .language("java")
                    .sourceType(SourceType.CODE)
                    .module(getPackageName(parentName))
                    .sourceFile(sourceFile)
                    .sourceLine(declaration.getBegin().map(pos -> pos.line).orElse(null))
                    .documentation(documentation)
                    .signature(fieldType + " " + fieldName)
                    .qualifiedName(qualifiedName)
                    .embeddingText(embeddingText.toString())
                    .build();

            artifacts.add(artifact);
        }

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
