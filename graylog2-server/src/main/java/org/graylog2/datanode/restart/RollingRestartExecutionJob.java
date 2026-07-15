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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.oxo42.stateless4j.StateMachine;
import com.google.auto.value.AutoValue;
import com.google.inject.assistedinject.Assisted;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.graylog.scheduler.Job;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobTriggerData;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.JobTriggerUpdate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class RollingRestartExecutionJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(RollingRestartExecutionJob.class);

    public static final String TYPE_NAME = "rolling-restart-v1";

    static final Duration TICK_INTERVAL = Duration.ofSeconds(5);
    static final Duration GREEN_WAIT_TIMEOUT = Duration.ofMinutes(30);

    /**
     * Built-in singleton job definition. The ID must remain stable across deployments so triggers can reference it.
     */
    public static final JobDefinitionDto DEFINITION_INSTANCE = JobDefinitionDto.builder()
            .id("6840d4f5b8c1d2e3a4f5b8c1")
            .title("DataNode Rolling Restart")
            .description("Built-in job definition for rolling restarts of the underlying OpenSearch processes on DataNodes.")
            .config(Config.builder().build())
            .build();

    private final RollingRestartActions actions;

    public interface Factory extends Job.Factory<RollingRestartExecutionJob> {
        @Override
        RollingRestartExecutionJob create(JobDefinitionDto jobDefinition);
    }

    @Inject
    public RollingRestartExecutionJob(@Assisted JobDefinitionDto jobDefinition, RollingRestartActions actions) {
        this.actions = actions;
    }

    @Override
    public JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException {
        final Data data = ctx.trigger().data()
                .filter(Data.class::isInstance)
                .map(Data.class::cast)
                .orElseThrow(() -> new JobExecutionException(
                        "Missing data for rolling restart job",
                        ctx.trigger(),
                        JobTriggerUpdate.withError(ctx.trigger())));

        try {
            final Data advanced = advance(data);
            return toUpdate(advanced);
        } catch (Exception e) {
            LOG.error("Rolling restart tick failed", e);
            try {
                actions.enableAllocation();
            } catch (Exception cleanup) {
                LOG.warn("Failed to re-enable allocation during failure cleanup", cleanup);
            }
            final Data failed = data.toBuilder()
                    .smState(RollingRestartState.FAILED)
                    .lastError(e.getMessage())
                    .build();
            return JobTriggerUpdate.builder()
                    .status(JobTriggerStatus.ERROR)
                    .data(failed)
                    .nextTime(null)
                    .build();
        }
    }

    private JobTriggerUpdate toUpdate(Data data) {
        return switch (data.smState()) {
            case COMPLETED -> JobTriggerUpdate.builder().data(data).nextTime(null).build();
            case ABORTED ->
                    JobTriggerUpdate.builder().status(JobTriggerStatus.CANCELLED).data(data).nextTime(null).build();
            case FAILED -> JobTriggerUpdate.builder().status(JobTriggerStatus.ERROR).data(data).nextTime(null).build();
            default -> JobTriggerUpdate.withNextTimeAndData(
                    DateTime.now(DateTimeZone.UTC).plus(TICK_INTERVAL.toMillis()),
                    data);
        };
    }

    private Data advance(Data data) {
        final StateMachine<RollingRestartState, RollingRestartTrigger> sm =
                RollingRestartStateMachineBuilder.buildFromState(data.smState());

        switch (data.smState()) {
            case PREPARING_CLUSTER -> {
                actions.prepareCluster();
                sm.fire(RollingRestartTrigger.PROCEED);
                return data.toBuilder().smState(sm.getState()).build();
            }
            case SELECTING_NEXT_NODE -> {
                if (data.abortRequested()) {
                    sm.fire(RollingRestartTrigger.ABORT);
                    return data.toBuilder().smState(sm.getState()).build();
                }
                final int nextIdx = pickNextNodeIndex(data);
                if (nextIdx < 0) {
                    sm.fire(RollingRestartTrigger.NO_MORE_NODES);
                    return data.toBuilder().smState(sm.getState()).build();
                }
                sm.fire(RollingRestartTrigger.MORE_NODES);
                return data.toBuilder()
                        .smState(sm.getState())
                        .currentNodeIndex(nextIdx)
                        .nodes(replaceNode(data.nodes(), nextIdx,
                                data.nodes().get(nextIdx)
                                        .withStatus(RollingRestartNodeStatus.RESTARTING)
                                        .withStarted(Instant.now())))
                        .build();
            }
            case UPGRADING_NODE -> {
                final RollingRestartNodeEntry current = requireCurrent(data);
                actions.upgradeNode(current.hostname());
                sm.fire(RollingRestartTrigger.PROCEED);
                return data.toBuilder().smState(sm.getState()).build();
            }
            case WAITING_NODE_JOINED -> {
                final RollingRestartNodeEntry current = requireCurrent(data);
                String expectedVersion = data.targetOpensearchVersion();

                if (actions.isNodeInCluster(current.hostname(), expectedVersion)) {
                    sm.fire(RollingRestartTrigger.NODE_JOINED);
                    return data.toBuilder()
                            .smState(sm.getState())
                            .nodes(replaceNode(data.nodes(), data.currentNodeIndex(),
                                    current.withStatus(RollingRestartNodeStatus.STARTED)))
                            .build();
                }
                return data;
            }
            case REENABLING_ALLOCATION -> {
                actions.enableAllocation();
                sm.fire(RollingRestartTrigger.PROCEED);
                return data.toBuilder()
                        .smState(sm.getState())
                        .waitingGreenSince(Instant.now())
                        .build();
            }
            case WAITING_GREEN -> {
                if (actions.isClusterGreen()) {
                    sm.fire(RollingRestartTrigger.CLUSTER_GREEN);
                    final RollingRestartNodeEntry current = requireCurrent(data);
                    return data.toBuilder()
                            .smState(sm.getState())
                            .nodes(replaceNode(data.nodes(), data.currentNodeIndex(),
                                    current.withStatus(RollingRestartNodeStatus.COMPLETED).withFinished(Instant.now())))
                            .build();
                }
                if (Duration.between(data.waitingGreenSince(), Instant.now()).compareTo(GREEN_WAIT_TIMEOUT) > 0) {
                    sm.fire(RollingRestartTrigger.GREEN_TIMEOUT);
                    return data.toBuilder()
                            .smState(sm.getState())
                            .pausedReason("Cluster did not return to GREEN within "
                                    + GREEN_WAIT_TIMEOUT
                                    + ". Investigate and POST /datanodes/restart/resume to retry.")
                            .build();
                }
                return data;
            }
            case PAUSED_WAITING_GREEN -> {
                // Wait for explicit resume via the handler, which sets smState back to WAITING_GREEN.
                return data;
            }
            case FINALIZING -> {
                if (!actions.isAllocationEnabled()) {
                    actions.enableAllocation();
                }
                sm.fire(RollingRestartTrigger.PROCEED);
                return data.toBuilder().smState(sm.getState()).build();
            }
            default -> {
                return data;
            }
        }
    }

    private RollingRestartNodeEntry requireCurrent(Data data) {
        if (data.currentNodeIndex() < 0 || data.currentNodeIndex() >= data.nodes().size()) {
            throw new IllegalStateException("No current node selected for state " + data.smState());
        }
        return data.nodes().get(data.currentNodeIndex());
    }

    private int pickNextNodeIndex(Data data) {
        final List<RollingRestartNodeEntry> nodes = data.nodes();
        final Optional<String> manager = actions.electedManagerHostname();

        for (int i = 0; i < nodes.size(); i++) {
            final RollingRestartNodeEntry e = nodes.get(i);
            if (e.status() == RollingRestartNodeStatus.PENDING
                    && (manager.isEmpty() || !manager.get().equals(e.hostname()))) {
                return i;
            }
        }
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).status() == RollingRestartNodeStatus.PENDING) {
                return i;
            }
        }
        return -1;
    }

    private static List<RollingRestartNodeEntry> replaceNode(List<RollingRestartNodeEntry> nodes, int idx, RollingRestartNodeEntry replacement) {
        final java.util.ArrayList<RollingRestartNodeEntry> out = new java.util.ArrayList<>(nodes);
        out.set(idx, replacement);
        return List.copyOf(out);
    }

    @AutoValue
    @JsonTypeName(RollingRestartExecutionJob.TYPE_NAME)
    @JsonDeserialize(builder = Config.Builder.class)
    public abstract static class Config implements JobDefinitionConfig {

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public abstract static class Builder implements JobDefinitionConfig.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_RollingRestartExecutionJob_Config.Builder().type(TYPE_NAME);
            }

            abstract Config autoBuild();

            public Config build() {
                type(TYPE_NAME);
                return autoBuild();
            }
        }
    }

    @AutoValue
    @JsonTypeName(RollingRestartExecutionJob.TYPE_NAME)
    @JsonDeserialize(builder = Data.Builder.class)
    public abstract static class Data implements JobTriggerData {

        public static final String FIELD_SM_STATE = "sm_state";
        public static final String FIELD_NODES = "nodes";
        public static final String FIELD_CURRENT_NODE_INDEX = "current_node_index";
        public static final String FIELD_ABORT_REQUESTED = "abort_requested";
        public static final String FIELD_TRIGGERED_BY = "triggered_by";
        public static final String FIELD_PAUSED_REASON = "paused_reason";
        public static final String FIELD_LAST_ERROR = "last_error";
        public static final String FIELD_WAITING_GREEN_SINCE = "waiting_green_since";
        public static final String TARGET_OPENSEARCH_VERSION = "target_opensearch_version";

        @JsonProperty(FIELD_SM_STATE)
        public abstract RollingRestartState smState();

        @JsonProperty(FIELD_NODES)
        public abstract List<RollingRestartNodeEntry> nodes();

        @JsonProperty(FIELD_CURRENT_NODE_INDEX)
        public abstract int currentNodeIndex();

        @JsonProperty(FIELD_ABORT_REQUESTED)
        public abstract boolean abortRequested();

        @JsonProperty(FIELD_TRIGGERED_BY)
        public abstract String triggeredBy();

        @Nullable
        @JsonProperty(FIELD_PAUSED_REASON)
        public abstract String pausedReason();

        @Nullable
        @JsonProperty(FIELD_LAST_ERROR)
        public abstract String lastError();

        @JsonProperty(FIELD_WAITING_GREEN_SINCE)
        public abstract Instant waitingGreenSince();

        @JsonProperty(TARGET_OPENSEARCH_VERSION)
        public abstract String targetOpensearchVersion();


        public abstract Builder toBuilder();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public abstract static class Builder implements JobTriggerData.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_RollingRestartExecutionJob_Data.Builder()
                        .type(TYPE_NAME)
                        .currentNodeIndex(-1)
                        .abortRequested(false)
                        .waitingGreenSince(Instant.EPOCH);
            }

            @JsonProperty(FIELD_SM_STATE)
            public abstract Builder smState(RollingRestartState smState);

            @JsonProperty(FIELD_NODES)
            public abstract Builder nodes(List<RollingRestartNodeEntry> nodes);

            @JsonProperty(FIELD_CURRENT_NODE_INDEX)
            public abstract Builder currentNodeIndex(int currentNodeIndex);

            @JsonProperty(FIELD_ABORT_REQUESTED)
            public abstract Builder abortRequested(boolean abortRequested);

            @JsonProperty(FIELD_TRIGGERED_BY)
            public abstract Builder triggeredBy(String triggeredBy);

            @JsonProperty(FIELD_PAUSED_REASON)
            public abstract Builder pausedReason(@Nullable String pausedReason);

            @JsonProperty(FIELD_LAST_ERROR)
            public abstract Builder lastError(@Nullable String lastError);

            @JsonProperty(FIELD_WAITING_GREEN_SINCE)
            public abstract Builder waitingGreenSince(Instant waitingGreenSince);

            @JsonProperty(TARGET_OPENSEARCH_VERSION)
            public abstract Builder targetOpensearchVersion(String targetOpensearchVersion);

            abstract Data autoBuild();

            public Data build() {
                type(TYPE_NAME);
                return autoBuild();
            }
        }
    }
}
