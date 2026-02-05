/*
 * Copyright (c) 2026. David Noha
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

package net.netbeing.cheap.tags.registry;

import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.impl.reflect.ImmutablePojoAspectDef;
import net.netbeing.cheap.model.*;
import net.netbeing.cheap.tags.aspect.TagApplicationAspect;
import net.netbeing.cheap.tags.aspect.TagDefinitionAspect;
import net.netbeing.cheap.tags.model.ElementType;
import net.netbeing.cheap.tags.model.TagApplication;
import net.netbeing.cheap.tags.model.TagDefinition;
import net.netbeing.cheap.tags.model.TagSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link TagRegistry}.
 */
public class TagRegistryImpl implements TagRegistry
{
    private static final String TAG_INDEX_BY_NAME_HIERARCHY = "tag_index_by_name";
    private static final String TAG_INDEX_BY_ELEMENT_PREFIX = "tag_index_element_";
    private static final String TAG_INDEX_BY_TAG_PREFIX = "tag_index_tag_";

    private final Catalog catalog;
    private final CheapFactory factory;

    private final AspectMapHierarchy tagDefinitionsHierarchy;
    private final AspectMapHierarchy tagApplicationsHierarchy;
    private final EntityDirectoryHierarchy tagIndexByName;

    private final ImmutablePojoAspectDef tagDefinitionAspectDef;
    private final ImmutablePojoAspectDef tagApplicationAspectDef;

    public TagRegistryImpl(@NotNull Catalog catalog, @NotNull CheapFactory factory)
    {
        this.catalog = Objects.requireNonNull(catalog, "catalog cannot be null");
        this.factory = Objects.requireNonNull(factory, "factory cannot be null");

        this.tagDefinitionAspectDef = TagDefinitionAspect.aspectDef();
        this.tagApplicationAspectDef = TagApplicationAspect.aspectDef();

        // Create or retrieve aspect map hierarchies
        this.tagDefinitionsHierarchy = getOrCreateAspectMapHierarchy(tagDefinitionAspectDef);
        this.tagApplicationsHierarchy = getOrCreateAspectMapHierarchy(tagApplicationAspectDef);

        this.tagIndexByName = getOrCreateEntityDirectoryHierarchy(TAG_INDEX_BY_NAME_HIERARCHY);
    }

    private AspectMapHierarchy getOrCreateAspectMapHierarchy(AspectDef aspectDef)
    {
        AspectMapHierarchy existing = catalog.aspects(aspectDef);
        if (existing != null) {
            return existing;
        }
        return catalog.createAspectMap(aspectDef, 0L);
    }

    private EntityDirectoryHierarchy getOrCreateEntityDirectoryHierarchy(String name)
    {
        Hierarchy existing = catalog.hierarchy(name);
        if (existing instanceof EntityDirectoryHierarchy) {
            return (EntityDirectoryHierarchy) existing;
        }
        return catalog.createEntityDirectory(name, 0L);
    }

    private EntitySetHierarchy getOrCreateEntitySetHierarchy(String name)
    {
        Hierarchy existing = catalog.hierarchy(name);
        if (existing instanceof EntitySetHierarchy) {
            return (EntitySetHierarchy) existing;
        }
        return catalog.createEntitySet(name, 0L);
    }

    private String getElementIndexKey(UUID elementId, ElementType type)
    {
        return TAG_INDEX_BY_ELEMENT_PREFIX + type.toLowerString() + "_" + elementId.toString();
    }

    private String getTagIndexKey(UUID tagId, ElementType type)
    {
        return TAG_INDEX_BY_TAG_PREFIX + type.toLowerString() + "_" + tagId.toString();
    }

    @Override
    @NotNull
    public Catalog catalog()
    {
        return catalog;
    }

    // ==================== Tag Definition Management ====================

    @Override
    @NotNull
    public Entity defineTag(@NotNull TagDefinition definition)
    {
        Objects.requireNonNull(definition, "definition cannot be null");

        validateTagDefinition(definition);

        String fullName = definition.getFullName();
        Entity existing = tagIndexByName.get(fullName);
        if (existing != null) {
            throw new IllegalArgumentException("Tag already exists: " + fullName);
        }

        Entity tagEntity = factory.createEntity();
        TagDefinitionAspect aspect = new TagDefinitionAspect(tagEntity, definition);
        tagDefinitionsHierarchy.put(tagEntity, aspect);
        tagIndexByName.put(fullName, tagEntity);

        return tagEntity;
    }

