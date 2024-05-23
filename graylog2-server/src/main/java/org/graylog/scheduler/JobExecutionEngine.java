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
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Functions;
import com.google.inject.assistedinject.Assisted;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Inject;
import one.util.streamex.EntryStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.graylog.scheduler.eventbus.JobCompletedEvent;
import org.graylog.scheduler.eventbus.JobSchedulerEventBus;
import org.graylog.scheduler.worker.JobWorkerPool;
import org.graylog2.cluster.lock.AlreadyLockedException;
import org.graylog2.cluster.lock.RefreshingLockService;
import org.graylog2.shared.metrics.MetricUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.graylog.tracing.GraylogSemanticAttributes.SCHEDULER_JOB_CLASS;
import static org.graylog.tracing.GraylogSemanticAttributes.SCHEDULER_JOB_DEFINITION_ID;
import static org.graylog.tracing.GraylogSemanticAttributes.SCHEDULER_JOB_DEFINITION_TITLE;
import static org.graylog.tracing.GraylogSemanticAttributes.SCHEDULER_JOB_DEFINITION_TYPE;

/**
 * The job execution engine checks runnable triggers and starts job execution in the given worker pool.
 */
public class JobExecutionEngine {
    private static final long DEFAULT_BACKOFF = 5000L;


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
    private final RefreshingLockService.Factory refreshingLockServiceFactory;
    private final Map<String, Integer> concurrencyLimits;
    private final long backoffMillis;

    private final Counter executionSuccessful;
    private final Counter executionFailed;
    private final Meter executionDenied;
    private final Meter executionRescheduled;
    private final Timer executionTime;
    private final LoadingCache<String, Long> gaugeCache;

    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicBoolean shouldCleanup = new AtomicBoolean(true);

    @Inject
    public JobExecutionEngine(DBJobTriggerService jobTriggerService,
                              DBJobDefinitionService jobDefinitionService,
                              JobSchedulerEventBus eventBus,
                              JobScheduleStrategies scheduleStrategies,
                              JobTriggerUpdates.Factory jobTriggerUpdatesFactory,
                              RefreshingLockService.Factory refreshingLockServiceFactory,
                              Map<String, Job.Factory> jobFactory,
                              @Assisted JobWorkerPool workerPool,
                              JobSchedulerConfig schedulerConfig,
                              MetricRegistry metricRegistry) {
        this(jobTriggerService, jobDefinitionService, eventBus, scheduleStrategies, jobTriggerUpdatesFactory,
                refreshingLockServiceFactory, jobFactory, workerPool, schedulerConfig, metricRegistry, DEFAULT_BACKOFF);
    }

