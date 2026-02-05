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

package net.netbeing.cheap.tags.standard;

import net.netbeing.cheap.tags.model.ElementType;
import net.netbeing.cheap.tags.model.TagDefinition;
import net.netbeing.cheap.tags.model.TagScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Standard tag definitions in the "cheap.core" namespace.
 *
 * <p>This class provides a curated set of 60+ standard tags organized into
 * semantic categories. These tags cover common metadata patterns for:</p>
 * <ul>
 *   <li>Identity and Keys (primary keys, foreign keys, etc.)</li>
 *   <li>Temporal and Versioning (timestamps, version numbers, etc.)</li>
 *   <li>Lifecycle and State (soft deletes, status flags, etc.)</li>
 *   <li>Relationships (parent references, polymorphic links, etc.)</li>
 *   <li>Data Semantics (display names, sort order, etc.)</li>
 *   <li>Validation and Constraints (required, unique, immutable, etc.)</li>
 *   <li>Security and Privacy (PII, encryption, audit logging, etc.)</li>
 *   <li>Business Domain (monetary amounts, email addresses, etc.)</li>
 *   <li>Technical Behavior (indexing, caching, lazy loading, etc.)</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * // Get all standard tags
 * Collection<TagDefinition> allTags = StandardTags.allStandardTags();
 *
 * // Get specific tag by name
 * TagDefinition piiTag = StandardTags.getStandardTag("pii");
 *
 * // Initialize in registry
 * registry.initializeStandardTags();
 * }</pre>
 */
public class StandardTags
{
    private static final String NAMESPACE = "cheap.core";

    // ==================== Identity and Keys ====================

