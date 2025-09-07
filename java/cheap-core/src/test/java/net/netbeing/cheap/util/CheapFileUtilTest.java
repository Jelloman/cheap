package net.netbeing.cheap.util;

import net.netbeing.cheap.util.CheapFileUtil.FileRec;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CheapFileUtilTest
{

    @Test
    void hierarchyTest() throws IOException
    {
        Path root = Paths.get("src/test/resources/hierarchyTestDir");
        Path subdir = Paths.get("src/test/resources/hierarchyTestDir/subdir");
        Path text1 = Paths.get("src/test/resources/hierarchyTestDir/subdir/file1.txt");
        Path subdir2 = Paths.get("src/test/resources/hierarchyTestDir/subdir/subdir2");
        Path text2 = Paths.get("src/test/resources/hierarchyTestDir/subdir/subdir2/file2.txt");

        Map<Path, FileRec> files = CheapFileUtil.walkAll(root, 10);

        //files.values().forEach(System.out::println);

        assertEquals(5, files.size());

        assertEquals("hierarchyTestDir", files.get(root).name());
        assertTrue(files.get(root).isDirectory());

        assertEquals("subdir", files.get(subdir).name());
        assertTrue(files.get(subdir).isDirectory());

        assertEquals("file1.txt", files.get(text1).name());
        assertFalse(files.get(text1).isDirectory());

        assertEquals("subdir2", files.get(subdir2).name());
        assertTrue(files.get(subdir2).isDirectory());

        assertEquals("file2.txt", files.get(text2).name());
        assertFalse(files.get(text2).isDirectory());
    }


}
