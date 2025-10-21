package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.PropertyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PropertyDefImplTest
{

    @Test
    void constructor_BasicNameAndType_CreatesProperty()
    {
        PropertyDefImpl propDef = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();

        assertEquals("testProp", propDef.name());
        assertEquals(PropertyType.String, propDef.type());
        assertNull(propDef.defaultValue());
        assertFalse(propDef.hasDefaultValue());
        assertTrue(propDef.isReadable());
        assertTrue(propDef.isWritable());
        assertTrue(propDef.isNullable());
        assertTrue(propDef.isRemovable());
        assertFalse(propDef.isMultivalued());
    }

    @Test
    void constructor_WithNullType_ThrowsNullPointerException()
    {
        PropertyDefBuilder builder = new PropertyDefBuilder();
        builder.setName("testProp");
        builder.setType(null);
        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void constructor_WithEmptyName_ThrowsIllegalArgumentException()
    {
        PropertyDefBuilder builder = new PropertyDefBuilder();
        builder.setName("");
        builder.setType(PropertyType.String);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void constructor_WithAllParameters_CreatesProperty()
    {
        PropertyDefImpl propDef = new PropertyDefBuilder()
            .setName("testProp")
            .setType(PropertyType.Integer)
            .setIsReadable(true)
            .setIsWritable(false)
            .setIsNullable(false)
            .setIsRemovable(true)
            .setIsMultivalued(true)
            .build();

        assertEquals("testProp", propDef.name());
        assertEquals(PropertyType.Integer, propDef.type());
        assertNull(propDef.defaultValue());
        assertFalse(propDef.hasDefaultValue());
        assertTrue(propDef.isReadable());
        assertFalse(propDef.isWritable());
        assertFalse(propDef.isNullable());
        assertTrue(propDef.isRemovable());
        assertTrue(propDef.isMultivalued());
    }

    @Test
    void builder_WithDefaultValues_CreatesProperty()
    {
        PropertyDefImpl propDef = new PropertyDefBuilder()
            .setName("testProp")
            .setType(PropertyType.String)
            .setDefaultValue("defaultValue")
            .build();

        assertEquals("testProp", propDef.name());
        assertEquals(PropertyType.String, propDef.type());
        assertEquals("defaultValue", propDef.defaultValue());
        assertTrue(propDef.hasDefaultValue());
        assertTrue(propDef.isReadable());
        assertTrue(propDef.isWritable());
        assertTrue(propDef.isNullable());
        assertTrue(propDef.isRemovable());
        assertFalse(propDef.isMultivalued());
    }

    @Test
    void equals_DifferentPropertyDefsSameName_ReturnsTrue()
    {
        PropertyDefImpl propDef1 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();
        PropertyDefImpl propDef2 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.Integer).build();

        assertEquals(propDef1, propDef2);
    }

    @Test
    void equals_DifferentNames_ReturnsFalse()
    {
        PropertyDefImpl propDef1 = new PropertyDefBuilder().setName("testProp1").setType(PropertyType.String).build();
        PropertyDefImpl propDef2 = new PropertyDefBuilder().setName("testProp2").setType(PropertyType.String).build();

        assertNotEquals(propDef1, propDef2);
    }

    @Test
    void hashCode_SameName_ReturnsSameHashCode()
    {
        PropertyDefImpl propDef1 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();
        PropertyDefImpl propDef2 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.Integer).build();

        assertEquals(propDef1.hashCode(), propDef2.hashCode());
    }

    @Test
    void hashCode_DifferentNames_ReturnsDifferentHashCode()
    {
        PropertyDefImpl propDef1 = new PropertyDefBuilder().setName("testProp1").setType(PropertyType.String).build();
        PropertyDefImpl propDef2 = new PropertyDefBuilder().setName("testProp2").setType(PropertyType.String).build();

        assertNotEquals(propDef1.hashCode(), propDef2.hashCode());
    }

    @Test
    void fullyEquals_IdenticalInstancesConstructedSeparately_ReturnsTrue()
    {
        PropertyDefImpl propDef1 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();
        PropertyDefImpl propDef2 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();

        assertTrue(propDef1.fullyEquals(propDef2));
        assertTrue(propDef2.fullyEquals(propDef1));
    }

    @Test
    void fullyEquals_SameInstance_ReturnsTrue()
    {
        PropertyDefImpl propDef = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();

        assertTrue(propDef.fullyEquals(propDef));
    }

    @Test
    void fullyEquals_DifferentTypes_ReturnsFalse()
    {
        PropertyDefImpl propDef1 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();
        PropertyDefImpl propDef2 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.Integer).build();

        assertFalse(propDef1.fullyEquals(propDef2));
    }

    @Test
    void fullyEquals_DifferentNames_ReturnsFalse()
    {
        PropertyDefImpl propDef1 = new PropertyDefBuilder().setName("testProp1").setType(PropertyType.String).build();
        PropertyDefImpl propDef2 = new PropertyDefBuilder().setName("testProp2").setType(PropertyType.String).build();

        assertFalse(propDef1.fullyEquals(propDef2));
    }

    @Test
    void fullyEquals_DifferentAttributes_ReturnsFalse()
    {
        PropertyDefImpl propDef1 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).setIsReadable(true).setIsWritable(true).setIsNullable(true).setIsRemovable(true).setIsMultivalued(false).build();
        PropertyDefImpl propDef2 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).setIsReadable(true).setIsWritable(false).setIsNullable(true).setIsRemovable(true).setIsMultivalued(false).build();

        assertFalse(propDef1.fullyEquals(propDef2));
    }

    @Test
    void fullyEquals_WithNull_ReturnsFalse()
    {
        PropertyDefImpl propDef = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();

        assertFalse(propDef.fullyEquals(null));
    }

    @Test
    void hash_IdenticalInstancesConstructedSeparately_ReturnsSameHash()
    {
        PropertyDefImpl propDef1 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();
        PropertyDefImpl propDef2 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();

        assertEquals(propDef1.hash(), propDef2.hash());
    }

    @Test
    void hash_SameInstance_ReturnsSameHash()
    {
        PropertyDefImpl propDef = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();

        assertEquals(propDef.hash(), propDef.hash());
    }

    @Test
    void hash_DifferentTypes_ReturnsDifferentHash()
    {
        PropertyDefImpl propDef1 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();
        PropertyDefImpl propDef2 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.Integer).build();

        assertNotEquals(propDef1.hash(), propDef2.hash());
    }

    @Test
    void hash_DifferentNames_ReturnsDifferentHash()
    {
        PropertyDefImpl propDef1 = new PropertyDefBuilder().setName("testProp1").setType(PropertyType.String).build();
        PropertyDefImpl propDef2 = new PropertyDefBuilder().setName("testProp2").setType(PropertyType.String).build();

        assertNotEquals(propDef1.hash(), propDef2.hash());
    }

    @Test
    void hash_DifferentAttributes_ReturnsDifferentHash()
    {
        PropertyDefImpl propDef1 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).setIsReadable(true).setIsWritable(true).setIsNullable(true).setIsRemovable(true).setIsMultivalued(false).build();
        PropertyDefImpl propDef2 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).setIsReadable(true).setIsWritable(false).setIsNullable(true).setIsRemovable(true).setIsMultivalued(false).build();

        assertNotEquals(propDef1.hash(), propDef2.hash());
    }

    @Test
    void validatePropertyValue_ValidValue_ReturnsTrue()
    {
        PropertyDefImpl propDef = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();

        assertTrue(propDef.validatePropertyValue("validString", false));
    }

    @Test
    void validatePropertyValue_NullValueNullableProperty_ReturnsTrue()
    {
        PropertyDefImpl propDef = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).setIsReadable(true).setIsWritable(true).setIsNullable(true).setIsRemovable(true).setIsMultivalued(false).build();

        assertTrue(propDef.validatePropertyValue(null, false));
    }

    @Test
    void validatePropertyValue_NullValueNonNullableProperty_ReturnsFalse()
    {
        PropertyDefImpl propDef = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).setIsReadable(true).setIsWritable(true).setIsNullable(false).setIsRemovable(true).setIsMultivalued(false).build();

        assertFalse(propDef.validatePropertyValue(null, false));
    }

    @Test
    void validatePropertyValue_NullValueNonNullablePropertyThrowExceptions_ThrowsIllegalArgumentException()
    {
        PropertyDefImpl propDef = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).setIsReadable(true).setIsWritable(true).setIsNullable(false).setIsRemovable(true).setIsMultivalued(false).build();

        assertThrows(IllegalArgumentException.class, () -> propDef.validatePropertyValue(null, true));
    }

    @Test
    void validatePropertyValue_WrongType_ReturnsFalse()
    {
        PropertyDefImpl propDef = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();

        assertFalse(propDef.validatePropertyValue(123, false));
    }

    @Test
    void validatePropertyValue_WrongTypeThrowExceptions_ThrowsIllegalArgumentException()
    {
        PropertyDefImpl propDef = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();

        assertThrows(IllegalArgumentException.class, () -> propDef.validatePropertyValue(123, true));
    }

    @Test
    void name_IsInterned_UsesStringIntern()
    {
        PropertyDefImpl propDef1 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.String).build();
        PropertyDefImpl propDef2 = new PropertyDefBuilder().setName("testProp").setType(PropertyType.Integer).build();

        // Names should be the same reference due to String.intern()
        assertSame(propDef1.name(), propDef2.name());
    }
}