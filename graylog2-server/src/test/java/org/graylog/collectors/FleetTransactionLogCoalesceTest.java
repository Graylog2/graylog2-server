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

import org.bson.Document;
import org.graylog.collectors.db.CoalescedActions;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.db.TransactionMarker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.collectors.db.TransactionMarker.TARGET_COLLECTOR;
import static org.graylog.collectors.db.TransactionMarker.TARGET_FLEET;

class FleetTransactionLogCoalesceTest {

    @Test
    void emptyMarkerListProducesNoActions() {
        CoalescedActions actions = FleetTransactionLogService.doCoalesce(List.of());
        assertThat(actions.recomputeConfig()).isFalse();
        assertThat(actions.newFleetId()).isNull();
        assertThat(actions.restart()).isFalse();
        assertThat(actions.runDiscovery()).isFalse();
        assertThat(actions.maxSeq()).isEqualTo(0L);
    }

    @Test
    void singleConfigChangedSetsRecomputeConfig() {
        var markers = List.of(marker(10, TARGET_FLEET, "fleet-1", MarkerType.CONFIG_CHANGED));
        CoalescedActions actions = FleetTransactionLogService.doCoalesce(markers);
        assertThat(actions.recomputeConfig()).isTrue();
        assertThat(actions.restart()).isFalse();
        assertThat(actions.runDiscovery()).isFalse();
        assertThat(actions.maxSeq()).isEqualTo(10L);
    }

    @Test
    void multipleConfigChangedCoalescesToOne() {
        var markers = List.of(
                marker(10, TARGET_FLEET, "fleet-1", MarkerType.CONFIG_CHANGED),
                marker(15, TARGET_FLEET, "fleet-1", MarkerType.CONFIG_CHANGED),
                marker(20, TARGET_FLEET, "fleet-1", MarkerType.CONFIG_CHANGED)
        );
        CoalescedActions actions = FleetTransactionLogService.doCoalesce(markers);
        assertThat(actions.recomputeConfig()).isTrue();
        assertThat(actions.maxSeq()).isEqualTo(20L);
    }

    @Test
    void singleRestartSetsRestartFlag() {
        var markers = List.of(marker(10, TARGET_COLLECTOR, "inst-1", MarkerType.RESTART));
        CoalescedActions actions = FleetTransactionLogService.doCoalesce(markers);
        assertThat(actions.restart()).isTrue();
        assertThat(actions.recomputeConfig()).isFalse();
        assertThat(actions.maxSeq()).isEqualTo(10L);
    }

    @Test
    void multipleRestartsCoalescesToOne() {
        var markers = List.of(
                marker(10, TARGET_FLEET, "fleet-1", MarkerType.RESTART),
                marker(15, TARGET_COLLECTOR, "inst-1", MarkerType.RESTART)
        );
        CoalescedActions actions = FleetTransactionLogService.doCoalesce(markers);
        assertThat(actions.restart()).isTrue();
        assertThat(actions.maxSeq()).isEqualTo(15L);
    }

    @Test
    void singleDiscoveryRunSetsFlag() {
        var markers = List.of(marker(10, TARGET_COLLECTOR, "inst-1", MarkerType.DISCOVERY_RUN));
        CoalescedActions actions = FleetTransactionLogService.doCoalesce(markers);
        assertThat(actions.runDiscovery()).isTrue();
        assertThat(actions.maxSeq()).isEqualTo(10L);
    }

    @Test
    void fleetReassignedSetsNewFleetIdAndRecomputeConfig() {
        var markers = List.of(
                reassignmentMarker(10, "inst-1", "fleet-B")
        );
        CoalescedActions actions = FleetTransactionLogService.doCoalesce(markers);
        assertThat(actions.recomputeConfig()).isTrue();
        assertThat(actions.newFleetId()).isEqualTo("fleet-B");
        assertThat(actions.maxSeq()).isEqualTo(10L);
    }

    @Test
    void fleetReassignedUsesHighestSeqReassignment() {
        var markers = List.of(
                reassignmentMarker(10, "inst-1", "fleet-B"),
                reassignmentMarker(20, "inst-1", "fleet-C")
        );
        CoalescedActions actions = FleetTransactionLogService.doCoalesce(markers);
        assertThat(actions.newFleetId()).isEqualTo("fleet-C");
        assertThat(actions.maxSeq()).isEqualTo(20L);
    }

