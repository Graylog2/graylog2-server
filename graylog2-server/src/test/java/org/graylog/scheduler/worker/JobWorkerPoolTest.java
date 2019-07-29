package org.graylog.scheduler.worker;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.Uninterruptibles;
import org.assertj.core.api.AbstractThrowableAssert;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class JobWorkerPoolTest {
    @Test
    public void testExecute() throws Exception {
        final JobWorkerPool pool = new JobWorkerPool("test", 2, new GracefulShutdownService(), new MetricRegistry());

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        final CountDownLatch task1Latch = new CountDownLatch(1);
        final CountDownLatch task2Latch = new CountDownLatch(1);

        // Before we do anything, the number of free slots should be the same as the pool size
        assertThat(pool.freeSlots()).isEqualTo(2);

        // Execute the first task
        assertThat(pool.execute(() -> {
            Uninterruptibles.awaitUninterruptibly(task1Latch, 60, TimeUnit.SECONDS);
            latch1.countDown();
        })).isTrue();

        // The number of free slots should be reduced by one
        assertThat(pool.freeSlots()).isEqualTo(1);

        // Execute the second task
        assertThat(pool.execute(() -> {
            Uninterruptibles.awaitUninterruptibly(task2Latch, 60, TimeUnit.SECONDS);
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

        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
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
        return assertThatCode(() -> new JobWorkerPool(name, 1, new GracefulShutdownService(), new MetricRegistry()));
    }
}
