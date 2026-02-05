# CHEAP Tags System - Implementation TODO

This document provides an actionable task list for implementing the Tags system in the CHEAP Java framework. Tasks are organized by phase and prioritized for sequential implementation.

**Project Location**: `/d/src/claude/cheap/cheap-core/src/main/java/net/netbeing/cheap/tags/`

**Reference**: See `TAGS-PLAN.md` for detailed architecture and design decisions.

---

## Phase 1: Core Data Model (Days 1-3)

### 1.1 Create Package Structure
- [ ] Create `net.netbeing.cheap.tags` package
- [ ] Create `net.netbeing.cheap.tags.model` subpackage
- [ ] Create `net.netbeing.cheap.tags.aspect` subpackage
- [ ] Create `net.netbeing.cheap.tags.registry` subpackage
- [ ] Create `net.netbeing.cheap.tags.query` subpackage
- [ ] Create `net.netbeing.cheap.tags.validation` subpackage
- [ ] Create `net.netbeing.cheap.tags.standard` subpackage

### 1.2 Create Enum Types
- [ ] Create `ElementType.java` enum
  - Values: PROPERTY, ASPECT, HIERARCHY, ENTITY, CATALOG
  - Add utility methods (fromString, toString)
- [ ] Create `TagScope.java` enum
  - Values: STANDARD, CUSTOM
- [ ] Create `TagSource.java` enum
  - Values: EXPLICIT, INFERRED, GENERATED
- [ ] Write unit tests for enums

### 1.3 Create TagDefinition POJO
- [ ] Create `TagDefinition.java` class
  - Fields: namespace, name, description, appliesTo, scope, aliases, parentTagIds
  - Constructor with all fields
  - JavaBean getters (no setters - immutable)
  - Implement equals(), hashCode(), toString()
  - Add builder pattern support (optional)
- [ ] Write unit tests for TagDefinition
  - Test immutability
  - Test equals/hashCode contract
  - Test validation of required fields

### 1.4 Create TagApplication POJO
- [ ] Create `TagApplication.java` class
  - Fields: tagDefinitionId, targetElementId, targetType, metadata, source, appliedAt, appliedBy
  - Constructor with all fields
  - JavaBean getters (no setters - immutable)
  - Implement equals(), hashCode(), toString()
- [ ] Write unit tests for TagApplication
  - Test immutability
  - Test equals/hashCode contract
  - Test metadata serialization

### 1.5 Create Aspect Wrappers
- [ ] Create `TagDefinitionAspect.java`
  - Extend `ImmutablePojoAspect<TagDefinition>`
  - Static method: `aspectDef()` returning `ImmutablePojoAspectDef`
  - Constructor: `TagDefinitionAspect(Entity, TagDefinition)`
  - Convenience accessors: getNamespace(), getName(), getFullName(), etc.
- [ ] Create `TagApplicationAspect.java`
  - Extend `ImmutablePojoAspect<TagApplication>`
  - Static method: `aspectDef()` returning `ImmutablePojoAspectDef`
  - Constructor: `TagApplicationAspect(Entity, TagApplication)`
  - Convenience accessors: getTagDefinitionId(), getTargetElementId(), etc.
- [ ] Write unit tests for aspect wrappers
  - Test aspect creation with POJOs
  - Test field access via reflection
  - Test aspect attachment to entities
  - Verify immutability through aspect interface

---

## Phase 2: Registry Implementation (Days 4-8)

