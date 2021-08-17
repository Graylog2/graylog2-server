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

import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class FailureSubmissionServiceTest {

    private final FailureSubmissionQueue failureSubmissionQueue = Mockito.mock(FailureSubmissionQueue.class);
    private final FailureHandlingConfiguration failureHandlingConfiguration = Mockito.mock(FailureHandlingConfiguration.class);
    private final FailureSubmissionService underTest = new FailureSubmissionService(failureSubmissionQueue, failureHandlingConfiguration);

    private final ArgumentCaptor<FailureBatch> failureBatchCaptor = ArgumentCaptor.forClass(FailureBatch.class);

    @Test
    public void submitIndexingErrors_allIndexingErrorsTransformedAndSubmittedToFailureQueue() throws Exception {
        // given
        final Message msg1 = Mockito.mock(Message.class);
        when(msg1.getMessageId()).thenReturn("msg-1");
        when(msg1.supportsFailureHandling()).thenReturn(true);
        final Message msg2 = Mockito.mock(Message.class);
        when(msg2.getMessageId()).thenReturn("msg-2");
        when(msg2.supportsFailureHandling()).thenReturn(true);

        final List<Messages.IndexingError> indexingErrors = ImmutableList.of(
                Messages.IndexingError.create(msg1, "index-1", Messages.IndexingError.ErrorType.MappingError, "Error"),
                Messages.IndexingError.create(msg2, "index-2", Messages.IndexingError.ErrorType.Unknown, "Error2")
        );

        // when
        underTest.submitIndexingErrors(indexingErrors);

        // then
        verify(failureSubmissionQueue, times(1)).submitBlocking(failureBatchCaptor.capture());

        assertThat(failureBatchCaptor.getValue()).satisfies(fb -> {
            assertThat(fb.containsIndexingFailures()).isTrue();
            assertThat(fb.size()).isEqualTo(2);

            assertThat(fb.getFailures().get(0)).satisfies(indexingFailure -> {
                assertThat(indexingFailure.failureType()).isEqualTo(FailureType.INDEXING);
                assertThat(indexingFailure.failureCause().label()).isEqualTo("MappingError");
                assertThat(indexingFailure.message()).isEqualTo("Failed to index message with id 'msg-1' targeting 'index-1'");
                assertThat(indexingFailure.failureDetails()).isEqualTo("Error");
                assertThat(indexingFailure.failureTimestamp()).isNotNull();
                assertThat(indexingFailure.failedMessage()).isEqualTo(msg1);
                assertThat(indexingFailure.targetIndex()).isEqualTo("index-1");
                assertThat(indexingFailure.requiresAcknowledgement()).isFalse();
            });

            assertThat(fb.getFailures().get(1)).satisfies(indexingFailure -> {
                assertThat(indexingFailure.failureType()).isEqualTo(FailureType.INDEXING);
                assertThat(indexingFailure.failureCause().label()).isEqualTo("UNKNOWN");
                assertThat(indexingFailure.message()).isEqualTo("Failed to index message with id 'msg-2' targeting 'index-2'");
                assertThat(indexingFailure.failureDetails()).isEqualTo("Error2");
                assertThat(indexingFailure.failureTimestamp()).isNotNull();
                assertThat(indexingFailure.failedMessage()).isEqualTo(msg2);
                assertThat(indexingFailure.targetIndex()).isEqualTo("index-2");
                assertThat(indexingFailure.requiresAcknowledgement()).isFalse();
            });
        });
    }

    @Test
    public void submitIndexingErrors_messageNotSupportingFailureHandlingNotSubmittedToQueue() throws Exception {
        // given
        final Message msg1 = Mockito.mock(Message.class);
        when(msg1.getMessageId()).thenReturn("msg-1");
        when(msg1.supportsFailureHandling()).thenReturn(false);
        final Message msg2 = Mockito.mock(Message.class);
        when(msg2.getMessageId()).thenReturn("msg-2");
        when(msg2.supportsFailureHandling()).thenReturn(false);

        final List<Messages.IndexingError> indexingErrors = ImmutableList.of(
                Messages.IndexingError.create(msg1, "index-1", Messages.IndexingError.ErrorType.MappingError, "Error"),
                Messages.IndexingError.create(msg2, "index-2", Messages.IndexingError.ErrorType.Unknown, "Error2")
        );

        // when
        underTest.submitIndexingErrors(indexingErrors);

        // then
        verifyNoInteractions(failureSubmissionQueue);
    }


    @Test
    public void submitProcessingErrors_allProcessingErrorsSubmittedToQueueAndMessageNotFilteredOut_ifSubmissionEnabledAndDuplicatesAreKept() throws Exception {
        // given
        final Message msg = Mockito.mock(Message.class);
        when(msg.getMessageId()).thenReturn("msg-x");
        when(msg.supportsFailureHandling()).thenReturn(true);

        when(msg.processingErrors()).thenReturn(ImmutableList.of(
                new Message.ProcessingError(() -> "Cause 1", "Message 1", "Details 1"),
                new Message.ProcessingError(() -> "Cause 2", "Message 2", "Details 2")
        ));

        when(failureHandlingConfiguration.submitProcessingFailures()).thenReturn(true);
        when(failureHandlingConfiguration.keepFailedMessageDuplicate()).thenReturn(true);

        // when
        final boolean notFilterOut = underTest.submitProcessingErrors(msg);

        // then

        assertThat(notFilterOut).isTrue();

        verify(failureSubmissionQueue, times(2)).submitBlocking(failureBatchCaptor.capture());

        assertThat(failureBatchCaptor.getAllValues().get(0)).satisfies(fb -> {
            assertThat(fb.containsProcessingFailures()).isTrue();
            assertThat(fb.size()).isEqualTo(1);

            assertThat(fb.getFailures().get(0)).satisfies(processingFailure -> {
                assertThat(processingFailure.failureType()).isEqualTo(FailureType.PROCESSING);
                assertThat(processingFailure.failureCause().label()).isEqualTo("Cause 1");
                assertThat(processingFailure.message()).isEqualTo("Failed to process message with id 'msg-x': Message 1");
                assertThat(processingFailure.failureDetails()).isEqualTo("Details 1");
                assertThat(processingFailure.failureTimestamp()).isNotNull();
                assertThat(processingFailure.failedMessage()).isEqualTo(msg);
                assertThat(processingFailure.targetIndex()).isNull();
                assertThat(processingFailure.requiresAcknowledgement()).isFalse();
            });
        });

        assertThat(failureBatchCaptor.getAllValues().get(1)).satisfies(fb -> {
            assertThat(fb.containsProcessingFailures()).isTrue();
            assertThat(fb.size()).isEqualTo(1);

            assertThat(fb.getFailures().get(0)).satisfies(processingFailure -> {
                assertThat(processingFailure.failureType()).isEqualTo(FailureType.PROCESSING);
                assertThat(processingFailure.failureCause().label()).isEqualTo("Cause 2");
                assertThat(processingFailure.message()).isEqualTo("Failed to process message with id 'msg-x': Message 2");
                assertThat(processingFailure.failureDetails()).isEqualTo("Details 2");
                assertThat(processingFailure.failureTimestamp()).isNotNull();
                assertThat(processingFailure.failedMessage()).isEqualTo(msg);
                assertThat(processingFailure.targetIndex()).isNull();
                assertThat(processingFailure.requiresAcknowledgement()).isFalse();
            });
        });
    }

    @Test
    public void submitProcessingErrors_nothingSubmittedAndMessageNotFilteredOut_ifSubmissionEnabledAndDuplicatesAreKeptAndMessageDoesntSupportFailureHandling() throws Exception {
        // given
        final Message msg = Mockito.mock(Message.class);
        when(msg.getMessageId()).thenReturn("msg-x");
        when(msg.supportsFailureHandling()).thenReturn(false);

        when(msg.processingErrors()).thenReturn(ImmutableList.of(
                new Message.ProcessingError(() -> "Cause 1", "Message 1", "Details 1"),
                new Message.ProcessingError(() -> "Cause 2", "Message 2", "Details 2")
        ));

        when(failureHandlingConfiguration.submitProcessingFailures()).thenReturn(true);
        when(failureHandlingConfiguration.keepFailedMessageDuplicate()).thenReturn(true);

        // when
        final boolean notFilterOut = underTest.submitProcessingErrors(msg);

        // then
        assertThat(notFilterOut).isTrue();

        verifyNoInteractions(failureSubmissionQueue);
    }


    @Test
    public void submitProcessingErrors_nothingSubmittedAndMessageNotFilteredOut_ifSubmissionDisabledAndDuplicatesAreKept() throws Exception {
        // given
        final Message msg = Mockito.mock(Message.class);
        when(msg.getMessageId()).thenReturn("msg-x");
        when(msg.supportsFailureHandling()).thenReturn(true);

        when(msg.processingErrors()).thenReturn(ImmutableList.of(
                new Message.ProcessingError(() -> "Cause 1", "Message 1", "Details 1"),
                new Message.ProcessingError(() -> "Cause 2", "Message 2", "Details 2")
        ));

        when(failureHandlingConfiguration.submitProcessingFailures()).thenReturn(false);
        when(failureHandlingConfiguration.keepFailedMessageDuplicate()).thenReturn(true);

        // when
        final boolean notFilterOut = underTest.submitProcessingErrors(msg);

        // then

        assertThat(notFilterOut).isTrue();

        verifyNoInteractions(failureSubmissionQueue);
    }

    @Test
    public void submitProcessingErrors_nothingSubmittedAndMessageNotFilteredOut_ifSubmissionDisabledAndDuplicatesAreNotKept() throws Exception {
        // given
        final Message msg = Mockito.mock(Message.class);
        when(msg.getMessageId()).thenReturn("msg-x");
        when(msg.supportsFailureHandling()).thenReturn(true);

        when(msg.processingErrors()).thenReturn(ImmutableList.of(
                new Message.ProcessingError(() -> "Cause 1", "Message 1", "Details 1"),
                new Message.ProcessingError(() -> "Cause 2", "Message 2", "Details 2")
        ));

        when(failureHandlingConfiguration.submitProcessingFailures()).thenReturn(false);
        when(failureHandlingConfiguration.keepFailedMessageDuplicate()).thenReturn(false);

        // when
        final boolean notFilterOut = underTest.submitProcessingErrors(msg);

        // then

        assertThat(notFilterOut).isTrue();

        verifyNoInteractions(failureSubmissionQueue);
    }

    @Test
    public void submitProcessingErrors_nothingSubmittedAndMessageNotFilteredOut_ifMessageHasNoErrors() throws Exception {
        // given
        final Message msg = Mockito.mock(Message.class);
        when(msg.getMessageId()).thenReturn("msg-x");
        when(msg.supportsFailureHandling()).thenReturn(true);
        when(msg.processingErrors()).thenReturn(ImmutableList.of());

        when(failureHandlingConfiguration.submitProcessingFailures()).thenReturn(true);
        when(failureHandlingConfiguration.keepFailedMessageDuplicate()).thenReturn(false);

        // when
        final boolean notFilterOut = underTest.submitProcessingErrors(msg);

        // then

        assertThat(notFilterOut).isTrue();

        verifyNoInteractions(failureSubmissionQueue);
    }


    @Test
    public void submitProcessingErrors_processingErrorSubmittedToQueueAndMessageFilteredOut_ifSubmissionEnabledAndDuplicatesAreNotKept() throws Exception {
        // given
        final Message msg = Mockito.mock(Message.class);
        when(msg.getMessageId()).thenReturn("msg-x");
        when(msg.supportsFailureHandling()).thenReturn(true);
        when(msg.processingErrors()).thenReturn(ImmutableList.of(
                new Message.ProcessingError(() -> "Cause", "Message", "Details")
        ));

        when(failureHandlingConfiguration.submitProcessingFailures()).thenReturn(true);
        when(failureHandlingConfiguration.keepFailedMessageDuplicate()).thenReturn(false);

        // when
        final boolean notFilterOut = underTest.submitProcessingErrors(msg);

        // then

        assertThat(notFilterOut).isFalse();

        verify(msg).setFilterOut(true);

        verify(failureSubmissionQueue, times(1)).submitBlocking(failureBatchCaptor.capture());

        assertThat(failureBatchCaptor.getValue()).satisfies(fb -> {
            assertThat(fb.containsProcessingFailures()).isTrue();
            assertThat(fb.size()).isEqualTo(1);

            assertThat(fb.getFailures().get(0)).satisfies(processingFailure -> {
                assertThat(processingFailure.failureType()).isEqualTo(FailureType.PROCESSING);
                assertThat(processingFailure.failureCause().label()).isEqualTo("Cause");
                assertThat(processingFailure.message()).isEqualTo("Failed to process message with id 'msg-x': Message");
                assertThat(processingFailure.failureDetails()).isEqualTo("Details");
                assertThat(processingFailure.failureTimestamp()).isNotNull();
                assertThat(processingFailure.failedMessage()).isEqualTo(msg);
                assertThat(processingFailure.targetIndex()).isNull();
                assertThat(processingFailure.requiresAcknowledgement()).isTrue();
            });
        });
    }

    @Test
    public void submitUnknownProcessingError_unknownProcessingErrorSubmittedToQueue() throws Exception {
        // given
        final Message msg = Mockito.mock(Message.class);
        when(msg.processingErrors()).thenReturn(ImmutableList.of());
        when(msg.supportsFailureHandling()).thenReturn(true);

        when(failureHandlingConfiguration.submitProcessingFailures()).thenReturn(true);
        when(failureHandlingConfiguration.keepFailedMessageDuplicate()).thenReturn(true);

        // when
        final boolean notFilterOut = underTest.submitUnknownProcessingError(msg, "Details of the unknown error!");

        // then

        assertThat(notFilterOut).isTrue();

        verify(failureSubmissionQueue, times(1)).submitBlocking(failureBatchCaptor.capture());

        assertThat(failureBatchCaptor.getValue()).satisfies(fb -> {
            assertThat(fb.containsProcessingFailures()).isTrue();
            assertThat(fb.size()).isEqualTo(1);

            assertThat(fb.getFailures().get(0)).satisfies(processingFailure -> {
                assertThat(processingFailure.failureType()).isEqualTo(FailureType.PROCESSING);
                assertThat(processingFailure.failureCause().label()).isEqualTo("UNKNOWN");
                assertThat(processingFailure.message()).isEqualTo("Failed to process a message with unknown id: Encountered an unrecognizable processing error");
                assertThat(processingFailure.failureDetails()).isEqualTo("Details of the unknown error!");
                assertThat(processingFailure.failureTimestamp()).isNotNull();
                assertThat(processingFailure.failedMessage()).isEqualTo(msg);
                assertThat(processingFailure.targetIndex()).isNull();
                assertThat(processingFailure.requiresAcknowledgement()).isFalse();
            });
        });

    }
}
