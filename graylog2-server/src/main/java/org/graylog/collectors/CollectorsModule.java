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
package org.graylog.collectors;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import jakarta.inject.Singleton;
import org.graylog.collectors.cloud.CloudCollectorIngestService;
import org.graylog.collectors.config.receiver.FilelogReceiverConfig;
import org.graylog.collectors.config.receiver.JournaldReceiverConfig;
import org.graylog.collectors.config.receiver.WindowsEventLogReceiverConfig;
import org.graylog.collectors.db.FileSourceConfig;
import org.graylog.collectors.db.FleetDTO;
import org.graylog.collectors.db.JournaldSourceConfig;
import org.graylog.collectors.db.WindowsEventLogSourceConfig;
import org.graylog.collectors.indexer.CollectorLogsIndexTemplateProvider;
import org.graylog.collectors.input.CollectorIngestCodec;
import org.graylog.collectors.input.CollectorIngestHttpInput;
import org.graylog.collectors.input.debug.NoOpOtlpTrafficDump;
import org.graylog.collectors.input.debug.OtlpTrafficDump;
import org.graylog.collectors.input.debug.OtlpTrafficDumpService;
import org.graylog.collectors.input.processor.CollectorLogRecordProcessor;
import org.graylog.collectors.input.processor.FilelogRecordProcessor;
import org.graylog.collectors.input.processor.JournaldRecordProcessor;
import org.graylog.collectors.input.processor.LogRecordProcessor;
import org.graylog.collectors.input.processor.WindowsEventLogRecordProcessor;
import org.graylog.collectors.input.transport.CollectorIngestHttpHandler;
import org.graylog.collectors.input.transport.CollectorIngestHttpTransport;
import org.graylog.collectors.opamp.OpAmpModule;
import org.graylog.collectors.periodical.CollectorCaRenewalPeriodical;
import org.graylog.collectors.periodical.PurgeExpiredCollectorInstancesPeriodical;
import org.graylog.collectors.rest.CollectorInstancesResource;
import org.graylog.collectors.rest.CollectorsActivityResource;
import org.graylog.collectors.rest.CollectorsConfigResource;
import org.graylog.collectors.rest.FleetResource;
import org.graylog.collectors.rest.SourceResource;
import org.graylog2.Configuration;
import org.graylog2.database.SequenceTopics;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.indexer.template.IndexTemplateProvider;
import org.graylog2.plugin.PluginModule;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.concurrent.TimeUnit.SECONDS;

public class CollectorsModule extends PluginModule {
    private static final String OTLP_DUMP_FLAG = "collector_otlp_traffic_dump";
    public static final String COLLECTORS_FLAG = "collectors";

    private final boolean collectorsEnabled;
    private final boolean otlpDumpEnabled;
    private final boolean isCloud;

    public CollectorsModule(FeatureFlags featureFlags, Configuration configuration) {
        this.collectorsEnabled = featureFlags.isOn(COLLECTORS_FLAG);
        this.otlpDumpEnabled = featureFlags.isOn(OTLP_DUMP_FLAG);
        this.isCloud = configuration.isCloud();
    }

    @Override
    protected void configure() {
        if (!collectorsEnabled) {
            return;
        }

        install(new OpAmpModule());

        // Fleet transaction log
        Multibinder.newSetBinder(binder(), String.class, SequenceTopics.class)
                .addBinding().toInstance("fleet_txn_log");
        bind(FleetTransactionLogService.class).asEagerSingleton();

        // Currently only HTTP is supported. A gRPC variant was removed to simplify the initial
        // release. See https://github.com/Graylog2/graylog2-server/pull/24815 for the removed
        // implementation. The shared codec, journal record factory, and cert infrastructure
        // are transport-agnostic and can be reused for a gRPC input.
        addMessageInput(CollectorIngestHttpInput.class);
        addTransport(CollectorIngestHttpTransport.NAME, CollectorIngestHttpTransport.class);
        addCodec(CollectorIngestCodec.NAME, CollectorIngestCodec.class);
        install(new FactoryModuleBuilder().build(CollectorIngestHttpHandler.Factory.class));
        addInitializer(CertBindingResolver.class);

        if (isCloud) {
            serviceBinder().addBinding().to(CloudCollectorIngestService.class).in(Scopes.SINGLETON);
        }

        final var logRecordProcessorBinder = MapBinder.newMapBinder(binder(), String.class, LogRecordProcessor.class);

        logRecordProcessorBinder.addBinding(FilelogReceiverConfig.RECEIVER_TYPE).to(FilelogRecordProcessor.class);
        logRecordProcessorBinder.addBinding(JournaldReceiverConfig.RECEIVER_TYPE).to(JournaldRecordProcessor.class);
        logRecordProcessorBinder.addBinding(WindowsEventLogReceiverConfig.RECEIVER_TYPE).to(WindowsEventLogRecordProcessor.class);
        logRecordProcessorBinder.addBinding(CollectorLogRecordProcessor.RECEIVER_TYPE).to(CollectorLogRecordProcessor.class);

        if (otlpDumpEnabled) {
            bind(OtlpTrafficDump.class).to(OtlpTrafficDumpService.class).asEagerSingleton();
            addInitializer(OtlpTrafficDumpService.class);
        } else {
            bind(OtlpTrafficDump.class).to(NoOpOtlpTrafficDump.class);
        }

        addSystemRestResource(CollectorsConfigResource.class);
        addSystemRestResource(CollectorInstancesResource.class);

        // CA
        bind(CollectorCaService.class).in(Scopes.SINGLETON);
        bind(CollectorCaCache.class).in(Scopes.SINGLETON);
        bind(CollectorCaKeyManager.class).in(Scopes.SINGLETON);
        bind(CollectorCaTrustManager.class).in(Scopes.SINGLETON);
        bind(CollectorTLSUtils.class).in(Scopes.SINGLETON);
        addInitializer(CollectorCaCache.class);

        // Collectors config
        bind(CollectorsConfigService.class).asEagerSingleton();

        // Fleet management services
        bind(FleetService.class).asEagerSingleton();
        bind(SourceService.class).asEagerSingleton();

        // Fleet management REST resources
        addSystemRestResource(FleetResource.class);
        addSystemRestResource(SourceResource.class);
        addSystemRestResource(CollectorsActivityResource.class);

        // Periodicals
        addPeriodical(PurgeExpiredCollectorInstancesPeriodical.class);
        addPeriodical(CollectorCaRenewalPeriodical.class);

        // Fleet permissions
        addPermissions(CollectorsPermissions.class);

        // Register entities with the title service
        addDbEntities(FleetDTO.class);

        // SourceConfig Jackson subtypes
        registerJacksonSubtype(FileSourceConfig.class);
        registerJacksonSubtype(JournaldSourceConfig.class);
        registerJacksonSubtype(WindowsEventLogSourceConfig.class);

        final var indexTemplateProviderBinder = MapBinder.newMapBinder(binder(), String.class,
                IndexTemplateProvider.class);
        indexTemplateProviderBinder.addBinding(CollectorLogsIndexTemplateProvider.COLLECTOR_LOGS_TEMPLATE_TYPE)
                .to(CollectorLogsIndexTemplateProvider.class);

        addTelemetryMetricProvider("Collector Metrics", CollectorMetricsSupplier.class);
    }

