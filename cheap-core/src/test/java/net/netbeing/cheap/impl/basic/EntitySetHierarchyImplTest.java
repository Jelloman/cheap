package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class EntitySetHierarchyImplTest
{
    private Catalog catalog;
    private HierarchyDef hierarchyDef;
    private EntitySetHierarchyImpl entitySet;
    private Entity entity1;
    private Entity entity2;
    private Entity entity3;

    @BeforeEach
    void setUp()
    {
        catalog = new CatalogImpl();
        hierarchyDef = new HierarchyDefImpl("testEntitySet", HierarchyType.ENTITY_SET);
        entitySet = new EntitySetHierarchyImpl(catalog, "testEntitySet");

        entity1 = new EntityImpl();
        entity2 = new EntityImpl();
        entity3 = new EntityImpl();
    }

    @Test
    void constructor_ValidHierarchyDef_CreatesEmptySet()
    {
        EntitySetHierarchyImpl set = new EntitySetHierarchyImpl(catalog, "testEntitySet");

        assertEquals("testEntitySet", set.name());
        assertEquals(HierarchyType.ENTITY_SET, set.type());
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }


    @Test
    void name_Always_ReturnsHierarchyName()
    {
        assertEquals("testEntitySet", entitySet.name());
    }

    @Test
    void add_NewEntity_AddsEntity()
    {
        boolean result = entitySet.add(entity1);
        
        assertTrue(result);
        assertEquals(1, entitySet.size());
        assertTrue(entitySet.contains(entity1));
    }

    @Test
    void add_DuplicateEntity_ReturnsFalse()
    {
        entitySet.add(entity1);
        
        boolean result = entitySet.add(entity1);
        
        assertFalse(result);
        assertEquals(1, entitySet.size());
        assertTrue(entitySet.contains(entity1));
    }

    @Test
    void add_MultipleEntities_AddsAllUniqueEntities()
    {
        entitySet.add(entity1);
        entitySet.add(entity2);
        entitySet.add(entity3);
        
        assertEquals(3, entitySet.size());
        assertTrue(entitySet.contains(entity1));
        assertTrue(entitySet.contains(entity2));
        assertTrue(entitySet.contains(entity3));
    }

    @Test
    void add_NullEntity_AddsNull()
    {
        boolean result = entitySet.add(null);
        
        assertTrue(result);
        assertEquals(1, entitySet.size());
        assertTrue(entitySet.contains(null));
    }

    @Test
    void remove_ExistingEntity_RemovesEntity()
    {
        entitySet.add(entity1);
        
        boolean result = entitySet.remove(entity1);
        
        assertTrue(result);
        assertEquals(0, entitySet.size());
        assertFalse(entitySet.contains(entity1));
    }

    @Test
    void remove_NonExistentEntity_ReturnsFalse()
    {
        boolean result = entitySet.remove(entity1);
        
        assertFalse(result);
        assertEquals(0, entitySet.size());
    }

    @Test
    void contains_ExistingEntity_ReturnsTrue()
    {
        entitySet.add(entity1);
        
        assertTrue(entitySet.contains(entity1));
    }

    @Test
    void contains_NonExistentEntity_ReturnsFalse()
    {
        assertFalse(entitySet.contains(entity1));
    }

    @Test
    void contains_NullEntity_WorksCorrectly()
    {
        assertFalse(entitySet.contains(null));
        
        entitySet.add(null);
        assertTrue(entitySet.contains(null));
    }

    @Test
    void clear_PopulatedSet_RemovesAllEntities()
    {
        entitySet.add(entity1);
        entitySet.add(entity2);
        entitySet.add(entity3);
        
        entitySet.clear();
        
        assertEquals(0, entitySet.size());
        assertTrue(entitySet.isEmpty());
        assertFalse(entitySet.contains(entity1));
        assertFalse(entitySet.contains(entity2));
        assertFalse(entitySet.contains(entity3));
    }

    @Test
    void isEmpty_EmptySet_ReturnsTrue()
    {
        assertTrue(entitySet.isEmpty());
    }

    @Test
    void isEmpty_PopulatedSet_ReturnsFalse()
    {
        entitySet.add(entity1);
        
        assertFalse(entitySet.isEmpty());
    }

    @Test
    void size_EmptySet_ReturnsZero()
    {
        assertEquals(0, entitySet.size());
    }

    @Test
    void size_PopulatedSet_ReturnsCorrectCount()
    {
        entitySet.add(entity1);
        entitySet.add(entity2);
        entitySet.add(entity3);
        
        assertEquals(3, entitySet.size());
    }

    @Test
    void iterator_EmptySet_HasNoElements()
    {
        Iterator<Entity> iterator = entitySet.iterator();
        
        assertFalse(iterator.hasNext());
    }

    @Test
    void iterator_PopulatedSet_IteratesOverAllEntities()
    {
        entitySet.add(entity1);
        entitySet.add(entity2);
        entitySet.add(entity3);
        
        Iterator<Entity> iterator = entitySet.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            assertTrue(entitySet.contains(entity));
            count++;
        }
        
        assertEquals(3, count);
    }

    @Test
    void toArray_PopulatedSet_ReturnsAllEntities()
    {
        entitySet.add(entity1);
        entitySet.add(entity2);
        
        Object[] array = entitySet.toArray();
        
        assertEquals(2, array.length);
        assertTrue(Arrays.asList(array).contains(entity1));
        assertTrue(Arrays.asList(array).contains(entity2));
    }

    @Test
    void toArray_WithTypedArray_ReturnsTypedArray()
    {
        entitySet.add(entity1);
        entitySet.add(entity2);
        
        Entity[] array = entitySet.toArray(new Entity[0]);
        
        assertEquals(2, array.length);
        assertTrue(Arrays.asList(array).contains(entity1));
        assertTrue(Arrays.asList(array).contains(entity2));
    }

    @Test
    void addAll_Collection_AddsAllUniqueEntities()
    {
        Collection<Entity> entities = Arrays.asList(entity1, entity2, entity3, entity1); // Duplicate
        
        boolean result = entitySet.addAll(entities);
        
        assertTrue(result);
        assertEquals(3, entitySet.size()); // Should only have 3 unique entities
        assertTrue(entitySet.contains(entity1));
        assertTrue(entitySet.contains(entity2));
        assertTrue(entitySet.contains(entity3));
    }

    @Test
    void removeAll_Collection_RemovesSpecifiedEntities()
    {
        entitySet.add(entity1);
        entitySet.add(entity2);
        entitySet.add(entity3);
        
        Collection<Entity> toRemove = Arrays.asList(entity1, entity3);
        boolean result = entitySet.removeAll(toRemove);
        
        assertTrue(result);
        assertEquals(1, entitySet.size());
        assertFalse(entitySet.contains(entity1));
        assertTrue(entitySet.contains(entity2));
        assertFalse(entitySet.contains(entity3));
    }

    @Test
    void retainAll_Collection_KeepsOnlySpecifiedEntities()
    {
        entitySet.add(entity1);
        entitySet.add(entity2);
        entitySet.add(entity3);
        
        Collection<Entity> toRetain = Arrays.asList(entity1, entity3);
        boolean result = entitySet.retainAll(toRetain);
        
        assertTrue(result);
        assertEquals(2, entitySet.size());
        assertTrue(entitySet.contains(entity1));
        assertFalse(entitySet.contains(entity2));
        assertTrue(entitySet.contains(entity3));
    }

    @Test
    void containsAll_AllPresent_ReturnsTrue()
    {
        entitySet.add(entity1);
        entitySet.add(entity2);
        entitySet.add(entity3);
        
        Collection<Entity> testCollection = Arrays.asList(entity1, entity2);
        
        assertTrue(entitySet.containsAll(testCollection));
    }

    @Test
    void containsAll_SomeMissing_ReturnsFalse()
    {
        entitySet.add(entity1);
        entitySet.add(entity2);
        
        Collection<Entity> testCollection = Arrays.asList(entity1, entity3); // entity3 not added
        
        assertFalse(entitySet.containsAll(testCollection));
    }

    @Test
    void hashCode_SameEntities_ProducesSameHashCode()
    {
        EntitySetHierarchyImpl set1 = new EntitySetHierarchyImpl(catalog, "testEntitySet");
        EntitySetHierarchyImpl set2 = new EntitySetHierarchyImpl(catalog, "testEntitySet");

        set1.add(entity1);
        set1.add(entity2);

        set2.add(entity2);
        set2.add(entity1); // Different order

        assertEquals(set1.hashCode(), set2.hashCode());
    }

    @Test
    void equals_SameEntities_ReturnsTrue()
    {
        EntitySetHierarchyImpl set1 = new EntitySetHierarchyImpl(catalog, "testEntitySet");
        EntitySetHierarchyImpl set2 = new EntitySetHierarchyImpl(catalog, "testEntitySet");

        set1.add(entity1);
        set1.add(entity2);

        set2.add(entity2);
        set2.add(entity1); // Different order

        assertEquals(set1, set2);
    }

    @Test
    void add_LargeNumberOfEntities_HandlesCorrectly()
    {
        int entityCount = 1000;
        Entity[] entities = new Entity[entityCount];
        
        for (int i = 0; i < entityCount; i++) {
            entities[i] = new EntityImpl();
            entitySet.add(entities[i]);
        }
        
        assertEquals(entityCount, entitySet.size());
        
        // Verify all entities are present
        for (Entity entity : entities) {
            assertTrue(entitySet.contains(entity));
        }
    }

    @Test
    void uniqueness_SameEntityMultipleTimes_StaysUnique()
    {
        for (int i = 0; i < 10; i++) {
            entitySet.add(entity1);
        }
        
        assertEquals(1, entitySet.size());
        assertTrue(entitySet.contains(entity1));
    }
}