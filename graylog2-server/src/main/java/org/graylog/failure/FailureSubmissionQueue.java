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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * A blocking FIFO queue accepting failure batches for further handling.
 * It should be used as an entry point for failure producers.
 *
 * The queue was introduced for 2 essential reasons:
 * 1. To control pressure on the failure handling framework.
 * 2. To decouple failure producers from failure consumers.
 *
 * The capacity of the underlying queue is controlled by {@link Configuration#getFailureHandlingQueueCapacity()}}
 */
@Singleton
class FailureSubmissionQueue {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final BlockingQueue<FailureBatch> queue;
    private final Configuration configuration;
    private final Meter submittedFailureBatches;
    private final Meter submittedFailures;
    private final Meter consumedFailureBatches;
    private final Meter consumedFailures;

    @Inject
    FailureSubmissionQueue(Configuration configuration,
                           MetricRegistry metricRegistry) {
        this.queue = new LinkedBlockingQueue<>(configuration.getFailureHandlingQueueCapacity());
        this.configuration = configuration;

        this.submittedFailureBatches = metricRegistry.meter(name(FailureSubmissionQueue.class, "submittedFailureBatches"));
        this.submittedFailures = metricRegistry.meter(name(FailureSubmissionQueue.class, "submittedFailures"));
        this.consumedFailureBatches = metricRegistry.meter(name(FailureSubmissionQueue.class, "consumedFailureBatches"));
        this.consumedFailures = metricRegistry.meter(name(FailureSubmissionQueue.class, "consumedFailures"));

        metricRegistry.register(MetricRegistry.name(FailureSubmissionQueue.class, "queueSize"),
                (Gauge<Integer>) queue::size);
    }

    /**
     * Submits a failure batch for handling. If the underlying queue is full,
     * the call will block until the queue is ready to accept new batches.
     */
    void submitBlocking(FailureBatch batch) throws InterruptedException {
        queue.put(batch);

        if (queueSize() == configuration.getFailureHandlingQueueCapacity()) {
            logger.debug("The queue is full! Current capacity: {}", configuration.getFailureHandlingQueueCapacity());
        }

        submittedFailureBatches.mark();
        submittedFailures.mark(batch.size());
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
