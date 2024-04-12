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
package org.graylog2.system.jobs;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.Uninterruptibles;
import org.assertj.core.api.Assertions;
import org.graylog2.system.activities.SystemMessageActivityWriter;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SystemJobManagerTest {

    private SystemMessageActivityWriter systemMessageActivityWriter;

    @BeforeEach
    void setUp() {
        systemMessageActivityWriter = Mockito.mock(SystemMessageActivityWriter.class);
    }

    @Test
    public void testGetRunningJobs() throws Exception {
        SystemJobManager manager = new SystemJobManager(systemMessageActivityWriter, new MetricRegistry());

        LongRunningJob job1 = new LongRunningJob(1);
        LongRunningJob job2 = new LongRunningJob(1);

        String jobID1 = manager.submit(job1);
        String jobID2 = manager.submit(job2);

        assertEquals(2, manager.getRunningJobs().size());
        assertTrue(manager.getRunningJobs().containsValue(job1));
        assertTrue(manager.getRunningJobs().containsValue(job2));

        assertEquals(jobID1, manager.getRunningJobs().get(jobID1).getId());
        assertEquals(jobID2, manager.getRunningJobs().get(jobID2).getId());
    }

    @Test
    public void testConcurrentJobs() throws Exception {
        SystemJobManager manager = new SystemJobManager(systemMessageActivityWriter, new MetricRegistry());

        SystemJob job1 = new LongRunningJob(3);
        SystemJob job2 = new LongRunningJob(3);
        SystemJob job3 = new AnotherLongRunningJob(3);

        manager.submit(job1);
        manager.submit(job2);
        manager.submit(job3);

        assertEquals(3, manager.getRunningJobs().size());
        assertEquals(2, manager.concurrentJobs(job1.getClass()));
    }

    @RepeatedTest(100)
    public void testSubmitThrowsExceptionIfMaxConcurrencyLevelReached() throws Exception {
        SystemJobManager manager = new SystemJobManager(systemMessageActivityWriter, new MetricRegistry());

        final ExecutorService executorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat("job-trigger-%d").build());

        LongRunningJob job1 = new LongRunningJob(3);
        LongRunningJob job2 = new LongRunningJob(3);
        SystemJob job3 = new AnotherLongRunningJob(3);

        // We have to set it for both instances in tests because the stubs are dynamic and no static max level can be set.
        job1.setMaxConcurrency(1);
        job2.setMaxConcurrency(1);

        final List<Callable<Optional<String>>> tasks = Arrays.asList(
                wrapJobCatchException(manager, job1),
                wrapJobCatchException(manager, job2)
        );

        final List<Future<Optional<String>>> futures = executorService.invokeAll(tasks);
        executorService.shutdown();
        final boolean terminatedSuccessfully = executorService.awaitTermination(5, TimeUnit.SECONDS);
        Assertions.assertThat(terminatedSuccessfully).isTrue();

        // now futures are either an optional with job ID or empty optional if the execution failed due to a  SystemJobConcurrencyException
        Assertions.assertThat(futures)
                .extracting(Future::get)
                .filteredOn(Optional::isEmpty)
                .hasSize(1);

        manager.submit(job3);

        assertEquals(2, manager.getRunningJobs().size());
        assertEquals(1, manager.concurrentJobs(job1.getClass()));
    }

    @NotNull
    private static Callable<Optional<String>> wrapJobCatchException(SystemJobManager manager, LongRunningJob job1) {
        return () -> {
            try {
                return Optional.of(manager.submit(job1));
            } catch (SystemJobConcurrencyException e) {
                return Optional.empty();
            }
        };
    }

    private static class LongRunningJob extends SystemJob {

        private int seconds;
        private int maxConcurrency = 9001;

        public LongRunningJob(int seconds) {
            this.seconds = seconds;
        }

        @Override
        public void execute() {
            Uninterruptibles.sleepUninterruptibly(seconds, TimeUnit.SECONDS);
        }

        void setMaxConcurrency(int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
        }

        @Override
        public void requestCancel() {
        }

        @Override
        public int getProgress() {
            return 0;
        }

        @Override
        public int maxConcurrency() {
            return maxConcurrency;
        }

        @Override
        public boolean providesProgress() {
            return false;
        }

        @Override
        public boolean isCancelable() {
            return false;
        }

        @Override
        public String getDescription() {
            return "Test Job. You better not use this anywhere else, bro.";
        }

        @Override
        public String getClassName() {
            return getClass().getCanonicalName();
        }
    }

    private static class AnotherLongRunningJob extends SystemJob {

        private int seconds;

        public AnotherLongRunningJob(int seconds) {
            this.seconds = seconds;
        }

        @Override
        public void execute() {
            Uninterruptibles.sleepUninterruptibly(seconds, TimeUnit.SECONDS);
        }

        @Override
        public void requestCancel() {
        }

        @Override
        public int getProgress() {
            return 0;
        }

        @Override
        public int maxConcurrency() {
            return 9001;
        }

        @Override
        public boolean providesProgress() {
            return false;
        }

        @Override
        public boolean isCancelable() {
            return false;
        }

        @Override
        public String getDescription() {
            return "Another Test Job. You better not use this anywhere else, bro.";
        }

        @Override
        public String getClassName() {
            return getClass().getCanonicalName();
        }
    }
}
