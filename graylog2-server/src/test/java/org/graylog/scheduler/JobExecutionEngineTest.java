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
import org.graylog2.cluster.lock.AlreadyLockedException;
import org.graylog2.cluster.lock.RefreshingLockService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
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
    @Mock
    private RefreshingLockService refreshingLockService;
    @Spy
    private MetricRegistry metricRegistry = new MetricRegistry();

    @Test
    void updateLockedJobsOnlyIfSomeJobWorkersRun() {
        final JobExecutionEngine underTest = engine();

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
        assertThat(underTest.slidingBackoff(trigger("slow-type", 1)).getMillis()).isEqualTo(60_000);
        assertThat(underTest.slidingBackoff(trigger("slow-type", 2)).getMillis()).isEqualTo(30_000);
        assertThat(underTest.slidingBackoff(trigger("slow-type", 10)).getMillis()).isEqualTo(12_000);
        assertThat(underTest.slidingBackoff(trigger("other-type", 0)).getMillis()).isEqualTo(5_000);
    }

    @Test
    void ignoresTypeBackoffThatIsNotPositive() {
        given(jobSchedulerConfig.concurrencyRescheduleBackoffs())
                .willReturn(Map.of("zero-type", Duration.ZERO, "negative-type", Duration.ofSeconds(-1)));
        final JobExecutionEngine underTest = engine();

        assertThat(underTest.slidingBackoff(trigger("zero-type", 0)).getMillis()).isEqualTo(5_000);
        assertThat(underTest.slidingBackoff(trigger("negative-type", 0)).getMillis()).isEqualTo(5_000);
    }

    @Test
    void jitterStaysWithinFifthOfBackoff() {
        for (int i = 0; i < 1_000; i++) {
            assertThat(JobExecutionEngine.jittered(org.joda.time.Duration.millis(60_000)).getMillis()).isBetween(48_000L, 72_000L);
        }
    }

    @Test
    void turnedAwayTriggerWaitsTypeBackoff() throws Exception {
        given(jobSchedulerConfig.concurrencyLimits()).willReturn(Map.of("slow-type", 1));
        given(jobSchedulerConfig.concurrencyRescheduleBackoffs()).willReturn(Map.of("slow-type", Duration.ofMinutes(1)));
        willThrow(new AlreadyLockedException("busy")).given(refreshingLockService).acquireAndKeepLock(anyString(), anyInt());
        final JobTriggerDto trigger = trigger("slow-type", 0);
        runOnWorker(trigger);

        final JobTriggerUpdate update = releasedWith(trigger);
        assertThat(update.concurrencyReschedule()).isTrue();
        assertThat(secondsFromNow(update)).isBetween(47L, 73L);
    }

    @Test
    void lockErrorReleasesTriggerForRetry() throws Exception {
        given(jobSchedulerConfig.concurrencyLimits()).willReturn(Map.of("slow-type", 1));
        willThrow(new IllegalStateException("MongoDB is gone")).given(refreshingLockService).acquireAndKeepLock(anyString(), anyInt());
        final JobTriggerDto trigger = trigger("slow-type", 0);
        runOnWorker(trigger);

        final JobTriggerUpdate update = releasedWith(trigger);
        assertThat(update.concurrencyReschedule()).isFalse();
        assertThat(secondsFromNow(update)).isBetween(4L, 6L);
    }

    @Test
    void lockReleaseErrorAfterRunDoesNotReleaseTriggerAgain() {
        given(jobSchedulerConfig.concurrencyLimits()).willReturn(Map.of("slow-type", 1));
        given(jobDefinitionLookup.lookup(any())).willReturn(Optional.empty());
        willThrow(new IllegalStateException("MongoDB is gone")).given(refreshingLockService).close();
        final JobTriggerDto trigger = trigger("slow-type", 0);
        runOnWorker(trigger);

        verify(jobTriggerService).setTriggerError(trigger);
        verify(jobTriggerService, never()).releaseTrigger(any(), any());
    }

    private void runOnWorker(JobTriggerDto trigger) {
        given(refreshingLockServiceFactory.create()).willReturn(refreshingLockService);
        given(workerPool.hasFreeSlots()).willReturn(true);
        given(workerPool.execute(any())).willAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return true;
        });
        given(jobTriggerService.nextRunnableTrigger()).willReturn(Optional.of(trigger));

        assertThat(engine().execute()).isTrue();
    }

    private JobTriggerUpdate releasedWith(JobTriggerDto trigger) {
        final ArgumentCaptor<JobTriggerUpdate> update = ArgumentCaptor.forClass(JobTriggerUpdate.class);
        verify(jobTriggerService).releaseTrigger(eq(trigger), update.capture());
        return update.getValue();
    }

    private static long secondsFromNow(JobTriggerUpdate update) {
        return (update.nextTime().orElseThrow().getMillis() - DateTime.now(DateTimeZone.UTC).getMillis()) / 1000;
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
