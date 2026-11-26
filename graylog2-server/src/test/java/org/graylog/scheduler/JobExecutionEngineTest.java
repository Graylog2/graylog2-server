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
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog.scheduler.worker.JobWorkerPool;
import org.graylog2.cluster.lock.RefreshingLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobExecutionEngineTest {

    @Mock
    private DBJobTriggerService jobTriggerService;
    @Mock
    private JobDefinitionLookup jobDefinitionLookup;
    @Mock
    private JobSchedulerEventBus eventBus;
    @Mock
    private JobScheduleStrategies scheduleStrategies;
    @Mock
    private JobTriggerUpdates.Factory jobTriggerUpdatesFactory;
    @Mock
    private RefreshingLockService.Factory refreshingLockServiceFactory;
    @Mock
    private Map<String, Job.Factory<? extends Job>> jobFactory;
    @Mock
    private JobSchedulerConfig jobSchedulerConfig;
    @Mock
    private JobWorkerPool workerPool;
    @Spy
    private MetricRegistry metricRegistry = new MetricRegistry();

    @Test
    void updateLockedJobsOnlyIfSomeJobWorkersRun() {
        JobExecutionEngine underTest = new JobExecutionEngine(jobTriggerService, jobDefinitionLookup, eventBus, scheduleStrategies, jobTriggerUpdatesFactory,
                refreshingLockServiceFactory, jobFactory, workerPool, "test", jobSchedulerConfig, metricRegistry);

        underTest.updateLockedJobs();
        given(workerPool.anySlotsUsed()).willReturn(true);
        underTest.updateLockedJobs();

        verify(jobTriggerService, times(1)).updateLockedJobTriggers();
    }

    @Test
    void concurrencyRescheduleUsesDefaultBackoffWithoutTypeBackoff() {
        final JobExecutionEngine underTest = engine();

        assertThat(underTest.slidingBackoff(trigger("other-type", 0)).getMillis()).isEqualTo(5_000);
        assertThat(underTest.slidingBackoff(trigger("other-type", 10)).getMillis()).isEqualTo(1_000);
    }

    @Test
    void concurrencyRescheduleUsesTypeBackoffAndSlidesToFifth() {
        given(jobSchedulerConfig.concurrencyRescheduleBackoffs()).willReturn(Map.of("slow-type", Duration.ofMinutes(1)));
        final JobExecutionEngine underTest = engine();

        assertThat(underTest.slidingBackoff(trigger("slow-type", 0)).getMillis()).isEqualTo(60_000);
        assertThat(underTest.slidingBackoff(trigger("slow-type", 2)).getMillis()).isEqualTo(30_000);
        assertThat(underTest.slidingBackoff(trigger("slow-type", 10)).getMillis()).isEqualTo(12_000);
    }

    private JobExecutionEngine engine() {
        return new JobExecutionEngine(jobTriggerService, jobDefinitionLookup, eventBus, scheduleStrategies, jobTriggerUpdatesFactory,
                refreshingLockServiceFactory, jobFactory, workerPool, "test", jobSchedulerConfig, metricRegistry);
    }

    private static JobTriggerDto trigger(String jobType, int concurrencyRescheduleCount) {
        return JobTriggerDto.builder()
                .jobDefinitionId("job-definition-1")
                .jobDefinitionType(jobType)
                .schedule(OnceJobSchedule.create())
                .concurrencyRescheduleCount(concurrencyRescheduleCount)
                .build();
    }
}
