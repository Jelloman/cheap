/*
 * Copyright (c) 2026. David Noha
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

/**
 * CHEAP Tags System - Flexible metadata tagging for CHEAP framework elements.
 *
 * <h2>Overview</h2>
 * <p>The tags system provides a flexible, powerful way to attach metadata to any CHEAP
 * element type (Property, Aspect, Entity, Hierarchy, or Catalog). Tags enable semantic
 * classification, validation rules, security policies, and rich querying capabilities.</p>
 *
 * <h2>Core Concepts</h2>
 *
 * <h3>Tag Definitions</h3>
 * <p>A {@link net.netbeing.cheap.tags.model.TagDefinition} describes a reusable tag with:</p>
 * <ul>
 *   <li>Namespace and name (e.g., "cheap.core.pii")</li>
 *   <li>Description explaining the tag's meaning</li>
 *   <li>Applicable element types (which CHEAP elements can use this tag)</li>
 *   <li>Scope (STANDARD for cheap.core, CUSTOM for application-specific)</li>
 *   <li>Optional parent tags for inheritance hierarchies</li>
 * </ul>
 *
 * <h3>Tag Applications</h3>
 * <p>A {@link net.netbeing.cheap.tags.model.TagApplication} records when a tag is applied to
 * an element, tracking:</p>
 * <ul>
 *   <li>Which tag was applied</li>
 *   <li>Which element received the tag</li>
 *   <li>Optional metadata (key-value pairs)</li>
 *   <li>Source (EXPLICIT, INFERRED, or GENERATED)</li>
 *   <li>Timestamp and user who applied it</li>
 * </ul>
 *
 * <h3>Tag Registry</h3>
 * <p>The {@link net.netbeing.cheap.tags.registry.TagRegistry} manages all tag operations:</p>
 * <ul>
 *   <li>Define new tags</li>
 *   <li>Apply tags to elements</li>
 *   <li>Query elements by tags</li>
 *   <li>Query tags for elements</li>
 *   <li>Validate tag applications</li>
 *   <li>Detect conflicts</li>
 *   <li>Initialize standard tags</li>
 * </ul>
 *
 * <h2>Standard Tags</h2>
 * <p>The system includes 55 standard tags in the {@code cheap.core} namespace,
 * organized into categories:</p>
 * <ul>
 *   <li><b>Identity and Keys</b>: primary-key, foreign-key, composite-key-part, etc.</li>
 *   <li><b>Temporal</b>: created-timestamp, modified-timestamp, version-number, etc.</li>
 *   <li><b>Lifecycle</b>: soft-delete-flag, archived-flag, status-field, etc.</li>
 *   <li><b>Relationships</b>: parent-reference, owner-reference, many-to-many-link, etc.</li>
 *   <li><b>Data Semantics</b>: display-name, sort-order, computed-field, etc.</li>
 *   <li><b>Validation</b>: required, unique, immutable, range-bounded, etc.</li>
 *   <li><b>Security</b>: pii, sensitive, encrypted, audit-logged, masked, etc.</li>
 *   <li><b>Business Domain</b>: monetary-amount, email-address, postal-address, etc.</li>
 *   <li><b>Technical</b>: indexed, searchable, lazy-loaded, cached, etc.</li>
 * </ul>
 *
 * <h2>Tag Inheritance</h2>
 * <p>Tags can inherit from parent tags, creating taxonomies:</p>
 * <pre>{@code
 * // Define a hierarchy
 * TagDefinition sensitiveData = new TagDefinition(
 *     "myapp", "sensitive-data", "Sensitive data", ...);
 *
 * TagDefinition piiData = new TagDefinition(
 *     "myapp", "pii-data", "PII data", ...,
 *     List.of(sensitiveDataId)); // Inherits from sensitive-data
 * }</pre>
 *
 * <h2>Validation and Conflicts</h2>
 * <p>The system validates tag applications and detects conflicts:</p>
 * <ul>
 *   <li>Namespace and name format validation</li>
 *   <li>Tag applicability to element types</li>
 *   <li>Circular inheritance detection</li>
 *   <li>Semantic conflict detection (e.g., immutable + modified-timestamp)</li>
 * </ul>
 *
 * <h2>Querying</h2>
 * <p>Powerful query builder for finding elements by tags:</p>
 * <pre>{@code
 * TagQuery query = new TagQuery(registry)
 *     .forType(ElementType.PROPERTY)
 *     .withTag(piiTagId)
 *     .withTag(encryptedTagId)
 *     .inNamespace("myapp.security")
 *     .fromSource(TagSource.EXPLICIT);
 *
 * TagQueryResult result = query.execute();
 * for (UUID elementId : result.getElements()) {
 *     // Process PII properties that are encrypted
 * }
 * }</pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // 1. Create registry
 * TagRegistry registry = TagRegistry.create(catalog, factory);
 *
 * // 2. Initialize standard tags
 * registry.initializeStandardTags();
 *
 * // 3. Define custom tag
 * TagDefinition customerTag = new TagDefinition(
 *     "myapp.domain",
 *     "customer-data",
 *     "Customer-related data",
 *     List.of(ElementType.PROPERTY, ElementType.ASPECT),
 *     TagScope.CUSTOM,
 *     null,
 *     null
 * );
 * Entity tagEntity = registry.defineTag(customerTag);
 *
 * // 4. Apply tag to element
 * UUID propertyId = ...;
 * registry.applyTag(propertyId, ElementType.PROPERTY,
 *     tagEntity.globalId(), null, TagSource.EXPLICIT);
 *
 * // 5. Query tags
 * Collection<TagApplication> tags = registry.getTagsForElement(
 *     propertyId, ElementType.PROPERTY);
 *
 * // 6. Find elements by tag
 * Collection<UUID> elements = registry.getElementsByTag(
 *     tagEntity.globalId(), ElementType.PROPERTY);
 * }</pre>
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li>Tag lookup by name: O(1) via EntityDirectoryHierarchy</li>
 *   <li>Tag application: O(1) idempotent operation</li>
 *   <li>Element → tags query: O(1) via EntitySetHierarchy index</li>
 *   <li>Tag → elements query: O(1) via EntitySetHierarchy index</li>
 *   <li>Complex queries: O(n) where n is candidate element count</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All tag POJOs ({@link net.netbeing.cheap.tags.model.TagDefinition},
 * {@link net.netbeing.cheap.tags.model.TagApplication}) are immutable and thread-safe.
 * The {@link net.netbeing.cheap.tags.registry.TagRegistry} delegates thread safety
 * to underlying CHEAP catalog operations.</p>
 *
 * <h2>Storage Strategy</h2>
 * <p>Tags are stored using CHEAP-native hierarchies:</p>
 * <ul>
 *   <li><b>AspectMapHierarchy</b>: Stores tag definitions and applications</li>
 *   <li><b>EntityDirectoryHierarchy</b>: Name-based tag lookup index</li>
 *   <li><b>EntitySetHierarchy</b>: Bidirectional element↔tag indices</li>
 * </ul>
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@link net.netbeing.cheap.tags.model} - Core data models (enums, POJOs)</li>
 *   <li>{@link net.netbeing.cheap.tags.aspect} - CHEAP aspect wrappers</li>
 *   <li>{@link net.netbeing.cheap.tags.registry} - Tag registry implementation</li>
 *   <li>{@link net.netbeing.cheap.tags.validation} - Validation and conflict detection</li>
 *   <li>{@link net.netbeing.cheap.tags.query} - Query builder and results</li>
 *   <li>{@link net.netbeing.cheap.tags.standard} - Standard tag definitions</li>
 * </ul>
 *
 * @since 1.0
 * @see net.netbeing.cheap.tags.registry.TagRegistry
 * @see net.netbeing.cheap.tags.model.TagDefinition
 * @see net.netbeing.cheap.tags.model.TagApplication
 * @see net.netbeing.cheap.tags.standard.StandardTags
 */
package net.netbeing.cheap.tags;
