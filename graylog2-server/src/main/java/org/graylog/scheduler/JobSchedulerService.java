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

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.eventbus.JobCompletedEvent;
import org.graylog.scheduler.eventbus.JobSchedulerEventBus;
import org.graylog.scheduler.worker.JobWorkerPool;
import org.graylog2.plugin.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Singleton
public class JobSchedulerService extends AbstractExecutionThreadService {
    private static final Logger LOG = LoggerFactory.getLogger(JobSchedulerService.class);

    private final JobExecutionEngine jobExecutionEngine;
    private final JobSchedulerConfig schedulerConfig;
    private final JobSchedulerClock clock;
    private final JobSchedulerEventBus schedulerEventBus;
    private final ServerStatus serverStatus;
    private final JobWorkerPool workerPool;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private final Duration loopSleepDuration;
    private final InterruptibleSleeper sleeper = new InterruptibleSleeper();

    @Inject
    public JobSchedulerService(JobExecutionEngine.Factory engineFactory,
                               JobWorkerPool.Factory workerPoolFactory,
                               JobSchedulerConfig schedulerConfig,
                               JobSchedulerClock clock,
                               JobSchedulerEventBus schedulerEventBus,
                               ServerStatus serverStatus,
                               @Named(JobSchedulerConfiguration.LOOP_SLEEP_DURATION) Duration loopSleepDuration) {
        this.workerPool = workerPoolFactory.create("system", schedulerConfig.numberOfWorkerThreads());
        this.jobExecutionEngine = engineFactory.create(workerPool);
        this.schedulerConfig = schedulerConfig;
        this.clock = clock;
        this.schedulerEventBus = schedulerEventBus;
        this.serverStatus = serverStatus;
        this.loopSleepDuration = loopSleepDuration;
    }

    @Override
    protected void startUp() throws Exception {
        schedulerEventBus.register(this);
    }

    @Override
    protected void run() throws Exception {
        // Safety measure to make sure everything is started before we start job scheduling.
        LOG.debug("Waiting for server to enter RUNNING status before starting the scheduler loop");
        serverStatus.awaitRunning(() -> LOG.debug("Server entered RUNNING state, starting scheduler loop"));


        if (schedulerConfig.canStart()) {
            boolean executionEnabled = true;
            while (isRunning()) {
                if (!schedulerConfig.canExecute()) {
                    executionEnabled = logExecutionConfigState(executionEnabled, false);
                    clock.sleepUninterruptibly(1, TimeUnit.SECONDS);
                    continue;
                }
                executionEnabled = logExecutionConfigState(executionEnabled, true);

                LOG.debug("Starting scheduler loop iteration");
                try {
                    if (!jobExecutionEngine.execute() && isRunning()) {
                        // When the execution engine returned false, there are either no free worker threads or no
                        // runnable triggers. To avoid busy spinning we sleep for the configured duration or until
                        // we receive a job completion event via the scheduler event bus.
                        if (sleeper.sleep(loopSleepDuration.getQuantity(), loopSleepDuration.getUnit())) {
                            LOG.debug("Waited for {} {} because there are either no free worker threads or no runnable triggers",
                                loopSleepDuration.getQuantity(), loopSleepDuration.getUnit());
                        }
                    }
                } catch (InterruptedException e) {
                    LOG.debug("Received interrupted exception", e);
                } catch (Exception e) {
                    LOG.error("Error running job execution engine", e);
                }
                LOG.debug("Ending scheduler loop iteration");
            }
        } else {
            LOG.debug("Scheduler cannot run on this node, waiting for shutdown");
            shutdownLatch.await();
        }
    }

    @Subscribe
    public void handleJobCompleted(JobCompletedEvent triggerCompletedEvent) {
        // The job execution engine has just completed a job so we want to check for runnable triggers immediately.
        sleeper.interrupt();
    }

    @Override
    protected void triggerShutdown() {
        // We don't want to process events when shutting down, so do this first
        schedulerEventBus.unregister(this);
        shutdownLatch.countDown();
        jobExecutionEngine.shutdown();
    }

    /**
     * This class provides a sleep method that can be interrupted without interrupting threads.
     * The same could be achieved by using a {@link CountDownLatch} but that one cannot be reused and we would need
     * to create new latch objects all the time. This implementation is using a {@link Semaphore} internally which
     * can be reused.
     */
    @VisibleForTesting
    static class InterruptibleSleeper {

        private final Semaphore semaphore;

        InterruptibleSleeper() {
            this(new Semaphore(1));
        }

        @VisibleForTesting
        InterruptibleSleeper(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        /**
         * Blocks for the given duration or until interrupted via {@link #interrupt()}.
         *
         * @param duration the duration to sleep
         * @param unit     the duration unit
         * @return true if slept for the given duration, false if interrupted
         * @throws InterruptedException if the thread gets interrupted
         */
        public boolean sleep(long duration, TimeUnit unit) throws InterruptedException {
            // First we have to drain all available permits because interrupt() might get called very often and thus
            // there might be a lot of permits.
            semaphore.drainPermits();
            // Now try to acquire a permit. This won't work except #interrupt() got called in the meantime.
            // It waits for the given duration, basically emulating a sleep.
            return !semaphore.tryAcquire(duration, unit);
        }

        /**
         * Interrupt a {@link #sleep(long, TimeUnit)} call so it unblocks.
         */
        public void interrupt() {
            // Attention: this will increase available permits every time it's called.
            semaphore.release();
        }
    }

    /**
     * {@link JobSchedulerConfig#canExecute()} may return false for a lot of consecutive calls. We don't want to log
     * this on every scheduler loop iteration but only when there is a change in state .
     */
    private boolean logExecutionConfigState(boolean previouslyEnabled, boolean nowEnabled) {
        if (previouslyEnabled && !nowEnabled) {
            LOG.info("Job scheduler execution is disabled. Waiting and trying again until enabled.");
        } else if (!previouslyEnabled && nowEnabled) {
            LOG.info("Job scheduler execution is now enabled. Proceeding.");
        }
        return nowEnabled;
    }
}
