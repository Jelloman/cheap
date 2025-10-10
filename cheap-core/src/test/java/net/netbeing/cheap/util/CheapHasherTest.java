package net.netbeing.cheap.util;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the CheapHasher class.
 * Tests FNV-1a hash computation for all PropertyType javaClass types.
 */
@SuppressWarnings("NullableProblems")
class CheapHasherTest
{
    @Test
    void testHashString()
    {
        // Test static hash method
        String str1 = "hello";
        String str2 = "hello";
        String str3 = "world";

        long hash1 = CheapHasher.hash(str1);
        long hash2 = CheapHasher.hash(str2);
        long hash3 = CheapHasher.hash(str3);

        // Identical inputs should produce identical hashes
        assertEquals(hash1, hash2);

        // Different inputs should produce different hashes (highly likely)
        assertNotEquals(hash1, hash3);

        // Test null string
        long nullHash = CheapHasher.hash((String) null);
        assertNotEquals(hash1, nullHash);
    }

    @Test
    void testHashLong()
    {
        // Test static hash method
        Long long1 = 12345L;
        Long long2 = 12345L;
        Long long3 = 67890L;

        long hash1 = CheapHasher.hash(long1);
        long hash2 = CheapHasher.hash(long2);
        long hash3 = CheapHasher.hash(long3);

        // Identical inputs should produce identical hashes
        assertEquals(hash1, hash2);

        // Different inputs should produce different hashes
        assertNotEquals(hash1, hash3);
    }

    @Test
    void testHashDouble()
    {
        // Test static hash method
        double double1 = 3.14159;
        double double2 = 3.14159;
        double double3 = 2.71828;

        long hash1 = CheapHasher.hash(double1);
        long hash2 = CheapHasher.hash(double2);
        long hash3 = CheapHasher.hash(double3);

        // Identical inputs should produce identical hashes
        assertEquals(hash1, hash2);

        // Different inputs should produce different hashes
        assertNotEquals(hash1, hash3);
    }

    @Test
    void testHashBoolean()
    {
        // Test static hash method
        boolean bool1 = true;
        boolean bool2 = true;
        boolean bool3 = false;

        long hash1 = CheapHasher.hash(bool1);
        long hash2 = CheapHasher.hash(bool2);
        long hash3 = CheapHasher.hash(bool3);

        // Identical inputs should produce identical hashes
        assertEquals(hash1, hash2);

        // Different inputs should produce different hashes
        assertNotEquals(hash1, hash3);
    }

    @Test
    void testHashByteArray()
    {
        // Test static hash method
        byte[] bytes1 = new byte[]{1, 2, 3, 4, 5};
        byte[] bytes2 = new byte[]{1, 2, 3, 4, 5};
        byte[] bytes3 = new byte[]{5, 4, 3, 2, 1};

        long hash1 = CheapHasher.hash(bytes1);
        long hash2 = CheapHasher.hash(bytes2);
        long hash3 = CheapHasher.hash(bytes3);

        // Identical inputs should produce identical hashes
        assertEquals(hash1, hash2);

        // Different inputs should produce different hashes
        assertNotEquals(hash1, hash3);

        // Test null byte array
        long nullHash = CheapHasher.hash((byte[]) null);
        assertNotEquals(hash1, nullHash);

        // Test empty byte array
        byte[] empty = new byte[0];
        long emptyHash = CheapHasher.hash(empty);
        assertNotEquals(hash1, emptyHash);
    }

    @Test
    void testHashBigInteger()
    {
        // Test static hash method
        BigInteger bigInt1 = new BigInteger("123456789012345678901234567890");
        BigInteger bigInt2 = new BigInteger("123456789012345678901234567890");
        BigInteger bigInt3 = new BigInteger("987654321098765432109876543210");

        long hash1 = CheapHasher.hash(bigInt1);
        long hash2 = CheapHasher.hash(bigInt2);
        long hash3 = CheapHasher.hash(bigInt3);

        // Identical inputs should produce identical hashes
        assertEquals(hash1, hash2);

        // Different inputs should produce different hashes
        assertNotEquals(hash1, hash3);

        // Test null BigInteger
        long nullHash = CheapHasher.hash((BigInteger) null);
        assertNotEquals(hash1, nullHash);
    }

