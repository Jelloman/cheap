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

package net.netbeing.cheap.tags.aspect;

import net.netbeing.cheap.impl.reflect.ImmutablePojoAspect;
import net.netbeing.cheap.impl.reflect.ImmutablePojoAspectDef;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.tags.model.ElementType;
import net.netbeing.cheap.tags.model.TagDefinition;
import net.netbeing.cheap.tags.model.TagScope;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * An {@link net.netbeing.cheap.model.Aspect} wrapper for {@link TagDefinition} POJOs.
 *
 * <p>This class provides a CHEAP-native representation of tag definitions, wrapping
 * the TagDefinition POJO with the ImmutablePojoAspect framework. It uses reflection-based
 * property access for efficient field retrieval and provides convenience methods for
 * commonly accessed properties.</p>
 *
 * <p>TagDefinitionAspect instances are immutable and provide read-only access to the
 * underlying TagDefinition POJO through the CHEAP property model.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * TagDefinition definition = new TagDefinition(
 *     "cheap.core",
 *     "primary-key",
 *     "Primary identifier for entity",
 *     List.of(ElementType.PROPERTY),
 *     TagScope.STANDARD,
 *     null,
 *     null
 * );
 *
 * Entity tagEntity = factory.createEntity(catalog, UUID.randomUUID());
 * TagDefinitionAspect aspect = new TagDefinitionAspect(tagEntity, definition);
 *
 * // Access properties through convenience methods
 * String fullName = aspect.getFullName(); // "cheap.core.primary-key"
 * List<ElementType> appliesTo = aspect.getAppliesTo();
 * }</pre>
 *
 * @see TagDefinition
 * @see ImmutablePojoAspect
 * @see ImmutablePojoAspectDef
 */
public class TagDefinitionAspect extends ImmutablePojoAspect<TagDefinition>
{
    /**
     * The shared aspect definition for all TagDefinitionAspect instances.
     * This is cached to avoid repeated reflection introspection.
     */
    private static final ImmutablePojoAspectDef ASPECT_DEF = new ImmutablePojoAspectDef(TagDefinition.class);

    /**
     * Returns the shared aspect definition for TagDefinition POJOs.
     *
     * <p>This aspect definition is created once using Java Bean introspection
     * and cached for reuse across all TagDefinitionAspect instances.</p>
     *
     * @return the immutable aspect definition for TagDefinition
     */
    @NotNull
    public static ImmutablePojoAspectDef aspectDef()
    {
        return ASPECT_DEF;
    }

    /**
     * Constructs a new TagDefinitionAspect wrapping the specified TagDefinition POJO.
     *
     * @param entity the entity that this aspect is associated with
     * @param definition the TagDefinition POJO to wrap
     * @throws NullPointerException if entity or definition is null
     */
    public TagDefinitionAspect(@NotNull Entity entity, @NotNull TagDefinition definition)
    {
        super(entity, ASPECT_DEF, definition);
    }

    /**
     * Returns the namespace of this tag definition.
     *
     * @return the tag namespace (e.g., "cheap.core", "myapp.domain")
     */
    @NotNull
    public String getNamespace()
    {
        return object().getNamespace();
    }

    /**
     * Returns the name of this tag definition within its namespace.
     *
     * @return the tag name (e.g., "primary-key", "created-timestamp")
     */
    @NotNull
    public String getName()
    {
        return object().getName();
    }

    /**
     * Returns the full qualified name of this tag definition.
     *
     * <p>The full name is constructed as {@code namespace.name}.</p>
     *
     * @return the full qualified tag name (e.g., "cheap.core.primary-key")
     */
    @NotNull
    public String getFullName()
    {
        return object().getFullName();
    }

    /**
     * Returns the description of this tag definition.
     *
     * @return the human-readable tag description
     */
    @NotNull
    public String getDescription()
    {
        return object().getDescription();
    }

    /**
     * Returns the list of element types this tag can be applied to.
     *
     * @return immutable list of applicable element types
     */
    @NotNull
    public List<ElementType> getAppliesTo()
    {
        return object().getAppliesTo();
    }

    /**
     * Returns the scope of this tag definition (STANDARD or CUSTOM).
     *
     * @return the tag scope
     */
    @NotNull
    public TagScope getScope()
    {
        return object().getScope();
    }

    /**
     * Returns the list of alternative names for this tag.
     *
     * @return immutable list of aliases (may be empty)
     */
    @NotNull
    public List<String> getAliases()
    {
        return object().getAliases();
    }

    /**
     * Returns the list of parent tag entity UUIDs for tag inheritance.
     *
     * @return immutable list of parent tag entity UUIDs (may be empty)
     */
    @NotNull
    public List<UUID> getParentTagIds()
    {
        return object().getParentTagIds();
    }

    /**
     * Checks if this tag can be applied to the specified element type.
     *
     * @param elementType the element type to check
     * @return true if this tag can be applied to the element type, false otherwise
     */
    public boolean isApplicableTo(@NotNull ElementType elementType)
    {
        return object().isApplicableTo(elementType);
    }
}
