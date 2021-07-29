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
import org.graylog2.Configuration;
import org.graylog2.plugin.Message;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * This class represents a blocking FIFO queue accepting failure batches for further handling.
 * It should be used as an entry point for failure producers.
 *
 * The service was introduced for 2 essential reasons:
 *  1. To control pressure on the failure handling framework.
 *  2. To decouple failure producers from failure consumers.
 *
 * The capacity of the underlying queue is controlled by `failure_handling_queue_capacity` configuration
 * property. By default its value is 1000.
 */
@Singleton
public class FailureSubmissionService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final BlockingQueue<FailureBatch> queue;
    private final Configuration configuration;
    private final Meter submittedFailureBatches;
    private final Meter submittedFailures;
    private final Meter consumedFailureBatches;
    private final Meter consumedFailures;
    private final FailureHandlingConfiguration failureHandlingConfiguration;

    @Inject
    public FailureSubmissionService(Configuration configuration,
                                    MetricRegistry metricRegistry,
                                    FailureHandlingConfiguration failureHandlingConfiguration) {
        this.queue = new LinkedBlockingQueue<>(configuration.getFailureHandlingQueueCapacity());
        this.configuration = configuration;
        this.submittedFailureBatches = metricRegistry.meter(name(FailureSubmissionService.class, "submittedFailureBatches"));
        this.submittedFailures = metricRegistry.meter(name(FailureSubmissionService.class, "submittedFailures"));
        this.consumedFailureBatches = metricRegistry.meter(name(FailureSubmissionService.class, "consumedFailureBatches"));
        this.consumedFailures = metricRegistry.meter(name(FailureSubmissionService.class, "consumedFailures"));
        this.failureHandlingConfiguration = failureHandlingConfiguration;
    }

    public boolean keepFailedMessageDuplicate() {
        return failureHandlingConfiguration.keepFailedMessageDuplicate();
    }

    public boolean submitProcessingFailures() {
        return failureHandlingConfiguration.submitProcessingFailures();
    }

    /**
     * Submits a failure batch for handling. If the underlying queue is full,
     * the call will block until the queue is ready to accept new batches.
     */
    public void submitBlocking(FailureBatch batch) throws InterruptedException {
        queue.put(batch);

        if (queueSize() == configuration.getFailureHandlingQueueCapacity()) {
            logger.debug("The queue is full! Current capacity: {}", configuration.getFailureHandlingQueueCapacity());
        }

        submittedFailureBatches.mark();
        submittedFailures.mark(batch.size());
    }

    public void handleProcessingException(Message message, String failureContext, Exception e) {
        if (!submitProcessingFailures()) {
            return;
        }
        if (!keepFailedMessageDuplicate()) {
            message.setFilterOut(true);
        }
        submitFailure(message, failureContext, ExceptionUtils.getShortenedStackTrace(e));
    }

    public void handleProcessingFailure(Message message, String failureContext) {
        if (!submitProcessingFailures()) {
            // We don't handle processing errors
            return;
        }
        final String processingError = message.getFieldAs(String.class, Message.FIELD_GL2_PROCESSING_ERROR);
        if (processingError == null) {
            return;
        }
        if (!keepFailedMessageDuplicate()) {
            message.setFilterOut(true);
        }

        final Message failedMessage = new Message(message);
        failedMessage.removeField(Message.FIELD_GL2_PROCESSING_ERROR);
        submitFailure(failedMessage, failureContext, processingError);
    }

    private void submitFailure(Message failedMessage, String errorType, String error) {
        try {
            // If we store the regular message, the acknowledgement happens in the output path
            boolean needsAcknowledgement = !keepFailedMessageDuplicate();
            // TODO use message.getMesssgeId() once this field is set early in processing
            final ProcessingFailure processingFailure = new ProcessingFailure(failedMessage.getId(), errorType, error, failedMessage.getTimestamp(), failedMessage, needsAcknowledgement);
            final FailureBatch failureBatch = FailureBatch.processingFailureBatch(processingFailure);
            submitBlocking(failureBatch);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Logs current submission/consumption stats.
     */
    void logStats(String tag) {
        logger.info("[{}] Total number of submitted batches: {} ({} failures), total number of consumed batches: {} ({} failures)",
                tag,
                submittedFailureBatches.getCount(), submittedFailures.getCount(),
                consumedFailureBatches.getCount(), consumedFailures.getCount());
    }

    /**
     * @return one batch from the queue. If the queue is empty,
     * waits for a batch to become available.
     */
    FailureBatch consumeBlocking() throws InterruptedException {
        final FailureBatch fb = queue.take();
        consumedFailureBatches.mark();
        consumedFailures.mark(fb.size());
        return fb;
    }


    /**
     * @return one batch from the queue. If the queue is empty,
     * waits for the specified period of time for a batch to become available,
     * otherwise returns null.
     */
    @Nullable
    FailureBatch consumeBlockingWithTimeout(long timeoutInMs) throws InterruptedException {
        final FailureBatch fb = queue.poll(timeoutInMs, TimeUnit.MILLISECONDS);
        if (fb != null) {
            consumedFailureBatches.mark();
            consumedFailures.mark(fb.size());
        }
        return fb;
    }

    /**
     * @return the current amount of failure batches in the queue.
     */
    int queueSize() {
        return queue.size();
    }
}
