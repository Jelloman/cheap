package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EntityDirectoryHierarchyImplTest
{
    private HierarchyDef hierarchyDef;
    private EntityDirectoryHierarchyImpl entityDirectory;
    private Entity entity1;
    private Entity entity2;
    private Entity entity3;

    @BeforeEach
    void setUp()
    {
        hierarchyDef = new HierarchyDefImpl("testEntityDirectory", HierarchyType.ENTITY_DIR);
        entityDirectory = new EntityDirectoryHierarchyImpl(hierarchyDef);
        
        entity1 = new EntityFullImpl();
        entity2 = new EntityFullImpl();
        entity3 = new EntityFullImpl();
    }

    @Test
    void constructor_ValidHierarchyDef_CreatesEmptyDirectory()
    {
        EntityDirectoryHierarchyImpl directory = new EntityDirectoryHierarchyImpl(hierarchyDef);
        
        assertSame(hierarchyDef, directory.def());
        assertTrue(directory.isEmpty());
    }

    @Test
    void constructor_NullHierarchyDef_AcceptsNull()
    {
        EntityDirectoryHierarchyImpl directory = new EntityDirectoryHierarchyImpl(null);
        
        assertNull(directory.def());
        assertTrue(directory.isEmpty());
    }

    @Test
    void def_Always_ReturnsHierarchyDef()
    {
        assertSame(hierarchyDef, entityDirectory.def());
    }

    @Test
    void put_NewEntry_AddsEntity()
    {
        Entity result = entityDirectory.put("entity1", entity1);
        
        assertNull(result);
        assertEquals(1, entityDirectory.size());
        assertSame(entity1, entityDirectory.get("entity1"));
        assertTrue(entityDirectory.containsKey("entity1"));
        assertTrue(entityDirectory.containsValue(entity1));
    }

    @Test
    void put_ExistingKey_ReplacesEntity()
    {
        entityDirectory.put("entity1", entity1);
        
        Entity result = entityDirectory.put("entity1", entity2);
        
        assertSame(entity1, result);
        assertEquals(1, entityDirectory.size());
        assertSame(entity2, entityDirectory.get("entity1"));
        assertFalse(entityDirectory.containsValue(entity1));
        assertTrue(entityDirectory.containsValue(entity2));
    }

    @Test
    void put_MultipleEntries_AddsAllEntries()
    {
        entityDirectory.put("entity1", entity1);
        entityDirectory.put("entity2", entity2);
        entityDirectory.put("entity3", entity3);
        
        assertEquals(3, entityDirectory.size());
        assertSame(entity1, entityDirectory.get("entity1"));
        assertSame(entity2, entityDirectory.get("entity2"));
        assertSame(entity3, entityDirectory.get("entity3"));
    }

    @Test
    void put_NullKey_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> entityDirectory.put(null, entity1));
    }

    @Test
    void put_EmptyStringKey_AcceptsEmptyString()
    {
        Entity result = entityDirectory.put("", entity1);
        
        assertNull(result);
        assertEquals(1, entityDirectory.size());
        assertSame(entity1, entityDirectory.get(""));
        assertTrue(entityDirectory.containsKey(""));
    }

    @Test
    void put_WhitespaceKey_AcceptsWhitespace()
    {
        String whitespaceKey = "  \t\n  ";
        Entity result = entityDirectory.put(whitespaceKey, entity1);
        
        assertNull(result);
        assertEquals(1, entityDirectory.size());
        assertSame(entity1, entityDirectory.get(whitespaceKey));
        assertTrue(entityDirectory.containsKey(whitespaceKey));
    }

    @Test
    void get_ExistingKey_ReturnsEntity()
    {
        entityDirectory.put("entity1", entity1);
        
        assertSame(entity1, entityDirectory.get("entity1"));
    }

    @Test
    void remove_ExistingKey_RemovesEntity()
    {
        entityDirectory.put("entity1", entity1);
        
        Entity result = entityDirectory.remove("entity1");
        
        assertSame(entity1, result);
        assertEquals(0, entityDirectory.size());
    }

    @Test
    void remove_NonExistentKey_ReturnsNull()
    {
        Entity result = entityDirectory.remove("nonexistent");
        
        assertNull(result);
        assertEquals(0, entityDirectory.size());
    }

    @Test
    void containsKey_ExistingKey_ReturnsTrue()
    {
        entityDirectory.put("entity1", entity1);
        
        assertTrue(entityDirectory.containsKey("entity1"));
    }

    @Test
    void containsValue_ExistingValue_ReturnsTrue()
    {
        entityDirectory.put("entity1", entity1);

        assertTrue(entityDirectory.containsValue(entity1));
    }

    @Test
    void containsKey_CaseSensitive_DistinguishesCases()
    {
        entityDirectory.put("Entity1", entity1);
        
        assertTrue(entityDirectory.containsKey("Entity1"));
        assertFalse(entityDirectory.containsKey("entity1"));
        assertFalse(entityDirectory.containsKey("ENTITY1"));
    }

    @Test
    void keySet_PopulatedDirectory_ReturnsAllKeys()
    {
        entityDirectory.put("entity1", entity1);
        entityDirectory.put("entity2", entity2);
        
        Set<String> keys = entityDirectory.keySet();
        
        assertEquals(2, keys.size());
        assertTrue(keys.contains("entity1"));
        assertTrue(keys.contains("entity2"));
    }

    @Test
    void values_PopulatedDirectory_ReturnsAllValues()
    {
        entityDirectory.put("entity1", entity1);
        entityDirectory.put("entity2", entity2);
        
        Collection<Entity> values = entityDirectory.values();
        
        assertEquals(2, values.size());
        assertTrue(values.contains(entity1));
        assertTrue(values.contains(entity2));
    }

    @Test
    void entrySet_PopulatedDirectory_ReturnsAllEntries()
    {
        entityDirectory.put("entity1", entity1);
        entityDirectory.put("entity2", entity2);
        
        Set<java.util.Map.Entry<String, Entity>> entries = entityDirectory.entrySet();
        
        assertEquals(2, entries.size());
        assertTrue(entries.stream().anyMatch(e -> e.getKey().equals("entity1") && e.getValue().equals(entity1)));
        assertTrue(entries.stream().anyMatch(e -> e.getKey().equals("entity2") && e.getValue().equals(entity2)));
    }

    @Test
    void put_SpecialCharacterKeys_HandlesCorrectly()
    {
        String[] specialKeys = {
            "entity-with-dashes",
            "entity_with_underscores",
            "entity.with.dots",
            "entity@with@symbols",
            "entity/with/slashes",
            "entity\\with\\backslashes",
            "entity with spaces",
            "ENTITY_UPPERCASE",
            "entity123numbers",
            "αβγ_unicode",
            ""
        };

        for (String specialKey : specialKeys) {
            Entity entity = new EntityFullImpl();
            entityDirectory.put(specialKey, entity);
        }
        
        assertEquals(specialKeys.length, entityDirectory.size());
        
        for (String key : specialKeys) {
            assertTrue(entityDirectory.containsKey(key));
            assertNotNull(entityDirectory.get(key));
        }
    }

    @Test
    void put_VeryLongKey_HandlesCorrectly()
    {
        String longKey = "a".repeat(10000);
        
        Entity result = entityDirectory.put(longKey, entity1);
        
        assertNull(result);
        assertSame(entity1, entityDirectory.get(longKey));
    }

    @Test
    void put_DuplicateValues_AllowsMultipleKeysToSameEntity()
    {
        entityDirectory.put("key1", entity1);
        entityDirectory.put("key2", entity1); // Same entity, different key
        
        assertEquals(2, entityDirectory.size());
        assertSame(entity1, entityDirectory.get("key1"));
        assertSame(entity1, entityDirectory.get("key2"));
        assertTrue(entityDirectory.containsValue(entity1));
    }

    @Test
    void hashCode_SameEntries_ProducesSameHashCode()
    {
        EntityDirectoryHierarchyImpl dir1 = new EntityDirectoryHierarchyImpl(hierarchyDef);
        EntityDirectoryHierarchyImpl dir2 = new EntityDirectoryHierarchyImpl(hierarchyDef);

        dir1.put("entity1", entity1);
        dir1.put("entity2", entity2);

        dir2.put("entity2", entity2);
        dir2.put("entity1", entity1); // Different order

        assertEquals(dir1.hashCode(), dir2.hashCode());
    }

    @Test
    void hashCode_DifferentEntries_ProducesDifferentHashCode()
    {
        EntityDirectoryHierarchyImpl dir1 = new EntityDirectoryHierarchyImpl(hierarchyDef);
        EntityDirectoryHierarchyImpl dir2 = new EntityDirectoryHierarchyImpl(hierarchyDef);

        dir1.put("entity1", entity1);

        dir2.put("entity2", entity2);

        assertNotEquals(dir1.hashCode(), dir2.hashCode());
    }

    @Test
    void equals_SameEntries_ReturnsTrue()
    {
        EntityDirectoryHierarchyImpl dir1 = new EntityDirectoryHierarchyImpl(hierarchyDef);
        EntityDirectoryHierarchyImpl dir2 = new EntityDirectoryHierarchyImpl(hierarchyDef);
        
        dir1.put("entity1", entity1);
        dir1.put("entity2", entity2);
        
        dir2.put("entity2", entity2);
        dir2.put("entity1", entity1); // Different order
        
        assertEquals(dir1, dir2);
    }

    @Test
    void performance_LargeNumberOfEntries_HandlesCorrectly()
    {
        int entryCount = 1000;
        Entity[] entities = new Entity[entryCount];
        
        for (int i = 0; i < entryCount; i++) {
            entities[i] = new EntityFullImpl();
            entityDirectory.put("entity" + i, entities[i]);
        }
        
        assertEquals(entryCount, entityDirectory.size());
        
        // Verify all entities are present and retrievable
        for (int i = 0; i < entryCount; i++) {
            assertTrue(entityDirectory.containsKey("entity" + i));
            assertSame(entities[i], entityDirectory.get("entity" + i));
        }
    }

    @Test
    void put_ReplaceWithSameEntity_DoesNotChange()
    {
        entityDirectory.put("entity1", entity1);
        
        Entity result = entityDirectory.put("entity1", entity1); // Same entity
        
        assertSame(entity1, result);
        assertEquals(1, entityDirectory.size());
        assertSame(entity1, entityDirectory.get("entity1"));
    }
}