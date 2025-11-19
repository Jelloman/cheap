package net.netbeing.cheap.json.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request to remove entries from an EntityDirectory hierarchy.
 * Must specify EITHER names OR entityIds, not both.
 *
 * @param names List of names to remove (null if removing by entity IDs)
 * @param entityIds List of entity IDs to remove (null if removing by names)
 */
public record RemoveDirectoryEntriesRequest(
    List<String> names,
    List<UUID> entityIds
) {
}