    @Override
    @Nullable
    public TagDefinition getTagDefinition(@NotNull UUID tagEntityId)
    {
        Objects.requireNonNull(tagEntityId, "tagEntityId cannot be null");

        for (Map.Entry<Entity, Aspect> entry : tagDefinitionsHierarchy.entrySet()) {
            if (entry.getKey().globalId().equals(tagEntityId)) {
                if (entry.getValue() instanceof TagDefinitionAspect) {
                    return ((TagDefinitionAspect) entry.getValue()).object();
                }
            }
        }
        return null;
    }

    @Override
    @Nullable
    public TagDefinition getTagDefinitionByName(@NotNull String namespace, @NotNull String name)
    {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(name, "name cannot be null");

        String fullName = namespace + "." + name;
        Entity entity = tagIndexByName.get(fullName);
        if (entity == null) {
            return null;
        }
        return getTagDefinition(entity.globalId());
    }

    @Override
    @NotNull
    public Collection<TagDefinition> getAllTagDefinitions()
    {
        List<TagDefinition> definitions = new ArrayList<>();
        for (Aspect aspect : tagDefinitionsHierarchy.values()) {
            if (aspect instanceof TagDefinitionAspect) {
                definitions.add(((TagDefinitionAspect) aspect).object());
            }
        }
        return definitions;
    }

    @Override
    @NotNull
    public Collection<TagDefinition> getTagDefinitionsByNamespace(@NotNull String namespace)
    {
        Objects.requireNonNull(namespace, "namespace cannot be null");

        return getAllTagDefinitions().stream()
            .filter(def -> def.getNamespace().equals(namespace) ||
                          def.getNamespace().startsWith(namespace + "."))
            .collect(Collectors.toList());
    }

    private void validateTagDefinition(TagDefinition definition)
    {
        if (!isValidNamespace(definition.getNamespace())) {
            throw new IllegalArgumentException("Invalid namespace format: " + definition.getNamespace());
        }

        if (!isValidName(definition.getName())) {
            throw new IllegalArgumentException("Invalid name format: " + definition.getName());
        }

        for (UUID parentTagId : definition.getParentTagIds()) {
            if (getTagDefinition(parentTagId) == null) {
                throw new IllegalArgumentException("Parent tag not found: " + parentTagId);
            }
        }
    }

    private boolean isValidNamespace(String namespace)
    {
        if (namespace == null || namespace.isEmpty()) {
            return false;
        }
        return namespace.matches("^[a-z0-9]+([.-][a-z0-9]+)+$");
    }

    private boolean isValidName(String name)
    {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return name.matches("^[a-z0-9]+(-[a-z0-9]+)*$");
    }

    // ==================== Tag Application ====================

    @Override
    @NotNull
    public Entity applyTag(
        @NotNull UUID targetElementId,
        @NotNull ElementType targetType,
        @NotNull UUID tagDefinitionId,
        @Nullable Map<String, Object> metadata,
        @NotNull TagSource source)
    {
        Objects.requireNonNull(targetElementId, "targetElementId cannot be null");
        Objects.requireNonNull(targetType, "targetType cannot be null");
        Objects.requireNonNull(tagDefinitionId, "tagDefinitionId cannot be null");
        Objects.requireNonNull(source, "source cannot be null");

        TagDefinition tagDef = getTagDefinition(tagDefinitionId);
        if (tagDef == null) {
            throw new IllegalArgumentException("Tag definition not found: " + tagDefinitionId);
        }

        if (!tagDef.isApplicableTo(targetType)) {
            throw new IllegalArgumentException(
                "Tag " + tagDef.getFullName() + " cannot be applied to " + targetType
            );
        }

        // Check for existing application (idempotency)
        String elementIndexKey = getElementIndexKey(targetElementId, targetType);
        EntitySetHierarchy elementIndex = getOrCreateEntitySetHierarchy(elementIndexKey);

        for (Entity appEntity : elementIndex) {
            Aspect aspect = tagApplicationsHierarchy.get(appEntity);
            if (aspect instanceof TagApplicationAspect) {
                TagApplication app = ((TagApplicationAspect) aspect).object();
                if (app.getTagDefinitionId().equals(tagDefinitionId) &&
                    app.getTargetElementId().equals(targetElementId)) {
                    return appEntity;
                }
            }
        }

        // Create new application
        Entity appEntity = factory.createEntity();
        TagApplication application = new TagApplication(
            tagDefinitionId,
            targetElementId,
            targetType,
            metadata,
            source,
            ZonedDateTime.now(),
            null
        );

        TagApplicationAspect aspect = new TagApplicationAspect(appEntity, application);
        tagApplicationsHierarchy.put(appEntity, aspect);

        // Update indices
        elementIndex.add(appEntity);

        String tagIndexKey = getTagIndexKey(tagDefinitionId, targetType);
        EntitySetHierarchy tagIndex = getOrCreateEntitySetHierarchy(tagIndexKey);
        tagIndex.add(appEntity);

        return appEntity;
    }

