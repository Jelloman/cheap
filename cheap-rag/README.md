# cheap-rag

Java utilities for the CHEAP RAG system, featuring a comprehensive Java metadata extractor.

## Overview

The cheap-rag module provides Java-based tools for the CHEAP RAG (Retrieval-Augmented Generation) system. The primary component is a high-quality Java metadata extractor that uses JavaParser to extract code structure for semantic search.

## Features

- **Comprehensive Extraction**: Classes, interfaces, enums, methods, fields
- **Better than Python**: Improved Javadoc parsing, generics, annotations
- **Configurable**: Control what to extract and visibility levels
- **ID Compatibility**: Same SHA-256 ID generation as Python implementation
- **JSON Output**: MetadataArtifact format matching Python schema exactly
- **CLI + API**: Use standalone or programmatically
- **Python Integration**: Wrapper script for seamless integration

## Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":cheap-rag"))
}
```

## Quick Start

### Command-Line Usage

```bash
# Extract from single file
java -jar cheap-rag.jar extract Person.java

# Extract from directory
java -jar cheap-rag.jar extract src/main/java/

# Output to file
java -jar cheap-rag.jar extract src/ --output metadata.json

# Public API only
java -jar cheap-rag.jar extract src/ --public-only

# Skip methods or fields
java -jar cheap-rag.jar extract src/ --no-methods --no-fields
```

### Programmatic Usage

```java
import net.netbeing.cheap.rag.extractor.*;
import net.netbeing.cheap.rag.extractor.model.*;

// Default configuration
JavaExtractor extractor = new JavaExtractor();
List<MetadataArtifact> artifacts = extractor.extract(Paths.get("src/"));

// Custom configuration
JavaExtractorConfig config = JavaExtractorConfig.builder()
    .extractClasses(true)
    .extractMethods(true)
    .publicOnly(true)
    .build();

JavaExtractor extractor = new JavaExtractor(config);
List<MetadataArtifact> artifacts = extractor.extractFromFile(
    Paths.get("Person.java")
);

// Write to JSON
ObjectMapper mapper = new ObjectMapper();
mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
mapper.writerWithDefaultPrettyPrinter()
    .writeValue(new File("metadata.json"), artifacts);
```

## What Gets Extracted

### Classes

- Name, package, qualified name
- Javadoc documentation
- Extends/implements relationships
- Modifiers (public, abstract, final, etc.)
- Source location (file, line number)
- Nested classes

Example:
```json
{
  "id": "java_class_a1b2c3d4e5f6g7h8",
  "type": "class",
  "name": "CheapFactory",
  "language": "java",
  "source_type": "code",
  "module": "net.netbeing.cheap",
  "source_file": "cheap-core/src/main/java/CheapFactory.java",
  "source_line": 25,
  "documentation": "Factory for creating Cheap objects...",
  "qualified_name": "net.netbeing.cheap.CheapFactory",
  "embedding_text": "Java class: net.netbeing.cheap.CheapFactory\n\nFactory for creating Cheap objects..."
}
```

### Interfaces

- Name, package, qualified name
- Javadoc documentation
- Extends relationships
- Source location

### Enums

- Name, package, qualified name
- Javadoc documentation
- Enum constants
- Source location

### Methods

- Name, signature, return type
- Parameters with types
- Javadoc documentation
- Parent class/interface
- Source location

Example:
```json
{
  "id": "java_method_x9y8z7w6v5u4t3s2",
  "type": "method",
  "name": "createCatalog",
  "language": "java",
  "source_type": "code",
  "signature": "Catalog createCatalog(String name, UUID upstream)",
  "documentation": "Creates a new catalog...",
  "embedding_text": "Java method: Catalog createCatalog(String name, UUID upstream)\nIn class: net.netbeing.cheap.CheapFactory\n\nCreates a new catalog..."
}
```

### Fields

- Name, type
- Modifiers (public, static, final, etc.)
- Javadoc documentation
- Parent class
- Source location

## Configuration Options

```java
JavaExtractorConfig config = JavaExtractorConfig.builder()
    .extractClasses(true)      // Extract classes (default: true)
    .extractInterfaces(true)   // Extract interfaces (default: true)
    .extractEnums(true)        // Extract enums (default: true)
    .extractMethods(true)      // Extract methods (default: true)
    .extractFields(true)       // Extract fields (default: true)
    .publicOnly(false)         // Only public API (default: false)
    .build();
