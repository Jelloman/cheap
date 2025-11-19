package net.netbeing.cheap.json.dto;

import java.util.List;

/**
 * Request to remove nodes from an EntityTree hierarchy.
 * Removal always cascades to remove all descendants.
 *
 * @param paths List of node paths to remove (must not be null or empty)
 */
public record RemoveTreeNodesRequest(
    List<String> paths
) {
}
