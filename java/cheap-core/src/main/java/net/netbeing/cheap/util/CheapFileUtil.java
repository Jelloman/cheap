package net.netbeing.cheap.util;

import lombok.experimental.UtilityClass;
import net.netbeing.cheap.impl.reflect.RecordAspect;
import net.netbeing.cheap.impl.reflect.RecordAspectDef;
import net.netbeing.cheap.model.AspectMapHierarchy;
import net.netbeing.cheap.model.Catalog;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class CheapFileUtil
{
    public final String FILE_REC_ASPECT_NAME = FileRec.class.getCanonicalName();

    public record FileRec(
        String name,
        Path path,
        long size,
        FileTime creationTime,
        FileTime modifiedTime,
        FileTime accessTime,
        boolean isSymLink,
        boolean isDirectory
    )
    {
        public FileRec(@NotNull Path p, @NotNull BasicFileAttributes a)
        {
            this(p.getFileName().toString(), p, a.size(),
                a.creationTime(), a.lastModifiedTime(), a.lastAccessTime(),
                a.isSymbolicLink(), a.isDirectory());
        }
    }

    public Stream<FileRec> streamDir(Path dir) throws IOException
    {
        return stream(dir, 1);
    }

    public Stream<FileRec> stream(Path dir, int maxDepth, FileVisitOption... options) throws IOException
    {
        final HashMap<Path,BasicFileAttributes> attrs = new HashMap<>();

        return Files.find(dir, maxDepth, (p,a) -> attrs.put(p,a) == null, options)
            .map(p -> new FileRec(p, attrs.remove(p)));
    }

    public Map<Path,FileRec> walkAll(Path dir, int maxDepth, FileVisitOption... options) throws IOException
    {
        try (Stream<FileRec> stream = stream(dir, maxDepth, options)) {
            return stream.collect(Collectors.toMap(FileRec::path, f -> f));
        }
    }

    public void loadFiles(Catalog catalog, Path dir, int maxDepth, FileVisitOption... options) throws IOException
    {
        RecordAspectDef aspectDef = (RecordAspectDef) catalog.aspectDef(FILE_REC_ASPECT_NAME);
        AspectMapHierarchy aspects = catalog.aspects(aspectDef);

        try (Stream<FileRec> stream = stream(dir, maxDepth, options)) {
            stream.forEach(rec -> aspects.add(new RecordAspect<>(catalog, aspectDef, rec)));
        }
    }


}
