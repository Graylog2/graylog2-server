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
package org.graylog.plugins.pipelineprocessor.processors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StateTest {

    @Test
    public void testMetricName() {
        final PipelineInterpreter.State state = new PipelineInterpreter.State(null, null, null,
                new MetricRegistry(), 1, false);
        assertEquals("org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.stage-cache",
                state.getStageCacheMetricName());
    }

    /**
     * Regression test for <a href="https://github.com/Graylog2/graylog2-server/issues/26080">#26080</a>.
     * Verifies that concurrent State construction does not produce duplicate metric registration errors.
     */
    @Test
    public void concurrentStateConstructionDoesNotCauseDuplicateMetricErrors() throws Exception {
        final int threadCount = 10;
        final MetricRegistry sharedRegistry = new MetricRegistry() {
            @Override
            public void registerAll(com.codahale.metrics.MetricSet metrics) throws IllegalArgumentException {
                try {
                    super.registerAll(metrics);
                } catch (IllegalArgumentException e) {
                    // Fail the test if duplicate metric registration is attempted (this would normally only be logged).
                    throw new AssertionError("Duplicate metric set registered", e);
                }
            }
        };
        final CountDownLatch startLatch = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount,
                new ThreadFactoryBuilder().setNameFormat("state-test-%d").build());

        try {
            final List<Future<PipelineInterpreter.State>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    return new PipelineInterpreter.State(null, null, null, sharedRegistry, 1, false);
                }));
            }

            // Release all threads simultaneously to maximize contention
            startLatch.countDown();

            for (final Future<PipelineInterpreter.State> future : futures) {
                assertThat(future.get(5, TimeUnit.SECONDS)).isNotNull();
            }

            // CacheStatsSet registers 9 gauge metrics under the stage-cache prefix
            final long stageCacheMetricCount = sharedRegistry.getMetrics().keySet().stream()
                    .filter(name -> name.startsWith("org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.stage-cache"))
                    .count();
            assertThat(stageCacheMetricCount).isEqualTo(9);
        } finally {
            executor.shutdownNow();
        }
    }
}