### 2.1 Create TagRegistry Interface
- [ ] Create `TagRegistry.java` interface
  - Initialization methods
    - `static TagRegistry create(Catalog, CheapFactory)`
    - `Catalog catalog()`
  - Tag definition management methods
    - `Entity defineTag(TagDefinition)`
    - `TagDefinition getTagDefinition(UUID)`
    - `TagDefinition getTagDefinitionByName(String namespace, String name)`
    - `Collection<TagDefinition> getAllTagDefinitions()`
    - `Collection<TagDefinition> getTagDefinitionsByNamespace(String namespace)`
  - Tag application methods
    - `Entity applyTag(UUID targetId, ElementType type, UUID tagDefId, Map<String, Object> metadata, TagSource source)`
    - `void removeTag(UUID tagApplicationId)`
  - Query methods
    - `Collection<TagApplication> getTagsForElement(UUID elementId, ElementType type)`
    - `Collection<UUID> getElementsByTag(UUID tagDefId, ElementType type)`
    - `Collection<UUID> getElementsByTagName(String namespace, String name, ElementType type)`
    - `boolean hasTag(UUID elementId, ElementType type, UUID tagDefId)`
  - Validation methods
    - `boolean isTagApplicable(UUID tagDefId, ElementType targetType)`
    - `Collection<String> validateTagApplication(UUID tagDefId, UUID targetId, ElementType type)`
  - Tag inheritance methods
    - `Collection<UUID> getParentTags(UUID tagDefId)`
    - `Collection<UUID> getAllAncestorTags(UUID tagDefId)`
    - `Collection<UUID> getChildTags(UUID tagDefId)`
    - `boolean inheritsFrom(UUID childTagId, UUID parentTagId)`
  - Standard tags methods
    - `void initializeStandardTags()`
    - `Collection<TagDefinition> getStandardTags()`
- [ ] Add comprehensive Javadoc to interface

### 2.2 Implement TagRegistryImpl - Setup
- [ ] Create `TagRegistryImpl.java` class
- [ ] Add fields:
  - `Catalog catalog`
  - `CheapFactory factory`
  - `AspectMapHierarchy tagDefinitionsHierarchy`
  - `Map<ElementType, AspectMapHierarchy> tagApplicationsHierarchies`
  - `EntityDirectoryHierarchy tagIndexByName`
  - `EntityDirectoryHierarchy tagsByElement`
  - `ImmutablePojoAspectDef tagDefinitionAspectDef`
  - `ImmutablePojoAspectDef tagApplicationAspectDef`
- [ ] Implement static factory method `create(Catalog, CheapFactory)`
  - Initialize all hierarchies
  - Create aspect definitions
  - Return new TagRegistryImpl instance
- [ ] Implement constructor
  - Store catalog and factory references
  - Create/retrieve tag_definitions hierarchy
  - Create/retrieve tag_applications_* hierarchies (one per ElementType)
  - Create/retrieve tag_index_by_name hierarchy
  - Create/retrieve tags_by_element hierarchy
  - Initialize aspect definitions

### 2.3 Implement TagRegistryImpl - Tag Definitions
- [ ] Implement `defineTag(TagDefinition definition)`
  - Validate definition (namespace, name, appliesTo)
  - Check for duplicate namespace:name
  - Create new Entity for tag
  - Create TagDefinitionAspect from definition
  - Attach aspect to entity
  - Store in tag_definitions hierarchy
  - Add to tag_index_by_name
  - Return entity
- [ ] Implement `getTagDefinition(UUID tagEntityId)`
  - Retrieve entity from tag_definitions
  - Get TagDefinitionAspect
  - Extract and return TagDefinition POJO
- [ ] Implement `getTagDefinitionByName(String namespace, String name)`
  - Lookup in tag_index_by_name using "namespace:name" key
  - If found, call getTagDefinition(UUID)
  - Return null if not found
- [ ] Implement `getAllTagDefinitions()`
  - Iterate all entities in tag_definitions hierarchy
  - Extract TagDefinition POJOs
  - Return collection
- [ ] Implement `getTagDefinitionsByNamespace(String namespace)`
  - Get all tag definitions
  - Filter by namespace prefix
  - Return matching collection

### 2.4 Implement TagRegistryImpl - Tag Applications
- [ ] Implement `applyTag(UUID targetId, ElementType type, UUID tagDefId, Map<String, Object> metadata, TagSource source)`
  - Validate tag definition exists
  - Validate tag is applicable to target type
  - Check if tag already applied (idempotency)
  - Create new Entity for tag application
  - Create TagApplication POJO with current timestamp
  - Create TagApplicationAspect
  - Attach aspect to entity
  - Store in appropriate tag_applications_* hierarchy
  - Update tags_by_element index
  - Return entity
