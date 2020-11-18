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
package org.graylog.scheduler;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.assistedinject.Assisted;
import org.graylog.scheduler.eventbus.JobCompletedEvent;
import org.graylog.scheduler.eventbus.JobSchedulerEventBus;
import org.graylog.scheduler.worker.JobWorkerPool;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The job execution engine checks runnable triggers and starts job execution in the given worker pool.
 */
public class JobExecutionEngine {


    public interface Factory {
        JobExecutionEngine create(JobWorkerPool workerPool);
    }

    private static final Logger LOG = LoggerFactory.getLogger(JobExecutionEngine.class);

    private final DBJobTriggerService jobTriggerService;
    private final DBJobDefinitionService jobDefinitionService;
    private final JobSchedulerEventBus eventBus;
    private final JobScheduleStrategies scheduleStrategies;
    private final JobTriggerUpdates.Factory jobTriggerUpdatesFactory;
    private final Map<String, Job.Factory> jobFactory;
    private final JobWorkerPool workerPool;
    private Counter executionSuccessful;
    private Counter executionFailed;
    private Timer executionTime;

    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicBoolean shouldCleanup = new AtomicBoolean(true);

    @Inject
    public JobExecutionEngine(DBJobTriggerService jobTriggerService,
                              DBJobDefinitionService jobDefinitionService,
                              JobSchedulerEventBus eventBus,
                              JobScheduleStrategies scheduleStrategies,
                              JobTriggerUpdates.Factory jobTriggerUpdatesFactory,
                              Map<String, Job.Factory> jobFactory,
                              @Assisted JobWorkerPool workerPool, MetricRegistry metricRegistry) {
        this.jobTriggerService = jobTriggerService;
        this.jobDefinitionService = jobDefinitionService;
        this.eventBus = eventBus;
        this.scheduleStrategies = scheduleStrategies;
        this.jobTriggerUpdatesFactory = jobTriggerUpdatesFactory;
        this.jobFactory = jobFactory;
        this.workerPool = workerPool;
        this.executionSuccessful = metricRegistry.counter(MetricRegistry.name(getClass(), "executions", "successful"));
        this.executionFailed = metricRegistry.counter(MetricRegistry.name(getClass(), "executions", "failed"));
        this.executionTime = metricRegistry.timer(MetricRegistry.name(getClass(), "executions", "time"));
    }

    /**
     * Signal shutdown to the engine.
     */
    public void shutdown() {
        isRunning.set(false);
    }

    private void cleanup() {
        // The cleanup should only run once before starting job processing to avoid damaging any state.
        if (shouldCleanup.getAndSet(false)) {
            final int releasedTriggers = jobTriggerService.forceReleaseOwnedTriggers();
            if (releasedTriggers > 0) {
                LOG.warn("Force-released {} stale job triggers after an unclean job scheduler shutdown", releasedTriggers);
            }
        }
    }

    /**
     * Execute the engine. This will try to lock a trigger and execute the job if there are free slots in the
     * worker pool and the engine is not shutting down.
     *
     * @return true if a job trigger has been locked and the related job has been triggered, false otherwise
     */
    public boolean execute() {
        // Cleanup stale scheduler state *before* processing any triggers for the first time.
        // This is a no-op after the first invocation.
        if (shouldCleanup.get()) {
            cleanup();
        }

        // We want to avoid a call to the database if there are no free slots in the pool or the engine is shutting down
        if (isRunning.get() && workerPool.hasFreeSlots()) {
            final Optional<JobTriggerDto> triggerOptional = jobTriggerService.nextRunnableTrigger();

            if (triggerOptional.isPresent()) {
                final JobTriggerDto trigger = triggerOptional.get();

                if (!workerPool.execute(() -> handleTrigger(trigger))) {
                    // The job couldn't be executed so we have to release the trigger again with the same nextTime
                    jobTriggerService.releaseTrigger(trigger, JobTriggerUpdate.withNextTime(trigger.nextTime()));
                    return false;
                }

                return true;
            }
        }

        return false;
    }

    private void handleTrigger(JobTriggerDto trigger) {
        LOG.trace("Locked trigger {} (owner={})", trigger.id(), trigger.lock().owner());

        try {
            final JobDefinitionDto jobDefinition = jobDefinitionService.get(trigger.jobDefinitionId())
                    .orElseThrow(() -> new IllegalStateException("Couldn't find job definition " + trigger.jobDefinitionId()));

            final Job job = jobFactory.get(jobDefinition.config().type()).create(jobDefinition);
            if (job == null) {
                throw new IllegalStateException("Couldn't find job factory for type " + jobDefinition.config().type());
            }

            executionTime.time(() -> executeJob(trigger, jobDefinition, job));
        } catch (IllegalStateException e) {
            // The trigger cannot be handled because of a permanent error so we mark the trigger as defective
            LOG.error("Couldn't handle trigger due to a permanent error {} - trigger won't be retried", trigger.id(), e);
            jobTriggerService.setTriggerError(trigger);
        } catch (Exception e) {
            // The trigger cannot be handled because of an unknown error, retry in a few seconds
            // TODO: Check if we need to implement a max-retry after which the trigger is set to ERROR
            final DateTime nextTime = DateTime.now(DateTimeZone.UTC).plusSeconds(5);
            LOG.error("Couldn't handle trigger {} - retrying at {}", trigger.id(), nextTime, e);
            jobTriggerService.releaseTrigger(trigger, JobTriggerUpdate.withNextTime(nextTime));
        } finally {
            eventBus.post(JobCompletedEvent.INSTANCE);
        }
    }

    private void executeJob(JobTriggerDto trigger, JobDefinitionDto jobDefinition, Job job) {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Execute job: {}/{}/{} (job-class={} trigger={} config={})", jobDefinition.title(), jobDefinition.id(),
                        jobDefinition.config().type(), job.getClass().getSimpleName(), trigger.id(), jobDefinition.config());
            }
            final JobTriggerUpdate triggerUpdate = job.execute(JobExecutionContext.create(trigger, jobDefinition, jobTriggerUpdatesFactory.create(trigger), isRunning));

            if (triggerUpdate == null) {
                executionFailed.inc();
                throw new IllegalStateException("Job#execute() must not return null - this is a bug in the job class");
            }
            executionSuccessful.inc();

            LOG.trace("Update trigger: trigger={} update={}", trigger.id(), triggerUpdate);
            jobTriggerService.releaseTrigger(trigger, triggerUpdate);
        } catch (JobExecutionException e) {
            LOG.error("Job execution error - trigger={} job={}", trigger.id(), jobDefinition.id(), e);
            executionFailed.inc();

            jobTriggerService.releaseTrigger(e.getTrigger(), e.getUpdate());
        } catch (Exception e) {
            executionFailed.inc();
            // This is an unhandled job execution error so we mark the trigger as defective
            LOG.error("Unhandled job execution error - trigger={} job={}", trigger.id(), jobDefinition.id(), e);

            // Calculate the next time in the future based on the trigger schedule. We cannot do much else because we
            // don't know what happened and we also got no instructions from the job. (no JobExecutionException)
            final DateTime nextFutureTime = scheduleStrategies.nextFutureTime(trigger).orElse(null);

            jobTriggerService.releaseTrigger(trigger, JobTriggerUpdate.withNextTime(nextFutureTime));
        }
    }
}
