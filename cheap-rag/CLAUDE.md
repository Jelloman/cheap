# cheap-rag Module

This file provides guidance to Claude Code when working with the cheap-rag module.

## Module Overview

The cheap-rag module provides Java utilities for the CHEAP RAG system. The primary component is a comprehensive Java metadata extractor that uses JavaParser to extract code structure, generating MetadataArtifact objects that match the Python schema exactly.

## Key Design Principles

1. **Python Compatibility**: JSON output must match Python MetadataArtifact schema exactly
2. **ID Consistency**: SHA-256 ID generation must match Python implementation
3. **Dual Interface**: Both CLI and programmatic API
4. **Quality Over Python**: Better Javadoc parsing, generics, annotations than javalang
5. **Configurable Extraction**: User controls what gets extracted

## Package Structure

```
net.netbeing.cheap.rag/
├── extractor/
│   ├── JavaExtractor.java           # Main extractor class
│   ├── JavaExtractorConfig.java     # Configuration
│   ├── ExtractorCli.java            # CLI with picocli
│   ├── model/
│   │   ├── MetadataArtifact.java    # Matches Python schema
│   │   ├── ArtifactType.java        # Enum
│   │   └── SourceType.java          # Enum
│   └── visitor/
│       ├── ClassExtractorVisitor.java
│       ├── InterfaceExtractorVisitor.java
│       ├── EnumExtractorVisitor.java
│       ├── MethodExtractorVisitor.java
│       └── FieldExtractorVisitor.java
└── util/
    └── IdGenerator.java             # SHA-256 ID generation
```

## Development Guidelines

### MetadataArtifact Schema Compatibility

**Critical**: Must match Python schema exactly, including field names and snake_case:

```java
@JsonProperty("source_file")
String sourceFile;

@JsonProperty("source_line")
Integer sourceLine;

@JsonProperty("embedding_text")
String embeddingText;
```

Configure Jackson mapper:
```java
ObjectMapper mapper = new ObjectMapper();
mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
```

### ID Generation Algorithm

Must match Python implementation:

```python
# Python version
hash_input = f"{language}:{type}:{qualified_name}"
hash_hex = hashlib.sha256(hash_input.encode()).hexdigest()
artifact_id = f"{language}_{type}_{hash_hex[:16]}"
```

Java version:

```java
String hashInput = String.format("%s:%s:%s", language, type.getValue(), qualifiedName);
String hash = sha256(hashInput);
String hash16 = hash.substring(0, 16);
return String.format("%s_%s_%s", language, type.getValue(), hash16);
```

**Test this carefully** - same qualified name must produce same ID in both Python and Java.

### Visitor Pattern for Extraction

Use JavaParser's VoidVisitorAdapter:

```java
public class ClassExtractorVisitor extends VoidVisitorAdapter<List<MetadataArtifact>>
{
    @Override
    public void visit(ClassOrInterfaceDeclaration declaration, List<MetadataArtifact> artifacts)
    {
        // Extract class info
        MetadataArtifact artifact = MetadataArtifact.builder()
            .id(id)
            .type(ArtifactType.CLASS)
            .name(className)
            .build();

        artifacts.add(artifact);

        // Continue visiting nested classes
        super.visit(declaration, artifacts);
    }
}
```

**Key points**:
- Always call `super.visit()` to handle nested declarations
- Check config before extracting (publicOnly, extract flags)
- Build complete embedding_text for semantic search

### Embedding Text Format

Format embedding text for optimal vector search:

```java
StringBuilder embeddingText = new StringBuilder();
embeddingText.append("Java class: ").append(qualifiedName);

if (documentation != null) {
    embeddingText.append("\n\n").append(documentation);
}

if (!extendsTypes.isEmpty()) {
    embeddingText.append("\nExtends: ").append(String.join(", ", extendsTypes));
}
```

**Purpose**: LLM will embed this text, so include:
- Artifact type and name
- Documentation
- Key relationships (extends, implements)
- Signature (for methods)

### Configuration Pattern

Use builder pattern with sensible defaults:

```java
@Value
@Builder
public class JavaExtractorConfig
{
    @Builder.Default
    boolean extractClasses = true;

    @Builder.Default
    boolean extractMethods = true;

    @Builder.Default
    boolean publicOnly = false;
}
```

