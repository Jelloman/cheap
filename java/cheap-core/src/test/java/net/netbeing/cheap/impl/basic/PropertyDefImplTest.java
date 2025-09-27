package net.netbeing.cheap.impl.basic;

import com.google.common.collect.ImmutableList;
import net.netbeing.cheap.model.PropertyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PropertyDefImplTest
{

    @Test
    void constructor_BasicNameAndType_CreatesProperty()
    {
        PropertyDefImpl propDef = new PropertyDefImpl("testProp", PropertyType.String);

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
        assertThrows(NullPointerException.class, () -> new PropertyDefImpl("testProp", null));
    }

    @Test
    void constructor_WithEmptyName_ThrowsIllegalArgumentException()
    {
        assertThrows(IllegalArgumentException.class, () -> new PropertyDefImpl("", PropertyType.String));
    }

    @Test
    void constructor_WithAllParameters_CreatesProperty()
    {
        PropertyDefImpl propDef = new PropertyDefImpl(
            "testProp",
            PropertyType.Integer,
            true,  // isReadable
            false, // isWritable
            false, // isNullable
            true,  // isRemovable
            true   // isMultivalued
        );

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
    void constructor_WithDefaultValue_CreatesProperty()
    {
        PropertyDefImpl propDef = new PropertyDefImpl(
            "testProp",
            PropertyType.String,
            "defaultValue",
            true,  // hasDefaultValue
            true,  // isReadable
            true,  // isWritable
            true,  // isNullable
            true,  // isRemovable
            false  // isMultivalued
        );

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
    void readOnly_CreatesReadOnlyProperty()
    {
        PropertyDefImpl propDef = PropertyDefImpl.readOnly("readOnlyProp", PropertyType.Boolean, true, false);

        assertEquals("readOnlyProp", propDef.name());
        assertEquals(PropertyType.Boolean, propDef.type());
        assertNull(propDef.defaultValue());
        assertFalse(propDef.hasDefaultValue());
        assertTrue(propDef.isReadable());
        assertFalse(propDef.isWritable());
        assertTrue(propDef.isNullable());
        assertFalse(propDef.isRemovable());
        assertFalse(propDef.isMultivalued());
    }

    @Test
    void equals_DifferentPropertyDefsSameName_ReturnsTrue()
    {
        PropertyDefImpl propDef1 = new PropertyDefImpl("testProp", PropertyType.String);
        PropertyDefImpl propDef2 = new PropertyDefImpl("testProp", PropertyType.Integer);

        assertEquals(propDef1, propDef2);
    }

    @Test
    void equals_DifferentNames_ReturnsFalse()
    {
        PropertyDefImpl propDef1 = new PropertyDefImpl("testProp1", PropertyType.String);
        PropertyDefImpl propDef2 = new PropertyDefImpl("testProp2", PropertyType.String);

        assertNotEquals(propDef1, propDef2);
    }

    @Test
    void hashCode_SameName_ReturnsSameHashCode()
    {
        PropertyDefImpl propDef1 = new PropertyDefImpl("testProp", PropertyType.String);
        PropertyDefImpl propDef2 = new PropertyDefImpl("testProp", PropertyType.Integer);

        assertEquals(propDef1.hashCode(), propDef2.hashCode());
    }

    @Test
    void hashCode_DifferentNames_ReturnsDifferentHashCode()
    {
        PropertyDefImpl propDef1 = new PropertyDefImpl("testProp1", PropertyType.String);
        PropertyDefImpl propDef2 = new PropertyDefImpl("testProp2", PropertyType.String);

        assertNotEquals(propDef1.hashCode(), propDef2.hashCode());
    }

    @Test
    void fullyEquals_IdenticalInstancesConstructedSeparately_ReturnsTrue()
    {
        PropertyDefImpl propDef1 = new PropertyDefImpl("testProp", PropertyType.String);
        PropertyDefImpl propDef2 = new PropertyDefImpl("testProp", PropertyType.String);

        assertTrue(propDef1.fullyEquals(propDef2));
        assertTrue(propDef2.fullyEquals(propDef1));
    }

    @Test
    void fullyEquals_SameInstance_ReturnsTrue()
    {
        PropertyDefImpl propDef = new PropertyDefImpl("testProp", PropertyType.String);

        assertTrue(propDef.fullyEquals(propDef));
    }

    @Test
    void fullyEquals_DifferentTypes_ReturnsFalse()
    {
        PropertyDefImpl propDef1 = new PropertyDefImpl("testProp", PropertyType.String);
        PropertyDefImpl propDef2 = new PropertyDefImpl("testProp", PropertyType.Integer);

        assertFalse(propDef1.fullyEquals(propDef2));
    }

    @Test
    void fullyEquals_DifferentNames_ReturnsFalse()
    {
        PropertyDefImpl propDef1 = new PropertyDefImpl("testProp1", PropertyType.String);
        PropertyDefImpl propDef2 = new PropertyDefImpl("testProp2", PropertyType.String);

        assertFalse(propDef1.fullyEquals(propDef2));
    }

    @Test
    void fullyEquals_DifferentAttributes_ReturnsFalse()
    {
        PropertyDefImpl propDef1 = new PropertyDefImpl("testProp", PropertyType.String, true, true, true, true, false);
        PropertyDefImpl propDef2 = new PropertyDefImpl("testProp", PropertyType.String, true, false, true, true, false);

        assertFalse(propDef1.fullyEquals(propDef2));
    }

    @Test
    void fullyEquals_WithNull_ReturnsFalse()
    {
        PropertyDefImpl propDef = new PropertyDefImpl("testProp", PropertyType.String);

        assertFalse(propDef.fullyEquals(null));
    }

    @Test
    void hash_IdenticalInstancesConstructedSeparately_ReturnsSameHash()
    {
        PropertyDefImpl propDef1 = new PropertyDefImpl("testProp", PropertyType.String);
        PropertyDefImpl propDef2 = new PropertyDefImpl("testProp", PropertyType.String);

        assertEquals(propDef1.hash(), propDef2.hash());
    }

    @Test
    void hash_SameInstance_ReturnsSameHash()
    {
        PropertyDefImpl propDef = new PropertyDefImpl("testProp", PropertyType.String);

        assertEquals(propDef.hash(), propDef.hash());
    }

    @Test
    void hash_DifferentTypes_ReturnsDifferentHash()
    {
        PropertyDefImpl propDef1 = new PropertyDefImpl("testProp", PropertyType.String);
        PropertyDefImpl propDef2 = new PropertyDefImpl("testProp", PropertyType.Integer);

        assertNotEquals(propDef1.hash(), propDef2.hash());
    }

    @Test
    void hash_DifferentNames_ReturnsDifferentHash()
    {
        PropertyDefImpl propDef1 = new PropertyDefImpl("testProp1", PropertyType.String);
        PropertyDefImpl propDef2 = new PropertyDefImpl("testProp2", PropertyType.String);

        assertNotEquals(propDef1.hash(), propDef2.hash());
    }

    @Test
    void hash_DifferentAttributes_ReturnsDifferentHash()
    {
        PropertyDefImpl propDef1 = new PropertyDefImpl("testProp", PropertyType.String, true, true, true, true, false);
        PropertyDefImpl propDef2 = new PropertyDefImpl("testProp", PropertyType.String, true, false, true, true, false);

        assertNotEquals(propDef1.hash(), propDef2.hash());
    }

    @Test
    void validatePropertyValue_ValidValue_ReturnsTrue()
    {
        PropertyDefImpl propDef = new PropertyDefImpl("testProp", PropertyType.String);

        assertTrue(propDef.validatePropertyValue("validString", false));
    }

    @Test
    void validatePropertyValue_NullValueNullableProperty_ReturnsTrue()
    {
        PropertyDefImpl propDef = new PropertyDefImpl("testProp", PropertyType.String, true, true, true, true, false);

        assertTrue(propDef.validatePropertyValue(null, false));
    }

    @Test
    void validatePropertyValue_NullValueNonNullableProperty_ReturnsFalse()
    {
        PropertyDefImpl propDef = new PropertyDefImpl("testProp", PropertyType.String, true, true, false, true, false);

        assertFalse(propDef.validatePropertyValue(null, false));
    }

    @Test
    void validatePropertyValue_NullValueNonNullablePropertyThrowExceptions_ThrowsIllegalArgumentException()
    {
        PropertyDefImpl propDef = new PropertyDefImpl("testProp", PropertyType.String, true, true, false, true, false);

        assertThrows(IllegalArgumentException.class, () -> propDef.validatePropertyValue(null, true));
    }

    @Test
    void validatePropertyValue_WrongType_ReturnsFalse()
    {
        PropertyDefImpl propDef = new PropertyDefImpl("testProp", PropertyType.String);

        assertFalse(propDef.validatePropertyValue(123, false));
    }

    @Test
    void validatePropertyValue_WrongTypeThrowExceptions_ThrowsIllegalArgumentException()
    {
        PropertyDefImpl propDef = new PropertyDefImpl("testProp", PropertyType.String);

        assertThrows(IllegalArgumentException.class, () -> propDef.validatePropertyValue(123, true));
    }

    @Test
    void name_IsInterned_UsesStringIntern()
    {
        PropertyDefImpl propDef1 = new PropertyDefImpl("testProp", PropertyType.String);
        PropertyDefImpl propDef2 = new PropertyDefImpl("testProp", PropertyType.Integer);

        // Names should be the same reference due to String.intern()
        assertSame(propDef1.name(), propDef2.name());
    }
}