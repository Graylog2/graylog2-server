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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        super.startUp();
        executionThread = Thread.currentThread();
    }

    @Override
    protected void shutDown() throws Exception {
        super.shutDown();
    }

    @Override
    protected void triggerShutdown() {
        executionThread.interrupt();
    }

    @Override
    protected void run() throws Exception {
        while (isRunning()) {
            try {
                handle(failureSubmitService.consumeBlocking());
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                logger.error("Error occurred while handling a failure!", e);
            }
        }
    }

    private void handle(FailureBatch failureBatch) {
        suitableHandlers(failureBatch)
                .forEach(handler -> handler.handle(failureBatch));
    }

    private List<FailureHandler> suitableHandlers(FailureBatch failureBatch) {
        final List<FailureHandler> suitableHandlers = failureHandlers.stream()
                .filter(FailureHandler::isEnabled)
                .filter(h -> h.supports(failureBatch))
                .collect(Collectors.toList());

        if (suitableHandlers.isEmpty()) {
            return fallbackFailureHandlerAsList.stream().filter(h -> h.supports(failureBatch)).collect(Collectors.toList());
        } else {
            return suitableHandlers;
        }
    }

    public boolean canHandleProcessingErrors() {
        // TODO this should probably be cached
        return !suitableHandlers(new FailureBatch(ImmutableList.of(), ProcessingFailure.class)).isEmpty();
    }
}
