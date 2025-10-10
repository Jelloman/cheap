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

package net.netbeing.cheap.model;

/**
 * A Species is the fundamental type of a catalog in the Cheap data model.
 * The species determines the data source relationship and caching behavior
 * of the catalog within the overall system architecture.
 * 
 * <p>All catalogs in Cheap are caches or working copies, but they differ in
 * their relationship to upstream data sources and their write-through behavior.</p>
 */
public enum CatalogSpecies
{
    /**
     * A Source catalog represents a read-only cache of an external data source.
     */
    SOURCE,

    /**
     * A Sink catalog represents a read-write working copy of an external data source.
     */
    SINK,

    /**
     * A Mirror catalog provides a cached read-only view of another catalog.
     */
    MIRROR,

    /**
     * A Cache catalog provides a write-though view of another catalog.
     * (Writes may be buffered.)
     */
    CACHE,

    /**
     * A Clone catalog is a working copy of another catalog. Reads and writes to and from
     * the upstream catalog are manually invoked.
     */
    CLONE,

    /**
     * A Fork catalog is a transient copy of another catalog, severed from the original.
     * Forks should usually be converted into a Sink, i.e., a permanent copy, AKA "Save As".
     */
    FORK
}
