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

package net.netbeing.cheap.rest.service;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AspectService.
 */
class AspectServiceTest extends BaseServiceTest
{
    private UUID catalogId;
    private String aspectDefName;

    @BeforeEach
    void setupAspectTest()
    {
        // Create a catalog with an AspectDef
        MutableAspectDef personAspect = factory.createMutableAspectDef(
            "com.example.PersonAspect"
        );
        personAspect.add(factory.createPropertyDef(
            "name", PropertyType.String, true, true, false, false, false
        ));
        personAspect.add(factory.createPropertyDef(
            "age", PropertyType.Integer, true, true, true, false, false
        ));

        CatalogDef catalogDef = factory.createCatalogDef(
            Collections.emptyList(),
            Collections.singletonList(personAspect)
        );

        catalogId = catalogService.createCatalog(catalogDef, CatalogSpecies.SINK, null, URI.create("http://example.com/api/catalog"));
        aspectDefName = "com.example.PersonAspect";
    }

    @Test
    void testUpsertAspectsCreate()
    {
        UUID entityId = UUID.randomUUID();

        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "John Doe");
        properties.put("age", 30);

        Map<UUID, Map<String, Object>> aspectsByEntity = new HashMap<>();
        aspectsByEntity.put(entityId, properties);

        // Upsert
        Map<UUID, AspectService.UpsertResult> results = aspectService.upsertAspects(
            catalogId,
            aspectDefName,
            aspectsByEntity
        );

        // Verify
        assertNotNull(results);
        assertEquals(1, results.size());
        AspectService.UpsertResult result = results.get(entityId);
        assertTrue(result.success());
        assertTrue(result.created());
    }

    @Test
    void testUpsertAspectsUpdate()
    {
        UUID entityId = UUID.randomUUID();

        // First upsert - create
        Map<String, Object> properties1 = new HashMap<>();
        properties1.put("name", "John Doe");
        properties1.put("age", 30);

        Map<UUID, Map<String, Object>> aspectsByEntity1 = new HashMap<>();
        aspectsByEntity1.put(entityId, properties1);

        aspectService.upsertAspects(catalogId, aspectDefName, aspectsByEntity1);

        // Second upsert - update
        Map<String, Object> properties2 = new HashMap<>();
        properties2.put("name", "John Doe");
        properties2.put("age", 31);

        Map<UUID, Map<String, Object>> aspectsByEntity2 = new HashMap<>();
        aspectsByEntity2.put(entityId, properties2);

        Map<UUID, AspectService.UpsertResult> results = aspectService.upsertAspects(
            catalogId,
            aspectDefName,
            aspectsByEntity2
        );

        // Verify
        assertNotNull(results);
        assertEquals(1, results.size());
        AspectService.UpsertResult result = results.get(entityId);
        assertTrue(result.success());
        assertFalse(result.created()); // Should be an update, not create
    }

    @Test
    void testUpsertMultipleAspects()
    {
        UUID entityId1 = UUID.randomUUID();
        UUID entityId2 = UUID.randomUUID();
        UUID entityId3 = UUID.randomUUID();

        Map<UUID, Map<String, Object>> aspectsByEntity = createTestAspectsByEntity(entityId1, entityId2, entityId3);

        // Upsert all
        Map<UUID, AspectService.UpsertResult> results = aspectService.upsertAspects(
            catalogId,
            aspectDefName,
            aspectsByEntity
        );

        // Verify
        assertEquals(3, results.size());
        assertTrue(results.get(entityId1).success());
        assertTrue(results.get(entityId2).success());
        assertTrue(results.get(entityId3).success());
    }

    private static Map<UUID, Map<String, Object>> createTestAspectsByEntity(UUID entityId1, UUID entityId2, UUID entityId3)
    {
        Map<UUID, Map<String, Object>> aspectsByEntity = new HashMap<>();

        Map<String, Object> props1 = new HashMap<>();
        props1.put("name", "John");
        props1.put("age", 30);
        aspectsByEntity.put(entityId1, props1);

        Map<String, Object> props2 = new HashMap<>();
        props2.put("name", "Jane");
        props2.put("age", 28);
        aspectsByEntity.put(entityId2, props2);

        Map<String, Object> props3 = new HashMap<>();
        props3.put("name", "Bob");
        props3.put("age", 35);
        aspectsByEntity.put(entityId3, props3);
        return aspectsByEntity;
    }

    @Test
    void testQueryAspects()
    {
        // Create some aspects
        UUID entityId1 = UUID.randomUUID();
        UUID entityId2 = UUID.randomUUID();

        Map<UUID, Map<String, Object>> aspectsByEntity = new HashMap<>();

        Map<String, Object> props1 = new HashMap<>();
        props1.put("name", "John");
        props1.put("age", 30);
        aspectsByEntity.put(entityId1, props1);

        Map<String, Object> props2 = new HashMap<>();
        props2.put("name", "Jane");
        props2.put("age", 28);
        aspectsByEntity.put(entityId2, props2);

        aspectService.upsertAspects(catalogId, aspectDefName, aspectsByEntity);

        // Query aspects
        Set<UUID> entityIds = new HashSet<>(Arrays.asList(entityId1, entityId2));
        Set<String> aspectDefNames = new HashSet<>(Collections.singletonList(aspectDefName));

        Map<UUID, Map<String, Aspect>> results = aspectService.queryAspects(
            catalogId,
            entityIds,
            aspectDefNames
        );

        // Verify
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.containsKey(entityId1));
        assertTrue(results.containsKey(entityId2));

        Map<String, Aspect> entity1Aspects = results.get(entityId1);
        assertNotNull(entity1Aspects);
        assertTrue(entity1Aspects.containsKey(aspectDefName));

        Map<String, Aspect> entity2Aspects = results.get(entityId2);
        assertNotNull(entity2Aspects);
        assertTrue(entity2Aspects.containsKey(aspectDefName));
    }

    @Test
    void testQueryAspectsPartialResults()
    {
        // Create aspect for only one entity
        UUID entityId1 = UUID.randomUUID();
        UUID entityId2 = UUID.randomUUID(); // This one won't have an aspect

        Map<UUID, Map<String, Object>> aspectsByEntity = new HashMap<>();

        Map<String, Object> props1 = new HashMap<>();
        props1.put("name", "John");
        props1.put("age", 30);
        aspectsByEntity.put(entityId1, props1);

        aspectService.upsertAspects(catalogId, aspectDefName, aspectsByEntity);

        // Query for both entities
        Set<UUID> entityIds = new HashSet<>(Arrays.asList(entityId1, entityId2));
        Set<String> aspectDefNames = new HashSet<>(Collections.singletonList(aspectDefName));

        Map<UUID, Map<String, Aspect>> results = aspectService.queryAspects(
            catalogId,
            entityIds,
            aspectDefNames
        );

        // Verify - should only get results for entityId1
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.containsKey(entityId1));
        assertFalse(results.containsKey(entityId2));
    }
}
