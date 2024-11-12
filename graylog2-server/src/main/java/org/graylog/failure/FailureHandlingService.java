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
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.graylog2.Configuration;
import org.graylog2.plugin.Message;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * A service consuming and processing failure batches submitted via {@link FailureSubmissionQueue}.
 * The processing is done in a dedicated thread, the lifecycle of this service is managed
 * by {@link com.google.common.util.concurrent.ServiceManager}.
 *
 * This service is designed with an idea of extensibility, so that Graylog plugins can inject
 * custom failure handlers via {@link com.google.inject.multibindings.Multibinder} -
 * see {@link org.graylog.failure.FailureHandler}. If no custom handlers found,
 * then the fallback one will be picked instead.
 */
@Singleton
public class FailureHandlingService extends AbstractExecutionThreadService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<FailureHandler> fallbackFailureHandlerAsList;
    private final Set<FailureHandler> failureHandlers;
    private final FailureSubmissionQueue failureSubmissionQueue;
    private final Configuration configuration;
    private final MessageQueueAcknowledger acknowledger;
    private Thread executionThread;
    private final MetricRegistry metricRegistry;
    private final ObjectMapper objectMapper;
    private final Meter dummyMeter = new Meter();

    @Inject
    public FailureHandlingService(
            @Named("fallbackFailureHandler") FailureHandler fallbackFailureHandler,
            Set<FailureHandler> failureHandlers,
            FailureSubmissionQueue failureSubmissionQueue,
            Configuration configuration,
            MessageQueueAcknowledger acknowledger,
            MetricRegistry metricRegistry,
            ObjectMapperProvider objectMapperProvider) {
        this.fallbackFailureHandlerAsList = Lists.newArrayList(fallbackFailureHandler);
        this.failureHandlers = failureHandlers;
        this.failureSubmissionQueue = failureSubmissionQueue;
        this.configuration = configuration;
        this.acknowledger = acknowledger;
        this.metricRegistry = metricRegistry;
        this.objectMapper = objectMapperProvider.get();
    }

    @Override
    protected void startUp() throws Exception {
        executionThread = Thread.currentThread();

        logger.debug("Starting up the service.");
    }

    @Override
    protected void shutDown() throws Exception {

        // if no new batches have been submitted within a certain period of time
        // we consider the processing being done with its job and we don't expect
        // further failures.
        final long shutdownAwaitInsMs = configuration.getFailureHandlingShutdownAwait().toMilliseconds();
        int remainingBatchCount = 0;
        FailureBatch remainingFailureBatch = failureSubmissionQueue.consumeBlockingWithTimeout(shutdownAwaitInsMs);

        while (remainingFailureBatch != null) {
            handle(remainingFailureBatch);
            remainingBatchCount++;
            remainingFailureBatch = failureSubmissionQueue.consumeBlockingWithTimeout(shutdownAwaitInsMs);
        }

        logger.info("Shutting down the service. Processed {} remaining failure batches.", remainingBatchCount);

        failureSubmissionQueue.logStats("FailureHandlerService#shutDown");
    }

    @Override
    protected void triggerShutdown() {
        logger.debug("Requested to shut down.");

        executionThread.interrupt();

        failureSubmissionQueue.logStats("FailureHandlerService#triggerShutdown");
    }

    @Override
    protected void run() throws Exception {

        if (isRunning()) {
            logger.debug("The service is up and running!");
        }

        while (isRunning()) {
            try {
                handle(failureSubmissionQueue.consumeBlocking());
            } catch (InterruptedException ignored) {
                logger.info("The service's thread has been interrupted. The queue currently contains {} failure batches.",
                        failureSubmissionQueue.queueSize());
            } catch (Exception e) {
                logger.error("Error occurred while handling failures!", e);
            }
        }

        logger.debug("The service has been interrupted.");
    }

    private void handle(FailureBatch failureBatch) {
        failureBatch.getFailures().forEach(this::updateMetrics);

        suitableHandlers(failureBatch)
                .forEach(handler -> {
                    try {
                        handler.handle(failureBatch);
                    } catch (Exception e) {
                        logger.error("Error occurred while handling failures by {}", handler.getClass().getName());
                    }
                });

        final List<Message> requiresAcknowledgement = failureBatch.getFailures().stream()
                .filter(Failure::requiresAcknowledgement)
                .map(Failure::failedMessage)
                .filter(Message.class::isInstance)
                .map(Message.class::cast)
                .collect(Collectors.toList());

        if (!requiresAcknowledgement.isEmpty()) {
            acknowledger.acknowledge(requiresAcknowledgement);
        }
    }

    private void updateMetrics(Failure failure) {
        if (failure == null || failure.failedMessage() == null) {
            return;
        }

        final Map<String, Object> searchObject = failure.failedMessage().toElasticSearchObject(objectMapper, dummyMeter);
        final Object inputId = searchObject.get(Message.FIELD_GL2_SOURCE_INPUT);
        if (inputId != null) {
            switch (failure.failureType()) {
                case INDEXING -> {
                    final String indexingFailureMetricName = name("org.graylog2", inputId.toString(), "failures.indexing");
                    metricRegistry.meter(indexingFailureMetricName).mark();
                }
                case PROCESSING -> {
                    final String processingFailureMetricName = name("org.graylog2", inputId.toString(), "failures.processing");
                    metricRegistry.meter(processingFailureMetricName).mark();
                }
            }
        }
    }

    private List<FailureHandler> suitableHandlers(FailureBatch failureBatch) {
        final List<FailureHandler> suitableHandlers = suitableHandlers(failureHandlers, failureBatch)
                .filter(FailureHandler::isEnabled)
                .collect(Collectors.toList());

        if (suitableHandlers.isEmpty()) {
            return suitableHandlers(fallbackFailureHandlerAsList, failureBatch)
                    .collect(Collectors.toList());
        } else {
            return suitableHandlers;
        }
    }

    private Stream<FailureHandler> suitableHandlers(Collection<FailureHandler> handlers, FailureBatch failureBatch) {
        return handlers.stream()
                .filter(h -> h.supports(failureBatch));
    }
}