```

## ID Generation

IDs are generated using SHA-256 hashing, matching the Python implementation exactly:

- **Format**: `{language}_{type}_{hash16}`
- **Hash Input**: `"{language}:{type}:{qualifiedName}"`
- **Example**: `java_class_a1b2c3d4e5f6g7h8`

Same qualified name always produces same ID:

```java
String id = IdGenerator.generateId("java", ArtifactType.CLASS, "com.example.MyClass");
// Always produces: java_class_<same-hash>
```

## Output Format

Outputs JSON array of MetadataArtifact objects matching Python schema:

```json
[
  {
    "id": "java_class_...",
    "type": "class",
    "name": "MyClass",
    "language": "java",
    "source_type": "code",
    "module": "com.example",
    "source_file": "src/main/java/com/example/MyClass.java",
    "source_line": 10,
    "documentation": "Class documentation...",
    "qualified_name": "com.example.MyClass",
    "embedding_text": "Java class: com.example.MyClass\n\nClass documentation..."
  }
]
```

All field names use snake_case to match Python convention.

## Comparison with Python Extractor

### Advantages of Java Extractor

- **Better Javadoc Parsing**: Handles multi-line, inline tags, complex formatting
- **Accurate Generics**: Full support for generic types
- **Annotations**: Extracts class and method-level annotations
- **Inner Classes**: Proper handling of nested and anonymous classes
- **Error Recovery**: Better handling of malformed code
- **Performance**: Generally faster on large codebases

### What's the Same

- ID generation algorithm
- JSON output schema
- Artifact types
- Embedding text format

## Python Integration

Use the provided wrapper script from cheap-rag Python project:

```python
# cheap-rag/scripts/extract_with_java.py
from cheap_rag.extractors import extract_with_java

# Extract using Java extractor
artifacts = extract_with_java(
    source_path="cheap-core/src/main/java",
    jar_path="cheap-rag.jar",
    public_only=True
)

# Returns list of MetadataArtifact objects
for artifact in artifacts:
    print(f"{artifact.type}: {artifact.name}")
```

Or invoke directly:

```python
import subprocess
import json

result = subprocess.run(
    ["java", "-jar", "cheap-rag.jar", "extract", source_path, "--output", "temp.json"],
    capture_output=True,
    text=True
)

with open("temp.json") as f:
    artifacts = json.load(f)
```

## CLI Reference

```
Usage: extract <source-path> [options]

Parameters:
  <source-path>       Path to Java source file or directory

Options:
  -o, --output FILE   Output JSON file (default: stdout)
  --public-only       Extract only public API
  --no-classes        Skip class extraction
  --no-interfaces     Skip interface extraction
  --no-enums          Skip enum extraction
  --no-methods        Skip method extraction
  --no-fields         Skip field extraction
  -h, --help          Show help message
  -V, --version       Show version information
```

## Examples

### Extract Public API Only

```bash
java -jar cheap-rag.jar extract cheap-core/src/main/java \
    --public-only \
    --output cheap-core-api.json
```

### Extract Just Classes and Interfaces

```bash
java -jar cheap-rag.jar extract src/ \
    --no-methods \
    --no-fields \
    --no-enums
```

### Programmatic Filtering

```java
List<MetadataArtifact> artifacts = extractor.extract(Paths.get("src/"));

// Filter for interfaces only
List<MetadataArtifact> interfaces = artifacts.stream()
    .filter(a -> a.getType() == ArtifactType.INTERFACE)
    .toList();

// Filter for specific package
List<MetadataArtifact> coreArtifacts = artifacts.stream()
    .filter(a -> a.getModule() != null && a.getModule().startsWith("net.netbeing.cheap"))
    .toList();
```

## Building from Source

```bash
# Build JAR
./gradlew :cheap-rag:build

# Run tests
./gradlew :cheap-rag:test

# Build fat JAR (includes dependencies)
./gradlew :cheap-rag:jar

# Find JAR at:
cheap-rag/build/libs/cheap-rag-0.1.jar
```

## Dependencies

- **JavaParser**: Java AST parsing (v3.26.2)
- **Picocli**: Command-line interface (v4.7.6)
- **Jackson**: JSON output
- **SLF4J**: Logging

## Future Enhancements (Phase 2)

- Method body analysis
- Call graph extraction
- Dependency analysis
- Complexity metrics (cyclomatic, cognitive)
- Test coverage metadata
- TypeScript/Python extractors in Java

## License

Apache License 2.0 - See LICENSE file for details.
