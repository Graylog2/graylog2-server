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
import org.apache.commons.lang3.StringUtils;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * A supplementary service layer, which is aimed to simplify failure
 * submission for the calling code. Apart from the <b>input transformation</b>,
 * it also encapsulates integration with <b>the failure handling configuration</b>.
 */
@Singleton
public class FailureSubmissionService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final FailureSubmissionQueue failureSubmissionQueue;
    private final FailureHandlingConfiguration failureHandlingConfiguration;

    @Inject
    public FailureSubmissionService(
            FailureSubmissionQueue failureSubmissionQueue,
            FailureHandlingConfiguration failureHandlingConfiguration) {
        this.failureSubmissionQueue = failureSubmissionQueue;
        this.failureHandlingConfiguration = failureHandlingConfiguration;
    }

    /**
     * Submits an unrecognized processing error to the failure queue.
     * Depending on the configuration might ignore the error
     *
     * Must be called the last in the processing chain!
     *
     * @param message a problematic message
     * @param details error details
     * @return true if the message is not filtered out
     */
    public boolean submitUnknownProcessingError(Message message, String details) {
        return submitProcessingErrorsInternal(message, ImmutableList.of(new Message.ProcessingError(
                ProcessingFailureCause.UNKNOWN,
                "Encountered an unrecognizable processing error",
                details)));
    }

    /**
     * Submits message's processing errors to the failure queue. The errors
     * are obtained via {@link Message#processingErrors()}. Depending on the
     * configuration might ignore the errors.
     *
     * Must be called the last in the processing chain!
     *
     * @param message a message with processing errors
     * @return true if the message is not filtered out
     */
    public boolean submitProcessingErrors(Message message) {
        return submitProcessingErrorsInternal(message, message.processingErrors());
    }

    private boolean submitProcessingErrorsInternal(Message message, List<Message.ProcessingError> processingErrors) {
        if (processingErrors.isEmpty()) {
            return true;
        }

        if (!message.supportsFailureHandling()) {
            logger.warn("Submitted a message with processing errors, which doesn't support failure handling!");
            return true;
        }

        if (!failureHandlingConfiguration.submitProcessingFailures()) {
            // We don't handle processing errors
            return true;
        }

        if (!failureHandlingConfiguration.keepFailedMessageDuplicate()) {
            message.setFilterOut(true);
        }

        processingErrors.forEach(pe -> submitProcessingFailure(message, pe));

        return failureHandlingConfiguration.keepFailedMessageDuplicate();
    }

    private void submitProcessingFailure(Message failedMessage, Message.ProcessingError processingError) {
        try {
            // If we store the regular message, the acknowledgement happens in the output path
            final boolean needsAcknowledgement = !failureHandlingConfiguration.keepFailedMessageDuplicate();

            final String messageId = StringUtils.isBlank(failedMessage.getMessageId()) ? failedMessage.getId() : failedMessage.getMessageId();

            final String message = String.format(Locale.ENGLISH,
                    "Failed to process message with id '%s': %s",
                    StringUtils.isBlank(messageId) ? "UNKNOWN" : messageId,
                    processingError.getMessage());

            final ProcessingFailure processingFailure = new ProcessingFailure(
                    processingError.getCause(),
                    message,
                    processingError.getDetails(),
                    Tools.nowUTC(),
                    failedMessage,
                    needsAcknowledgement);

            failureSubmissionQueue.submitBlocking(FailureBatch.processingFailureBatch(processingFailure));
        } catch (InterruptedException ignored) {
            logger.warn("Failed to submit a processing failure for failure handling. The thread has been interrupted!");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Submits Elasticsearch indexing errors to the failure queue
     * @param indexingErrors a collection of indexing errors
     */
    public void submitIndexingErrors(Collection<Messages.IndexingError> indexingErrors) {
        try {
            final FailureBatch fb = FailureBatch.indexingFailureBatch(
                    indexingErrors.stream()
                            .filter(ie -> {
                                if (!ie.message().supportsFailureHandling()) {
                                    logger.warn("Submitted a message with indexing errors, which doesn't support failure handling!");
                                    return false;
                                } else {
                                    return true;
                                }
                            })
                            .map(this::fromIndexingError)
                            .collect(Collectors.toList()));

            if (fb.size() > 0) {
                failureSubmissionQueue.submitBlocking(fb);
            }

        } catch (InterruptedException ignored) {
            logger.warn("Failed to submit {} indexing errors for failure handling. The thread has been interrupted!",
                    indexingErrors.size());
            Thread.currentThread().interrupt();
        }
    }

    private IndexingFailure fromIndexingError(Messages.IndexingError indexingError) {
        return new IndexingFailure(
                indexingError.errorType() == Messages.IndexingError.ErrorType.MappingError ?
                        IndexingFailureCause.MappingError : IndexingFailureCause.UNKNOWN,
                String.format(Locale.ENGLISH,
                        "Failed to index message with id '%s' targeting '%s'",
                        indexingError.message().getMessageId(), indexingError.index()),
                indexingError.errorMessage(),
                Tools.nowUTC(),
                indexingError.message(),
                indexingError.index()
        );
    }
}
