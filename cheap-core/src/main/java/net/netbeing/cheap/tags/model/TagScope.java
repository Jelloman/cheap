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

package net.netbeing.cheap.tags.model;

/**
 * Defines the scope of a tag definition, distinguishing between standard
 * built-in tags and custom user-defined tags.
 *
 * <p>Tag scopes provide semantic categorization:</p>
 * <ul>
 *   <li><b>STANDARD</b> - Built-in tags provided by the CHEAP framework in the
 *       {@code cheap.core} namespace. These tags have well-defined semantics
 *       and are part of the framework's standard vocabulary.</li>
 *   <li><b>CUSTOM</b> - User-defined tags in application-specific namespaces.
 *       These tags allow applications to extend the tagging system with
 *       domain-specific metadata.</li>
 * </ul>
 *
 * <p>Standard tags are initialized by the TagRegistry and provide common
 * metadata patterns such as primary keys, timestamps, and data semantics.
 * Custom tags allow applications to define their own semantic markers
 * tailored to specific business domains.</p>
 *
 * @see TagDefinition
 */
public enum TagScope
{
    /**
     * Standard built-in tags defined by the CHEAP framework.
     * These tags use the {@code cheap.core} namespace and provide
     * common metadata patterns for data modeling.
     */
    STANDARD,

    /**
     * Custom user-defined tags for application-specific metadata.
     * These tags use application namespaces (e.g., {@code myapp.domain})
     * and allow extending the tagging system with domain-specific semantics.
     */
    CUSTOM
}
