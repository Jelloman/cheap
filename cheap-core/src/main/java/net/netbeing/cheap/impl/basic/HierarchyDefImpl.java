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

package net.netbeing.cheap.impl.basic;

import net.netbeing.cheap.model.HierarchyDef;
import net.netbeing.cheap.model.HierarchyType;

import java.util.Objects;

/**
 * Record-based implementation of HierarchyDef that defines the structure and
 * properties of a hierarchy in the Cheap system.
 *
 * @param name the name of the hierarchy
 * @param type the type of hierarchy
 *
 * @see HierarchyDef
 * @see HierarchyType
 */
public record HierarchyDefImpl(
        String name,
        HierarchyType type) implements HierarchyDef
{
    /**
     * Compact constructor that validates the hierarchy definition parameters.
     * 
     * @throws NullPointerException if name or type is null
     */
    public HierarchyDefImpl
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
    }
}
