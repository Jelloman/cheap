package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HierarchyDirImplTest
{
    private HierarchyDef directoryDef;
    private HierarchyDef hierarchy1Def;
    private HierarchyDef hierarchy2Def;
    private HierarchyDef hierarchy3Def;
    private Hierarchy hierarchy1;
    private Hierarchy hierarchy2;
    private Hierarchy hierarchy3;
    private HierarchyDirImpl hierarchyDir;

    @BeforeEach
    void setUp()
    {
        directoryDef = new HierarchyDefImpl("directoryHierarchy", HierarchyType.ENTITY_DIR);
        hierarchy1Def = new HierarchyDefImpl("hierarchy1", HierarchyType.ENTITY_SET);
        hierarchy2Def = new HierarchyDefImpl("hierarchy2", HierarchyType.ENTITY_LIST);
        hierarchy3Def = new HierarchyDefImpl("hierarchy3", HierarchyType.ENTITY_TREE);
        
        hierarchy1 = new EntitySetHierarchyImpl(hierarchy1Def);
        hierarchy2 = new EntityListHierarchyImpl(hierarchy2Def);
        Entity rootEntity = new EntityFullImpl();
        hierarchy3 = new EntityTreeHierarchyImpl(hierarchy3Def, rootEntity);
        
        hierarchyDir = new HierarchyDirImpl(directoryDef);
    }

    @Test
    void constructor_WithDefOnly_CreatesEmptyDirectory()
    {
        HierarchyDirImpl dir = new HierarchyDirImpl(directoryDef);
        
        assertSame(directoryDef, dir.def());
        assertTrue(dir.isEmpty());
        assertEquals(0, dir.size());
    }

    @Test
    void constructor_WithDefCapacityAndLoadFactor_CreatesEmptyDirectory()
    {
        HierarchyDirImpl dir = new HierarchyDirImpl(directoryDef, 20, 0.8f);
        
        assertSame(directoryDef, dir.def());
        assertTrue(dir.isEmpty());
        assertEquals(0, dir.size());
    }

    @Test
    void constructor_WithNullDef_AcceptsNull()
    {
        HierarchyDirImpl dir = new HierarchyDirImpl(null);
        
        assertNull(dir.def());
    }

    @Test
    void def_Always_ReturnsHierarchyDef()
    {
        assertSame(directoryDef, hierarchyDir.def());
    }

    @Test
    void put_NewHierarchy_AddsHierarchy()
    {
        Hierarchy result = hierarchyDir.put("hier1", hierarchy1);
        
        assertNull(result);
        assertEquals(1, hierarchyDir.size());
        assertSame(hierarchy1, hierarchyDir.get("hier1"));
        assertTrue(hierarchyDir.containsKey("hier1"));
        assertTrue(hierarchyDir.containsValue(hierarchy1));
    }

    @Test
    void put_ExistingKey_ReplacesHierarchy()
    {
        hierarchyDir.put("hier1", hierarchy1);
        
        Hierarchy result = hierarchyDir.put("hier1", hierarchy2);
        
        assertSame(hierarchy1, result);
        assertEquals(1, hierarchyDir.size());
        assertSame(hierarchy2, hierarchyDir.get("hier1"));
        assertFalse(hierarchyDir.containsValue(hierarchy1));
        assertTrue(hierarchyDir.containsValue(hierarchy2));
    }

    @Test
    void put_MultipleHierarchies_AddsAllHierarchies()
    {
        hierarchyDir.put("hier1", hierarchy1);
        hierarchyDir.put("hier2", hierarchy2);
        hierarchyDir.put("hier3", hierarchy3);
        
        assertEquals(3, hierarchyDir.size());
        assertSame(hierarchy1, hierarchyDir.get("hier1"));
        assertSame(hierarchy2, hierarchyDir.get("hier2"));
        assertSame(hierarchy3, hierarchyDir.get("hier3"));
    }

    @Test
    void put_NullKey_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> hierarchyDir.put(null, hierarchy1));
    }

    @Test
    void get_ExistingKey_ReturnsHierarchy()
    {
        hierarchyDir.put("hier1", hierarchy1);
        
        assertSame(hierarchy1, hierarchyDir.get("hier1"));
    }

    @Test
    void get_NonExistentKey_ReturnsNull()
    {
        assertNull(hierarchyDir.get("nonexistent"));
    }

    @Test
    void remove_ExistingKey_RemovesHierarchy()
    {
        hierarchyDir.put("hier1", hierarchy1);
        
        Hierarchy result = hierarchyDir.remove("hier1");
        
        assertSame(hierarchy1, result);
        assertEquals(0, hierarchyDir.size());
        assertNull(hierarchyDir.get("hier1"));
        assertFalse(hierarchyDir.containsKey("hier1"));
        assertFalse(hierarchyDir.containsValue(hierarchy1));
    }

    @Test
    void remove_NonExistentKey_ReturnsNull()
    {
        Hierarchy result = hierarchyDir.remove("nonexistent");
        
        assertNull(result);
        assertEquals(0, hierarchyDir.size());
    }

    @Test
    void clear_PopulatedDirectory_RemovesAllHierarchies()
    {
        hierarchyDir.put("hier1", hierarchy1);
        hierarchyDir.put("hier2", hierarchy2);
        hierarchyDir.put("hier3", hierarchy3);
        
        hierarchyDir.clear();
        
        assertEquals(0, hierarchyDir.size());
        assertTrue(hierarchyDir.isEmpty());
        assertNull(hierarchyDir.get("hier1"));
        assertNull(hierarchyDir.get("hier2"));
        assertNull(hierarchyDir.get("hier3"));
    }

    @Test
    void containsKey_ExistingKey_ReturnsTrue()
    {
        hierarchyDir.put("hier1", hierarchy1);
        
        assertTrue(hierarchyDir.containsKey("hier1"));
    }

    @Test
    void containsKey_NonExistentKey_ReturnsFalse()
    {
        assertFalse(hierarchyDir.containsKey("nonexistent"));
    }

    @Test
    void containsValue_ExistingValue_ReturnsTrue()
    {
        hierarchyDir.put("hier1", hierarchy1);
        
        assertTrue(hierarchyDir.containsValue(hierarchy1));
    }

    @Test
    void containsValue_NonExistentValue_ReturnsFalse()
    {
        assertFalse(hierarchyDir.containsValue(hierarchy1));
    }

    @Test
    void keySet_PopulatedDirectory_ReturnsAllKeys()
    {
        hierarchyDir.put("hier1", hierarchy1);
        hierarchyDir.put("hier2", hierarchy2);
        
        Set<String> keys = hierarchyDir.keySet();
        
        assertEquals(2, keys.size());
        assertTrue(keys.contains("hier1"));
        assertTrue(keys.contains("hier2"));
    }

    @Test
    void values_PopulatedDirectory_ReturnsAllValues()
    {
        hierarchyDir.put("hier1", hierarchy1);
        hierarchyDir.put("hier2", hierarchy2);
        
        Collection<Hierarchy> values = hierarchyDir.values();
        
        assertEquals(2, values.size());
        assertTrue(values.contains(hierarchy1));
        assertTrue(values.contains(hierarchy2));
    }

    @Test
    void entrySet_PopulatedDirectory_ReturnsAllEntries()
    {
        hierarchyDir.put("hier1", hierarchy1);
        hierarchyDir.put("hier2", hierarchy2);
        
        Set<java.util.Map.Entry<String, Hierarchy>> entries = hierarchyDir.entrySet();
        
        assertEquals(2, entries.size());
        assertTrue(entries.stream().anyMatch(e -> e.getKey().equals("hier1") && e.getValue().equals(hierarchy1)));
        assertTrue(entries.stream().anyMatch(e -> e.getKey().equals("hier2") && e.getValue().equals(hierarchy2)));
    }

    @Test
    void isEmpty_EmptyDirectory_ReturnsTrue()
    {
        assertTrue(hierarchyDir.isEmpty());
    }

    @Test
    void isEmpty_PopulatedDirectory_ReturnsFalse()
    {
        hierarchyDir.put("hier1", hierarchy1);
        
        assertFalse(hierarchyDir.isEmpty());
    }

    @Test
    void size_EmptyDirectory_ReturnsZero()
    {
        assertEquals(0, hierarchyDir.size());
    }

    @Test
    void size_PopulatedDirectory_ReturnsCorrectCount()
    {
        hierarchyDir.put("hier1", hierarchy1);
        hierarchyDir.put("hier2", hierarchy2);
        hierarchyDir.put("hier3", hierarchy3);
        
        assertEquals(3, hierarchyDir.size());
    }

    @Test
    void constructor_WithNegativeCapacity_ThrowsException()
    {
        assertThrows(IllegalArgumentException.class, 
            () -> new HierarchyDirImpl(directoryDef, -1, 0.75f));
    }

    @Test
    void put_EmptyStringKey_AcceptsEmptyString()
    {
        Hierarchy result = hierarchyDir.put("", hierarchy1);
        
        assertNull(result);
        assertSame(hierarchy1, hierarchyDir.get(""));
        assertTrue(hierarchyDir.containsKey(""));
    }

    @Test
    void put_WhitespaceKey_AcceptsWhitespace()
    {
        String whitespaceKey = "  \t\n  ";
        Hierarchy result = hierarchyDir.put(whitespaceKey, hierarchy1);
        
        assertNull(result);
        assertSame(hierarchy1, hierarchyDir.get(whitespaceKey));
        assertTrue(hierarchyDir.containsKey(whitespaceKey));
    }
}