    /**
     * Provides the executor that runs collector mTLS certificate verification off the Netty event
     * loop (see {@link CollectorCertVerificationExecutor}).
     * <p>
     * Sized to {@code max(2, cores/2)} threads: the work is mostly fast in-memory cache hits with
     * occasional blocking MongoDB lookups, and concurrency is bounded so a reconnect storm cannot
     * oversubscribe CPU or flood the shared Mongo connection pool. {@code allowCoreThreadTimeOut}
     * lets the pool scale back to zero when idle, and daemon threads avoid any shutdown wiring.
     * <p>
     * The bounded queue with the default {@code AbortPolicy} makes overload shed rather than grow
     * unboundedly: once the pool and queue are full, {@code execute} throws, which fails the TLS
     * handshake and lets the collector reconnect with backoff — bounded admission rather than
     * latency-unbounded queueing. Wrapped in an {@link InstrumentedExecutorService} so queue depth
     * and rejections are observable.
     */
    @Provides
    @Singleton
    @CollectorCertVerificationExecutor
    Executor collectorCertVerificationExecutor(MetricRegistry metricRegistry) {
        final var maxThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("collector-cert-verification-%d")
                .setDaemon(true)
                .build();
        final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(1024);
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(maxThreads, maxThreads, 60L, SECONDS, queue,
                threadFactory);
        executor.allowCoreThreadTimeOut(true);
        return new InstrumentedExecutorService(executor, metricRegistry,
                name("collector-cert-verification", "executor-service"));
    }

    /**
     * Provides the executor that runs the {@link CertBindingResolver}'s background cache work —
     * refreshes (event-driven and {@code refreshAfterWrite}) and the startup prewarm (see
     * {@link CollectorCertCacheRefreshExecutor}).
     * <p>
     * Deliberately separate from the cert-verification executor: refresh tasks block on MongoDB, and
     * during a Mongo outage each attempt can park a thread for the driver's server-selection timeout.
     * On a shared pool that would delay or shed TLS handshakes (the periodic refresh wave alone could
     * saturate it at large fleet sizes); on this dedicated two-thread pool the blast radius is capped
     * at "refreshes stall", which is harmless — reads keep serving the current value and stale entries
     * are retried on later access.
     * <p>
     * The queue is unbounded but intrinsically limited: Caffeine coalesces reloads per cache key, so
     * the backlog can never exceed the number of cached fingerprints. Nobody waits on these tasks, so
     * queueing (rather than shedding) is the right overload behavior, and the fixed two-thread pool is
     * the throttle towards MongoDB.
     */
    @Provides
    @Singleton
    @CollectorCertCacheRefreshExecutor
    Executor collectorCertCacheRefreshExecutor(MetricRegistry metricRegistry) {
        final var maxThreads = 2;
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("collector-cert-refresh-%d")
                .setDaemon(true)
                .build();
        final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(maxThreads, maxThreads, 60L, SECONDS, queue,
                threadFactory);
        executor.allowCoreThreadTimeOut(true);
        return new InstrumentedExecutorService(executor, metricRegistry,
                name("collector-cert-cache-refresh", "executor-service"));
    }
}