- [ ] Implement `removeTag(UUID tagApplicationId)`
  - Retrieve TagApplicationAspect by ID
  - Get targetElementId and targetType
  - Remove from tag_applications_* hierarchy
  - Update tags_by_element index
- [ ] Implement `getTagsForElement(UUID elementId, ElementType type)`
  - Lookup in tags_by_element index
  - Get list of tag application entities
  - Retrieve TagApplicationAspects
  - Filter by targetType
  - Extract and return TagApplication POJOs
- [ ] Implement `getElementsByTag(UUID tagDefId, ElementType type)`
  - Get appropriate tag_applications_* hierarchy
  - Iterate all tag applications
  - Filter by tagDefinitionId and targetType
  - Extract targetElementIds
  - Return collection
- [ ] Implement `getElementsByTagName(String namespace, String name, ElementType type)`
  - Resolve tag definition by name
  - If found, call getElementsByTag(UUID, ElementType)
  - Return empty collection if tag not found
- [ ] Implement `hasTag(UUID elementId, ElementType type, UUID tagDefId)`
  - Get tags for element
  - Check if any match tagDefId
  - Return boolean

### 2.5 Write Unit Tests for TagRegistryImpl
- [ ] Test registry creation and initialization
- [ ] Test tag definition lifecycle (create, retrieve, list)
- [ ] Test duplicate tag definition prevention
- [ ] Test tag application lifecycle (apply, retrieve, remove)
- [ ] Test tag application idempotency
- [ ] Test tag queries (by element, by tag, by name)
- [ ] Test index consistency (tag_index_by_name, tags_by_element)
- [ ] Test error handling (invalid UUIDs, missing tags, etc.)

---

## Phase 3: Validation & Inheritance (Days 9-12)

### 3.1 Create TagValidator
- [ ] Create `TagValidator.java` class
- [ ] Add constructor with TagRegistry parameter
- [ ] Implement `validateTagDefinition(TagDefinition definition)`
  - Check namespace format (e.g., "cheap.core", "myapp.domain")
  - Check name format (lowercase, hyphens, no spaces)
  - Validate appliesTo list is not empty
  - Check parent tags exist (if specified)
  - Detect circular inheritance
  - Return list of validation errors
- [ ] Implement `validateTagApplication(UUID tagDefId, UUID targetId, ElementType targetType)`
  - Check tag definition exists
  - Check tag is applicable to target type
  - Check for conflicting tags on target
  - Return list of validation errors
- [ ] Implement `isNamespaceValid(String namespace)`
  - Regex check: lowercase, dots, hyphens only
  - Must have at least one dot
  - Return boolean
- [ ] Implement `isNameValid(String name)`
  - Regex check: lowercase, hyphens only
  - No leading/trailing hyphens
  - Return boolean
- [ ] Implement `detectCircularInheritance(UUID tagId)`
  - Traverse parent tag chain
  - Detect if tagId appears in ancestors
  - Return boolean

### 3.2 Create TagConflictDetector
- [ ] Create `TagConflictDetector.java` class
- [ ] Add constructor with TagRegistry parameter
- [ ] Define conflict rules map
  - "cheap.core.immutable" conflicts with "cheap.core.modified-timestamp"
  - "cheap.core.required" conflicts with "cheap.core.nullable"
  - Add other semantic conflicts
- [ ] Implement `detectConflicts(UUID elementId, ElementType type)`
  - Get all tags on element
  - Check each tag against conflict rules
  - Check inherited tags for conflicts
  - Return list of conflict descriptions
- [ ] Implement `getConflictingTags(String tagFullName)`
  - Look up conflict rules
  - Return set of conflicting tag names

### 3.3 Implement Tag Inheritance in TagRegistryImpl
- [ ] Implement `getParentTags(UUID tagDefId)`
  - Get tag definition
  - Extract parentTagIds
  - Return collection
- [ ] Implement `getAllAncestorTags(UUID tagDefId)`
  - Recursively traverse parent tags
  - Collect all ancestor UUIDs
  - Detect and prevent infinite loops
  - Return collection
