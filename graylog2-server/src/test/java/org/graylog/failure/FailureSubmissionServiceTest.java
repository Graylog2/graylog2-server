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
package org.graylog.failure;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.Configuration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FailureSubmissionServiceTest {

    private final Configuration configuration = mock(Configuration.class);

    private final MetricRegistry metricRegistry = new MetricRegistry();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder()
            .setNameFormat("failure-scheduled-%d")
            .setDaemon(false)
            .build());

    @Test
    public void submitBlocking_whenQueueNotFull_acceptsNewBatches() throws Exception {
        //given
        when(configuration.getFailureHandlingQueueCapacity()).thenReturn(1000);

        final FailureSubmissionService underTest = new FailureSubmissionService(configuration, metricRegistry);

        final ProcessingFailure prcFailure1 = createProcessingFailure();
        final ProcessingFailure prcFailure2 = createProcessingFailure();

        // when
        underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure1));
        underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure2));

        // then
        assertThat(underTest.queueSize()).isEqualTo(2);
        assertThat(underTest.consumeBlocking()).isEqualTo(FailureBatch.processingFailureBatch(prcFailure1));
        assertThat(underTest.consumeBlocking()).isEqualTo(FailureBatch.processingFailureBatch(prcFailure2));
    }

    @Test
    public void submitBlocking_whenQueueIsFull_submissionIsBlocked() throws Exception {
        //given
        when(configuration.getFailureHandlingQueueCapacity()).thenReturn(2);

        final FailureSubmissionService underTest = new FailureSubmissionService(configuration, metricRegistry);

        final ProcessingFailure prcFailure1 = createProcessingFailure();
        final ProcessingFailure prcFailure2 = createProcessingFailure();
        final ProcessingFailure prcFailure3 = createProcessingFailure();

        underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure1));
        underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure2));

        // when
        scheduler.schedule(() -> {
            assertThat(underTest.queueSize()).isEqualTo(2);
            try {
                assertThat(underTest.consumeBlocking()).isEqualTo(FailureBatch.processingFailureBatch(prcFailure1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 300, TimeUnit.MILLISECONDS);

        final long started = System.currentTimeMillis();
        underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure3));
        final long waited = System.currentTimeMillis() - started;

        // then
        assertThat(waited).isGreaterThan(200);
        assertThat(underTest.queueSize()).isEqualTo(2);
        assertThat(underTest.consumeBlocking()).isEqualTo(FailureBatch.processingFailureBatch(prcFailure2));
        assertThat(underTest.consumeBlocking()).isEqualTo(FailureBatch.processingFailureBatch(prcFailure3));

    }

    @Test
    public void consumeBlocking_waitsForBatch_whenQueueIsEmpty() throws Exception {
        //given
        when(configuration.getFailureHandlingQueueCapacity()).thenReturn(2);

        final FailureSubmissionService underTest = new FailureSubmissionService(configuration, metricRegistry);

        final ProcessingFailure prcFailure1 = createProcessingFailure();

        // when
        scheduler.schedule(() -> {
            try {
                underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 300, TimeUnit.MILLISECONDS);

        final long started = System.currentTimeMillis();
        final FailureBatch consumedBatch = underTest.consumeBlocking();
        final long waited = System.currentTimeMillis() - started;

        // then
        assertThat(waited).isGreaterThan(200);
        assertThat(consumedBatch).isEqualTo(FailureBatch.processingFailureBatch(prcFailure1));
    }

    @Test
    public void consumeBlockingWithTimeout_returnsBatch_whenSubmittedWithinWaitingTimeout() throws Exception {
        //given
        when(configuration.getFailureHandlingQueueCapacity()).thenReturn(2);

        final FailureSubmissionService underTest = new FailureSubmissionService(configuration, metricRegistry);

        final ProcessingFailure prcFailure1 = createProcessingFailure();

        // when
        scheduler.schedule(() -> {
            try {
                underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 300, TimeUnit.MILLISECONDS);

        final long started = System.currentTimeMillis();
        final FailureBatch consumedBatch = underTest.consumeBlockingWithTimeout(500);
        final long waited = System.currentTimeMillis() - started;

        // then
        assertThat(waited).isGreaterThan(200);
        assertThat(consumedBatch).isEqualTo(FailureBatch.processingFailureBatch(prcFailure1));
    }

    @Test
    public void consumeBlockingWithTimeout_returnsNull_whenReachedWaitingTimeout() throws Exception {
        //given
        when(configuration.getFailureHandlingQueueCapacity()).thenReturn(2);

        final FailureSubmissionService underTest = new FailureSubmissionService(configuration, metricRegistry);

        final ProcessingFailure prcFailure1 = createProcessingFailure();

        // when
        scheduler.schedule(() -> {
            try {
                underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 300, TimeUnit.MILLISECONDS);

        final FailureBatch consumedBatch = underTest.consumeBlockingWithTimeout(50);

        // then
        assertThat(consumedBatch).isNull();
    }

    private ProcessingFailure createProcessingFailure() {
        return new ProcessingFailure(
                UUID.randomUUID().toString(), "error-type", "error-message",
                DateTime.now(DateTimeZone.UTC), null,
                true);
    }
}
