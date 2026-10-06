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
package org.graylog2.indexer.management.allocation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import javax.annotation.Nullable;

/**
 * What an unassigned shard's situation is, in terms a Graylog user can act on: the situation, where its data
 * still is, what is damaged or blocking, and the options in order of how much data they keep. The web interface
 * turns this into sentences; the raw explain text stays available next to it.
 *
 * @param situation       the matched situation; {@link Situation#UNKNOWN} when none matched
 * @param needsAction     false when OpenSearch resolves it on its own (waiting, throttling, a replica rebuild)
 * @param copies          for primaries: the nodes that reported a copy of the shard and its state
 * @param failedOnNode    the node a failed recovery ran on (name if known, else its id)
 * @param leftNode        the id of the node whose departure unassigned the shard
 * @param damagedFile     file name of the damaged file, e.g. {@code translog.ckp}
 * @param cause           the decisive exception class or message, e.g. {@code TranslogCorruptedException}
 * @param blocking        the checks that said NO (or THROTTLE) on every node
 * @param remainingDelayMs for a delayed replica: milliseconds until OpenSearch allocates it elsewhere
 * @param options         what can be done, most data kept first
 * @param avoid           actions that look plausible but fail or make it worse in this situation
 */
public record AllocationDiagnosis(@JsonProperty("situation") Situation situation,
                                  @JsonProperty("needs_action") boolean needsAction,
                                  @JsonProperty("copies") List<Copy> copies,
                                  @JsonProperty("failed_on_node") @Nullable String failedOnNode,
                                  @JsonProperty("left_node") @Nullable String leftNode,
                                  @JsonProperty("damaged_file") @Nullable String damagedFile,
                                  @JsonProperty("cause") @Nullable String cause,
                                  @JsonProperty("blocking") List<Blocker> blocking,
                                  @JsonProperty("remaining_delay_ms") @Nullable Long remainingDelayMs,
                                  @JsonProperty("options") List<Option> options,
                                  @JsonProperty("avoid") List<Action> avoid) {

    /** Situations, see research-allocation-explain.md §5 and §6 in the graylog-more-mgmt repo. */
    public enum Situation {
        // Resolves itself
        INITIALIZING,
        DELAYED_NODE_LEFT,
        FETCHING_SHARD_DATA,
        THROTTLED,
        REPLICA_REBUILDS_FROM_PRIMARY,
        PRIMARY_NOT_ACTIVE,
        // Failed recoveries
        TRANSIENT_FAILURE,
        RESTORE_FAILED,
        TRANSLOG_DAMAGED,
        RETENTION_LEASES_DAMAGED,
        SEGMENT_DATA_DAMAGED,
        COMMIT_UNREADABLE,
        DAMAGED_OTHER,
        // Copies
        STALE_COPY_ONLY,
        NO_COPY_FOUND,
        // Allocation rules (deciders)
        TOO_FEW_NODES,
        ALLOCATION_FILTER,
        SHARDS_PER_NODE_LIMIT,
        ALLOCATION_DISABLED,
        DISK_WATERMARK,
        AWARENESS,
        NODE_VERSION,
        RETRY_LIMIT,
        OTHER_RULE,
        UNKNOWN
    }

    public enum CopyState {
        IN_SYNC,
        STALE,
        DAMAGED,
        LOCKED
    }

    public record Copy(@JsonProperty("node") String node,
                       @JsonProperty("state") CopyState state,
                       @JsonProperty("problem") @Nullable String problem) {
    }

    public record Blocker(@JsonProperty("rule") String rule,
                          @JsonProperty("decision") String decision,
                          @JsonProperty("explanation") @Nullable String explanation) {
    }

    public enum Action {
        WAIT,
        RETRY_FAILED,
        FIX_ENVIRONMENT_THEN_RETRY,
        FIX_PRIMARY,
        RESTORE_AGAIN,
        SHARD_TOOL_THEN_ALLOCATE_STALE_PRIMARY,
        ALLOCATE_STALE_PRIMARY,
        RESTORE_SNAPSHOT,
        ALLOCATE_EMPTY_PRIMARY,
        DELETE_INDEX,
        BRING_NODE_BACK,
        ADD_NODES,
        LOWER_REPLICAS,
        CHANGE_ALLOCATION_FILTER,
        RAISE_SHARD_LIMIT,
        ENABLE_ALLOCATION,
        FREE_DISK_SPACE,
        FIX_AWARENESS,
        FINISH_UPGRADE
    }

    /** What an option gives up. */
    public enum DataLoss {
        NONE,
        UNFLUSHED_OPERATIONS,
        DOCUMENTS_IN_DAMAGED_SEGMENTS,
        WRITES_THE_STALE_COPY_MISSED,
        WRITES_SINCE_SNAPSHOT,
        WHOLE_SHARD,
        WHOLE_INDEX,
        UNKNOWN
    }

    /** Who can carry an option out. */
    public enum Where {
        AUTOMATIC,
        GRAYLOG,
        OPENSEARCH_API,
        HOST_ACCESS,
        INFRASTRUCTURE
    }

    /**
     * @param command for OPENSEARCH_API and HOST_ACCESS options: the exact request body or command line
     */
    public record Option(@JsonProperty("action") Action action,
                         @JsonProperty("data_loss") DataLoss dataLoss,
                         @JsonProperty("where") Where where,
                         @JsonProperty("node") @Nullable String node,
                         @JsonProperty("command") @Nullable String command) {
    }
}