    @VisibleForTesting
    public JobExecutionEngine(DBJobTriggerService jobTriggerService,
                              DBJobDefinitionService jobDefinitionService,
                              JobSchedulerEventBus eventBus,
                              JobScheduleStrategies scheduleStrategies,
                              JobTriggerUpdates.Factory jobTriggerUpdatesFactory,
                              RefreshingLockService.Factory refreshingLockServiceFactory,
                              Map<String, Job.Factory> jobFactory,
                              JobWorkerPool workerPool,
                              JobSchedulerConfig schedulerConfig,
                              MetricRegistry metricRegistry,
                              long backoffMillis) {
        this.jobTriggerService = jobTriggerService;
        this.jobDefinitionService = jobDefinitionService;
        this.eventBus = eventBus;
        this.scheduleStrategies = scheduleStrategies;
        this.jobTriggerUpdatesFactory = jobTriggerUpdatesFactory;
        this.jobFactory = jobFactory;
        this.workerPool = workerPool;
        this.refreshingLockServiceFactory = refreshingLockServiceFactory;
        this.concurrencyLimits = schedulerConfig.concurrencyLimits();
        this.backoffMillis = backoffMillis;

        this.executionSuccessful = metricRegistry.counter(MetricRegistry.name(getClass(), "executions", "successful"));
        this.executionFailed = metricRegistry.counter(MetricRegistry.name(getClass(), "executions", "failed"));
        this.executionDenied = metricRegistry.meter(MetricRegistry.name(getClass(), "executions", "denied"));
        this.executionRescheduled = metricRegistry.meter(MetricRegistry.name(getClass(), "executions", "rescheduled"));
        this.executionTime = metricRegistry.timer(MetricRegistry.name(getClass(), "executions", "time"));

        // We use a cache to avoid having every gauge metric hitting the database.
        this.gaugeCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public @Nullable Long load(String key) {
                        throw new UnsupportedOperationException("Always use #loadAll");
                    }

                    @Override
                    public Map<String, Long> loadAll(Set<? extends String> keys) {
                        // Since the DBJobTriggerService#numberOfOverdueTriggers only returns counts for existing
                        // triggers, we initialize all known job definition types with zero to always get counts
                        // for all types.
                        return EntryStream.of(jobFactory.entrySet().stream())
                                .mapValues(Functions.constant(0L))
                                .append(jobTriggerService.numberOfOverdueTriggers())
                                .toMap((defaultValue, dbValue) -> dbValue);
                    }
                });

        // We always get the full set of job type names to populate the cache for the next gauge calls.
        jobFactory.keySet().forEach(jobType -> MetricUtils.safelyRegister(
                metricRegistry,
                MetricRegistry.name(getClass(), "executions", "overdue", "type", jobType),
                (Gauge<Long>) () -> gaugeCache.getAll(jobFactory.keySet()).get(jobType)
        ));
    }

    /**
     * Signal shutdown to the engine.
     */
    public void shutdown() {
        // TODO this should indicate that running jobs have been aborted
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

                if (!workerPool.execute(() -> handleTriggerWithConcurrencyLimit(trigger))) {
                    // The job couldn't be executed so we have to release the trigger again with the same nextTime
                    jobTriggerService.releaseTrigger(trigger, JobTriggerUpdate.withNextTime(trigger.nextTime()));
                    executionDenied.mark();
                    return false;
                }

                return true;
            }
        }
        executionDenied.mark();
        return false;
    }

    public void updateLockedJobs() {
        if (workerPool.anySlotsUsed()) {
            jobTriggerService.updateLockedJobTriggers();
        }
    }

    private void handleTriggerWithConcurrencyLimit(JobTriggerDto trigger) {
        final int maxTypeConcurrency = concurrencyLimits.getOrDefault(trigger.jobDefinitionType(), 0);
        if (maxTypeConcurrency > 0) {
            try (final RefreshingLockService refreshingLockService = refreshingLockServiceFactory.create()) {
                try {
                    refreshingLockService.acquireAndKeepLock(trigger.jobDefinitionType(), maxTypeConcurrency);
                    handleTrigger(trigger);
                } catch (AlreadyLockedException e) {
                    final DateTime nextTime = DateTime.now(DateTimeZone.UTC).plus(slidingBackoff(trigger));
                    jobTriggerService.releaseTrigger(trigger, JobTriggerUpdate.withConcurrencyReschedule(nextTime));
                    executionDenied.mark();
                    executionRescheduled.mark();
                }
            }
        } else {
            handleTrigger(trigger);
        }
    }

    /**
     * Progressively reduce backoff from 100% to 20% based on how many times the trigger was rescheduled.
     */
    private Duration slidingBackoff(JobTriggerDto trigger) {
        final long slidingBackoffMillis;
        if (trigger.concurrencyRescheduleCount() < 1) {
            slidingBackoffMillis = backoffMillis;
        } else {
            slidingBackoffMillis = backoffMillis / Math.min(trigger.concurrencyRescheduleCount(), 5);
        }
        return Duration.millis(slidingBackoffMillis);
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

    @WithSpan
    private void executeJob(JobTriggerDto trigger, JobDefinitionDto jobDefinition, Job job) {
        Span.current().setAttribute(SCHEDULER_JOB_CLASS, job.getClass().getSimpleName())
                .setAttribute(SCHEDULER_JOB_DEFINITION_TYPE, jobDefinition.config().type())
                .setAttribute(SCHEDULER_JOB_DEFINITION_TITLE, jobDefinition.title())
                .setAttribute(SCHEDULER_JOB_DEFINITION_ID, String.valueOf(jobDefinition.id()));
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Execute job: {}/{}/{} (job-class={} trigger={} config={})", jobDefinition.title(), jobDefinition.id(),
                        jobDefinition.config().type(), job.getClass().getSimpleName(), trigger.id(), jobDefinition.config());
            }
            final JobTriggerUpdate triggerUpdate = job.execute(JobExecutionContext.create(trigger, jobDefinition, jobTriggerUpdatesFactory.create(trigger), isRunning, jobTriggerService));

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
