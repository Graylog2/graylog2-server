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
package org.graylog2.system.shutdown;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GracefulShutdownServiceTest {
    private GracefulShutdownService shutdownService;

    @BeforeEach
    public void setUp() throws Exception {
        this.shutdownService = new GracefulShutdownService();
        shutdownService.startAsync().awaitRunning();
    }

    private void stop(GracefulShutdownService service) throws Exception {
        service.stopAsync().awaitTerminated(1, TimeUnit.MINUTES);
    }

    @Test
    public void registerAndShutdown() throws Exception {
        final AtomicBoolean hook1Called = new AtomicBoolean(false);
        final AtomicBoolean hook2Called = new AtomicBoolean(false);

        shutdownService.register(() -> hook1Called.set(true));
        shutdownService.register(() -> hook2Called.set(true));

        assertThat(hook1Called).isFalse();
        assertThat(hook2Called).isFalse();

        stop(shutdownService);

        assertThat(hook1Called).isTrue();
        assertThat(hook2Called).isTrue();
    }

    @Test
    public void withExceptionOnShutdown() throws Exception {
        final AtomicBoolean hook1Called = new AtomicBoolean(false);
        final AtomicBoolean hook2Called = new AtomicBoolean(false);

        shutdownService.register(() -> hook1Called.set(true));
        shutdownService.register(() -> { throw new Exception("eek"); });

        stop(shutdownService);

        assertThat(hook1Called).isTrue();
        assertThat(hook2Called).isFalse();
    }

    @Test
    public void registerAndUnregister() throws Exception {
        final AtomicBoolean hook1Called = new AtomicBoolean(false);
        final AtomicBoolean hook2Called = new AtomicBoolean(false);

        final GracefulShutdownHook hook1 = () -> hook1Called.set(true);
        final GracefulShutdownHook hook2 = () -> hook2Called.set(true);

        shutdownService.register(hook1);
        shutdownService.register(hook2);

        assertThat(hook1Called).isFalse();
        assertThat(hook2Called).isFalse();

        shutdownService.unregister(hook1);

        stop(shutdownService);

        assertThat(hook1Called).isFalse();
        assertThat(hook2Called).isTrue();
    }

    @Test
    public void registerAndUnregisterNull() throws Exception {
        assertThatThrownBy(() -> shutdownService.register(null))
                .hasMessageContaining("shutdownHook")
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> shutdownService.unregister(null))
                .hasMessageContaining("shutdownHook")
                .isInstanceOf(NullPointerException.class);

        stop(shutdownService);
    }

    @Test
    public void registerMoreThanOnce() throws Exception {
        final AtomicInteger value = new AtomicInteger(0);

        final GracefulShutdownHook hook = value::incrementAndGet;

        shutdownService.register(hook);
        shutdownService.register(hook);
        shutdownService.register(hook);

        assertThat(value.get()).isEqualTo(0);

        stop(shutdownService);

        assertThat(value.get()).isEqualTo(1);
    }

    @Test
    public void failRegisterAndUnregisterDuringShutdown() {
        final GracefulShutdownHook hook = () -> {};

        shutdownService.register(hook);

        shutdownService.stopAsync().awaitTerminated();

        assertThatThrownBy(() -> shutdownService.register(hook), "register during shutdown should not work")
                .hasMessageContaining("register")
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> shutdownService.unregister(hook), "unregister during shutdown should not work")
                .hasMessageContaining("unregister")
                .isInstanceOf(IllegalStateException.class);
    }

    @DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "Flaky on Jenkins")
    @Test
    @Timeout(1)
    public void registerMany() throws Exception {

        final int numberOfHooks = 20;
        final AtomicInteger hooksStarted = new AtomicInteger(0);
        final AtomicInteger hooksFinished = new AtomicInteger(0);
        final AtomicInteger maxConcurrency = new AtomicInteger(0);

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        for (int i = 0; i < numberOfHooks; i++) {
            shutdownService.register(() -> {
                hooksStarted.incrementAndGet();
                countDownLatch.await();
                maxConcurrency.getAndAccumulate(hooksStarted.get() - hooksFinished.get(), Math::max);
                hooksFinished.incrementAndGet();
            });
        }

        shutdownService.stopAsync();

        // Wait until 10 hooks are being actively executed.
        // This should not lead to other hooks being skipped, which was a bug.
        Awaitility.await()
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .atMost(100, TimeUnit.MILLISECONDS)
                .until(() -> hooksStarted.get() >= 10);

        countDownLatch.countDown();

        shutdownService.awaitTerminated(200, TimeUnit.MILLISECONDS);

        assertThat(hooksStarted.get()).isEqualTo(numberOfHooks);
        assertThat(hooksFinished.get()).isEqualTo(numberOfHooks);
        assertThat(maxConcurrency.get()).isEqualTo(10);
    }
}
