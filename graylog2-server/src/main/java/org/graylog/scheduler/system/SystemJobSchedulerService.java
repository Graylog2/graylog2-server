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

import com.github.joschi.jadconfig.util.Duration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.graylog.scheduler.DBSystemJobTriggerService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobExecutionEngine;
import org.graylog.scheduler.JobSchedulerConfig;
import org.graylog.scheduler.JobSchedulerConfiguration;
import org.graylog.scheduler.JobSchedulerService;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.eventbus.JobSchedulerEventBus;
import org.graylog.scheduler.worker.JobWorkerPool;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class SystemJobSchedulerService extends JobSchedulerService {
    private static final Logger LOG = LoggerFactory.getLogger(SystemJobSchedulerService.class);
    private static final String NAME = "system";

    @Inject
    public SystemJobSchedulerService(JobExecutionEngine.Factory engineFactory,
                                     JobWorkerPool.Factory workerPoolFactory,
                                     JobSchedulerConfig schedulerConfig,
                                     JobSchedulerClock clock,
                                     JobSchedulerEventBus.Factory schedulerEventBusFactory,
                                     SystemJobFactories systemJobFactories,
                                     DBSystemJobTriggerService systemJobTriggerService,
                                     ServerStatus serverStatus,
                                     GracefulShutdownService gracefulShutdownService,
                                     @Named("shutdown_timeout") int shutdownTimeoutMs,
                                     @Named(JobSchedulerConfiguration.LOOP_SLEEP_DURATION) Duration loopSleepDuration,
                                     @Named(JobSchedulerConfiguration.SYSTEM_WORKER_THREADS) int systemWorkerThreads) {
        super(LOG,
                (workerPool) -> engineFactory.create(
                        NAME,
                        systemJobFactories.getJobFactories(),
                        workerPool,
                        SystemJobSchedulerService::createJobDefinitionLookup,
                        systemJobTriggerService // MUST be the system job trigger service!
                ),
                workerPoolFactory.create(NAME, systemWorkerThreads),
                () -> true, // System jobs should always be allowed to execute on all nodes
                clock,
                schedulerEventBusFactory.create(NAME),
                serverStatus,
                gracefulShutdownService,
                shutdownTimeoutMs,
                loopSleepDuration);
    }

    private static Optional<JobDefinitionDto> createJobDefinitionLookup(String jobType) {
        // We use a single job definition for all system jobs, as they are all pre-defined and known to the system.
        // We use the job type as ID to be able to distinguish between different system jobs in the scheduler.
        return Optional.of(JobDefinitionDto.builder()
                .id(jobType)
                .title(f("%s:%s", SystemJobDefinitionConfig.TYPE_NAME, jobType))
                .description("")
                .config(SystemJobDefinitionConfig.forJobType(jobType))
                .build());
    }
}
