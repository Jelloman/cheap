package net.netbeing.cheap.json.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request to add entity IDs to an EntityList or EntitySet hierarchy.
 *
 * @param entityIds List of entity IDs to add (must not be null or empty)
 */
public record AddEntityIdsRequest(
    List<UUID> entityIds
) {
}
