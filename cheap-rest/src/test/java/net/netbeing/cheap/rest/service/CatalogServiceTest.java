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
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.CatalogSpecies;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;
import net.netbeing.cheap.model.MutableAspectDef;
import net.netbeing.cheap.model.PropertyType;
import net.netbeing.cheap.rest.exception.ResourceNotFoundException;
import net.netbeing.cheap.rest.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CatalogService.
 */
class CatalogServiceTest extends BaseServiceTest
{
    @Test
    void testCreateCatalog()
    {
        // Create a simple AspectDef
        MutableAspectDef personAspect = factory.createMutableAspectDef("com.example.PersonAspect");
        personAspect.add(factory.createPropertyDef("name", PropertyType.String, true, true, false, false, false));

        // Create a HierarchyDef
        HierarchyDef peopleHierarchy = factory.createHierarchyDef("people", HierarchyType.ENTITY_SET);

        // Create CatalogDef
        CatalogDef catalogDef = factory.createCatalogDef(Collections.singletonList(peopleHierarchy),
            Collections.singletonList(personAspect));

        // Create catalog
        UUID catalogId = catalogService.createCatalog(catalogDef, CatalogSpecies.SINK, null, URI.create("http" +
            "://example.com/api/catalog"));

        // Verify
        assertNotNull(catalogId);

        // Verify we can retrieve it
        Catalog loadedCatalog = catalogService.getCatalog(catalogId);
        assertNotNull(loadedCatalog);
        assertEquals(catalogId, loadedCatalog.globalId());
        assertEquals(CatalogSpecies.SINK, loadedCatalog.species());
        assertEquals(URI.create("http://example.com/api/catalog/" + loadedCatalog.globalId()), loadedCatalog.uri());
    }

    @Test
    void testCreateCatalogWithUpstream()
    {
        // Create upstream catalog first
        CatalogDef upstreamDef = factory.createCatalogDef(Collections.emptyList(), Collections.emptyList());
        UUID upstreamId = catalogService.createCatalog(upstreamDef, CatalogSpecies.SOURCE, null, URI.create("http" +
            "://example.com/api/catalog"));

        // Create derived catalog
        CatalogDef catalogDef = factory.createCatalogDef(Collections.emptyList(), Collections.emptyList());
        UUID catalogId = catalogService.createCatalog(catalogDef, CatalogSpecies.MIRROR, upstreamId, URI.create("http" +
            "://example.com/api/catalog"));

        // Verify
        assertNotNull(catalogId);
        Catalog loadedCatalog = catalogService.getCatalog(catalogId);
        assertEquals(upstreamId, loadedCatalog.upstream());
    }

    @Test
    void testListCatalogIds()
    {
        // Create multiple catalogs
        CatalogDef catalogDef = factory.createCatalogDef(Collections.emptyList(), Collections.emptyList());

        UUID id1 = catalogService.createCatalog(catalogDef, CatalogSpecies.SINK, null, URI.create("http://example" +
            ".com/api/catalog"));
        UUID id2 = catalogService.createCatalog(catalogDef, CatalogSpecies.SINK, null, URI.create("http://example" +
            ".com/api/catalog"));
        UUID id3 = catalogService.createCatalog(catalogDef, CatalogSpecies.SINK, null, URI.create("http://example" +
            ".com/api/catalog"));

        // Test listing
        List<UUID> catalogIds = catalogService.listCatalogIds(0, 10);
        assertNotNull(catalogIds);
        assertEquals(3, catalogIds.size());
        assertTrue(catalogIds.contains(id1));
        assertTrue(catalogIds.contains(id2));
        assertTrue(catalogIds.contains(id3));
    }

