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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class FailureService extends AbstractExecutionThreadService {

    private final ExecutorService executor;
    private FailureSubmitQueue failureQueue;
    private final List<FailureHandler> fallbackFailureHandlerAsList;
    private final Set<FailureHandler> failureHandlers;
    private Thread executionThread;

    @Inject
    public FailureService(
            FailureSubmitQueue failureQueue,
            @Named("fallbackFailureHandler") FailureHandler fallbackFailureHandler,
            Set<FailureHandler> failureHandlers
    ) {
        this.failureQueue = failureQueue;
        this.fallbackFailureHandlerAsList = Lists.newArrayList(fallbackFailureHandler);
        this.failureHandlers = failureHandlers;
        // TODO: the executor uses 'offer' instead of 'add' => will cause lost messages if the queue is full
        this.executor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1000));
    }

    private void submit(List<FailureObject> failures) {
        executor.submit(() -> handle(failures));
    }

    private void handle(List<FailureObject> failures) {
        enabledHandlers()
                .forEach(handler -> handler.handle(failures));
    }

    private List<FailureHandler> enabledHandlers() {
        final List<FailureHandler> suitableHandlers = failureHandlers.stream()
                .filter(FailureHandler::isEnabled)
                .collect(Collectors.toList());
        return suitableHandlers.isEmpty() ? fallbackFailureHandlerAsList : suitableHandlers;
    }

    private List<FailureHandler> suitableHandlers(FailureObject failure) {
        final List<FailureHandler> suitableHandlers = failureHandlers.stream()
                .filter(FailureHandler::isEnabled)
                .filter(h -> h.supports(failure))
                .collect(Collectors.toList());

        return suitableHandlers.isEmpty() ? fallbackFailureHandlerAsList : suitableHandlers;
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
                final List<FailureObject> failureObjects = failureQueue.getFailureQueue().take();
                submit(failureObjects);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
