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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
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

import static org.graylog.failure.FailureBatch.EMPTY_PROCESSING_FAILURE_BATCH;

@Singleton
public class FailureHandlerService extends AbstractExecutionThreadService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<FailureHandler> fallbackFailureHandlerAsList;
    private final Set<FailureHandler> failureHandlers;
    private final FailureSubmitService failureSubmitService;
    private Thread executionThread;

    @Inject
    public FailureHandlerService(
            @Named("fallbackFailureHandler") FailureHandler fallbackFailureHandler,
            Set<FailureHandler> failureHandlers,
            FailureSubmitService failureSubmitService
    ) {
        this.fallbackFailureHandlerAsList = Lists.newArrayList(fallbackFailureHandler);
        this.failureHandlers = failureHandlers;
        this.failureSubmitService = failureSubmitService;
    }

    @Override
    protected void startUp() throws Exception {
        executionThread = Thread.currentThread();

        logger.debug("Starting up the service.");
    }

    @Override
    protected void shutDown() throws Exception {

        final List<FailureBatch> remainingFailures = failureSubmitService.drain();

        logger.info("Shutting down the service. {} remaining failure batches to be processed.", remainingFailures.size());

        remainingFailures.forEach(this::handle);
    }

    @Override
    protected void triggerShutdown() {
        logger.debug("Requested to shut down.");

        executionThread.interrupt();

        failureSubmitService.shutDown();
    }

    @Override
    protected void run() throws Exception {

        if (isRunning()) {
            logger.debug("The service is up and running!");
        }

        while (isRunning()) {
            try {
                handle(failureSubmitService.consumeBlocking());
            } catch (InterruptedException ignored) {
                logger.info("The service's thread has been interrupted. The queue currently contains {} failure batches.",
                        failureSubmitService.queueSize());
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

    public boolean canHandleProcessingErrors() {
        return !suitableHandlers(EMPTY_PROCESSING_FAILURE_BATCH).isEmpty();
    }
}
