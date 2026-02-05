# CHEAP Tags System - Implementation Plan (Java)

## Overview

This document details the implementation plan for adding a Tags system to the CHEAP framework. Tags provide lightweight, flexible semantic markers that can be attached to all five CHEAP elements (Catalogs, Hierarchies, Entities, Properties, and Aspects) without requiring schema changes.

The Tags system will be built entirely on top of the existing CHEAP data model, using Aspects and Properties for tag definitions and applications, all stored in Hierarchies within Catalogs.

## Architecture

### Core Design Principles

1. **CHEAP-Native Implementation**: Tags are implemented using existing CHEAP primitives (Aspects, Properties, Hierarchies)
2. **Entity-Based Storage**: Both tag definitions and tag applications are Aspects attached to Entities
3. **Reflection-Based Performance**: Use POJOs with reflection wrappers (`ImmutablePojoAspectDef`) for efficient field access
4. **Generic Interface Usage**: Maximize use of core CHEAP interfaces (`Entity`, `Aspect`, `Property`, etc.) for portability
5. **Catalog-Scoped**: Tag definitions and applications stored in AspectMapHierarchies within a Catalog

### Component Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Catalog                              │
├─────────────────────────────────────────────────────────┤
│  AspectMapHierarchy: "tag_definitions"                  │
│    Entity (UUID) → TagDefinitionAspect                  │
│                                                          │
│  AspectMapHierarchy: "tag_applications_property"        │
│  AspectMapHierarchy: "tag_applications_aspect"          │
│  AspectMapHierarchy: "tag_applications_hierarchy"       │
│  AspectMapHierarchy: "tag_applications_entity"          │
│  AspectMapHierarchy: "tag_applications_catalog"         │
│    Entity (UUID) → TagApplicationAspect                 │
│                                                          │
│  EntityDirectoryHierarchy: "tag_index_by_name"          │
│    "namespace:name" → Tag Entity                        │
│                                                          │
│  EntityDirectoryHierarchy: "tags_by_element"            │
│    Element UUID → List of Tag Application Entities      │
└─────────────────────────────────────────────────────────┘
```

## Data Model

### 1. Tag Definition

**Purpose**: Defines a tag type that can be applied to CHEAP elements.

**POJO Class**: `net.netbeing.cheap.tags.model.TagDefinition`

```java
public class TagDefinition {
    private String namespace;           // e.g., "cheap.core", "myapp.domain"
    private String name;                // e.g., "primary-key", "soft-delete"
    private String description;         // Human and LLM-readable explanation
    private List<ElementType> appliesTo; // Which elements can be tagged
    private TagScope scope;             // STANDARD or CUSTOM
    private List<String> aliases;       // Alternative names for search
    private List<UUID> parentTagIds;    // Tag inheritance (parent tag entities)

    // JavaBean getters (no setters - immutable)
}
```

**Aspect Definition**: Created via `ImmutablePojoAspectDef`
- Name: `"net.netbeing.cheap.tags.TagDefinition"`
- Properties derived from JavaBean introspection
- Stored in AspectMapHierarchy: `"tag_definitions"`

**Entity Representation**:
- Each tag definition is an Entity with a UUID
- TagDefinitionAspect attached to the entity
- Entity stored in tag_definitions hierarchy

### 2. Tag Application

**Purpose**: Records the application of a tag to a specific CHEAP element.

**POJO Class**: `net.netbeing.cheap.tags.model.TagApplication`

```java
public class TagApplication {
    private UUID tagDefinitionId;       // Reference to TagDefinition entity
    private UUID targetElementId;       // UUID of tagged element
    private ElementType targetType;     // Type of element being tagged
    private Map<String, Object> metadata; // Tag-specific attributes
    private TagSource source;           // EXPLICIT, INFERRED, or GENERATED
    private ZonedDateTime appliedAt;    // When tag was applied
    private String appliedBy;           // User/system that applied tag

