package org.graylog.collectors.db;

/**
 * Types of markers in the fleet transaction log. Stored as strings in MongoDB.
 * Unknown values (from newer server versions) are parsed as {@link #UNKNOWN} to ensure
 * forward compatibility across cluster upgrades.
 */
public enum MarkerType {
    CONFIG_CHANGED,
    RESTART,
    DISCOVERY_RUN,
    FLEET_REASSIGNED,
    UNKNOWN;

    /**
     * Parse a marker type from its string representation, returning {@link #UNKNOWN}
     * for unrecognized values instead of throwing.
     */
    public static MarkerType fromString(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
