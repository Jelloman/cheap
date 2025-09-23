package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EntityTreeHierarchyImplTest
{
    private HierarchyDef hierarchyDef;
    private Entity rootEntity;
    private Entity childEntity1;
    private Entity childEntity2;
    private Entity leafEntity;
    private EntityTreeHierarchyImpl entityTree;

    @BeforeEach
    void setUp()
    {
        hierarchyDef = new HierarchyDefImpl("testEntityTree", HierarchyType.ENTITY_TREE);
        rootEntity = new EntityImpl();
        childEntity1 = new EntityImpl();
        childEntity2 = new EntityImpl();
        leafEntity = new EntityImpl();
        entityTree = new EntityTreeHierarchyImpl(hierarchyDef, rootEntity);
    }

    @Test
    void constructor_ValidParameters_CreatesTreeWithRoot()
    {
        EntityTreeHierarchyImpl tree = new EntityTreeHierarchyImpl(hierarchyDef, rootEntity);
        
        assertSame(hierarchyDef, tree.def());
        assertNotNull(tree.root());
        assertSame(rootEntity, tree.root().value());
        assertNull(tree.root().getParent());
        assertTrue(tree.root().isLeaf());
        assertTrue(tree.root().isEmpty());
    }

    @Test
    void constructor_NullHierarchyDef_AcceptsNull()
    {
        EntityTreeHierarchyImpl tree = new EntityTreeHierarchyImpl(null, rootEntity);
        
        assertNull(tree.def());
        assertNotNull(tree.root());
        assertSame(rootEntity, tree.root().value());
    }

    @Test
    void constructor_NullRootEntity_AcceptsNull()
    {
        EntityTreeHierarchyImpl tree = new EntityTreeHierarchyImpl(hierarchyDef);
        
        assertSame(hierarchyDef, tree.def());
        assertNotNull(tree.root());
        assertNull(tree.root().value());
    }

    @Test
    void def_Always_ReturnsHierarchyDef()
    {
        assertSame(hierarchyDef, entityTree.def());
    }

    @Test
    void root_Always_ReturnsRootNode()
    {
        EntityTreeHierarchy.Node root = entityTree.root();
        
        assertNotNull(root);
        assertSame(rootEntity, root.value());
        assertNull(root.getParent());
        assertTrue(root.isLeaf()); // has no children so is a leaf
    }

    // NodeImpl Tests
    @Test
    void nodeImpl_Constructor_WithValueOnly_CreatesNodeWithNoParent()
    {
        EntityTreeHierarchyImpl.NodeImpl node = new EntityTreeHierarchyImpl.NodeImpl(childEntity1);
        
        assertSame(childEntity1, node.value());
        assertNull(node.getParent());
        assertTrue(node.isLeaf()); // has no children so is a leaf
        assertTrue(node.isEmpty());
    }

    @Test
    void nodeImpl_Constructor_WithValueAndParent_CreatesNodeWithParent()
    {
        EntityTreeHierarchyImpl.NodeImpl parent = new EntityTreeHierarchyImpl.NodeImpl(rootEntity);
        EntityTreeHierarchyImpl.NodeImpl child = new EntityTreeHierarchyImpl.NodeImpl(childEntity1, parent);
        parent.put("child", child);

        assertSame(childEntity1, child.value());
        assertSame(parent, child.getParent());
        assertTrue(child.isLeaf()); // has no children so is a leaf
        assertFalse(parent.isLeaf());
    }

    @Test
    void nodeImpl_AddChild_AddsChildNode()
    {
        EntityTreeHierarchy.Node root = entityTree.root();
        EntityTreeHierarchyImpl.NodeImpl childNode = new EntityTreeHierarchyImpl.NodeImpl(childEntity1, root);
        
        root.put("child1", childNode);
        
        assertEquals(1, root.size());
        assertSame(childNode, root.get("child1"));
        assertTrue(root.containsKey("child1"));
        assertTrue(root.containsValue(childNode));
    }

    @Test
    void nodeImpl_AddMultipleChildren_AddsAllChildren()
    {
        EntityTreeHierarchy.Node root = entityTree.root();
        EntityTreeHierarchyImpl.NodeImpl child1 = new EntityTreeHierarchyImpl.NodeImpl(childEntity1, root);
        EntityTreeHierarchyImpl.NodeImpl child2 = new EntityTreeHierarchyImpl.NodeImpl(childEntity2, root);
        
        root.put("child1", child1);
        root.put("child2", child2);
        
        assertEquals(2, root.size());
        assertSame(child1, root.get("child1"));
        assertSame(child2, root.get("child2"));
    }

    @Test
    void nodeImpl_IsLeaf_ReturnsTrueOnlyWithChildren()
    {
        EntityTreeHierarchyImpl.NodeImpl node = new EntityTreeHierarchyImpl.NodeImpl(childEntity1);

        assertTrue(node.isLeaf()); // has no children so is a leaf

        node.put("child", new EntityTreeHierarchyImpl.NodeImpl(childEntity2));
        assertFalse(node.isLeaf());
    }

    @Test
    void nodeImpl_Value_ReturnsStoredEntity()
    {
        EntityTreeHierarchyImpl.NodeImpl node = new EntityTreeHierarchyImpl.NodeImpl(childEntity1);
        
        assertSame(childEntity1, node.value());
    }

    @Test
    void nodeImpl_Value_CanBeNull()
    {
        EntityTreeHierarchyImpl.NodeImpl node = new EntityTreeHierarchyImpl.NodeImpl(null);
        
        assertNull(node.value());
    }

    // LeafNodeImpl Tests
    @Test
    void leafNodeImpl_Constructor_WithValueOnly_CreatesLeafWithNoParent()
    {
        EntityTreeHierarchyImpl.LeafNodeImpl leaf = new EntityTreeHierarchyImpl.LeafNodeImpl(leafEntity);
        
        assertSame(leafEntity, leaf.value());
        assertNull(leaf.getParent());
        assertTrue(leaf.isLeaf());
        assertTrue(leaf.isEmpty());
    }

    @Test
    void leafNodeImpl_Constructor_WithValueAndParent_CreatesLeafWithParent()
    {
        EntityTreeHierarchy.Node parent = entityTree.root();
        EntityTreeHierarchyImpl.LeafNodeImpl leaf = new EntityTreeHierarchyImpl.LeafNodeImpl(leafEntity, parent);
        
        assertSame(leafEntity, leaf.value());
        assertSame(parent, leaf.getParent());
        assertTrue(leaf.isLeaf());
    }

    @Test
    void leafNodeImpl_IsLeaf_AlwaysReturnsTrue()
    {
        EntityTreeHierarchyImpl.LeafNodeImpl leaf = new EntityTreeHierarchyImpl.LeafNodeImpl(leafEntity);
        
        assertTrue(leaf.isLeaf());
    }

    @Test
    void leafNodeImpl_Value_ReturnsStoredEntity()
    {
        EntityTreeHierarchyImpl.LeafNodeImpl leaf = new EntityTreeHierarchyImpl.LeafNodeImpl(leafEntity);
        
        assertSame(leafEntity, leaf.value());
    }

    @Test
    void leafNodeImpl_Value_CanBeNull()
    {
        EntityTreeHierarchyImpl.LeafNodeImpl leaf = new EntityTreeHierarchyImpl.LeafNodeImpl(null);
        
        assertNull(leaf.value());
    }

    @Test
    void leafNodeImpl_EntrySet_ReturnsEmptySet()
    {
        EntityTreeHierarchyImpl.LeafNodeImpl leaf = new EntityTreeHierarchyImpl.LeafNodeImpl(leafEntity);
        
        Set<java.util.Map.Entry<String, EntityTreeHierarchy.Node>> entries = leaf.entrySet();
        
        assertNotNull(entries);
        assertTrue(entries.isEmpty());
    }

    @Test
    void leafNodeImpl_Size_AlwaysReturnsZero()
    {
        EntityTreeHierarchyImpl.LeafNodeImpl leaf = new EntityTreeHierarchyImpl.LeafNodeImpl(leafEntity);
        
        assertEquals(0, leaf.size());
    }

    @Test
    void leafNodeImpl_IsEmpty_AlwaysReturnsTrue()
    {
        EntityTreeHierarchyImpl.LeafNodeImpl leaf = new EntityTreeHierarchyImpl.LeafNodeImpl(leafEntity);
        
        assertTrue(leaf.isEmpty());
    }

    @Test
    void leafNodeImpl_ContainsKey_AlwaysReturnsFalse()
    {
        EntityTreeHierarchyImpl.LeafNodeImpl leaf = new EntityTreeHierarchyImpl.LeafNodeImpl(leafEntity);
        
        assertFalse(leaf.containsKey("anyKey"));
        assertFalse(leaf.containsKey(null));
        assertFalse(leaf.containsKey(""));
    }

    @Test
    void leafNodeImpl_ContainsValue_AlwaysReturnsFalse()
    {
        EntityTreeHierarchyImpl.LeafNodeImpl leaf = new EntityTreeHierarchyImpl.LeafNodeImpl(leafEntity);
        EntityTreeHierarchyImpl.NodeImpl node = new EntityTreeHierarchyImpl.NodeImpl(childEntity1);
        
        assertFalse(leaf.containsValue(node));
        assertFalse(leaf.containsValue(null));
    }

    @Test
    void leafNodeImpl_Get_AlwaysReturnsNull()
    {
        EntityTreeHierarchyImpl.LeafNodeImpl leaf = new EntityTreeHierarchyImpl.LeafNodeImpl(leafEntity);
        
        assertNull(leaf.get("anyKey"));
        assertNull(leaf.get(null));
        assertNull(leaf.get(""));
    }

    // Tree Structure Tests
    @Test
    void treeStructure_MultiLevelTree_WorksCorrectly()
    {
        EntityTreeHierarchy.Node root = entityTree.root();
        
        // Level 1: Add children to root
        EntityTreeHierarchyImpl.NodeImpl child1 = new EntityTreeHierarchyImpl.NodeImpl(childEntity1, root);
        EntityTreeHierarchyImpl.NodeImpl child2 = new EntityTreeHierarchyImpl.NodeImpl(childEntity2, root);
        root.put("child1", child1);
        root.put("child2", child2);
        
        // Level 2: Add grandchildren to child1
        Entity grandChild1 = new EntityImpl();
        Entity grandChild2 = new EntityImpl();
        EntityTreeHierarchyImpl.NodeImpl grandChild1Node = new EntityTreeHierarchyImpl.NodeImpl(grandChild1, child1);
        EntityTreeHierarchyImpl.LeafNodeImpl grandChild2Leaf = new EntityTreeHierarchyImpl.LeafNodeImpl(grandChild2, child1);
        child1.put("grandChild1", grandChild1Node);
        child1.put("grandChild2", grandChild2Leaf);
        
        // Verify structure
        assertEquals(2, root.size());
        assertEquals(2, child1.size());
        assertEquals(0, child2.size());
        
        assertSame(child1, root.get("child1"));
        assertSame(child2, root.get("child2"));
        assertSame(grandChild1Node, child1.get("grandChild1"));
        assertSame(grandChild2Leaf, child1.get("grandChild2"));
        
        // Verify parent-child relationships
        assertSame(root, child1.getParent());
        assertSame(root, child2.getParent());
        assertSame(child1, grandChild1Node.getParent());
        assertSame(child1, grandChild2Leaf.getParent());
        
        // Verify leaf status
        assertFalse(root.isLeaf());
        assertFalse(child1.isLeaf());
        assertTrue(child2.isLeaf());
        assertTrue(grandChild1Node.isLeaf());
        assertTrue(grandChild2Leaf.isLeaf());
    }

    @Test
    void treeStructure_MixedNodeTypes_WorksCorrectly()
    {
        EntityTreeHierarchy.Node root = entityTree.root();
        
        // Add both regular and leaf nodes as children
        EntityTreeHierarchyImpl.NodeImpl regularChild = new EntityTreeHierarchyImpl.NodeImpl(childEntity1, root);
        EntityTreeHierarchyImpl.LeafNodeImpl leafChild = new EntityTreeHierarchyImpl.LeafNodeImpl(childEntity2, root);
        
        root.put("regular", regularChild);
        root.put("leaf", leafChild);
        
        assertEquals(2, root.size());
        assertSame(regularChild, root.get("regular"));
        assertSame(leafChild, root.get("leaf"));
        
        assertTrue(regularChild.isLeaf());
        assertTrue(leafChild.isLeaf());
        
        // Regular child can have children
        EntityTreeHierarchyImpl.LeafNodeImpl grandChild = new EntityTreeHierarchyImpl.LeafNodeImpl(leafEntity, regularChild);
        regularChild.put("grandChild", grandChild);
        assertEquals(1, regularChild.size());
        
        // Leaf child cannot have children (conceptually - it returns empty for all map operations)
        assertEquals(0, leafChild.size());
        assertTrue(leafChild.isEmpty());
    }

    @Test
    void nodeImpl_RemoveChild_RemovesChildCorrectly()
    {
        EntityTreeHierarchy.Node root = entityTree.root();
        EntityTreeHierarchyImpl.NodeImpl child = new EntityTreeHierarchyImpl.NodeImpl(childEntity1, root);
        
        root.put("child", child);
        assertEquals(1, root.size());
        
        EntityTreeHierarchy.Node removed = root.remove("child");
        assertSame(child, removed);
        assertEquals(0, root.size());
        assertFalse(root.containsKey("child"));
    }

    @Test
    void nodeImpl_ClearChildren_RemovesAllChildren()
    {
        EntityTreeHierarchy.Node root = entityTree.root();
        
        root.put("child1", new EntityTreeHierarchyImpl.NodeImpl(childEntity1, root));
        root.put("child2", new EntityTreeHierarchyImpl.NodeImpl(childEntity2, root));
        assertEquals(2, root.size());
        
        root.clear();
        assertTrue(root.isEmpty());
    }

    @Test
    void treeStructure_DeepNesting_WorksCorrectly()
    {
        EntityTreeHierarchy.Node current = entityTree.root();
        
        // Create a deep tree (10 levels)
        for (int i = 0; i < 10; i++) {
            Entity entity = new EntityImpl();
            EntityTreeHierarchyImpl.NodeImpl child = new EntityTreeHierarchyImpl.NodeImpl(entity, current);
            current.put("level" + i, child);
            current = child;
        }
        
        // Verify we can traverse back up
        EntityTreeHierarchy.Node node = current;
        int levels = 0;
        while (node != null) {
            levels++;
            node = node.getParent();
        }
        
        assertEquals(11, levels); // 10 levels + root
    }

    @Test
    void nodeImpl_KeysWithSpecialCharacters_HandlesCorrectly()
    {
        EntityTreeHierarchy.Node root = entityTree.root();
        
        String[] specialKeys = {
            "child-with-dashes",
            "child_with_underscores",
            "child.with.dots",
            "child/with/slashes",
            "child with spaces",
            "",
            "αβγ_unicode"
        };
        
        for (String key : specialKeys) {
            EntityTreeHierarchyImpl.NodeImpl child = new EntityTreeHierarchyImpl.NodeImpl(new EntityImpl(), root);
            root.put(key, child);
        }
        
        assertEquals(specialKeys.length, root.size());
        
        for (String key : specialKeys) {
            assertTrue(root.containsKey(key));
            assertNotNull(root.get(key));
        }
    }
}