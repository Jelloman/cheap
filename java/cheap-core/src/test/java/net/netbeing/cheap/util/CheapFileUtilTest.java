package net.netbeing.cheap.util;

import net.netbeing.cheap.impl.basic.AspectMapHierarchyImpl;
import net.netbeing.cheap.impl.basic.CatalogImpl;
import net.netbeing.cheap.impl.basic.HierarchyDefImpl;
import net.netbeing.cheap.impl.reflect.RecordAspectDef;
import net.netbeing.cheap.model.AspectDefDirHierarchy;
import net.netbeing.cheap.model.AspectMapHierarchy;
import net.netbeing.cheap.model.Catalog;
import net.netbeing.cheap.model.HierarchyType;
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
        AspectDefDirHierarchy aspectage = (AspectDefDirHierarchy) catalog.hierarchy("aspectage");
        aspectage.add(fileRecAspectDef);
        
        // Create and add the FileRec aspect hierarchy to the catalog
        HierarchyDefImpl hierarchyDef = new HierarchyDefImpl(fileRecAspectDef.name(), HierarchyType.ASPECT_MAP);
        AspectMapHierarchy aspectMapHierarchy = new AspectMapHierarchyImpl(hierarchyDef, fileRecAspectDef);
        catalog.hierarchies().put(fileRecAspectDef.name(), aspectMapHierarchy);
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
        assertNotNull(catalog.aspectDef(CheapFileUtil.FILE_REC_ASPECT_NAME));
        assertEquals(fileRecAspectDef, catalog.aspectDef(CheapFileUtil.FILE_REC_ASPECT_NAME));
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
}