    // JavaBean getters (no setters - immutable)
}
```

**Aspect Definition**: Created via `ImmutablePojoAspectDef`
- Name: `"net.netbeing.cheap.tags.TagApplication"`
- Properties derived from JavaBean introspection
- Stored in multiple AspectMapHierarchies (one per ElementType)

**Entity Representation**:
- Each tag application is an Entity with a UUID
- TagApplicationAspect attached to the entity
- Entity stored in appropriate tag_applications hierarchy

### 3. Supporting Enums

**ElementType**: `net.netbeing.cheap.tags.model.ElementType`
```java
public enum ElementType {
    PROPERTY,
    ASPECT,
    HIERARCHY,
    ENTITY,
    CATALOG
}
```

**TagScope**: `net.netbeing.cheap.tags.model.TagScope`
```java
public enum TagScope {
    STANDARD,   // Built-in CHEAP tags (cheap.core namespace)
    CUSTOM      // User-defined tags
}
```

**TagSource**: `net.netbeing.cheap.tags.model.TagSource`
```java
public enum TagSource {
    EXPLICIT,   // Manually applied by user
    INFERRED,   // Derived by system rules
    GENERATED   // Created by automated process
}
```

## Package Structure

```
net.netbeing.cheap.tags/
├── model/
│   ├── TagDefinition.java          (POJO)
│   ├── TagApplication.java         (POJO)
│   ├── ElementType.java            (enum)
│   ├── TagScope.java               (enum)
│   └── TagSource.java              (enum)
├── aspect/
│   ├── TagDefinitionAspect.java    (ImmutablePojoAspect wrapper)
│   └── TagApplicationAspect.java   (ImmutablePojoAspect wrapper)
├── registry/
│   ├── TagRegistry.java            (Main API interface)
│   └── TagRegistryImpl.java        (Implementation)
├── query/
│   ├── TagQuery.java               (Query builder)
│   └── TagQueryResult.java         (Query results)
├── validation/
│   ├── TagValidator.java           (Validation logic)
│   └── TagConflictDetector.java    (Detect conflicting tags)
└── standard/
    └── StandardTags.java           (cheap.core tag definitions)
```

## Core Components

### 1. TagRegistry

**Purpose**: Central API for creating, storing, retrieving, and querying tags.

**Interface**: `net.netbeing.cheap.tags.registry.TagRegistry`

```java
public interface TagRegistry {
    // Initialization
    static TagRegistry create(Catalog catalog, CheapFactory factory);
    Catalog catalog();

    // Tag Definition Management
    Entity defineTag(TagDefinition definition);
    TagDefinition getTagDefinition(UUID tagEntityId);
    TagDefinition getTagDefinitionByName(String namespace, String name);
    Collection<TagDefinition> getAllTagDefinitions();
    Collection<TagDefinition> getTagDefinitionsByNamespace(String namespace);

    // Tag Application
    Entity applyTag(UUID targetElementId, ElementType targetType,
                    UUID tagDefinitionId, Map<String, Object> metadata,
                    TagSource source);
    void removeTag(UUID tagApplicationId);

    // Tag Queries
    Collection<TagApplication> getTagsForElement(UUID elementId, ElementType type);
    Collection<UUID> getElementsByTag(UUID tagDefinitionId, ElementType type);
    Collection<UUID> getElementsByTagName(String namespace, String name, ElementType type);
    boolean hasTag(UUID elementId, ElementType type, UUID tagDefinitionId);

    // Tag Validation
    boolean isTagApplicable(UUID tagDefinitionId, ElementType targetType);
    Collection<String> validateTagApplication(UUID tagDefinitionId, UUID targetElementId,
                                             ElementType targetType);

    // Tag Inheritance
    Collection<UUID> getParentTags(UUID tagDefinitionId);
    Collection<UUID> getAllAncestorTags(UUID tagDefinitionId); // Transitive
    Collection<UUID> getChildTags(UUID tagDefinitionId);
    boolean inheritsFrom(UUID childTagId, UUID parentTagId);

