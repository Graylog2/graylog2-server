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
public class FailureService {

    private final ExecutorService executor;
    private final List<FailureHandler> fallbackFailureHandlerAsList;
    private final Set<FailureHandler> failureHandlers;

    @Inject
    public FailureService(
            @Named("fallbackFailureHandler") FailureHandler fallbackFailureHandler,
            Set<FailureHandler> failureHandlers
    ) {
        this.fallbackFailureHandlerAsList = Lists.newArrayList(fallbackFailureHandler);
        this.failureHandlers = failureHandlers;
        // TODO: the executor uses 'offer' instead of 'add' => will cause lost messages if the queue is full
        this.executor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1000));
    }

    public void submit(Failure failure) {
        executor.submit(() -> handle(failure));
    }

    private void handle(Failure failure) {
        suitableHandlers(failure)
                .forEach(handler -> handler.handle(failure));
    }

    private List<FailureHandler> suitableHandlers(Failure failure) {
        final List<FailureHandler> suitableHandlers = failureHandlers.stream()
                .filter(FailureHandler::isEnabled)
                .filter(h -> h.supports(failure))
                .collect(Collectors.toList());

        return suitableHandlers.isEmpty() ? fallbackFailureHandlerAsList : suitableHandlers;
    }
}
