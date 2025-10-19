package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.HierarchyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;

import static org.junit.jupiter.api.Assertions.*;

class EntityListHierarchyImplTest
{
    private Catalog catalog;
    private EntityListHierarchyImpl entityList;
    private Entity entity1;
    private Entity entity2;
    private Entity entity3;

    @BeforeEach
    void setUp()
    {
        catalog = new CatalogImpl();
        entityList = new EntityListHierarchyImpl(catalog, "testEntityList");

        entity1 = new EntityImpl();
        entity2 = new EntityImpl();
        entity3 = new EntityImpl();
    }

    @SuppressWarnings("ConstantValue")
    @Test
    void constructor_ValidHierarchyDef_CreatesEmptyList()
    {
        EntityListHierarchyImpl list = new EntityListHierarchyImpl(catalog, "testEntityList");

        assertEquals("testEntityList", list.name());
        assertEquals(HierarchyType.ENTITY_LIST, list.type());
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }


    @Test
    void name_Always_ReturnsHierarchyName()
    {
        assertEquals("testEntityList", entityList.name());
    }

    @Test
    void add_NewEntity_AddsEntity()
    {
        boolean result = entityList.add(entity1);
        
        assertTrue(result);
        assertEquals(1, entityList.size());
        assertSame(entity1, entityList.getFirst());
        assertTrue(entityList.contains(entity1));
    }

    @Test
    void add_DuplicateEntity_AddsBoth()
    {
        entityList.add(entity1);
        boolean result = entityList.add(entity1);
        
        assertTrue(result);
        assertEquals(2, entityList.size());
        assertSame(entity1, entityList.get(0));
        assertSame(entity1, entityList.get(1));
    }

