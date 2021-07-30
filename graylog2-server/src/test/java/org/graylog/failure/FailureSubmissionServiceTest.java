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

import com.google.common.collect.ImmutableList;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FailureSubmissionServiceTest {

    private final FailureSubmissionQueue failureSubmissionQueue = mock(FailureSubmissionQueue.class);
    private final FailureHandlingConfiguration handlingConfiguration = mock(FailureHandlingConfiguration.class);

    @Test
    public void processingFailureWithoutKeepDuplicate() throws InterruptedException {
        when(handlingConfiguration.submitProcessingFailures()).thenReturn(true);
        when(handlingConfiguration.keepFailedMessageDuplicate()).thenReturn(false);

        final FailureSubmissionService underTest = new FailureSubmissionService(failureSubmissionQueue, handlingConfiguration);

        final Message message = new Message("message", "source", new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC));
        message.addProcessingError("pipeline rule 123 failed");

        underTest.handleProcessingFailure(message, "test");

        verify(failureSubmissionQueue, times(1)).submitBlocking(argThat(argument ->
        {
            assertThat(argument.getFailures()).hasSize(1);
            return true;
        }));

        assertThat(message.getFilterOut()).isTrue();
    }

    @Test
    public void processingFailureWithKeepDuplicate() throws InterruptedException {
        when(handlingConfiguration.submitProcessingFailures()).thenReturn(true);
        when(handlingConfiguration.keepFailedMessageDuplicate()).thenReturn(true);

        final FailureSubmissionService underTest = new FailureSubmissionService(failureSubmissionQueue, handlingConfiguration);

        final Message message = new Message("message", "source", new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC));
        message.addProcessingError("pipeline rule 123 failed");

        underTest.handleProcessingFailure(message, "test");

        verify(failureSubmissionQueue, times(1)).submitBlocking(argThat(argument ->
        {
            assertThat(argument.getFailures()).hasSize(1);
            return true;
        }));

        assertThat(message.getFilterOut()).isFalse();
    }

    @Test
    public void processingExceptions() throws InterruptedException {
        when(handlingConfiguration.submitProcessingFailures()).thenReturn(true);
        when(handlingConfiguration.keepFailedMessageDuplicate()).thenReturn(false);

        final FailureSubmissionService underTest = new FailureSubmissionService(failureSubmissionQueue, handlingConfiguration);

        final Message message = new Message("message", "source", new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC));

        underTest.handleProcessingException(message, "test", new RuntimeException("bad processing"));

        verify(failureSubmissionQueue, times(1)).submitBlocking(argThat(argument ->
        {
            assertThat(argument.getFailures()).satisfies(failures -> {
                assertThat(failures).hasSize(1);
                assertThat(failures.iterator().next().errorMessage()).contains("RuntimeException: bad processing");
                assertThat(failures.iterator().next().context()).isEqualTo("test");
                assertThat(failures.iterator().next().failureType()).isEqualTo(FailureType.PROCESSING);
                assertThat(failures.iterator().next().requiresAcknowledgement()).isTrue();
            });
            return true;
        }));
        assertThat(message.getFilterOut()).isTrue();
    }

    @Test
    public void disabledProcessingExceptions() throws InterruptedException {
        when(handlingConfiguration.submitProcessingFailures()).thenReturn(false);

        final FailureSubmissionService underTest = new FailureSubmissionService(failureSubmissionQueue, handlingConfiguration);

        final Message message = new Message("message", "source", new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC));

        underTest.handleProcessingException(message, "test", new RuntimeException("bad processing"));

        verify(failureSubmissionQueue, times(0)).submitBlocking(any());
        assertThat(message.getFilterOut()).isFalse();
    }

    @Test
    public void handleIndexingErrors() throws InterruptedException {
        final FailureSubmissionService underTest = new FailureSubmissionService(failureSubmissionQueue, handlingConfiguration);

        final Message message = new Message("message", "source", new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC));
        final Messages.IndexingError indexingError = mock(Messages.IndexingError.class, invocation -> {
            if (String.class.equals(invocation.getMethod().getReturnType())) {
                return "dummy";
            } else {
                return RETURNS_DEFAULTS.answer(invocation);
            }
        });
        when(indexingError.message()).thenReturn(message);
        when(indexingError.errorType()).thenReturn(Messages.IndexingError.ErrorType.MappingError);

        final ImmutableList<Messages.IndexingError> indexingErrors = ImmutableList.of(indexingError, indexingError);
        underTest.handleIndexingErrors(indexingErrors);

        verify(failureSubmissionQueue, times(1)).submitBlocking(any());
    }
}
