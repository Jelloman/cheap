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

package net.netbeing.cheap.db;

import net.netbeing.cheap.model.*;
import net.netbeing.cheap.util.CheapFactory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CatalogPersistenceBasicTest {

    @Test
    void testCatalogPersistenceInterfaceExists() {
        // Just verify that our interface and implementation compile
        assertNotNull(CatalogPersistence.class);
        assertNotNull(PostgresDao.class);
    }

    @Test
    void testFactoryCanCreateCatalog() {
        CheapFactory factory = new CheapFactory();

        // Test creating basic catalog
        Catalog catalog = factory.createCatalog();
        assertNotNull(catalog);
        assertNotNull(catalog.globalId());
        assertEquals(CatalogSpecies.SINK, catalog.species());
        // catalog.isStrict() removed from model
    }

    @Test
    void testFactoryCanCreateCatalogWithParams() {
        CheapFactory factory = new CheapFactory();
        UUID catalogId = UUID.randomUUID();

        // Test creating catalog with parameters
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SOURCE, null);
        assertNotNull(catalog);
        assertEquals(catalogId, catalog.globalId());
        assertEquals(CatalogSpecies.SOURCE, catalog.species());
        // catalog.isStrict() removed from model
        assertNull(catalog.upstream());
    }

    @Test
    void testFactoryCanCreateHierarchies() {
        CheapFactory factory = new CheapFactory();
        Catalog catalog = factory.createCatalog();

        // Test creating different hierarchy types
        HierarchyDef entitySetDef = factory.createHierarchyDef("entities", HierarchyType.ENTITY_SET);
        EntitySetHierarchy entitySet = factory.createEntitySetHierarchy(catalog, entitySetDef.name());
        assertNotNull(entitySet);
        assertEquals("entities", entitySet.name());
        assertEquals(HierarchyType.ENTITY_SET, entitySet.type());

        HierarchyDef directoryDef = factory.createHierarchyDef("directory", HierarchyType.ENTITY_DIR);
        EntityDirectoryHierarchy directory = factory.createEntityDirectoryHierarchy(catalog, directoryDef.name());
        assertNotNull(directory);
        assertEquals("directory", directory.name());
        assertEquals(HierarchyType.ENTITY_DIR, directory.type());
    }

    @Test
    void testFactoryCanCreateAspects() {
        CheapFactory factory = new CheapFactory();

        // Test creating aspect definitions
        AspectDef aspectDef = factory.createMutableAspectDef("person");
        assertNotNull(aspectDef);
        assertEquals("person", aspectDef.name());

        // Test creating property definitions
        PropertyDef propertyDef = factory.createPropertyDef("name", PropertyType.String);
        assertNotNull(propertyDef);
        assertEquals("name", propertyDef.name());
        assertEquals(PropertyType.String, propertyDef.type());

        // Test creating properties
        Property property = factory.createProperty(propertyDef, "John Doe");
        assertNotNull(property);
        assertEquals("John Doe", property.read());
        assertEquals(propertyDef, property.def());
    }

    @Test
    void testCatalogCanBeExtendedWithAspects() {
        CheapFactory factory = new CheapFactory();
        Catalog catalog = factory.createCatalog();

        AspectDef personAspect = factory.createMutableAspectDef("person");

        // Test extending catalog with aspect
        AspectMapHierarchy aspectMap = catalog.extend(personAspect);
        assertNotNull(aspectMap);
        assertEquals("person", aspectMap.aspectDef().name());

        // Verify the hierarchy was added to the catalog
        assertNotNull(catalog.hierarchy("person"));
        assertEquals(aspectMap, catalog.hierarchy("person"));
    }

    @Test
    void testCatalogHierarchyManagement() {
        CheapFactory factory = new CheapFactory();
        Catalog catalog = factory.createCatalog();

        // Create and add hierarchy
        HierarchyDef hierarchyDef = factory.createHierarchyDef("test", HierarchyType.ENTITY_SET);
        EntitySetHierarchy hierarchy = factory.createEntitySetHierarchy(catalog, hierarchyDef.name());

        catalog.addHierarchy(hierarchy);

        // Verify hierarchy was added
        assertNotNull(catalog.hierarchy("test"));
        assertEquals(hierarchy, catalog.hierarchy("test"));

        // Verify hierarchies collection includes our hierarchy
        boolean found = false;
        for (Hierarchy h : catalog.hierarchies()) {
            if ("test".equals(h.name())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Hierarchy should be found in catalog.hierarchies()");
    }
}