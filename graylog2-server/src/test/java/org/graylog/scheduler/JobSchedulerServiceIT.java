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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.Uninterruptibles;
import org.bson.types.ObjectId;
import org.graylog.scheduler.capabilities.SchedulerCapabilitiesService;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.clock.JobSchedulerSystemClock;
import org.graylog.scheduler.eventbus.JobSchedulerEventBus;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog.scheduler.worker.JobWorkerPool;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.CommonMongoJackObjectMapperProvider;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.lock.LockService;
import org.graylog2.cluster.lock.MongoLockService;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
@Timeout(10)
class JobSchedulerServiceIT {

    @Mock
    private ServerStatus serverStatus;

    private NodeId nodeId = new SimpleNodeId("dummy-node");

    @Mock
    SchedulerCapabilitiesService schedulerCapabilitiesService;

    @Mock
    private GracefulShutdownService gracefulShutdownService;

    private DBJobTriggerService jobTriggerService;
    private DBCustomJobDefinitionService customJobDefinitionService;
    private JobSchedulerService jobSchedulerService;
    private JobSchedulerClock clock;

    // intentionally mutable so that individual tests can add their factories
    private final Map<String, Job.Factory> jobFactories = new HashMap<>();

    @BeforeEach
    void setUp(MongoDBTestService mongoDBTestService) throws Exception {

        final ObjectMapper objectMapper = new ObjectMapperProvider(getClass().getClassLoader(), Set.of(
                new NamedType(UnlimitedJob.class, UnlimitedJob.TYPE_NAME),
                new NamedType(LimitedJobA.class, LimitedJobA.TYPE_NAME),
                new NamedType(OnceJobSchedule.class, OnceJobSchedule.TYPE_NAME)
        )).get();

        clock = new JobSchedulerSystemClock();

        final MetricRegistry metricRegistry = new MetricRegistry();
        final JobSchedulerEventBus eventBus = new JobSchedulerEventBus("job-scheduler", metricRegistry);
        final JobSchedulerConfig schedulerConfig = new TestSchedulerConfig();
        final Duration lockExpirationDuration = Duration.seconds(10);

        customJobDefinitionService = new DBCustomJobDefinitionService(
                new MongoCollections(new CommonMongoJackObjectMapperProvider(() -> objectMapper),
                        mongoDBTestService.mongoConnection()));

        jobTriggerService = new DBJobTriggerService(
                mongoDBTestService.mongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper),
                nodeId,
                clock,
                schedulerCapabilitiesService,
                lockExpirationDuration
        );

        final DBJobDefinitionService jobDefinitionService = new DBJobDefinitionService(
                mongoDBTestService.mongoConnection(), new MongoJackObjectMapperProvider(objectMapper));
        final JobScheduleStrategies scheduleStrategies = new JobScheduleStrategies(clock);

        final JobTriggerUpdates.Factory jobTriggerUpdatesFactory = trigger -> new JobTriggerUpdates(
                clock,
                scheduleStrategies,
                trigger);

        final java.time.Duration leaderElectionLockTTL = java.time.Duration.ofSeconds(10);
        final LockService lockService = new MongoLockService(nodeId, mongoDBTestService.mongoConnection(), leaderElectionLockTTL);

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(30,
                new ThreadFactoryBuilder()
                        .setNameFormat("scheduled-daemon-%d")
                        .setDaemon(true)
                        .build()
        );

        final JobExecutionEngine.Factory executionEnginFactory = workerPool -> new JobExecutionEngine(
                jobTriggerService,
                jobDefinitionService,
                eventBus,
                scheduleStrategies,
                jobTriggerUpdatesFactory,
                () -> new RefreshingLockService(lockService, scheduler, leaderElectionLockTTL),
                jobFactories,
                workerPool,
                schedulerConfig,
                metricRegistry,
                200);

        final JobWorkerPool.Factory workerPoolFactory = (name, poolSize, shutdownCallback) ->
                new JobWorkerPool(name, poolSize, shutdownCallback, gracefulShutdownService, metricRegistry);