    // Standard Tags
    void initializeStandardTags();
    Collection<TagDefinition> getStandardTags();
}
```

**Implementation Class**: `net.netbeing.cheap.tags.registry.TagRegistryImpl`

Key responsibilities:
- Manages AspectMapHierarchies for tag storage
- Maintains EntityDirectory indices for fast lookups
- Validates tag applications against definitions
- Enforces tag inheritance rules
- Initializes standard cheap.core tags

### 2. Aspect Wrappers

**TagDefinitionAspect**: Wraps TagDefinition POJO

```java
public class TagDefinitionAspect extends ImmutablePojoAspect<TagDefinition> {
    public static ImmutablePojoAspectDef aspectDef() {
        return new ImmutablePojoAspectDef(TagDefinition.class);
    }

    public TagDefinitionAspect(Entity entity, TagDefinition definition) {
        super(entity, aspectDef(), definition);
    }

    // Convenience accessors
    public String getNamespace() { return object().getNamespace(); }
    public String getName() { return object().getName(); }
    public String getFullName() { return getNamespace() + "." + getName(); }
    public List<ElementType> getAppliesTo() { return object().getAppliesTo(); }
}
```

**TagApplicationAspect**: Wraps TagApplication POJO

```java
public class TagApplicationAspect extends ImmutablePojoAspect<TagApplication> {
    public static ImmutablePojoAspectDef aspectDef() {
        return new ImmutablePojoAspectDef(TagApplication.class);
    }

    public TagApplicationAspect(Entity entity, TagApplication application) {
        super(entity, aspectDef(), application);
    }

    // Convenience accessors
    public UUID getTagDefinitionId() { return object().getTagDefinitionId(); }
    public UUID getTargetElementId() { return object().getTargetElementId(); }
    public ElementType getTargetType() { return object().getTargetType(); }
    public TagSource getSource() { return object().getSource(); }
}
```

### 3. Query Builder

**Purpose**: Fluent API for complex tag queries.

```java
public class TagQuery {
    private final TagRegistry registry;
    private ElementType targetType;
    private Set<UUID> includeTagIds = new HashSet<>();
    private Set<UUID> excludeTagIds = new HashSet<>();
    private Set<String> includeNamespaces = new HashSet<>();
    private Set<String> excludeNamespaces = new HashSet<>();
    private TagSource sourceFilter;
    private boolean includeInherited = true;

    // Builder methods
    public TagQuery forType(ElementType type);
    public TagQuery withTag(UUID tagId);
    public TagQuery withTagName(String namespace, String name);
    public TagQuery withoutTag(UUID tagId);
    public TagQuery inNamespace(String namespace);
    public TagQuery notInNamespace(String namespace);
    public TagQuery fromSource(TagSource source);
    public TagQuery includeInheritedTags(boolean include);

    // Execution
    public TagQueryResult execute();
}
```

### 4. Validation

**TagValidator**: Validates tag applications

```java
public class TagValidator {
    private final TagRegistry registry;

    public List<String> validateTagDefinition(TagDefinition definition);
    public List<String> validateTagApplication(UUID tagDefId, UUID targetId,
                                               ElementType targetType);
    public boolean isNamespaceValid(String namespace);
    public boolean isNameValid(String name);
    public boolean detectCircularInheritance(UUID tagId);
}
```

**TagConflictDetector**: Detects conflicting tags

```java
public class TagConflictDetector {
    private final TagRegistry registry;

    // Detect semantic conflicts (e.g., immutable + modified-timestamp)
    public List<String> detectConflicts(UUID elementId, ElementType type);

    // Pre-defined conflict rules
    private static final Map<String, Set<String>> CONFLICTING_TAGS = Map.of(
        "cheap.core.immutable", Set.of("cheap.core.modified-timestamp"),
        "cheap.core.required", Set.of("cheap.core.nullable")
    );
}
```

### 5. Standard Tags

**StandardTags**: Defines cheap.core namespace tags

```java
public class StandardTags {
    // Standard tag definitions from TAGS-DESIGN.md
    public static final TagDefinition PRIMARY_KEY = TagDefinition.builder()
        .namespace("cheap.core")
        .name("primary-key")
        .description("Primary identifier for entity")
        .appliesTo(ElementType.PROPERTY)
        .scope(TagScope.STANDARD)
        .build();

