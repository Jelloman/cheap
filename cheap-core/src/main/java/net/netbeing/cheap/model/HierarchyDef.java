package net.netbeing.cheap.model;

import com.google.common.hash.*;

import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Defines the metadata and characteristics of a hierarchy within the Cheap data model.
 * A hierarchy definition specifies the type, name, and mutability constraints of
 * a hierarchy instance.
 * 
 * <p>In the Cheap model, hierarchies provide the structural organization for entities
 * and aspects within a catalog. The HierarchyDef serves as the schema definition
 * that determines how a hierarchy behaves and what operations are permitted on it.</p>
 */
public interface HierarchyDef
{
    /**
     * Returns the unique name identifier for this hierarchy definition.
     * 
     * @return the hierarchy name, never null
     */
    String name();

    /**
     * Returns the type of hierarchy this definition describes.
     * The type determines the structure and behavior of hierarchy instances
     * created from this definition.
     * 
     * @return the hierarchy type, never null
     */
    HierarchyType type();

    /**
     * Generate a Cheap-specific SHA-256 hash of this HierarchyDef.
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
    class Funneler implements Funnel<HierarchyDef>
    {
        @Override
        public void funnel(HierarchyDef def, PrimitiveSink sink)
        {
            sink.putString(Objects.requireNonNull(def).name(), UTF_8);
            sink.putString(def.type().typeCode(), UTF_8);
        }
    }

}
