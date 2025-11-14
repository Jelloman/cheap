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

import net.netbeing.cheap.json.dto.UpsertAspectsResponse.AspectResult;
import net.netbeing.cheap.model.Aspect;
import net.netbeing.cheap.model.AspectMap;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.MutableAspectDef;
import net.netbeing.cheap.model.PropertyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AspectService.
 */
class AspectServiceTest extends BaseServiceTest
{
    private UUID catalogId;
    private String aspectDefName;
    private MutableAspectDef personAspect;

    @BeforeEach
    void setupAspectTest()
    {
        // Create a catalog with an AspectDef
        aspectDefName = "com.example.PersonAspect";
        personAspect = factory.createMutableAspectDef(aspectDefName);
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
        Map<UUID, AspectResult> results = aspectService.upsertAspects(
            catalogId,
            aspectDefName,
            aspectsByEntity
        );

        // Verify
        assertNotNull(results);
        assertEquals(1, results.size());
        AspectResult result = results.get(entityId);
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

        Map<UUID, AspectResult> results = aspectService.upsertAspects(
            catalogId,
            aspectDefName,
            aspectsByEntity2
        );

        // Verify
        assertNotNull(results);
        assertEquals(1, results.size());
        AspectResult result = results.get(entityId);
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
        Map<UUID, AspectResult> results = aspectService.upsertAspects(
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
        Entity entity1 = factory.createEntity(entityId1);
        Entity entity2 = factory.createEntity(entityId2);

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

        List<AspectMap> results = aspectService.queryAspects(
            catalogId,
            entityIds,
            aspectDefNames
        );

        // Verify
        assertNotNull(results);
        assertEquals(1, results.size());

        AspectMap resultMap = results.getFirst();
        assertEquals(2, resultMap.size());
        assertTrue(resultMap.containsKey(entity1));
        assertTrue(resultMap.containsKey(entity2));

        Aspect entity1Aspect = resultMap.get(entity1);
        assertNotNull(entity1Aspect);
        assertEquals(entity1Aspect.def(), personAspect);

        Aspect entity2Aspect = resultMap.get(entity2);
        assertNotNull(entity2Aspect);
        assertEquals(entity2Aspect.def(), personAspect);
    }

    @Test
    void testQueryAspectsPartialResults()
    {
        // Create aspect for only one entity
        UUID entityId1 = UUID.randomUUID();
        UUID entityId2 = UUID.randomUUID(); // This one won't have an aspect
        Entity entity1 = factory.createEntity(entityId1);
        Entity entity2 = factory.createEntity(entityId2);

        Map<UUID, Map<String, Object>> aspectsByEntity = new HashMap<>();

        Map<String, Object> props1 = new HashMap<>();
        props1.put("name", "John");
        props1.put("age", 30);
        aspectsByEntity.put(entityId1, props1);

        aspectService.upsertAspects(catalogId, aspectDefName, aspectsByEntity);

        // Query for both entities
        Set<UUID> entityIds = new HashSet<>(Arrays.asList(entityId1, entityId2));
        Set<String> aspectDefNames = new HashSet<>(Collections.singletonList(aspectDefName));

        List<AspectMap> results = aspectService.queryAspects(
            catalogId,
            entityIds,
            aspectDefNames
        );

        // Verify - should only get results for entityId1
        assertNotNull(results);
        assertEquals(1, results.size());

        AspectMap resultMap = results.getFirst();
        assertEquals(1, resultMap.size());
        assertTrue(resultMap.containsKey(entity1));
        assertFalse(resultMap.containsKey(entity2));
    }
}