    @Test
    void testHashBigDecimal()
    {
        // Test static hash method
        BigDecimal bigDec1 = new BigDecimal("123456789.987654321");
        BigDecimal bigDec2 = new BigDecimal("123456789.987654321");
        BigDecimal bigDec3 = new BigDecimal("987654321.123456789");

        long hash1 = CheapHasher.hash(bigDec1);
        long hash2 = CheapHasher.hash(bigDec2);
        long hash3 = CheapHasher.hash(bigDec3);

        // Identical inputs should produce identical hashes
        assertEquals(hash1, hash2);

        // Different inputs should produce different hashes
        assertNotEquals(hash1, hash3);

        // Test null BigDecimal
        long nullHash = CheapHasher.hash((BigDecimal) null);
        assertNotEquals(hash1, nullHash);
    }

    @Test
    void testHashZonedDateTime()
    {
        // Test static hash method
        ZonedDateTime time1 = ZonedDateTime.parse("2025-01-15T10:30:00Z");
        ZonedDateTime time2 = ZonedDateTime.parse("2025-01-15T10:30:00Z");
        ZonedDateTime time3 = ZonedDateTime.parse("2025-12-31T23:59:59Z");

        long hash1 = CheapHasher.hash(time1);
        long hash2 = CheapHasher.hash(time2);
        long hash3 = CheapHasher.hash(time3);

        // Identical inputs should produce identical hashes
        assertEquals(hash1, hash2);

        // Different inputs should produce different hashes
        assertNotEquals(hash1, hash3);

        // Test null ZonedDateTime
        long nullHash = CheapHasher.hash((ZonedDateTime) null);
        assertNotEquals(hash1, nullHash);
    }

    @Test
    void testHashURI()
    {
        // Test static hash method
        URI uri1 = URI.create("https://example.com/path");
        URI uri2 = URI.create("https://example.com/path");
        URI uri3 = URI.create("https://example.org/other");

        long hash1 = CheapHasher.hash(uri1);
        long hash2 = CheapHasher.hash(uri2);
        long hash3 = CheapHasher.hash(uri3);

        // Identical inputs should produce identical hashes
        assertEquals(hash1, hash2);

        // Different inputs should produce different hashes
        assertNotEquals(hash1, hash3);

        // Test null URI
        long nullHash = CheapHasher.hash((URI) null);
        assertNotEquals(hash1, nullHash);
    }

    @Test
    void testHashUUID()
    {
        // Test static hash method
        UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID uuid3 = UUID.fromString("987fcdeb-51a2-43f1-89ab-fedcba987654");

        long hash1 = CheapHasher.hash(uuid1);
        long hash2 = CheapHasher.hash(uuid2);
        long hash3 = CheapHasher.hash(uuid3);

        // Identical inputs should produce identical hashes
        assertEquals(hash1, hash2);

        // Different inputs should produce different hashes
        assertNotEquals(hash1, hash3);

        // Test null UUID
        long nullHash = CheapHasher.hash((UUID) null);
        assertNotEquals(hash1, nullHash);
    }

    @Test
    void testInstanceHasher()
    {
        // Test instance-based rolling hash
        CheapHasher hasher = new CheapHasher();

        // Update with multiple values
        hasher.update("hello");
        long hash1 = hasher.getHash();

        hasher.update(42L);
        long hash2 = hasher.getHash();

        hasher.update(3.14);
        long hash3 = hasher.getHash();

        // Each update should change the hash
        assertNotEquals(hash1, hash2);
        assertNotEquals(hash2, hash3);
    }

    @Test
    void testInstanceHasherReset()
    {
        // Test reset functionality
        CheapHasher hasher = new CheapHasher();

        hasher.update("hello");
        long hash1 = hasher.getHash();

        hasher.reset();
        long resetHash = hasher.getHash();

        // After reset, hash should return to default offset basis
        CheapHasher fresh = new CheapHasher();
        assertEquals(fresh.getHash(), resetHash);

        hasher.update("hello");
        long hash2 = hasher.getHash();

        // After reset and same update, should get same hash
        assertEquals(hash1, hash2);
    }