    @Test
    void add_MultipleEntities_MaintainsOrder()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity3);
        
        assertEquals(3, entityList.size());
        assertSame(entity1, entityList.get(0));
        assertSame(entity2, entityList.get(1));
        assertSame(entity3, entityList.get(2));
    }

    @Test
    void add_NullEntity_AddsNull()
    {
        boolean result = entityList.add(null);
        
        assertTrue(result);
        assertEquals(1, entityList.size());
        assertNull(entityList.getFirst());
        assertTrue(entityList.contains(null));
    }

    @Test
    void add_AtIndex_InsertsAtCorrectPosition()
    {
        entityList.add(entity1);
        entityList.add(entity3);
        
        entityList.add(1, entity2);
        
        assertEquals(3, entityList.size());
        assertSame(entity1, entityList.get(0));
        assertSame(entity2, entityList.get(1));
        assertSame(entity3, entityList.get(2));
    }

    @Test
    void add_AtInvalidIndex_ThrowsException()
    {
        assertThrows(IndexOutOfBoundsException.class, () -> entityList.add(1, entity1));
        assertThrows(IndexOutOfBoundsException.class, () -> entityList.add(-1, entity1));
    }

    @Test
    void get_ValidIndex_ReturnsEntity()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        
        assertSame(entity1, entityList.get(0));
        assertSame(entity2, entityList.get(1));
    }

    @Test
    void get_InvalidIndex_ThrowsException()
    {
        entityList.add(entity1);
        
        assertThrows(IndexOutOfBoundsException.class, () -> entityList.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> entityList.get(-1));
    }

    @Test
    void set_ValidIndex_ReplacesEntity()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        
        Entity oldEntity = entityList.set(1, entity3);
        
        assertSame(entity2, oldEntity);
        assertEquals(2, entityList.size());
        assertSame(entity1, entityList.get(0));
        assertSame(entity3, entityList.get(1));
    }

    @Test
    void set_InvalidIndex_ThrowsException()
    {
        assertThrows(IndexOutOfBoundsException.class, () -> entityList.set(0, entity1));
        assertThrows(IndexOutOfBoundsException.class, () -> entityList.set(-1, entity1));
    }

    @Test
    void remove_ByIndex_RemovesEntity()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity3);
        
        Entity removed = entityList.remove(1);
        
        assertSame(entity2, removed);
        assertEquals(2, entityList.size());
        assertSame(entity1, entityList.get(0));
        assertSame(entity3, entityList.get(1));
    }

    @Test
    void remove_ByObject_RemovesFirstOccurrence()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity1); // Duplicate
        
        boolean result = entityList.remove(entity1);
        
        assertTrue(result);
        assertEquals(2, entityList.size());
        assertSame(entity2, entityList.get(0));
        assertSame(entity1, entityList.get(1)); // Second occurrence remains
    }

    @Test
    void remove_NonExistentEntity_ReturnsFalse()
    {
        entityList.add(entity1);
        
        boolean result = entityList.remove(entity2);
        
        assertFalse(result);
        assertEquals(1, entityList.size());
    }

    @Test
    void indexOf_ExistingEntity_ReturnsCorrectIndex()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity1); // Duplicate
        
        assertEquals(0, entityList.indexOf(entity1)); // First occurrence
        assertEquals(1, entityList.indexOf(entity2));
    }

    @Test
    void indexOf_NonExistentEntity_ReturnsMinusOne()
    {
        entityList.add(entity1);
        
        assertEquals(-1, entityList.indexOf(entity2));
    }

    @Test
    void lastIndexOf_ExistingEntity_ReturnsLastIndex()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity1); // Duplicate
        
        assertEquals(2, entityList.lastIndexOf(entity1)); // Last occurrence
        assertEquals(1, entityList.lastIndexOf(entity2));
    }

    @Test
    void lastIndexOf_NonExistentEntity_ReturnsMinusOne()
    {
        entityList.add(entity1);
        
        assertEquals(-1, entityList.lastIndexOf(entity2));
    }

    @Test
    void contains_ExistingEntity_ReturnsTrue()
    {
        entityList.add(entity1);
        
        assertTrue(entityList.contains(entity1));
    }

    @Test
    void contains_NonExistentEntity_ReturnsFalse()
    {
        assertFalse(entityList.contains(entity1));
    }

    @SuppressWarnings("ConstantValue")
    @Test
    void clear_PopulatedList_RemovesAllEntities()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity3);
        
        entityList.clear();
        
        assertEquals(0, entityList.size());
        assertTrue(entityList.isEmpty());
    }

    @Test
    void isEmpty_EmptyList_ReturnsTrue()
    {
        assertTrue(entityList.isEmpty());
    }

    @Test
    void isEmpty_PopulatedList_ReturnsFalse()
    {
        entityList.add(entity1);
        
        assertFalse(entityList.isEmpty());
    }

    @Test
    void size_EmptyList_ReturnsZero()
    {
        assertEquals(0, entityList.size());
    }

    @Test
    void size_PopulatedList_ReturnsCorrectCount()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity1); // Duplicate
        
        assertEquals(3, entityList.size());
    }

    @Test
    void iterator_PopulatedList_IteratesInOrder()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity3);
        
        Iterator<Entity> iterator = entityList.iterator();
        assertTrue(iterator.hasNext());
        assertSame(entity1, iterator.next());
        assertTrue(iterator.hasNext());
        assertSame(entity2, iterator.next());
        assertTrue(iterator.hasNext());
        assertSame(entity3, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void listIterator_PopulatedList_AllowsBidirectionalIteration()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity3);
        
        ListIterator<Entity> iterator = entityList.listIterator();
        
        // Forward iteration
        assertTrue(iterator.hasNext());
        assertSame(entity1, iterator.next());
        assertTrue(iterator.hasNext());
        assertSame(entity2, iterator.next());
        
        // Backward iteration
        assertTrue(iterator.hasPrevious());
        assertSame(entity2, iterator.previous());
        assertTrue(iterator.hasPrevious());
        assertSame(entity1, iterator.previous());
        assertFalse(iterator.hasPrevious());
    }

    @Test
    void subList_ValidRange_ReturnsSublist()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity3);
        
        var subList = entityList.subList(1, 3);
        
        assertEquals(2, subList.size());
        assertSame(entity2, subList.get(0));
        assertSame(entity3, subList.get(1));
    }

    @Test
    void addAll_Collection_AddsAllEntities()
    {
        Collection<Entity> entities = Arrays.asList(entity1, entity2, entity3);
        
        boolean result = entityList.addAll(entities);
        
        assertTrue(result);
        assertEquals(3, entityList.size());
        assertSame(entity1, entityList.get(0));
        assertSame(entity2, entityList.get(1));
        assertSame(entity3, entityList.get(2));
    }

    @Test
    void addAll_AtIndex_InsertsAtCorrectPosition()
    {
        entityList.add(entity1);
        entityList.add(entity3);
        
        Collection<Entity> entities = Collections.singletonList(entity2);
        boolean result = entityList.addAll(1, entities);
        
        assertTrue(result);
        assertEquals(3, entityList.size());
        assertSame(entity1, entityList.get(0));
        assertSame(entity2, entityList.get(1));
        assertSame(entity3, entityList.get(2));
    }

    @Test
    void removeAll_Collection_RemovesSpecifiedEntities()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity3);
        entityList.add(entity1); // Duplicate
        
        Collection<Entity> toRemove = Arrays.asList(entity1, entity3);
        boolean result = entityList.removeAll(toRemove);
        
        assertTrue(result);
        assertEquals(1, entityList.size());
        assertSame(entity2, entityList.getFirst());
    }

    @Test
    void retainAll_Collection_KeepsOnlySpecifiedEntities()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity3);
        
        Collection<Entity> toRetain = Arrays.asList(entity1, entity3);
        boolean result = entityList.retainAll(toRetain);
        
        assertTrue(result);
        assertEquals(2, entityList.size());
        assertSame(entity1, entityList.get(0));
        assertSame(entity3, entityList.get(1));
    }

    @Test
    void toArray_PopulatedList_ReturnsArrayInOrder()
    {
        entityList.add(entity1);
        entityList.add(entity2);
        entityList.add(entity3);
        
        Object[] array = entityList.toArray();
        
        assertEquals(3, array.length);
        assertSame(entity1, array[0]);
        assertSame(entity2, array[1]);
        assertSame(entity3, array[2]);
    }

    @Test
    void duplicates_AllowedInList_MaintainsBoth()
    {
        entityList.add(entity1);
        entityList.add(entity1);
        entityList.add(entity1);
        
        assertEquals(3, entityList.size());
        for (int i = 0; i < 3; i++) {
            assertSame(entity1, entityList.get(i));
        }
    }

    @Test
    void ensureCapacity_LargeNumberOfElements_HandlesCorrectly()
    {
        int entityCount = 1000;
        
        for (int i = 0; i < entityCount; i++) {
            entityList.add(new EntityImpl());
        }
        
        assertEquals(entityCount, entityList.size());
    }
}