- [ ] Implement `getChildTags(UUID tagDefId)`
  - Iterate all tag definitions
  - Check if parentTagIds contains tagDefId
  - Return collection of children
- [ ] Implement `inheritsFrom(UUID childTagId, UUID parentTagId)`
  - Get all ancestor tags of child
  - Check if parentTagId is in ancestors
  - Return boolean
- [ ] Integrate validation into defineTag()
  - Call TagValidator.validateTagDefinition()
  - Throw exception if validation fails
- [ ] Integrate validation into applyTag()
  - Call TagValidator.validateTagApplication()
  - Call TagConflictDetector.detectConflicts()
  - Throw exception if validation fails or conflicts exist

### 3.4 Write Unit Tests for Validation & Inheritance
- [ ] Test namespace validation (valid and invalid formats)
- [ ] Test name validation (valid and invalid formats)
- [ ] Test circular inheritance detection
- [ ] Test tag applicability validation
- [ ] Test conflict detection (immutable + modified-timestamp, etc.)
- [ ] Test parent tag retrieval
- [ ] Test ancestor tag retrieval (transitive)
- [ ] Test child tag retrieval
- [ ] Test inheritance checking (inheritsFrom)
- [ ] Test validation integration in registry methods

---

## Phase 4: Query Builder (Days 13-15)

### 4.1 Create TagQuery Class
- [ ] Create `TagQuery.java` class
- [ ] Add fields:
  - `TagRegistry registry`
  - `ElementType targetType`
  - `Set<UUID> includeTagIds`
  - `Set<UUID> excludeTagIds`
  - `Set<String> includeNamespaces`
  - `Set<String> excludeNamespaces`
  - `TagSource sourceFilter`
  - `boolean includeInherited`
- [ ] Add constructor: `TagQuery(TagRegistry registry)`
- [ ] Implement builder methods:
  - `forType(ElementType type)` - Set target element type
  - `withTag(UUID tagId)` - Include elements with tag
  - `withTagName(String namespace, String name)` - Include by tag name
  - `withoutTag(UUID tagId)` - Exclude elements with tag
  - `inNamespace(String namespace)` - Include tags in namespace
  - `notInNamespace(String namespace)` - Exclude tags in namespace
  - `fromSource(TagSource source)` - Filter by tag source
  - `includeInheritedTags(boolean include)` - Include inherited tags
- [ ] Implement `execute()` method
  - Resolve all tag filters
  - Query registry for matching elements
  - Apply namespace filters
  - Apply source filters
  - Handle tag inheritance if enabled
  - Return TagQueryResult

### 4.2 Create TagQueryResult Class
- [ ] Create `TagQueryResult.java` class
- [ ] Add fields:
  - `Collection<UUID> elementIds`
  - `Map<UUID, Collection<TagApplication>> tagsByElement`
  - `int totalCount`
- [ ] Add getters for all fields
- [ ] Add convenience methods:
  - `getElements()` - Get element IDs
  - `getTagsFor(UUID elementId)` - Get tags for specific element
  - `isEmpty()` - Check if no results
  - `size()` - Get count of elements

### 4.3 Write Unit Tests for Query Builder
- [ ] Test query builder chaining
- [ ] Test single tag filter
- [ ] Test multiple tag filters (AND logic)
- [ ] Test tag exclusion
- [ ] Test namespace filters
- [ ] Test source filters
- [ ] Test inherited tag inclusion/exclusion
- [ ] Test complex queries with multiple filters
- [ ] Test empty results
- [ ] Test query result accessors

---

## Phase 5: Standard Tags (Days 16-19)