    @Test
    void testInstanceHasherWithSeed()
    {
        // Test custom seed
        long seed = 0x123456789ABCDEF0L;
        CheapHasher hasher1 = new CheapHasher(seed);
        CheapHasher hasher2 = new CheapHasher(seed);

        assertEquals(hasher1.getHash(), hasher2.getHash());

        hasher1.update("test");
        hasher2.update("test");

        assertEquals(hasher1.getHash(), hasher2.getHash());
    }

    @Test
    void testInstanceHasherResetWithSeed()
    {
        // Test reset with custom seed
        long seed = 0xFEDCBA9876543210L;
        CheapHasher hasher = new CheapHasher();

        hasher.reset(seed);
        assertEquals(seed, hasher.getHash());

        hasher.update("data");
        long hash1 = hasher.getHash();

        hasher.reset(seed);
        hasher.update("data");
        long hash2 = hasher.getHash();

        // Resetting to same seed and applying same updates should give same result
        assertEquals(hash1, hash2);
    }

    @Test
    void testInstanceUpdateString()
    {
        CheapHasher hasher = new CheapHasher();
        hasher.update("test");

        // Compare with static method
        long staticHash = CheapHasher.hash("test");
        assertEquals(staticHash, hasher.getHash());
    }

    @Test
    void testInstanceUpdateLong()
    {
        CheapHasher hasher = new CheapHasher();
        hasher.update(12345L);

        // Compare with static method
        long staticHash = CheapHasher.hash(12345L);
        assertEquals(staticHash, hasher.getHash());
    }

    @Test
    void testInstanceUpdateDouble()
    {
        CheapHasher hasher = new CheapHasher();
        hasher.update(3.14159);

        // Compare with static method
        long staticHash = CheapHasher.hash(3.14159);
        assertEquals(staticHash, hasher.getHash());
    }

    @Test
    void testInstanceUpdateBoolean()
    {
        CheapHasher hasher = new CheapHasher();
        hasher.update(true);

        // Compare with static method
        long staticHash = CheapHasher.hash(true);
        assertEquals(staticHash, hasher.getHash());
    }

    @Test
    void testInstanceUpdateByteArray()
    {
        byte[] bytes = new byte[]{1, 2, 3, 4, 5};
        CheapHasher hasher = new CheapHasher();
        hasher.update(bytes);

        // Compare with static method
        long staticHash = CheapHasher.hash(bytes);
        assertEquals(staticHash, hasher.getHash());
    }

    @Test
    void testInstanceUpdateBigInteger()
    {
        BigInteger bigInt = new BigInteger("123456789012345678901234567890");
        CheapHasher hasher = new CheapHasher();
        hasher.update(bigInt);

        // Compare with static method
        long staticHash = CheapHasher.hash(bigInt);
        assertEquals(staticHash, hasher.getHash());
    }

    @Test
    void testInstanceUpdateBigDecimal()
    {
        BigDecimal bigDec = new BigDecimal("123456789.987654321");
        CheapHasher hasher = new CheapHasher();
        hasher.update(bigDec);

        // Compare with static method
        long staticHash = CheapHasher.hash(bigDec);
        assertEquals(staticHash, hasher.getHash());
    }

    @Test
    void testInstanceUpdateZonedDateTime()
    {
        ZonedDateTime time = ZonedDateTime.parse("2025-01-15T10:30:00Z");
        CheapHasher hasher = new CheapHasher();
        hasher.update(time);

        // Compare with static method
        long staticHash = CheapHasher.hash(time);
        assertEquals(staticHash, hasher.getHash());
    }

    @Test
    void testInstanceUpdateURI()
    {
        URI uri = URI.create("https://example.com/path");
        CheapHasher hasher = new CheapHasher();
        hasher.update(uri);

        // Compare with static method
        long staticHash = CheapHasher.hash(uri);
        assertEquals(staticHash, hasher.getHash());
    }

    @Test
    void testInstanceUpdateUUID()
    {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        CheapHasher hasher = new CheapHasher();
        hasher.update(uuid);

        // Compare with static method
        long staticHash = CheapHasher.hash(uuid);
        assertEquals(staticHash, hasher.getHash());
    }

