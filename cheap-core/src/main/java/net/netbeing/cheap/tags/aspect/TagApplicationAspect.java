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
import net.netbeing.cheap.tags.model.TagApplication;
import net.netbeing.cheap.tags.model.TagSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * An {@link net.netbeing.cheap.model.Aspect} wrapper for {@link TagApplication} POJOs.
 *
 * <p>This class provides a CHEAP-native representation of tag applications, wrapping
 * the TagApplication POJO with the ImmutablePojoAspect framework. It uses reflection-based
 * property access for efficient field retrieval and provides convenience methods for
 * commonly accessed properties.</p>
 *
 * <p>TagApplicationAspect instances are immutable and provide read-only access to the
 * underlying TagApplication POJO through the CHEAP property model.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * TagApplication application = new TagApplication(
 *     tagDefinitionId,
 *     targetElementId,
 *     ElementType.PROPERTY,
 *     Map.of("format", "INV-{year}-{seq}"),
 *     TagSource.EXPLICIT,
 *     ZonedDateTime.now(),
 *     "john.doe@example.com"
 * );
 *
 * Entity appEntity = factory.createEntity(catalog, UUID.randomUUID());
 * TagApplicationAspect aspect = new TagApplicationAspect(appEntity, application);
 *
 * // Access properties through convenience methods
 * UUID tagId = aspect.getTagDefinitionId();
 * UUID targetId = aspect.getTargetElementId();
 * ElementType type = aspect.getTargetType();
 * }</pre>
 *
 * @see TagApplication
 * @see ImmutablePojoAspect
 * @see ImmutablePojoAspectDef
 */
public class TagApplicationAspect extends ImmutablePojoAspect<TagApplication>
{
    /**
     * The shared aspect definition for all TagApplicationAspect instances.
     * This is cached to avoid repeated reflection introspection.
     */
    private static final ImmutablePojoAspectDef ASPECT_DEF = new ImmutablePojoAspectDef(TagApplication.class);

    /**
     * Returns the shared aspect definition for TagApplication POJOs.
     *
     * <p>This aspect definition is created once using Java Bean introspection
     * and cached for reuse across all TagApplicationAspect instances.</p>
     *
     * @return the immutable aspect definition for TagApplication
     */
    @NotNull
    public static ImmutablePojoAspectDef aspectDef()
    {
        return ASPECT_DEF;
    }

    /**
     * Constructs a new TagApplicationAspect wrapping the specified TagApplication POJO.
     *
     * @param entity the entity that this aspect is associated with
     * @param application the TagApplication POJO to wrap
     * @throws NullPointerException if entity or application is null
     */
    public TagApplicationAspect(@NotNull Entity entity, @NotNull TagApplication application)
    {
        super(entity, ASPECT_DEF, application);
    }

    /**
     * Returns the UUID of the tag definition entity being applied.
     *
     * @return the tag definition entity UUID
     */
    @NotNull
    public UUID getTagDefinitionId()
    {
        return object().getTagDefinitionId();
    }

    /**
     * Returns the UUID of the element being tagged.
     *
     * @return the target element UUID
     */
    @NotNull
    public UUID getTargetElementId()
    {
        return object().getTargetElementId();
    }

    /**
     * Returns the type of the element being tagged.
     *
     * @return the target element type
     */
    @NotNull
    public ElementType getTargetType()
    {
        return object().getTargetType();
    }

    /**
     * Returns the optional tag-specific metadata.
     *
     * @return immutable map of metadata (never null, but may be empty)
     */
    @NotNull
    public Map<String, Object> getMetadata()
    {
        return object().getMetadata();
    }

    /**
     * Returns how the tag was applied.
     *
     * @return the tag source
     */
    @NotNull
    public TagSource getSource()
    {
        return object().getSource();
    }

    /**
     * Returns the timestamp when the tag was applied.
     *
     * @return the application timestamp
     */
    @NotNull
    public ZonedDateTime getAppliedAt()
    {
        return object().getAppliedAt();
    }

    /**
     * Returns the identifier of the user or system that applied the tag.
     *
     * @return the applier identifier (may be null)
     */
    @Nullable
    public String getAppliedBy()
    {
        return object().getAppliedBy();
    }
}
