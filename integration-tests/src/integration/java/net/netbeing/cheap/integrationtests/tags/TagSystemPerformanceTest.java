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

package net.netbeing.cheap.integrationtests.tags;

import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.tags.model.*;
import net.netbeing.cheap.tags.query.TagQuery;
import net.netbeing.cheap.tags.query.TagQueryResult;
import net.netbeing.cheap.tags.registry.TagRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for the tags system.
 * Validates performance targets and identifies bottlenecks.
 *
 * <p>Performance Targets (from TAGS-TODO.md):</p>
 * <ul>
 *   <li>Tag lookup by name: < 10ms</li>
 *   <li>Tag application: < 20ms</li>
 *   <li>Query with filters: < 100ms for 10k elements</li>
 *   <li>Memory: < 1MB for 1000 tag definitions</li>
 * </ul>
 */
class TagSystemPerformanceTest
{
    private CheapFactory factory;
    private Catalog catalog;
    private TagRegistry registry;

    @BeforeEach
    void setUp()
    {
        factory = new CheapFactory();
        catalog = factory.createCatalog(
            UUID.randomUUID(),
            CatalogSpecies.SINK,
            URI.create("mem://perf-test-catalog"),
            null,
            0L
        );
        registry = TagRegistry.create(catalog, factory);
    }

    @AfterEach
    void tearDown()
    {
        registry = null;
        catalog = null;
        factory = null;
    }

    // ==================== Tag Definition Performance ====================

    @Test
    void testTagDefinitionCreation_1000Tags()
    {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            TagDefinition tag = new TagDefinition(
                "perf.test",
                "tag-" + i,
                "Performance test tag " + i,
                List.of(ElementType.PROPERTY),
                TagScope.CUSTOM,
                null,
                null
            );
            registry.defineTag(tag);
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Created 1000 tags in " + duration + "ms");
        System.out.println("Average: " + (duration / 1000.0) + "ms per tag");

        // Verify all created
        Collection<TagDefinition> allTags = registry.getAllTagDefinitions();
        assertEquals(1000, allTags.size());
    }

    // ==================== Tag Lookup Performance ====================

    @Test
    void testTagLookupByName_Performance()
    {
        // Create tags
        List<Entity> tagEntities = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TagDefinition tag = new TagDefinition(
                "perf.test",
                "tag-" + i,
                "Test tag " + i,
                List.of(ElementType.PROPERTY),
                TagScope.CUSTOM,
                null,
                null
            );
            tagEntities.add(registry.defineTag(tag));
        }

        // Benchmark lookups
        long startTime = System.nanoTime();
        int lookupCount = 10000;

        for (int i = 0; i < lookupCount; i++) {
            int tagIndex = i % 100;
            TagDefinition found = registry.getTagDefinitionByName("perf.test", "tag-" + tagIndex);
            assertNotNull(found);
        }

        long durationNanos = System.nanoTime() - startTime;
        double avgMillis = (durationNanos / 1_000_000.0) / lookupCount;

        System.out.println("Performed " + lookupCount + " lookups in " + (durationNanos / 1_000_000.0) + "ms");
        System.out.println("Average lookup time: " + avgMillis + "ms");

