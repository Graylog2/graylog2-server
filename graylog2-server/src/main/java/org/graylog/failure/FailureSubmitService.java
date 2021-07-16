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

@Singleton
public class FailureSubmitService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final BlockingQueue<FailureBatch> queue = new LinkedBlockingQueue<>(1000);
    private final AtomicBoolean isUp = new AtomicBoolean(true);
    private final Meter submittedFailureBatches;
    private final Meter submittedFailures;
    private final Meter consumedFailureBatches;
    private final Meter consumedFailures;

    @Inject
    public FailureSubmitService(MetricRegistry metricRegistry) {
        this.submittedFailureBatches = metricRegistry.meter(name(FailureSubmitService.class, "submittedFailureBatches"));
        this.submittedFailures = metricRegistry.meter(name(FailureSubmitService.class, "submittedFailures"));
        this.consumedFailureBatches = metricRegistry.meter(name(FailureSubmitService.class, "consumedFailureBatches"));
        this.consumedFailures = metricRegistry.meter(name(FailureSubmitService.class, "consumedFailures"));
    }

    public void submitBlocking(FailureBatch batch) throws InterruptedException {
        if (isUp.get()) {
            queue.put(batch);
            submittedFailureBatches.mark();
            submittedFailures.mark(batch.size());
        } else {
            logger.warn("The service is already down and doesn't accept new failures for processing!");
        }
    }

    void shutDown() {
        isUp.set(false);
        logger.info("Requested to shut down the failure submission queue. " +
                        "Total number of submitted batches: {} ({} failures), total number of consumed batches: {} ({} failures)",
                submittedFailureBatches.getCount(), submittedFailures.getCount(),
                consumedFailureBatches.getCount(), consumedFailures.getCount());
    }

    FailureBatch consumeBlocking() throws InterruptedException {
        final FailureBatch fb = queue.take();
        consumedFailureBatches.mark();
        consumedFailures.mark(fb.size());
        return fb;
    }

    int queueSize() {
        return queue.size();
    }

    List<FailureBatch> drain() {
        if (isUp.get()) {
            throw new IllegalStateException("Must not be called when the service is up!");
        }

        final List<FailureBatch> remainingFailures = new ArrayList<>(queue.size());
        queue.drainTo(remainingFailures);
        return remainingFailures;
    }
}
