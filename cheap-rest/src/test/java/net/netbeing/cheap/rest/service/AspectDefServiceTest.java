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

import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.MutableAspectDef;
import net.netbeing.cheap.model.PropertyType;
import net.netbeing.cheap.rest.exception.ResourceConflictException;
import net.netbeing.cheap.rest.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AspectDefService.
 */
class AspectDefServiceTest extends BaseServiceTest
{
    private UUID setupTestCatalog()
    {
        CatalogDef catalogDef = factory.createCatalogDef(Collections.emptyList(), Collections.emptyList());
        return catalogService.createCatalog(catalogDef, CatalogSpecies.SINK, null, URI.create("http://example" +
            ".com/api/catalog"));
    }

    @Test
    void testCreateAspectDef()
    {
        UUID catalogId = setupTestCatalog();

        // Create AspectDef
        MutableAspectDef aspectDef = factory.createMutableAspectDef("com.example.TestAspect");
        aspectDef.add(factory.createPropertyDef("testProp", PropertyType.String, true, true, false, false, false));

        // Create AspectDef in catalog
        AspectDef created = aspectDefService.createAspectDef(catalogId, aspectDef);

        // Verify
        assertNotNull(created);
        assertEquals(aspectDef.name(), created.name());
        assertEquals(aspectDef.globalId(), created.globalId());
    }

    @Test
    void testCreateAspectDefCatalogNotFound()
    {
        UUID nonExistentCatalogId = UUID.randomUUID();

        MutableAspectDef aspectDef = factory.createMutableAspectDef("com.example.TestAspect");
        aspectDef.add(factory.createPropertyDef("testProp", PropertyType.String, true, true, false, false, false));

        assertThrows(ResourceNotFoundException.class, () -> aspectDefService.createAspectDef(nonExistentCatalogId,
            aspectDef));
    }

    @Test
    void testCreateDuplicateAspectDef()
    {
        UUID catalogId = setupTestCatalog();

        MutableAspectDef aspectDef = factory.createMutableAspectDef("com.example.TestAspect");
        aspectDef.add(factory.createPropertyDef("testProp", PropertyType.String, true, true, false, false, false));

        // Create first time - should succeed
        aspectDefService.createAspectDef(catalogId, aspectDef);

        // Create second time with same name - should fail
        MutableAspectDef duplicate = factory.createMutableAspectDef("com.example.TestAspect", UUID.randomUUID(),
            new HashMap<>());
        duplicate.add(factory.createPropertyDef("prop", PropertyType.String, true, true, false, false, false));

        assertThrows(ResourceConflictException.class, () -> aspectDefService.createAspectDef(catalogId, duplicate));
    }

    @Test
    void testListAspectDefs()
    {
        UUID catalogId = setupTestCatalog();

        // Create multiple AspectDefs
        for (int i = 0; i < 3; i++) {
            MutableAspectDef aspectDef = factory.createMutableAspectDef("com.example.Aspect" + i);
            aspectDef.add(factory.createPropertyDef("prop", PropertyType.String, true, true, false, false, false));
            aspectDefService.createAspectDef(catalogId, aspectDef);
        }

        // List AspectDefs
        List<AspectDef> aspectDefs = aspectDefService.listAspectDefs(catalogId, 0, 10);
        assertNotNull(aspectDefs);
        assertEquals(3, aspectDefs.size());
    }

    @Test
    void testListAspectDefsPagination()
    {
        UUID catalogId = setupTestCatalog();

        // Create multiple AspectDefs
        for (int i = 0; i < 5; i++) {
            MutableAspectDef aspectDef = factory.createMutableAspectDef("com.example.Aspect" + i);
            aspectDef.add(factory.createPropertyDef("prop", PropertyType.String, true, true, false, false, false));
            aspectDefService.createAspectDef(catalogId, aspectDef);
        }

        // Test first page
        List<AspectDef> page1 = aspectDefService.listAspectDefs(catalogId, 0, 2);
        assertEquals(2, page1.size());

        // Test second page
        List<AspectDef> page2 = aspectDefService.listAspectDefs(catalogId, 1, 2);
        assertEquals(2, page2.size());

        // Test third page
        List<AspectDef> page3 = aspectDefService.listAspectDefs(catalogId, 2, 2);
        assertEquals(1, page3.size());
    }

    @Test
    void testCountAspectDefs()
    {
        UUID catalogId = setupTestCatalog();

        // Initially should be 0
        assertEquals(0, aspectDefService.countAspectDefs(catalogId));

        // Create AspectDefs
        for (int i = 0; i < 3; i++) {
            MutableAspectDef aspectDef = factory.createMutableAspectDef("com.example.Aspect" + i);
            aspectDef.add(factory.createPropertyDef("prop", PropertyType.String, true, true, false, false, false));
            aspectDefService.createAspectDef(catalogId, aspectDef);
            assertEquals(i + 1, aspectDefService.countAspectDefs(catalogId));
        }
    }

    @Test
    void testGetAspectDefByName()
    {
        UUID catalogId = setupTestCatalog();

        // Create AspectDef
        UUID aspectDefId = UUID.randomUUID();
        MutableAspectDef original = factory.createMutableAspectDef("com.example.TestAspect", aspectDefId,
            new HashMap<>());
        original.add(factory.createPropertyDef("testProp", PropertyType.String, true, true, false, false, false));
        aspectDefService.createAspectDef(catalogId, original);

        // Get by name
        AspectDef retrieved = aspectDefService.getAspectDefByName(catalogId, "com.example.TestAspect");
        assertNotNull(retrieved);
        assertEquals(original.name(), retrieved.name());
        assertEquals(original.globalId(), retrieved.globalId());
    }

    @Test
    void testGetAspectDefByNameNotFound()
    {
        UUID catalogId = setupTestCatalog();

        assertThrows(ResourceNotFoundException.class, () -> aspectDefService.getAspectDefByName(catalogId,
            "nonexistent.Aspect"));
    }

    @Test
    void testGetAspectDefById()
    {
        UUID catalogId = setupTestCatalog();

        // Create AspectDef
        UUID aspectDefId = UUID.randomUUID();
        MutableAspectDef original = factory.createMutableAspectDef("com.example.TestAspect", aspectDefId,
            new HashMap<>());
        original.add(factory.createPropertyDef("testProp", PropertyType.String, true, true, false, false, false));
        aspectDefService.createAspectDef(catalogId, original);

        // Get by ID
        AspectDef retrieved = aspectDefService.getAspectDefById(catalogId, aspectDefId);
        assertNotNull(retrieved);
        assertEquals(original.name(), retrieved.name());
        assertEquals(original.globalId(), retrieved.globalId());
    }

    @Test
    void testGetAspectDefByIdNotFound()
    {
        UUID catalogId = setupTestCatalog();
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(ResourceNotFoundException.class, () -> aspectDefService.getAspectDefById(catalogId,
            nonExistentId));
    }
}