    @Test
    void testListCatalogIdsPagination()
    {
        // Create multiple catalogs
        CatalogDef catalogDef = factory.createCatalogDef(Collections.emptyList(), Collections.emptyList());

        for (int i = 0; i < 5; i++) {
            catalogService.createCatalog(catalogDef, CatalogSpecies.SINK, null, URI.create("http://example" +
                ".com/api/catalog"));
        }

        // Test first page
        List<UUID> page1 = catalogService.listCatalogIds(0, 2);
        assertEquals(2, page1.size());

        // Test second page
        List<UUID> page2 = catalogService.listCatalogIds(1, 2);
        assertEquals(2, page2.size());

        // Test third page
        List<UUID> page3 = catalogService.listCatalogIds(2, 2);
        assertEquals(1, page3.size());
    }

    @Test
    void testCountCatalogs()
    {
        // Initially should be 0
        assertEquals(0, catalogService.countCatalogs());

        // Create catalogs
        CatalogDef catalogDef = factory.createCatalogDef(Collections.emptyList(), Collections.emptyList());

        catalogService.createCatalog(catalogDef, CatalogSpecies.SINK, null, URI.create("http://example" +
            ".com/api/catalog"));
        assertEquals(1, catalogService.countCatalogs());

        catalogService.createCatalog(catalogDef, CatalogSpecies.SINK, null, URI.create("http://example" +
            ".com/api/catalog"));
        assertEquals(2, catalogService.countCatalogs());

        catalogService.createCatalog(catalogDef, CatalogSpecies.SINK, null, URI.create("http://example" +
            ".com/api/catalog"));
        assertEquals(3, catalogService.countCatalogs());
    }

    @Test
    void testGetCatalog()
    {
        // Create catalog
        CatalogDef catalogDef = factory.createCatalogDef(Collections.emptyList(), Collections.emptyList());
        UUID catalogId = catalogService.createCatalog(catalogDef, CatalogSpecies.SINK, null, URI.create("http" +
            "://example.com/api/catalog"));

        // Get catalog
        Catalog catalog = catalogService.getCatalog(catalogId);
        assertNotNull(catalog);
        assertEquals(catalogId, catalog.globalId());
        assertEquals(CatalogSpecies.SINK, catalog.species());
        assertEquals(URI.create("http://example.com/api/catalog/" + catalogId), catalog.uri());
    }

    @Test
    void testGetCatalogNotFound()
    {
        UUID nonExistentId = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () -> catalogService.getCatalog(nonExistentId));
    }

    @Test
    void testGetCatalogDef()
    {
        // Create catalog with AspectDef and HierarchyDef
        MutableAspectDef personAspect = factory.createMutableAspectDef("com.example.PersonAspect");
        personAspect.add(factory.createPropertyDef("name", PropertyType.String, true, true, false, false, false));

        HierarchyDef peopleHierarchy = factory.createHierarchyDef("people", HierarchyType.ENTITY_SET);

        CatalogDef originalDef = factory.createCatalogDef(Collections.singletonList(peopleHierarchy),
            Collections.singletonList(personAspect));

        UUID catalogId = catalogService.createCatalog(originalDef, CatalogSpecies.SINK, null, URI.create("http" +
            "://example.com/api/catalog"));

        // Get catalog def
        CatalogDef retrievedDef = catalogService.getCatalogDef(catalogId);
        assertNotNull(retrievedDef);

        // Verify AspectDefs
        boolean foundAspect = false;
        for (AspectDef def : retrievedDef.aspectDefs()) {
            if (def.name().equals("com.example.PersonAspect")) {
                foundAspect = true;
                break;
            }
        }
        assertTrue(foundAspect, "PersonAspect should be in CatalogDef");

        // Verify HierarchyDefs
        boolean foundHierarchy = false;
        for (HierarchyDef def : retrievedDef.hierarchyDefs()) {
            if (def.name().equals("people")) {
                foundHierarchy = true;
                assertEquals(HierarchyType.ENTITY_SET, def.type());
                break;
            }
        }
        assertTrue(foundHierarchy, "people hierarchy should be in CatalogDef");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testValidateCatalogDefNull()
    {
        assertThrows(ValidationException.class, () -> catalogService.validateCatalogDef(null));
    }
}
