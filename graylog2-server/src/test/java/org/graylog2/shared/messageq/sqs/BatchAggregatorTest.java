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
package org.graylog2.shared.messageq.sqs;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchAggregatorTest {

    private BatchAggregator<String> aggregator;

    @After
    public void tearDown() throws Exception {
        if (aggregator != null) {
            aggregator.shutdown();
        }
    }

    @Test
    public void testFlushWhenFull() {
        List<List<String>> flushedBatches = new CopyOnWriteArrayList<>();

        aggregator = new BatchAggregator<>(flushedBatches::add, 2, Duration.ofHours(1));
        aggregator.start();

        aggregator.feed("a");
        assertThat(flushedBatches).isEmpty();
        aggregator.feed("b");
        assertThat(flushedBatches).containsExactly(ImmutableList.of("a", "b"));
        aggregator.feed("c");
        assertThat(flushedBatches).containsExactly(ImmutableList.of("a", "b"));
        aggregator.feed("d");
        assertThat(flushedBatches).containsExactly(ImmutableList.of("a", "b"), ImmutableList.of("c", "d"));
    }

    @Test
    public void testFlushPeriodically() {
        List<List<String>> flushedBatches = new CopyOnWriteArrayList<>();

        aggregator = new BatchAggregator<>(flushedBatches::add, 2, Duration.ofMillis(10));
        aggregator.start();

        aggregator.feed("a");
        Awaitility.await()
                .atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(flushedBatches).containsExactly(ImmutableList.of("a")));

        aggregator.feed("b");
        Awaitility.await()
                .atMost(1, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(flushedBatches).containsExactly(ImmutableList.of("a"), ImmutableList.of("b")));
    }

}