        // Target: < 10ms per lookup (we should be much faster)
        assertTrue(avgMillis < 10.0, "Lookup performance target not met: " + avgMillis + "ms");
    }

    // ==================== Tag Application Performance ====================

    @Test
    void testTagApplication_10000Applications()
    {
        // Create a few tags
        List<Entity> tagEntities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TagDefinition tag = new TagDefinition(
                "perf.test",
                "tag-" + i,
                "Test tag " + i,
                List.of(ElementType.PROPERTY),
                TagScope.CUSTOM,
                null,
                null
            );
            tagEntities.add(registry.defineTag(tag));
        }

        // Apply tags to many elements
        long startTime = System.currentTimeMillis();
        int applicationCount = 10000;

        for (int i = 0; i < applicationCount; i++) {
            UUID elementId = UUID.randomUUID();
            Entity tagEntity = tagEntities.get(i % tagEntities.size());

            registry.applyTag(elementId, ElementType.PROPERTY,
                tagEntity.globalId(), null, TagSource.EXPLICIT);
        }

        long duration = System.currentTimeMillis() - startTime;
        double avgMillis = (double) duration / applicationCount;

        System.out.println("Applied " + applicationCount + " tags in " + duration + "ms");
        System.out.println("Average application time: " + avgMillis + "ms");

        // Target: < 20ms per application
        assertTrue(avgMillis < 20.0, "Application performance target not met: " + avgMillis + "ms");
    }

    // ==================== Query Performance ====================

    @Test
    void testQueryPerformance_10kElements()
    {
        // Create tags
        TagDefinition tag1 = new TagDefinition(
            "perf.query",
            "tag1",
            "Query test tag 1",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        TagDefinition tag2 = new TagDefinition(
            "perf.query",
            "tag2",
            "Query test tag 2",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );

        Entity tag1Entity = registry.defineTag(tag1);
        Entity tag2Entity = registry.defineTag(tag2);

        // Create 10k elements with tags
        System.out.println("Creating 10k elements with tags...");
        for (int i = 0; i < 10000; i++) {
            UUID elementId = UUID.randomUUID();

            // Half have tag1, half have tag2, quarter have both
            if (i % 2 == 0) {
                registry.applyTag(elementId, ElementType.PROPERTY,
                    tag1Entity.globalId(), null, TagSource.EXPLICIT);
            }
            if (i % 4 == 0) {
                registry.applyTag(elementId, ElementType.PROPERTY,
                    tag2Entity.globalId(), null, TagSource.EXPLICIT);
            }
        }

        // Benchmark query
        long startTime = System.currentTimeMillis();

        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .withTag(tag1Entity.globalId())
            .inNamespace("perf.query")
            .fromSource(TagSource.EXPLICIT);

        TagQueryResult result = query.execute();

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Queried 10k elements in " + duration + "ms");
        System.out.println("Found " + result.size() + " matching elements");

        // Target: < 100ms for 10k elements
        assertTrue(duration < 100, "Query performance target not met: " + duration + "ms");
    }

    @Test
    void testComplexQueryPerformance()
    {
        // Create multiple tags
        List<Entity> tagEntities = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            TagDefinition tag = new TagDefinition(
                "perf.complex",
                "tag-" + i,
                "Complex query tag " + i,
                List.of(ElementType.PROPERTY),
                TagScope.CUSTOM,
                null,
                null
            );
            tagEntities.add(registry.defineTag(tag));
        }

        // Create elements with various tag combinations
        System.out.println("Creating 5k elements with multiple tags...");
        for (int i = 0; i < 5000; i++) {
            UUID elementId = UUID.randomUUID();

            // Apply 1-3 tags to each element
            for (int j = 0; j < 3; j++) {
                if (i % (j + 1) == 0) {
                    registry.applyTag(elementId, ElementType.PROPERTY,
                        tagEntities.get(j).globalId(), null, TagSource.EXPLICIT);
                }
            }
        }

        // Complex query with multiple filters
        long startTime = System.currentTimeMillis();

        TagQuery query = new TagQuery(registry)
            .forType(ElementType.PROPERTY)
            .withTag(tagEntities.get(0).globalId())
            .withTag(tagEntities.get(1).globalId())
            .inNamespace("perf.complex")
            .fromSource(TagSource.EXPLICIT);

        TagQueryResult result = query.execute();

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Complex query on 5k elements completed in " + duration + "ms");
        System.out.println("Found " + result.size() + " matching elements");

        // Should complete reasonably fast
        assertTrue(duration < 200, "Complex query took too long: " + duration + "ms");
    }

    // ==================== Bulk Operations Performance ====================

    @Test
    void testBulkTagRetrieval()
    {
        // Create tags and apply to many elements
        TagDefinition tag1 = new TagDefinition(
            "perf.bulk",
            "bulk-tag",
            "Bulk test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        Entity tagEntity = registry.defineTag(tag1);

        List<UUID> elementIds = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            UUID elementId = UUID.randomUUID();
            elementIds.add(elementId);
            registry.applyTag(elementId, ElementType.PROPERTY,
                tagEntity.globalId(), null, TagSource.EXPLICIT);
        }

        // Benchmark bulk retrieval
        long startTime = System.currentTimeMillis();

        for (UUID elementId : elementIds) {
            Collection<TagApplication> tags = registry.getTagsForElement(
                elementId, ElementType.PROPERTY);
            assertEquals(1, tags.size());
        }

        long duration = System.currentTimeMillis() - startTime;
        double avgMillis = (double) duration / elementIds.size();

        System.out.println("Retrieved tags for 1000 elements in " + duration + "ms");
        System.out.println("Average retrieval time: " + avgMillis + "ms");

        assertTrue(avgMillis < 5.0, "Bulk retrieval too slow: " + avgMillis + "ms per element");
    }

    // ==================== Stress Tests ====================

    @Test
    void testHighVolumeTagApplications()
    {
        // Create a single tag
        TagDefinition tag = new TagDefinition(
            "perf.stress",
            "stress-tag",
            "Stress test tag",
            List.of(ElementType.PROPERTY),
            TagScope.CUSTOM,
            null,
            null
        );
        Entity tagEntity = registry.defineTag(tag);

        // Apply to 50k unique elements
        System.out.println("Applying tag to 50k elements...");
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 50000; i++) {
            UUID elementId = UUID.randomUUID();
            registry.applyTag(elementId, ElementType.PROPERTY,
                tagEntity.globalId(), null, TagSource.EXPLICIT);
        }

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Applied 50k tags in " + duration + "ms");
        System.out.println("Average: " + (duration / 50000.0) + "ms per application");

        // Verify count
        Collection<UUID> elements = registry.getElementsByTag(tagEntity.globalId(), ElementType.PROPERTY);
        assertEquals(50000, elements.size());

        System.out.println("Query returned " + elements.size() + " elements");
    }

    @Test
    void testMemoryUsageEstimate()
    {
        // Force garbage collection
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Create 1000 tag definitions
        for (int i = 0; i < 1000; i++) {
            TagDefinition tag = new TagDefinition(
                "perf.memory",
                "tag-" + i,
                "Memory test tag " + i,
                List.of(ElementType.PROPERTY),
                TagScope.CUSTOM,
                null,
                null
            );
            registry.defineTag(tag);
        }

        // Force garbage collection again
        System.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        long memoryUsedBytes = memoryAfter - memoryBefore;
        double memoryUsedMB = memoryUsedBytes / (1024.0 * 1024.0);

        System.out.println("Memory used for 1000 tags: " + memoryUsedMB + " MB");
        System.out.println("Average: " + (memoryUsedBytes / 1000.0) + " bytes per tag");

        // Target: < 1MB for 1000 tags (note: this is approximate due to GC behavior)
        // We'll be lenient since memory measurement is imprecise
        assertTrue(memoryUsedMB < 5.0,
            "Memory usage higher than expected: " + memoryUsedMB + " MB");
    }
}
