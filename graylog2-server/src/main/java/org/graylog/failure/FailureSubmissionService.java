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
import com.google.common.collect.ImmutableList;
import org.graylog2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final AtomicBoolean isUp = new AtomicBoolean(true);
    private final Configuration configuration;
    private final Meter submittedFailureBatches;
    private final Meter submittedFailures;
    private final Meter consumedFailureBatches;
    private final Meter consumedFailures;

    @Inject
    public FailureSubmissionService(Configuration configuration,
                                    MetricRegistry metricRegistry) {
        this.queue = new LinkedBlockingQueue<>(configuration.getFailureHandlingQueueCapacity());
        this.configuration = configuration;
        this.submittedFailureBatches = metricRegistry.meter(name(FailureSubmissionService.class, "submittedFailureBatches"));
        this.submittedFailures = metricRegistry.meter(name(FailureSubmissionService.class, "submittedFailures"));
        this.consumedFailureBatches = metricRegistry.meter(name(FailureSubmissionService.class, "consumedFailureBatches"));
        this.consumedFailures = metricRegistry.meter(name(FailureSubmissionService.class, "consumedFailures"));
    }

    /**
     * Submits a failure batch for handling. If the underlying queue is full,
     * the call will block until the queue is ready to accept new batches.
     */
    public void submitBlocking(FailureBatch batch) throws InterruptedException {
        if (isUp.get()) {
            queue.put(batch);

            if (queueSize() == configuration.getFailureHandlingQueueCapacity()) {
                logger.debug("The queue is full! Current capacity: {}", configuration.getFailureHandlingQueueCapacity());
            }

            submittedFailureBatches.mark();
            submittedFailures.mark(batch.size());
        } else {
            logger.warn("The service is already down and doesn't accept new failures for processing!");
        }
    }

    /**
     * Shuts down the service. Afterwards no new batches are accepted.
     */
    void shutDown() {
        isUp.set(false);
        logger.info("Requested to shut down the failure submission queue. " +
                        "Total number of submitted batches: {} ({} failures), total number of consumed batches: {} ({} failures)",
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
     * @return the current amount of failure batches in the queue.
     */
    int queueSize() {
        return queue.size();
    }

    /**
     * Can be called only after the service is down.
     * @return a list of remaining failure batches.
     */
    List<FailureBatch> drain() {
        if (isUp.get()) {
            throw new IllegalStateException("Must not be called when the service is up!");
        }

        final List<FailureBatch> remainingFailures = new ArrayList<>(queue.size());
        queue.drainTo(remainingFailures);
        return ImmutableList.copyOf(remainingFailures);
    }
}
