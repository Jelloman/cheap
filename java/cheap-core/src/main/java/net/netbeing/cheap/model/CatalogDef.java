package net.netbeing.cheap.model;

import com.google.common.hash.*;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A CatalogDef defines the structure and properties of a catalog.
 *
 * @see AspectDef
 * @see HierarchyDef
 */
public interface CatalogDef
{
    /**
     * Returns the globally unique identifier for this CatalogDef.
     * This may be the same as the Catalog UUID, which means that this
     * CatalogDef is a thin wrapper around the Catalog, returning exactly
     * the set of AspectDefs and HierarchyDefs in that Catalog at all times.
     *
     * @return the UUID identifying this CatalogDef globally
     */
    @NotNull UUID globalId();

    /**
     * Returns a read-only collection of the aspect definitions that are typically found
     * in a catalog with this definition. Catalogs flagged as "strict" will only contain
     * these Aspects; otherwise they may contain additional types of Aspects.
     *
     * @return collection of aspect definitions
     */
    @NotNull Iterable<AspectDef> aspectDefs();

    /**
     * Returns  a read-only collection of hierarchy definitions that are typically found
     * in a catalog with this definition. Catalogs flagged as "strict" will only contain
     * these Hierarchies; otherwise they may contain additional Hierarchies.
     *
     * @return collection of hierarchy definitions
     */
    @NotNull Iterable<HierarchyDef> hierarchyDefs();

    /**
     * Retrieves a hierarchy definition by name.
     *
     * @param name the name of the hierarchy definition to retrieve
     * @return the hierarchy definition with the given name, or {@code null} if not found
     */
    HierarchyDef hierarchyDef(String name);

    /**
     * Retrieves an aspect definition by name.
     *
     * @param name the name of the aspect definition to retrieve
     * @return the aspect definition with the given name, or {@code null} if not found
     */
    AspectDef aspectDef(String name);

    /**
     * Generate a Cheap-specific SHA-256 hash of this CatalogDef.
     * This hash should be consistent across all Cheap implementations.
     *
     * <P>Implementations of this interface should probably cache the result of this
     * default method for improved performance.</P>
     *
     * @return a HashCode
     */
    @SuppressWarnings("UnstableApiUsage")
    default HashCode hash()
    {
        //TODO: replace use of Hasher with language-independent algo
        Hasher hasher = Hashing.sha256().newHasher();
        hasher.putObject(this, FUNNEL);
        return hasher.hash();
    }

    static Funneler FUNNEL = new Funneler();

    @SuppressWarnings("UnstableApiUsage")
    class Funneler implements Funnel<CatalogDef>
    {
        @Override
        public void funnel(CatalogDef def, PrimitiveSink sink)
        {
            sink.putString(Objects.requireNonNull(def).globalId().toString(), UTF_8);
            for (AspectDef aDef : def.aspectDefs()) {
                AspectDef.FUNNEL.funnel(aDef, sink);
            }
            for (HierarchyDef hDef : def.hierarchyDefs()) {
                HierarchyDef.FUNNEL.funnel(hDef, sink);
            }
        }
    }

}
