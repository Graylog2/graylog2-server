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
package org.graylog.collectors.db;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoalescedActionsTest {

    @Test
    void withForcedRecomputeSetsBothRecomputeFlags() {
        final var forced = CoalescedActions.empty(0L).withForcedRecompute(100L);

        assertThat(forced.recomputeConfig()).isTrue();
        assertThat(forced.recomputeIngestConfig()).isTrue();
    }

    @Test
    void withForcedRecomputePreservesCommandsAndFleetReassignment() {
        final var actions = new CoalescedActions(false, false, "fleet-B", true, true, 150L);

        final var forced = actions.withForcedRecompute(100L);

        assertThat(forced.newFleetId()).isEqualTo("fleet-B");
        assertThat(forced.restart()).isTrue();
        assertThat(forced.runDiscovery()).isTrue();
    }

    @Test
    void withForcedRecomputeAdvancesMaxSeqToPurgeBoundary() {
        // No unprocessed markers: the collector must still acknowledge the purged range, otherwise
        // it stays below the truncation floor and is forced to recompute on every exchange.
        final var forced = CoalescedActions.empty(0L).withForcedRecompute(100L);

        assertThat(forced.maxSeq()).isEqualTo(100L);
    }

    @Test
    void withForcedRecomputeKeepsMaxSeqOfRetainedMarkers() {
        // Retained markers above the boundary were part of this exchange and win the max.
        final var actions = new CoalescedActions(true, false, null, false, false, 150L);

        final var forced = actions.withForcedRecompute(100L);

        assertThat(forced.maxSeq()).isEqualTo(150L);
    }
}
