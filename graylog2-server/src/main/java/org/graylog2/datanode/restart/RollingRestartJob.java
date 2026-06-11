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
package org.graylog2.datanode.restart;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import org.graylog2.database.MongoEntity;
import org.graylog2.jackson.MongoInstantDeserializer;
import org.graylog2.jackson.MongoInstantSerializer;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record RollingRestartJob(
        @ObjectId @Id @Nullable @JsonProperty(MongoEntity.FIELD_ID) String id,
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_CREATED_AT) Instant createdAt,
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_UPDATED_AT) Instant updatedAt,
        @Nullable
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_STARTED_AT) Instant startedAt,
        @Nullable
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_FINISHED_AT) Instant finishedAt,
        @JsonProperty(FIELD_STATUS) RollingRestartJobStatus status,
        @JsonProperty(FIELD_SM_STATE) RollingRestartState smState,
        @Nullable @JsonProperty(FIELD_PAUSED_REASON) String pausedReason,
        @Nullable @JsonProperty(FIELD_LAST_ERROR) String lastError,
        @JsonProperty(FIELD_TRIGGERED_BY) String triggeredBy,
        @JsonProperty(FIELD_ABORT_REQUESTED) boolean abortRequested,
        @JsonProperty(FIELD_CURRENT_NODE_INDEX) int currentNodeIndex,
        @JsonProperty(FIELD_NODES) List<RollingRestartNodeEntry> nodes
) implements MongoEntity {

    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_UPDATED_AT = "updated_at";
    public static final String FIELD_STARTED_AT = "started_at";
    public static final String FIELD_FINISHED_AT = "finished_at";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_SM_STATE = "sm_state";
    public static final String FIELD_PAUSED_REASON = "paused_reason";
    public static final String FIELD_LAST_ERROR = "last_error";
    public static final String FIELD_TRIGGERED_BY = "triggered_by";
    public static final String FIELD_ABORT_REQUESTED = "abort_requested";
    public static final String FIELD_CURRENT_NODE_INDEX = "current_node_index";
    public static final String FIELD_NODES = "nodes";

    public RollingRestartJob withId(String newId) {
        return new RollingRestartJob(newId, createdAt, updatedAt, startedAt, finishedAt, status, smState, pausedReason, lastError, triggeredBy, abortRequested, currentNodeIndex, nodes);
    }

    public RollingRestartJob withUpdatedAt(Instant at) {
        return new RollingRestartJob(id, createdAt, at, startedAt, finishedAt, status, smState, pausedReason, lastError, triggeredBy, abortRequested, currentNodeIndex, nodes);
    }

    public RollingRestartJob withSmState(RollingRestartState newState) {
        return new RollingRestartJob(id, createdAt, updatedAt, startedAt, finishedAt, status, newState, pausedReason, lastError, triggeredBy, abortRequested, currentNodeIndex, nodes);
    }

    public RollingRestartJob withJobStatus(RollingRestartJobStatus newStatus) {
        return new RollingRestartJob(id, createdAt, updatedAt, startedAt, finishedAt, newStatus, smState, pausedReason, lastError, triggeredBy, abortRequested, currentNodeIndex, nodes);
    }

    public RollingRestartJob withPausedReason(@Nullable String reason) {
        return new RollingRestartJob(id, createdAt, updatedAt, startedAt, finishedAt, status, smState, reason, lastError, triggeredBy, abortRequested, currentNodeIndex, nodes);
    }

    public RollingRestartJob withLastError(@Nullable String err) {
        return new RollingRestartJob(id, createdAt, updatedAt, startedAt, finishedAt, status, smState, pausedReason, err, triggeredBy, abortRequested, currentNodeIndex, nodes);
    }

    public RollingRestartJob withAbortRequested(boolean req) {
        return new RollingRestartJob(id, createdAt, updatedAt, startedAt, finishedAt, status, smState, pausedReason, lastError, triggeredBy, req, currentNodeIndex, nodes);
    }

    public RollingRestartJob withCurrentNodeIndex(int idx) {
        return new RollingRestartJob(id, createdAt, updatedAt, startedAt, finishedAt, status, smState, pausedReason, lastError, triggeredBy, abortRequested, idx, nodes);
    }

    public RollingRestartJob withNodes(List<RollingRestartNodeEntry> newNodes) {
        return new RollingRestartJob(id, createdAt, updatedAt, startedAt, finishedAt, status, smState, pausedReason, lastError, triggeredBy, abortRequested, currentNodeIndex, newNodes);
    }

    public RollingRestartJob withFinishedAt(Instant at) {
        return new RollingRestartJob(id, createdAt, updatedAt, startedAt, at, status, smState, pausedReason, lastError, triggeredBy, abortRequested, currentNodeIndex, nodes);
    }

    @Nullable
    public RollingRestartNodeEntry currentNode() {
        if (currentNodeIndex < 0 || currentNodeIndex >= nodes.size()) {
            return null;
        }
        return nodes.get(currentNodeIndex);
    }

    public RollingRestartJob withCurrentNode(RollingRestartNodeEntry newEntry) {
        if (currentNodeIndex < 0 || currentNodeIndex >= nodes.size()) {
            return this;
        }
        final List<RollingRestartNodeEntry> newNodes = new ArrayList<>(nodes);
        newNodes.set(currentNodeIndex, newEntry);
        return withNodes(newNodes);
    }
}
