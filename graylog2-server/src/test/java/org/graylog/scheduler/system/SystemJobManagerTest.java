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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.github.joschi.jadconfig.util.Duration;
import com.google.auto.value.AutoValue;
import org.graylog.events.JobSchedulerTestClock;
import org.graylog.scheduler.DBSystemJobTriggerService;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.capabilities.SchedulerCapabilitiesService;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.rest.models.system.SystemJobSummary;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
class SystemJobManagerTest {
    private static final String NODE_ID = "test-node-1";
    private static final Duration EXPIRATION_DURATION = Duration.minutes(5);

    private final NodeId nodeId = new SimpleNodeId(NODE_ID);

    private DBSystemJobTriggerService triggerService;
    private SystemJobManager systemJobManager;

    private final JobSchedulerTestClock clock = new JobSchedulerTestClock(DateTime.parse("2024-01-15T10:00:00.000Z"));

    @BeforeEach
    void setUp(MongoConnection mongoConnection) {
        final var objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(new NamedType(OnceJobSchedule.class, OnceJobSchedule.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(TestSystemJobConfig.class, TestSystemJobConfig.TYPE_NAME));

        final var mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        final var mongoCollections = new MongoCollections(mapperProvider, mongoConnection);

        this.triggerService = new DBSystemJobTriggerService(mongoCollections, nodeId, clock, new SchedulerCapabilitiesService(Set.of()), EXPIRATION_DURATION);
        this.systemJobManager = new SystemJobManager(triggerService, clock);
    }

    @Test
    void submitCreatesJobTrigger() {
        systemJobManager.submit(TestSystemJobConfig.create("test-value"));

        try (var stream = triggerService.streamAll()) {
            final var triggers = stream.toList();
            assertThat(triggers).hasSize(1);
            assertThat(triggers.getFirst()).satisfies(trigger -> {
                assertThat(trigger.jobDefinitionId()).isEqualTo(TestSystemJobConfig.TYPE_NAME);
                assertThat(trigger.jobDefinitionType()).isEqualTo(SystemJobDefinitionConfig.TYPE_NAME);
                assertThat(trigger.status()).isEqualTo(JobTriggerStatus.RUNNABLE);
                assertThat(trigger.data()).isPresent();
                assertThat(trigger.data().get()).isInstanceOf(TestSystemJobConfig.class);
                assertThat(((TestSystemJobConfig) trigger.data().get()).testValue()).isEqualTo("test-value");
            });
        }
    }

    @Test
    void submitWithDelayCreatesJobTriggerWithFutureStartTime() {
        final var config = TestSystemJobConfig.create("delayed-job");
        final var delay = java.time.Duration.ofMinutes(5);
        final var expectedStartTime = clock.nowUTC().plusMinutes(5);

        systemJobManager.submitWithDelay(config, delay);

        try (var stream = triggerService.streamAll()) {
            final var triggers = stream.toList();
            assertThat(triggers).hasSize(1);
            assertThat(triggers.getFirst()).satisfies(trigger -> {
                assertThat(trigger.startTime()).isEqualTo(expectedStartTime);
                assertThat(trigger.nextTime()).isEqualTo(expectedStartTime);
            });
        }
    }

    @Test
    void getRunningJobConfigsReturnsOnlyRunningJobsOfSpecifiedType() {
        // Create and lock a job (simulating a running job)
        systemJobManager.submit(TestSystemJobConfig.create("running-job-1"));
        final var lockedTrigger = triggerService.nextRunnableTrigger();
        assertThat(lockedTrigger).isPresent();

        // Create a non-running job
        systemJobManager.submit(TestSystemJobConfig.create("pending-job"));

        final var runningConfigs = systemJobManager.getRunningJobConfigs(TestSystemJobConfig.TYPE_NAME);

        assertThat(runningConfigs).hasSize(1);
        assertThat(((TestSystemJobConfig) runningConfigs.getFirst()).testValue()).isEqualTo("running-job-1");
    }

    @Test
    void getRunningJobConfigsReturnsEmptyListWhenNoRunningJobs() {
        systemJobManager.submit(TestSystemJobConfig.create("pending-job"));

        assertThat(systemJobManager.getRunningJobConfigs(TestSystemJobConfig.TYPE_NAME)).isEmpty();
    }

    @Test
    void getRunningJobsReturnsAllRunningJobs() {
        // Submit and lock first job
        systemJobManager.submit(TestSystemJobConfig.create("job-1"));
        assertThat(triggerService.nextRunnableTrigger()).isPresent();

        // Submit and lock second job
        systemJobManager.submit(TestSystemJobConfig.create("job-2"));
        assertThat(triggerService.nextRunnableTrigger()).isPresent();

        // Submit but don't lock third job
        systemJobManager.submit(TestSystemJobConfig.create("job-3"));

        final var runningJobs = systemJobManager.getRunningJobs();

        assertThat(runningJobs).hasSize(2);
        assertThat(runningJobs.values())
                .extracting(SystemJobSummary::name)
                .containsExactlyInAnyOrder(TestSystemJobConfig.TYPE_NAME, TestSystemJobConfig.TYPE_NAME);
    }

    @Test
    void getRunningJobsForNodeReturnsOnlyJobsOwnedByNode() {
        // Submit and lock a job (owned by this node)
        systemJobManager.submit(TestSystemJobConfig.create("my-job"));
        assertThat(triggerService.nextRunnableTrigger()).isPresent();

        final var runningJobs = systemJobManager.getRunningJobs(nodeId);

        assertThat(runningJobs).hasSize(1);
        assertThat(runningJobs.values().iterator().next().nodeId()).isEqualTo(NODE_ID);
    }

    @Test
    void getRunningJobReturnsJobById() {
        systemJobManager.submit(TestSystemJobConfig.create("specific-job"));
        final var locked = triggerService.nextRunnableTrigger();
        assertThat(locked).isPresent();
        final var triggerId = locked.get().id();

        final var runningJob = systemJobManager.getRunningJob(triggerId);

        assertThat(runningJob).isPresent();
        assertThat(runningJob.get().id()).isEqualTo(triggerId);
        assertThat(runningJob.get().info()).isEqualTo("test-value=specific-job");
    }

    @Test
    void getRunningJobReturnsEmptyForNonRunningJob() {
        systemJobManager.submit(TestSystemJobConfig.create("pending-job"));

        // Get the trigger ID without locking it
        try (var stream = triggerService.streamAll()) {
            final var trigger = stream.findFirst().orElseThrow();
            final var runningJob = systemJobManager.getRunningJob(trigger.id());

            assertThat(runningJob).isEmpty();
        }
    }

    @Test
    void getRunningJobReturnsEmptyForNonExistentId() {
        // Use a valid ObjectId format that doesn't exist in the database
        assertThat(systemJobManager.getRunningJob("000000000000000000000000")).isEmpty();
    }

    /**
     * Test implementation of SystemJobConfig for use in tests.
     */
    @AutoValue
    @JsonTypeName(TestSystemJobConfig.TYPE_NAME)
    @JsonDeserialize(builder = TestSystemJobConfig.Builder.class)
    public static abstract class TestSystemJobConfig implements SystemJobConfig {
        public static final String TYPE_NAME = "__test_system_job_config__";

        @JsonProperty("test_value")
        public abstract String testValue();

        public static TestSystemJobConfig create(String testValue) {
            return builder().testValue(testValue).build();
        }

        public static Builder builder() {
            return Builder.create();
        }

        @Override
        public SystemJobInfo toInfo() {
            return SystemJobInfo.builder()
                    .type(TYPE_NAME)
                    .description("Test System Job")
                    .statusInfo("test-value=" + testValue())
                    .isCancelable(false)
                    .reportsProgress(false)
                    .build();
        }

        @AutoValue.Builder
        public static abstract class Builder implements SystemJobConfig.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_SystemJobManagerTest_TestSystemJobConfig.Builder().type(TYPE_NAME);
            }

            @JsonProperty("test_value")
            public abstract Builder testValue(String testValue);

            public abstract TestSystemJobConfig build();
        }
    }
}
