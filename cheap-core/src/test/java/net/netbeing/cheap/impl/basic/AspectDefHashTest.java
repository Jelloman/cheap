package net.netbeing.cheap.impl.basic;

import com.google.common.collect.ImmutableMap;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for verifying that identical AspectDef instances produce the same hash value,
 * even when using different implementations (FullAspectDefImpl vs MutableAspectDefImpl).
 */
class AspectDefHashTest
{
    @Test
    void hash_IdenticalFullAndMutableAspectDefs_ProduceSameHash()
    {
        // Create shared UUID and property definitions
        UUID globalId = UUID.randomUUID();
        Map<String, PropertyDef> propertyDefs = ImmutableMap.of(
            "prop1", new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build(),
            "prop2", new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build()
        );

        // Create FullAspectDefImpl with all mutability flags set to true (same as MutableAspectDefImpl)
        FullAspectDefImpl fullAspectDef = new FullAspectDefImpl(
            "testAspect",
            globalId,
            new LinkedHashMap<>(propertyDefs),
            true,  // isReadable
            true,  // isWritable
            true,  // canAddProperties
            true   // canRemoveProperties
        );

        // Create MutableAspectDefImpl with same properties
        MutableAspectDefImpl mutableAspectDef = new MutableAspectDefImpl(
            "testAspect",
            globalId,
            new LinkedHashMap<>(propertyDefs)
        );

        // Verify both have the same hash
        assertEquals(fullAspectDef.hash(), mutableAspectDef.hash(),
            "FullAspectDefImpl and MutableAspectDefImpl with identical properties should produce the same hash");
    }

    @Test
    void hash_IdenticalFullAndImmutableAspectDefs_ProduceSameHash()
    {
        // Create shared UUID and property definitions
        UUID globalId = UUID.randomUUID();
        Map<String, PropertyDef> propertyDefs = ImmutableMap.of(
            "prop1", new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build(),
            "prop2", new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build()
        );

        // Create FullAspectDefImpl with all mutability flags set to false (same as ImmutableAspectDefImpl)
        FullAspectDefImpl fullAspectDef = new FullAspectDefImpl(
            "testAspect",
            globalId,
            new LinkedHashMap<>(propertyDefs),
            true,  // isReadable
            true,  // isWritable
            false, // canAddProperties
            false  // canRemoveProperties
        );

        // Create ImmutableAspectDefImpl with same properties
        ImmutableAspectDefImpl immutableAspectDef = new ImmutableAspectDefImpl(
            "testAspect",
            globalId,
            new LinkedHashMap<>(propertyDefs)
        );

        // Verify both have the same hash
        assertEquals(fullAspectDef.hash(), immutableAspectDef.hash(),
            "FullAspectDefImpl and ImmutableAspectDefImpl with identical properties should produce the same hash");
    }

    @Test
    void hash_TwoFullAspectDefsWithDifferentMutabilityFlags_ProduceDifferentHash()
    {
        // Create shared UUID and property definitions
        UUID globalId = UUID.randomUUID();
        Map<String, PropertyDef> propertyDefs = ImmutableMap.of(
            "prop1", new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build(),
            "prop2", new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build()
        );

        // Create FullAspectDefImpl with canAddProperties=true
        FullAspectDefImpl aspectDef1 = new FullAspectDefImpl(
            "testAspect",
            globalId,
            new LinkedHashMap<>(propertyDefs),
            true,  // isReadable
            true,  // isWritable
            true,  // canAddProperties
            false  // canRemoveProperties
        );

        // Create FullAspectDefImpl with canAddProperties=false
        FullAspectDefImpl aspectDef2 = new FullAspectDefImpl(
            "testAspect",
            globalId,
            new LinkedHashMap<>(propertyDefs),
            true,  // isReadable
            true,  // isWritable
            false, // canAddProperties (different!)
            false  // canRemoveProperties
        );

        // Verify they have different hashes
        assertTrue(aspectDef1.hash() != aspectDef2.hash(), "FullAspectDefImpl instances with different mutability flags should produce different hashes");
    }

    @Test
    void hash_MutableAndImmutableAspectDefs_ProduceDifferentHash()
    {
        // Create shared UUID and property definitions
        UUID globalId = UUID.randomUUID();
        Map<String, PropertyDef> propertyDefs = ImmutableMap.of(
            "prop1", new PropertyDefBuilder().setName("prop1").setType(PropertyType.String).build(),
            "prop2", new PropertyDefBuilder().setName("prop2").setType(PropertyType.Integer).build()
        );

        // Create MutableAspectDefImpl
        MutableAspectDefImpl mutableAspectDef = new MutableAspectDefImpl(
            "testAspect",
            globalId,
            new LinkedHashMap<>(propertyDefs)
        );

        // Create ImmutableAspectDefImpl
        ImmutableAspectDefImpl immutableAspectDef = new ImmutableAspectDefImpl(
            "testAspect",
            globalId,
            new LinkedHashMap<>(propertyDefs)
        );

        // Verify they have different hashes due to different mutability flags
        assertTrue(mutableAspectDef.hash() != immutableAspectDef.hash(), "MutableAspectDefImpl and ImmutableAspectDefImpl should produce different hashes");
    }
}
