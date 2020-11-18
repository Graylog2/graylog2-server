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
package org.graylog2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.graylog2.log4j.MemoryAppender;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class MemoryAppenderTest {
    @Test
    public void testGetLogMessages() {
        final int bufferSize = 10;
        final MemoryAppender appender = MemoryAppender.createAppender(null, null, "memory", String.valueOf(bufferSize), "false");
        assertThat(appender).isNotNull();

        for (int i = 1; i <= bufferSize; i++) {
            final LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setLevel(Level.INFO)
                .setLoggerName("test")
                .setLoggerFqcn("com.example.test")
                .setMessage(new SimpleMessage("Message " + i))
                .build();

            appender.append(logEvent);
        }

        assertThat(appender.getLogMessages(bufferSize * 2)).hasSize(bufferSize);
        assertThat(appender.getLogMessages(bufferSize)).hasSize(bufferSize);
        assertThat(appender.getLogMessages(bufferSize / 2)).hasSize(bufferSize / 2);
        assertThat(appender.getLogMessages(0)).isEmpty();

        final List<LogEvent> messages = appender.getLogMessages(5);
        for (int i = 0; i < messages.size(); i++) {
            assertThat(messages.get(i).getMessage().getFormattedMessage()).isEqualTo("Message " + (bufferSize - i));
        }
    }

    @Test
    public void appenderCanConsumeMoreMessagesThanBufferSize() {
        final int bufferSize = 10;
        final MemoryAppender appender = MemoryAppender.createAppender(null, null, "memory", String.valueOf(bufferSize), "false");
        assertThat(appender).isNotNull();

        for (int i = 1; i <= bufferSize + 1; i++) {
            final LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setLevel(Level.INFO)
                .setLoggerName("test")
                .setLoggerFqcn("com.example.test")
                .setMessage(new SimpleMessage("Message " + i))
                .build();


            appender.append(logEvent);
        }

        final List<LogEvent> messages = appender.getLogMessages(bufferSize);
        for (int i = 0; i < messages.size(); i++) {
            assertThat(messages.get(i).getMessage().getFormattedMessage()).isEqualTo("Message " + (bufferSize - i + 1));
        }
    }

    @Test
    public void appenderIsThreadSafe() throws Exception {
        final int bufferSize = 1;
        final MemoryAppender appender = MemoryAppender.createAppender(null, null, "memory", String.valueOf(bufferSize), "false");
        assertThat(appender).isNotNull();

        final LogEvent logEvent = Log4jLogEvent.newBuilder()
            .setLevel(Level.INFO)
            .setLoggerName("test")
            .setLoggerFqcn("com.example.test")
            .setMessage(new SimpleMessage("Message"))
            .build();

        final int threadCount = 48;
        final Thread[] threads = new Thread[threadCount];
        final TestAwareThreadGroup threadGroup = new TestAwareThreadGroup("memory-appender-test");
        final CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < threadCount; i++) {
            final Runnable runner = () -> {
                try {
                    latch.await();
                    long start = System.currentTimeMillis();
                    while (System.currentTimeMillis() - start < TimeUnit.SECONDS.toMillis(4L)) {
                        appender.append(logEvent);
                    }
                } catch (InterruptedException ie) {
                    // Do nothing
                }
            };
            final Thread thread = new Thread(threadGroup, runner, "TestThread-" + i);
            threads[i] = thread;
            thread.start();
        }
        latch.countDown();

        for (int i = 0; i < threadCount; i++) {
            threads[i].join(TimeUnit.SECONDS.toMillis(5L));
        }

        assertThat(threadGroup.getExceptionsInThreads().get()).isEqualTo(0);
    }

    private final static class TestAwareThreadGroup extends ThreadGroup {
        private final AtomicInteger exceptionsInThreads = new AtomicInteger(0);

        public TestAwareThreadGroup(String name) {
            super(name);
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            exceptionsInThreads.incrementAndGet();
            super.uncaughtException(t, e);
        }

        public AtomicInteger getExceptionsInThreads() {
            return exceptionsInThreads;
        }
    }
}
