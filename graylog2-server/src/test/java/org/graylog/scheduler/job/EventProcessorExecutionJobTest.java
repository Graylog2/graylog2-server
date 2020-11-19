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
package org.graylog.scheduler.job;

import org.graylog.events.JobSchedulerTestClock;
import org.graylog.events.TestEventProcessorParameters;
import org.graylog.events.configuration.EventsConfiguration;
import org.graylog.events.configuration.EventsConfigurationProvider;
import org.graylog.events.processor.EventProcessorEngine;
import org.graylog.events.processor.EventProcessorExecutionJob;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobScheduleStrategies;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.JobTriggerUpdate;
import org.graylog.scheduler.JobTriggerUpdates;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("UnnecessaryLocalVariable")
public class EventProcessorExecutionJobTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private EventProcessorEngine eventProcessorEngine;

    @Mock
    private EventsConfigurationProvider eventsConfigurationProvider;

    private JobScheduleStrategies jobScheduleStrategies;
    private JobSchedulerTestClock clock;

    @Before
    public void setUp() {
        clock = new JobSchedulerTestClock(DateTime.parse("2019-01-01T00:00:00.000Z"));
        jobScheduleStrategies = new JobScheduleStrategies(clock);
        when(eventsConfigurationProvider.get()).thenReturn(EventsConfiguration.builder().build());
    }

    @Test
    public void execute() throws Exception {
        final DateTime now = clock.nowUTC();
        final long processingWindowSize = Duration.standardSeconds(60).getMillis();
        final long processingHopSize = Duration.standardSeconds(60).getMillis();
        final int scheduleIntervalSeconds = 1;
        final DateTime from = now.minus(processingWindowSize);
        final DateTime to = now;
        final DateTime triggerNextTime = now;

        final TestEventProcessorParameters eventProcessorParameters = TestEventProcessorParameters.create(from, to);
        final JobDefinitionDto jobDefinition = JobDefinitionDto.builder()
                .id("job-1")
                .title("Test")
                .description("A test")
                .config(EventProcessorExecutionJob.Config.builder()
                        .eventDefinitionId("processor-1")
                        .processingWindowSize(processingWindowSize)
                        .processingHopSize(processingHopSize)
                        .parameters(eventProcessorParameters)
                        .build())
                .build();

        final EventProcessorExecutionJob job = new EventProcessorExecutionJob(jobScheduleStrategies, clock, eventProcessorEngine, eventsConfigurationProvider, jobDefinition);

        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .id("trigger-1")
                .jobDefinitionId(jobDefinition.id())
                .startTime(now)
                .nextTime(triggerNextTime)
                .status(JobTriggerStatus.RUNNABLE)
                .schedule(IntervalJobSchedule.builder()
                        .interval(scheduleIntervalSeconds)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build();

        final JobExecutionContext jobExecutionContext = JobExecutionContext.builder()
                .definition(jobDefinition)
                .trigger(trigger)
                .isRunning(new AtomicBoolean(true))
                .jobTriggerUpdates(new JobTriggerUpdates(clock, jobScheduleStrategies, trigger))
                .build();

        final JobTriggerUpdate triggerUpdate = job.execute(jobExecutionContext);

        verify(eventProcessorEngine, times(1))
                .execute("processor-1", eventProcessorParameters);

        assertThat(triggerUpdate.nextTime()).isPresent().get().isEqualTo(triggerNextTime.plusSeconds(scheduleIntervalSeconds));

        assertThat(triggerUpdate.data()).isPresent().get().isEqualTo(EventProcessorExecutionJob.Data.builder()
                .timerangeFrom(to.plusMillis(1))
                .timerangeTo(to.plus(processingWindowSize))
                .build());

        assertThat(triggerUpdate.status()).isNotPresent();
    }

    @Test
    public void executeWithNextTimeNotBasedOnCurrentTime() throws Exception {
        final DateTime now = clock.nowUTC();
        final long processingWindowSize = Duration.standardSeconds(60).getMillis();
        final long processingHopSize = Duration.standardSeconds(60).getMillis();
        final int scheduleIntervalSeconds = 1;
        final DateTime from = now.minus(processingWindowSize);
        final DateTime to = now;
        final DateTime triggerNextTime = now;

        final TestEventProcessorParameters eventProcessorParameters = TestEventProcessorParameters.create(from, to);
        final JobDefinitionDto jobDefinition = JobDefinitionDto.builder()
                .id("job-1")
                .title("Test")
                .description("A test")
                .config(EventProcessorExecutionJob.Config.builder()
                        .eventDefinitionId("processor-1")
                        .processingWindowSize(processingWindowSize)
                        .processingHopSize(processingHopSize)
                        .parameters(eventProcessorParameters)
                        .build())
                .build();

        final EventProcessorExecutionJob job = new EventProcessorExecutionJob(jobScheduleStrategies, clock, eventProcessorEngine, eventsConfigurationProvider, jobDefinition);

        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .id("trigger-1")
                .jobDefinitionId(jobDefinition.id())
                .startTime(now)
                .nextTime(triggerNextTime)
                .status(JobTriggerStatus.RUNNABLE)
                .schedule(IntervalJobSchedule.builder()
                        .interval(scheduleIntervalSeconds)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build();

        final JobExecutionContext jobExecutionContext = JobExecutionContext.builder()
                .definition(jobDefinition)
                .trigger(trigger)
                .isRunning(new AtomicBoolean(true))
                .jobTriggerUpdates(new JobTriggerUpdates(clock, jobScheduleStrategies, trigger))
                .build();

        doAnswer(invocation -> {
            // Simulate work in the event processor
            clock.plus(10, TimeUnit.SECONDS);
            return null;
        }).when(eventProcessorEngine).execute(any(), any());

        final JobTriggerUpdate triggerUpdate = job.execute(jobExecutionContext);

        verify(eventProcessorEngine, times(1))
                .execute("processor-1", eventProcessorParameters);

        // The next time should be based on the previous nextTime + the schedule. The 10 second event processor
        // runtime should not be added to the new nextTime.
        assertThat(triggerUpdate.nextTime()).isPresent().get().isEqualTo(triggerNextTime.plusSeconds(scheduleIntervalSeconds));

        assertThat(triggerUpdate.data()).isPresent().get().isEqualTo(EventProcessorExecutionJob.Data.builder()
                .timerangeFrom(to.plusMillis(1))
                .timerangeTo(to.plus(processingWindowSize))
                .build());

        assertThat(triggerUpdate.status()).isNotPresent();
    }

    @Test
    public void executeWithTriggerDataTimerange() throws Exception {
        final DateTime now = clock.nowUTC();
        final long processingWindowSize = Duration.standardSeconds(60).getMillis();
        final long processingHopSize = Duration.standardSeconds(60).getMillis();
        final int scheduleIntervalSeconds = 1;
        final DateTime from = now.minusDays(10).minus(processingWindowSize);
        final DateTime to = now.minusDays(10);
        final DateTime triggerFrom = now.minus(processingWindowSize);
        final DateTime triggerTo = now;
        final DateTime triggerNextTime = now;

        final TestEventProcessorParameters eventProcessorParameters = TestEventProcessorParameters.create(from, to);
        final JobDefinitionDto jobDefinition = JobDefinitionDto.builder()
                .id("job-1")
                .title("Test")
                .description("A test")
                .config(EventProcessorExecutionJob.Config.builder()
                        .eventDefinitionId("processor-1")
                        .processingWindowSize(processingWindowSize)
                        .processingHopSize(processingHopSize)
                        .parameters(eventProcessorParameters)
                        .build())
                .build();

        final EventProcessorExecutionJob job = new EventProcessorExecutionJob(jobScheduleStrategies, clock, eventProcessorEngine, eventsConfigurationProvider, jobDefinition);

        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .id("trigger-1")
                .jobDefinitionId(jobDefinition.id())
                .startTime(now)
                .nextTime(triggerNextTime)
                .status(JobTriggerStatus.RUNNABLE)
                .schedule(IntervalJobSchedule.builder()
                        .interval(scheduleIntervalSeconds)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .data(EventProcessorExecutionJob.Data.create(triggerFrom, triggerTo))
                .build();

        final JobExecutionContext jobExecutionContext = JobExecutionContext.builder()
                .definition(jobDefinition)
                .trigger(trigger)
                .isRunning(new AtomicBoolean(true))
                .jobTriggerUpdates(new JobTriggerUpdates(clock, jobScheduleStrategies, trigger))
                .build();

        doAnswer(invocation -> {
            // Simulate work in the event processor
            clock.plus(5, TimeUnit.SECONDS);
            return null;
        }).when(eventProcessorEngine).execute(any(), any());

        final JobTriggerUpdate triggerUpdate = job.execute(jobExecutionContext);

        // Check that we use the timerange from the trigger instead of the parameters
        verify(eventProcessorEngine, times(1))
                .execute("processor-1", eventProcessorParameters.withTimerange(triggerFrom, triggerTo));

        assertThat(triggerUpdate.nextTime()).isPresent().get().isEqualTo(triggerNextTime.plusSeconds(scheduleIntervalSeconds));

        // The next timerange in the trigger update also needs to be based on the timerange from the trigger
        assertThat(triggerUpdate.data()).isPresent().get().isEqualTo(EventProcessorExecutionJob.Data.builder()
                .timerangeFrom(triggerTo.plusMillis(1))
                .timerangeTo(triggerTo.plus(processingWindowSize))
                .build());

        assertThat(triggerUpdate.status()).isNotPresent();
    }

    @Test
    public void executeWithInvalidTimerange() throws Exception {
        final DateTime now = clock.nowUTC();
        final long processingWindowSize = Duration.standardSeconds(60).getMillis();
        final long processingHopSize = Duration.standardSeconds(60).getMillis();
        final int scheduleIntervalSeconds = 1;
        // We set "from" to be AFTER "to" - this is not valid so the job should not be executed and the triggers
        // should be set to ERROR
        final DateTime from = now.plusSeconds(1);
        final DateTime to = now;
        final DateTime triggerNextTime = now;

        final TestEventProcessorParameters eventProcessorParameters = TestEventProcessorParameters.create(from, to);
        final JobDefinitionDto jobDefinition = JobDefinitionDto.builder()
                .id("job-1")
                .title("Test")
                .description("A test")
                .config(EventProcessorExecutionJob.Config.builder()
                        .eventDefinitionId("processor-1")
                        .processingWindowSize(processingWindowSize)
                        .processingHopSize(processingHopSize)
                        .parameters(eventProcessorParameters)
                        .build())
                .build();

        final EventProcessorExecutionJob job = new EventProcessorExecutionJob(jobScheduleStrategies, clock, eventProcessorEngine, eventsConfigurationProvider, jobDefinition);

        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .id("trigger-1")
                .jobDefinitionId(jobDefinition.id())
                .startTime(now)
                .nextTime(triggerNextTime)
                .status(JobTriggerStatus.RUNNABLE)
                .schedule(IntervalJobSchedule.builder()
                        .interval(scheduleIntervalSeconds)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build();

        final JobExecutionContext jobExecutionContext = JobExecutionContext.builder()
                .definition(jobDefinition)
                .trigger(trigger)
                .isRunning(new AtomicBoolean(true))
                .jobTriggerUpdates(new JobTriggerUpdates(clock, jobScheduleStrategies, trigger))
                .build();

        assertThatThrownBy(() -> job.execute(jobExecutionContext))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("is not after")
                .satisfies(t -> {
                    final JobExecutionException e = (JobExecutionException) t;

                    assertThat(e.getTrigger()).isEqualTo(trigger);

                    assertThat(e.getUpdate()).satisfies(update -> {
                        // When setting the status to ERROR, we will keen the last nextTime
                        assertThat(update.nextTime()).isPresent().get().isEqualTo(triggerNextTime);
                        assertThat(update.data()).isNotPresent();
                        assertThat(update.status()).isPresent().get().isEqualTo(JobTriggerStatus.ERROR);
                    });
                });

        // The engine should not be called because the timerange is invalid
        verify(eventProcessorEngine, never()).execute(any(), any());
    }

    @Test
    public void executeWithTimerangeInTheFuture() throws Exception {
        final DateTime now = clock.nowUTC();
        final long processingWindowSize = Duration.standardSeconds(60).getMillis();
        final long processingHopSize = Duration.standardSeconds(60).getMillis();
        final int scheduleIntervalSeconds = 1;
        final DateTime from = now;
        // Set the "to" timestamp of the timerange way into the future
        final DateTime to = now.plusDays(1);
        final DateTime triggerNextTime = now;

        final TestEventProcessorParameters eventProcessorParameters = TestEventProcessorParameters.create(from, to);
        final JobDefinitionDto jobDefinition = JobDefinitionDto.builder()
                .id("job-1")
                .title("Test")
                .description("A test")
                .config(EventProcessorExecutionJob.Config.builder()
                        .eventDefinitionId("processor-1")
                        .processingWindowSize(processingWindowSize)
                        .processingHopSize(processingHopSize)
                        .parameters(eventProcessorParameters)
                        .build())
                .build();

        final EventProcessorExecutionJob job = new EventProcessorExecutionJob(jobScheduleStrategies, clock, eventProcessorEngine, eventsConfigurationProvider, jobDefinition);

        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .id("trigger-1")
                .jobDefinitionId(jobDefinition.id())
                .startTime(now)
                .nextTime(triggerNextTime)
                .status(JobTriggerStatus.RUNNABLE)
                .schedule(IntervalJobSchedule.builder()
                        .interval(scheduleIntervalSeconds)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build();

        final JobExecutionContext jobExecutionContext = JobExecutionContext.builder()
                .definition(jobDefinition)
                .trigger(trigger)
                .isRunning(new AtomicBoolean(true))
                .jobTriggerUpdates(new JobTriggerUpdates(clock, jobScheduleStrategies, trigger))
                .build();

        final JobTriggerUpdate triggerUpdate = job.execute(jobExecutionContext);

        // The engine should not be called because the "to" timestamp of the timerange is in the future
        verify(eventProcessorEngine, never()).execute(any(), any());

        // The update sets the nextTime to the "to" timestamp because that is the earliest time we can execute
        // the job for the timerange
        assertThat(triggerUpdate.nextTime()).isPresent().get().isEqualTo(to);

        // Data should not be updated with any new timerange
        assertThat(triggerUpdate.data()).isNotPresent();

        assertThat(triggerUpdate.status()).isNotPresent();
    }

    @Test
    public void executeWithOnceSchedule() throws Exception {
        final DateTime now = clock.nowUTC();
        final long processingWindowSize = Duration.standardSeconds(60).getMillis();
        final long processingHopSize = Duration.standardSeconds(60).getMillis();
        final DateTime from = now.minus(processingWindowSize);
        final DateTime to = now;
        final DateTime triggerNextTime = now;

        final TestEventProcessorParameters eventProcessorParameters = TestEventProcessorParameters.create(from, to);
        final JobDefinitionDto jobDefinition = JobDefinitionDto.builder()
                .id("job-1")
                .title("Test")
                .description("A test")
                .config(EventProcessorExecutionJob.Config.builder()
                        .eventDefinitionId("processor-1")
                        .processingWindowSize(processingWindowSize)
                        .processingHopSize(processingHopSize)
                        .parameters(eventProcessorParameters)
                        .build())
                .build();

        final EventProcessorExecutionJob job = new EventProcessorExecutionJob(jobScheduleStrategies, clock, eventProcessorEngine, eventsConfigurationProvider, jobDefinition);

        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .id("trigger-1")
                .jobDefinitionId(jobDefinition.id())
                .startTime(now)
                .nextTime(triggerNextTime)
                .status(JobTriggerStatus.RUNNABLE)
                .schedule(OnceJobSchedule.create()) // This job should only be triggered ONCE
                .build();

        final JobExecutionContext jobExecutionContext = JobExecutionContext.builder()
                .definition(jobDefinition)
                .trigger(trigger)
                .isRunning(new AtomicBoolean(true))
                .jobTriggerUpdates(new JobTriggerUpdates(clock, jobScheduleStrategies, trigger))
                .build();

        final JobTriggerUpdate triggerUpdate = job.execute(jobExecutionContext);

        verify(eventProcessorEngine, times(1))
                .execute("processor-1", eventProcessorParameters);

        // No nextTime because this job was triggered ONCE
        assertThat(triggerUpdate.nextTime()).isNotPresent();

        assertThat(triggerUpdate.data()).isNotPresent();

        assertThat(triggerUpdate.status()).isNotPresent();
    }

    @Test
    public void executeWithCatchUp() throws Exception {
        catchupWindowTestHelper(EventsConfiguration.DEFAULT_CATCH_UP_WINDOW_MS, Duration.standardSeconds(60).getMillis(), Duration.standardSeconds(60).getMillis());
    }
    @Test
    public void executeWithDisabledCatchUp() throws Exception {
        catchupWindowTestHelper(0, Duration.standardSeconds(60).getMillis(), Duration.standardSeconds(60).getMillis());
    }
    @Test
    public void executeWithSmallerThanWindowSizeCatchUp() throws Exception {
        catchupWindowTestHelper(Duration.standardSeconds(59).getMillis(), Duration.standardSeconds(60).getMillis(), Duration.standardSeconds(60).getMillis());
    }
    @Test
    public void executeWithHopSizeGreaterThanWindowSize() throws Exception {
        catchupWindowTestHelper(EventsConfiguration.DEFAULT_CATCH_UP_WINDOW_MS, Duration.standardSeconds(120).getMillis(), Duration.standardSeconds(60).getMillis());
    }

    private void catchupWindowTestHelper(long catchUpWindowSize, long processingHopSize, long processingWindowSize) throws Exception {

        when(eventsConfigurationProvider.get()).thenReturn(EventsConfiguration.builder().eventCatchupWindow(catchUpWindowSize).build());

        // for easier testing. don't run into the previous day
        clock.plus(1, TimeUnit.MINUTES);

        final DateTime now = clock.nowUTC();
        final long processingCatchUpWindowSize = eventsConfigurationProvider.get().eventCatchupWindow();
        final int scheduleIntervalSeconds = (int) processingHopSize * 1000;
        final DateTime from = now.minus(processingWindowSize);
        final DateTime to = now;
        final DateTime triggerNextTime = now;
        final Duration timeSpentInEventProcessor = Duration.standardSeconds(7);

        final TestEventProcessorParameters eventProcessorParameters = TestEventProcessorParameters.create(from, to);
        final JobDefinitionDto jobDefinition = JobDefinitionDto.builder()
                .id("job-1")
                .title("Test")
                .description("A test")
                .config(EventProcessorExecutionJob.Config.builder()
                        .eventDefinitionId("processor-1")
                        .processingWindowSize(processingWindowSize)
                        .processingHopSize(processingHopSize)
                        .parameters(eventProcessorParameters)
                        .build())
                .build();

        final EventProcessorExecutionJob job = new EventProcessorExecutionJob(jobScheduleStrategies, clock, eventProcessorEngine, eventsConfigurationProvider, jobDefinition);

        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .id("trigger-1")
                .jobDefinitionId(jobDefinition.id())
                .startTime(now)
                .nextTime(triggerNextTime)
                .status(JobTriggerStatus.RUNNABLE)
                .schedule(IntervalJobSchedule.builder()
                        .interval(scheduleIntervalSeconds)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build();

        final JobExecutionContext jobExecutionContext = JobExecutionContext.builder()
                .definition(jobDefinition)
                .trigger(trigger)
                .isRunning(new AtomicBoolean(true))
                .jobTriggerUpdates(new JobTriggerUpdates(clock, jobScheduleStrategies, trigger))
                .build();

        doAnswer(invocation -> {
            // Simulate work in the event processor
            clock.plus(timeSpentInEventProcessor.getStandardSeconds(), TimeUnit.SECONDS);
            return null;
        }).when(eventProcessorEngine).execute(any(), any());

        // Simulate that we are behind at least one `processingCatchUpWindowSize`
        clock.plus(EventsConfiguration.DEFAULT_CATCH_UP_WINDOW_MS, TimeUnit.MILLISECONDS);
        clock.plus(1, TimeUnit.MILLISECONDS);

        final JobTriggerUpdate triggerUpdate = job.execute(jobExecutionContext);

        verify(eventProcessorEngine, times(1))
                .execute("processor-1", eventProcessorParameters);

        assertThat(triggerUpdate.nextTime()).isPresent().get().isEqualTo(clock.nowUTC().minus(timeSpentInEventProcessor));

        if (catchUpWindowSize > processingWindowSize && processingHopSize <= processingWindowSize) {
            // We are behind at least one chunk of catchUpWindowSize
            // The new nextFrom should ignore the processingHopSize and start 1ms after the last `To` Range
            // The nextTo will be one window of the processingCatchUpWindowSize
            assertThat(triggerUpdate.data()).isPresent().get().isEqualTo(EventProcessorExecutionJob.Data.builder()
                    .timerangeFrom(to.plus(processingHopSize).minus(processingWindowSize).plusMillis(1))
                    .timerangeTo(to.plus(processingCatchUpWindowSize))
                    .build());
        } else {
            // If no catchup is in effect, we fall back into the configured hopping window mode.
            // With a hopping window the "to" is calculated by adding the hopSize and the "from" is based on the next "to"
            // minus the windowSize + 1 millisecond.
            assertThat(triggerUpdate.data()).isPresent().get().isEqualTo(EventProcessorExecutionJob.Data.builder()
                    .timerangeFrom(to.plus(processingHopSize).minus(processingWindowSize).plusMillis(1))
                    .timerangeTo(to.plus(processingHopSize))
                    .build());
        }

        assertThat(triggerUpdate.status()).isNotPresent();
    }

    @Test
    public void executeWithHoppingWindow() throws Exception {
        clock.plus(60, TimeUnit.SECONDS);

        final DateTime now = clock.nowUTC();
        // When using a hopping window, the window size is not the same as the hop size
        final long processingWindowSize = Duration.standardSeconds(60).getMillis();
        final long processingHopSize = Duration.standardSeconds(5).getMillis();
        final int scheduleIntervalSeconds = 5;
        final DateTime from = now.minus(processingWindowSize);
        final DateTime to = now;
        final DateTime triggerNextTime = now;

        final TestEventProcessorParameters eventProcessorParameters = TestEventProcessorParameters.create(from, to);
        final JobDefinitionDto jobDefinition = JobDefinitionDto.builder()
                .id("job-1")
                .title("Test")
                .description("A test")
                .config(EventProcessorExecutionJob.Config.builder()
                        .eventDefinitionId("processor-1")
                        .processingWindowSize(processingWindowSize)
                        .processingHopSize(processingHopSize)
                        .parameters(eventProcessorParameters)
                        .build())
                .build();

        final EventProcessorExecutionJob job = new EventProcessorExecutionJob(jobScheduleStrategies, clock, eventProcessorEngine, eventsConfigurationProvider, jobDefinition);

        final JobTriggerDto trigger = JobTriggerDto.builderWithClock(clock)
                .id("trigger-1")
                .jobDefinitionId(jobDefinition.id())
                .startTime(now)
                .nextTime(triggerNextTime)
                .status(JobTriggerStatus.RUNNABLE)
                .schedule(IntervalJobSchedule.builder()
                        .interval(scheduleIntervalSeconds)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build();

        final JobExecutionContext jobExecutionContext = JobExecutionContext.builder()
                .definition(jobDefinition)
                .trigger(trigger)
                .isRunning(new AtomicBoolean(true))
                .jobTriggerUpdates(new JobTriggerUpdates(clock, jobScheduleStrategies, trigger))
                .build();

        doAnswer(invocation -> {
            // Simulate work in the event processor
            clock.plus(7, TimeUnit.SECONDS);
            return null;
        }).when(eventProcessorEngine).execute(any(), any());

        final JobTriggerUpdate triggerUpdate = job.execute(jobExecutionContext);

        verify(eventProcessorEngine, times(1))
                .execute("processor-1", eventProcessorParameters);

        // The next time should be based on the previous nextTime + the schedule. The 7 second event processor
        // runtime should not be added to the new nextTime.
        assertThat(triggerUpdate.nextTime()).isPresent().get().isEqualTo(triggerNextTime.plusSeconds(scheduleIntervalSeconds));

        // With a hopping window the "to" is calculated by adding the hopSize and the "from" is based on the next "to"
        // minus the windowSize + 1 millisecond.
        assertThat(triggerUpdate.data()).isPresent().get().isEqualTo(EventProcessorExecutionJob.Data.builder()
                .timerangeFrom(to.plus(processingHopSize).minus(processingWindowSize).plusMillis(1))
                .timerangeTo(to.plus(processingHopSize))
                .build());

        assertThat(triggerUpdate.status()).isNotPresent();
    }

    @Test
    public void dataObject() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime from = now.minusMinutes(10);
        final DateTime to = now.plusMinutes(10);

        assertThat(EventProcessorExecutionJob.Data.create(from, to)).satisfies(data -> {
            assertThat(data.timerangeFrom()).isEqualTo(from);
            assertThat(data.timerangeTo()).isEqualTo(to);
        });

        assertThatThrownBy(() -> EventProcessorExecutionJob.Data.create(to, from))
                .hasMessageContaining("from")
                .hasMessageContaining("to")
                .isInstanceOf(IllegalArgumentException.class);
    }
}
