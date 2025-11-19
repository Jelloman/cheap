package net.netbeing.cheap.json.dto;

import java.util.UUID;

/**
 * Response for directory entry add/remove operations on EntityDirectory hierarchies.
 *
 * @param catalogId ID of the catalog
 * @param hierarchyName Name of the hierarchy
 * @param operation Operation performed ("add" or "remove")
 * @param count Number of entries added or removed
 * @param message Success message
 */
public record DirectoryOperationResponse(
    UUID catalogId,
    String hierarchyName,
    String operation,
    int count,
    String message
) {
}
