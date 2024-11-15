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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.indexer.messages.Indexable;
import org.graylog2.indexer.messages.IndexingError;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;
import static org.graylog2.indexer.messages.IndexingError.Type.MappingError;
import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_INPUT;

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

    private final MetricRegistry metricRegistry;
    private final ObjectMapper objectMapper;
    private final Meter dummyMeter = new Meter();

    @Inject
    public FailureSubmissionService(
            FailureSubmissionQueue failureSubmissionQueue,
            FailureHandlingConfiguration failureHandlingConfiguration,
            MetricRegistry metricRegistry,
            ObjectMapperProvider objectMapperProvider) {
        this.failureSubmissionQueue = failureSubmissionQueue;
        this.failureHandlingConfiguration = failureHandlingConfiguration;
        this.metricRegistry = metricRegistry;
        this.objectMapper = objectMapperProvider.get();
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

        updateProcessingFailureMetric(message);

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
     *
     * @param indexingErrors a collection of indexing errors
     */
    public void submitIndexingErrors(Collection<IndexingError> indexingErrors) {
        try {
            indexingErrors.forEach(ie -> updateIndexingFailureMetric(ie.message()));

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
                            .toList());

            if (fb.size() > 0) {
                failureSubmissionQueue.submitBlocking(fb);
            }

        } catch (InterruptedException ignored) {
            logger.warn("Failed to submit {} indexing errors for failure handling. The thread has been interrupted!",
                    indexingErrors.size());
            Thread.currentThread().interrupt();
        }
    }

    private IndexingFailure fromIndexingError(IndexingError indexingError) {
        return new IndexingFailure(
                indexingError.error().type() == MappingError ?
                        IndexingFailureCause.MappingError : IndexingFailureCause.UNKNOWN,
                String.format(Locale.ENGLISH,
                        "Failed to index message with id '%s' targeting '%s'",
                        indexingError.message().getMessageId(), indexingError.index()),
                indexingError.error().errorMessage(),
                Tools.nowUTC(),
                indexingError.message(),
                indexingError.index()
        );
    }

    private void updateProcessingFailureMetric(Message message) {
        Object inputId = message.getField(FIELD_GL2_SOURCE_INPUT);
        if (inputId != null) {
            final String indexingFailureMetricName = name("org.graylog2.inputs", inputId.toString(), "failures.processing");
            metricRegistry.meter(indexingFailureMetricName).mark();
        }
    }

    private void updateIndexingFailureMetric(Indexable message) {
        final Map<String, Object> searchObject = message.toElasticSearchObject(objectMapper, dummyMeter);
        Object inputId = searchObject.get(FIELD_GL2_SOURCE_INPUT);
        if (inputId != null) {
            final String indexingFailureMetricName = name("org.graylog2.inputs", inputId.toString(), "failures.indexing");
            metricRegistry.meter(indexingFailureMetricName).mark();
        }
    }
}