        final Duration loopSleepDuration = Duration.milliseconds(200);

        jobSchedulerService = new JobSchedulerService(executionEnginFactory, workerPoolFactory, schedulerConfig, clock, eventBus, serverStatus, loopSleepDuration);
    }

    @AfterEach
    public void cleanUp() {
        jobSchedulerService.triggerShutdown();
    }

    @Test
    void testMaxConcurrency() throws Exception {
        final int nLimited = 30;
        final int nUnlimited = 30;
        final CountDownLatch outstandingJobs = new CountDownLatch(nLimited + nUnlimited);
        final ConcurrentHashMap<String, Integer> concurrentExecutions = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, Integer> maxConcurrentExecutions = new ConcurrentHashMap<>();

        final Job job = ctx -> {
            final String jobType = ctx.trigger().jobDefinitionType();
            final Integer runningJobs = concurrentExecutions.compute(jobType, (k, v) -> v == null ? 1 : v + 1);
            maxConcurrentExecutions.compute(jobType, (k, v) -> Math.max(runningJobs, v == null ? 0 : v));
            //System.out.println(f("%s %s %d %d", now(), jobType, runningJobs, outstandingJobs.getCount()));

            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            concurrentExecutions.compute(jobType, (k, v) -> v == null ? 0 : v - 1);
            outstandingJobs.countDown();
            return JobTriggerUpdate.withoutNextTime();
        };

        jobFactories.put(UnlimitedJob.TYPE_NAME, jobDefinitionDto -> job);
        jobFactories.put(LimitedJobA.TYPE_NAME, jobDefinitionDto -> job);

        jobSchedulerService.startAsync().awaitRunning();
        try {
            createTriggers(nLimited, customJobDefinitionService.findOrCreate(jobDefinitionDto(new LimitedJobA())));
            createTriggers(nUnlimited, customJobDefinitionService.findOrCreate(jobDefinitionDto(new UnlimitedJob())));
            outstandingJobs.await();
        } finally {
            jobSchedulerService.stopAsync().awaitTerminated();
        }

        assertThat(maxConcurrentExecutions.get(UnlimitedJob.TYPE_NAME)).isEqualTo(TestSchedulerConfig.WORKER_THREADS);
        assertThat(maxConcurrentExecutions.get(LimitedJobA.TYPE_NAME)).isEqualTo(TestSchedulerConfig.MAX_CONCURRENCY);
    }

    @Test
    void testLimitedJobsAreNotBlockingOtherJobs() throws Exception {
        final int nLimited = 30;
        final int nUnlimited = 30;
        final CountDownLatch outstandingJobsTotal = new CountDownLatch(nLimited + nUnlimited);
        final CountDownLatch outstandingJobsUnlimited = new CountDownLatch(nUnlimited);

        final Job job = ctx -> {
            //System.out.println(f("Unlimited: %s %d", now(), outstandingJobsUnlimited.getCount()));
            outstandingJobsUnlimited.countDown();
            outstandingJobsTotal.countDown();
            return JobTriggerUpdate.withoutNextTime();
        };

        final Job blockingJob = ctx -> {
            try {
                outstandingJobsUnlimited.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            //System.out.println(f("Limited: %s %d", now(), outstandingJobsTotal.getCount()));
            outstandingJobsTotal.countDown();
            return JobTriggerUpdate.withoutNextTime();
        };

        jobFactories.put(UnlimitedJob.TYPE_NAME, jobDefinitionDto -> job);
        jobFactories.put(LimitedJobA.TYPE_NAME, jobDefinitionDto -> blockingJob);

        jobSchedulerService.startAsync().awaitRunning();
        try {
            createTriggers(nLimited, customJobDefinitionService.findOrCreate(jobDefinitionDto(new LimitedJobA())));
            createTriggers(nUnlimited, customJobDefinitionService.findOrCreate(jobDefinitionDto(new UnlimitedJob())));
            outstandingJobsTotal.await();
        } finally {
            jobSchedulerService.stopAsync().awaitTerminated();
        }
    }

    @Test
    void testInterleavedTriggers() throws Exception {
        final int nJobs = 2 * 30;
        final CountDownLatch outstandingJobs = new CountDownLatch(nJobs);
        final ConcurrentHashMap<String, Integer> concurrentExecutions = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, Integer> maxConcurrentExecutions = new ConcurrentHashMap<>();

        final Job job = ctx -> {
            final String jobType = ctx.trigger().jobDefinitionType();
            final Integer runningJobs = concurrentExecutions.compute(jobType, (k, v) -> v == null ? 1 : v + 1);
            maxConcurrentExecutions.compute(jobType, (k, v) -> Math.max(runningJobs, v == null ? 0 : v));
            //System.out.println(f("%s %s %d %d", now(), jobType, runningJobs, outstandingJobs.getCount()));

            Uninterruptibles.sleepUninterruptibly(20, TimeUnit.MILLISECONDS);
            concurrentExecutions.compute(jobType, (k, v) -> v == null ? 0 : v - 1);
            outstandingJobs.countDown();
            return JobTriggerUpdate.withoutNextTime();
        };

        jobFactories.put(UnlimitedJob.TYPE_NAME, jobDefinitionDto -> job);
        jobFactories.put(LimitedJobA.TYPE_NAME, jobDefinitionDto -> job);

        jobSchedulerService.startAsync().awaitRunning();
        try {
            for (int i = 0; i < (nJobs / 2); i++) {
                createTriggers(1, customJobDefinitionService.findOrCreate(jobDefinitionDto(new LimitedJobA())));
                createTriggers(1, customJobDefinitionService.findOrCreate(jobDefinitionDto(new UnlimitedJob())));
            }
            outstandingJobs.await();
        } finally {
            jobSchedulerService.stopAsync().awaitTerminated();
        }

        assertThat(maxConcurrentExecutions.get(LimitedJobA.TYPE_NAME)).isEqualTo(TestSchedulerConfig.MAX_CONCURRENCY);
    }

    private List<JobTriggerDto> createTriggers(int numberOfTriggers, JobDefinitionDto jobDefinition) {
        return IntStream.range(0, numberOfTriggers).mapToObj(i ->
                jobTriggerService.create(JobTriggerDto.builder()
                        .jobDefinitionType(jobDefinition.config().type())
                        .jobDefinitionId(jobDefinition.id())
                        .schedule(OnceJobSchedule.create())
                        .build())).toList();
    }

    private static class TestSchedulerConfig implements JobSchedulerConfig {
        public static final int WORKER_THREADS = 10;
        public static final int MAX_CONCURRENCY = 3;

        @Override
        public boolean canExecute() {
            return true;
        }

        @Override
        public int numberOfWorkerThreads() {
            return WORKER_THREADS;
        }

        @Override
        public Map<String, Integer> jobMaxConcurrency() {
            return Map.of(LimitedJobA.TYPE_NAME, MAX_CONCURRENCY);
        }
    }

    @JsonTypeName(UnlimitedJob.TYPE_NAME)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UnlimitedJob implements JobDefinitionConfig {
        public static final String TYPE_NAME = "unlimited-job";

        @Override
        public String type() {
            return TYPE_NAME;
        }
    }

    @JsonTypeName(LimitedJobA.TYPE_NAME)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LimitedJobA implements JobDefinitionConfig {
        public static final String TYPE_NAME = "limited-job-a";

        @Override
        public String type() {
            return TYPE_NAME;
        }
    }

    private JobDefinitionDto jobDefinitionDto(JobDefinitionConfig jobConfig) {
        return JobDefinitionDto.builder()
                .id(new ObjectId().toHexString())
                .title(JobSchedulerServiceIT.class.getSimpleName() + " job definition")
                .description("")
                .config(jobConfig)
                .build();
    }
}
