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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import net.netbeing.cheap.rag.extractor.model.MetadataArtifact;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command-line interface for the Java metadata extractor.
 */
@Command(
        name = "extract",
        description = "Extracts metadata from Java source code",
        mixinStandardHelpOptions = true,
        version = "0.1"
)
public class ExtractorCli implements Callable<Integer>
{
    @Parameters(
            index = "0",
            description = "Path to Java source file or directory"
    )
    private File sourcePath;

    @Option(
            names = {"-o", "--output"},
            description = "Output JSON file (default: stdout)"
    )
    private File outputFile;

    @Option(
            names = {"--public-only"},
            description = "Extract only public API (skip private members)"
    )
    private boolean publicOnly;

    @Option(
            names = {"--no-classes"},
            description = "Skip class extraction"
    )
    private boolean noClasses;

    @Option(
            names = {"--no-interfaces"},
            description = "Skip interface extraction"
    )
    private boolean noInterfaces;

    @Option(
            names = {"--no-enums"},
            description = "Skip enum extraction"
    )
    private boolean noEnums;

    @Option(
            names = {"--no-methods"},
            description = "Skip method extraction"
    )
    private boolean noMethods;

    @Option(
            names = {"--no-fields"},
            description = "Skip field extraction"
    )
    private boolean noFields;

    @Override
    public Integer call() throws Exception
    {
        // Build configuration
        JavaExtractorConfig config = JavaExtractorConfig.builder()
                .extractClasses(!noClasses)
                .extractInterfaces(!noInterfaces)
                .extractEnums(!noEnums)
                .extractMethods(!noMethods)
                .extractFields(!noFields)
                .publicOnly(publicOnly)
                .build();

        // Create extractor
        JavaExtractor extractor = new JavaExtractor(config);

        // Extract artifacts
        Path source = Paths.get(sourcePath.getAbsolutePath());
        List<MetadataArtifact> artifacts;

        try {
            artifacts = extractor.extract(source);
        } catch (IOException e) {
            System.err.println("Error extracting from source: " + e.getMessage());
            return 1;
        }

        // Configure JSON mapper with snake_case
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        // Write output
        try {
            if (outputFile != null) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, artifacts);
                System.out.println("Extracted " + artifacts.size() + " artifacts to " + outputFile);
            } else {
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(artifacts);
                System.out.println(json);
            }
        } catch (IOException e) {
            System.err.println("Error writing output: " + e.getMessage());
            return 1;
        }

        return 0;
    }

    public static void main(String[] args)
    {
        int exitCode = new CommandLine(new ExtractorCli()).execute(args);
        System.exit(exitCode);
    }
}
