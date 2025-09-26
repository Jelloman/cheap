package net.netbeing.cheap.util;

import lombok.experimental.UtilityClass;
import net.netbeing.cheap.impl.basic.*;
import net.netbeing.cheap.impl.basic.EntityTreeHierarchyImpl.NodeImpl;
import net.netbeing.cheap.impl.basic.EntityTreeHierarchyImpl.LeafNodeImpl;
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

/**
 * Utility class for loading filesystem data into Cheap catalogs.
 * <p>
 * This class provides methods to traverse filesystem directories and convert file/directory 
 * information into Cheap model structures. Files and directories are represented as
 * {@link FileRec} record aspects and can be loaded into catalogs as either flat collections 
 * or hierarchical tree structures.
 * <p>
 * The utility supports various file traversal options including depth limits and 
 * symbolic link handling through standard NIO2 {@link FileVisitOption}s.
 * 
 * @see FileRec
 * @see EntityTreeHierarchy
 * @see RecordAspect
 */
@UtilityClass
public class CheapFileUtil
{
    /** Canonical name used as the aspect definition name for FileRec aspects. */
    public final String FILE_REC_ASPECT_NAME = FileRec.class.getCanonicalName();

    /**
     * Record representing file or directory metadata for use in Cheap aspects.
     * <p>
     * This record captures essential file system information including path, size,
     * timestamps, and file type flags. It serves as the data structure for
     * {@link RecordAspect} instances when loading filesystem data into catalogs.
     * 
     * @param name the filename (without path)
     * @param path the full path to the file or directory
     * @param size the size in bytes (0 for directories)
     * @param creationTime when the file was created
     * @param modifiedTime when the file was last modified
     * @param accessTime when the file was last accessed
     * @param isSymLink true if this is a symbolic link
     * @param isDirectory true if this is a directory
     */
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
        /**
         * Creates a FileRec from a path and its basic file attributes.
         * 
         * @param p the path to the file or directory
         * @param a the basic file attributes for the path
         */
        public FileRec(@NotNull Path p, @NotNull BasicFileAttributes a)
        {
            this(p.getFileName().toString(), p, a.size(),
                a.creationTime(), a.lastModifiedTime(), a.lastAccessTime(),
                a.isSymbolicLink(), a.isDirectory());
        }
    }

    /**
     * Creates a stream of FileRec objects for the immediate contents of a directory.
     * This is equivalent to calling {@link #stream(Path, int, FileVisitOption...)} with maxDepth=1.
     * 
     * @param dir the directory to stream
     * @return a stream of FileRec objects for directory contents
     * @throws IOException if an I/O error occurs during directory traversal
     */
    public Stream<FileRec> streamDir(Path dir) throws IOException
    {
        return stream(dir, 1);
    }

    /**
     * Creates a stream of FileRec objects for files and directories within the specified path and depth.
     * <p>
     * This method uses {@link Files#find(Path, int, java.util.function.BiPredicate, FileVisitOption...)} 
     * to traverse the filesystem and collect file attributes efficiently. The attributes are cached 
     * during traversal to avoid redundant I/O operations.
     * 
     * @param dir the root directory to start traversal from
     * @param maxDepth the maximum number of directory levels to traverse
     * @param options options to control how symbolic links are handled during traversal
     * @return a stream of FileRec objects representing files and directories found
     * @throws IOException if an I/O error occurs during directory traversal
     */
    public Stream<FileRec> stream(Path dir, int maxDepth, FileVisitOption... options) throws IOException
    {
        final HashMap<Path,BasicFileAttributes> attrs = new HashMap<>();

        return Files.find(dir, maxDepth, (p,a) -> attrs.put(p,a) == null, options)
            .map(p -> new FileRec(p, attrs.remove(p)));
    }

    /**
     * Traverses a directory tree and returns all files and directories as a path-to-FileRec map.
     * <p>
     * This is a convenience method that collects the entire stream result into a {@link Map}
     * for cases where random access to file information by path is needed.
     * 
     * @param dir the root directory to start traversal from
     * @param maxDepth the maximum number of directory levels to traverse
     * @param options options to control how symbolic links are handled during traversal
     * @return a map from Path to FileRec for all files and directories found
     * @throws IOException if an I/O error occurs during directory traversal
     */
    public Map<Path,FileRec> walkAll(Path dir, int maxDepth, FileVisitOption... options) throws IOException
    {
        try (Stream<FileRec> stream = stream(dir, maxDepth, options)) {
            return stream.collect(Collectors.toMap(FileRec::path, f -> f));
        }
    }

    /**
     * Loads file and directory information as FileRec aspects into a catalog's aspect map hierarchy.
     * <p>
     * This method creates {@link RecordAspect} instances for each file and directory found during
     * traversal and adds them to the catalog's aspect map hierarchy for the FileRec aspect type.
     * The aspects are not organized in any hierarchical structure - they are simply added as a
     * flat collection.
     * <p>
     * The catalog must already have a {@link RecordAspectDef} registered for the FileRec type
     * (using {@link #FILE_REC_ASPECT_NAME} as the aspect definition name).
     * 
     * @param catalog the catalog to load file records into
     * @param dir the root directory to start traversal from
     * @param maxDepth the maximum number of directory levels to traverse
     * @param options options to control how symbolic links are handled during traversal
     * @throws IOException if an I/O error occurs during directory traversal
     * @throws ClassCastException if the catalog's FileRec aspect definition is not a RecordAspectDef
     */
    public void loadFileRecordsOnly(Catalog catalog, Path dir, int maxDepth, FileVisitOption... options) throws IOException
    {
        AspectMapHierarchy aspects = catalog.aspects(FILE_REC_ASPECT_NAME);
        if (aspects == null) {
            throw new IllegalStateException("Catalog does not contain aspects named '" + FILE_REC_ASPECT_NAME + ".");
        }
        RecordAspectDef aspectDef = (RecordAspectDef) aspects.aspectDef();

        try (Stream<FileRec> stream = stream(dir, maxDepth, options)) {
            stream.forEach(rec -> aspects.add(new RecordAspect<>(new EntityImpl(), aspectDef, rec)));
        }
    }

    /**
     * Loads file and directory information into a catalog as both aspects and a hierarchical tree structure.
     * <p>
     * This method performs a depth-first traversal of the filesystem and creates:
     * <ul>
     *   <li>{@link RecordAspect} instances for each file/directory (added to the catalog's aspect map)</li>
     *   <li>An {@link EntityTreeHierarchy} that mirrors the filesystem directory structure</li>
     *   <li>Entities with local aspect maps containing the FileRec information</li>
     * </ul>
     * <p>
     * The tree hierarchy uses {@link NodeImpl} for directories and {@link LeafNodeImpl} for files.
     * Note that empty directories are treated as nodes (not leaves) to allow future child additions.
     * <p>
     * The catalog must already have a {@link RecordAspectDef} registered for the FileRec type.
     * The hierarchy will be created and registered in the catalog using the provided hierarchyDef.
     * 
     * @param catalog the catalog to load the hierarchy into
     * @param hierarchyDef the hierarchy definition for the tree structure
     * @param dir the root directory to start traversal from
     * @param maxDepth the maximum number of directory levels to traverse
     * @param options options to control how symbolic links are handled during traversal
     * @throws IOException if an I/O error occurs during directory traversal
     * @throws ClassCastException if the catalog's FileRec aspect definition is not a RecordAspectDef
     */
    public void loadFileHierarchy(@NotNull Catalog catalog, @NotNull HierarchyDef hierarchyDef,
                                  @NotNull Path dir, int maxDepth, FileVisitOption... options) throws IOException
    {
        AspectMapHierarchy aspects = catalog.aspects(FILE_REC_ASPECT_NAME);
        if (aspects == null) {
            throw new IllegalStateException("Catalog does not contain aspects named '" + FILE_REC_ASPECT_NAME + ".");
        }
        RecordAspectDef aspectDef = (RecordAspectDef) aspects.aspectDef();

        var walkState = new Object() {
            EntityTreeHierarchyImpl hierarchy;
            Node root;
            Node currentNode;
            FileRec currentFile;
        };

        try (Stream<FileRec> stream = stream(dir, maxDepth, options)) {
            stream.forEach(newFile -> {
                // Construct a new Entity for each file
                LocalEntity nodeEntity = new LocalEntityOneCatalogImpl(catalog);

                // Create the FileRec aspect and persist it to the aspectage
                RecordAspect<FileRec> aspect = new RecordAspect<>(aspectDef, newFile);
                nodeEntity.attachAndSave(aspect, catalog);

                // Create the hierarchy and root entity the first time through
                if (walkState.hierarchy == null) {
                    walkState.hierarchy = new EntityTreeHierarchyImpl(catalog, hierarchyDef, nodeEntity);
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
                        LocalEntity parentEntity = (LocalEntity) parent.value();
                        Aspect ancestorFileAspect = parentEntity.getAspect(aspectDef);
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
