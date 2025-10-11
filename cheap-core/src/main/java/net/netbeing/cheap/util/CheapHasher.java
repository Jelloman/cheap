/*
 * Copyright (c) 2025. David Noha
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.netbeing.cheap.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Utility class for computing 64-bit FNV-1a hash values of various typed inputs.
 *
 * <p>The FNV-1a (Fowler-Noll-Vo) hash algorithm is a fast, non-cryptographic hash
 * function that provides good distribution for hash table and checksum applications.</p>
 *
 * <p>This class provides two modes of operation:</p>
 * <ul>
 *   <li><b>Static methods</b> - Compute standalone hash values for individual inputs</li>
 *   <li><b>Instance methods</b> - Maintain a rolling hash that can be updated incrementally</li>
 * </ul>
 *
 * <p>Supports all Java classes used by PropertyType enum values:</p>
 * <ul>
 *   <li>Long (Integer type)</li>
 *   <li>Double (Float type)</li>
 *   <li>Boolean (Boolean type)</li>
 *   <li>String (String, Text, CLOB types)</li>
 *   <li>BigInteger (BigInteger type)</li>
 *   <li>BigDecimal (BigDecimal type)</li>
 *   <li>ZonedDateTime (DateTime type)</li>
 *   <li>URI (URI type)</li>
 *   <li>UUID (UUID type)</li>
 *   <li>byte[] (BLOB type)</li>
 * </ul>
 */
public class CheapHasher
{
    // FNV-1a 64-bit constants
    private static final long FNV_OFFSET_BASIS_64 = 0xcbf29ce484222325L;
    private static final long FNV_PRIME_64 = 0x100000001b3L;

    /**
     * The current rolling hash value maintained by this instance.
     */
    private long hash;

    /**
     * Creates a new CheapHasher with the default FNV-1a offset basis.
     */
    public CheapHasher()
    {
        this.hash = FNV_OFFSET_BASIS_64;
    }

    /**
     * Creates a new CheapHasher with a custom seed value.
     *
     * @param seed the initial hash value
     */
    public CheapHasher(long seed)
    {
        this.hash = seed;
    }

    /**
     * Returns the current rolling hash value.
     *
     * @return the current hash value
     */
    public long getHash()
    {
        return hash;
    }

    /**
     * Resets the rolling hash to the default FNV-1a offset basis.
     */
    public void reset()
    {
        this.hash = FNV_OFFSET_BASIS_64;
    }

    /**
     * Resets the rolling hash to the specified seed value.
     *
     * @param seed the new hash value
     */
    public void reset(long seed)
    {
        this.hash = seed;
    }

    // ===== Static Hash Methods =====

    /**
     * Computes the FNV-1a hash of a byte array using the default offset basis.
     *
     * @param bytes the byte array to hash
     * @return the 64-bit hash value
     */
    public static long hash(byte[] bytes)
    {
        return hash(FNV_OFFSET_BASIS_64, bytes);
    }

    /**
     * Computes the FNV-1a hash of a byte array using the specified seed.
     *
     * @param seed the initial hash value (seed)
     * @param bytes the byte array to hash
     * @return the 64-bit hash value
     */
    public static long hash(long seed, byte[] bytes)
    {
        if (bytes == null) {
            return hashNull(seed);
        }

        long hash = seed;
        for (byte b : bytes) {
            hash ^= (b & 0xff);
            hash *= FNV_PRIME_64;
        }
        return hash;
    }

    /**
     * Computes the FNV-1a hash of a String converted to UTF-8 bytes using the default offset basis.
     *
     * @param value the string to hash
     * @return the 64-bit hash value
     */
    public static long hash(@Nullable String value)
    {
        return hash(FNV_OFFSET_BASIS_64, value);
    }