    // ... (60+ standard tags)

    public static Collection<TagDefinition> allStandardTags() {
        return List.of(PRIMARY_KEY, FOREIGN_KEY, ...);
    }
}
```

## Storage Strategy

### Hierarchies Created by TagRegistry

1. **tag_definitions** (AspectMapHierarchy)
   - Key: Tag definition Entity
   - Value: TagDefinitionAspect
   - Purpose: Store all tag definitions

2. **tag_applications_property** (AspectMapHierarchy)
   - Key: Tag application Entity
   - Value: TagApplicationAspect (for Properties)

3. **tag_applications_aspect** (AspectMapHierarchy)
   - Key: Tag application Entity
   - Value: TagApplicationAspect (for Aspects)

4. **tag_applications_hierarchy** (AspectMapHierarchy)
   - Key: Tag application Entity
   - Value: TagApplicationAspect (for Hierarchies)

5. **tag_applications_entity** (AspectMapHierarchy)
   - Key: Tag application Entity
   - Value: TagApplicationAspect (for Entities)

6. **tag_applications_catalog** (AspectMapHierarchy)
   - Key: Tag application Entity
   - Value: TagApplicationAspect (for Catalogs)

7. **tag_index_by_name** (EntityDirectoryHierarchy)
   - Key: "namespace:name" string
   - Value: Tag definition Entity
   - Purpose: Fast lookup by tag name

8. **tags_by_element** (EntityDirectoryHierarchy)
   - Key: Target element UUID string
   - Value: List of tag application Entities
   - Purpose: Fast retrieval of all tags on an element

## Implementation Phases

### Phase 1: Core Data Model (Week 1)
- [ ] Create model POJOs (TagDefinition, TagApplication, enums)
- [ ] Create aspect wrappers (TagDefinitionAspect, TagApplicationAspect)
- [ ] Unit tests for POJOs and aspect wrappers
- [ ] Verify reflection wrapper integration

### Phase 2: Registry Implementation (Week 1-2)
- [ ] Implement TagRegistry interface
- [ ] Create TagRegistryImpl with hierarchy management
- [ ] Implement tag definition CRUD operations
- [ ] Implement tag application CRUD operations
- [ ] Add index management for fast lookups
- [ ] Unit tests for registry operations

### Phase 3: Query & Validation (Week 2)
- [ ] Implement TagQuery builder
- [ ] Implement TagValidator
- [ ] Implement TagConflictDetector
- [ ] Add tag inheritance resolution
- [ ] Unit tests for queries and validation

### Phase 4: Standard Tags (Week 2-3)
- [ ] Define all 60+ standard tags from TAGS-DESIGN.md
- [ ] Implement StandardTags catalog
- [ ] Add tag initialization to registry
- [ ] Integration tests with standard tags

### Phase 5: Integration & Testing (Week 3)
- [ ] Integration tests with full CHEAP model
- [ ] Performance benchmarks (tag lookups, queries)
- [ ] Documentation and examples
- [ ] API usage guide

## Usage Examples

### Basic Tag Operations

```java
// Create catalog and registry
Catalog catalog = factory.createCatalog(UUID.randomUUID(),
                                       CatalogSpecies.CACHE,
                                       URI.create("mem://tags-catalog"));
TagRegistry tagRegistry = TagRegistry.create(catalog, factory);

// Initialize standard tags
tagRegistry.initializeStandardTags();

// Define custom tag
TagDefinition invoiceTag = TagDefinition.builder()
    .namespace("myapp.domain")
    .name("invoice-number")
    .description("Identifies invoice documents uniquely")
    .appliesTo(ElementType.PROPERTY)
    .scope(TagScope.CUSTOM)
    .parentTagIds(List.of(primaryKeyTagId, immutableTagId))
    .build();
