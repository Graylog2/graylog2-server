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

import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.JobTriggerUpdate;
import org.graylog.scheduler.JobTriggerUpdates;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RollingRestartExecutionJobTest {

    @Mock
    RollingRestartActions actions;

    @Mock
    JobTriggerUpdates jobTriggerUpdates;

    @Mock
    DBJobTriggerService jobTriggerService;

    private RollingRestartExecutionJob job;

    @BeforeEach
    void setUp() {
        job = new RollingRestartExecutionJob(RollingRestartExecutionJob.DEFINITION_INSTANCE, actions);
    }

    // ====== helpers ======

    private RollingRestartExecutionJob.Data baseData(RollingRestartState smState) {
        return RollingRestartExecutionJob.Data.builder()
                .smState(smState)
                .nodes(List.of(
                        RollingRestartNodeEntry.pending("node-a", "id-a"),
                        RollingRestartNodeEntry.pending("node-b", "id-b"),
                        RollingRestartNodeEntry.pending("node-c", "id-c")))
                .triggeredBy("alice")
                .waitingGreenSince(Instant.now())
                .targetOpensearchVersion("3.5.0")
                .build();
    }

    private JobExecutionContext ctxWith(RollingRestartExecutionJob.Data data) {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final JobTriggerDto trigger = JobTriggerDto.builder()
                .jobDefinitionType(RollingRestartExecutionJob.TYPE_NAME)
                .jobDefinitionId(RollingRestartExecutionJob.DEFINITION_INSTANCE.id())
                .schedule(OnceJobSchedule.create())
                .nextTime(now)
                .data(data)
                .build();
        return JobExecutionContext.create(
                trigger,
                RollingRestartExecutionJob.DEFINITION_INSTANCE,
                jobTriggerUpdates,
                new AtomicBoolean(true),
                jobTriggerService);
    }

    private static RollingRestartExecutionJob.Data dataOf(JobTriggerUpdate update) {
        return (RollingRestartExecutionJob.Data) update.data().orElseThrow();
    }

    // ====== happy path tests, one per state ======

    @Test
    void preparingCluster_disablesShardReplication_andAdvances() throws Exception {
        final var update = job.execute(ctxWith(baseData(RollingRestartState.PREPARING_CLUSTER)));

        verify(actions).prepareCluster();
        assertThat(dataOf(update).smState()).isEqualTo(RollingRestartState.SELECTING_NEXT_NODE);
        assertThat(update.nextTime()).isPresent();
    }

    @Test
    void selectingNextNode_picksNonManagerFirst() throws Exception {
        // node-b is the elected cluster manager — should be picked last
        when(actions.electedManagerHostname()).thenReturn(Optional.of("node-b"));

        final var update = job.execute(ctxWith(baseData(RollingRestartState.SELECTING_NEXT_NODE)));

        final var data = dataOf(update);
        assertThat(data.smState()).isEqualTo(RollingRestartState.UPGRADING_NODE);
        assertThat(data.currentNodeIndex()).isEqualTo(0);
        assertThat(data.nodes().get(0).hostname()).isEqualTo("node-a");
        assertThat(data.nodes().get(0).status()).isEqualTo(RollingRestartNodeStatus.RESTARTING);
    }

    @Test
    void selectingNextNode_managerLast_whenOnlyManagerPending() throws Exception {
        // Mark node-a and node-c as COMPLETED, leaving only the manager (node-b) pending.
        final var nodes = List.of(
                RollingRestartNodeEntry.pending("node-a", "id-a").withStatus(RollingRestartNodeStatus.COMPLETED),
                RollingRestartNodeEntry.pending("node-b", "id-b"),
                RollingRestartNodeEntry.pending("node-c", "id-c").withStatus(RollingRestartNodeStatus.COMPLETED));
        final var data = baseData(RollingRestartState.SELECTING_NEXT_NODE).toBuilder().nodes(nodes).build();
        when(actions.electedManagerHostname()).thenReturn(Optional.of("node-b"));

        final var update = job.execute(ctxWith(data));

        assertThat(dataOf(update).currentNodeIndex()).isEqualTo(1); // node-b
    }

    @Test
    void selectingNextNode_noPendingNodes_goesToFinalizing() throws Exception {
        final var nodes = List.of(
                RollingRestartNodeEntry.pending("node-a", "id-a").withStatus(RollingRestartNodeStatus.COMPLETED),
                RollingRestartNodeEntry.pending("node-b", "id-b").withStatus(RollingRestartNodeStatus.COMPLETED),
                RollingRestartNodeEntry.pending("node-c", "id-c").withStatus(RollingRestartNodeStatus.COMPLETED));
        final var data = baseData(RollingRestartState.SELECTING_NEXT_NODE).toBuilder().nodes(nodes).build();
        when(actions.electedManagerHostname()).thenReturn(Optional.empty());

        final var update = job.execute(ctxWith(data));

        assertThat(dataOf(update).smState()).isEqualTo(RollingRestartState.FINALIZING);
    }

    @Test
    void selectingNextNode_abortRequested_goesToAborted() throws Exception {
        final var data = baseData(RollingRestartState.SELECTING_NEXT_NODE)
                .toBuilder()
                .abortRequested(true)
                .build();

        final var update = job.execute(ctxWith(data));

        assertThat(dataOf(update).smState()).isEqualTo(RollingRestartState.ABORTED);
        assertThat(update.status()).contains(JobTriggerStatus.CANCELLED);
        assertThat(update.nextTime()).isEmpty();
    }

    @Test
    void waitingNodeJoined_advancesWhenNodePresent() throws Exception {
        when(actions.isNodeInCluster("node-a", "3.5.0")).thenReturn(true);
        final var data = baseData(RollingRestartState.WAITING_NODE_JOINED).toBuilder().currentNodeIndex(0).build();

        final var update = job.execute(ctxWith(data));

        final var newData = dataOf(update);
        assertThat(newData.smState()).isEqualTo(RollingRestartState.REENABLING_ALLOCATION);
        assertThat(newData.nodes().get(0).status()).isEqualTo(RollingRestartNodeStatus.STARTED);
    }

    @Test
    void reenablingAllocation_callsEnable_andResetsWaitingGreenSince() throws Exception {
        final var oldTimestamp = Instant.now().minus(Duration.ofHours(1));
        final var data = baseData(RollingRestartState.REENABLING_ALLOCATION)
                .toBuilder()
                .waitingGreenSince(oldTimestamp)
                .build();

        final var update = job.execute(ctxWith(data));

        verify(actions).enableAllocation();
        final var newData = dataOf(update);
        assertThat(newData.smState()).isEqualTo(RollingRestartState.WAITING_GREEN);
        // waitingGreenSince should be refreshed (more recent than the old timestamp)
        assertThat(newData.waitingGreenSince()).isAfter(oldTimestamp);
    }

    @Test
    void waitingGreen_advancesAndMarksNodeCompleted_whenGreen() throws Exception {
        when(actions.isClusterGreen()).thenReturn(true);
        final var data = baseData(RollingRestartState.WAITING_GREEN).toBuilder().currentNodeIndex(0).build();

        final var update = job.execute(ctxWith(data));

        final var newData = dataOf(update);
        assertThat(newData.smState()).isEqualTo(RollingRestartState.SELECTING_NEXT_NODE);
        assertThat(newData.nodes().get(0).status()).isEqualTo(RollingRestartNodeStatus.COMPLETED);
        assertThat(newData.nodes().get(0).finishedAt()).isNotNull();
    }

    @Test
    void waitingGreen_pausesOnTimeout() throws Exception {
        when(actions.isClusterGreen()).thenReturn(false);
        final var data = baseData(RollingRestartState.WAITING_GREEN)
                .toBuilder()
                .currentNodeIndex(0)
                .waitingGreenSince(Instant.now().minus(RollingRestartExecutionJob.GREEN_WAIT_TIMEOUT).minus(Duration.ofMinutes(1)))
                .build();

        final var update = job.execute(ctxWith(data));

        final var newData = dataOf(update);
        assertThat(newData.smState()).isEqualTo(RollingRestartState.PAUSED_WAITING_GREEN);
        assertThat(newData.pausedReason()).isNotBlank();
    }

    @Test
    void waitingGreen_staysWithinTimeout() throws Exception {
        when(actions.isClusterGreen()).thenReturn(false);
        final var data = baseData(RollingRestartState.WAITING_GREEN)
                .toBuilder()
                .currentNodeIndex(0)
                .waitingGreenSince(Instant.now())
                .build();

        final var update = job.execute(ctxWith(data));

        assertThat(dataOf(update).smState()).isEqualTo(RollingRestartState.WAITING_GREEN);
    }

    @Test
    void pausedWaitingGreen_doesNothing_untilResume() throws Exception {
        final var data = baseData(RollingRestartState.PAUSED_WAITING_GREEN);

        final var update = job.execute(ctxWith(data));

        // unchanged state, still ticking (we want operator to be able to /resume)
        assertThat(dataOf(update).smState()).isEqualTo(RollingRestartState.PAUSED_WAITING_GREEN);
        assertThat(update.nextTime()).isPresent();
    }

    @Test
    void finalizing_completesAndEmitsTerminalUpdate() throws Exception {
        when(actions.isAllocationEnabled()).thenReturn(true);
        final var update = job.execute(ctxWith(baseData(RollingRestartState.FINALIZING)));

        assertThat(dataOf(update).smState()).isEqualTo(RollingRestartState.COMPLETED);
        assertThat(update.nextTime()).isEmpty(); // null nextTime → framework marks COMPLETE
    }

    @Test
    void finalizing_recoversAllocation_ifSomehowDisabled() throws Exception {
        when(actions.isAllocationEnabled()).thenReturn(false);
        job.execute(ctxWith(baseData(RollingRestartState.FINALIZING)));
        verify(actions).enableAllocation();
    }

    // ====== error and edge cases ======

    @Test
    void execute_throwsWhenDataMissing() {
        final var trigger = JobTriggerDto.builder()
                .jobDefinitionType(RollingRestartExecutionJob.TYPE_NAME)
                .jobDefinitionId(RollingRestartExecutionJob.DEFINITION_INSTANCE.id())
                .schedule(OnceJobSchedule.create())
                .nextTime(DateTime.now(DateTimeZone.UTC))
                .build();
        final var ctx = JobExecutionContext.create(
                trigger,
                RollingRestartExecutionJob.DEFINITION_INSTANCE,
                jobTriggerUpdates,
                new AtomicBoolean(true),
                jobTriggerService);

        assertThatThrownBy(() -> job.execute(ctx)).isInstanceOf(JobExecutionException.class);
    }

    @Test
    void execute_marksFailed_andAttemptsAllocationCleanup_onActionThrow() throws Exception {
        org.mockito.Mockito.doThrow(new RuntimeException("simulated stop failure"))
                .when(actions).upgradeNode(any());

        final var data = baseData(RollingRestartState.UPGRADING_NODE).toBuilder().currentNodeIndex(0).build();
        final var update = job.execute(ctxWith(data));

        assertThat(dataOf(update).smState()).isEqualTo(RollingRestartState.FAILED);
        assertThat(dataOf(update).lastError()).contains("simulated stop failure");
        assertThat(update.status()).contains(JobTriggerStatus.ERROR);
        verify(actions).enableAllocation();
    }

    @Test
    void terminalSmState_emitsCompleteStatus_noNextTime() throws Exception {
        final var update = job.execute(ctxWith(baseData(RollingRestartState.COMPLETED)));

        verify(actions, never()).prepareCluster();
        verify(actions, never()).enableAllocation();
        assertThat(dataOf(update).smState()).isEqualTo(RollingRestartState.COMPLETED);
        assertThat(update.nextTime()).isEmpty();
        // No explicit status: framework infers COMPLETE when nextTime is null and no status set.
        assertThat(update.status()).isEmpty();
    }
}
