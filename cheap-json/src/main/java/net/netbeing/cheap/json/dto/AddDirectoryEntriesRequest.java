package net.netbeing.cheap.json.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Request to add (name, entityId) pairs to an EntityDirectory hierarchy.
 *
 * @param entries Map of name -> entity ID pairs to add (must not be null or empty)
 */
public record AddDirectoryEntriesRequest(
    Map<String, UUID> entries
) {
}
