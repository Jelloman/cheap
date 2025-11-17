package net.netbeing.cheap.json.dto;

import java.util.UUID;

/**
 * Response for tree node add/remove operations on EntityTree hierarchies.
 *
 * @param catalogId ID of the catalog
 * @param hierarchyName Name of the hierarchy
 * @param operation Operation performed ("add" or "remove")
 * @param nodesAffected Number of nodes affected (for remove, includes cascade deleted nodes)
 * @param message Success message
 */
public record TreeOperationResponse(
    UUID catalogId,
    String hierarchyName,
    String operation,
    int nodesAffected,
    String message
) {
}
