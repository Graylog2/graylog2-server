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
