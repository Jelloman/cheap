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
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.model.Entity;
import net.netbeing.cheap.model.EntityTreeHierarchy;
import net.netbeing.cheap.model.EntityTreeHierarchy.Node;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Jackson deserializer for {@link Node} objects in the Cheap data model.
 * <p>
 * This deserializer recursively reconstructs tree node structures from JSON format,
 * handling both leaf nodes and internal nodes with children. Each node can optionally
 * contain an entity reference identified by UUID.
 * </p>
 * <p>
 * The deserializer processes JSON with the following structure:
 * </p>
 * <ul>
 *   <li>entityId: Optional UUID of the entity associated with this node</li>
 *   <li>isLeaf: Optional flag indicating if this is a leaf node (defaults to false)</li>
 *   <li>children: Optional object mapping string keys to child nodes (for non-leaf nodes)</li>
 * </ul>
 * <p>
 * Entity references are resolved through the CheapFactory's entity registry to ensure
 * consistent Entity object instances across the deserialization process. The deserializer
 * uses the factory to create the appropriate node type (leaf or internal) based on the
 * isLeaf flag.
 * </p>
 * <p>
 * This class is package-private and used internally by {@link HierarchyDeserializer}
 * when deserializing EntityTreeHierarchy objects.
 * </p>
 *
 * @see Node
 * @see EntityTreeHierarchy
 * @see HierarchyDeserializer
 * @see CheapFactory
 */
class TreeNodeDeserializer extends JsonDeserializer<Node>
{
    private final CheapFactory factory;

    public TreeNodeDeserializer(@NotNull CheapFactory factory)
    {
        this.factory = factory;
    }

    @Override
    public Node deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonMappingException(p, "Expected START_OBJECT token");
        }

        final Node parent = (Node) context.getAttribute("CheapParentTreeNode");
        UUID entityId = null;
        Map<String, Node> children = new LinkedHashMap<>();
        boolean isLeaf = false;
        Node thisNode = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();

            switch (fieldName) {
                case "entityId" -> {
                    if (p.currentToken() != JsonToken.VALUE_NULL) {
                        entityId = UUID.fromString(p.getValueAsString());
                    }
                }
                case "isLeaf" -> {
                    if (p.currentToken() == JsonToken.VALUE_TRUE) {
                        isLeaf = true;
                    }
                }
                case "children" -> {
                    if (p.currentToken() == JsonToken.START_OBJECT) {
                        thisNode = factory.createTreeNode(null, parent);
                        context.setAttribute("CheapParentTreeNode", thisNode);
                        while (p.nextToken() != JsonToken.END_OBJECT) {
                            String key = p.currentName();
                            p.nextToken();
                            children.put(key, p.readValueAs(Node.class));
                        }
                        context.setAttribute("CheapParentTreeNode", parent);
                    }
                }
                default -> p.skipChildren();
            }
        }

        // Get or create entity from factory if entityId is provided
        Entity entity = null;
        if (entityId != null) {
            entity = factory.getOrRegisterNewEntity(entityId);
        }
        if (isLeaf) {
            return factory.createTreeLeafNode(entity, parent);
        }
        if (thisNode != null) {
            thisNode.setValue(entity);
        } else {
            thisNode = factory.createTreeNode(entity, parent);
        }

        // Add children to non-leaf nodes
        thisNode.putAll(children);

        return thisNode;
    }
}