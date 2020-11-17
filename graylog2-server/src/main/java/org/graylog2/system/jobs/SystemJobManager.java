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
package org.graylog2.system.jobs;

import com.codahale.metrics.InstrumentedScheduledExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.eaio.uuid.UUID;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

public class SystemJobManager {

    private static final Logger LOG = LoggerFactory.getLogger(SystemJobManager.class);
    private static final int THREAD_POOL_SIZE = 15;

    private final ActivityWriter activityWriter;
    private final ScheduledExecutorService executor;
    private final Map<String, SystemJob> jobs;

    @Inject
    public SystemJobManager(ActivityWriter activityWriter, MetricRegistry metricRegistry) {
        this.activityWriter = activityWriter;
        this.executor = executorService(metricRegistry);
        this.jobs = new ConcurrentHashMap<>();
    }

    private ScheduledExecutorService executorService(final MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("systemjob-executor-%d").build();
        return new InstrumentedScheduledExecutorService(
                Executors.newScheduledThreadPool(THREAD_POOL_SIZE, threadFactory),
                metricRegistry,
                name(this.getClass(), "executor-service"));
    }

    public String submit(final SystemJob job) throws SystemJobConcurrencyException {
        return submitWithDelay(job, 0, TimeUnit.SECONDS);
    }

    public String submitWithDelay(final SystemJob job, final long delay, TimeUnit timeUnit) throws SystemJobConcurrencyException {
        // for immediate jobs, check allowed concurrency right now
        if (delay == 0) {
            checkAllowedConcurrency(job);
        }

        final String jobClass = job.getClass().getCanonicalName();

        job.setId(new UUID().toString());
        jobs.put(job.getId(), job);

        executor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    if (delay > 0) {
                        checkAllowedConcurrency(job);
                    }
                    job.markStarted();

                    final Stopwatch x = Stopwatch.createStarted();

                    job.execute();  // ... blocks until it finishes.
                    x.stop();

                    final String msg = "SystemJob <" + job.getId() + "> [" + jobClass + "] finished in " + x.elapsed(
                        TimeUnit.MILLISECONDS) + "ms.";
                    LOG.info(msg);
                    activityWriter.write(new Activity(msg, SystemJobManager.class));
                } catch (SystemJobConcurrencyException ignored) {
                } catch (Exception e) {
                    LOG.error("Unhandled error while running SystemJob <" + job.getId() + "> [" + jobClass + "]", e);
                } finally {
                    jobs.remove(job.getId());
                }
            }
        }, delay, timeUnit);

        LOG.info("Submitted SystemJob <{}> [{}]", job.getId(), jobClass);
        return job.getId();
    }

    protected void checkAllowedConcurrency(SystemJob job) throws SystemJobConcurrencyException {
        final int concurrent = concurrentJobs(job.getClass());

        if (concurrent >= job.maxConcurrency()) {
            throw new SystemJobConcurrencyException("The maximum of parallel [" + job.getClass().getCanonicalName() + "] is locked " +
                                                            "to <" + job.maxConcurrency() + "> but <" + concurrent + "> are running.");
        }
    }

    public Map<String, SystemJob> getRunningJobs() {
        return jobs;
    }

    public int concurrentJobs(Class jobClass) {
        int concurrent = 0;

        for (final SystemJob job : jobs.values()) {
            if (job.getClass().equals(jobClass)) {
                concurrent += 1;
            }
        }

        return concurrent;
    }
}
