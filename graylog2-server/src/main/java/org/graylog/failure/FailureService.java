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
                .filter(h -> h.supports(failure))
                .collect(Collectors.toList());

        return suitableHandlers.isEmpty() ? fallbackFailureHandlerAsList : suitableHandlers;
    }
}
