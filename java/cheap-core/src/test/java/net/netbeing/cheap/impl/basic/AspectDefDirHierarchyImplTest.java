package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AspectDefDirHierarchyImplTest
{
    private HierarchyDef hierarchyDef;
    private AspectDefDirHierarchyImpl hierarchy;
    private AspectDef aspectDef1;
    private AspectDef aspectDef2;
    private AspectDef aspectDef3;

    @BeforeEach
    void setUp()
    {
        hierarchyDef = new HierarchyDefImpl("testHierarchy", HierarchyType.ENTITY_DIR);
        hierarchy = new AspectDefDirHierarchyImpl(hierarchyDef);
        
        aspectDef1 = new MutableAspectDefImpl("aspect1");
        aspectDef2 = new MutableAspectDefImpl("aspect2");
        aspectDef3 = new MutableAspectDefImpl("aspect3");
    }

    @Test
    void constructor_ValidHierarchyDef_CreatesEmptyHierarchy()
    {
        AspectDefDirHierarchyImpl hierarchy = new AspectDefDirHierarchyImpl(hierarchyDef);
        
        assertSame(hierarchyDef, hierarchy.def());
        assertNull(hierarchy.get("nonexistent"));
    }

    @Test
    void def_Always_ReturnsHierarchyDef()
    {
        assertSame(hierarchyDef, hierarchy.def());
    }

    @Test
    void add_NewAspectDef_AddsDefinition()
    {
        AspectDef result = hierarchy.add(aspectDef1);
        
        assertNull(result);
        assertSame(aspectDef1, hierarchy.get("aspect1"));
    }

    @Test
    void add_ExistingName_ReturnsOldDefinition()
    {
        hierarchy.add(aspectDef1);
        AspectDef newAspectDef = new MutableAspectDefImpl("aspect1");
        
        AspectDef result = hierarchy.add(newAspectDef);
        
        assertSame(aspectDef1, result);
        assertSame(newAspectDef, hierarchy.get("aspect1"));
    }

    @Test
    void add_MultipleAspectDefs_AddsAllDefinitions()
    {
        hierarchy.add(aspectDef1);
        hierarchy.add(aspectDef2);
        hierarchy.add(aspectDef3);
        
        assertSame(aspectDef1, hierarchy.get("aspect1"));
        assertSame(aspectDef2, hierarchy.get("aspect2"));
        assertSame(aspectDef3, hierarchy.get("aspect3"));
    }

    @Test
    void add_NullAspectDef_ThrowsException()
    {
        assertThrows(NullPointerException.class, () -> hierarchy.add(null));
    }

    @Test
    void get_NonExistentName_ReturnsNull()
    {
        assertNull(hierarchy.get("nonexistent"));
    }

    @Test
    void get_ExistingName_ReturnsAspectDef()
    {
        hierarchy.add(aspectDef1);
        
        assertSame(aspectDef1, hierarchy.get("aspect1"));
    }

    @Test
    void get_NullName_ReturnsNull()
    {
        assertNull(hierarchy.get(null));
    }

    @Test
    void get_EmptyName_ReturnsNull()
    {
        assertNull(hierarchy.get(""));
    }

    @Test
    void add_SameNameDifferentCase_TreatsAsDistinct()
    {
        AspectDef upperCaseAspect = new MutableAspectDefImpl("ASPECT1");
        
        hierarchy.add(aspectDef1); // "aspect1"
        hierarchy.add(upperCaseAspect); // "ASPECT1"
        
        assertSame(aspectDef1, hierarchy.get("aspect1"));
        assertSame(upperCaseAspect, hierarchy.get("ASPECT1"));
        assertNull(hierarchy.get("Aspect1"));
    }

    @Test
    void add_WithWhitespaceNames_HandlesCorrectly()
    {
        AspectDef whitespaceAspect = new MutableAspectDefImpl(" aspect with spaces ");
        
        hierarchy.add(whitespaceAspect);
        
        assertSame(whitespaceAspect, hierarchy.get(" aspect with spaces "));
        assertNull(hierarchy.get("aspect with spaces"));
    }

    @Test
    void add_ReplaceExisting_MaintainsInternalConsistency()
    {
        hierarchy.add(aspectDef1);
        AspectDef replacement = new MutableAspectDefImpl("aspect1");
        
        AspectDef oldValue = hierarchy.add(replacement);
        
        assertSame(aspectDef1, oldValue);
        assertSame(replacement, hierarchy.get("aspect1"));
        assertNotSame(aspectDef1, hierarchy.get("aspect1"));
    }

    @Test
    void add_ChainedOperations_WorksCorrectly()
    {
        AspectDef result1 = hierarchy.add(aspectDef1);
        AspectDef result2 = hierarchy.add(aspectDef2);
        AspectDef result3 = hierarchy.add(aspectDef3);
        
        assertNull(result1);
        assertNull(result2);
        assertNull(result3);
        
        assertSame(aspectDef1, hierarchy.get("aspect1"));
        assertSame(aspectDef2, hierarchy.get("aspect2"));
        assertSame(aspectDef3, hierarchy.get("aspect3"));
    }

    @Test
    void get_AfterMultipleAdds_ReturnsCorrectDefinitions()
    {
        hierarchy.add(aspectDef1);
        hierarchy.add(aspectDef2);
        hierarchy.add(aspectDef3);
        
        // Verify all can still be retrieved
        assertSame(aspectDef1, hierarchy.get("aspect1"));
        assertSame(aspectDef2, hierarchy.get("aspect2"));
        assertSame(aspectDef3, hierarchy.get("aspect3"));
        
        // Verify non-existent still returns null
        assertNull(hierarchy.get("aspect4"));
    }
}