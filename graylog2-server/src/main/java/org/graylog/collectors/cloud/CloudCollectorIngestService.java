package org.graylog.collectors.cloud;

import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Uninterruptibles;
import jakarta.inject.Inject;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.graylog.collectors.CollectorIngestInputService.getHttpIngestInputConfig;
import static org.slf4j.LoggerFactory.getLogger;

public class CloudCollectorIngestService extends AbstractExecutionThreadService {
    private static final Logger LOG = getLogger(CloudCollectorIngestService.class);

    private final InputBuffer inputBuffer;
    private final EventBus eventBus;
    private final CollectorsConfigService collectorsConfigService;
    private final CollectorIngestHttpInput.Factory httpInputFactory;
    private final Semaphore configChanged = new Semaphore(0);

    private volatile Thread executionThread;
    private CollectorIngestHttpInput input;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    @Inject
    public CloudCollectorIngestService(InputBuffer inputBuffer,
                                       EventBus eventBus,
                                       CollectorsConfigService collectorsConfigService,
                                       CollectorIngestHttpInput.Factory httpInputFactory) {
        this.inputBuffer = inputBuffer;
        this.eventBus = eventBus;
        this.collectorsConfigService = collectorsConfigService;
        this.httpInputFactory = httpInputFactory;
    }

    @Subscribe
    public void handleClusterConfigChanged(ClusterConfigChangedEvent event) {
        // At the moment we only care about the initial config being present, which means the collectors features
        // was enabled. If we need to react to actual changes, the code below needs to be adjusted.
        if (CollectorsConfig.class.getCanonicalName().equals(event.type())) {
            configChanged.release();
        }
    }

    @Override
    protected void startUp() throws Exception {
        this.executionThread = Thread.currentThread();
        eventBus.register(this);
    }

    @Override
    protected void run() throws Exception {
        final var config = waitForCollectorConfig();
        if (config == null) {
            return; // shutdown requested before config became available
        }

        final var retryer = RetryerBuilder.<Void>newBuilder()
                .retryIfException(t -> t instanceof MisfireException)
                .withStopStrategy(StopStrategies.neverStop())
                .withWaitStrategy(WaitStrategies.exponentialWait(1, TimeUnit.MINUTES))
                .build();

        try {
            retryer.call(() -> {
                launchInput(config);
                LOG.info("Collector Ingest on [{}:{}] launched successfully.", config.http().hostname(),
                        config.http().port());
                return null;
            });
        } catch (Exception e) {
            if (!isRunning()) {
                return; // shutdown requested
            }
            throw new RuntimeException("Failed to launch Collector Ingest Input", e);
        }

        Uninterruptibles.awaitUninterruptibly(shutdownLatch);
    }

    @Override
    protected void triggerShutdown() {
        shutdownLatch.countDown();
        if (executionThread != null) {
            executionThread.interrupt();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        if (input != null) {
            input.stop();
        }
        try {
            eventBus.unregister(this);
        } catch (Exception e) {
            // Ignore. We might have already unregistered after receiving the initial config.
        }
    }

    private CollectorsConfig waitForCollectorConfig() {
        while (isRunning()) {
            final var config = collectorsConfigService.get();
            if (config.isPresent()) {
                eventBus.unregister(this);
                return config.get();
            }
            try {
                configChanged.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null; // shutting down
            }
            configChanged.drainPermits();
        }
        return null;
    }

    private void launchInput(CollectorsConfig collectorsConfig) throws MisfireException {
        final var input = createInput(collectorsConfig);
        // A failure recorder that won't propagate state to the global event bus, because it has its own private copy
        final var sideEffectFreeFailureRecorder = new InputFailureRecorder(new IOState<>(new EventBus(), input));

        input.initialize();
        input.launch(inputBuffer, sideEffectFreeFailureRecorder);

        this.input = input;
    }

    private CollectorIngestHttpInput createInput(CollectorsConfig collectorsConfig) {
        final var inputConfig = new Configuration(getHttpIngestInputConfig(
                collectorsConfig.http().port(), httpInputFactory.getConfig().combinedRequestedConfiguration()));
        final var input = httpInputFactory.create(inputConfig);
        input.setPersistId(ReservedInputIds.EPHEMERAL_COLLECTOR_INGEST);
        input.setTitle("Managed Collector Ingest");
        return input;
    }
}
