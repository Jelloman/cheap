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

package net.netbeing.cheap.json.jackson.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import net.netbeing.cheap.model.AspectDef;
import net.netbeing.cheap.model.PropertyDef;
import net.netbeing.cheap.model.PropertyType;
import net.netbeing.cheap.util.CheapFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Jackson deserializer for {@link AspectDef} objects in the Cheap data model.
 * <p>
 * This deserializer reconstructs an AspectDef from JSON format, including its name,
 * access control flags, and property definitions. The deserializer intelligently
 * selects the appropriate AspectDef implementation (ImmutableAspectDef, MutableAspectDef,
 * or FullAspectDef) based on the mutability flags in the JSON.
 * </p>
 * <p>
 * The deserializer handles both aspect-level and property-level flags, with property
 * flags inheriting aspect defaults when not explicitly specified. This allows for
 * compact JSON representation while maintaining full configuration flexibility.
 * </p>
 * <p>
 * After deserialization, the AspectDef is automatically registered with the CheapFactory
 * to make it available for subsequent AspectMapHierarchy deserialization. If an AspectDef
 * with the same name already exists, the deserializer verifies full equality to prevent
 * conflicts.
 * </p>
 * <p>
 * This class is package-private and used internally by {@link CheapJacksonDeserializer}
 * and {@link CatalogDeserializer}.
 * </p>
 *
 * @see AspectDef
 * @see PropertyDef
 * @see CheapFactory
 * @see CatalogDeserializer
 */
class AspectDefDeserializer extends JsonDeserializer<AspectDef>
{
    private final CheapFactory factory;

    public AspectDefDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    private static final class Flags
    {
        boolean isReadable = true;
        boolean isWritable = true;
        boolean canAddProperties = false;
        boolean canRemoveProperties = false;
    }

    @Override
    public AspectDef deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token to begin AspectDef");
        }

        String name = null;
        List<PropertyDef> propertyDefs = new ArrayList<>();
        Flags flags = new Flags();

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "name" -> name = p.getValueAsString();
                case "isReadable" -> flags.isReadable = p.getBooleanValue();
                case "isWritable" -> flags.isWritable = p.getBooleanValue();
                case "canAddProperties" -> flags.canAddProperties = p.getBooleanValue();
                case "canRemoveProperties" -> flags.canRemoveProperties = p.getBooleanValue();
                case "propertyDefs" -> {
                    if (p.currentToken() == JsonToken.START_ARRAY) { // NOSONAR - Sonar bug, https://sonarsource.atlassian.net/browse/SONARJAVA-4962
                        while (p.nextToken() != JsonToken.END_ARRAY) {
                            PropertyDef propDef = deserializePropertyDef(p, flags);
                            propertyDefs.add(propDef);
                        }
                    }
                }
                default -> p.skipChildren();
            }
        }

        if (name == null) {
            throw new JsonMappingException(p, "Missing required field in AspectDef: name");
        }

        Map<String, PropertyDef> propertyDefMap = new LinkedHashMap<>();
        for (PropertyDef propertyDef : propertyDefs) {
            propertyDefMap.put(propertyDef.name(), propertyDef);
        }

        // Choose the appropriate AspectDef implementation based on the flags
        AspectDef def;
        if (flags.canAddProperties && flags.canRemoveProperties) {
            // Fully mutable - use MutableAspectDefImpl
            def = factory.createMutableAspectDef(name, propertyDefMap);
        } else if (!flags.canAddProperties && !flags.canRemoveProperties) {
            // Fully immutable - use ImmutableAspectDefImpl
            def = factory.createImmutableAspectDef(name, propertyDefMap);
        } else {
            // Mixed mutability - use FullAspectDefImpl
            def = factory.createFullAspectDef(name, UUID.randomUUID(), propertyDefMap,
                flags.isReadable, flags.isWritable, flags.canAddProperties, flags.canRemoveProperties);
        }
        AspectDef existingDef = factory.getAspectDef(name);
        if (existingDef != null) {
            if (!existingDef.fullyEquals(def)) {
                throw new JsonMappingException(p, "Attempted to deserialize AspectDef " + name + " that conflicts with the AspectDef already registered with that name.");
            }
        } else {
            factory.registerAspectDef(def);
        }

        return def;
    }

    private PropertyDef deserializePropertyDef(JsonParser p, Flags flags) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token to start PropertyDef");
        }

        String name = null;
        PropertyType type = null;
        Object defaultValue = null;
        boolean hasDefaultValue = false;
        boolean isReadable = flags.isReadable;
        boolean isWritable = flags.isWritable;
        boolean isNullable = true;
        boolean isRemovable = flags.canRemoveProperties;
        boolean isMultivalued = false;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "name" -> name = p.getValueAsString();
                case "type" -> type = PropertyType.valueOf(p.getValueAsString());
                case "defaultValue" -> {
                    defaultValue = readValue(p, type);
                    hasDefaultValue = true;
                }
                case "isReadable" -> isReadable = p.getBooleanValue();
                case "isWritable" -> isWritable = p.getBooleanValue();
                case "isNullable" -> isNullable = p.getBooleanValue();
                case "isRemovable" -> isRemovable = p.getBooleanValue();
                case "isMultivalued" -> isMultivalued = p.getBooleanValue();
                default -> p.skipChildren(); // Skip unknown fields
            }
        }

        if (name == null || type == null) {
            throw new JsonMappingException(p, "Missing required fields: name and type");
        }

        return factory.createPropertyDef(name, type, defaultValue, hasDefaultValue, isReadable,
            isWritable, isNullable, isRemovable, isMultivalued);
    }

    private Object readValue(JsonParser p, PropertyType type) throws IOException
    {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        if (type == null) {
            return p.getValueAsString();
        }

        return switch (type) {
            case Integer -> p.getLongValue();
            case Float -> p.getDoubleValue();
            case Boolean -> p.getBooleanValue();
            default -> p.getValueAsString();
        };
    }
}