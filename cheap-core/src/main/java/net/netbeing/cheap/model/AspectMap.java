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

import java.util.Map;

/**
 * A Map of entities to their aspects of a specific type.
 */
public interface AspectMap extends Map<Entity,Aspect>
{
    /**
     * Returns the aspect definition that defines the structure and constraints
     * for all aspects stored in this map.
     * 
     * @return the aspect definition for this map, never null
     */
    AspectDef aspectDef();

    /**
     * Convenience method to add an aspect to this map.
     * This method extracts the entity from the aspect and uses it as the key
     * in the underlying map.
     *
     * @param a the aspect to add
     * @return the previous aspect associated with the same entity, or null if none existed
     */
    default Aspect add(Aspect a)
    {
        if (!a.def().equals(aspectDef())) {
            throw new IllegalArgumentException("Cannot add Aspect of type '"+a.def().name()+"' to AspectMap for aspect '"+ aspectDef().name() + "'.");
        }
        return unsafeAdd(a);
    }

    /**
     * Convenience method to add an aspect to this map WITHOUT checking that it has the proper type.
     * This method extracts the entity from the aspect and uses it as the key in the underlying map.
     *
     * @param a the aspect to add
     * @return the previous aspect associated with the same entity, or null if none existed
     */
    default Aspect unsafeAdd(Aspect a) {
        return put(a.entity(), a);
    }
}
