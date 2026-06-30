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
package org.graylog.collectors.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.collectors.db.CoalescedActions;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Pending (not yet applied) changes for a single collector instance: the coalesced net effect plus the
 * queued markers as display-ready activity entries (same format as the activity feed, with fleet/host/actor
 * names resolved server-side).
 */
public record PendingChangesResponse(
        @JsonProperty("has_pending_changes") boolean hasPendingChanges,
        @JsonProperty("coalesced") CoalescedActionsView coalesced,
        @JsonProperty("activities") List<RecentActivityResponse.ActivityEntry> activities) {

    public PendingChangesResponse {
        requireNonNull(coalesced, "coalesced must not be null");
        requireNonNull(activities, "activities must not be null");
    }

    /**
     * The net effect of all pending markers, as computed by
     * {@link org.graylog.collectors.FleetTransactionLogService#coalesce(List)} — e.g. five queued config
     * changes coalesce into a single {@code recompute_config}, and a reassignment supersedes markers from
     * the old fleet.
     */
    public record CoalescedActionsView(
            @JsonProperty("recompute_config") boolean recomputeConfig,
            @JsonProperty("recompute_ingest_config") boolean recomputeIngestConfig,
            @JsonProperty("reassign") boolean reassign,
            @JsonProperty("restart") boolean restart,
            @JsonProperty("run_discovery") boolean runDiscovery) {

        static CoalescedActionsView from(CoalescedActions actions) {
            // We only expose whether a reassignment is pending, not its target fleet id. Exposing the id
            // would require a per-user permission check (see ActivityEntryMapper#resolveDetails); the
            // boolean leaks nothing, and the (permission-filtered) destination is available in the
            // activity entries.
            return new CoalescedActionsView(
                    actions.recomputeConfig(),
                    actions.recomputeIngestConfig(),
                    actions.newFleetId() != null,
                    actions.restart(),
                    actions.runDiscovery());
        }
    }
}
