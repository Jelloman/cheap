package net.netbeing.cheap.json.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Request to add child nodes to an EntityTree hierarchy under a specific parent.
 *
 * @param parentPath Path to the parent node (e.g., "/root/folder1")
 * @param nodes Map of child name -> entity ID pairs to add (must not be null or empty)
 */
public record AddTreeNodesRequest(
    String parentPath,
    Map<String, UUID> nodes
) {
}
