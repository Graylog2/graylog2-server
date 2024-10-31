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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog.events.JobSchedulerTestClock;
import org.graylog.events.TestJobTriggerData;
import org.graylog.scheduler.capabilities.SchedulerCapabilitiesService;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mongojack.DBQuery;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DBJobTriggerServiceTest {
    private static final String NODE_ID = "node-1";
    private static final Duration EXPIRATION_DURATION = Duration.minutes(5);

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final NodeId nodeId = new SimpleNodeId(NODE_ID);

    @Mock
    private SchedulerCapabilitiesService schedulerCapabilitiesService;

    private DBJobTriggerService dbJobTriggerService;
    private final JobSchedulerTestClock clock = new JobSchedulerTestClock(DateTime.now(DateTimeZone.UTC));
    private MongoJackObjectMapperProvider mapperProvider;
    private MongoCollections mongoCollections;

    @Before
    public void setUp() throws Exception {
        lenient().when(schedulerCapabilitiesService.getNodeCapabilities()).thenReturn(ImmutableSet.of());

        ObjectMapper objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(new NamedType(IntervalJobSchedule.class, IntervalJobSchedule.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(OnceJobSchedule.class, OnceJobSchedule.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(TestJobTriggerData.class, TestJobTriggerData.TYPE_NAME));

        mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        this.mongoCollections = new MongoCollections(mapperProvider, mongodb.mongoConnection());
        this.dbJobTriggerService = serviceWithClock(clock);
    }

    private DBJobTriggerService serviceWithClock(JobSchedulerClock clock) {
        return new DBJobTriggerService(mongoCollections, nodeId, clock, schedulerCapabilitiesService, EXPIRATION_DURATION);
    }

    @Test
    @MongoDBFixtures("job-triggers.json")
    public void loadPersistedTriggers() {
        // Sort by ID to make sure we have a defined order
        final List<JobTriggerDto> all = dbJobTriggerService.all()
                .stream()
                .sorted(comparing(jobTriggerDto -> requireNonNull(jobTriggerDto.id())))
                .collect(ImmutableList.toImmutableList());

        assertThat(all).hasSize(4);

        assertThat(all.get(0)).satisfies(dto -> {
            assertThat(dto.id()).isEqualTo("54e3deadbeefdeadbeef0000");
            assertThat(dto.jobDefinitionId()).isEqualTo("54e3deadbeefdeadbeefaff3");
            assertThat(dto.startTime()).isEqualTo(DateTime.parse("2019-01-01T00:00:00.000Z"));
            assertThat(dto.endTime()).isNotPresent();
            assertThat(dto.nextTime()).isEqualTo(DateTime.parse("2019-01-01T02:00:00.000Z"));
            assertThat(dto.createdAt()).isEqualTo(DateTime.parse("2019-01-01T00:00:00.000Z"));
            assertThat(dto.updatedAt()).isEqualTo(DateTime.parse("2019-01-01T00:00:00.000Z"));
            assertThat(dto.triggeredAt()).isNotPresent();
            assertThat(dto.status()).isEqualTo(JobTriggerStatus.RUNNABLE);
            assertThat(dto.executionDurationMs()).isEmpty();
            assertThat(dto.concurrencyRescheduleCount()).isEqualTo(0);
            assertThat(dto.constraints()).isEmpty();
            assertThat(dto.isCancelled()).isFalse();

            assertThat(dto.lock().owner()).isNull();
            assertThat(dto.lock().lastLockTime()).isNull();
            assertThat(dto.lock().clock()).isZero();
            assertThat(dto.lock().progress()).isZero();

            assertThat(dto.schedule().type()).isEqualTo("interval");
            assertThat(dto.schedule()).isInstanceOf(IntervalJobSchedule.class);
            assertThat(((IntervalJobSchedule) dto.schedule()).interval()).isEqualTo(1L);
            assertThat(((IntervalJobSchedule) dto.schedule()).unit()).isEqualTo(TimeUnit.SECONDS);

            assertThat(dto.data()).isNotPresent();
        });

        assertThat(all.get(1)).satisfies(dto -> {
            assertThat(dto.id()).isEqualTo("54e3deadbeefdeadbeef0001");
            assertThat(dto.jobDefinitionId()).isEqualTo("54e3deadbeefdeadbeefaff3");
            assertThat(dto.startTime()).isEqualTo(DateTime.parse("2019-01-01T00:00:00.000Z"));
            assertThat(dto.endTime()).isPresent().get().isEqualTo(DateTime.parse("2019-01-31T00:00:00.000Z"));
            assertThat(dto.nextTime()).isEqualTo(DateTime.parse("2019-01-01T03:00:00.000Z"));
            assertThat(dto.createdAt()).isEqualTo(DateTime.parse("2019-01-01T00:00:00.000Z"));
            assertThat(dto.updatedAt()).isEqualTo(DateTime.parse("2019-01-01T00:00:00.000Z"));
            assertThat(dto.triggeredAt()).isNotPresent();
            assertThat(dto.status()).isEqualTo(JobTriggerStatus.RUNNABLE);
            assertThat(dto.executionDurationMs()).isEmpty();
            assertThat(dto.concurrencyRescheduleCount()).isEqualTo(0);
            assertThat(dto.constraints()).isEmpty();
            assertThat(dto.isCancelled()).isFalse();

            assertThat(dto.lock().owner()).isNull();
            assertThat(dto.lock().lastLockTime()).isNull();
            assertThat(dto.lock().clock()).isZero();
            assertThat(dto.lock().progress()).isZero();

            assertThat(dto.schedule().type()).isEqualTo("interval");
            assertThat(dto.schedule()).isInstanceOf(IntervalJobSchedule.class);
            assertThat(((IntervalJobSchedule) dto.schedule()).interval()).isEqualTo(1L);
            assertThat(((IntervalJobSchedule) dto.schedule()).unit()).isEqualTo(TimeUnit.SECONDS);

            assertThat(dto.data()).isPresent().get().satisfies(data -> {
                assertThat(data.type()).isEqualTo("__test_job_trigger_data__");
                assertThat(data).isInstanceOf(TestJobTriggerData.class);
                assertThat(((TestJobTriggerData) data).map()).containsEntry("hello", "world");
            });
        });

        assertThat(all.get(2)).satisfies(dto -> {
            assertThat(dto.id()).isEqualTo("54e3deadbeefdeadbeef0002");
            assertThat(dto.jobDefinitionId()).isEqualTo("54e3deadbeefdeadbeefaff4");
            assertThat(dto.startTime()).isEqualTo(DateTime.parse("2019-01-01T00:00:00.000Z"));
            assertThat(dto.endTime()).isNotPresent();
            assertThat(dto.nextTime()).isEqualTo(DateTime.parse("2019-01-01T00:00:00.000Z"));
            assertThat(dto.createdAt()).isEqualTo(DateTime.parse("2019-01-01T00:00:00.000Z"));
            assertThat(dto.updatedAt()).isEqualTo(DateTime.parse("2019-01-01T00:00:00.000Z"));
            assertThat(dto.triggeredAt()).isPresent().get().isEqualTo(DateTime.parse("2019-01-01T01:00:00.000Z"));
            assertThat(dto.status()).isEqualTo(JobTriggerStatus.RUNNING);
            assertThat(dto.executionDurationMs()).isEmpty();
            assertThat(dto.concurrencyRescheduleCount()).isEqualTo(0);
            assertThat(dto.constraints()).isEmpty();
            assertThat(dto.isCancelled()).isFalse();

            assertThat(dto.lock().owner()).isEqualTo(NODE_ID);
            assertThat(dto.lock().lastLockTime()).isEqualTo(DateTime.parse("2019-01-01T01:00:00.000Z"));
            assertThat(dto.lock().clock()).isEqualTo(5L);
            assertThat(dto.lock().progress()).isEqualTo(80);

            assertThat(dto.schedule().type()).isEqualTo("interval");
            assertThat(dto.schedule()).isInstanceOf(IntervalJobSchedule.class);
            assertThat(((IntervalJobSchedule) dto.schedule()).interval()).isEqualTo(1L);
            assertThat(((IntervalJobSchedule) dto.schedule()).unit()).isEqualTo(TimeUnit.SECONDS);

            assertThat(dto.data()).isPresent().get().satisfies(data -> {
                assertThat(data.type()).isEqualTo("__test_job_trigger_data__");
                assertThat(data).isInstanceOf(TestJobTriggerData.class);
                assertThat(((TestJobTriggerData) data).map()).containsEntry("hello", "world");
            });
        });
    }

    @Test
    @MongoDBFixtures("job-triggers.json")
    public void getForJob() {
        assertThatCode(() -> dbJobTriggerService.getOneForJob(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jobDefinitionId");

        assertThatCode(() -> dbJobTriggerService.getOneForJob(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jobDefinitionId");


        assertThat(dbJobTriggerService.getOneForJob("54e3deadbeefdeadbeefaff4")).isPresent()
                .hasValueSatisfying(trigger -> {
                    assertThat(trigger.id()).isEqualTo("54e3deadbeefdeadbeef0002");
                    assertThat(trigger.jobDefinitionId()).isEqualTo("54e3deadbeefdeadbeefaff4");
                });

        assertThat(dbJobTriggerService.getOneForJob("doesntexist")).isEmpty();

        // We expect a ISE when there is more than one trigger for a single job definition
        assertThatCode(() -> dbJobTriggerService.getOneForJob("54e3deadbeefdeadbeefaff3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("54e3deadbeefdeadbeefaff3");
    }

    @Test
    @MongoDBFixtures("job-triggers.json")
    public void getAllForJob() {

        // We expect a ISE when there is more than one trigger for a single job definition
        assertThatCode(() -> dbJobTriggerService.getOneForJob("54e3deadbeefdeadbeefaff3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("54e3deadbeefdeadbeefaff3");

        // But we can also obtain all by calling following method:
        assertThat(dbJobTriggerService.getAllForJob("54e3deadbeefdeadbeefaff3"))
                .hasSize(2)
                .allSatisfy(trigger -> assertThat(trigger.jobDefinitionId()).isEqualTo("54e3deadbeefdeadbeefaff3"));
    }


    @Test
    @MongoDBFixtures("job-triggers.json")
    public void getForJobs() {
        assertThatCode(() -> dbJobTriggerService.getForJobs(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jobDefinitionId");

        assertThat(dbJobTriggerService.getForJobs(Collections.emptySet())).isEmpty();
        assertThat(dbJobTriggerService.getForJobs(Collections.singleton("doesntexist"))).isEmpty();

        assertThat(dbJobTriggerService.getForJobs(ImmutableSet.of("54e3deadbeefdeadbeefaff4", "54e3deadbeefdeadbeefaff5")))
                .hasSize(2)
                .satisfies(triggers -> {
                    assertThat(triggers.get("54e3deadbeefdeadbeefaff4")).hasSize(1);
                    assertThat(triggers.get("54e3deadbeefdeadbeefaff4").get(0)).satisfies(trigger -> {
                        assertThat(trigger.id()).isEqualTo("54e3deadbeefdeadbeef0002");
                        assertThat(trigger.jobDefinitionId()).isEqualTo("54e3deadbeefdeadbeefaff4");
                    });

                    assertThat(triggers.get("54e3deadbeefdeadbeefaff5")).hasSize(1);
                    assertThat(triggers.get("54e3deadbeefdeadbeefaff5").get(0)).satisfies(trigger -> {
                        assertThat(trigger.id()).isEqualTo("54e3deadbeefdeadbeef0003");
                        assertThat(trigger.jobDefinitionId()).isEqualTo("54e3deadbeefdeadbeefaff5");
                    });
                });

        // We expect a ISE when there is more than one trigger for a single job definition
        assertThatCode(() -> dbJobTriggerService.getForJobs(Collections.singleton("54e3deadbeefdeadbeefaff3")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("54e3deadbeefdeadbeefaff3");
    }

    @Test
    public void getOrCreateTrigger() {
        final String id = new ObjectId().toHexString();
        final JobTriggerDto trigger = dbJobTriggerService.getOrCreate(JobTriggerDto.Builder.create(clock)
                .id(id)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());

        assertThat(trigger.id()).isEqualTo(id);
        assertThat(trigger.status()).isEqualTo(JobTriggerStatus.RUNNABLE);
        assertThat(trigger.lock()).isEqualTo(JobTriggerLock.empty());

        assertThatCode(() -> dbJobTriggerService.create(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("trigger cannot be null");
    }

    @Test
    public void createTrigger() {
        final JobTriggerDto trigger = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());

        assertThat(trigger.id()).isNotBlank();
        assertThat(trigger.status()).isEqualTo(JobTriggerStatus.RUNNABLE);
        assertThat(trigger.lock()).isEqualTo(JobTriggerLock.empty());

        assertThatCode(() -> dbJobTriggerService.create(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("trigger cannot be null");
    }

    @Test
    public void createTriggerWithID() {
        final JobTriggerDto trigger = JobTriggerDto.Builder.create(clock)
                .id("5b983c77d06b3f114bf130e2")
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build();

        assertThatThrownBy(() -> dbJobTriggerService.create(trigger))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not have an ID");
    }

    @Test
    public void updateTrigger() {
        final JobTriggerDto originalTrigger = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .schedule(OnceJobSchedule.create())
                .build());

        assertThat(originalTrigger.id()).isNotBlank();
        assertThat(originalTrigger.status()).isEqualTo(JobTriggerStatus.RUNNABLE);
        assertThat(originalTrigger.lock()).isEqualTo(JobTriggerLock.empty());

        clock.plus(1, TimeUnit.MINUTES);

        final DateTime now = clock.nowUTC();

        final JobTriggerDto updatedTrigger = originalTrigger.toBuilder()
                .jobDefinitionId("xyz-123")
                .jobDefinitionType("event-processor-execution-v2")
                .startTime(now)
                .endTime(now)
                .nextTime(now)
                .createdAt(now)
                .updatedAt(now)
                .triggeredAt(now)
                .constraints(Set.of("nope"))
                .executionDurationMs(42L)
                .concurrencyRescheduleCount(99)
                .isCancelled(true)
                .status(JobTriggerStatus.ERROR)
                .lock(JobTriggerLock.builder()
                        .owner("yolo")
                        .lastOwner("yolo2")
                        .progress(42)
                        .build())
                .schedule(IntervalJobSchedule.builder()
                        .interval(15)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build();

        assertThat(dbJobTriggerService.update(updatedTrigger)).isTrue();

        assertThat(dbJobTriggerService.get(originalTrigger.id()))
                .isPresent()
                .get()
                .satisfies(dto -> {
                    // These should not be updated:
                    assertThat(dto.jobDefinitionId()).isEqualTo(originalTrigger.jobDefinitionId());
                    assertThat(dto.nextTime()).isEqualTo(now);
                    assertThat(dto.createdAt()).isEqualTo(originalTrigger.createdAt());
                    assertThat(dto.triggeredAt()).isEqualTo(originalTrigger.triggeredAt());
                    assertThat(dto.status()).isEqualTo(originalTrigger.status());
                    assertThat(dto.lock()).isEqualTo(originalTrigger.lock());
                    assertThat(dto.constraints()).isEmpty();
                    assertThat(dto.executionDurationMs()).isEmpty();
                    assertThat(dto.isCancelled()).isFalse();

                    // These should be updated:
                    assertThat(dto.startTime()).isEqualTo(updatedTrigger.startTime());
                    assertThat(dto.endTime()).isEqualTo(updatedTrigger.endTime());
                    assertThat(dto.updatedAt()).isEqualTo(updatedTrigger.updatedAt());
                    assertThat(dto.schedule()).isEqualTo(updatedTrigger.schedule());
                    assertThat(dto.concurrencyRescheduleCount()).isEqualTo(updatedTrigger.concurrencyRescheduleCount());
                });

        assertThatCode(() -> dbJobTriggerService.update(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("trigger cannot be null");
    }

    @Test
    public void nextRunnableTriggerWithPausedCompletedAndErrorStatus() {
        // No triggers yet
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isEmpty();

        final IntervalJobSchedule schedule = IntervalJobSchedule.builder()
                .interval(1)
                .unit(TimeUnit.SECONDS)
                .build();

        final JobTriggerDto trigger1 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .nextTime(clock.nowUTC().plusSeconds(11))
                .schedule(schedule)
                .build());

        // This trigger should never be fired because its status is COMPLETE
        dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .status(JobTriggerStatus.COMPLETE)
                .nextTime(clock.nowUTC().plusSeconds(10))
                .schedule(schedule)
                .build());

        // This trigger should never be fired because its status is PAUSED
        dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .status(JobTriggerStatus.PAUSED)
                .nextTime(clock.nowUTC().plusSeconds(10))
                .schedule(schedule)
                .build());

        // This trigger should never be fired because its status is ERROR
        dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .status(JobTriggerStatus.ERROR)
                .nextTime(clock.nowUTC().plusSeconds(10))
                .schedule(schedule)
                .build());

        // First try is empty because the next time of the trigger is in the future
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isEmpty();

        clock.plus(20, TimeUnit.SECONDS);

        // The second call should return trigger1 because the other ones are in status COMPLETE, PAUSED and ERROR
        assertThat(dbJobTriggerService.nextRunnableTrigger())
                .isNotEmpty()
                .get()
                .satisfies(trigger -> assertThat(trigger.id()).isEqualTo(trigger1.id()));

        clock.plus(20, TimeUnit.SECONDS);


        // No trigger left
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isEmpty();
    }

    @Test
    public void nextRunnableTrigger() {
        // No triggers yet
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isEmpty();

        final JobTriggerDto trigger1 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .nextTime(clock.nowUTC().plusSeconds(11))
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());

        final JobTriggerDto trigger2 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .nextTime(clock.nowUTC().plusSeconds(10))
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());

        final JobTriggerDto trigger3 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .nextTime(clock.nowUTC().plusSeconds(30))
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());

        final JobTriggerDto trigger4 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .startTime(clock.nowUTC().plusSeconds(60))
                .nextTime(clock.nowUTC().plusSeconds(30))
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());

        // First try is empty because the next time of the trigger is in the future
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isEmpty();

        clock.plus(20, TimeUnit.SECONDS);

        // Now the next time is in the past and we must get a locked trigger
        assertNextTrigger(dbJobTriggerService.nextRunnableTrigger(), trigger2);

        // The second call should return the second trigger
        assertNextTrigger(dbJobTriggerService.nextRunnableTrigger(), trigger1);

        clock.plus(20, TimeUnit.SECONDS);

        // The third call should return the third trigger
        assertNextTrigger(dbJobTriggerService.nextRunnableTrigger(), trigger3);

        // No trigger left because trigger4's startTime is still in the future
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isEmpty();

        clock.plus(20, TimeUnit.SECONDS);

        // The fourth call should return the third trigger
        assertNextTrigger(dbJobTriggerService.nextRunnableTrigger(), trigger4);

        // No trigger left
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isEmpty();
    }

    @Test
    @MongoDBFixtures("job-triggers.json")
    public void nextRunnableTriggerWithEndTime() {
        // Set clock to base date used in the fixture file
        final JobSchedulerTestClock clock = new JobSchedulerTestClock(DateTime.parse("2019-01-01T00:00:00.000Z"));
        final DBJobTriggerService service = serviceWithClock(clock);

        // No triggers yet because 54e3deadbeefdeadbeef0002 is already locked and RUNNING
        assertThat(service.nextRunnableTrigger()).isEmpty();

        // Advancing the clock a bit
        clock.plus(2, TimeUnit.HOURS);

        // Now we should get trigger 54e3deadbeefdeadbeef0000
        assertThat(service.nextRunnableTrigger())
                .isNotEmpty()
                .get()
                .satisfies(trigger -> assertThat(trigger.id()).isEqualTo("54e3deadbeefdeadbeef0000"));

        // No more runnable triggers now
        assertThat(service.nextRunnableTrigger()).isEmpty();

        // Advancing clock far into the future, past the endTime of trigger 54e3deadbeefdeadbeef0001
        clock.plus(40, TimeUnit.DAYS);

        // We shouldn't get trigger 54e3deadbeefdeadbeef0001 because of its endTime
        assertThat(service.nextRunnableTrigger()).isEmpty();
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void assertNextTrigger(Optional<JobTriggerDto> trigger, JobTriggerDto expected) {
        assertThat(trigger)
                .isNotEmpty()
                .get()
                .satisfies(t -> {
                    assertThat(t.id())
                            .withFailMessage("We expected the following trigger to be locked: %s", expected)
                            .isEqualTo(expected.id());
                    assertThat(t.status()).isEqualTo(JobTriggerStatus.RUNNING);
                    assertThat(t.triggeredAt()).isPresent().get().isEqualTo(clock.nowUTC());
                    assertThat(t.lock().owner()).isEqualTo(NODE_ID);
                    assertThat(t.lock().lastLockTime()).isEqualTo(clock.nowUTC());
                });
    }

    @Test
    public void releaseTrigger() {
        final JobTriggerDto trigger1 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .concurrencyRescheduleCount(42)
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());
        final JobTriggerData newData = TestJobTriggerData.create(Collections.singletonMap("hello", "world"));
        final JobTriggerUpdate update = JobTriggerUpdate.withNextTimeAndData(clock.nowUTC().plusSeconds(20), newData);

        // Releasing the trigger should not do anything because the trigger has not been locked yet
        assertThat(dbJobTriggerService.releaseTrigger(trigger1, update)).isFalse();

        // Lock the trigger
        final Optional<JobTriggerDto> runnableTrigger = dbJobTriggerService.nextRunnableTrigger();
        assertThat(runnableTrigger).isNotEmpty();

        clock.plus(15, TimeUnit.SECONDS);

        // Releasing the trigger should work now
        assertThat(dbJobTriggerService.releaseTrigger(runnableTrigger.get(), update)).isTrue();

        assertThat(dbJobTriggerService.get(trigger1.id()))
                .isPresent()
                .get()
                .satisfies(trigger -> {
                    // Make sure the lock is gone
                    assertThat(trigger.lock().owner()).isNull();
                    assertThat(trigger.status()).isEqualTo(JobTriggerStatus.RUNNABLE);
                    assertThat(trigger.nextTime()).isEqualTo(update.nextTime().orElse(null));
                    assertThat(trigger.executionDurationMs()).isPresent().get().isEqualTo(15_000L);
                    assertThat(trigger.concurrencyRescheduleCount()).isEqualTo(0);
                    assertThat(trigger.data()).isPresent().get().satisfies(data -> {
                        assertThat(data).isInstanceOf(TestJobTriggerData.class);
                        assertThat(data).isEqualTo(TestJobTriggerData.create(Collections.singletonMap("hello", "world")));
                    });
                });

        // Releasing it again doesn't do anything
        assertThat(dbJobTriggerService.releaseTrigger(trigger1, update)).isFalse();
    }

    @Test
    public void releaseTriggerWithConcurrencyRescheduleCount() {
        final JobTriggerDto trigger1 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .concurrencyRescheduleCount(0)
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());
        final JobTriggerUpdate update = JobTriggerUpdate.withConcurrencyReschedule(clock.nowUTC().plusSeconds(20));

        // Releasing the trigger should not do anything because the trigger has not been locked yet
        assertThat(dbJobTriggerService.releaseTrigger(trigger1, update)).isFalse();

        // Lock the trigger
        final Optional<JobTriggerDto> runnableTrigger = dbJobTriggerService.nextRunnableTrigger();
        assertThat(runnableTrigger).isNotEmpty();

        // Releasing the trigger should work now
        assertThat(dbJobTriggerService.releaseTrigger(runnableTrigger.get(), update)).isTrue();

        assertThat(dbJobTriggerService.get(trigger1.id()))
                .isPresent()
                .get()
                .satisfies(trigger -> {
                    // Make sure the lock is gone
                    assertThat(trigger.lock().owner()).isNull();
                    assertThat(trigger.status()).isEqualTo(JobTriggerStatus.RUNNABLE);
                    assertThat(trigger.nextTime()).isEqualTo(update.nextTime().orElse(null));
                    // The count must be increased by one
                    assertThat(trigger.concurrencyRescheduleCount()).isEqualTo(1);
                    assertThat(trigger.data()).isEmpty();
                });

        // Releasing it again doesn't do anything
        assertThat(dbJobTriggerService.releaseTrigger(trigger1, update)).isFalse();
    }

    @Test
    public void releaseTriggerWithoutNextTime() {
        final JobTriggerDto trigger1 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());
        final JobTriggerUpdate update = JobTriggerUpdate.builder().build();

        // Releasing the trigger should not do anything because the trigger has not been locked yet
        assertThat(dbJobTriggerService.releaseTrigger(trigger1, update)).isFalse();

        // Lock the trigger
        final Optional<JobTriggerDto> runnableTrigger = dbJobTriggerService.nextRunnableTrigger();
        assertThat(runnableTrigger).isNotEmpty();

        // Releasing the trigger should work now
        assertThat(dbJobTriggerService.releaseTrigger(runnableTrigger.get(), update)).isTrue();

        assertThat(dbJobTriggerService.get(trigger1.id()))
                .isPresent()
                .get()
                .satisfies(trigger -> {
                    // Make sure the lock is gone
                    assertThat(trigger.lock().owner()).isNull();
                    // Status should be COMPLETE because an empty nextTime means the trigger should not be executed anymore
                    assertThat(trigger.status()).isEqualTo(JobTriggerStatus.COMPLETE);
                    // The nextTime should not be updated and stay the same
                    assertThat(trigger.nextTime()).isEqualTo(trigger1.nextTime());
                });

        // Releasing it again doesn't do anything
        assertThat(dbJobTriggerService.releaseTrigger(trigger1, update)).isFalse();
    }

    @Test
    public void releaseTriggerWithStatus() {
        final JobTriggerDto trigger1 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());
        final JobTriggerUpdate update = JobTriggerUpdate.withError(trigger1);

        // Releasing the trigger should not do anything because the trigger has not been locked yet
        assertThat(dbJobTriggerService.releaseTrigger(trigger1, update)).isFalse();

        // Lock the trigger
        final Optional<JobTriggerDto> runnableTrigger = dbJobTriggerService.nextRunnableTrigger();
        assertThat(runnableTrigger).isNotEmpty();

        // Releasing the trigger should work now
        assertThat(dbJobTriggerService.releaseTrigger(runnableTrigger.get(), update)).isTrue();

        assertThat(dbJobTriggerService.get(trigger1.id()))
                .isPresent()
                .get()
                .satisfies(trigger -> {
                    // Make sure the lock is gone
                    assertThat(trigger.lock().owner()).isNull();
                    // Status must be error!
                    assertThat(trigger.status()).isEqualTo(JobTriggerStatus.ERROR);
                    assertThat(trigger.nextTime()).isEqualTo(trigger1.nextTime());
                });

        // Releasing it again doesn't do anything
        assertThat(dbJobTriggerService.releaseTrigger(trigger1, update)).isFalse();
    }

    @Test
    public void releaseTriggerWithInvalidArguments() {

        assertThatCode(() -> dbJobTriggerService.releaseTrigger(null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("trigger");

        assertThatCode(() -> dbJobTriggerService.releaseTrigger(null, mock(JobTriggerUpdate.class)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("trigger");

        assertThatCode(() -> dbJobTriggerService.releaseTrigger(mock(JobTriggerDto.class), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("triggerUpdate");
    }

    @Test
    public void releaseCancelledTrigger() {
        final JobTriggerDto trigger1 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .concurrencyRescheduleCount(42)
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());
        final JobTriggerData newData = TestJobTriggerData.create(Collections.singletonMap("hello", "world"));
        final JobTriggerUpdate update = JobTriggerUpdate.withNextTimeAndData(clock.nowUTC().plusSeconds(20), newData);

        // Lock the trigger
        final Optional<JobTriggerDto> runnableTrigger = dbJobTriggerService.nextRunnableTrigger();
        assertThat(runnableTrigger).isNotEmpty();

        clock.plus(15, TimeUnit.SECONDS);

        // Cancel the trigger
        dbJobTriggerService.cancelTriggerByQuery(MongoUtils.idEq(requireNonNull(trigger1.id())));

        // Check that the "is_cancelled" field is set to true
        assertThat(dbJobTriggerService.get(trigger1.id()))
                .map(JobTriggerDto::isCancelled)
                .get()
                .isEqualTo(true);

        // Release the trigger
        assertThat(dbJobTriggerService.releaseTrigger(runnableTrigger.get(), update)).isTrue();

        assertThat(dbJobTriggerService.get(trigger1.id()))
                .isPresent()
                .get()
                .satisfies(trigger -> {
                    // Make sure the lock is gone
                    assertThat(trigger.lock().owner()).isNull();
                    assertThat(trigger.status()).isEqualTo(JobTriggerStatus.RUNNABLE);
                    // Releasing the lock should reset the "is_cancelled" flag to false
                    assertThat(trigger.isCancelled()).isFalse();
                });
    }

    @Test
    public void setTriggerError() {
        final JobTriggerDto trigger1 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());

        // Lock the trigger
        assertThat(dbJobTriggerService.nextRunnableTrigger())
                .isNotEmpty()
                .get()
                .satisfies(trigger -> assertThat(trigger.id()).isEqualTo(trigger1.id()));

        assertThat(dbJobTriggerService.setTriggerError(trigger1)).isTrue();

        assertThat(dbJobTriggerService.get(trigger1.id()))
                .isPresent()
                .get()
                .satisfies(trigger -> {
                    // Make sure the lock is gone
                    assertThat(trigger.lock().owner()).isNull();
                    assertThat(trigger.status()).isEqualTo(JobTriggerStatus.ERROR);
                });

        assertThatCode(() -> dbJobTriggerService.setTriggerError(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("trigger");
    }

    @Test
    @MongoDBFixtures("job-triggers.json")
    public void delete() {
        assertThat(dbJobTriggerService.delete("54e3deadbeefdeadbeef0000")).isTrue();
        assertThat(dbJobTriggerService.delete("54e3deadbeefdeadbeef9999")).isFalse();

        assertThatCode(() -> dbJobTriggerService.delete(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("triggerId");

        assertThatCode(() -> dbJobTriggerService.delete(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("triggerId");
    }

    @Test
    @MongoDBFixtures("job-triggers.json")
    public void deleteCompleted() {
        assertThat(dbJobTriggerService.deleteCompletedOnceSchedulesOlderThan(1, TimeUnit.DAYS)).isEqualTo(1);
        assertThat(dbJobTriggerService.get("54e3deadbeefdeadbeef0003")).isNotPresent();
    }

    @Test
    @MongoDBFixtures("job-triggers.json")
    public void deleteCompletedTooNew() {
        final JobTriggerDto trigger = dbJobTriggerService.get("54e3deadbeefdeadbeef0003").orElseThrow(AssertionError::new);
        // sets updated_at to recent timestamp
        dbJobTriggerService.update(trigger);
        assertThat(dbJobTriggerService.deleteCompletedOnceSchedulesOlderThan(1, TimeUnit.DAYS)).isZero();
    }

    @Test
    @MongoDBFixtures("stale-job-triggers.json")
    public void forceReleaseOwnedTriggers() {
        final Set<String> triggerIds = dbJobTriggerService.all().stream()
                .filter(dto -> JobTriggerStatus.RUNNING.equals(dto.status()))
                .map(JobTriggerDto::id)
                .collect(Collectors.toSet());

        assertThat(triggerIds).containsOnly("54e3deadbeefdeadbeef0001", "54e3deadbeefdeadbeef0002", "54e3deadbeefdeadbeef0004");

        assertThat(dbJobTriggerService.forceReleaseOwnedTriggers()).isEqualTo(2);

        final Set<String> newTriggerIds = dbJobTriggerService.all().stream()
                .filter(dto -> JobTriggerStatus.RUNNING.equals(dto.status()))
                .map(JobTriggerDto::id)
                .collect(Collectors.toSet());

        // Running triggers not owned by this node should not be released
        assertThat(newTriggerIds).containsOnly("54e3deadbeefdeadbeef0002");
    }

    @Test
    @MongoDBFixtures("stale-job-triggers.json")
    public void forceReleaseOwnedCancelledTriggers() {
        final Set<String> cancelledTriggerIds = dbJobTriggerService.all().stream()
                .filter(JobTriggerDto::isCancelled)
                .map(JobTriggerDto::id)
                .collect(Collectors.toSet());

        assertThat(cancelledTriggerIds).containsOnly("54e3deadbeefdeadbeef0001");

        assertThat(dbJobTriggerService.forceReleaseOwnedTriggers()).isEqualTo(2);

        final Set<String> newCancelledTriggerIds = dbJobTriggerService.all().stream()
                .filter(JobTriggerDto::isCancelled)
                .map(JobTriggerDto::id)
                .collect(Collectors.toSet());

        // Force releasing triggers should reset the "is_cancelled" flag to false
        assertThat(newCancelledTriggerIds).isEmpty();
    }

    @Test
    @MongoDBFixtures("stale-job-triggers-with-expired-lock.json")
    public void nextStaleTrigger() {
        final JobSchedulerTestClock clock = new JobSchedulerTestClock(DateTime.parse("2019-01-01T02:00:00.000Z"));
        final DBJobTriggerService service = serviceWithClock(clock);

        assertThat(service.nextRunnableTrigger())
                .isNotEmpty()
                .get()
                .satisfies(trigger -> assertThat(trigger.id()).isEqualTo("54e3deadbeefdeadbeef0002"));
    }

    @Test
    @MongoDBFixtures("locked-job-triggers.json")
    public void updateLockedJobTriggers() {
        DateTime newLockTime = DateTime.parse("2019-01-01T02:00:00.000Z");
        final JobSchedulerTestClock clock = new JobSchedulerTestClock(newLockTime);
        final DBJobTriggerService service = serviceWithClock(clock);

        service.updateLockedJobTriggers();

        List<String> updatedJobTriggerIds = service.all().stream()
                .filter(jobTriggerDto -> newLockTime.equals(jobTriggerDto.lock().lastLockTime()))
                .map(JobTriggerDto::id)
                .collect(Collectors.toList());
        assertThat(updatedJobTriggerIds).containsOnly("54e3deadbeefdeadbeef0001", "54e3deadbeefdeadbeef0002");
    }

    @Test
    public void triggerWithConstraints() {
        final JobTriggerDto.Builder triggerBuilder = JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .jobDefinitionType("event-processor-execution-v1")
                .nextTime(clock.nowUTC().minusSeconds(10))
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build());

        // no constraints
        dbJobTriggerService.create(triggerBuilder
                .build());
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isNotEmpty();
        dbJobTriggerService.deleteByQuery(DBQuery.empty());

        // two unfulfilled constraints
        dbJobTriggerService.create(triggerBuilder
                .constraints(ImmutableSet.of("IS_LEADER", "HAS_ARCHIVE"))
                .build());
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isEmpty();
        dbJobTriggerService.deleteByQuery(DBQuery.empty());

        // two fulfilled constraints
        when(schedulerCapabilitiesService.getNodeCapabilities()).thenReturn(ImmutableSet.of("HAS_ARCHIVE", "IS_LEADER"));
        dbJobTriggerService.create(triggerBuilder
                .constraints(ImmutableSet.of("IS_LEADER", "HAS_ARCHIVE"))
                .build());
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isNotEmpty();
        dbJobTriggerService.deleteByQuery(DBQuery.empty());

        // more capabilities than constraints
        when(schedulerCapabilitiesService.getNodeCapabilities()).thenReturn(ImmutableSet.of("HAS_ARCHIVE", "IS_LEADER", "ANOTHER_CAPABITILITY"));
        dbJobTriggerService.create(triggerBuilder
                .constraints(ImmutableSet.of("IS_LEADER", "HAS_ARCHIVE"))
                .build());
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isNotEmpty();
        dbJobTriggerService.deleteByQuery(DBQuery.empty());

        // more constraints than capabilities
        when(schedulerCapabilitiesService.getNodeCapabilities()).thenReturn(ImmutableSet.of("HAS_ARCHIVE", "IS_LEADER"));
        dbJobTriggerService.create(triggerBuilder
                .constraints(ImmutableSet.of("IS_LEADER", "HAS_ARCHIVE", "ANOTHER_CONSTRAINT"))
                .build());
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isEmpty();
        dbJobTriggerService.deleteByQuery(DBQuery.empty());
    }

    @Test
    @MongoDBFixtures("job-triggers.json")
    public void updateProgress() {
        final JobTriggerDto trigger = dbJobTriggerService.get("54e3deadbeefdeadbeef0003").orElseThrow(AssertionError::new);

        assertThat(trigger.lock().progress()).isEqualTo(0);

        assertThat(dbJobTriggerService.updateProgress(trigger, 42)).isEqualTo(1);

        final JobTriggerDto updatedTrigger = dbJobTriggerService.get("54e3deadbeefdeadbeef0003").orElseThrow(AssertionError::new);

        assertThat(updatedTrigger.lock().progress()).isEqualTo(42);
    }

    @Test
    @MongoDBFixtures("locked-job-triggers.json")
    public void cancelTriggerByQuery() {
        // Must return an empty Optional if the query didn't match any trigger
        assertThat(dbJobTriggerService.cancelTriggerByQuery(DBQuery.is("foo", "bar"))).isEmpty();

        final JobTriggerDto lockedTrigger = dbJobTriggerService.get("54e3deadbeefdeadbeef0001").orElseThrow(AssertionError::new);

        assertThat(lockedTrigger.isCancelled()).isFalse();

        assertThat(dbJobTriggerService.cancelTriggerByQuery(DBQuery.is("_id", "54e3deadbeefdeadbeef0001"))).isPresent();

        final JobTriggerDto cancelledTrigger = dbJobTriggerService.get(lockedTrigger.id()).orElseThrow(AssertionError::new);

        assertThat(cancelledTrigger.isCancelled()).isTrue();
    }

    @Test
    @MongoDBFixtures("job-triggers-for-overdue-count.json")
    public void numberOfOverdueTriggers() {
        final JobSchedulerTestClock clock = new JobSchedulerTestClock(DateTime.parse("2019-01-01T04:00:00.000Z"));
        final DBJobTriggerService service = serviceWithClock(clock);

        final Map<String, Long> result = service.numberOfOverdueTriggers();

        assertThat(result).isEqualTo(Map.of(
                "event-processor-execution-v1", 2L,
                "notification-execution-v1", 1L
        ));
    }
}
