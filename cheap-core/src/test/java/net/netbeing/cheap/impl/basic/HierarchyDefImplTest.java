package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.HierarchyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HierarchyDefImpl to verify that identical instances produce the same hash value.
 */
class HierarchyDefImplTest
{
    @Test
    void constructor_ValidNameAndType_CreatesHierarchyDef()
    {
        HierarchyDefImpl hierarchyDef = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_LIST);

        assertEquals("testHierarchy", hierarchyDef.name());
        assertEquals(HierarchyType.ENTITY_LIST, hierarchyDef.type());
    }

    @Test
    void constructor_WithNullName_ThrowsNullPointerException()
    {
        assertThrows(NullPointerException.class, () -> new HierarchyDefImpl(null, HierarchyType.ENTITY_SET));
    }

    @Test
    void constructor_WithNullType_ThrowsNullPointerException()
    {
        assertThrows(NullPointerException.class, () -> new HierarchyDefImpl("testHierarchy", null));
    }

    @Test
    void hash_IdenticalInstancesConstructedSeparately_ReturnsSameHash()
    {
        HierarchyDefImpl hierarchyDef1 = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_SET);
        HierarchyDefImpl hierarchyDef2 = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_SET);

        assertEquals(hierarchyDef1.hash(), hierarchyDef2.hash(),
            "Identical HierarchyDef instances should produce the same hash");
    }

    @Test
    void hash_SameInstance_ReturnsSameHash()
    {
        HierarchyDefImpl hierarchyDef = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_DIR);

        assertEquals(hierarchyDef.hash(), hierarchyDef.hash(),
            "Same HierarchyDef instance should consistently produce the same hash");
    }

    @Test
    void hash_DifferentNames_ReturnsDifferentHash()
    {
        HierarchyDefImpl hierarchyDef1 = new HierarchyDefImpl("hierarchy1", HierarchyType.ENTITY_LIST);
        HierarchyDefImpl hierarchyDef2 = new HierarchyDefImpl("hierarchy2", HierarchyType.ENTITY_LIST);

        assertNotEquals(hierarchyDef1.hash(), hierarchyDef2.hash(),
            "HierarchyDef instances with different names should produce different hashes");
    }

    @Test
    void hash_DifferentTypes_ReturnsDifferentHash()
    {
        HierarchyDefImpl hierarchyDef1 = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_LIST);
        HierarchyDefImpl hierarchyDef2 = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_SET);

        assertNotEquals(hierarchyDef1.hash(), hierarchyDef2.hash(),
            "HierarchyDef instances with different types should produce different hashes");
    }

    @Test
    void hash_AllHierarchyTypes_ProduceDistinctHashes()
    {
        HierarchyDefImpl entityList = new HierarchyDefImpl("test", HierarchyType.ENTITY_LIST);
        HierarchyDefImpl entitySet = new HierarchyDefImpl("test", HierarchyType.ENTITY_SET);
        HierarchyDefImpl entityDir = new HierarchyDefImpl("test", HierarchyType.ENTITY_DIR);
        HierarchyDefImpl entityTree = new HierarchyDefImpl("test", HierarchyType.ENTITY_TREE);
        HierarchyDefImpl aspectMap = new HierarchyDefImpl("test", HierarchyType.ASPECT_MAP);

        // All five types should produce distinct hashes
        assertNotEquals(entityList.hash(), entitySet.hash());
        assertNotEquals(entityList.hash(), entityDir.hash());
        assertNotEquals(entityList.hash(), entityTree.hash());
        assertNotEquals(entityList.hash(), aspectMap.hash());
        assertNotEquals(entitySet.hash(), entityDir.hash());
        assertNotEquals(entitySet.hash(), entityTree.hash());
        assertNotEquals(entitySet.hash(), aspectMap.hash());
        assertNotEquals(entityDir.hash(), entityTree.hash());
        assertNotEquals(entityDir.hash(), aspectMap.hash());
        assertNotEquals(entityTree.hash(), aspectMap.hash());
    }

    @Test
    void equals_IdenticalInstances_ReturnsTrue()
    {
        HierarchyDefImpl hierarchyDef1 = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_LIST);
        HierarchyDefImpl hierarchyDef2 = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_LIST);

        assertEquals(hierarchyDef1, hierarchyDef2,
            "Identical HierarchyDef instances should be equal");
    }

    @Test
    void equals_DifferentNames_ReturnsFalse()
    {
        HierarchyDefImpl hierarchyDef1 = new HierarchyDefImpl("hierarchy1", HierarchyType.ENTITY_LIST);
        HierarchyDefImpl hierarchyDef2 = new HierarchyDefImpl("hierarchy2", HierarchyType.ENTITY_LIST);

        assertNotEquals(hierarchyDef1, hierarchyDef2,
            "HierarchyDef instances with different names should not be equal");
    }

    @Test
    void equals_DifferentTypes_ReturnsFalse()
    {
        HierarchyDefImpl hierarchyDef1 = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_LIST);
        HierarchyDefImpl hierarchyDef2 = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_SET);

        assertNotEquals(hierarchyDef1, hierarchyDef2,
            "HierarchyDef instances with different types should not be equal");
    }
}
