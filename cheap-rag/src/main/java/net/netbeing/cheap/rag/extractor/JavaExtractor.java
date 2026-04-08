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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import net.netbeing.cheap.rag.extractor.model.MetadataArtifact;
import net.netbeing.cheap.rag.extractor.visitor.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Main extractor class for parsing Java source code and extracting metadata artifacts.
 */
public class JavaExtractor
{
    private static final Logger logger = LoggerFactory.getLogger(JavaExtractor.class);

    private final JavaExtractorConfig config;
    private final JavaParser parser;

    public JavaExtractor()
    {
        this(JavaExtractorConfig.builder().build());
    }

    public JavaExtractor(@NotNull JavaExtractorConfig config)
    {
        this.config = config;
        this.parser = new JavaParser(
                new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25));
    }

    /**
     * Extracts metadata artifacts from a single Java source file.
     *
     * @param sourceFile Path to the Java source file
     * @return List of extracted metadata artifacts
     * @throws IOException If file cannot be read
     */
    @NotNull
    public List<MetadataArtifact> extractFromFile(@NotNull Path sourceFile) throws IOException
    {
        if (!Files.exists(sourceFile)) {
            throw new IOException("Source file not found: " + sourceFile);
        }

        if (!sourceFile.toString().endsWith(".java")) {
            logger.warn("Skipping non-Java file: {}", sourceFile);
            return List.of();
        }

        logger.debug("Parsing file: {}", sourceFile);

        ParseResult<CompilationUnit> parseResult = parser.parse(sourceFile);

        if (!parseResult.isSuccessful()) {
            logger.warn("Failed to parse file: {}. Errors: {}", sourceFile, parseResult.getProblems());
            return List.of();
        }

        CompilationUnit cu = parseResult.getResult()
                .orElseThrow(() -> new IOException("No compilation unit found for: " + sourceFile));

        List<MetadataArtifact> artifacts = new ArrayList<>();

        // Apply visitors based on configuration
        String sourceFilePath = sourceFile.toString();

        if (config.isExtractClasses()) {
            cu.accept(new ClassExtractorVisitor(sourceFilePath, config), artifacts);
        }

        if (config.isExtractInterfaces()) {
            cu.accept(new InterfaceExtractorVisitor(sourceFilePath, config), artifacts);
        }

        if (config.isExtractEnums()) {
            cu.accept(new EnumExtractorVisitor(sourceFilePath, config), artifacts);
        }

        if (config.isExtractMethods()) {
            cu.accept(new MethodExtractorVisitor(sourceFilePath, config), artifacts);
        }

        if (config.isExtractFields()) {
            cu.accept(new FieldExtractorVisitor(sourceFilePath, config), artifacts);
        }

        logger.debug("Extracted {} artifacts from {}", artifacts.size(), sourceFile);

        return artifacts;
    }

    /**
     * Extracts metadata artifacts from a directory recursively.
     *
     * @param sourceDir Path to the directory containing Java source files
     * @return List of extracted metadata artifacts from all Java files
     * @throws IOException If directory cannot be read
     */
    @NotNull
    public List<MetadataArtifact> extractFromDirectory(@NotNull Path sourceDir) throws IOException
    {
        if (!Files.exists(sourceDir)) {
            throw new IOException("Source directory not found: " + sourceDir);
        }

        if (!Files.isDirectory(sourceDir)) {
            throw new IOException("Not a directory: " + sourceDir);
        }

        logger.info("Extracting from directory: {}", sourceDir);

        List<MetadataArtifact> allArtifacts = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            List<MetadataArtifact> artifacts = extractFromFile(path);
                            allArtifacts.addAll(artifacts);
                        } catch (IOException e) {
                            logger.error("Error extracting from file: " + path, e);
                        }
                    });
        }

        logger.info("Extracted {} total artifacts from {}", allArtifacts.size(), sourceDir);

        return allArtifacts;
    }

    /**
     * Extracts metadata artifacts from a file or directory.
     *
     * @param sourcePath Path to a Java file or directory
     * @return List of extracted metadata artifacts
     * @throws IOException If path cannot be read
     */
    @NotNull
    public List<MetadataArtifact> extract(@NotNull Path sourcePath) throws IOException
    {
        if (Files.isDirectory(sourcePath)) {
            return extractFromDirectory(sourcePath);
        } else {
            return extractFromFile(sourcePath);
        }
    }
}
