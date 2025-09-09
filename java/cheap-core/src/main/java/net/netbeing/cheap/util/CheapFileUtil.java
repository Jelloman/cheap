package net.netbeing.cheap.util;

import lombok.experimental.UtilityClass;
import net.netbeing.cheap.impl.basic.EntityLazyIdImpl;
import net.netbeing.cheap.impl.basic.EntityTreeHierarchyImpl;
import net.netbeing.cheap.impl.basic.EntityTreeHierarchyImpl.NodeImpl;
import net.netbeing.cheap.impl.basic.EntityTreeHierarchyImpl.LeafNodeImpl;
import net.netbeing.cheap.impl.basic.WeakAspectMap;
import net.netbeing.cheap.impl.reflect.RecordAspect;
import net.netbeing.cheap.impl.reflect.RecordAspectDef;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.model.EntityTreeHierarchy.Node;
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

    public void loadFileRecordsOnly(Catalog catalog, Path dir, int maxDepth, FileVisitOption... options) throws IOException
    {
        RecordAspectDef aspectDef = (RecordAspectDef) catalog.aspectDef(FILE_REC_ASPECT_NAME);
        AspectMapHierarchy aspects = catalog.aspects(aspectDef);

        try (Stream<FileRec> stream = stream(dir, maxDepth, options)) {
            stream.forEach(rec -> aspects.add(new RecordAspect<>(catalog, aspectDef, rec)));
        }
    }

    public void loadFileHierarchy(@NotNull Catalog catalog, @NotNull HierarchyDef hierarchyDef,
                                  @NotNull Path dir, int maxDepth, FileVisitOption... options) throws IOException
    {
        final RecordAspectDef aspectDef = (RecordAspectDef) catalog.aspectDef(FILE_REC_ASPECT_NAME);
        final AspectMapHierarchy aspects = catalog.aspects(aspectDef);

        var walkState = new Object() {
            EntityTreeHierarchyImpl hierarchy;
            Node root;
            Node currentNode;
            FileRec currentFile;
        };

        try (Stream<FileRec> stream = stream(dir, maxDepth, options)) {
            stream.forEach(newFile -> {
                // Construct a new Entity for each file
                Entity nodeEntity = new EntityLazyIdImpl();

                // Create the FileRec aspect and add it to the aspectage
                RecordAspect<FileRec> aspect = new RecordAspect<>(catalog, nodeEntity, aspectDef, newFile);
                aspects.add(aspect);
                
                // Add the aspect to the entity's local aspect map so it can be retrieved later
                ((WeakAspectMap) nodeEntity.local().aspects()).add(aspect);

                // Create the hierarchy and root entity the first time through
                if (walkState.hierarchy == null) {
                    walkState.hierarchy = new EntityTreeHierarchyImpl(hierarchyDef, nodeEntity);
                    catalog.hierarchies().put(hierarchyDef.name(), walkState.hierarchy);
                    walkState.root = walkState.hierarchy.root();
                    walkState.currentNode = walkState.root;
                    walkState.currentFile = newFile;
                    return;
                }
                // otherwise this is not a root, so insert it into the tree hierarchy in the right place.
                Path parentPath = newFile.path.getParent();
                Node parent;
                if (parentPath.equals(walkState.currentFile.path)) { // Child of previous node
                    parent = walkState.currentNode;
                } else if (parentPath.equals(walkState.currentFile.path.getParent())) { // Sibling of previous node
                    parent = walkState.currentNode.getParent();
                } else {
                    // This is DFS, so if it's not a child or a sibling, it must be the sibling of
                    // an ancestor. Walk up the tree to find it.
                    parent = walkState.currentNode;
                    FileRec ancestorFile;
                    do {
                        parent = parent.getParent();
                        Aspect ancestorFileAspect = parent.value().local().aspect(aspectDef);
                        // We can safely assume this aspect is a RecordAspect<FileRec> since we made it here.
                        @SuppressWarnings("unchecked")
                        FileRec fileRec = ((RecordAspect<FileRec>) ancestorFileAspect).record();
                        ancestorFile = fileRec;
                    } while (!parentPath.equals(ancestorFile.path));
                }
                // Create the proper type of tree node; note that this means empty directories are NOT leaves
                Node node = newFile.isDirectory ? new NodeImpl(nodeEntity, parent) : new LeafNodeImpl(nodeEntity, parent);
                parent.put(newFile.name, node);
                walkState.currentNode = node;
                walkState.currentFile = newFile;
            });
        }
    }


}