Entity invoiceTagEntity = tagRegistry.defineTag(invoiceTag);

// Apply tag to property
UUID propertyId = UUID.randomUUID();
Entity tagApplication = tagRegistry.applyTag(
    propertyId,
    ElementType.PROPERTY,
    invoiceTagEntity.globalId(),
    Map.of("format", "INV-{year}-{sequence}"),
    TagSource.EXPLICIT
);

// Query tags on property
Collection<TagApplication> tags = tagRegistry.getTagsForElement(
    propertyId, ElementType.PROPERTY
);

// Find all properties with primary-key tag
TagDefinition pkTag = tagRegistry.getTagDefinitionByName("cheap.core", "primary-key");
Collection<UUID> primaryKeyProperties = tagRegistry.getElementsByTag(
    pkTag.getId(), ElementType.PROPERTY
);
```

### Query Builder

```java
// Find all properties with PII tags in custom namespaces
TagQueryResult result = new TagQuery(tagRegistry)
    .forType(ElementType.PROPERTY)
    .withTagName("cheap.core", "pii")
    .inNamespace("myapp.")
    .fromSource(TagSource.EXPLICIT)
    .execute();
```

### Tag Inheritance

```java
// Get all tags that inherit from primary-key
Collection<UUID> childTags = tagRegistry.getChildTags(primaryKeyTagId);

// Check if invoice-number inherits from primary-key
boolean inherits = tagRegistry.inheritsFrom(invoiceTagId, primaryKeyTagId);

// Get all ancestor tags (transitive)
Collection<UUID> ancestors = tagRegistry.getAllAncestorTags(invoiceTagId);
```

## Performance Considerations

1. **Index Hierarchies**: Use EntityDirectoryHierarchy for O(1) lookups
2. **Caching**: Cache frequently-accessed tag definitions in memory
3. **Lazy Loading**: Don't load all tag applications upfront
4. **Batch Operations**: Provide bulk tag application methods
5. **Reflection Optimization**: ImmutablePojoAspectDef caches field accessors

## Testing Strategy

### Unit Tests
- POJO validation and immutability
- Aspect wrapper creation and field access
- Registry CRUD operations
- Query builder logic
- Validation rules
- Tag inheritance resolution

### Integration Tests
- Full tag lifecycle (define → apply → query → remove)
- Standard tags initialization
- Multi-element tagging
- Complex queries with filters
- Conflict detection

### Performance Tests
- Tag lookup performance (1000+ tags)
- Query performance with filters
- Bulk tag application (1000+ elements)
- Memory usage with large tag catalogs

## Migration & Compatibility

This is a new feature, so no migration is needed. However:

1. **Versioning**: Tag definitions should support versioning for future evolution
2. **Deprecation**: Add deprecation markers to TagDefinition
3. **Backward Compatibility**: Ensure tag system doesn't break existing CHEAP usage

## Future Enhancements (Out of Scope)

1. **Tag Materialized Views**: Pre-compute tag queries for performance
2. **Tag Analytics**: Statistics on tag usage across catalogs
3. **Tag Suggestion**: ML-based tag recommendations
4. **Tag Validation Rules**: Custom validation logic per tag
5. **Tag Constraints**: Enforce constraints based on tags
6. **Multi-Language Ports**: Port to TypeScript, Python, Rust

## Dependencies

- CHEAP Core: All core interfaces and implementations
- Java 11+: For `var`, `List.of()`, etc.
- No external dependencies beyond CHEAP core

## Success Criteria

1. All 60+ standard tags defined and tested
2. Tag registry supports all CRUD operations
3. Query builder handles complex filters
4. Tag inheritance works correctly
5. Performance acceptable (< 10ms for tag lookups)
6. 90%+ test coverage
7. Complete API documentation
8. Usage examples for common scenarios
