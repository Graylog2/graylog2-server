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
package org.graylog.scheduler.worker;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.Uninterruptibles;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
public class JobWorkerPoolTest {

    @Test
    public void testExecute() throws Exception {
        final JobWorkerPool pool = new JobWorkerPool("test", 2, new MetricRegistry());

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        final CountDownLatch task1Latch = new CountDownLatch(1);
        final CountDownLatch task2Latch = new CountDownLatch(1);

        // Before we do anything, the number of free slots should be the same as the pool size
        assertThat(pool.freeSlots()).isEqualTo(2);
        assertThat(pool.anySlotsUsed()).isFalse();

        // Execute the first task
        assertThat(pool.execute(() -> {
            var ignored = Uninterruptibles.awaitUninterruptibly(task1Latch, 60, TimeUnit.SECONDS);
            latch1.countDown();
        })).isTrue();

        // The number of free slots should be reduced by one
        assertThat(pool.freeSlots()).isEqualTo(1);
        assertThat(pool.anySlotsUsed()).isTrue();

        // Execute the second task
        assertThat(pool.execute(() -> {
            var ignored = Uninterruptibles.awaitUninterruptibly(task2Latch, 60, TimeUnit.SECONDS);
            latch2.countDown();
        })).isTrue();

        // The number of free slots should be reduced by one
        assertThat(pool.freeSlots()).isEqualTo(0);

        // Since there are no slots left, the tasks shouldn't be executed
        assertThat(pool.execute(() -> {})).isFalse();
        assertThat(pool.freeSlots()).isEqualTo(0);

        // Wait for the first task to finish
        task1Latch.countDown();
        assertThat(latch1.await(60, TimeUnit.SECONDS)).isTrue();

        // Wait for the second task to finish
        task2Latch.countDown();
        assertThat(latch2.await(60, TimeUnit.SECONDS)).isTrue();

        pool.shutdown(Duration.ofSeconds(30));
        assertThat(pool.anySlotsUsed()).isFalse();
    }

    @Test
    public void testPoolNames() {
        assertName("a").doesNotThrowAnyException();
        assertName("hello-world").doesNotThrowAnyException();
        assertName("hello123").doesNotThrowAnyException();
        assertName("hello-world-123").doesNotThrowAnyException();

        assertName("hello.world").isInstanceOf(IllegalArgumentException.class);
        assertName("hello_world").isInstanceOf(IllegalArgumentException.class);
        assertName("hello world").isInstanceOf(IllegalArgumentException.class);
        assertName("hello/world").isInstanceOf(IllegalArgumentException.class);
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertName(String name) {
        return assertThatCode(() -> new JobWorkerPool(name, 1, new MetricRegistry()));
    }
}
