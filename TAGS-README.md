# CHEAP Tags System

A flexible, performant metadata tagging system for the CHEAP framework, enabling semantic classification, validation, and rich querying capabilities for all CHEAP element types.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Quick Start](#quick-start)
- [Core Concepts](#core-concepts)
- [Standard Tags](#standard-tags)
- [Tag Inheritance](#tag-inheritance)
- [Validation and Conflicts](#validation-and-conflicts)
- [Querying](#querying)
- [Performance](#performance)
- [Architecture](#architecture)
- [API Reference](#api-reference)
- [Examples](#examples)

## Overview

The CHEAP Tags System provides a production-ready tagging infrastructure that:

- **Attaches metadata** to any CHEAP element (Property, Aspect, Entity, Hierarchy, Catalog)
- **Validates** tag applications with conflict detection
- **Queries** elements efficiently using tags and filters
- **Scales** to 50k+ tag applications with sub-millisecond lookups
- **Integrates** seamlessly with CHEAP's native storage

## Features

✅ **55 Standard Tags** in 9 semantic categories
✅ **Tag Inheritance** for building taxonomies
✅ **Validation** with namespace/name format rules
✅ **Conflict Detection** for mutually exclusive tags
✅ **Query Builder** with fluent API and complex filtering
✅ **O(1) Lookups** via optimized indexing
✅ **Idempotent Operations** for safe tag application
✅ **Thread-Safe** immutable data structures
✅ **249 Tests** with 90%+ coverage

## Quick Start

### 1. Create a TagRegistry

```java
CheapFactory factory = new CheapFactory();
Catalog catalog = factory.createCatalog(
    UUID.randomUUID(),
    CatalogSpecies.SINK,
    URI.create("mem://my-catalog"),
    null,
    0L
);

TagRegistry registry = TagRegistry.create(catalog, factory);
```

### 2. Initialize Standard Tags

```java
// Loads 55 standard tags in cheap.core namespace
registry.initializeStandardTags();
```

### 3. Define Custom Tags

```java
TagDefinition customerTag = new TagDefinition(
    "myapp.domain",           // namespace
    "customer-data",          // name
    "Customer-related data",  // description
    List.of(ElementType.PROPERTY, ElementType.ASPECT),  // applicable types
    TagScope.CUSTOM,          // scope
    null,                     // aliases (optional)
    null                      // parent tag IDs (optional)
);

Entity tagEntity = registry.defineTag(customerTag);
```

### 4. Apply Tags to Elements

```java
UUID propertyId = UUID.randomUUID();

registry.applyTag(
    propertyId,               // target element ID
    ElementType.PROPERTY,     // target element type
    tagEntity.globalId(),     // tag definition ID
    null,                     // metadata (optional)
    TagSource.EXPLICIT        // source
);
```

### 5. Query Tags

```java
// Get all tags for an element
Collection<TagApplication> tags = registry.getTagsForElement(
    propertyId,
    ElementType.PROPERTY
);

// Find all elements with a tag
Collection<UUID> elements = registry.getElementsByTag(
    tagEntity.globalId(),
    ElementType.PROPERTY
);

// Complex query
TagQuery query = new TagQuery(registry)
    .forType(ElementType.PROPERTY)
    .withTag(piiTagId)
    .withTag(encryptedTagId)
    .inNamespace("myapp.security")
    .fromSource(TagSource.EXPLICIT);

TagQueryResult result = query.execute();
```

## Core Concepts

### Tag Definitions

A **TagDefinition** describes a reusable tag:

```java
public class TagDefinition {
    private final String namespace;          // e.g., "cheap.core"
    private final String name;               // e.g., "pii"
    private final String description;        // human-readable description
    private final List<ElementType> appliesTo;  // which CHEAP elements can use it
    private final TagScope scope;            // STANDARD or CUSTOM
    private final List<String> aliases;      // alternative names (optional)
    private final List<UUID> parentTagIds;   // for inheritance (optional)
}
```

**Full name**: `namespace.name` (e.g., `cheap.core.pii`)

### Tag Applications

A **TagApplication** records when a tag is applied to an element:

```java
public class TagApplication {
    private final UUID tagDefinitionId;      // which tag
    private final UUID targetElementId;      // which element
    private final ElementType targetType;    // element type
    private final Map<String, Object> metadata;  // optional key-value data
    private final TagSource source;          // EXPLICIT, INFERRED, or GENERATED
    private final ZonedDateTime appliedAt;   // when
    private final String appliedBy;          // who (optional)
}
```

### Element Types

Tags can be applied to any CHEAP element:

- **PROPERTY** - Individual data fields
- **ASPECT** - Collections of properties
- **ENTITY** - Keyed data records
- **HIERARCHY** - Organizational structures
- **CATALOG** - Top-level data containers

### Tag Sources

Track the provenance of tag applications:

- **EXPLICIT** - Manually applied by user/system
- **INFERRED** - Derived from other tags or rules
- **GENERATED** - Automatically created by system

### Tag Scopes

Distinguish between framework and application tags:

- **STANDARD** - Built-in tags in `cheap.core` namespace
- **CUSTOM** - Application-specific tags in custom namespaces

## Standard Tags

The system includes 55 standard tags organized into 9 categories:

### Identity and Keys (6 tags)
- `primary-key` - Primary key field
- `foreign-key` - Foreign key reference
- `composite-key-part` - Part of composite key
- `natural-key` - Business-derived key
- `surrogate-key` - System-generated key
- `alternate-key` - Alternative unique identifier

### Temporal and Versioning (7 tags)
- `created-timestamp` - Creation time
- `modified-timestamp` - Last modification time
- `version-number` - Optimistic locking version
- `effective-date` - When entity becomes effective
- `expiration-date` - When entity expires
- `temporal-range-start` - Start of validity range
- `temporal-range-end` - End of validity range

### Lifecycle and State (5 tags)
- `soft-delete-flag` - Logical deletion marker
- `archived-flag` - Archive status
- `status-field` - Lifecycle status
- `approval-status` - Approval workflow status
- `published-flag` - Publication status

### Relationships (5 tags)
- `parent-reference` - Parent in hierarchy
- `owner-reference` - Owning entity
- `many-to-many-link` - Join table entity
- `polymorphic-reference` - Multi-type reference
- `self-reference` - Same-type reference

### Data Semantics (6 tags)
- `display-name` - Human-readable name
- `description-field` - Descriptive text
- `sort-order` - Default sort field
- `code-value` - Machine-readable code
- `computed-field` - Derived value
- `denormalized-cache` - Cached data

### Validation and Constraints (7 tags)
- `required` - Must have value
- `nullable` - May be null
- `unique` - Unique constraint
- `immutable` - Cannot be modified
- `range-bounded` - Min/max constraints
- `format-constrained` - Pattern requirements
- `enum-valued` - Fixed value set

### Security and Privacy (6 tags)
- `pii` - Personally Identifiable Information
- `sensitive` - Requires access controls
- `encrypted` - Encrypted at rest
- `audit-logged` - Changes are logged
- `masked` - Masked in displays
- `anonymizable` - Can be anonymized

### Business Domain (8 tags)
- `monetary-amount` - Currency value
- `quantity` - Numeric quantity with unit
- `percentage` - Percentage value
- `email-address` - Email format
- `phone-number` - Phone format
- `postal-address` - Mailing address
- `url` - Web address
- `geo-coordinate` - Geographic coordinate

### Technical Behavior (5 tags)
- `indexed` - Database index
- `searchable` - Full-text searchable
- `lazy-loaded` - On-demand loading
- `cached` - Performance caching
- `immutable-aggregate-root` - Immutable aggregate

## Tag Inheritance

Tags can inherit from parent tags to create taxonomies:

```java
// Define base tag
TagDefinition dataField = new TagDefinition(
    "myapp", "data-field", "Generic data field",
    List.of(ElementType.PROPERTY), TagScope.CUSTOM, null, null
);
Entity dataFieldEntity = registry.defineTag(dataField);

// Define child tag that inherits from base
TagDefinition sensitiveField = new TagDefinition(
    "myapp", "sensitive-field", "Sensitive data field",
    List.of(ElementType.PROPERTY), TagScope.CUSTOM, null,
    List.of(dataFieldEntity.globalId())  // parent tag ID
);
Entity sensitiveEntity = registry.defineTag(sensitiveField);

// Query inheritance relationships
Collection<UUID> parents = registry.getParentTags(sensitiveEntity.globalId());
Collection<UUID> ancestors = registry.getAllAncestorTags(sensitiveEntity.globalId());
Collection<UUID> children = registry.getChildTags(dataFieldEntity.globalId());
boolean inherits = registry.inheritsFrom(sensitiveEntity.globalId(), dataFieldEntity.globalId());
```

## Validation and Conflicts

### Namespace and Name Validation

- **Namespace format**: `[a-z0-9]+([.-][a-z0-9]+)+` (e.g., `cheap.core`, `myapp.domain.security`)
- **Name format**: `[a-z0-9]+(-[a-z0-9]+)*` (e.g., `primary-key`, `pii`)
- Must have at least one dot in namespace
- No leading/trailing hyphens in names

### Applicability Validation

Tags can only be applied to element types they're designed for:

```java
// This tag only applies to properties
TagDefinition primaryKey = StandardTags.PRIMARY_KEY;

// This will succeed
registry.applyTag(propertyId, ElementType.PROPERTY, pkTagId, null, TagSource.EXPLICIT);

// This will fail validation
registry.applyTag(hierarchyId, ElementType.HIERARCHY, pkTagId, null, TagSource.EXPLICIT);
```

### Conflict Detection

The system detects semantic conflicts between tags:

```java
// These tags conflict
"cheap.core.immutable" ↔ "cheap.core.modified-timestamp"
"cheap.core.required" ↔ "cheap.core.nullable"
"cheap.core.encrypted" ↔ "cheap.core.masked"
"cheap.core.primary-key" ↔ "cheap.core.foreign-key"

// Validate before applying
Collection<String> errors = registry.validateTagApplication(
    tagId, elementId, elementType
);

if (!errors.isEmpty()) {
    // Handle validation errors
}
```

### Circular Inheritance Detection

The system prevents circular inheritance:

```java
// This will be rejected
TagDefinition child = new TagDefinition(..., List.of(parentId));
TagDefinition parent = new TagDefinition(..., List.of(childId));  // CIRCULAR!
```

## Querying

### Basic Queries

```java
// Get all tags for an element
Collection<TagApplication> tags = registry.getTagsForElement(
    elementId, ElementType.PROPERTY
);

// Get all elements with a specific tag
Collection<UUID> elements = registry.getElementsByTag(
    tagId, ElementType.PROPERTY
);

// Get elements by tag name
Collection<UUID> elements = registry.getElementsByTagName(
    "cheap.core", "pii", ElementType.PROPERTY
);

// Check if element has a tag
boolean hasPii = registry.hasTag(elementId, ElementType.PROPERTY, piiTagId);
```

### Query Builder

```java
TagQuery query = new TagQuery(registry)
    .forType(ElementType.PROPERTY)              // Required: element type
    .withTag(tag1Id)                            // AND: must have tag1
    .withTag(tag2Id)                            // AND: must have tag2
    .withoutTag(tag3Id)                         // NOT: must not have tag3
    .inNamespace("myapp.security")              // IN: namespace filter
    .notInNamespace("test")                     // NOT IN: exclude namespace
    .fromSource(TagSource.EXPLICIT)             // FROM: source filter
    .includeInheritedTags(true);                // Include child tags

TagQueryResult result = query.execute();

// Access results
Collection<UUID> elementIds = result.getElements();
int count = result.size();
boolean isEmpty = result.isEmpty();

// Get tags for each element
for (UUID elementId : result.getElements()) {
    Collection<TagApplication> elementTags = result.getTagsFor(elementId);
}
```

### Query Logic

- **Multiple `withTag()` calls**: AND logic (element must have ALL specified tags)
- **Namespace filters**: Hierarchical (e.g., "cheap.core" includes "cheap.core.security")
- **Source filter**: Exact match (EXPLICIT, INFERRED, or GENERATED)
- **Exclude filters**: Element must NOT have any excluded tags

## Performance

Validated performance characteristics:

| Operation | Target | Actual | Complexity |
|-----------|--------|--------|------------|
| Tag lookup by name | <10ms | <1ms | O(1) |
| Tag application | <20ms | <5ms | O(1) |
| Query 10k elements | <100ms | <50ms | O(n) |
| Bulk retrieval (1k) | N/A | <10ms | O(n) |
| Memory (1000 tags) | <1MB | <1MB | O(n) |

**Scalability tested**: 50k+ tag applications with consistent performance.

## Architecture

### Storage Strategy

Tags are stored using CHEAP-native hierarchies:

1. **AspectMapHierarchy**
   - Stores `TagDefinitionAspect` (one per tag definition)
   - Stores `TagApplicationAspect` (one per tag application)

2. **EntityDirectoryHierarchy**
   - Index: `tag_index_by_name` → O(1) name-based lookups
   - Key format: `namespace:name`

3. **EntitySetHierarchy** (bidirectional indices)
   - Index: `tag_index_by_element_<type>_<elementId>` → O(1) element→tags
   - Index: `tag_index_by_tag_<type>_<tagId>` → O(1) tag→elements

### Class Structure

```
net.netbeing.cheap.tags
├── model                 # Core data models
│   ├── ElementType       # Enum: PROPERTY, ASPECT, ENTITY, HIERARCHY, CATALOG
│   ├── TagScope          # Enum: STANDARD, CUSTOM
│   ├── TagSource         # Enum: EXPLICIT, INFERRED, GENERATED
│   ├── TagDefinition     # Immutable POJO
│   └── TagApplication    # Immutable POJO
├── aspect                # CHEAP aspect wrappers
│   ├── TagDefinitionAspect
│   └── TagApplicationAspect
├── registry              # Tag management
│   ├── TagRegistry       # Interface
│   └── TagRegistryImpl   # Implementation
├── validation            # Validation and conflicts
│   ├── TagValidator      # Format and applicability validation
│   └── TagConflictDetector  # Semantic conflict detection
├── query                 # Query builder
│   ├── TagQuery          # Fluent query API
│   └── TagQueryResult    # Query results
└── standard              # Standard tags
    └── StandardTags      # 55 standard tag definitions
```

### Thread Safety

- **TagDefinition** and **TagApplication** are immutable POJOs (thread-safe)
- **TagRegistry** delegates to CHEAP catalog operations (inherits catalog's thread safety)
- **StandardTags** constants are final and unmodifiable (thread-safe)

## API Reference

### TagRegistry

```java
// Creation
static TagRegistry create(Catalog catalog, CheapFactory factory)

// Tag Definitions
Entity defineTag(TagDefinition definition)
TagDefinition getTagDefinition(UUID tagEntityId)
TagDefinition getTagDefinitionByName(String namespace, String name)
Collection<TagDefinition> getAllTagDefinitions()
Collection<TagDefinition> getTagDefinitionsByNamespace(String namespace)

// Tag Applications
Entity applyTag(UUID targetId, ElementType type, UUID tagDefId,
                Map<String, Object> metadata, TagSource source)
void removeTag(UUID tagApplicationId)

// Queries
Collection<TagApplication> getTagsForElement(UUID elementId, ElementType type)
Collection<UUID> getElementsByTag(UUID tagDefId, ElementType type)
Collection<UUID> getElementsByTagName(String namespace, String name, ElementType type)
boolean hasTag(UUID elementId, ElementType type, UUID tagDefId)

// Validation
boolean isTagApplicable(UUID tagDefId, ElementType targetType)
Collection<String> validateTagApplication(UUID tagDefId, UUID targetId, ElementType type)

// Inheritance
Collection<UUID> getParentTags(UUID tagDefId)
Collection<UUID> getAllAncestorTags(UUID tagDefId)
Collection<UUID> getChildTags(UUID tagDefId)
boolean inheritsFrom(UUID childTagId, UUID parentTagId)

// Standard Tags
void initializeStandardTags()
Collection<TagDefinition> getStandardTags()
```

### StandardTags

```java
// All standard tags
Collection<TagDefinition> allStandardTags()

// Lookup
TagDefinition getStandardTag(String name)
boolean isStandardTag(String name)
int getStandardTagCount()

// Constants (55 total)
TagDefinition PRIMARY_KEY
TagDefinition PII
TagDefinition CREATED_TIMESTAMP
TagDefinition IMMUTABLE
// ... and 51 more
```

## Examples

### Example 1: Tagging Customer Data

```java
// Initialize
TagRegistry registry = TagRegistry.create(catalog, factory);
registry.initializeStandardTags();

// Define custom tags
TagDefinition customerTag = new TagDefinition(
    "myapp.domain", "customer-data", "Customer data",
    List.of(ElementType.PROPERTY, ElementType.ASPECT),
    TagScope.CUSTOM, null, null
);
Entity customerEntity = registry.defineTag(customerTag);

// Apply standard + custom tags
UUID emailProp = UUID.randomUUID();
UUID ssnProp = UUID.randomUUID();

// Get standard tag entity IDs
TagDefinition piiTag = registry.getTagDefinitionByName("cheap.core", "pii");
TagDefinition emailTag = registry.getTagDefinitionByName("cheap.core", "email-address");

// Apply tags (need to get entity IDs first)
registry.applyTag(emailProp, ElementType.PROPERTY,
    customerEntity.globalId(), null, TagSource.EXPLICIT);
```

### Example 2: Building a Tag Taxonomy

```java
// Base tag
TagDefinition secureData = new TagDefinition(
    "myapp.security", "secure-data", "Data requiring security",
    List.of(ElementType.PROPERTY), TagScope.CUSTOM, null, null
);
Entity secureEntity = registry.defineTag(secureData);

// Level 1 children
TagDefinition encryptedData = new TagDefinition(
    "myapp.security", "encrypted-data", "Encrypted secure data",
    List.of(ElementType.PROPERTY), TagScope.CUSTOM, null,
    List.of(secureEntity.globalId())
);
Entity encryptedEntity = registry.defineTag(encryptedData);

TagDefinition maskedData = new TagDefinition(
    "myapp.security", "masked-data", "Masked secure data",
    List.of(ElementType.PROPERTY), TagScope.CUSTOM, null,
    List.of(secureEntity.globalId())
);
Entity maskedEntity = registry.defineTag(maskedData);

// Query the hierarchy
Collection<UUID> children = registry.getChildTags(secureEntity.globalId());
// Returns: [encryptedEntity.id, maskedEntity.id]

boolean inherits = registry.inheritsFrom(encryptedEntity.globalId(), secureEntity.globalId());
// Returns: true
```

### Example 3: Complex Query

```java
// Find all explicitly-tagged PII properties that are encrypted,
// in the customer domain, excluding test data
TagQuery query = new TagQuery(registry)
    .forType(ElementType.PROPERTY)
    .withTag(piiTagId)
    .withTag(encryptedTagId)
    .inNamespace("myapp.customer")
    .notInNamespace("test")
    .fromSource(TagSource.EXPLICIT);

TagQueryResult result = query.execute();

System.out.println("Found " + result.size() + " matching properties");

for (UUID propId : result.getElements()) {
    Collection<TagApplication> tags = result.getTagsFor(propId);
    System.out.println("Property " + propId + " has " + tags.size() + " tags");
}
```

### Example 4: Validation and Error Handling

```java
UUID propertyId = UUID.randomUUID();
UUID tagId = someTagEntity.globalId();

// Validate before applying
Collection<String> errors = registry.validateTagApplication(
    tagId, propertyId, ElementType.PROPERTY
);

if (!errors.isEmpty()) {
    System.err.println("Validation errors:");
    for (String error : errors) {
        System.err.println("  - " + error);
    }
} else {
    // Safe to apply
    registry.applyTag(propertyId, ElementType.PROPERTY,
        tagId, null, TagSource.EXPLICIT);
}
```

---

## Implementation Status

✅ **Phase 1**: Core Data Model (90 tests)
✅ **Phase 2**: TagRegistry (29 tests)
✅ **Phase 3**: Validation & Inheritance (25 tests)
✅ **Phase 4**: Query System (35 tests)
✅ **Phase 5**: Standard Tags (52 tests)
✅ **Phase 6**: Integration & Performance (18 tests)
✅ **Phase 7**: Code Review & Documentation

**Total**: 249 tests, 100% passing, 90%+ coverage

---

## License

Copyright (c) 2026 David Noha

Licensed under the Apache License, Version 2.0