### CLI Implementation

Use Picocli for clean CLI:

```java
@Command(
    name = "extract",
    description = "Extracts metadata from Java source code",
    mixinStandardHelpOptions = true
)
public class ExtractorCli implements Callable<Integer>
{
    @Parameters(index = "0")
    private File sourcePath;

    @Option(names = {"-o", "--output"})
    private File outputFile;

    @Override
    public Integer call() throws Exception {
        // Implementation
        return 0; // Success
    }
}
```

**Exit codes**:
- 0: Success
- 1: Error

**Output**:
- If --output specified: Write to file
- Otherwise: Write to stdout

### Error Handling

Log errors but continue processing other files:

```java
try (Stream<Path> paths = Files.walk(sourceDir)) {
    paths.filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".java"))
        .forEach(path -> {
            try {
                List<MetadataArtifact> artifacts = extractFromFile(path);
                allArtifacts.addAll(artifacts);
            } catch (IOException e) {
                logger.error("Error extracting from file: " + path, e);
                // Continue with other files
            }
        });
}
```

**Parse failures**: Log warning, skip file, continue

## Testing Guidelines

### Test ID Generation

```java
@Test
void testIdMatchesPython() {
    // Known Python result for this input
    String id = IdGenerator.generateId("java", ArtifactType.CLASS, "com.example.MyClass");

    // Verify format
    assertTrue(id.startsWith("java_class_"));
    assertEquals(28, id.length());

    // Verify stability
    String id2 = IdGenerator.generateId("java", ArtifactType.CLASS, "com.example.MyClass");
    assertEquals(id, id2);
}
```

### Test Extraction with @TempDir

```java
@Test
void testExtractClass(@TempDir Path tempDir) throws IOException {
    String javaCode = """
        package com.example;
        public class TestClass {
            public void testMethod() {}
        }
        """;

    Path javaFile = tempDir.resolve("TestClass.java");
    Files.writeString(javaFile, javaCode);

    JavaExtractor extractor = new JavaExtractor();
    List<MetadataArtifact> artifacts = extractor.extractFromFile(javaFile);

    assertEquals(2, artifacts.size()); // class + method
}
```

### Test JSON Output Format

```java
@Test
void testJsonOutputFormat() throws IOException {
    MetadataArtifact artifact = MetadataArtifact.builder()
        .id("java_class_test123")
        .type(ArtifactType.CLASS)
        .name("TestClass")
        .language("java")
        .sourceType(SourceType.CODE)
        .module("com.example")
        .sourceFile("Test.java")
        .sourceLine(10)
        .build();

    ObjectMapper mapper = new ObjectMapper();
    mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    String json = mapper.writeValueAsString(artifact);

    assertTrue(json.contains("\"source_file\""));
    assertTrue(json.contains("\"source_line\""));
    assertTrue(json.contains("\"source_type\""));
}
```

### Test Configuration

```java
@Test
void testPublicOnlyConfig(@TempDir Path tempDir) throws IOException {
    String javaCode = """
        public class TestClass {
            public String publicField;
            private String privateField;
        }
        """;

    Path javaFile = tempDir.resolve("Test.java");
    Files.writeString(javaFile, javaCode);

    JavaExtractorConfig config = JavaExtractorConfig.builder()
        .publicOnly(true)
        .build();

    JavaExtractor extractor = new JavaExtractor(config);
    List<MetadataArtifact> artifacts = extractor.extractFromFile(javaFile);

    // Should only have public members
    boolean hasPrivate = artifacts.stream()
        .anyMatch(a -> a.getName().contains("private"));
    assertFalse(hasPrivate);
}
```

## Common Tasks

### Adding a New Visitor

1. Create visitor class extending `VoidVisitorAdapter<List<MetadataArtifact>>`

2. Override `visit()` method for target AST node:
   ```java
   @Override
   public void visit(AnnotationDeclaration declaration, List<MetadataArtifact> artifacts)
   {
       // Extract annotation info
       // Build MetadataArtifact
       // Add to artifacts list
       super.visit(declaration, artifacts);
   }
   ```

