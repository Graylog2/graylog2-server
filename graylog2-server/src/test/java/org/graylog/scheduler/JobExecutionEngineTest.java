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

import com.codahale.metrics.MetricRegistry;
import org.graylog.scheduler.eventbus.JobSchedulerEventBus;
import org.graylog.scheduler.worker.JobWorkerPool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobExecutionEngineTest {

    @Mock
    private DBJobTriggerService jobTriggerService;
    @Mock
    private DBJobDefinitionService jobDefinitionService;
    @Mock
    private JobSchedulerEventBus eventBus;
    @Mock
    private JobScheduleStrategies scheduleStrategies;
    @Mock
    private JobTriggerUpdates.Factory jobTriggerUpdatesFactory;
    @Mock
    private Map<String, Job.Factory> jobFactory;
    @Mock
    private JobWorkerPool workerPool;
    @Spy
    private MetricRegistry metricRegistry = new MetricRegistry();

    @InjectMocks
    private JobExecutionEngine underTest;

    @Test
    void updateLockedJobsOnlyIfSomeJobWorkersRun() {
        underTest.updateLockedJobs();
        given(workerPool.anySlotsUsed()).willReturn(true);
        underTest.updateLockedJobs();

        verify(jobTriggerService, times(1)).updateLockedJobTriggers();
    }
}