### 5.1 Create StandardTags Class
- [ ] Create `StandardTags.java` class
- [ ] Define constants for all cheap.core tags (60+ tags):
  - **Identity and Keys** (6 tags)
    - PRIMARY_KEY, FOREIGN_KEY, COMPOSITE_KEY_PART, NATURAL_KEY, SURROGATE_KEY, ALTERNATE_KEY
  - **Temporal and Versioning** (7 tags)
    - CREATED_TIMESTAMP, MODIFIED_TIMESTAMP, VERSION_NUMBER, EFFECTIVE_DATE, EXPIRATION_DATE, TEMPORAL_RANGE_START, TEMPORAL_RANGE_END
  - **Lifecycle and State** (5 tags)
    - SOFT_DELETE_FLAG, ARCHIVED_FLAG, STATUS_FIELD, APPROVAL_STATUS, PUBLISHED_FLAG
  - **Relationships** (5 tags)
    - PARENT_REFERENCE, OWNER_REFERENCE, MANY_TO_MANY_LINK, POLYMORPHIC_REFERENCE, SELF_REFERENCE
  - **Data Semantics** (6 tags)
    - DISPLAY_NAME, DESCRIPTION_FIELD, SORT_ORDER, CODE_VALUE, COMPUTED_FIELD, DENORMALIZED_CACHE
  - **Validation and Constraints** (6 tags)
    - REQUIRED, UNIQUE, IMMUTABLE, RANGE_BOUNDED, FORMAT_CONSTRAINED, ENUM_VALUED
  - **Security and Privacy** (6 tags)
    - PII, SENSITIVE, ENCRYPTED, AUDIT_LOGGED, MASKED, ANONYMIZABLE
  - **Business Domain** (8 tags)
    - MONETARY_AMOUNT, QUANTITY, PERCENTAGE, EMAIL_ADDRESS, PHONE_NUMBER, POSTAL_ADDRESS, URL, GEO_COORDINATE
  - **Technical Behavior** (5 tags)
    - INDEXED, SEARCHABLE, LAZY_LOADED, CACHED, IMMUTABLE_AGGREGATE_ROOT
- [ ] Implement `allStandardTags()` method
  - Return collection of all standard tag definitions
- [ ] Implement `getStandardTag(String name)` method
  - Look up tag by name
  - Return TagDefinition or null

### 5.2 Integrate Standard Tags into TagRegistry
- [ ] Implement `initializeStandardTags()` in TagRegistryImpl
  - Get all standard tags from StandardTags
  - Define each tag using defineTag()
  - Handle duplicates gracefully (idempotent)
  - Store standard tag entity IDs for quick access
- [ ] Implement `getStandardTags()` in TagRegistryImpl
  - Return collection of standard tag definitions
  - Query from tag_definitions hierarchy
  - Filter by namespace = "cheap.core"

### 5.3 Write Unit Tests for Standard Tags
- [ ] Test StandardTags class constants
- [ ] Test allStandardTags() returns complete set
- [ ] Test getStandardTag() lookups
- [ ] Test initializeStandardTags() in registry
- [ ] Test idempotency of standard tag initialization
- [ ] Test standard tag retrieval from registry
- [ ] Verify all 60+ tags have correct properties

---

## Phase 6: Integration & Testing (Days 20-23)

### 6.1 Integration Tests
- [ ] Create `TagSystemIntegrationTest.java`
- [ ] Test full tag lifecycle with real catalog
  - Create catalog
  - Initialize registry with standard tags
  - Define custom tags with inheritance
  - Apply tags to various element types
  - Query tags by element
  - Query elements by tag
  - Remove tags
  - Verify persistence in hierarchies
- [ ] Test tag inheritance scenarios
  - Define parent and child tags
  - Apply child tag to element
  - Verify inherited properties
  - Query by parent tag (should include children)
- [ ] Test validation and conflict detection
  - Apply conflicting tags
  - Verify conflict detection
  - Test validation error handling
- [ ] Test complex queries
  - Multiple filters
  - Namespace filtering
  - Source filtering
  - Inherited tag queries
- [ ] Test multi-element scenarios
  - Tag multiple entities
  - Tag multiple properties
  - Query across element types
  - Verify index consistency

### 6.2 Performance Tests
- [ ] Create `TagSystemPerformanceTest.java`
- [ ] Benchmark tag definition creation (1000 tags)
- [ ] Benchmark tag application (10000 applications)
- [ ] Benchmark tag lookup by name (10000 lookups)
- [ ] Benchmark tag query by element (10000 elements)
- [ ] Benchmark tag query by tag (filter 10000 elements)
- [ ] Benchmark complex queries with multiple filters
- [ ] Measure memory usage with large tag catalog
- [ ] Verify lookup performance < 10ms (target)

