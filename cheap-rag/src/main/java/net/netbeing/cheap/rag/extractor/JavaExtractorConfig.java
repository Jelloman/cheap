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

package net.netbeing.cheap.rag.extractor;

import lombok.Builder;
import lombok.Value;

/**
 * Configuration for Java code extraction.
 */
@Value
@Builder
public class JavaExtractorConfig
{
    /**
     * Extract classes (default: true)
     */
    @Builder.Default
    boolean extractClasses = true;

    /**
     * Extract interfaces (default: true)
     */
    @Builder.Default
    boolean extractInterfaces = true;

    /**
     * Extract enums (default: true)
     */
    @Builder.Default
    boolean extractEnums = true;

    /**
     * Extract methods (default: true)
     */
    @Builder.Default
    boolean extractMethods = true;

    /**
     * Extract fields (default: true)
     */
    @Builder.Default
    boolean extractFields = true;

    /**
     * Only extract public API (skip private members) (default: false)
     */
    @Builder.Default
    boolean publicOnly = false;
}
