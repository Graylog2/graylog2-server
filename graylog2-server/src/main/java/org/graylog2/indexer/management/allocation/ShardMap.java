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
import org.graylog2.indexer.management.allocation.AllocationDiagnosis.Situation;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Every node and every shard copy in one answer, cheap enough for large clusters ({@code _cat/nodes} and
 * {@code _cat/shards}, no explain calls). Unassigned copies carry a quick diagnosis from their failure text; the
 * full one comes from explaining that copy on demand.
 */
public record ShardMap(@JsonProperty("nodes") List<Node> nodes,
                       @JsonProperty("shards") List<Copy> shards,
                       @JsonProperty("generated_at") String generatedAt) {

    public record Node(@JsonProperty("id") String id,
                       @JsonProperty("name") String name,
                       @JsonProperty("roles") @Nullable String roles) {
    }

    /**
     * @param node         the node the copy is on (started, initializing, relocating from); null when unassigned
     * @param failedOnNode for an unassigned copy: the node its last recovery failed on
     * @param leftNode     for an unassigned copy: the id of the node whose departure unassigned it
     * @param situation    for an unassigned copy: the quick diagnosis, {@link Situation#UNKNOWN} until explained
     */
    public record Copy(@JsonProperty("index") String index,
                       @JsonProperty("shard") int shard,
                       @JsonProperty("primary") boolean primary,
                       @JsonProperty("state") String state,
                       @JsonProperty("node") @Nullable String node,
                       @JsonProperty("unassigned_reason") @Nullable String unassignedReason,
                       @JsonProperty("unassigned_since") @Nullable String unassignedSince,
                       @JsonProperty("failed_on_node") @Nullable String failedOnNode,
                       @JsonProperty("left_node") @Nullable String leftNode,
                       @JsonProperty("situation") @Nullable Situation situation,
                       @JsonProperty("needs_action") boolean needsAction) {
    }
}
