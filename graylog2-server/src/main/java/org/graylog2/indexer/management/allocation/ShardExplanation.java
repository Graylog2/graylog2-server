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

/**
 * The essentials of one {@code _cluster/allocation/explain} answer: which shard, why it is unassigned, whether it
 * can be allocated, and per node only the deciders that said NO (or THROTTLE). Weights, YES decisions and store
 * details are dropped from the display fields; {@code diagnosis} is computed from the full answer
 * ({@link AllocationDiagnoser}).
 */
public record ShardExplanation(@JsonProperty("index") String index,
                               @JsonProperty("shard") int shard,
                               @JsonProperty("primary") boolean primary,
                               @JsonProperty("current_state") String currentState,
                               @JsonProperty("unassigned_reason") String unassignedReason,
                               @JsonProperty("unassigned_since") String unassignedSince,
                               @JsonProperty("failed_attempts") Integer failedAttempts,
                               @JsonProperty("root_cause") String rootCause,
                               @JsonProperty("details") String details,
                               @JsonProperty("can_allocate") String canAllocate,
                               @JsonProperty("explanation") String explanation,
                               @JsonProperty("max_retries_exceeded") boolean maxRetriesExceeded,
                               @JsonProperty("nodes") List<NodeDecision> nodes,
                               @JsonProperty("diagnosis") AllocationDiagnosis diagnosis,
                               @JsonProperty("error") String error) {

    public record NodeDecision(@JsonProperty("node_name") String nodeName,
                               @JsonProperty("decision") String decision,
                               @JsonProperty("deciders") List<Decider> deciders) {
    }

    public record Decider(@JsonProperty("decider") String decider,
                          @JsonProperty("decision") String decision,
                          @JsonProperty("explanation") String explanation) {
    }

    /** An unassigned shard whose explain call failed (e.g. it got assigned in the meantime). */
    public static ShardExplanation failed(UnassignedShard shard, String error) {
        return new ShardExplanation(shard.index(), shard.shard(), shard.primary(), "unassigned", shard.reason(),
                shard.since(), null, null, null, null, null, false, List.of(), null, error);
    }

    public record Response(@JsonProperty("unassigned_total") int unassignedTotal,
                           @JsonProperty("unassigned_primaries") int unassignedPrimaries,
                           @JsonProperty("explained") List<ShardExplanation> explained,
                           @JsonProperty("truncated") boolean truncated,
                           @JsonProperty("generated_at") String generatedAt) {
    }
}