    @Test
    void testMethodChaining()
    {
        // Test that instance methods return this for chaining
        CheapHasher hasher = new CheapHasher();

        CheapHasher result = hasher
            .update("hello")
            .update(42L)
            .update(3.14)
            .update(true)
            .update(new byte[]{1, 2, 3});

        assertSame(hasher, result);
    }

    @Test
    void testRollingHashOrder()
    {
        // Test that order matters for rolling hash
        CheapHasher hasher1 = new CheapHasher();
        hasher1.update("hello").update("world");

        CheapHasher hasher2 = new CheapHasher();
        hasher2.update("world").update("hello");

        // Different order should produce different hashes
        assertNotEquals(hasher1.getHash(), hasher2.getHash());
    }

    @Test
    void testConsistentHashAcrossInstances()
    {
        // Test that multiple instances produce consistent hashes
        String testString = "consistent test";

        long hash1 = CheapHasher.hash(testString);
        long hash2 = CheapHasher.hash(testString);
        long hash3 = CheapHasher.hash(testString);

        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    @Test
    void testNullHandling()
    {
        // Test null handling for all types
        // All null values should hash the same
        CheapHasher hasher = new CheapHasher();

        hasher.update((String) null);
        long hash1 = hasher.getHash();

        hasher.reset();
        hasher.update((byte[]) null);
        assertEquals(hash1, hasher.getHash());

        hasher.reset();
        hasher.update((BigInteger) null);
        assertEquals(hash1, hasher.getHash());

        hasher.reset();
        hasher.update((BigDecimal) null);
        assertEquals(hash1, hasher.getHash());

        hasher.reset();
        hasher.update((ZonedDateTime) null);
        assertEquals(hash1, hasher.getHash());

        hasher.reset();
        hasher.update((URI) null);
        assertEquals(hash1, hasher.getHash());

        hasher.reset();
        hasher.update((UUID) null);
        assertEquals(hash1, hasher.getHash());
    }

    @Test
    void testComplexRollingHash()
    {
        // Test rolling hash with multiple different types
        CheapHasher hasher1 = new CheapHasher();
        hasher1
            .update("test string")
            .update(42L)
            .update(3.14159)
            .update(true)
            .update(new BigInteger("123456789"))
            .update(new BigDecimal("987.654"))
            .update(ZonedDateTime.parse("2025-01-15T10:30:00Z"))
            .update(URI.create("https://example.com"))
            .update(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
            .update(new byte[]{1, 2, 3, 4, 5});

        CheapHasher hasher2 = new CheapHasher();
        hasher2
            .update("test string")
            .update(42L)
            .update(3.14159)
            .update(true)
            .update(new BigInteger("123456789"))
            .update(new BigDecimal("987.654"))
            .update(ZonedDateTime.parse("2025-01-15T10:30:00Z"))
            .update(URI.create("https://example.com"))
            .update(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
            .update(new byte[]{1, 2, 3, 4, 5});

        // Same sequence should produce same hash
        assertEquals(hasher1.getHash(), hasher2.getHash());
    }

    @Test
    void testDifferentValuesSameType()
    {
        // Ensure different values of the same type produce different hashes
        ImmutableList<String> strings = ImmutableList.of("a", "b", "c", "abc", "xyz", "test", "hello", "world");
        ImmutableList<Long> longs = ImmutableList.of(0L, 1L, 2L, 100L, 1000L, -1L, -100L, Long.MAX_VALUE, Long.MIN_VALUE);
        ImmutableList<Double> doubles = ImmutableList.of(0.0, 1.0, -1.0, 3.14, 2.71, Double.MAX_VALUE, Double.MIN_VALUE);

        // Test strings
        for (int i = 0; i < strings.size(); i++) {
            for (int j = i + 1; j < strings.size(); j++) {
                assertNotEquals(
                    CheapHasher.hash(strings.get(i)),
                    CheapHasher.hash(strings.get(j)),
                    "Different strings should have different hashes: " + strings.get(i) + " vs " + strings.get(j)
                );
            }
        }

        // Test longs
        for (int i = 0; i < longs.size(); i++) {
            for (int j = i + 1; j < longs.size(); j++) {
                assertNotEquals(
                    CheapHasher.hash(longs.get(i)),
                    CheapHasher.hash(longs.get(j)),
                    "Different longs should have different hashes: " + longs.get(i) + " vs " + longs.get(j)
                );
            }
        }

        // Test doubles
        for (int i = 0; i < doubles.size(); i++) {
            for (int j = i + 1; j < doubles.size(); j++) {
                assertNotEquals(
                    CheapHasher.hash(doubles.get(i)),
                    CheapHasher.hash(doubles.get(j)),
                    "Different doubles should have different hashes: " + doubles.get(i) + " vs " + doubles.get(j)
                );
            }
        }
    }

    @Test
    void testUTF8Encoding()
    {
        // Test that strings are properly encoded as UTF-8
        String unicodeString = "Hello \u4E16\u754C"; // "Hello 世界"
        long hash = CheapHasher.hash(unicodeString);

        // Should produce consistent hash
        assertEquals(hash, CheapHasher.hash(unicodeString));

        // Different unicode strings should produce different hashes
        String otherUnicode = "Hello \u65E5\u672C"; // "Hello 日本"
        assertNotEquals(hash, CheapHasher.hash(otherUnicode));
    }

    @Test
    void testEmptyInputs()
    {
        // Test empty inputs
        long emptyStringHash = CheapHasher.hash("");
        long emptyBytesHash = CheapHasher.hash(new byte[0]);

        // Empty string and empty bytes should produce different hashes
        // (because string goes through UTF-8 encoding path)
        // Actually they should be the same since empty string encodes to empty byte array
        assertEquals(emptyStringHash, emptyBytesHash);

        // But different from a non-empty input
        assertNotEquals(emptyStringHash, CheapHasher.hash("a"));
    }

    @Test
    void testBigDecimalPrecision()
    {
        // Test that BigDecimal values with different precision hash differently
        BigDecimal bd1 = new BigDecimal("1.0");
        BigDecimal bd2 = new BigDecimal("1.00");

        // These have different string representations, so should hash differently
        if (!bd1.toString().equals(bd2.toString())) {
            assertNotEquals(CheapHasher.hash(bd1), CheapHasher.hash(bd2));
        } else {
            assertEquals(CheapHasher.hash(bd1), CheapHasher.hash(bd2));
        }
    }

    @Test
    void testUUIDComponents()
    {
        // Test that UUIDs are hashed based on their bits, not string representation
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        long hash1 = CheapHasher.hash(uuid);

        // Same UUID should always produce same hash
        UUID sameUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        long hash2 = CheapHasher.hash(sameUuid);

        assertEquals(hash1, hash2);

        // UUID with only 1 bit different should produce different hash
        UUID differentUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        long hash3 = CheapHasher.hash(differentUuid);

        assertNotEquals(hash1, hash3);
    }

    @Test
    void testStaticHashWithSeed()
    {
        // Test that static methods accept seed parameter
        long seed = 0x123456789ABCDEF0L;
        String testString = "test";

        // Hash with default seed
        long defaultHash = CheapHasher.hash(testString);

        // Hash with custom seed
        long customHash = CheapHasher.hash(seed, testString);

        // Should produce different hashes
        assertNotEquals(defaultHash, customHash);

        // Same seed and value should produce same hash
        long customHash2 = CheapHasher.hash(seed, testString);
        assertEquals(customHash, customHash2);
    }

    @Test
    void testStaticHashWithSeedAllTypes()
    {
        // Test seed parameter for all types
        long seed = 0xFEDCBA9876543210L;

        // Test with different types
        long stringHash1 = CheapHasher.hash(seed, "test");
        long stringHash2 = CheapHasher.hash(seed, "test");
        assertEquals(stringHash1, stringHash2);

        long longHash1 = CheapHasher.hash(seed, 12345L);
        long longHash2 = CheapHasher.hash(seed, 12345L);
        assertEquals(longHash1, longHash2);

        long doubleHash1 = CheapHasher.hash(seed, 3.14159);
        long doubleHash2 = CheapHasher.hash(seed, 3.14159);
        assertEquals(doubleHash1, doubleHash2);

        long boolHash1 = CheapHasher.hash(seed, true);
        long boolHash2 = CheapHasher.hash(seed, true);
        assertEquals(boolHash1, boolHash2);

        byte[] bytes = new byte[]{1, 2, 3};
        long bytesHash1 = CheapHasher.hash(seed, bytes);
        long bytesHash2 = CheapHasher.hash(seed, bytes);
        assertEquals(bytesHash1, bytesHash2);

        BigInteger bigInt = new BigInteger("123456789");
        long bigIntHash1 = CheapHasher.hash(seed, bigInt);
        long bigIntHash2 = CheapHasher.hash(seed, bigInt);
        assertEquals(bigIntHash1, bigIntHash2);

        BigDecimal bigDec = new BigDecimal("987.654");
        long bigDecHash1 = CheapHasher.hash(seed, bigDec);
        long bigDecHash2 = CheapHasher.hash(seed, bigDec);
        assertEquals(bigDecHash1, bigDecHash2);

        ZonedDateTime time = ZonedDateTime.parse("2025-01-15T10:30:00Z");
        long timeHash1 = CheapHasher.hash(seed, time);
        long timeHash2 = CheapHasher.hash(seed, time);
        assertEquals(timeHash1, timeHash2);

        URI uri = URI.create("https://example.com");
        long uriHash1 = CheapHasher.hash(seed, uri);
        long uriHash2 = CheapHasher.hash(seed, uri);
        assertEquals(uriHash1, uriHash2);

        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        long uuidHash1 = CheapHasher.hash(seed, uuid);
        long uuidHash2 = CheapHasher.hash(seed, uuid);
        assertEquals(uuidHash1, uuidHash2);
    }

    @Test
    void testStaticHashWithDifferentSeeds()
    {
        // Test that different seeds produce different hashes
        String value = "same value";

        long seed1 = 0x1111111111111111L;
        long seed2 = 0x2222222222222222L;
        long seed3 = 0x3333333333333333L;

        long hash1 = CheapHasher.hash(seed1, value);
        long hash2 = CheapHasher.hash(seed2, value);
        long hash3 = CheapHasher.hash(seed3, value);

        // Different seeds should produce different hashes
        assertNotEquals(hash1, hash2);
        assertNotEquals(hash2, hash3);
        assertNotEquals(hash1, hash3);
    }

    @Test
    void testInstanceUsesStaticMethods()
    {
        // Test that instance methods use static methods with instance hash as seed
        CheapHasher hasher = new CheapHasher();

        // First update
        String value1 = "first";
        hasher.update(value1);
        long afterFirst = hasher.getHash();

        // This should be equivalent to hash(defaultSeed, value1)
        long staticEquivalent1 = CheapHasher.hash(value1);
        assertEquals(staticEquivalent1, afterFirst);

        // Second update
        String value2 = "second";
        hasher.update(value2);
        long afterSecond = hasher.getHash();

        // This should be equivalent to hash(afterFirst, value2)
        long staticEquivalent2 = CheapHasher.hash(afterFirst, value2);
        assertEquals(staticEquivalent2, afterSecond);
    }

    @Test
    void testRollingHashUsingSeeds()
    {
        // Manually verify rolling hash behavior using seeds
        long seed = 0xABCDEF0123456789L;

        // Build hash step by step
        long hash1 = CheapHasher.hash(seed, "hello");
        long hash2 = CheapHasher.hash(hash1, 42L);
        long hash3 = CheapHasher.hash(hash2, true);

        // Should match instance-based rolling hash
        CheapHasher hasher = new CheapHasher(seed);
        hasher.update("hello").update(42L).update(true);

        assertEquals(hash3, hasher.getHash());
    }

    @Test
    void testSeedWithNullValues()
    {
        // Test that null handling works correctly with custom seeds
        long seed = 0x9999999999999999L;

        long nullHash1 = CheapHasher.hash(seed, (String) null);
        long nullHash2 = CheapHasher.hash(seed, (ZonedDateTime) null);
        long nullHash3 = CheapHasher.hash(seed, (BigInteger) null);

        // All null values should produce the same hash with same seed
        assertEquals(nullHash1, nullHash2);
        assertEquals(nullHash2, nullHash3);

        // But different from non-null values
        assertNotEquals(nullHash1, CheapHasher.hash(seed, ""));
        assertNotEquals(nullHash1, CheapHasher.hash(seed, 0L));
        assertNotEquals(nullHash1, CheapHasher.hash(seed, false));
    }
}