    /**
     * Computes the FNV-1a hash of a String converted to UTF-8 bytes using the specified seed.
     *
     * @param seed the initial hash value (seed)
     * @param value the string to hash
     * @return the 64-bit hash value
     */
    public static long hash(long seed, @Nullable String value)
    {
        if (value == null) {
            return hashNull(seed);
        }
        return hash(seed, value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Computes the FNV-1a hash of a Long value using the default offset basis.
     *
     * @param value the long to hash
     * @return the 64-bit hash value
     */
    public static long hash(long value)
    {
        return hash(FNV_OFFSET_BASIS_64, value);
    }

    /**
     * Computes the FNV-1a hash of a Long value using the specified seed.
     *
     * @param seed the initial hash value (seed)
     * @param value the long to hash
     * @return the 64-bit hash value
     */
    public static long hash(long seed, long value)
    {
        long v = value;
        long hash = seed;
        for (int i = 0; i < 8; i++) {
            hash ^= (v & 0xff);
            hash *= FNV_PRIME_64;
            v >>= 8;
        }
        return hash;
    }

    /**
     * Computes the FNV-1a hash of a Double value using the default offset basis.
     *
     * @param value the double to hash
     * @return the 64-bit hash value
     */
    public static long hash(double value)
    {
        return hash(FNV_OFFSET_BASIS_64, value);
    }

    /**
     * Computes the FNV-1a hash of a Double value using the specified seed.
     *
     * @param seed the initial hash value (seed)
     * @param value the double to hash
     * @return the 64-bit hash value
     */
    public static long hash(long seed, double value)
    {
        return hash(seed, Double.doubleToRawLongBits(value));
    }

    /**
     * Computes the FNV-1a hash of a Boolean value using the default offset basis.
     *
     * @param value the boolean to hash
     * @return the 64-bit hash value
     */
    public static long hash(boolean value)
    {
        return hash(FNV_OFFSET_BASIS_64, value);
    }

    /**
     * Computes the FNV-1a hash of a Boolean value using the specified seed.
     *
     * @param seed the initial hash value (seed)
     * @param value the boolean to hash
     * @return the 64-bit hash value
     */
    public static long hash(long seed, boolean value)
    {
        long hash = seed;
        hash ^= (value ? 1 : 0);
        hash *= FNV_PRIME_64;
        return hash;
    }

    /**
     * Computes the FNV-1a hash of a BigInteger by hashing its string representation using the default offset basis.
     *
     * @param value the BigInteger to hash
     * @return the 64-bit hash value
     */
    public static long hash(@Nullable BigInteger value)
    {
        return hash(FNV_OFFSET_BASIS_64, value);
    }

    /**
     * Computes the FNV-1a hash of a BigInteger by hashing its string representation using the specified seed.
     *
     * @param seed the initial hash value (seed)
     * @param value the BigInteger to hash
     * @return the 64-bit hash value
     */
    public static long hash(long seed, @Nullable BigInteger value)
    {
        if (value == null) {
            return hashNull(seed);
        }
        return hash(seed, value.toString());
    }

    /**
     * Computes the FNV-1a hash of a BigDecimal by hashing its string representation using the default offset basis.
     *
     * @param value the BigDecimal to hash
     * @return the 64-bit hash value
     */
    public static long hash(@Nullable BigDecimal value)
    {
        return hash(FNV_OFFSET_BASIS_64, value);
    }

    /**
     * Computes the FNV-1a hash of a BigDecimal by hashing its string representation using the specified seed.
     *
     * @param seed the initial hash value (seed)
     * @param value the BigDecimal to hash
     * @return the 64-bit hash value
     */
    public static long hash(long seed, @Nullable BigDecimal value)
    {
        if (value == null) {
            return hashNull(seed);
        }
        return hash(seed, value.toString());
    }

    /**
     * Computes the FNV-1a hash of a ZonedDateTime by hashing its ISO-8601 string representation using the default offset basis.
     *
     * @param value the ZonedDateTime to hash
     * @return the 64-bit hash value
     */
    public static long hash(@Nullable ZonedDateTime value)
    {
        return hash(FNV_OFFSET_BASIS_64, value);
    }

    /**
     * Computes the FNV-1a hash of a ZonedDateTime by hashing its ISO-8601 string representation using the specified seed.
     *
     * @param seed the initial hash value (seed)
     * @param value the ZonedDateTime to hash
     * @return the 64-bit hash value
     */
    public static long hash(long seed, @Nullable ZonedDateTime value)
    {
        if (value == null) {
            return hashNull(seed);
        }
        return hash(seed, value.toString());
    }

    /**
     * Computes the FNV-1a hash of a URI by hashing its string representation using the default offset basis.
     *
     * @param value the URI to hash
     * @return the 64-bit hash value
     */
    public static long hash(@Nullable URI value)
    {
        return hash(FNV_OFFSET_BASIS_64, value);
    }

    /**
     * Computes the FNV-1a hash of a URI by hashing its string representation using the specified seed.
     *
     * @param seed the initial hash value (seed)
     * @param value the URI to hash
     * @return the 64-bit hash value
     */
    public static long hash(long seed, @Nullable URI value)
    {
        if (value == null) {
            return hashNull(seed);
        }
        return hash(seed, value.toString());
    }

    /**
     * Computes the FNV-1a hash of a UUID by hashing its most and least significant bits using the default offset basis.
     *
     * @param value the UUID to hash
     * @return the 64-bit hash value
     */
    public static long hash(@Nullable UUID value)
    {
        return hash(FNV_OFFSET_BASIS_64, value);
    }

    /**
     * Computes the FNV-1a hash of a UUID by hashing its most and least significant bits using the specified seed.
     *
     * @param seed the initial hash value (seed)
     * @param value the UUID to hash
     * @return the 64-bit hash value
     */
    public static long hash(long seed, @Nullable UUID value)
    {
        if (value == null) {
            return hashNull(seed);
        }

        long hash = seed;

        // Hash most significant bits
        long msb = value.getMostSignificantBits();
        for (int i = 0; i < 8; i++) {
            hash ^= (msb & 0xff);
            hash *= FNV_PRIME_64;
            msb >>= 8;
        }

        // Hash least significant bits
        long lsb = value.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            hash ^= (lsb & 0xff);
            hash *= FNV_PRIME_64;
            lsb >>= 8;
        }

        return hash;
    }

    /**
     * Returns a hash value for null inputs using the default offset basis.
     * Uses a distinct marker byte (0xFF) to ensure null hashes differently from empty/zero values.
     *
     * @return the hash value representing null
     */
    public static long hashNull()
    {
        return hashNull(FNV_OFFSET_BASIS_64);
    }

    /**
     * Returns a hash value for null inputs using the specified seed.
     * Uses a distinct marker byte (0xFF) to ensure null hashes differently from empty/zero values.
     *
     * @param seed the initial hash value (seed)
     * @return the hash value representing null
     */
    public static long hashNull(long seed)
    {
        long hash = seed;
        hash ^= 0xFF;
        hash *= FNV_PRIME_64;
        return hash;
    }

    // ===== Instance Update Methods =====

    /**
     * Updates the rolling hash with a byte array.
     *
     * @param bytes the byte array to hash
     * @return this CheapHasher instance for method chaining
     */
    public @NotNull CheapHasher update(byte[] bytes)
    {
        hash = hash(hash, bytes);
        return this;
    }

    /**
     * Updates the rolling hash with a String converted to UTF-8 bytes.
     *
     * @param value the string to hash
     * @return this CheapHasher instance for method chaining
     */
    public @NotNull CheapHasher update(@Nullable String value)
    {
        hash = hash(hash, value);
        return this;
    }

    /**
     * Updates the rolling hash with a Long value.
     *
     * @param value the long to hash
     * @return this CheapHasher instance for method chaining
     */
    public @NotNull CheapHasher update(long value)
    {
        hash = hash(hash, value);
        return this;
    }

    /**
     * Updates the rolling hash with a Double value.
     *
     * @param value the double to hash
     * @return this CheapHasher instance for method chaining
     */
    public @NotNull CheapHasher update(double value)
    {
        hash = hash(hash, value);
        return this;
    }

    /**
     * Updates the rolling hash with a Boolean value.
     *
     * @param value the boolean to hash
     * @return this CheapHasher instance for method chaining
     */
    public @NotNull CheapHasher update(boolean value)
    {
        hash = hash(hash, value);
        return this;
    }

    /**
     * Updates the rolling hash with a BigInteger by hashing its string representation.
     *
     * @param value the BigInteger to hash
     * @return this CheapHasher instance for method chaining
     */
    public @NotNull CheapHasher update(@Nullable BigInteger value)
    {
        hash = hash(hash, value);
        return this;
    }

    /**
     * Updates the rolling hash with a BigDecimal by hashing its string representation.
     *
     * @param value the BigDecimal to hash
     * @return this CheapHasher instance for method chaining
     */
    public @NotNull CheapHasher update(@Nullable BigDecimal value)
    {
        hash = hash(hash, value);
        return this;
    }

    /**
     * Updates the rolling hash with a ZonedDateTime by hashing its ISO-8601 string representation.
     *
     * @param value the ZonedDateTime to hash
     * @return this CheapHasher instance for method chaining
     */
    public @NotNull CheapHasher update(@Nullable ZonedDateTime value)
    {
        hash = hash(hash, value);
        return this;
    }

    /**
     * Updates the rolling hash with a URI by hashing its string representation.
     *
     * @param value the URI to hash
     * @return this CheapHasher instance for method chaining
     */
    public @NotNull CheapHasher update(@Nullable URI value)
    {
        hash = hash(hash, value);
        return this;
    }

    /**
     * Updates the rolling hash with a UUID by hashing its most and least significant bits.
     *
     * @param value the UUID to hash
     * @return this CheapHasher instance for method chaining
     */
    public @NotNull CheapHasher update(@Nullable UUID value)
    {
        hash = hash(hash, value);
        return this;
    }
}