    @Test
    void fleetReassignedSupersedesFleetLevelMarkers() {
        var markers = List.of(
                marker(10, TARGET_FLEET, "fleet-A", MarkerType.CONFIG_CHANGED),
                marker(12, TARGET_FLEET, "fleet-A", MarkerType.RESTART),
                reassignmentMarker(15, "inst-1", "fleet-B")
        );
        CoalescedActions actions = FleetTransactionLogService.doCoalesce(markers);
        assertThat(actions.recomputeConfig()).isTrue();
        assertThat(actions.newFleetId()).isEqualTo("fleet-B");
        // Fleet-level RESTART from old fleet is discarded
        assertThat(actions.restart()).isFalse();
        assertThat(actions.maxSeq()).isEqualTo(15L);
    }

    @Test
    void fleetReassignedPreservesCollectorLevelCommands() {
        var markers = List.of(
                marker(10, TARGET_COLLECTOR, "inst-1", MarkerType.RESTART),
                reassignmentMarker(15, "inst-1", "fleet-B"),
                marker(18, TARGET_COLLECTOR, "inst-1", MarkerType.DISCOVERY_RUN)
        );
        CoalescedActions actions = FleetTransactionLogService.doCoalesce(markers);
        assertThat(actions.recomputeConfig()).isTrue();
        assertThat(actions.newFleetId()).isEqualTo("fleet-B");
        assertThat(actions.restart()).isTrue();
        assertThat(actions.runDiscovery()).isTrue();
        assertThat(actions.maxSeq()).isEqualTo(18L);
    }

    @Test
    void unknownMarkerTypesAreSkipped() {
        var markers = List.of(
                new TransactionMarker(10, TARGET_FLEET, Set.of("fleet-1"), MarkerType.UNKNOWN, "FUTURE_TYPE", null),
                marker(15, TARGET_FLEET, "fleet-1", MarkerType.CONFIG_CHANGED)
        );
        CoalescedActions actions = FleetTransactionLogService.doCoalesce(markers);
        assertThat(actions.recomputeConfig()).isTrue();
        // maxSeq includes the unknown marker — we advance past it
        assertThat(actions.maxSeq()).isEqualTo(15L);
    }

    @Test
    void maxSeqIncludesUnknownMarkers() {
        var markers = List.of(
                new TransactionMarker(20, TARGET_FLEET, Set.of("fleet-1"), MarkerType.UNKNOWN, "FUTURE_TYPE", null)
        );
        CoalescedActions actions = FleetTransactionLogService.doCoalesce(markers);
        assertThat(actions.recomputeConfig()).isFalse();
        assertThat(actions.maxSeq()).isEqualTo(20L);
    }

    @Test
    void mixedMarkerTypesProduceAllFlags() {
        var markers = List.of(
                marker(10, TARGET_FLEET, "fleet-1", MarkerType.CONFIG_CHANGED),
                marker(11, TARGET_COLLECTOR, "inst-1", MarkerType.RESTART),
                marker(12, TARGET_COLLECTOR, "inst-1", MarkerType.DISCOVERY_RUN)
        );
        CoalescedActions actions = FleetTransactionLogService.doCoalesce(markers);
        assertThat(actions.recomputeConfig()).isTrue();
        assertThat(actions.restart()).isTrue();
        assertThat(actions.runDiscovery()).isTrue();
        assertThat(actions.newFleetId()).isNull();
        assertThat(actions.maxSeq()).isEqualTo(12L);
    }

    // --- helpers ---

    private static TransactionMarker marker(long seq, String target, String targetId, MarkerType type) {
        return new TransactionMarker(seq, target, Set.of(targetId), type, type.name(), null);
    }

    private static TransactionMarker reassignmentMarker(long seq, String instanceUid, String newFleetId) {
        return new TransactionMarker(seq, TransactionMarker.TARGET_COLLECTOR, Set.of(instanceUid),
                MarkerType.FLEET_REASSIGNED, MarkerType.FLEET_REASSIGNED.name(),
                new Document("new_fleet_id", newFleetId));
    }
}