    public static final TagDefinition PRIMARY_KEY = new TagDefinition(
        NAMESPACE,
        "primary-key",
        "Primary key field that uniquely identifies an entity",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition FOREIGN_KEY = new TagDefinition(
        NAMESPACE,
        "foreign-key",
        "Foreign key field that references another entity",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition COMPOSITE_KEY_PART = new TagDefinition(
        NAMESPACE,
        "composite-key-part",
        "Part of a composite primary key",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition NATURAL_KEY = new TagDefinition(
        NAMESPACE,
        "natural-key",
        "Natural key derived from business data",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition SURROGATE_KEY = new TagDefinition(
        NAMESPACE,
        "surrogate-key",
        "Surrogate key with no business meaning",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition ALTERNATE_KEY = new TagDefinition(
        NAMESPACE,
        "alternate-key",
        "Alternate unique identifier for an entity",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    // ==================== Temporal and Versioning ====================

    public static final TagDefinition CREATED_TIMESTAMP = new TagDefinition(
        NAMESPACE,
        "created-timestamp",
        "Timestamp when the entity was created",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition MODIFIED_TIMESTAMP = new TagDefinition(
        NAMESPACE,
        "modified-timestamp",
        "Timestamp when the entity was last modified",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition VERSION_NUMBER = new TagDefinition(
        NAMESPACE,
        "version-number",
        "Version number for optimistic locking",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition EFFECTIVE_DATE = new TagDefinition(
        NAMESPACE,
        "effective-date",
        "Date when the entity becomes effective",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition EXPIRATION_DATE = new TagDefinition(
        NAMESPACE,
        "expiration-date",
        "Date when the entity expires or becomes invalid",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition TEMPORAL_RANGE_START = new TagDefinition(
        NAMESPACE,
        "temporal-range-start",
        "Start of a temporal validity range",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition TEMPORAL_RANGE_END = new TagDefinition(
        NAMESPACE,
        "temporal-range-end",
        "End of a temporal validity range",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    // ==================== Lifecycle and State ====================

    public static final TagDefinition SOFT_DELETE_FLAG = new TagDefinition(
        NAMESPACE,
        "soft-delete-flag",
        "Flag indicating logical deletion without physical removal",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition ARCHIVED_FLAG = new TagDefinition(
        NAMESPACE,
        "archived-flag",
        "Flag indicating the entity has been archived",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition STATUS_FIELD = new TagDefinition(
        NAMESPACE,
        "status-field",
        "Field representing entity lifecycle status",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition APPROVAL_STATUS = new TagDefinition(
        NAMESPACE,
        "approval-status",
        "Field tracking approval workflow status",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition PUBLISHED_FLAG = new TagDefinition(
        NAMESPACE,
        "published-flag",
        "Flag indicating whether the entity is published",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    // ==================== Relationships ====================

    public static final TagDefinition PARENT_REFERENCE = new TagDefinition(
        NAMESPACE,
        "parent-reference",
        "Reference to a parent entity in a hierarchy",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition OWNER_REFERENCE = new TagDefinition(
        NAMESPACE,
        "owner-reference",
        "Reference to the owning entity",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition MANY_TO_MANY_LINK = new TagDefinition(
        NAMESPACE,
        "many-to-many-link",
        "Join table or link entity for many-to-many relationship",
        List.of(ElementType.ENTITY, ElementType.ASPECT),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition POLYMORPHIC_REFERENCE = new TagDefinition(
        NAMESPACE,
        "polymorphic-reference",
        "Reference that can point to multiple entity types",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition SELF_REFERENCE = new TagDefinition(
        NAMESPACE,
        "self-reference",
        "Reference to another entity of the same type",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    // ==================== Data Semantics ====================

    public static final TagDefinition DISPLAY_NAME = new TagDefinition(
        NAMESPACE,
        "display-name",
        "Human-readable display name or title",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition DESCRIPTION_FIELD = new TagDefinition(
        NAMESPACE,
        "description-field",
        "Descriptive text field providing additional information",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition SORT_ORDER = new TagDefinition(
        NAMESPACE,
        "sort-order",
        "Field used for default sorting or display order",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition CODE_VALUE = new TagDefinition(
        NAMESPACE,
        "code-value",
        "Machine-readable code or identifier",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition COMPUTED_FIELD = new TagDefinition(
        NAMESPACE,
        "computed-field",
        "Field whose value is computed or derived",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition DENORMALIZED_CACHE = new TagDefinition(
        NAMESPACE,
        "denormalized-cache",
        "Denormalized data cached for performance",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    // ==================== Validation and Constraints ====================

    public static final TagDefinition REQUIRED = new TagDefinition(
        NAMESPACE,
        "required",
        "Field that must have a value (not null)",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition NULLABLE = new TagDefinition(
        NAMESPACE,
        "nullable",
        "Field that may be null or empty",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition UNIQUE = new TagDefinition(
        NAMESPACE,
        "unique",
        "Field with unique constraint (no duplicates)",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition IMMUTABLE = new TagDefinition(
        NAMESPACE,
        "immutable",
        "Field that cannot be modified after creation",
        List.of(ElementType.PROPERTY, ElementType.ASPECT, ElementType.ENTITY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition RANGE_BOUNDED = new TagDefinition(
        NAMESPACE,
        "range-bounded",
        "Field with minimum and maximum value constraints",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition FORMAT_CONSTRAINED = new TagDefinition(
        NAMESPACE,
        "format-constrained",
        "Field with specific format or pattern requirements",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition ENUM_VALUED = new TagDefinition(
        NAMESPACE,
        "enum-valued",
        "Field limited to a fixed set of values",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    // ==================== Security and Privacy ====================

    public static final TagDefinition PII = new TagDefinition(
        NAMESPACE,
        "pii",
        "Personally Identifiable Information requiring special protection",
        List.of(ElementType.PROPERTY, ElementType.ASPECT, ElementType.ENTITY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition SENSITIVE = new TagDefinition(
        NAMESPACE,
        "sensitive",
        "Sensitive data requiring access controls",
        List.of(ElementType.PROPERTY, ElementType.ASPECT, ElementType.ENTITY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition ENCRYPTED = new TagDefinition(
        NAMESPACE,
        "encrypted",
        "Data that is encrypted at rest",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition AUDIT_LOGGED = new TagDefinition(
        NAMESPACE,
        "audit-logged",
        "Changes to this data are audit logged",
        List.of(ElementType.PROPERTY, ElementType.ASPECT, ElementType.ENTITY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition MASKED = new TagDefinition(
        NAMESPACE,
        "masked",
        "Data that should be masked in displays and logs",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition ANONYMIZABLE = new TagDefinition(
        NAMESPACE,
        "anonymizable",
        "Data that can be anonymized for privacy compliance",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    // ==================== Business Domain ====================

    public static final TagDefinition MONETARY_AMOUNT = new TagDefinition(
        NAMESPACE,
        "monetary-amount",
        "Monetary value with currency considerations",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition QUANTITY = new TagDefinition(
        NAMESPACE,
        "quantity",
        "Numeric quantity with unit of measure",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition PERCENTAGE = new TagDefinition(
        NAMESPACE,
        "percentage",
        "Percentage value (typically 0-100)",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition EMAIL_ADDRESS = new TagDefinition(
        NAMESPACE,
        "email-address",
        "Email address requiring format validation",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition PHONE_NUMBER = new TagDefinition(
        NAMESPACE,
        "phone-number",
        "Phone number with formatting requirements",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition POSTAL_ADDRESS = new TagDefinition(
        NAMESPACE,
        "postal-address",
        "Physical mailing address",
        List.of(ElementType.PROPERTY, ElementType.ASPECT),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition URL = new TagDefinition(
        NAMESPACE,
        "url",
        "URL or web address",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition GEO_COORDINATE = new TagDefinition(
        NAMESPACE,
        "geo-coordinate",
        "Geographic coordinate (latitude/longitude)",
        List.of(ElementType.PROPERTY, ElementType.ASPECT),
        TagScope.STANDARD,
        null,
        null
    );

    // ==================== Technical Behavior ====================

    public static final TagDefinition INDEXED = new TagDefinition(
        NAMESPACE,
        "indexed",
        "Field with database index for query performance",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition SEARCHABLE = new TagDefinition(
        NAMESPACE,
        "searchable",
        "Field included in full-text search indexes",
        List.of(ElementType.PROPERTY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition LAZY_LOADED = new TagDefinition(
        NAMESPACE,
        "lazy-loaded",
        "Data loaded on-demand rather than eagerly",
        List.of(ElementType.PROPERTY, ElementType.ASPECT),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition CACHED = new TagDefinition(
        NAMESPACE,
        "cached",
        "Data that is cached for performance",
        List.of(ElementType.PROPERTY, ElementType.ASPECT, ElementType.ENTITY),
        TagScope.STANDARD,
        null,
        null
    );

    public static final TagDefinition IMMUTABLE_AGGREGATE_ROOT = new TagDefinition(
        NAMESPACE,
        "immutable-aggregate-root",
        "Root entity of an immutable aggregate",
        List.of(ElementType.ENTITY),
        TagScope.STANDARD,
        null,
        null
    );

    // ==================== Static Registry ====================

    private static final Map<String, TagDefinition> TAG_REGISTRY;

    static {
        Map<String, TagDefinition> tags = new HashMap<>();

        // Identity and Keys
        tags.put("primary-key", PRIMARY_KEY);
        tags.put("foreign-key", FOREIGN_KEY);
        tags.put("composite-key-part", COMPOSITE_KEY_PART);
        tags.put("natural-key", NATURAL_KEY);
        tags.put("surrogate-key", SURROGATE_KEY);
        tags.put("alternate-key", ALTERNATE_KEY);

        // Temporal and Versioning
        tags.put("created-timestamp", CREATED_TIMESTAMP);
        tags.put("modified-timestamp", MODIFIED_TIMESTAMP);
        tags.put("version-number", VERSION_NUMBER);
        tags.put("effective-date", EFFECTIVE_DATE);
        tags.put("expiration-date", EXPIRATION_DATE);
        tags.put("temporal-range-start", TEMPORAL_RANGE_START);
        tags.put("temporal-range-end", TEMPORAL_RANGE_END);

        // Lifecycle and State
        tags.put("soft-delete-flag", SOFT_DELETE_FLAG);
        tags.put("archived-flag", ARCHIVED_FLAG);
        tags.put("status-field", STATUS_FIELD);
        tags.put("approval-status", APPROVAL_STATUS);
        tags.put("published-flag", PUBLISHED_FLAG);

        // Relationships
        tags.put("parent-reference", PARENT_REFERENCE);
        tags.put("owner-reference", OWNER_REFERENCE);
        tags.put("many-to-many-link", MANY_TO_MANY_LINK);
        tags.put("polymorphic-reference", POLYMORPHIC_REFERENCE);
        tags.put("self-reference", SELF_REFERENCE);

        // Data Semantics
        tags.put("display-name", DISPLAY_NAME);
        tags.put("description-field", DESCRIPTION_FIELD);
        tags.put("sort-order", SORT_ORDER);
        tags.put("code-value", CODE_VALUE);
        tags.put("computed-field", COMPUTED_FIELD);
        tags.put("denormalized-cache", DENORMALIZED_CACHE);

        // Validation and Constraints
        tags.put("required", REQUIRED);
        tags.put("nullable", NULLABLE);
        tags.put("unique", UNIQUE);
        tags.put("immutable", IMMUTABLE);
        tags.put("range-bounded", RANGE_BOUNDED);
        tags.put("format-constrained", FORMAT_CONSTRAINED);
        tags.put("enum-valued", ENUM_VALUED);

        // Security and Privacy
        tags.put("pii", PII);
        tags.put("sensitive", SENSITIVE);
        tags.put("encrypted", ENCRYPTED);
        tags.put("audit-logged", AUDIT_LOGGED);
        tags.put("masked", MASKED);
        tags.put("anonymizable", ANONYMIZABLE);

        // Business Domain
        tags.put("monetary-amount", MONETARY_AMOUNT);
        tags.put("quantity", QUANTITY);
        tags.put("percentage", PERCENTAGE);
        tags.put("email-address", EMAIL_ADDRESS);
        tags.put("phone-number", PHONE_NUMBER);
        tags.put("postal-address", POSTAL_ADDRESS);
        tags.put("url", URL);
        tags.put("geo-coordinate", GEO_COORDINATE);

        // Technical Behavior
        tags.put("indexed", INDEXED);
        tags.put("searchable", SEARCHABLE);
        tags.put("lazy-loaded", LAZY_LOADED);
        tags.put("cached", CACHED);
        tags.put("immutable-aggregate-root", IMMUTABLE_AGGREGATE_ROOT);

        TAG_REGISTRY = Collections.unmodifiableMap(tags);
    }

    /**
     * Returns all standard tag definitions.
     *
     * @return unmodifiable collection of all 60+ standard tags
     */
    @NotNull
    public static Collection<TagDefinition> allStandardTags()
    {
        return TAG_REGISTRY.values();
    }

    /**
     * Retrieves a standard tag by its name (without namespace).
     *
     * @param name the tag name (e.g., "primary-key", "pii")
     * @return the tag definition, or null if not found
     */
    @Nullable
    public static TagDefinition getStandardTag(@NotNull String name)
    {
        Objects.requireNonNull(name, "name cannot be null");
        return TAG_REGISTRY.get(name);
    }

    /**
     * Checks if a tag name corresponds to a standard tag.
     *
     * @param name the tag name to check
     * @return true if the tag is a standard tag, false otherwise
     */
    public static boolean isStandardTag(@NotNull String name)
    {
        Objects.requireNonNull(name, "name cannot be null");
        return TAG_REGISTRY.containsKey(name);
    }

    /**
     * Returns the count of standard tags.
     *
     * @return number of standard tags defined
     */
    public static int getStandardTagCount()
    {
        return TAG_REGISTRY.size();
    }

    // Private constructor to prevent instantiation
    private StandardTags()
    {
        throw new UnsupportedOperationException("StandardTags is a utility class");
    }
}
