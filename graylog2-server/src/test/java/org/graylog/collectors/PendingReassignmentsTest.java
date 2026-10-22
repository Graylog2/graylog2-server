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
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.db.TransactionMarker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PendingReassignmentsTest {

    @Test
    void resolvesTheTargetOfAnUnprocessedReassignment() {
        final var reassignments = PendingReassignments.of(List.of(reassignment(5L, "fleet-b", "uid-1")));

        assertThat(reassignments.targetFleetId("uid-1", "fleet-a", 4L)).contains("fleet-b");
    }

    @Test
    void ignoresProcessedReassignments() {
        final var reassignments = PendingReassignments.of(List.of(reassignment(5L, "fleet-b", "uid-1")));

        assertThat(reassignments.targetFleetId("uid-1", "fleet-b", 5L)).isEmpty();
    }

    @Test
    void theLatestUnprocessedReassignmentWins() {
        final var reassignments = PendingReassignments.of(List.of(
                reassignment(7L, "fleet-c", "uid-1"),
                reassignment(5L, "fleet-b", "uid-1")));

        assertThat(reassignments.targetFleetId("uid-1", "fleet-a", 0L)).contains("fleet-c");
        // Only the later marker is still unprocessed.
        assertThat(reassignments.targetFleetId("uid-1", "fleet-b", 6L)).contains("fleet-c");
    }

    @Test
    void aReassignmentBackToTheCurrentFleetIsNotPending() {
        final var reassignments = PendingReassignments.of(List.of(
                reassignment(5L, "fleet-b", "uid-1"),
                reassignment(7L, "fleet-a", "uid-1")));

        assertThat(reassignments.targetFleetId("uid-1", "fleet-a", 0L)).isEmpty();
    }

    @Test
    void resolvesEachTargetOfABulkReassignment() {
        final var reassignments = PendingReassignments.of(List.of(reassignment(5L, "fleet-b", "uid-1", "uid-2")));

        assertThat(reassignments.instanceUids()).containsExactlyInAnyOrder("uid-1", "uid-2");
        assertThat(reassignments.targetFleetId("uid-2", "fleet-a", 0L)).contains("fleet-b");
        assertThat(reassignments.targetFleetId("uid-3", "fleet-a", 0L)).isEmpty();
    }

    @Test
    void ignoresMarkersWithoutAReassignmentPayload() {
        final var marker = new TransactionMarker(5L, TransactionMarker.TARGET_COLLECTOR, Set.of("uid-1"),
                MarkerType.FLEET_REASSIGNED, null, null, null, null);

        assertThat(PendingReassignments.of(List.of(marker)).targetFleetId("uid-1", "fleet-a", 0L)).isEmpty();
    }

    @Test
    void ignoresReassignmentsWithoutATargetFleet() {
        final var reassignments = PendingReassignments.of(List.of(reassignment(5L, null, "uid-1")));

        assertThat(reassignments.instanceUids()).isEmpty();
        assertThat(reassignments.targetFleetId("uid-1", "fleet-a", 0L)).isEmpty();
    }

    private static TransactionMarker reassignment(long seq, String targetFleetId, String... instanceUids) {
        return new TransactionMarker(seq, TransactionMarker.TARGET_COLLECTOR, Set.of(instanceUids),
                MarkerType.FLEET_REASSIGNED, new FleetReassignedPayload(targetFleetId), null, null, null);
    }
}
