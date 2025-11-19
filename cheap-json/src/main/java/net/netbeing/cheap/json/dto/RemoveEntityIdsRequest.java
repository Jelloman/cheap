package net.netbeing.cheap.json.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request to remove entity IDs from an EntityList or EntitySet hierarchy.
 *
 * @param entityIds List of entity IDs to remove (must not be null or empty)
 */
public record RemoveEntityIdsRequest(
    List<UUID> entityIds
) {
}
