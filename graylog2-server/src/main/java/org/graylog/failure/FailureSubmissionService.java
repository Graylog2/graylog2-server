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

import org.graylog2.indexer.messages.Indexable;
import org.graylog2.indexer.messages.Messages.IndexingError;
import org.graylog2.plugin.Message;
import org.graylog2.shared.utilities.ExceptionUtils;

import javax.inject.Inject;
import java.util.Collection;
import java.util.stream.Collectors;

public class FailureSubmissionService {

    private final FailureSubmissionQueue failureSubmissionQueue;
    private final FailureHandlingConfiguration failureHandlingConfiguration;

    @Inject
    public FailureSubmissionService(FailureSubmissionQueue failureSubmissionQueue, FailureHandlingConfiguration failureHandlingConfiguration) {
        this.failureSubmissionQueue = failureSubmissionQueue;
        this.failureHandlingConfiguration = failureHandlingConfiguration;
    }

    public void handleIndexingErrors(Collection<IndexingError> indexingErrors) throws InterruptedException {
        failureSubmissionQueue.submitBlocking(FailureBatch.indexingFailureBatch(
                indexingErrors.stream()
                        .map(this::fromIndexingError)
                        .collect(Collectors.toList())));

    }

    private IndexingFailure fromIndexingError(IndexingError indexingError) {
        final Indexable message = indexingError.message();
        return new IndexingFailure(
                message.getMessageId(),
                indexingError.index(),
                indexingError.errorType().toString(),
                indexingError.errorMessage(),
                message.getTimestamp(),
                message
        );
    }

    public void handleProcessingException(Message message, String failureContext, Exception e) {
        if (!failureHandlingConfiguration.submitProcessingFailures()) {
            return;
        }
        if (!failureHandlingConfiguration.keepFailedMessageDuplicate()) {
            message.setFilterOut(true);
        }
        submitProcessingFailure(message, failureContext, ExceptionUtils.getShortenedStackTrace(e));
    }

    public void handleProcessingFailure(Message message, String failureContext) {
        if (!failureHandlingConfiguration.submitProcessingFailures()) {
            // We don't handle processing errors
            return;
        }
        final String processingError = message.getFieldAs(String.class, Message.FIELD_GL2_PROCESSING_ERROR);
        if (processingError == null) {
            return;
        }
        if (!failureHandlingConfiguration.keepFailedMessageDuplicate()) {
            message.setFilterOut(true);
        }

        final Message failedMessage = new Message(message);
        failedMessage.removeField(Message.FIELD_GL2_PROCESSING_ERROR);
        submitProcessingFailure(failedMessage, failureContext, processingError);
    }

    private void submitProcessingFailure(Message failedMessage, String errorType, String error) {
        try {
            // If we store the regular message, the acknowledgement happens in the output path
            boolean needsAcknowledgement = !failureHandlingConfiguration.keepFailedMessageDuplicate();
            // TODO use message.getMesssgeId() once this field is set early in processing
            final ProcessingFailure processingFailure = new ProcessingFailure(failedMessage.getId(), errorType, error, failedMessage.getTimestamp(), failedMessage, needsAcknowledgement);
            final FailureBatch failureBatch = FailureBatch.processingFailureBatch(processingFailure);
            failureSubmissionQueue.submitBlocking(failureBatch);
        } catch (InterruptedException ignored) {
        }
    }
}
