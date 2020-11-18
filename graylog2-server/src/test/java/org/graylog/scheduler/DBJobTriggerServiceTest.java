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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.JobSchedulerTestClock;
import org.graylog.events.TestJobTriggerData;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DBJobTriggerServiceTest {
    private static final String NODE_ID = "node-1";

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private NodeId nodeId;

    private DBJobTriggerService dbJobTriggerService;
    private JobSchedulerTestClock clock = new JobSchedulerTestClock(DateTime.now(DateTimeZone.UTC));
    private ObjectMapper objectMapper;
    private MongoJackObjectMapperProvider mapperProvider;

    @Before
    public void setUp() throws Exception {
        when(nodeId.toString()).thenReturn(NODE_ID);

        objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(new NamedType(IntervalJobSchedule.class, IntervalJobSchedule.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(OnceJobSchedule.class, OnceJobSchedule.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(TestJobTriggerData.class, TestJobTriggerData.TYPE_NAME));

        mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        this.dbJobTriggerService = new DBJobTriggerService(mongodb.mongoConnection(), mapperProvider, nodeId, clock);
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

            assertThat(dto.lock().owner()).isNull();
            assertThat(dto.lock().lastLockTime()).isNull();
            assertThat(dto.lock().clock()).isEqualTo(0L);
            assertThat(dto.lock().progress()).isEqualTo(0);

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

            assertThat(dto.lock().owner()).isNull();
            assertThat(dto.lock().lastLockTime()).isNull();
            assertThat(dto.lock().clock()).isEqualTo(0L);
            assertThat(dto.lock().progress()).isEqualTo(0);

            assertThat(dto.schedule().type()).isEqualTo("interval");
            assertThat(dto.schedule()).isInstanceOf(IntervalJobSchedule.class);
            assertThat(((IntervalJobSchedule) dto.schedule()).interval()).isEqualTo(1L);
            assertThat(((IntervalJobSchedule) dto.schedule()).unit()).isEqualTo(TimeUnit.SECONDS);

            assertThat(dto.data()).isPresent().get().satisfies(data -> {
                assertThat(data.type()).isEqualTo("__test_job_trigger_data__");
                assertThat(data).isInstanceOf(TestJobTriggerData.class);
                assertThat(((TestJobTriggerData) data).map().get("hello")).isEqualTo("world");
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

            assertThat(dto.lock().owner()).isEqualTo("node-a");
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
                assertThat(((TestJobTriggerData) data).map().get("hello")).isEqualTo("world");
            });
        });
    }

    @Test
    @MongoDBFixtures("job-triggers.json")
    public void getForJob() {
        assertThatCode(() -> dbJobTriggerService.getForJob(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jobDefinitionId");

        assertThatCode(() -> dbJobTriggerService.getForJob(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jobDefinitionId");


        assertThat(dbJobTriggerService.getForJob("54e3deadbeefdeadbeefaff4")).hasSize(1).satisfies(triggers -> {
            assertThat(triggers.get(0)).satisfies(trigger -> {
                assertThat(trigger.id()).isEqualTo("54e3deadbeefdeadbeef0002");
                assertThat(trigger.jobDefinitionId()).isEqualTo("54e3deadbeefdeadbeefaff4");
            });
        });

        assertThat(dbJobTriggerService.getForJob("doesntexist")).isEmpty();

        // We expect a ISE when there is more than one trigger for a single job definition
        assertThatCode(() -> dbJobTriggerService.getForJob("54e3deadbeefdeadbeefaff3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("54e3deadbeefdeadbeefaff3");
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
    public void createTrigger() {
        final JobTriggerDto trigger = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
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
                .schedule(IntervalJobSchedule.builder()
                        .interval(15)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());

        assertThat(originalTrigger.id()).isNotBlank();
        assertThat(originalTrigger.status()).isEqualTo(JobTriggerStatus.RUNNABLE);
        assertThat(originalTrigger.lock()).isEqualTo(JobTriggerLock.empty());

        clock.plus(1, TimeUnit.MINUTES);

        final DateTime now = clock.nowUTC();

        final JobTriggerDto updatedTrigger = originalTrigger.toBuilder()
                .jobDefinitionId("xyz-123")
                .startTime(now)
                .endTime(now)
                .nextTime(now)
                .createdAt(now)
                .updatedAt(now)
                .triggeredAt(now)
                .status(JobTriggerStatus.ERROR)
                .lock(JobTriggerLock.builder()
                        .owner("yolo")
                        .build())
                .schedule(OnceJobSchedule.create())
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

                    // These should be updated:
                    assertThat(dto.startTime()).isEqualTo(updatedTrigger.startTime());
                    assertThat(dto.endTime()).isEqualTo(updatedTrigger.endTime());
                    assertThat(dto.updatedAt()).isEqualTo(updatedTrigger.updatedAt());
                    assertThat(dto.schedule()).isEqualTo(updatedTrigger.schedule());
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
                .nextTime(clock.nowUTC().plusSeconds(11))
                .schedule(schedule)
                .build());

        // This trigger should never be fired because its status is COMPLETE
        dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .status(JobTriggerStatus.COMPLETE)
                .nextTime(clock.nowUTC().plusSeconds(10))
                .schedule(schedule)
                .build());

        // This trigger should never be fired because its status is PAUSED
        dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .status(JobTriggerStatus.PAUSED)
                .nextTime(clock.nowUTC().plusSeconds(10))
                .schedule(schedule)
                .build());

        // This trigger should never be fired because its status is ERROR
        dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
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
                .nextTime(clock.nowUTC().plusSeconds(11))
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());

        final JobTriggerDto trigger2 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .nextTime(clock.nowUTC().plusSeconds(10))
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());

        final JobTriggerDto trigger3 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .nextTime(clock.nowUTC().plusSeconds(30))
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());

        final JobTriggerDto trigger4 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
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
        assertThat(dbJobTriggerService.nextRunnableTrigger())
                .isNotEmpty()
                .get()
                .satisfies(trigger -> {
                    assertThat(trigger.id())
                            .withFailMessage("We expected the following trigger to be locked because its the first one: %s", trigger2)
                            .isEqualTo(trigger2.id());
                    assertThat(trigger.status()).isEqualTo(JobTriggerStatus.RUNNING);
                    assertThat(trigger.triggeredAt()).isPresent().get().isEqualTo(clock.nowUTC());
                    assertThat(trigger.lock().owner()).isEqualTo(NODE_ID);
                    assertThat(trigger.lock().lastLockTime()).isEqualTo(clock.nowUTC());
                });

        // The second call should return the second trigger
        assertThat(dbJobTriggerService.nextRunnableTrigger())
                .isNotEmpty()
                .get()
                .satisfies(trigger -> {
                    assertThat(trigger.id())
                            .withFailMessage("We expected the following trigger to be locked because its the second one: %s", trigger1)
                            .isEqualTo(trigger1.id());
                    assertThat(trigger.status()).isEqualTo(JobTriggerStatus.RUNNING);
                    assertThat(trigger.triggeredAt()).isPresent().get().isEqualTo(clock.nowUTC());
                    assertThat(trigger.lock().owner()).isEqualTo(NODE_ID);
                    assertThat(trigger.lock().lastLockTime()).isEqualTo(clock.nowUTC());
                });

        clock.plus(20, TimeUnit.SECONDS);

        // The third call should return the third trigger
        assertThat(dbJobTriggerService.nextRunnableTrigger())
                .isNotEmpty()
                .get()
                .satisfies(trigger -> {
                    assertThat(trigger.id())
                            .withFailMessage("We expected the following trigger to be locked because its the third one: %s", trigger3)
                            .isEqualTo(trigger3.id());
                    assertThat(trigger.status()).isEqualTo(JobTriggerStatus.RUNNING);
                    assertThat(trigger.triggeredAt()).isPresent().get().isEqualTo(clock.nowUTC());
                    assertThat(trigger.lock().owner()).isEqualTo(NODE_ID);
                    assertThat(trigger.lock().lastLockTime()).isEqualTo(clock.nowUTC());
                });

        // No trigger left because trigger4's startTime is still in the future
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isEmpty();

        clock.plus(20, TimeUnit.SECONDS);

        // The fourth call should return the third trigger
        assertThat(dbJobTriggerService.nextRunnableTrigger())
                .isNotEmpty()
                .get()
                .satisfies(trigger -> {
                    assertThat(trigger.id())
                            .withFailMessage("We expected the following trigger to be locked because its the only one left: %s", trigger4)
                            .isEqualTo(trigger4.id());
                    assertThat(trigger.status()).isEqualTo(JobTriggerStatus.RUNNING);
                    assertThat(trigger.triggeredAt()).isPresent().get().isEqualTo(clock.nowUTC());
                    assertThat(trigger.lock().owner()).isEqualTo(NODE_ID);
                    assertThat(trigger.lock().lastLockTime()).isEqualTo(clock.nowUTC());
                });

        // No trigger left
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isEmpty();
    }

    @Test
    @MongoDBFixtures("job-triggers.json")
    public void nextRunnableTriggerWithEndTime() {
        // Set clock to base date used in the fixture file
        final JobSchedulerTestClock clock = new JobSchedulerTestClock(DateTime.parse("2019-01-01T00:00:00.000Z"));
        final DBJobTriggerService service = new DBJobTriggerService(mongodb.mongoConnection(), mapperProvider, nodeId, clock);

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

    @Test
    public void releaseTrigger() {
        final JobTriggerDto trigger1 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
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
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isNotEmpty();

        // Releasing the trigger should work now
        assertThat(dbJobTriggerService.releaseTrigger(trigger1, update)).isTrue();

        assertThat(dbJobTriggerService.get(trigger1.id()))
                .isPresent()
                .get()
                .satisfies(trigger -> {
                    // Make sure the lock is gone
                    assertThat(trigger.lock().owner()).isNull();
                    assertThat(trigger.status()).isEqualTo(JobTriggerStatus.RUNNABLE);
                    assertThat(trigger.nextTime()).isEqualTo(update.nextTime().orElse(null));
                    assertThat(trigger.data()).isPresent().get().satisfies(data -> {
                        assertThat(data).isInstanceOf(TestJobTriggerData.class);
                        assertThat(data).isEqualTo(TestJobTriggerData.create(Collections.singletonMap("hello", "world")));
                    });
                });

        // Releasing it again doesn't do anything
        assertThat(dbJobTriggerService.releaseTrigger(trigger1, update)).isFalse();
    }

    @Test
    public void releaseTriggerWithoutNextTime() {
        final JobTriggerDto trigger1 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());
        final JobTriggerUpdate update = JobTriggerUpdate.builder().build();

        // Releasing the trigger should not do anything because the trigger has not been locked yet
        assertThat(dbJobTriggerService.releaseTrigger(trigger1, update)).isFalse();

        // Lock the trigger
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isNotEmpty();

        // Releasing the trigger should work now
        assertThat(dbJobTriggerService.releaseTrigger(trigger1, update)).isTrue();

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
                .schedule(IntervalJobSchedule.builder()
                        .interval(1)
                        .unit(TimeUnit.SECONDS)
                        .build())
                .build());
        final JobTriggerUpdate update = JobTriggerUpdate.withError(trigger1);

        // Releasing the trigger should not do anything because the trigger has not been locked yet
        assertThat(dbJobTriggerService.releaseTrigger(trigger1, update)).isFalse();

        // Lock the trigger
        assertThat(dbJobTriggerService.nextRunnableTrigger()).isNotEmpty();

        // Releasing the trigger should work now
        assertThat(dbJobTriggerService.releaseTrigger(trigger1, update)).isTrue();

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
    public void setTriggerError() {
        final JobTriggerDto trigger1 = dbJobTriggerService.create(JobTriggerDto.Builder.create(clock)
                .jobDefinitionId("abc-123")
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
        assertThat(dbJobTriggerService.get("54e3deadbeefdeadbeef0003").isPresent()).isFalse();


    }

    @Test
    @MongoDBFixtures("job-triggers.json")
    public void deleteCompletedTooNew() {
        final JobTriggerDto trigger = dbJobTriggerService.get("54e3deadbeefdeadbeef0003").orElseThrow(AssertionError::new);
        // sets updated_at to recent timestamp
        dbJobTriggerService.update(trigger);
        assertThat(dbJobTriggerService.deleteCompletedOnceSchedulesOlderThan(1, TimeUnit.DAYS)).isEqualTo(0);

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
}
