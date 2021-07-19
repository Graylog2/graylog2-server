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
import org.graylog2.Configuration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FailureSubmissionServiceTest {

    private final Configuration configuration = mock(Configuration.class);

    @Test
    public void submitBlocking_whenTheServiceIsUp_itAcceptsNewBatches() throws Exception {
        //given
        when(configuration.getFailureHandlingQueueCapacity()).thenReturn(1000);

        final FailureSubmissionService underTest = new FailureSubmissionService(configuration, new MetricRegistry());

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
    public void submitBlocking_whenTheServiceIsDown_noNewBatchesAccepted() throws Exception {
        //given
        when(configuration.getFailureHandlingQueueCapacity()).thenReturn(1000);

        final FailureSubmissionService underTest = new FailureSubmissionService(configuration, new MetricRegistry());

        final ProcessingFailure prcFailure1 = createProcessingFailure();
        final ProcessingFailure prcFailure2 = createProcessingFailure();

        underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure1));
        underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure2));

        // when
        underTest.shutDown();

        underTest.submitBlocking(FailureBatch.processingFailureBatch(createProcessingFailure()));

        // then
        assertThat(underTest.queueSize()).isEqualTo(2);
        assertThat(underTest.consumeBlocking()).isEqualTo(FailureBatch.processingFailureBatch(prcFailure1));
        assertThat(underTest.consumeBlocking()).isEqualTo(FailureBatch.processingFailureBatch(prcFailure2));
    }

    @Test
    public void submitBlocking_whenTheQueueIsFull_submissionIsBlocked() throws Exception {
        //given
        when(configuration.getFailureHandlingQueueCapacity()).thenReturn(2);

        final FailureSubmissionService underTest = new FailureSubmissionService(configuration, new MetricRegistry());

        final ProcessingFailure prcFailure1 = createProcessingFailure();
        final ProcessingFailure prcFailure2 = createProcessingFailure();
        final ProcessingFailure prcFailure3 = createProcessingFailure();

        underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure1));
        underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure2));

        // when
        Executors.newScheduledThreadPool(1).schedule(() -> {
            assertThat(underTest.queueSize()).isEqualTo(2);
            try {
                assertThat(underTest.consumeBlocking()).isEqualTo(FailureBatch.processingFailureBatch(prcFailure1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 300, TimeUnit.MILLISECONDS);

        final long stared = System.currentTimeMillis();
        underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure3));
        final long waited = System.currentTimeMillis() - stared;

        // then
        assertThat(waited).isBetween(300L, 500L);
        assertThat(underTest.queueSize()).isEqualTo(2);
        assertThat(underTest.consumeBlocking()).isEqualTo(FailureBatch.processingFailureBatch(prcFailure2));
        assertThat(underTest.consumeBlocking()).isEqualTo(FailureBatch.processingFailureBatch(prcFailure3));

    }

    @Test
    public void drain_mustNotBeCalled_whenTheServiceIsUp() {
        //given
        when(configuration.getFailureHandlingQueueCapacity()).thenReturn(2);

        final FailureSubmissionService underTest = new FailureSubmissionService(configuration, new MetricRegistry());

        // when + then
        assertThatCode(underTest::drain).isExactlyInstanceOf(IllegalStateException.class);
    }


    @Test
    public void drain_returnsRemainingBatches_whenTheServiceIsDown() throws Exception {
        //given
        when(configuration.getFailureHandlingQueueCapacity()).thenReturn(3);

        final FailureSubmissionService underTest = new FailureSubmissionService(configuration, new MetricRegistry());

        final ProcessingFailure prcFailure1 = createProcessingFailure();
        final ProcessingFailure prcFailure2 = createProcessingFailure();
        final ProcessingFailure prcFailure3 = createProcessingFailure();

        underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure1));
        underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure2));
        underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure3));

        // when + then
        underTest.consumeBlocking();

        underTest.shutDown();

        final List<FailureBatch> drained = underTest.drain();
        assertThat(drained).containsExactly(FailureBatch.processingFailureBatch(prcFailure2), FailureBatch.processingFailureBatch(prcFailure3));
    }

    @Test
    public void consumeBlocking_waitsForBatch_whenTheQueueIsEmpty() throws Exception {
        //given
        when(configuration.getFailureHandlingQueueCapacity()).thenReturn(2);

        final FailureSubmissionService underTest = new FailureSubmissionService(configuration, new MetricRegistry());

        final ProcessingFailure prcFailure1 = createProcessingFailure();

        // when
        Executors.newScheduledThreadPool(1).schedule(() -> {
            try {
                underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 300, TimeUnit.MILLISECONDS);

        final long stared = System.currentTimeMillis();
        final FailureBatch consumedBatch = underTest.consumeBlocking();
        final long waited = System.currentTimeMillis() - stared;

        // then
        assertThat(waited).isBetween(300L, 500L);
        assertThat(consumedBatch).isEqualTo(FailureBatch.processingFailureBatch(prcFailure1));
    }


    @Test
    public void consumeBlocking_returnsBatches_whenTheServiceIsDown() throws Exception {
        //given
        when(configuration.getFailureHandlingQueueCapacity()).thenReturn(2);

        final FailureSubmissionService underTest = new FailureSubmissionService(configuration, new MetricRegistry());

        final ProcessingFailure prcFailure1 = createProcessingFailure();
        underTest.submitBlocking(FailureBatch.processingFailureBatch(prcFailure1));

        // when + them
        underTest.shutDown();

        assertThat(underTest.consumeBlocking()).isEqualTo(FailureBatch.processingFailureBatch(prcFailure1));
    }

    private ProcessingFailure createProcessingFailure() {
        return new ProcessingFailure(
                UUID.randomUUID().toString(), "error-type", "error-message",
                DateTime.now(DateTimeZone.UTC), null
        );
    }
}