### 6.3 Edge Case Tests
- [ ] Test with null/invalid inputs
- [ ] Test with empty collections
- [ ] Test with very long namespace/name strings
- [ ] Test with deep tag inheritance (10+ levels)
- [ ] Test with circular inheritance attempts
- [ ] Test with duplicate tag applications
- [ ] Test with concurrent tag operations
- [ ] Test with missing parent tags
- [ ] Test catalog serialization/deserialization

### 6.4 Documentation
- [ ] Write API documentation (Javadoc)
  - All public classes
  - All public methods
  - Usage examples in class-level docs
- [ ] Create usage guide
  - Basic tag operations
  - Tag inheritance
  - Query examples
  - Validation and conflict detection
  - Best practices
- [ ] Create architecture documentation
  - Storage strategy
  - Performance considerations
  - Extension points
- [ ] Create examples
  - Simple tag usage example
  - Custom tag definition example
  - Complex query example
  - Tag inheritance example

---

## Phase 7: Code Review & Refinement (Days 24-25)

### 7.1 Code Quality
- [ ] Run static analysis (PMD, SpotBugs)
- [ ] Fix all compiler warnings
- [ ] Format code consistently
- [ ] Add missing Javadoc
- [ ] Review error handling
- [ ] Review exception types
- [ ] Check for code duplication

### 7.2 Test Coverage
- [ ] Run code coverage analysis
- [ ] Ensure 90%+ coverage target
- [ ] Add tests for uncovered branches
- [ ] Review test assertions
- [ ] Check for flaky tests

### 7.3 Performance Review
- [ ] Profile tag operations
- [ ] Identify bottlenecks
- [ ] Optimize hot paths
- [ ] Review caching strategy
- [ ] Check for memory leaks

### 7.4 API Review
- [ ] Review method naming consistency
- [ ] Check parameter ordering
- [ ] Verify return types
- [ ] Review exception handling
- [ ] Ensure API is intuitive
- [ ] Check for breaking changes

---

## Definition of Done

A task is considered complete when:
- [ ] Code is implemented and compiles without warnings
- [ ] Unit tests written and passing (90%+ coverage)
- [ ] Integration tests written and passing (if applicable)
- [ ] Javadoc added to all public APIs
- [ ] Code reviewed and approved
- [ ] Performance benchmarks meet targets (if applicable)
- [ ] No known bugs or issues

---

## Dependencies & Prerequisites

- CHEAP Core framework fully functional
- Java 11+ development environment
- JUnit 5 for testing
- Maven or Gradle build system
- Git for version control

---

## Notes & Decisions

### Design Decisions
- Use ImmutablePojoAspectDef for performance (reflection caching)
- Separate hierarchies per ElementType for efficient queries
- EntityDirectoryHierarchy for O(1) lookups
- Immutable POJOs to ensure thread safety
- Builder pattern for complex object construction

### Performance Targets
- Tag lookup by name: < 10ms
- Tag application: < 20ms
- Query with filters: < 100ms for 10k elements
- Memory: < 1MB for 1000 tag definitions

### Future Enhancements (Not in Initial Implementation)
- Tag materialized views
- Tag analytics and statistics
- ML-based tag suggestions
- Custom validation rules per tag
- Tag-based constraints enforcement
- Batch tag operations
- Tag versioning and migration
- Multi-language ports (TypeScript, Python, Rust)

---

## Progress Tracking

- **Start Date**: TBD
- **Target Completion**: TBD (25 days)
- **Current Phase**: Not started
- **Completion Status**: 0% (0/142 tasks complete)

---

## Questions & Blockers

(Track open questions and blockers here as implementation progresses)

- [ ] Q: Should tag applications be mutable (allow metadata updates)?
  - Decision: Keep immutable, remove and re-apply if needed
- [ ] Q: Should we support tag versioning in initial implementation?
  - Decision: No, defer to future enhancement
- [ ] Q: Maximum tag inheritance depth?
  - Decision: No hard limit, but warn if > 10 levels