    @Override
    public void removeTag(@NotNull UUID tagApplicationId)
    {
        Objects.requireNonNull(tagApplicationId, "tagApplicationId cannot be null");

        Entity appEntity = null;
        TagApplication app = null;

        for (Map.Entry<Entity, Aspect> entry : tagApplicationsHierarchy.entrySet()) {
            if (entry.getKey().globalId().equals(tagApplicationId)) {
                appEntity = entry.getKey();
                if (entry.getValue() instanceof TagApplicationAspect) {
                    app = ((TagApplicationAspect) entry.getValue()).object();
                }
                break;
            }
        }

        if (appEntity != null && app != null) {
            tagApplicationsHierarchy.remove(appEntity);

            // Update indices
            String elementIndexKey = getElementIndexKey(app.getTargetElementId(), app.getTargetType());
            EntitySetHierarchy elementIndex = getOrCreateEntitySetHierarchy(elementIndexKey);
            elementIndex.remove(appEntity);

            String tagIndexKey = getTagIndexKey(app.getTagDefinitionId(), app.getTargetType());
            EntitySetHierarchy tagIndex = getOrCreateEntitySetHierarchy(tagIndexKey);
            tagIndex.remove(appEntity);
        }
    }

    // ==================== Tag Queries ====================

    @Override
    @NotNull
    public Collection<TagApplication> getTagsForElement(@NotNull UUID elementId, @NotNull ElementType type)
    {
        Objects.requireNonNull(elementId, "elementId cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        List<TagApplication> applications = new ArrayList<>();
        String indexKey = getElementIndexKey(elementId, type);
        EntitySetHierarchy index = getOrCreateEntitySetHierarchy(indexKey);

        for (Entity appEntity : index) {
            Aspect aspect = tagApplicationsHierarchy.get(appEntity);
            if (aspect instanceof TagApplicationAspect) {
                applications.add(((TagApplicationAspect) aspect).object());
            }
        }

        return applications;
    }

    @Override
    @NotNull
    public Collection<UUID> getElementsByTag(@NotNull UUID tagDefinitionId, @NotNull ElementType type)
    {
        Objects.requireNonNull(tagDefinitionId, "tagDefinitionId cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        List<UUID> elementIds = new ArrayList<>();
        String indexKey = getTagIndexKey(tagDefinitionId, type);
        EntitySetHierarchy index = getOrCreateEntitySetHierarchy(indexKey);

        for (Entity appEntity : index) {
            Aspect aspect = tagApplicationsHierarchy.get(appEntity);
            if (aspect instanceof TagApplicationAspect) {
                TagApplication app = ((TagApplicationAspect) aspect).object();
                elementIds.add(app.getTargetElementId());
            }
        }

        return elementIds;
    }

    @Override
    @NotNull
    public Collection<UUID> getElementsByTagName(
        @NotNull String namespace,
        @NotNull String name,
        @NotNull ElementType type)
    {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        TagDefinition tagDef = getTagDefinitionByName(namespace, name);
        if (tagDef == null) {
            return Collections.emptyList();
        }

        Entity tagEntity = tagIndexByName.get(namespace + "." + name);
        if (tagEntity == null) {
            return Collections.emptyList();
        }

        return getElementsByTag(tagEntity.globalId(), type);
    }

    @Override
    public boolean hasTag(
        @NotNull UUID elementId,
        @NotNull ElementType type,
        @NotNull UUID tagDefinitionId)
    {
        Objects.requireNonNull(elementId, "elementId cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(tagDefinitionId, "tagDefinitionId cannot be null");

        Collection<TagApplication> tags = getTagsForElement(elementId, type);
        return tags.stream().anyMatch(app -> app.getTagDefinitionId().equals(tagDefinitionId));
    }

    // ==================== Tag Validation ====================

    @Override
    public boolean isTagApplicable(@NotNull UUID tagDefinitionId, @NotNull ElementType targetType)
    {
        Objects.requireNonNull(tagDefinitionId, "tagDefinitionId cannot be null");
        Objects.requireNonNull(targetType, "targetType cannot be null");

        TagDefinition tagDef = getTagDefinition(tagDefinitionId);
        if (tagDef == null) {
            return false;
        }

        return tagDef.isApplicableTo(targetType);
    }

    @Override
    @NotNull
    public Collection<String> validateTagApplication(
        @NotNull UUID tagDefinitionId,
        @NotNull UUID targetElementId,
        @NotNull ElementType targetType)
    {
        Objects.requireNonNull(tagDefinitionId, "tagDefinitionId cannot be null");
        Objects.requireNonNull(targetElementId, "targetElementId cannot be null");
        Objects.requireNonNull(targetType, "targetType cannot be null");

        List<String> errors = new ArrayList<>();

        TagDefinition tagDef = getTagDefinition(tagDefinitionId);
        if (tagDef == null) {
            errors.add("Tag definition not found: " + tagDefinitionId);
            return errors;
        }

        if (!tagDef.isApplicableTo(targetType)) {
            errors.add("Tag " + tagDef.getFullName() + " cannot be applied to " + targetType);
        }

        return errors;
    }

    // ==================== Tag Inheritance ====================

    @Override
    @NotNull
    public Collection<UUID> getParentTags(@NotNull UUID tagDefinitionId)
    {
        Objects.requireNonNull(tagDefinitionId, "tagDefinitionId cannot be null");

        TagDefinition tagDef = getTagDefinition(tagDefinitionId);
        if (tagDef == null) {
            return Collections.emptyList();
        }

        return tagDef.getParentTagIds();
    }

    @Override
    @NotNull
    public Collection<UUID> getAllAncestorTags(@NotNull UUID tagDefinitionId)
    {
        Objects.requireNonNull(tagDefinitionId, "tagDefinitionId cannot be null");

        Set<UUID> ancestors = new HashSet<>();
        Set<UUID> visited = new HashSet<>();
        collectAncestors(tagDefinitionId, ancestors, visited);
        return ancestors;
    }

    private void collectAncestors(UUID tagId, Set<UUID> ancestors, Set<UUID> visited)
    {
        if (visited.contains(tagId)) {
            return;
        }
        visited.add(tagId);

        Collection<UUID> parents = getParentTags(tagId);
        for (UUID parentId : parents) {
            ancestors.add(parentId);
            collectAncestors(parentId, ancestors, visited);
        }
    }

    @Override
    @NotNull
    public Collection<UUID> getChildTags(@NotNull UUID tagDefinitionId)
    {
        Objects.requireNonNull(tagDefinitionId, "tagDefinitionId cannot be null");

        List<UUID> children = new ArrayList<>();
        for (TagDefinition def : getAllTagDefinitions()) {
            if (def.getParentTagIds().contains(tagDefinitionId)) {
                Entity entity = tagIndexByName.get(def.getFullName());
                if (entity != null) {
                    children.add(entity.globalId());
                }
            }
        }
        return children;
    }

    @Override
    public boolean inheritsFrom(@NotNull UUID childTagId, @NotNull UUID parentTagId)
    {
        Objects.requireNonNull(childTagId, "childTagId cannot be null");
        Objects.requireNonNull(parentTagId, "parentTagId cannot be null");

        Collection<UUID> ancestors = getAllAncestorTags(childTagId);
        return ancestors.contains(parentTagId);
    }

    // ==================== Standard Tags ====================

    @Override
    public void initializeStandardTags()
    {
        // Will be implemented in Phase 5
    }

    @Override
    @NotNull
    public Collection<TagDefinition> getStandardTags()
    {
        return getTagDefinitionsByNamespace("cheap.core");
    }
}
