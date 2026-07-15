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

import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.graylog.plugins.datanode.dto.ClusterState;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog2.cluster.lock.Lock;
import org.graylog2.cluster.lock.LockService;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeMetadataService;
import org.graylog2.indexer.indices.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Singleton
public class RollingRestartJobHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RollingRestartJobHandler.class);
    static final int MIN_DATANODES = 3;
    static final String START_LOCK_RESOURCE = "datanode-rolling-restart-start";

    private final DBJobDefinitionService jobDefinitionService;
    private final DBJobTriggerService jobTriggerService;
    private final RollingRestartActions actions;
    private final JobSchedulerClock clock;
    private final LockService lockService;
    private final DataNodeMetadataService dataNodeMetadataService;

    @Inject
    public RollingRestartJobHandler(DBJobDefinitionService jobDefinitionService,
                                    DBJobTriggerService jobTriggerService,
                                    RollingRestartActions actions,
                                    JobSchedulerClock clock,
                                    LockService lockService, DataNodeMetadataService dataNodeMetadataService) {
        this.jobDefinitionService = jobDefinitionService;
        this.jobTriggerService = jobTriggerService;
        this.actions = actions;
        this.clock = clock;
        this.lockService = lockService;
        this.dataNodeMetadataService = dataNodeMetadataService;
    }

    public JobTriggerDto start(String triggeredBy, boolean force) {
        // Cluster-wide lock around the find-active + create sequence so two concurrent /restart calls
        // (different Graylog nodes or the same node) cannot both pass preflight and insert duplicate triggers.
        final Lock lock = lockService.lock(START_LOCK_RESOURCE)
                .orElseThrow(() -> new IllegalStateException(
                        "Another rolling restart is being started right now — please retry shortly."));
        try {
            final List<String> failures = checkPreconditions(force);
            if (!failures.isEmpty()) {
                throw new RollingRestartPreconditionsException(failures);
            }

            final JobDefinitionDto definition = ensureDefinition();

            final List<RollingRestartNodeEntry> nodes = actions.liveDataNodes().stream()
                    .sorted(Comparator.comparing(DataNodeDto::getHostname))
                    .map(n -> RollingRestartNodeEntry.pending(n.getHostname(), n.getNodeId()))
                    .toList();

            final String targetOpensearchVersion = dataNodeMetadataService.getVersionsOverview()
                    .highestAvailableVersion()
                    .orElseThrow(() -> new IllegalStateException("There is no available opensearch version to upgrade"));

            final RollingRestartExecutionJob.Data data = RollingRestartExecutionJob.Data.builder()
                    .smState(RollingRestartState.PREPARING_CLUSTER)
                    .nodes(nodes)
                    .triggeredBy(triggeredBy)
                    .waitingGreenSince(Instant.now())
                    .targetOpensearchVersion(targetOpensearchVersion)
                    .build();

            final JobTriggerDto trigger = JobTriggerDto.builder()
                    .jobDefinitionType(RollingRestartExecutionJob.TYPE_NAME)
                    .jobDefinitionId(definition.id())
                    .data(data)
                    .schedule(OnceJobSchedule.create())
                    .nextTime(clock.nowUTC())
                    .build();

            final JobTriggerDto created = jobTriggerService.create(trigger);
            LOG.info("Started rolling restart trigger {} by {} ({} nodes)", created.id(), triggeredBy, nodes.size());
            return created;
        } finally {
            lockService.unlock(lock);
        }
    }

    public JobTriggerDto abort() {
        final JobTriggerDto trigger = findActive()
                .orElseThrow(() -> new IllegalStateException("No active rolling restart job to abort"));
        final RollingRestartExecutionJob.Data data = requireData(trigger);
        if (data.smState().isTerminal()) {
            throw new IllegalStateException("Job already in terminal SM state " + data.smState());
        }
        final RollingRestartExecutionJob.Data updated = data.toBuilder().abortRequested(true).build();
        LOG.info("Abort requested for rolling restart trigger {}", trigger.id());
        return persistData(trigger, updated);
    }

    public JobTriggerDto resume() {
        final JobTriggerDto trigger = findActive()
                .orElseThrow(() -> new IllegalStateException("No active rolling restart job to resume"));
        final RollingRestartExecutionJob.Data data = requireData(trigger);
        if (data.smState() != RollingRestartState.PAUSED_WAITING_GREEN) {
            throw new IllegalStateException("Job is not paused (sm_state=" + data.smState() + ")");
        }
        final RollingRestartExecutionJob.Data updated = data.toBuilder()
                .smState(RollingRestartState.WAITING_GREEN)
                .waitingGreenSince(Instant.now())
                .pausedReason(null)
                .build();
        LOG.info("Resuming rolling restart trigger {}", trigger.id());
        return persistData(trigger, updated);
    }

    public Optional<JobTriggerDto> current() {
        final Optional<JobTriggerDto> active = findActive();
        if (active.isPresent()) {
            return active;
        }
        return findLatestOfType(null);
    }

    public List<JobTriggerDto> history(int limit) {
        final List<JobTriggerDto> out = new ArrayList<>();
        try (final var stream = jobTriggerService.streamByQuery(typeFilter())) {
            stream.sorted(Comparator.comparing(JobTriggerDto::createdAt).reversed())
                    .limit(limit)
                    .forEach(out::add);
        }
        return out;
    }

    private JobDefinitionDto ensureDefinition() {
        return jobDefinitionService.get(RollingRestartExecutionJob.DEFINITION_INSTANCE.id())
                .orElseGet(() -> jobDefinitionService.save(RollingRestartExecutionJob.DEFINITION_INSTANCE));
    }

    private Optional<JobTriggerDto> findActive() {
        try (final var stream = jobTriggerService.streamByQuery(
                Filters.and(
                        typeFilter(),
                        Filters.nin(JobTriggerDto.FIELD_STATUS,
                                JobTriggerStatus.COMPLETE,
                                JobTriggerStatus.ERROR,
                                JobTriggerStatus.CANCELLED)))) {
            return stream.findFirst();
        }
    }

    private Optional<JobTriggerDto> findLatestOfType(Bson extra) {
        final Bson filter = extra == null ? typeFilter() : Filters.and(typeFilter(), extra);
        try (final var stream = jobTriggerService.streamByQuery(filter)) {
            return stream.max(Comparator.comparing(JobTriggerDto::createdAt));
        }
    }

    private static Bson typeFilter() {
        return Filters.eq(JobTriggerDto.FIELD_JOB_DEFINITION_TYPE, RollingRestartExecutionJob.TYPE_NAME);
    }

    private static RollingRestartExecutionJob.Data requireData(JobTriggerDto trigger) {
        return trigger.data()
                .filter(RollingRestartExecutionJob.Data.class::isInstance)
                .map(RollingRestartExecutionJob.Data.class::cast)
                .orElseThrow(() -> new IllegalStateException("Rolling restart trigger " + trigger.id() + " has no data"));
    }

    private JobTriggerDto persistData(JobTriggerDto trigger, RollingRestartExecutionJob.Data newData) {
        final JobTriggerDto updated = trigger.toBuilder().data(newData).build();
        jobTriggerService.update(updated);
        return updated;
    }

    private List<String> checkPreconditions(boolean force) {
        final List<String> failures = new ArrayList<>();
        if (findActive().isPresent()) {
            failures.add("Another rolling restart job is already active");
        }
        final List<DataNodeDto> dataNodes = actions.liveDataNodes();
        if (dataNodes.size() < MIN_DATANODES) {
            failures.add("Need at least " + MIN_DATANODES + " DataNodes for safe rolling restart (found " + dataNodes.size() + ")");
        }
        try {
            final ClusterState state = actions.getClusterState();
            if (state.status() != HealthStatus.Green && !force) {
                failures.add("Cluster status is " + state.status() + " — must be GREEN (pass force=true to override)");
            }
        } catch (Exception e) {
            failures.add("Unable to fetch cluster state: " + e.getMessage());
        }
        return failures;
    }
}
