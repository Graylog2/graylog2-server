/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.collectors;

import org.graylog.collectors.db.FleetReassignedPayload;
import org.graylog.collectors.db.TransactionMarker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves pending fleet reassignments from the collector-targeted {@code FLEET_REASSIGNED} markers.
 * <p>
 * A reassignment only updates an instance's {@code fleet_id} once the collector checks in and processes the
 * marker. Until then, the instance's effective fleet is the target of its latest unprocessed reassignment marker,
 * following the same "highest sequence number wins" rule as {@link FleetTransactionLogService#coalesce}.
 */
public final class PendingReassignments {

    private static final PendingReassignments NONE = new PendingReassignments(Map.of());

    private record Reassignment(long seq, String targetFleetId) {}

    // Only the latest reassignment per instance matters: if it has been processed, all earlier ones have been as well.
    private final Map<String, Reassignment> latestByInstanceUid;

    private PendingReassignments(Map<String, Reassignment> latestByInstanceUid) {
        this.latestByInstanceUid = latestByInstanceUid;
    }

    /**
     * @param reassignmentMarkers collector-targeted {@code FLEET_REASSIGNED} markers, see
     *                            {@link FleetTransactionLogService#pendingReassignments()}
     */
    public static PendingReassignments of(List<TransactionMarker> reassignmentMarkers) {
        if (reassignmentMarkers.isEmpty()) {
            return NONE;
        }
        final Map<String, Reassignment> latestByUid = new HashMap<>();
        for (final var marker : reassignmentMarkers) {
            // A reassignment marker without a target fleet has nothing to resolve to.
            if (marker.payload() instanceof FleetReassignedPayload(String targetFleetId) && targetFleetId != null) {
                final var reassignment = new Reassignment(marker.seq(), targetFleetId);
                marker.targetIds().forEach(uid ->
                        latestByUid.merge(uid, reassignment, (a, b) -> a.seq() >= b.seq() ? a : b));
            }
        }
        return new PendingReassignments(latestByUid);
    }

    public static PendingReassignments none() {
        return NONE;
    }

    /**
     * The instance UIDs that have at least one reassignment marker, processed or not.
     */
    public Set<String> instanceUids() {
        return latestByInstanceUid.keySet();
    }

    /**
     * The fleet an instance is being moved to: the target of its latest reassignment marker it has not processed
     * yet, unless that target is its current fleet.
     */
    public Optional<String> targetFleetId(String instanceUid, String currentFleetId, long lastProcessedSeq) {
        return Optional.ofNullable(latestByInstanceUid.get(instanceUid))
                .filter(reassignment -> reassignment.seq() > lastProcessedSeq)
                .map(Reassignment::targetFleetId)
                .filter(targetFleetId -> !targetFleetId.equals(currentFleetId));
    }
}
