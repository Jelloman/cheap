package net.netbeing.cheap.impl.basic;

import com.google.common.collect.ImmutableList;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.CatalogDef;
import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CatalogDefImpl to verify that identical instances produce the same hash value.
 */
class CatalogDefImplTest
{
    @Test
    void constructor_NoParameters_CreatesEmptyCatalogDef()
    {
        CatalogDefImpl catalogDef = new CatalogDefImpl();

        assertNotNull(catalogDef.aspectDefs());
        assertNotNull(catalogDef.hierarchyDefs());
        assertFalse(catalogDef.aspectDefs().iterator().hasNext());
        assertFalse(catalogDef.hierarchyDefs().iterator().hasNext());
    }

    @Test
    void constructor_WithHierarchyAndAspectDefs_CreatesCatalogDef()
    {
        HierarchyDef hierarchyDef = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_LIST);
        Map<String, PropertyDef> propertyDefs = new LinkedHashMap<>();
        propertyDefs.put("prop1", new PropertyDefImpl("prop1", PropertyType.String));
        AspectDef aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);

        CatalogDefImpl catalogDef = new CatalogDefImpl(
            ImmutableList.of(hierarchyDef),
            ImmutableList.of(aspectDef)
        );

        assertEquals(hierarchyDef, catalogDef.hierarchyDef("testHierarchy"));
        assertEquals(aspectDef, catalogDef.aspectDef("testAspect"));
    }

    @Test
    void hash_IdenticalInstancesConstructedSeparately_ReturnsSameHash()
    {
        // Create identical hierarchy defs
        HierarchyDef hierarchyDef1 = new HierarchyDefImpl("hierarchy1", HierarchyType.ENTITY_SET);
        HierarchyDef hierarchyDef2 = new HierarchyDefImpl("hierarchy2", HierarchyType.ENTITY_DIR);

        // Create identical aspect defs with same UUID
        UUID aspectId = UUID.randomUUID();
        Map<String, PropertyDef> propertyDefs1 = new LinkedHashMap<>();
        propertyDefs1.put("prop1", new PropertyDefImpl("prop1", PropertyType.String));
        propertyDefs1.put("prop2", new PropertyDefImpl("prop2", PropertyType.Integer));
        AspectDef aspectDef1 = new ImmutableAspectDefImpl("aspect1", aspectId, propertyDefs1);

        Map<String, PropertyDef> propertyDefs2 = new LinkedHashMap<>();
        propertyDefs2.put("prop1", new PropertyDefImpl("prop1", PropertyType.String));
        propertyDefs2.put("prop2", new PropertyDefImpl("prop2", PropertyType.Integer));
        AspectDef aspectDef2 = new ImmutableAspectDefImpl("aspect1", aspectId, propertyDefs2);

        // Create two CatalogDefs with identical content
        CatalogDefImpl catalogDef1 = new CatalogDefImpl(
            ImmutableList.of(hierarchyDef1, hierarchyDef2),
            ImmutableList.of(aspectDef1)
        );

        CatalogDefImpl catalogDef2 = new CatalogDefImpl(
            ImmutableList.of(
                new HierarchyDefImpl("hierarchy1", HierarchyType.ENTITY_SET),
                new HierarchyDefImpl("hierarchy2", HierarchyType.ENTITY_DIR)
            ),
            ImmutableList.of(aspectDef2)
        );

        assertEquals(catalogDef1.hash(), catalogDef2.hash(),
            "Identical CatalogDef instances should produce the same hash");
    }

    @Test
    void hash_SameInstance_ReturnsSameHash()
    {
        HierarchyDef hierarchyDef = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_LIST);
        Map<String, PropertyDef> propertyDefs = new LinkedHashMap<>();
        propertyDefs.put("prop1", new PropertyDefImpl("prop1", PropertyType.String));
        AspectDef aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);

        CatalogDefImpl catalogDef = new CatalogDefImpl(
            ImmutableList.of(hierarchyDef),
            ImmutableList.of(aspectDef)
        );

        assertEquals(catalogDef.hash(), catalogDef.hash(),
            "Same CatalogDef instance should consistently produce the same hash");
    }

    @Test
    void hash_DifferentHierarchyDefs_ReturnsDifferentHash()
    {
        Map<String, PropertyDef> propertyDefs = new LinkedHashMap<>();
        propertyDefs.put("prop1", new PropertyDefImpl("prop1", PropertyType.String));
        AspectDef aspectDef = new ImmutableAspectDefImpl("testAspect", propertyDefs);

        CatalogDefImpl catalogDef1 = new CatalogDefImpl(
            ImmutableList.of(new HierarchyDefImpl("hierarchy1", HierarchyType.ENTITY_LIST)),
            ImmutableList.of(aspectDef)
        );

        CatalogDefImpl catalogDef2 = new CatalogDefImpl(
            ImmutableList.of(new HierarchyDefImpl("hierarchy2", HierarchyType.ENTITY_SET)),
            ImmutableList.of(aspectDef)
        );

        assertNotEquals(catalogDef1.hash(), catalogDef2.hash(),
            "CatalogDef instances with different hierarchies should produce different hashes");
    }

    @Test
    void hash_DifferentAspectDefs_ReturnsDifferentHash()
    {
        HierarchyDef hierarchyDef = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_LIST);

        Map<String, PropertyDef> propertyDefs1 = new LinkedHashMap<>();
        propertyDefs1.put("prop1", new PropertyDefImpl("prop1", PropertyType.String));
        AspectDef aspectDef1 = new ImmutableAspectDefImpl("aspect1", propertyDefs1);

        Map<String, PropertyDef> propertyDefs2 = new LinkedHashMap<>();
        propertyDefs2.put("prop2", new PropertyDefImpl("prop2", PropertyType.Integer));
        AspectDef aspectDef2 = new ImmutableAspectDefImpl("aspect2", propertyDefs2);

        CatalogDefImpl catalogDef1 = new CatalogDefImpl(
            ImmutableList.of(hierarchyDef),
            ImmutableList.of(aspectDef1)
        );

        CatalogDefImpl catalogDef2 = new CatalogDefImpl(
            ImmutableList.of(hierarchyDef),
            ImmutableList.of(aspectDef2)
        );

        assertNotEquals(catalogDef1.hash(), catalogDef2.hash(),
            "CatalogDef instances with different aspects should produce different hashes");
    }

    @Test
    void hash_EmptyCatalogDefs_ReturnsSameHash()
    {
        CatalogDefImpl catalogDef1 = new CatalogDefImpl();
        CatalogDefImpl catalogDef2 = new CatalogDefImpl();

        assertEquals(catalogDef1.hash(), catalogDef2.hash(),
            "Empty CatalogDef instances should produce the same hash");
    }

    @Test
    void hash_DifferentOrderOfHierarchyDefs_ReturnsDifferentHash()
    {
        HierarchyDef hierarchyDef1 = new HierarchyDefImpl("hierarchy1", HierarchyType.ENTITY_LIST);
        HierarchyDef hierarchyDef2 = new HierarchyDefImpl("hierarchy2", HierarchyType.ENTITY_SET);

        CatalogDefImpl catalogDef1 = new CatalogDefImpl(
            ImmutableList.of(hierarchyDef1, hierarchyDef2),
            ImmutableList.of()
        );

        CatalogDefImpl catalogDef2 = new CatalogDefImpl(
            ImmutableList.of(hierarchyDef2, hierarchyDef1),
            ImmutableList.of()
        );

        assertNotEquals(catalogDef1.hash(), catalogDef2.hash(),
            "CatalogDef instances with different hierarchy order should produce different hashes");
    }

    @Test
    void hash_DifferentOrderOfAspectDefs_ReturnsDifferentHash()
    {
        Map<String, PropertyDef> propertyDefs1 = new LinkedHashMap<>();
        propertyDefs1.put("prop1", new PropertyDefImpl("prop1", PropertyType.String));
        AspectDef aspectDef1 = new ImmutableAspectDefImpl("aspect1", propertyDefs1);

        Map<String, PropertyDef> propertyDefs2 = new LinkedHashMap<>();
        propertyDefs2.put("prop2", new PropertyDefImpl("prop2", PropertyType.Integer));
        AspectDef aspectDef2 = new ImmutableAspectDefImpl("aspect2", propertyDefs2);

        CatalogDefImpl catalogDef1 = new CatalogDefImpl(
            ImmutableList.of(),
            ImmutableList.of(aspectDef1, aspectDef2)
        );

        CatalogDefImpl catalogDef2 = new CatalogDefImpl(
            ImmutableList.of(),
            ImmutableList.of(aspectDef2, aspectDef1)
        );

        assertNotEquals(catalogDef1.hash(), catalogDef2.hash(),
            "CatalogDef instances with different aspect order should produce different hashes");
    }
}