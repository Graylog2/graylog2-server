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
package org.graylog.scheduler.system;

import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.scheduler.DBSystemJobTriggerService;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.system.SystemJobSummary;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

@Singleton
public class SystemJobManager {
    private final DBSystemJobTriggerService triggerService;
    private final JobSchedulerClock clock;

    @Inject
    public SystemJobManager(DBSystemJobTriggerService triggerService,
                            JobSchedulerClock clock) {
        this.triggerService = triggerService;
        this.clock = clock;
    }

    public SystemJobSubmitResult submit(SystemJobConfig config) {
        return submitWithConstraints(config, Set.of());
    }

    /**
     * Submits a system job that only nodes providing all given scheduler capabilities may execute.
     *
     * @param config      the job configuration
     * @param constraints the scheduler capabilities a node must provide to execute the job
     * @return the result of the submission
     */
    public SystemJobSubmitResult submitWithConstraints(SystemJobConfig config, Set<String> constraints) {
        return submitWithDelayAndConstraints(config, Duration.ZERO, constraints);
    }

    public SystemJobSubmitResult submitWithDelay(SystemJobConfig config, Duration delay) {
        return submitWithDelayAndConstraints(config, delay, Set.of());
    }

    public SystemJobSubmitResult submitWithDelayAndConstraints(SystemJobConfig config, Duration delay, Set<String> constraints) {
        final var now = clock.nowUTC();
        final var startTime = now.plus(delay.toMillis());
        final var trigger = JobTriggerDto.builderWithClock(clock)
                .jobDefinitionType(SystemJobDefinitionConfig.TYPE_NAME)
                .jobDefinitionId(config.type())
                .constraints(constraints)
                .data(config)
                .startTime(startTime)
                .nextTime(startTime)
                .schedule(OnceJobSchedule.create())
                .build();

        final var created = triggerService.create(trigger);

        return new SystemJobSubmitResult(requireNonNull(created.id(), "Created job trigger must have an ID"));
    }

    public List<SystemJobConfig> getRunningJobConfigs(String type) {
        return getJobConfigs(type, JobTriggerStatus.RUNNING);
    }

    /**
     * Returns the configurations of all queued, running and paused jobs of the given type.
     */
    public List<SystemJobConfig> getActiveJobConfigs(String type) {
        return getJobConfigs(type, JobTriggerStatus.RUNNABLE, JobTriggerStatus.RUNNING, JobTriggerStatus.PAUSED);
    }

    private List<SystemJobConfig> getJobConfigs(String type, JobTriggerStatus... statuses) {
        final var query = Filters.and(
                // The trigger's job definition ID is the type name for system jobs
                Filters.eq(JobTriggerDto.FIELD_JOB_DEFINITION_ID, type),
                Filters.in(JobTriggerDto.FIELD_STATUS, statuses)
        );

        try (var stream = triggerService.streamByQuery(query)) {
            return stream.map(JobTriggerDto::data)
                    .flatMap(Optional::stream)
                    .map(SystemJobConfig.class::cast)
                    .toList();
        }
    }

    public Map<String, SystemJobSummary> getRunningJobs() {
        return getJobsByQuery(Filters.eq(JobTriggerDto.FIELD_STATUS, JobTriggerStatus.RUNNING));
    }

    public Map<String, SystemJobSummary> getRunningJobs(NodeId nodeId) {
        return getJobsByQuery(
                Filters.and(
                        Filters.eq(JobTriggerDto.FIELD_LOCK_OWNER, nodeId.getNodeId()),
                        Filters.eq(JobTriggerDto.FIELD_STATUS, JobTriggerStatus.RUNNING)
                )
        );
    }

    public Optional<SystemJobSummary> getRunningJob(String id) {
        // A trigger ID is a Mongo ObjectId; a non-ObjectId id can't match a running job, so treat it as not found.
        if (!ObjectId.isValid(requireNonBlank(id, "id can't be blank"))) {
            return Optional.empty();
        }
        return triggerService.get(id)
                .filter(trigger -> trigger.status() == JobTriggerStatus.RUNNING)
                .map(this::toSystemJobInfo);
    }

    /**
     * Requests cancellation of the system job with the given trigger ID by setting the trigger's cancel flag. The
     * running job stops at its next check of {@link SystemJobContext#isCancelled()}. A non-ObjectId id, or an id with
     * no matching trigger, is a no-op; a blank id is rejected. Callers are responsible for permission checks and for
     * verifying that the job is cancelable (see {@link SystemJobSummary#isCancelable()}).
     */
    public void cancel(String id) {
        // A trigger ID is a Mongo ObjectId; a non-ObjectId id can't match a job, so there's nothing to cancel.
        if (!ObjectId.isValid(requireNonBlank(id, "id can't be blank"))) {
            return;
        }
        triggerService.cancelTriggerByQuery(idEq(id));
    }

    private Map<String, SystemJobSummary> getJobsByQuery(Bson query) {
        try (var stream = triggerService.streamByQuery(query)) {
            return stream.map(this::toSystemJobInfo)
                    .collect(Collectors.toMap(SystemJobSummary::id, Function.identity()));
        }
    }

    private SystemJobSummary toSystemJobInfo(JobTriggerDto trigger) {
        final var data = trigger.data()
                .map(SystemJobConfig.class::cast)
                // System jobs must always have data.
                .orElseThrow(() -> new IllegalStateException("System job trigger " + trigger.id() + " has no associated config"));
        final var info = data.toInfo();

        return SystemJobSummary.create(
                trigger.id(),
                info.description(),
                trigger.jobDefinitionId(), // The job definition ID is the type name for system jobs
                info.statusInfo(),
                trigger.lock().owner(),
                trigger.startTime(),
                Duration.between(Instant.ofEpochMilli(trigger.startTime().getMillis()), clock.instantNow()),
                trigger.lock().progress(),
                info.isCancelable(),
                info.reportsProgress(),
                trigger.status()
        );
    }
}
