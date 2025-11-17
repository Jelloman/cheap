package net.netbeing.cheap.json.dto;

import java.util.UUID;

/**
 * Response for entity ID add/remove operations on EntityList or EntitySet hierarchies.
 *
 * @param catalogId ID of the catalog
 * @param hierarchyName Name of the hierarchy
 * @param operation Operation performed ("add" or "remove")
 * @param count Number of entity IDs added or removed
 * @param message Success message
 */
public record EntityIdsOperationResponse(
    UUID catalogId,
    String hierarchyName,
    String operation,
    int count,
    String message
) {
}
