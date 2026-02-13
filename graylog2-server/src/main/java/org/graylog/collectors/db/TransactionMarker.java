package org.graylog.collectors.db;

import jakarta.annotation.Nullable;
import org.bson.Document;

import java.util.Set;

/**
 * A raw marker entry from the fleet transaction log.
 *
 * @param seq       sequence number (the _id in MongoDB)
 * @param target    "fleet" or "collector"
 * @param targetIds fleet IDs or collector instance UIDs (always a set, even for single targets)
 * @param type      parsed marker type
 * @param rawType   original string from MongoDB (for logging unknown types)
 * @param payload   optional type-specific data (e.g., new_fleet_id for FLEET_REASSIGNED)
 */
public record TransactionMarker(long seq,
                                String target,
                                Set<String> targetIds,
                                MarkerType type,
                                String rawType,
                                @Nullable Document payload) {

    public static final String TARGET_FLEET = "fleet";
    public static final String TARGET_COLLECTOR = "collector";
}
