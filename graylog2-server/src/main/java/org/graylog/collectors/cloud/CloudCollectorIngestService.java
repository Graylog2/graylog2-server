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
package org.graylog.collectors.cloud;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.input.CollectorIngestHttpInput;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.inputs.ReservedInputIds;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MisfireException;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.graylog.collectors.CollectorIngestInputService.getHttpIngestInputConfig;
import static org.slf4j.LoggerFactory.getLogger;

public class CloudCollectorIngestService extends AbstractIdleService {
    private static final Logger LOG = getLogger(CloudCollectorIngestService.class);

    private final InputBuffer inputBuffer;
    private final EventBus eventBus;
    private final CollectorsConfigService collectorsConfigService;
    private final CollectorIngestHttpInput.Factory httpInputFactory;
    private final int shutdownTimeoutMs;
    private final ExecutorService executorService;

    private volatile boolean shuttingDown = false;
    private volatile CollectorIngestHttpInput input;

    @Inject
    public CloudCollectorIngestService(InputBuffer inputBuffer,
                                       EventBus eventBus,
                                       CollectorsConfigService collectorsConfigService,
                                       CollectorIngestHttpInput.Factory httpInputFactory,
                                       @Named("shutdown_timeout") int shutdownTimeoutMs) {
        this.inputBuffer = inputBuffer;
        this.eventBus = eventBus;
        this.collectorsConfigService = collectorsConfigService;
        this.httpInputFactory = httpInputFactory;
        this.shutdownTimeoutMs = shutdownTimeoutMs;

        // Executor that runs a single thread and doesn't keep its core thread around.
        final var threadPoolExecutor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactoryBuilder().setNameFormat("collector-ingest-launcher-%d").build());
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        this.executorService = threadPoolExecutor;
    }

    @Subscribe
    public void handleClusterConfigChanged(ClusterConfigChangedEvent event) {
        // At the moment we only care about the initial config being present, which means the collectors features
        // was enabled. If we need to react to actual changes, the code below needs to be adjusted.
        if (CollectorsConfig.class.getCanonicalName().equals(event.type())) {
            if (!shuttingDown) {
                launch();
            }
        }
    }

    @Override
    protected void startUp() throws Exception {
        eventBus.register(this);
        launch();
    }

    private void launch() {
        if (input == null) {
            executorService.submit(this::reconcileIngestState);
        }
    }

    private void reconcileIngestState() {
        if (shuttingDown) {
            return;
        }

        if (input != null) {
            return;
        }

        final var config = collectorsConfigService.get();
        if (config.isEmpty()) {
            return;
        }

        //noinspection UnstableApiUsage
        final var retryer = RetryerBuilder.<Void>newBuilder()
                .retryIfException(t -> t instanceof MisfireException)
                .withStopStrategy(StopStrategies.neverStop())
                .withWaitStrategy(WaitStrategies.exponentialWait(1, TimeUnit.MINUTES))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        // Only log attempts that will actually be retried (MisfireException); other exceptions
                        // abort the retryer and are logged by the caller.
                        if (attempt.hasException() && attempt.getExceptionCause() instanceof MisfireException) {
                            LOG.warn("Launching Collector Ingest failed (attempt #{}), retrying.",
                                    attempt.getAttemptNumber(), attempt.getExceptionCause());
                        }
                    }
                })
                .build();

        try {
            retryer.call(() -> {
                stopInput();
                final var collectorsConfig = config.get();
                input = createInput(collectorsConfig);
                launchInput(input);
                LOG.info("Collector Ingest on [{}:{}] launched successfully.", collectorsConfig.http().hostname(),
                        collectorsConfig.http().port());
                return null;
            });
        } catch (Exception e) {
            if (shuttingDown) {
                return; // shutdown requested
            }
            LOG.error("Ultimately failed to launch Collector Ingest", e);
        }
    }

    @Override
    protected void shutDown() throws Exception {
        this.shuttingDown = true;
        eventBus.unregister(this);
        executorService.shutdownNow();
        if (!executorService.awaitTermination(shutdownTimeoutMs, TimeUnit.MILLISECONDS)) {
            LOG.warn("Timed out after {} ms waiting for executor to shut down.", shutdownTimeoutMs);
        }
        stopInput();
    }

    private void launchInput(CollectorIngestHttpInput input) throws MisfireException {
        // A failure recorder that keeps this system-managed input isolated from user-managed inputs.
        final var sideEffectFreeFailureRecorder = new InputFailureRecorder(new IOState<>(new EventBus(), input));

        input.initialize();
        input.launch(inputBuffer, sideEffectFreeFailureRecorder);
    }

    private CollectorIngestHttpInput createInput(CollectorsConfig collectorsConfig) {
        final var inputConfig = new Configuration(getHttpIngestInputConfig(
                collectorsConfig.http().port(), httpInputFactory.getConfig().combinedRequestedConfiguration()));
        final var input = httpInputFactory.create(inputConfig);
        input.setPersistId(ReservedInputIds.EPHEMERAL_COLLECTOR_INGEST);
        input.setTitle("Managed Collector Ingest");
        return input;
    }

    private void stopInput() {
        if (input != null) {
            input.stop();
            input = null;
        }
    }

    @VisibleForTesting
    ExecutorService executorService() {
        return executorService;
    }
}
