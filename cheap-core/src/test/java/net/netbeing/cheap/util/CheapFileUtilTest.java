package net.netbeing.cheap.util;

import net.netbeing.cheap.impl.basic.AspectMapHierarchyImpl;
import net.netbeing.cheap.impl.basic.CatalogImpl;
import net.netbeing.cheap.impl.basic.HierarchyDefImpl;
import net.netbeing.cheap.impl.reflect.RecordAspectDef;
import net.netbeing.cheap.impl.reflect.RecordAspect;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.model.EntityTreeHierarchy.Node;
import net.netbeing.cheap.util.CheapFileUtil.FileRec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class CheapFileUtilTest
{
    private Path testRoot;
    private Path subdir;
    private Path file1;
    private Path subdir2;
    private Path file2;
    private Catalog catalog;
    private RecordAspectDef fileRecAspectDef;

    @BeforeEach
    void setUp()
    {
        testRoot = Paths.get("src/test/resources/hierarchyTestDir");
        subdir = Paths.get("src/test/resources/hierarchyTestDir/subdir");
        file1 = Paths.get("src/test/resources/hierarchyTestDir/subdir/file1.txt");
        subdir2 = Paths.get("src/test/resources/hierarchyTestDir/subdir/subdir2");
        file2 = Paths.get("src/test/resources/hierarchyTestDir/subdir/subdir2/file2.txt");
        
        catalog = new CatalogImpl();
        fileRecAspectDef = new RecordAspectDef(FileRec.class);
        
        // Add the FileRec aspect definition to the catalog
        catalog.extend(fileRecAspectDef);
    }

    @Test
    void hierarchyTest() throws IOException
    {
        Map<Path, FileRec> files = CheapFileUtil.walkAll(testRoot, 10);

        //files.values().forEach(System.out::println);

        assertEquals(5, files.size());

        assertEquals("hierarchyTestDir", files.get(testRoot).name());
        assertTrue(files.get(testRoot).isDirectory());

        assertEquals("subdir", files.get(subdir).name());
        assertTrue(files.get(subdir).isDirectory());

        assertEquals("file1.txt", files.get(file1).name());
        assertFalse(files.get(file1).isDirectory());

        assertEquals("subdir2", files.get(subdir2).name());
        assertTrue(files.get(subdir2).isDirectory());

        assertEquals("file2.txt", files.get(file2).name());
        assertFalse(files.get(file2).isDirectory());
    }

    @Test
    void fileRecConstant_CorrectValue()
    {
        assertEquals(FileRec.class.getCanonicalName(), CheapFileUtil.FILE_REC_ASPECT_NAME);
    }

    @Test
    void aspectDef_AvailableInCatalog()
    {
        // Verify that the aspect definition was properly added to the catalog using the constant
        AspectMapHierarchy aMap = catalog.aspects(CheapFileUtil.FILE_REC_ASPECT_NAME);
        assertNotNull(aMap);
        assertEquals(fileRecAspectDef, aMap.aspectDef());
    }

    @Test
    void fileRec_Constructor_WithPathAndAttributes() throws IOException
    {
        Map<Path, FileRec> files = CheapFileUtil.walkAll(testRoot, 1);
        
        FileRec rootRec = files.get(testRoot);
        assertNotNull(rootRec);
        assertEquals("hierarchyTestDir", rootRec.name());
        assertEquals(testRoot, rootRec.path());
        assertTrue(rootRec.isDirectory());
        assertFalse(rootRec.isSymLink());
        assertNotNull(rootRec.creationTime());
        assertNotNull(rootRec.modifiedTime());
        assertNotNull(rootRec.accessTime());
    }

    @Test
    void streamDir_ReturnsDirectChildren() throws IOException
    {
        try (Stream<FileRec> stream = CheapFileUtil.streamDir(testRoot)) {
            List<FileRec> files = stream.toList();
            
            // Should include root directory and its immediate children
            assertEquals(2, files.size());
            
            // Verify we get the root directory and subdir
            assertTrue(files.stream().anyMatch(f -> f.name().equals("hierarchyTestDir")));
            assertTrue(files.stream().anyMatch(f -> f.name().equals("subdir")));
        }
    }

    @Test
    void stream_WithMaxDepth() throws IOException
    {
        // Test depth 1 - should get root + immediate children
        try (Stream<FileRec> stream = CheapFileUtil.stream(testRoot, 1)) {
            List<FileRec> files = stream.toList();
            assertEquals(2, files.size()); // root + subdir
        }

        // Test depth 2 - should get root + children + grandchildren
        try (Stream<FileRec> stream = CheapFileUtil.stream(testRoot, 2)) {
            List<FileRec> files = stream.toList();
            assertEquals(4, files.size()); // root + subdir + file1.txt + subdir2
        }

        // Test depth 3 - should get all files
        try (Stream<FileRec> stream = CheapFileUtil.stream(testRoot, 3)) {
            List<FileRec> files = stream.toList();
            assertEquals(5, files.size()); // all files
        }
    }

    @Test
    void stream_WithFileVisitOptions() throws IOException
    {
        // Test with FOLLOW_LINKS option (though we don't have symlinks in test data)
        try (Stream<FileRec> stream = CheapFileUtil.stream(testRoot, 10, FileVisitOption.FOLLOW_LINKS)) {
            List<FileRec> files = stream.toList();
            assertEquals(5, files.size());
            assertFalse(files.stream().anyMatch(FileRec::isSymLink));
        }
    }

    @Test
    void walkAll_ReturnsAllFiles() throws IOException
    {
        Map<Path, FileRec> files = CheapFileUtil.walkAll(testRoot, 10);

        assertEquals(5, files.size());
        assertTrue(files.containsKey(testRoot));
        assertTrue(files.containsKey(subdir));
        assertTrue(files.containsKey(file1));
        assertTrue(files.containsKey(subdir2));
        assertTrue(files.containsKey(file2));
    }

    @Test
    void walkAll_WithMaxDepthLimitation() throws IOException
    {
        Map<Path, FileRec> files = CheapFileUtil.walkAll(testRoot, 2);

        assertEquals(4, files.size()); // Should not include file2.txt which is at depth 3
        assertTrue(files.containsKey(testRoot));
        assertTrue(files.containsKey(subdir));
        assertTrue(files.containsKey(file1));
        assertTrue(files.containsKey(subdir2));
        assertFalse(files.containsKey(file2)); // This should be excluded due to depth limit
    }

    @Test
    void walkAll_WithFileVisitOptions() throws IOException
    {
        Map<Path, FileRec> files = CheapFileUtil.walkAll(testRoot, 10, FileVisitOption.FOLLOW_LINKS);

        assertEquals(5, files.size());
        assertTrue(files.containsKey(testRoot));
        assertTrue(files.containsKey(subdir));
        assertTrue(files.containsKey(file1));
        assertTrue(files.containsKey(subdir2));
        assertTrue(files.containsKey(file2));
    }

    @Test
    void fileRec_Properties() throws IOException
    {
        Map<Path, FileRec> files = CheapFileUtil.walkAll(testRoot, 10);
        
        // Test directory properties
        FileRec dirRec = files.get(testRoot);
        assertTrue(dirRec.isDirectory());
        assertFalse(dirRec.isSymLink());
        assertEquals(0, dirRec.size()); // Directories typically have 0 size
        assertNotNull(dirRec.creationTime());
        assertNotNull(dirRec.modifiedTime());
        assertNotNull(dirRec.accessTime());
        
        // Test file properties
        FileRec fileRec = files.get(file1);
        assertFalse(fileRec.isDirectory());
        assertFalse(fileRec.isSymLink());
        assertTrue(fileRec.size() > 0); // file1.txt has content
        assertNotNull(fileRec.creationTime());
        assertNotNull(fileRec.modifiedTime());
        assertNotNull(fileRec.accessTime());
    }

    @Test
    void fileRec_FileNames() throws IOException
    {
        Map<Path, FileRec> files = CheapFileUtil.walkAll(testRoot, 10);
        
        assertEquals("hierarchyTestDir", files.get(testRoot).name());
        assertEquals("subdir", files.get(subdir).name());
        assertEquals("file1.txt", files.get(file1).name());
        assertEquals("subdir2", files.get(subdir2).name());
        assertEquals("file2.txt", files.get(file2).name());
    }

    @Test
    void fileRec_FilePaths() throws IOException
    {
        Map<Path, FileRec> files = CheapFileUtil.walkAll(testRoot, 10);
        
        assertEquals(testRoot, files.get(testRoot).path());
        assertEquals(subdir, files.get(subdir).path());
        assertEquals(file1, files.get(file1).path());
        assertEquals(subdir2, files.get(subdir2).path());
        assertEquals(file2, files.get(file2).path());
    }

    @Test
    void loadFileRecordsOnly_LoadsIntoCatalog() throws IOException
    {
        // Load files into catalog
        CheapFileUtil.loadFileRecordsOnly(catalog, testRoot, 10);
        
        AspectMapHierarchy aspects = catalog.aspects(fileRecAspectDef);
        assertNotNull(aspects);
        
        // Verify that files were loaded (should have 5 entries)
        assertEquals(5, aspects.size());
    }

    @Test
    void loadFileRecordsOnly_WithMaxDepth() throws IOException
    {
        // Load with depth limit
        CheapFileUtil.loadFileRecordsOnly(catalog, testRoot, 2);
        
        AspectMapHierarchy aspects = catalog.aspects(fileRecAspectDef);
        assertNotNull(aspects);
        
        // Should have 4 entries (excludes file2.txt at depth 3)
        assertEquals(4, aspects.size());
    }

    @Test
    void stream_ClosesCorrectly() throws IOException
    {
        // Test that stream can be closed without issues
        Stream<FileRec> stream = CheapFileUtil.stream(testRoot, 10);
        assertNotNull(stream);
        
        List<FileRec> files = stream.toList();
        assertEquals(5, files.size());
        
        stream.close(); // Should not throw exception
    }

    @Test
    void streamDir_ClosesCorrectly() throws IOException
    {
        // Test that streamDir can be closed without issues
        Stream<FileRec> stream = CheapFileUtil.streamDir(testRoot);
        assertNotNull(stream);
        
        List<FileRec> files = stream.toList();
        assertEquals(2, files.size());
        
        stream.close(); // Should not throw exception
    }

    @Test
    void fileRec_Equality() throws IOException
    {
        Map<Path, FileRec> files1 = CheapFileUtil.walkAll(testRoot, 10);
        Map<Path, FileRec> files2 = CheapFileUtil.walkAll(testRoot, 10);
        
        // FileRec should have proper equals implementation (records have default equals)
        FileRec rec1 = files1.get(file1);
        FileRec rec2 = files2.get(file1);
        
        assertEquals(rec1, rec2);
        assertEquals(rec1.hashCode(), rec2.hashCode());
    }

    @Test
    void loadFileHierarchy_CreatesTreeStructure() throws IOException
    {
        HierarchyDefImpl treeHierarchyDef = new HierarchyDefImpl("fileTree", HierarchyType.ENTITY_TREE);
        
        CheapFileUtil.loadFileHierarchy(catalog, treeHierarchyDef, testRoot, 10);
        
        // Verify hierarchy was added to catalog
        EntityTreeHierarchy hierarchy = (EntityTreeHierarchy) catalog.hierarchy("fileTree");
        assertNotNull(hierarchy);
        assertEquals(treeHierarchyDef.name(), hierarchy.name());
        assertEquals(treeHierarchyDef.type(), hierarchy.type());
        
        // Verify root node
        Node root = hierarchy.root();
        assertNotNull(root);
        assertFalse(root.isLeaf()); // Root should be directory (non-leaf)
        assertNull(root.getParent()); // Root has no parent
        
        // Verify root has FileRec aspect
        Entity rootEntity = root.value();
        Aspect rootAspect = rootEntity.getAspect(fileRecAspectDef, catalog);
        assertNotNull(rootAspect);
        @SuppressWarnings("unchecked")
        FileRec rootFileRec = ((RecordAspect<FileRec>) rootAspect).record();
        assertEquals("hierarchyTestDir", rootFileRec.name());
        assertTrue(rootFileRec.isDirectory());
        
        // Verify aspects were loaded
        AspectMapHierarchy aspects = catalog.aspects(fileRecAspectDef);
        assertEquals(5, aspects.size()); // Should have 5 file records total
    }

    @Test
    void loadFileHierarchy_BuildsCorrectTreeStructure() throws IOException
    {
        HierarchyDefImpl treeHierarchyDef = new HierarchyDefImpl("fileTree", HierarchyType.ENTITY_TREE);
        
        CheapFileUtil.loadFileHierarchy(catalog, treeHierarchyDef, testRoot, 10);
        
        EntityTreeHierarchy hierarchy = (EntityTreeHierarchy) catalog.hierarchy("fileTree");
        Node root = hierarchy.root();
        
        // Navigate tree structure: root -> subdir
        assertTrue(root.containsKey("subdir"));
        Node subdirNode = root.get("subdir");
        assertNotNull(subdirNode);
        assertFalse(subdirNode.isLeaf()); // subdir is a directory
        assertEquals(root, subdirNode.getParent());
        
        // Verify subdir has FileRec aspect
        Entity subdirEntity = subdirNode.value();
        Aspect subdirAspect = subdirEntity.getAspect(fileRecAspectDef, catalog);
        @SuppressWarnings("unchecked")
        FileRec subdirFileRec = ((RecordAspect<FileRec>) subdirAspect).record();
        assertEquals("subdir", subdirFileRec.name());
        assertTrue(subdirFileRec.isDirectory());
        
        // Navigate tree structure: subdir -> file1.txt
        assertTrue(subdirNode.containsKey("file1.txt"));
        Node file1Node = subdirNode.get("file1.txt");
        assertNotNull(file1Node);
        assertTrue(file1Node.isLeaf()); // file1.txt is a file (leaf)
        assertEquals(subdirNode, file1Node.getParent());
        
        // Verify file1 has FileRec aspect
        Entity file1Entity = file1Node.value();
        Aspect file1Aspect = file1Entity.getAspect(fileRecAspectDef, catalog);
        @SuppressWarnings("unchecked")
        FileRec file1FileRec = ((RecordAspect<FileRec>) file1Aspect).record();
        assertEquals("file1.txt", file1FileRec.name());
        assertFalse(file1FileRec.isDirectory());
        
        // Navigate tree structure: subdir -> subdir2
        assertTrue(subdirNode.containsKey("subdir2"));
        Node subdir2Node = subdirNode.get("subdir2");
        assertNotNull(subdir2Node);
        assertFalse(subdir2Node.isLeaf()); // subdir2 is a directory
        assertEquals(subdirNode, subdir2Node.getParent());
        
        // Navigate tree structure: subdir2 -> file2.txt
        assertTrue(subdir2Node.containsKey("file2.txt"));
        Node file2Node = subdir2Node.get("file2.txt");
        assertNotNull(file2Node);
        assertTrue(file2Node.isLeaf()); // file2.txt is a file (leaf)
        assertEquals(subdir2Node, file2Node.getParent());
        
        // Verify file2 has FileRec aspect
        Entity file2Entity = file2Node.value();
        Aspect file2Aspect = file2Entity.getAspect(fileRecAspectDef, catalog);
        @SuppressWarnings("unchecked")
        FileRec file2FileRec = ((RecordAspect<FileRec>) file2Aspect).record();
        assertEquals("file2.txt", file2FileRec.name());
        assertFalse(file2FileRec.isDirectory());
    }

    @Test
    void loadFileHierarchy_WithMaxDepthLimitation() throws IOException
    {
        HierarchyDefImpl treeHierarchyDef = new HierarchyDefImpl("fileTree", HierarchyType.ENTITY_TREE);
        
        // Load with depth 2 (should exclude file2.txt at depth 3)
        CheapFileUtil.loadFileHierarchy(catalog, treeHierarchyDef, testRoot, 2);
        
        EntityTreeHierarchy hierarchy = (EntityTreeHierarchy) catalog.hierarchy("fileTree");
        Node root = hierarchy.root();
        
        // Should have root and subdir
        Node subdirNode = root.get("subdir");
        assertNotNull(subdirNode);
        
        // Should have file1.txt and subdir2
        assertTrue(subdirNode.containsKey("file1.txt"));
        assertTrue(subdirNode.containsKey("subdir2"));
        
        // But subdir2 should be empty (no file2.txt due to depth limit)
        Node subdir2Node = subdirNode.get("subdir2");
        assertNotNull(subdir2Node);
        assertTrue(subdir2Node.isEmpty()); // No children due to depth limit
        
        // Verify aspects count
        AspectMapHierarchy aspects = catalog.aspects(fileRecAspectDef);
        assertEquals(4, aspects.size()); // Should have 4 file records (excludes file2.txt)
    }

    @Test
    void loadFileHierarchy_WithFileVisitOptions() throws IOException
    {
        HierarchyDefImpl treeHierarchyDef = new HierarchyDefImpl("fileTree", HierarchyType.ENTITY_TREE);
        
        CheapFileUtil.loadFileHierarchy(catalog, treeHierarchyDef, testRoot, 10, FileVisitOption.FOLLOW_LINKS);
        
        EntityTreeHierarchy hierarchy = (EntityTreeHierarchy) catalog.hierarchy("fileTree");
        assertNotNull(hierarchy);
        
        // Should still build complete tree structure
        Node root = hierarchy.root();
        assertNotNull(root);
        assertTrue(root.containsKey("subdir"));
        
        Node subdirNode = root.get("subdir");
        assertTrue(subdirNode.containsKey("file1.txt"));
        assertTrue(subdirNode.containsKey("subdir2"));
        
        Node subdir2Node = subdirNode.get("subdir2");
        assertTrue(subdir2Node.containsKey("file2.txt"));
        
        // Verify all aspects were loaded
        AspectMapHierarchy aspects = catalog.aspects(fileRecAspectDef);
        assertEquals(5, aspects.size());
    }

    @Test
    void loadFileHierarchy_EmptyDirectory() throws IOException
    {
        HierarchyDefImpl treeHierarchyDef = new HierarchyDefImpl("fileTree", HierarchyType.ENTITY_TREE);
        Path emptyDir = Paths.get("src/test/resources/hierarchyTestDir/subdir/subdir2"); // This only contains file2.txt
        
        // Load with depth 1 to get only the directory itself
        CheapFileUtil.loadFileHierarchy(catalog, treeHierarchyDef, emptyDir, 1);
        
        EntityTreeHierarchy hierarchy = (EntityTreeHierarchy) catalog.hierarchy("fileTree");
        Node root = hierarchy.root();
        
        // Root should be the subdir2 directory
        Entity rootEntity = root.value();
        Aspect rootAspect = rootEntity.getAspect(fileRecAspectDef, catalog);
        @SuppressWarnings("unchecked")
        FileRec rootFileRec = ((RecordAspect<FileRec>) rootAspect).record();
        assertEquals("subdir2", rootFileRec.name());
        assertTrue(rootFileRec.isDirectory());
        
        // With depth 1, should also include file2.txt
        assertTrue(root.containsKey("file2.txt"));
        Node fileNode = root.get("file2.txt");
        assertTrue(fileNode.isLeaf());
    }

    @Test
    void loadFileHierarchy_SingleFile() throws IOException
    {
        HierarchyDefImpl treeHierarchyDef = new HierarchyDefImpl("fileTree", HierarchyType.ENTITY_TREE);
        Path singleFile = Paths.get("src/test/resources/hierarchyTestDir/subdir/file1.txt");
        
        CheapFileUtil.loadFileHierarchy(catalog, treeHierarchyDef, singleFile, 1);
        
        EntityTreeHierarchy hierarchy = (EntityTreeHierarchy) catalog.hierarchy("fileTree");
        Node root = hierarchy.root();
        
        // Root should be the single file
        assertTrue(root.isLeaf()); // Root is empty, so leaf
        Entity rootEntity = root.value();
        Aspect rootAspect = rootEntity.getAspect(fileRecAspectDef, catalog);
        @SuppressWarnings("unchecked")
        FileRec rootFileRec = ((RecordAspect<FileRec>) rootAspect).record();
        assertEquals("file1.txt", rootFileRec.name());
        assertFalse(rootFileRec.isDirectory());
        
        // Should have no children
        assertTrue(root.isEmpty());
        
        // Should have only one aspect
        AspectMapHierarchy aspects = catalog.aspects(fileRecAspectDef);
        assertEquals(1, aspects.size());
    }

    @Test
    void loadFileHierarchy_HierarchyAddedToCatalog() throws IOException
    {
        HierarchyDefImpl treeHierarchyDef = new HierarchyDefImpl("myFileTree", HierarchyType.ENTITY_TREE);
        
        // Verify hierarchy doesn't exist before
        assertNull(catalog.hierarchy("myFileTree"));
        
        CheapFileUtil.loadFileHierarchy(catalog, treeHierarchyDef, testRoot, 10);
        
        // Verify hierarchy was added to catalog
        Hierarchy hierarchy = catalog.hierarchy("myFileTree");
        assertNotNull(hierarchy);
        assertInstanceOf(EntityTreeHierarchy.class, hierarchy);
        assertEquals(treeHierarchyDef.name(), hierarchy.name());
        assertEquals(treeHierarchyDef.type(), hierarchy.type());
    }

    @Test
    void loadFileHierarchy_MultipleCallsWithSameHierarchyName() throws IOException
    {
        HierarchyDefImpl treeHierarchyDef1 = new HierarchyDefImpl("fileTree", HierarchyType.ENTITY_TREE);
        HierarchyDefImpl treeHierarchyDef2 = new HierarchyDefImpl("fileTree", HierarchyType.ENTITY_TREE);
        
        // First call
        CheapFileUtil.loadFileHierarchy(catalog, treeHierarchyDef1, testRoot, 2);
        EntityTreeHierarchy hierarchy1 = (EntityTreeHierarchy) catalog.hierarchy("fileTree");
        
        // Second call with same name should replace the first
        CheapFileUtil.loadFileHierarchy(catalog, treeHierarchyDef2, testRoot, 1);
        EntityTreeHierarchy hierarchy2 = (EntityTreeHierarchy) catalog.hierarchy("fileTree");
        
        assertNotEquals(hierarchy1, hierarchy2); // Should be different instances
        assertEquals("fileTree", hierarchy2.name());
        
        // The second hierarchy should have fewer nodes due to depth limit
        AspectMapHierarchy aspects = catalog.aspects(fileRecAspectDef);
        // Note: aspects accumulate because we're adding to the same AspectMapHierarchy
        assertTrue(aspects.size() >= 2); // At least root + subdir from second call
    }
}
