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
package org.graylog.events.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.common.collect.ImmutableList;
import org.graylog.events.JobSchedulerTestClock;
import org.graylog.events.TestEventProcessorConfig;
import org.graylog.events.TestEventProcessorParameters;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.storage.PersistToStreamsStorageHandler;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class EventDefinitionHandlerTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private DBEventProcessorStateService stateService;
    @Mock
    private NodeId nodeId;

    private EventDefinitionHandler handler;
    private JobSchedulerTestClock clock;
    private DBEventDefinitionService eventDefinitionService;
    private DBJobDefinitionService jobDefinitionService;
    private DBJobTriggerService jobTriggerService;

    @Before
    public void setUp() throws Exception {
        when(nodeId.toString()).thenReturn("abc-123");

        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(new NamedType(TestEventProcessorConfig.class, TestEventProcessorConfig.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(TestEventProcessorParameters.class, TestEventProcessorParameters.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(EventProcessorExecutionJob.Config.class, EventProcessorExecutionJob.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(EventProcessorExecutionJob.Data.class, EventProcessorExecutionJob.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(IntervalJobSchedule.class, IntervalJobSchedule.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(PersistToStreamsStorageHandler.Config.class, PersistToStreamsStorageHandler.Config.TYPE_NAME));

        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        this.clock = new JobSchedulerTestClock(DateTime.now(DateTimeZone.UTC));
        this.eventDefinitionService = spy(new DBEventDefinitionService(mongodb.mongoConnection(), mapperProvider, stateService, mock(EntityOwnershipService.class)));
        this.jobDefinitionService = spy(new DBJobDefinitionService(mongodb.mongoConnection(), mapperProvider));
        this.jobTriggerService = spy(new DBJobTriggerService(mongodb.mongoConnection(), mapperProvider, nodeId, clock));

        this.handler = new EventDefinitionHandler(eventDefinitionService, jobDefinitionService, jobTriggerService, clock);
    }

    @Test
    public void create() {
        final EventDefinitionDto newDto = EventDefinitionDto.builder()
                .title("Test")
                .description("A test event definition")
                .config(TestEventProcessorConfig.builder()
                        .message("This is a test event processor")
                        .searchWithinMs(300000)
                        .executeEveryMs(60001)
                        .build())
                .priority(3)
                .alert(false)
                .notificationSettings(EventNotificationSettings.withGracePeriod(60000))
                .keySpec(ImmutableList.of("a", "b"))
                .notifications(ImmutableList.of())
                .build();

        final EventDefinitionDto dto = handler.create(newDto, Optional.empty());

        // Handler should create the event definition
        assertThat(eventDefinitionService.get(dto.id())).isPresent();

        final Optional<JobDefinitionDto> jobDefinition = jobDefinitionService.getByConfigField("event_definition_id", dto.id());

        // Handler also should create the job definition for the event definition/processor
        assertThat(jobDefinition).isPresent().get().satisfies(definition -> {
            assertThat(definition.title()).isEqualTo("Test");
            assertThat(definition.description()).isEqualTo("A test event definition");
            assertThat(definition.config()).isInstanceOf(EventProcessorExecutionJob.Config.class);

            final EventProcessorExecutionJob.Config config = (EventProcessorExecutionJob.Config) definition.config();


            assertThat(config.processingWindowSize()).isEqualTo(300000);
            assertThat(config.processingHopSize()).isEqualTo(60001);
        });

        // And the handler should also create a job trigger for the created job definition
        final Optional<JobTriggerDto> jobTrigger = jobTriggerService.nextRunnableTrigger();

        assertThat(jobTrigger).isPresent().get().satisfies(trigger -> {
            assertThat(trigger.jobDefinitionId()).isEqualTo(jobDefinition.get().id());
            assertThat(trigger.schedule()).isInstanceOf(IntervalJobSchedule.class);

            final IntervalJobSchedule schedule = (IntervalJobSchedule) trigger.schedule();

            assertThat(schedule.interval()).isEqualTo(60001);
            assertThat(schedule.unit()).isEqualTo(TimeUnit.MILLISECONDS);
        });
    }

    @Test
    public void createWithoutSchedule() {
        final EventDefinitionDto newDto = EventDefinitionDto.builder()
                .title("Test")
                .description("A test event definition")
                .config(TestEventProcessorConfig.builder()
                        .message("This is a test event processor")
                        .searchWithinMs(300000)
                        .executeEveryMs(60001)
                        .build())
                .priority(3)
                .alert(false)
                .notificationSettings(EventNotificationSettings.withGracePeriod(60000))
                .keySpec(ImmutableList.of("a", "b"))
                .notifications(ImmutableList.of())
                .build();

        final EventDefinitionDto dto = handler.createWithoutSchedule(newDto, Optional.empty());

        // Handler should create the event definition
        assertThat(eventDefinitionService.get(dto.id())).isPresent();

        // Handler should NOT create a job definition for the event definition/processor
        assertThat(jobDefinitionService.getByConfigField("event_definition_id", dto.id())).isNotPresent();

        // And the handler should also NOT create a job trigger for the created job definition
        assertThat(jobTriggerService.nextRunnableTrigger()).isNotPresent();
    }

    @Test
    @MongoDBFixtures("event-processors.json")
    public void update() {
        final String newTitle = "A NEW TITLE " + DateTime.now(DateTimeZone.UTC).toString();
        final String newDescription = "A NEW DESCRIPTION " + DateTime.now(DateTimeZone.UTC).toString();

        final EventDefinitionDto existingDto = eventDefinitionService.get("54e3deadbeefdeadbeef0000").orElse(null);
        final JobDefinitionDto existingJobDefinition = jobDefinitionService.get("54e3deadbeefdeadbeef0001").orElse(null);
        final JobTriggerDto existingTrigger = jobTriggerService.get("54e3deadbeefdeadbeef0002").orElse(null);
        final TestEventProcessorConfig existingConfig = (TestEventProcessorConfig) existingDto.config();
        final TestEventProcessorConfig newConfig = existingConfig.toBuilder()
                .executeEveryMs(550000)
                .searchWithinMs(800000)
                .build();
        final EventProcessorExecutionJob.Data existingTriggerData = (EventProcessorExecutionJob.Data) existingTrigger.data().orElseThrow(AssertionError::new);

        assertThat(existingDto).isNotNull();
        assertThat(existingJobDefinition).isNotNull();
        assertThat(existingTrigger).isNotNull();

        final EventDefinitionDto updatedDto = existingDto.toBuilder()
                .title(newTitle)
                .description(newDescription)
                .config(newConfig)
                .build();

        assertThat(handler.update(updatedDto, true)).isNotEqualTo(existingDto);

        assertThat(eventDefinitionService.get(existingDto.id())).isPresent().get().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(existingDto.id());
            assertThat(dto.title()).isEqualTo(newTitle);
            assertThat(dto.description()).isEqualTo(newDescription);
        });

        // Test that the schedule is updated to the new config
        final JobDefinitionDto newJobDefinition = jobDefinitionService.get("54e3deadbeefdeadbeef0001").orElseThrow(AssertionError::new);
        assertThat(newJobDefinition.title()).isEqualTo(newTitle);
        assertThat(newJobDefinition.description()).isEqualTo(newDescription);
        assertThat(((EventProcessorExecutionJob.Config) newJobDefinition.config()).processingHopSize()).isEqualTo(550000);
        assertThat(((EventProcessorExecutionJob.Config) newJobDefinition.config()).processingWindowSize()).isEqualTo(800000);

        // Test if the EventDefinition update removed the old trigger data
        // and reset the job definition timerange to the new parameters
        final EventProcessorExecutionJob.Config newJobConfig = (EventProcessorExecutionJob.Config) newJobDefinition.config();
        final TimeRange newTimeRange = newJobConfig.parameters().timerange();
        assertThat(newTimeRange.getFrom()).isEqualTo(clock.nowUTC().minus(newConfig.searchWithinMs()));
        assertThat(newTimeRange.getTo()).isEqualTo(clock.nowUTC());

        assertThat(jobTriggerService.get("54e3deadbeefdeadbeef0002")).isPresent().get().satisfies(trigger -> {
            assertThat(trigger.data()).isEmpty();
            assertThat(trigger.nextTime()).isEqualTo(clock.nowUTC());
        });
    }

    @Test
    @MongoDBFixtures("event-processors.json")
    public void updateWithSchedulingDisabled() {
        final String newTitle = "A NEW TITLE " + DateTime.now(DateTimeZone.UTC).toString();
        final String newDescription = "A NEW DESCRIPTION " + DateTime.now(DateTimeZone.UTC).toString();

        final EventDefinitionDto existingDto = eventDefinitionService.get("54e3deadbeefdeadbeef0000").orElse(null);
        final JobDefinitionDto existingJobDefinition = jobDefinitionService.get("54e3deadbeefdeadbeef0001").orElse(null);
        final JobTriggerDto existingTrigger = jobTriggerService.get("54e3deadbeefdeadbeef0002").orElse(null);
        final TestEventProcessorConfig existingConfig = (TestEventProcessorConfig) existingDto.config();
        final TestEventProcessorConfig newConfig = existingConfig.toBuilder()
                .executeEveryMs(550000)
                .searchWithinMs(800000)
                .build();

        assertThat(existingDto).isNotNull();
        assertThat(existingJobDefinition).isNotNull();
        assertThat(existingTrigger).isNotNull();

        final EventDefinitionDto updatedDto = existingDto.toBuilder()
                .title(newTitle)
                .description(newDescription)
                .config(newConfig)
                .build();

        assertThat(handler.update(updatedDto, false)).isNotEqualTo(existingDto);

        assertThat(eventDefinitionService.get(existingDto.id())).isPresent().get().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(existingDto.id());
            assertThat(dto.title()).isEqualTo(newTitle);
            assertThat(dto.description()).isEqualTo(newDescription);
        });

        assertThat(jobDefinitionService.get("54e3deadbeefdeadbeef0001")).isNotPresent();
        assertThat(jobTriggerService.get("54e3deadbeefdeadbeef0002")).isNotPresent();
    }

    @Test
    @MongoDBFixtures("event-processors-without-schedule.json")
    public void updateWithSchedulingReEnabled() {
        final String newTitle = "A NEW TITLE " + DateTime.now(DateTimeZone.UTC).toString();
        final String newDescription = "A NEW DESCRIPTION " + DateTime.now(DateTimeZone.UTC).toString();

        final EventDefinitionDto existingDto = eventDefinitionService.get("54e3deadbeefdeadbeef0000").orElse(null);
        final TestEventProcessorConfig existingConfig = (TestEventProcessorConfig) existingDto.config();
        final TestEventProcessorConfig newConfig = existingConfig.toBuilder()
                .executeEveryMs(550000)
                .searchWithinMs(800000)
                .build();

        assertThat(existingDto).isNotNull();

        final EventDefinitionDto updatedDto = existingDto.toBuilder()
                .title(newTitle)
                .description(newDescription)
                .config(newConfig)
                .build();

        assertThat(handler.update(updatedDto, true)).isNotEqualTo(existingDto);

        assertThat(eventDefinitionService.get(existingDto.id())).isPresent().get().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(existingDto.id());
            assertThat(dto.title()).isEqualTo(newTitle);
            assertThat(dto.description()).isEqualTo(newDescription);
        });

        final JobDefinitionDto newJobDefinition = jobDefinitionService.getByConfigField("event_definition_id", existingDto.id())
                .orElseThrow(AssertionError::new);
        assertThat(newJobDefinition.title()).isEqualTo(newTitle);
        assertThat(newJobDefinition.description()).isEqualTo(newDescription);
        assertThat(((EventProcessorExecutionJob.Config) newJobDefinition.config()).processingHopSize()).isEqualTo(550000);

        assertThat(jobTriggerService.getForJob(newJobDefinition.id()).get(0)).satisfies(trigger -> {
            final IntervalJobSchedule schedule = (IntervalJobSchedule) trigger.schedule();
            assertThat(schedule.interval()).isEqualTo(550000);
        });
    }

    @Test
    @MongoDBFixtures("event-processors.json")
    public void updateWithErrors() {
        final String newTitle = "A NEW TITLE " + DateTime.now(DateTimeZone.UTC).toString();
        final String newDescription = "A NEW DESCRIPTION " + DateTime.now(DateTimeZone.UTC).toString();

        final EventDefinitionDto existingDto = eventDefinitionService.get("54e3deadbeefdeadbeef0000").orElse(null);
        final JobDefinitionDto existingJobDefinition = jobDefinitionService.get("54e3deadbeefdeadbeef0001").orElse(null);
        final JobTriggerDto existingTrigger = jobTriggerService.get("54e3deadbeefdeadbeef0002").orElse(null);

        assertThat(existingDto).isNotNull();
        assertThat(existingJobDefinition).isNotNull();
        assertThat(existingTrigger).isNotNull();

        final EventDefinitionDto updatedDto = existingDto.toBuilder()
                .title(newTitle)
                .description(newDescription)
                .build();

        doThrow(new NullPointerException("yolo1")).when(eventDefinitionService).save(any());

        assertThatCode(() -> handler.update(updatedDto, true))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("yolo1");

        assertThat(eventDefinitionService.get(existingDto.id())).isPresent().get().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(existingDto.id());
            assertThat(dto.title()).isEqualTo(existingDto.title());
            assertThat(dto.description()).isEqualTo(existingDto.description());
        });

        assertThat(jobDefinitionService.get("54e3deadbeefdeadbeef0001")).isPresent().get().satisfies(definition -> {
            assertThat(definition.title()).isEqualTo(existingJobDefinition.title());
            assertThat(definition.description()).isEqualTo(existingJobDefinition.description());
        });

        // Reset all before doing new stubs
        reset(eventDefinitionService);
        reset(jobDefinitionService);
        reset(jobTriggerService);

        doThrow(new NullPointerException("yolo2")).when(jobDefinitionService).save(any());

        assertThatCode(() -> handler.update(updatedDto, true))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("yolo2");

        assertThat(eventDefinitionService.get(existingDto.id())).isPresent().get().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(existingDto.id());
            assertThat(dto.title()).isEqualTo(existingDto.title());
            assertThat(dto.description()).isEqualTo(existingDto.description());
        });

        assertThat(jobDefinitionService.get("54e3deadbeefdeadbeef0001")).isPresent().get().satisfies(definition -> {
            assertThat(definition.title()).isEqualTo(existingJobDefinition.title());
            assertThat(definition.description()).isEqualTo(existingJobDefinition.description());
        });

        // Reset all before doing new stubs
        reset(eventDefinitionService);
        reset(jobDefinitionService);
        reset(jobTriggerService);

        doThrow(new NullPointerException("yolo3")).when(jobTriggerService).update(any());

        assertThatCode(() -> handler.update(updatedDto, true))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("yolo3");

        assertThat(eventDefinitionService.get(existingDto.id())).isPresent().get().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(existingDto.id());
            assertThat(dto.title()).isEqualTo(existingDto.title());
            assertThat(dto.description()).isEqualTo(existingDto.description());
        });

        assertThat(jobDefinitionService.get("54e3deadbeefdeadbeef0001")).isPresent().get().satisfies(definition -> {
            assertThat(definition.title()).isEqualTo(existingJobDefinition.title());
            assertThat(definition.description()).isEqualTo(existingJobDefinition.description());
        });
    }

    @Test
    @MongoDBFixtures("event-processors.json")
    public void delete() {
        assertThat(eventDefinitionService.get("54e3deadbeefdeadbeef0000")).isPresent();
        assertThat(jobDefinitionService.get("54e3deadbeefdeadbeef0001")).isPresent();
        assertThat(jobTriggerService.get("54e3deadbeefdeadbeef0002")).isPresent();

        assertThat(handler.delete("54e3deadbeefdeadbeef0000")).isTrue();

        assertThat(eventDefinitionService.get("54e3deadbeefdeadbeef0000")).isNotPresent();
        assertThat(jobDefinitionService.get("54e3deadbeefdeadbeef0001")).isNotPresent();
        assertThat(jobTriggerService.get("54e3deadbeefdeadbeef0002")).isNotPresent();
    }

    @Test
    @MongoDBFixtures("event-processors-without-schedule.json")
    public void schedule() {
        assertThat(eventDefinitionService.get("54e3deadbeefdeadbeef0000")).isPresent();
        assertThat(jobDefinitionService.streamAll().count()).isEqualTo(0);
        assertThat(jobTriggerService.all()).isEmpty();

        handler.schedule("54e3deadbeefdeadbeef0000");

        assertThat(eventDefinitionService.get("54e3deadbeefdeadbeef0000")).isPresent();

        assertThat(jobDefinitionService.getByConfigField("event_definition_id", "54e3deadbeefdeadbeef0000"))
                .get()
                .satisfies(definition -> {
                    assertThat(definition.title()).isEqualTo("Test");
                    assertThat(definition.description()).isEqualTo("A test event definition");
                    assertThat(definition.config()).isInstanceOf(EventProcessorExecutionJob.Config.class);

                    final EventProcessorExecutionJob.Config config = (EventProcessorExecutionJob.Config) definition.config();


                    assertThat(config.processingWindowSize()).isEqualTo(300000);
                    assertThat(config.processingHopSize()).isEqualTo(60000);

                    assertThat(jobTriggerService.nextRunnableTrigger()).get().satisfies(trigger -> {
                        assertThat(trigger.jobDefinitionId()).isEqualTo(definition.id());
                        assertThat(trigger.schedule()).isInstanceOf(IntervalJobSchedule.class);

                        final IntervalJobSchedule schedule = (IntervalJobSchedule) trigger.schedule();

                        assertThat(schedule.interval()).isEqualTo(60000);
                        assertThat(schedule.unit()).isEqualTo(TimeUnit.MILLISECONDS);
                    });
                });


        assertThat(jobDefinitionService.get("54e3deadbeefdeadbeef0001")).isNotPresent();
        assertThat(jobTriggerService.get("54e3deadbeefdeadbeef0002")).isNotPresent();
    }

    @Test
    @MongoDBFixtures("event-processors-without-schedule.json")
    public void scheduleWithMissingEventDefinition() {
        final String id = "54e3deadbeefdeadbeef9999";

        // The event definition should not exist so our test works
        assertThat(eventDefinitionService.get(id)).isNotPresent();

        assertThatThrownBy(() -> handler.schedule(id))
                .hasMessageContaining("doesn't exist")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @MongoDBFixtures("event-processors.json")
    public void unschedule() {
        assertThat(eventDefinitionService.get("54e3deadbeefdeadbeef0000")).isPresent();
        assertThat(jobDefinitionService.get("54e3deadbeefdeadbeef0001")).isPresent();
        assertThat(jobTriggerService.get("54e3deadbeefdeadbeef0002")).isPresent();

        handler.unschedule("54e3deadbeefdeadbeef0000");

        // Unschedule should NOT delete the event definition!
        assertThat(eventDefinitionService.get("54e3deadbeefdeadbeef0000")).isPresent();

        // Only the job definition and the trigger
        assertThat(jobDefinitionService.get("54e3deadbeefdeadbeef0001")).isNotPresent();
        assertThat(jobTriggerService.get("54e3deadbeefdeadbeef0002")).isNotPresent();
    }

    @Test
    @MongoDBFixtures("event-processors.json")
    public void unscheduleWithMissingEventDefinition() {
        final String id = "54e3deadbeefdeadbeef9999";

        // The event definition should not exist so our test works
        assertThat(eventDefinitionService.get(id)).isNotPresent();

        assertThatThrownBy(() -> handler.unschedule(id))
                .hasMessageContaining("doesn't exist")
                .isInstanceOf(IllegalArgumentException.class);
    }
}
