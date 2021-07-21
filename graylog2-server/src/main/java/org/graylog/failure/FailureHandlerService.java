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
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.graylog2.Configuration;
import org.graylog2.shared.journal.Journal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The purpose of this service is to consume/process failure batches submitted via {@link FailureSubmissionService}.
 * The processing is done in a dedicated service thread, the lifecycle of the service is managed
 * by {@link com.google.common.util.concurrent.ServiceManager}.
 *
 * The service is designed with an idea of extensibility, so that Graylog plugins can inject
 * custom failure handlers via a DI framework (Guice in our case). If no custom handlers
 * found, then the fallback one will be used instead.
 */
@Singleton
public class FailureHandlerService extends AbstractExecutionThreadService {

    static final FailureBatch EMPTY_PROCESSING_FAILURE_BATCH = FailureBatch.processingFailureBatch(ImmutableList.of());

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<FailureHandler> fallbackFailureHandlerAsList;
    private final Set<FailureHandler> failureHandlers;
    private final FailureSubmissionService failureSubmissionService;
    private final Configuration configuration;
    private final Journal journal;
    private Thread executionThread;

    @Inject
    public FailureHandlerService(
            @Named("fallbackFailureHandler") FailureHandler fallbackFailureHandler,
            Set<FailureHandler> failureHandlers,
            FailureSubmissionService failureSubmissionService,
            Configuration configuration,
            Journal journal
    ) {
        this.fallbackFailureHandlerAsList = Lists.newArrayList(fallbackFailureHandler);
        this.failureHandlers = failureHandlers;
        this.failureSubmissionService = failureSubmissionService;
        this.configuration = configuration;
        this.journal = journal;
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
        FailureBatch remainingFailureBatch = failureSubmissionService.consumeBlockingWithTimeout(shutdownAwaitInsMs);

        while (remainingFailureBatch != null) {
            handle(remainingFailureBatch);
            remainingBatchCount++;
            remainingFailureBatch = failureSubmissionService.consumeBlockingWithTimeout(shutdownAwaitInsMs);
        }

        // very likely that the LocalKafkaJournal service will shut down
        // way earlier before we are done with the failures, in this way
        // the journal offset value will be left in inconsistent state,
        // what in its turn will lead to reprocessing and duplicates.
        // Therefore this explicit call is required here.
        journal.flush();

        logger.info("Shutting down the service. Processed {} remaining failure batches.", remainingBatchCount);

        failureSubmissionService.logStats("FailureHandlerService#shutDown");
    }

    @Override
    protected void triggerShutdown() {
        logger.debug("Requested to shut down.");

        executionThread.interrupt();

        failureSubmissionService.logStats("FailureHandlerService#triggerShutdown");
    }

    @Override
    protected void run() throws Exception {

        if (isRunning()) {
            logger.debug("The service is up and running!");
        }

        while (isRunning()) {
            try {
                handle(failureSubmissionService.consumeBlocking());
            } catch (InterruptedException ignored) {
                logger.info("The service's thread has been interrupted. The queue currently contains {} failure batches.",
                        failureSubmissionService.queueSize());
            } catch (Exception e) {
                logger.error("Error occurred while handling failures!", e);
            }
        }

        logger.debug("The service has been interrupted.");
    }

    private void handle(FailureBatch failureBatch) {
        suitableHandlers(failureBatch)
                .forEach(handler -> {
                    try {
                        handler.handle(failureBatch);
                    } catch (Exception e) {
                        logger.error("Error occurred while handling failures by {}", handler.getClass().getName());
                    }
                });
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

    /**
     * @return true if any of the provided handlers (either custom or the fallback one)
     * supports processing failures.
     */
    public boolean canHandleProcessingErrors() {
        return !suitableHandlers(EMPTY_PROCESSING_FAILURE_BATCH).isEmpty();
    }
}
