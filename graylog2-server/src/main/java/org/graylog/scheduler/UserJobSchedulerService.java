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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.eventbus.JobSchedulerEventBus;
import org.graylog.scheduler.worker.JobWorkerPool;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Singleton
public class UserJobSchedulerService extends JobSchedulerService {
    private static final Logger LOG = LoggerFactory.getLogger(UserJobSchedulerService.class);
    private static final String NAME = "user";

    @Inject
    public UserJobSchedulerService(JobExecutionEngine.Factory engineFactory,
                                   JobWorkerPool.Factory workerPoolFactory,
                                   JobSchedulerConfig schedulerConfig,
                                   JobSchedulerClock clock,
                                   JobSchedulerEventBus.Factory schedulerEventBusFactory,
                                   Map<String, Job.Factory<? extends Job>> jobFactories,
                                   DBJobDefinitionService jobDefinitionService,
                                   DBJobTriggerService jobTriggerService,
                                   ServerStatus serverStatus,
                                   GracefulShutdownService gracefulShutdownService,
                                   @Named("shutdown_timeout") int shutdownTimeoutMs,
                                   @Named(JobSchedulerConfiguration.LOOP_SLEEP_DURATION) Duration loopSleepDuration) {
        super(LOG,
                (workerPool) -> engineFactory.create(NAME, jobFactories, workerPool, jobDefinitionService::get, jobTriggerService),
                workerPoolFactory.create(NAME, schedulerConfig.numberOfWorkerThreads()),
                schedulerConfig::canExecute,
                clock,
                schedulerEventBusFactory.create(NAME),
                serverStatus,
                gracefulShutdownService,
                shutdownTimeoutMs,
                loopSleepDuration);
    }
}