3. Add config option:
   ```java
   @Builder.Default
   boolean extractAnnotations = true;
   ```

4. Apply visitor in JavaExtractor:
   ```java
   if (config.isExtractAnnotations()) {
       cu.accept(new AnnotationExtractorVisitor(sourceFilePath, config), artifacts);
   }
   ```

5. Add tests

### Adding a New Artifact Type

1. Add to ArtifactType enum:
   ```java
   ANNOTATION("annotation");
   ```

2. Create visitor for that type

3. Update IdGenerator if needed (usually no change)

4. Add tests

### Adding Metadata Fields

1. Add to MetadataArtifact:
   ```java
   @Nullable
   @JsonProperty("return_type")
   String returnType;
   ```

2. Extract in visitors:
   ```java
   .returnType(method.getReturnType().asString())
   ```

3. Update tests

4. **Verify**: Must coordinate with Python schema changes

## Python Integration

### Wrapper Script Pattern

Create `cheap-rag/scripts/extract_with_java.py`:

```python
import subprocess
import json
from pathlib import Path
from cheap_rag.models import MetadataArtifact

def extract_with_java(source_path: str, jar_path: str, **options):
    cmd = ["java", "-jar", jar_path, "extract", source_path]

    if options.get("public_only"):
        cmd.append("--public-only")

    result = subprocess.run(cmd, capture_output=True, text=True, check=True)
    artifacts_data = json.loads(result.stdout)

    return [MetadataArtifact.from_dict(d) for d in artifacts_data]
```

### Integration in Python Pipeline

```python
# In cheap-rag Python extractor factory
if language == "java" and use_java_extractor:
    artifacts = extract_with_java(
        source_path=source_path,
        jar_path=config.java_extractor_jar,
        public_only=True
    )
else:
    # Fall back to Python extractor
    artifacts = python_java_extractor.extract(source_path)
```

## Performance Considerations

### File Walking

Use Files.walk() for recursive directory traversal:

```java
try (Stream<Path> paths = Files.walk(sourceDir)) {
    paths.filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".java"))
        .forEach(this::processFile);
}
```

**Note**: Stream is AutoCloseable, use try-with-resources

### Parser Reuse

JavaParser instance can be reused:

```java
private final JavaParser parser = new JavaParser();

public List<MetadataArtifact> extractFromFile(Path file) {
    ParseResult<CompilationUnit> result = parser.parse(file);
    // Process...
}
```

### Logging

Use SLF4J with appropriate levels:
- **DEBUG**: Per-file parsing details
- **INFO**: Directory-level progress
- **WARN**: Parse failures, skipped files
- **ERROR**: Fatal errors only

## Build Configuration

### Fat JAR for CLI

Configure Gradle to create fat JAR:

```kotlin
tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "net.netbeing.cheap.rag.extractor.ExtractorCli"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })
}
```

### Application Plugin

```kotlin
application {
    mainClass = "net.netbeing.cheap.rag.extractor.ExtractorCli"
}
```

Enables: `./gradlew :cheap-rag:run --args='extract src/'`

## Dependencies

- **JavaParser** (com.github.javaparser:javaparser-core:3.26.2): AST parsing
- **Picocli** (info.picocli:picocli:4.7.6): CLI framework
- **Jackson**: JSON output
- **SLF4J + Logback**: Logging

## Future Enhancements (Phase 2)

### Method Body Analysis

Extract method calls, variable usage:

```java
public class MethodBodyVisitor extends VoidVisitorAdapter<MethodContext>
{
    @Override
    public void visit(MethodCallExpr call, MethodContext context) {
        context.addMethodCall(call.getNameAsString());
        super.visit(call, context);
    }
}
```

### Call Graph Extraction

Build caller → callee relationships:

```java
@JsonProperty("calls")
List<String> methodCalls;

@JsonProperty("called_by")
List<String> callers;
```

### Complexity Metrics

Calculate cyclomatic complexity:

```java
@JsonProperty("cyclomatic_complexity")
Integer cyclomaticComplexity;
```

## Related Modules

- `cheap-rag-client` - Java client for cheap-rag API
- `cheap-core` - CHEAP domain model (can be used as test input)

## Documentation

See [README.md](README.md) for CLI usage and API examples.
