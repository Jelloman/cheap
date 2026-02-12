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

package net.netbeing.cheap.rag.util;

import net.netbeing.cheap.rag.extractor.model.ArtifactType;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generates stable IDs for metadata artifacts using SHA-256 hashing.
 * Must match the Python implementation exactly.
 * <p>
 * Format: {language}_{type}_{hash16}
 * Hash input: "{language}:{type}:{qualifiedName}"
 */
public class IdGenerator
{
    /**
     * Generates a stable ID for an artifact.
     *
     * @param language The programming language (e.g., "java")
     * @param type The artifact type
     * @param qualifiedName The fully qualified name of the artifact
     * @return The generated ID in format {language}_{type}_{hash16}
     */
    @NotNull
    public static String generateId(
            @NotNull String language,
            @NotNull ArtifactType type,
            @NotNull String qualifiedName)
    {
        // Build hash input matching Python: "{language}:{type}:{qualifiedName}"
        String hashInput = String.format("%s:%s:%s", language, type.getValue(), qualifiedName);

        // Compute SHA-256 hash
        String hash = sha256(hashInput);

        // Take first 16 characters
        String hash16 = hash.substring(0, 16);

        // Format: {language}_{type}_{hash16}
        return String.format("%s_%s_%s", language, type.getValue(), hash16);
    }

    @NotNull
    private static String sha256(@NotNull String input)
    {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @NotNull
    private static String bytesToHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
