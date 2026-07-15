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

import org.bson.conversions.Bson;
import org.graylog.plugins.datanode.dto.ClusterState;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog2.cluster.lock.Lock;
import org.graylog2.cluster.lock.LockService;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeMetadataService;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.OpensearchVersionsOverview;
import org.graylog2.indexer.indices.HealthStatus;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RollingRestartJobHandlerTest {

    @Mock
    DBJobDefinitionService jobDefinitionService;
    @Mock
    DBJobTriggerService jobTriggerService;
    @Mock
    RollingRestartActions actions;
    @Mock
    JobSchedulerClock clock;
    @Mock
    LockService lockService;
    @Mock
    Lock lock;
    @Mock
    ClusterState clusterState;
    @Mock
    DataNodeMetadataService dataNodeMetadataService;

    private RollingRestartJobHandler handler;

    @BeforeEach
    void setUp() {
        OpensearchVersionsOverview versionOverview = mock(OpensearchVersionsOverview.class);
        lenient().when(versionOverview.highestAvailableVersion()).thenReturn(Optional.of("999.999.999"));
        lenient().when(dataNodeMetadataService.getVersionsOverview()).thenReturn(versionOverview);
        handler = new RollingRestartJobHandler(jobDefinitionService, jobTriggerService, actions, clock, lockService, dataNodeMetadataService);
        // Default: lock acquisition succeeds for most tests; tests that exercise contention override.
        lenient().when(lockService.lock(RollingRestartJobHandler.START_LOCK_RESOURCE)).thenReturn(Optional.of(lock));
        lenient().when(clock.nowUTC()).thenReturn(DateTime.now(DateTimeZone.UTC));
    }

    private static DataNodeDto node(String id, String hostname) {
        return DataNodeDto.Builder.builder()
                .setId(id)
                .setHostname(hostname)
                .setClusterAddress("http://" + hostname + ":9300")
                .setTransportAddress("http://" + hostname + ":9200")
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .build();
    }

    private static JobTriggerDto buildExistingTrigger(RollingRestartExecutionJob.Data data) {
        return JobTriggerDto.builder()
                .id("000000000000000000000001")
                .jobDefinitionType(RollingRestartExecutionJob.TYPE_NAME)
                .jobDefinitionId(RollingRestartExecutionJob.DEFINITION_INSTANCE.id())
                .schedule(OnceJobSchedule.create())
                .nextTime(DateTime.now(DateTimeZone.UTC))
                .data(data)
                .build();
    }

    private void mockNoActiveTrigger() {
        when(jobTriggerService.streamByQuery(any(Bson.class))).thenReturn(Stream.empty());
    }

    private void mockActiveTrigger(JobTriggerDto trigger) {
        when(jobTriggerService.streamByQuery(any(Bson.class))).thenReturn(Stream.of(trigger));
    }

    private void greenClusterWithThreeNodes() {
        when(actions.liveDataNodes()).thenReturn(List.of(node("a", "node-a"), node("b", "node-b"), node("c", "node-c")));
        when(actions.getClusterState()).thenReturn(clusterState);
        when(clusterState.status()).thenReturn(HealthStatus.Green);
    }

    // ====== start() preconditions ======

    @Test
    void start_failsWhenLockNotAcquired() {
        when(lockService.lock(RollingRestartJobHandler.START_LOCK_RESOURCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.start("alice", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Another rolling restart is being started");

        verify(jobTriggerService, never()).create(any());
    }

    @Test
    void start_failsWhenActiveJobExists() {
        greenClusterWithThreeNodes();
        final var existing = buildExistingTrigger(
                RollingRestartExecutionJob.Data.builder()
                        .smState(RollingRestartState.UPGRADING_NODE)
                        .nodes(List.of())
                        .triggeredBy("bob")
                        .waitingGreenSince(java.time.Instant.now())
                        .targetOpensearchVersion("any")
                        .build());
        mockActiveTrigger(existing);

        assertThatThrownBy(() -> handler.start("alice", false))
                .isInstanceOf(RollingRestartPreconditionsException.class)
                .extracting("failedChecks").asList()
                .anyMatch(c -> ((String) c).contains("already active"));
        verify(lockService).unlock(lock);
    }

    @Test
    void start_failsWhenFewerThanThreeNodes() {
        mockNoActiveTrigger();
        when(actions.liveDataNodes()).thenReturn(List.of(node("a", "node-a"), node("b", "node-b")));
        when(actions.getClusterState()).thenReturn(clusterState);
        when(clusterState.status()).thenReturn(HealthStatus.Green);

        assertThatThrownBy(() -> handler.start("alice", false))
                .isInstanceOf(RollingRestartPreconditionsException.class)
                .extracting("failedChecks").asList()
                .anyMatch(c -> ((String) c).contains("at least 3 DataNodes"));
    }

    @Test
    void start_failsWhenClusterYellow_andNotForced() {
        mockNoActiveTrigger();
        when(actions.liveDataNodes()).thenReturn(List.of(node("a", "node-a"), node("b", "node-b"), node("c", "node-c")));
        when(actions.getClusterState()).thenReturn(clusterState);
        when(clusterState.status()).thenReturn(HealthStatus.Yellow);

        assertThatThrownBy(() -> handler.start("alice", false))
                .isInstanceOf(RollingRestartPreconditionsException.class)
                .extracting("failedChecks").asList()
                .anyMatch(c -> ((String) c).contains("must be GREEN"));
    }

    @Test
    void start_passesYellowWhenForced() {
        // 4 invocations of streamByQuery for findActive(): preconditions-active-check;
        // the trigger is created on the same path so only one stream issued.
        when(jobTriggerService.streamByQuery(any(Bson.class))).thenReturn(Stream.empty());
        when(actions.liveDataNodes()).thenReturn(List.of(node("a", "node-a"), node("b", "node-b"), node("c", "node-c")));
        when(actions.getClusterState()).thenReturn(clusterState);
        when(clusterState.status()).thenReturn(HealthStatus.Yellow);
        when(jobDefinitionService.get(RollingRestartExecutionJob.DEFINITION_INSTANCE.id()))
                .thenReturn(Optional.of(RollingRestartExecutionJob.DEFINITION_INSTANCE));
        when(jobTriggerService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        final var created = handler.start("alice", /* force */ true);

        assertThat(created).isNotNull();
        final var data = (RollingRestartExecutionJob.Data) created.data().orElseThrow();
        assertThat(data.smState()).isEqualTo(RollingRestartState.PREPARING_CLUSTER);
        assertThat(data.triggeredBy()).isEqualTo("alice");
        assertThat(data.nodes()).hasSize(3);
    }

    @Test
    void start_createsTrigger_andReleasesLock_onSuccess() {
        mockNoActiveTrigger();
        greenClusterWithThreeNodes();
        when(jobDefinitionService.get(RollingRestartExecutionJob.DEFINITION_INSTANCE.id()))
                .thenReturn(Optional.of(RollingRestartExecutionJob.DEFINITION_INSTANCE));
        when(jobTriggerService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        final var created = handler.start("alice", false);

        assertThat(created).isNotNull();
        verify(jobTriggerService).create(any());
        verify(lockService).unlock(lock);
    }

    @Test
    void start_releasesLock_onPreconditionFailure() {
        when(jobTriggerService.streamByQuery(any(Bson.class))).thenReturn(Stream.empty());
        when(actions.liveDataNodes()).thenReturn(List.of(node("a", "node-a"))); // too few

        assertThatThrownBy(() -> handler.start("alice", false))
                .isInstanceOf(RollingRestartPreconditionsException.class);

        verify(lockService).unlock(lock);
    }

    @Test
    void start_lazyCreatesJobDefinition_whenAbsent() {
        mockNoActiveTrigger();
        greenClusterWithThreeNodes();
        when(jobDefinitionService.get(RollingRestartExecutionJob.DEFINITION_INSTANCE.id())).thenReturn(Optional.empty());
        when(jobDefinitionService.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobTriggerService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        handler.start("alice", false);

        verify(jobDefinitionService).save(eq(RollingRestartExecutionJob.DEFINITION_INSTANCE));
    }

    // ====== abort() ======

    @Test
    void abort_setsAbortRequestedAndUpdatesTrigger() {
        final var existing = buildExistingTrigger(
                RollingRestartExecutionJob.Data.builder()
                        .smState(RollingRestartState.WAITING_GREEN)
                        .nodes(List.of())
                        .triggeredBy("alice")
                        .waitingGreenSince(java.time.Instant.now())
                        .targetOpensearchVersion("any")
                        .build());
        mockActiveTrigger(existing);

        handler.abort();

        final var captor = ArgumentCaptor.forClass(JobTriggerDto.class);
        verify(jobTriggerService).update(captor.capture());
        final var updatedData = (RollingRestartExecutionJob.Data) captor.getValue().data().orElseThrow();
        assertThat(updatedData.abortRequested()).isTrue();
    }

    @Test
    void abort_throwsWhenNoActiveJob() {
        mockNoActiveTrigger();
        assertThatThrownBy(() -> handler.abort()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void abort_throwsWhenJobAlreadyTerminal() {
        final var existing = buildExistingTrigger(
                RollingRestartExecutionJob.Data.builder()
                        .smState(RollingRestartState.COMPLETED)
                        .nodes(List.of())
                        .triggeredBy("alice")
                        .waitingGreenSince(java.time.Instant.now())
                        .targetOpensearchVersion("any")
                        .build());
        mockActiveTrigger(existing);

        assertThatThrownBy(() -> handler.abort()).isInstanceOf(IllegalStateException.class);
        verify(jobTriggerService, never()).update(any());
    }

    // ====== resume() ======

    @Test
    void resume_transitionsOutOfPausedAndResetsTimer() {
        final var oldTs = java.time.Instant.parse("2025-01-01T00:00:00Z");
        final var existing = buildExistingTrigger(
                RollingRestartExecutionJob.Data.builder()
                        .smState(RollingRestartState.PAUSED_WAITING_GREEN)
                        .nodes(List.of())
                        .triggeredBy("alice")
                        .waitingGreenSince(oldTs)
                        .pausedReason("stuck")
                        .targetOpensearchVersion("any")
                        .build());
        mockActiveTrigger(existing);

        handler.resume();

        final var captor = ArgumentCaptor.forClass(JobTriggerDto.class);
        verify(jobTriggerService).update(captor.capture());
        final var data = (RollingRestartExecutionJob.Data) captor.getValue().data().orElseThrow();
        assertThat(data.smState()).isEqualTo(RollingRestartState.WAITING_GREEN);
        assertThat(data.waitingGreenSince()).isAfter(oldTs);
        assertThat(data.pausedReason()).isNull();
    }

    @Test
    void resume_throwsWhenNotPaused() {
        final var existing = buildExistingTrigger(
                RollingRestartExecutionJob.Data.builder()
                        .smState(RollingRestartState.WAITING_GREEN)
                        .nodes(List.of())
                        .triggeredBy("alice")
                        .waitingGreenSince(java.time.Instant.now())
                        .targetOpensearchVersion("any")
                        .build());
        mockActiveTrigger(existing);

        assertThatThrownBy(() -> handler.resume()).isInstanceOf(IllegalStateException.class);
        verify(jobTriggerService, never()).update(any());
    }

    @Test
    void resume_throwsWhenNoActiveJob() {
        mockNoActiveTrigger();
        assertThatThrownBy(() -> handler.resume()).isInstanceOf(IllegalStateException.class);
    }

    // ====== current() ======

    @Test
    void current_returnsActiveTrigger_whenPresent() {
        final var existing = buildExistingTrigger(
                RollingRestartExecutionJob.Data.builder()
                        .smState(RollingRestartState.WAITING_GREEN)
                        .nodes(List.of())
                        .triggeredBy("alice")
                        .waitingGreenSince(java.time.Instant.now())
                        .targetOpensearchVersion("any")
                        .build());
        // First streamByQuery (findActive) returns the trigger. We don't need any second query.
        when(jobTriggerService.streamByQuery(any(Bson.class))).thenReturn(Stream.of(existing));

        assertThat(handler.current()).contains(existing);
    }

    @Test
    void current_fallsBackToLatestOfType_whenNoActive() {
        final var latestCompleted = buildExistingTrigger(
                RollingRestartExecutionJob.Data.builder()
                        .smState(RollingRestartState.COMPLETED)
                        .nodes(List.of())
                        .triggeredBy("alice")
                        .waitingGreenSince(java.time.Instant.now())
                        .targetOpensearchVersion("any")
                        .build());
        when(jobTriggerService.streamByQuery(any(Bson.class)))
                .thenReturn(Stream.empty())     // findActive — none
                .thenReturn(Stream.of(latestCompleted)); // findLatestOfType — return previous one

        assertThat(handler.current()).contains(latestCompleted);
        verify(jobTriggerService, times(2)).streamByQuery(any(Bson.class));
    }

    // ====== history() ======

    @Test
    void history_returnsTriggersOrderedNewestFirst() {
        final var older = buildExistingTrigger(
                RollingRestartExecutionJob.Data.builder()
                        .smState(RollingRestartState.COMPLETED)
                        .nodes(List.of())
                        .triggeredBy("alice")
                        .waitingGreenSince(java.time.Instant.now())
                        .targetOpensearchVersion("any")
                        .build())
                .toBuilder()
                .createdAt(DateTime.now(DateTimeZone.UTC).minusDays(1))
                .build();
        final var newer = buildExistingTrigger(
                RollingRestartExecutionJob.Data.builder()
                        .smState(RollingRestartState.COMPLETED)
                        .nodes(List.of())
                        .triggeredBy("bob")
                        .waitingGreenSince(java.time.Instant.now())
                        .targetOpensearchVersion("any")
                        .build())
                .toBuilder()
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .build();
        when(jobTriggerService.streamByQuery(any(Bson.class))).thenReturn(Stream.of(older, newer));

        final var result = handler.history(10);

        assertThat(result).containsExactly(newer, older);
    }

    // ====== smoke: trigger status carries through ======

    @Test
    void start_buildsTriggerWithCorrectTypeAndSchedule() {
        mockNoActiveTrigger();
        greenClusterWithThreeNodes();
        when(jobDefinitionService.get(RollingRestartExecutionJob.DEFINITION_INSTANCE.id()))
                .thenReturn(Optional.of(RollingRestartExecutionJob.DEFINITION_INSTANCE));
        final var captor = ArgumentCaptor.forClass(JobTriggerDto.class);
        when(jobTriggerService.create(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        handler.start("alice", false);

        final JobTriggerDto trigger = captor.getValue();
        assertThat(trigger.jobDefinitionType()).isEqualTo(RollingRestartExecutionJob.TYPE_NAME);
        assertThat(trigger.jobDefinitionId()).isEqualTo(RollingRestartExecutionJob.DEFINITION_INSTANCE.id());
        assertThat(trigger.schedule()).isInstanceOf(OnceJobSchedule.class);
        assertThat(trigger.status()).isEqualTo(JobTriggerStatus.RUNNABLE);
    }
}
