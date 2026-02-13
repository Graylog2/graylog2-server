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
package org.graylog.collectors.input.transport;

import com.codahale.metrics.MetricSet;
import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import jakarta.inject.Inject;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.inputs.grpc.RemoteAddressProviderInterceptor;
import org.graylog2.opamp.OpAmpCaService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport2;
import org.graylog2.plugin.inputs.transports.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A gRPC transport for collector-managed agents that auto-configures Ed25519 mTLS
 * using the OpAMP CA hierarchy. Unlike {@link org.graylog.inputs.grpc.AbstractGrpcTransport},
 * this transport does not expose TLS certificate/key configuration fields -- it obtains
 * all cryptographic material from {@link OpAmpCaService}.
 * <p>
 * The transport installs {@link AgentCertTransportFilter} and {@link AgentCertAuthInterceptor}
 * to extract the agent's instance UID from the client certificate for each connection.
 */
public class CollectorIngestGrpcTransport extends ThrottleableTransport2 {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorIngestGrpcTransport.class);

    public static final String NAME = "CollectorIngestGrpcTransport";
    static final int DEFAULT_GRPC_PORT = 14402;
    private static final Duration GRACEFUL_SHUTDOWN_TIMEOUT = Duration.ofSeconds(10);

    private final LocalMetricRegistry localMetricRegistry;
    private final CollectorIngestLogsService.Factory logsServiceFactory;
    private final OpAmpCaService opAmpCaService;
    private final ClusterConfigService clusterConfigService;

    private Server server;

    @Inject
    public CollectorIngestGrpcTransport(EventBus eventBus,
                                  @Assisted Configuration configuration,
                                  LocalMetricRegistry localMetricRegistry,
                                  CollectorIngestLogsService.Factory logsServiceFactory,
                                  OpAmpCaService opAmpCaService,
                                  ClusterConfigService clusterConfigService) {
        super(eventBus, configuration);
        this.localMetricRegistry = localMetricRegistry;
        this.logsServiceFactory = logsServiceFactory;
        this.opAmpCaService = opAmpCaService;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    protected void doLaunch(MessageInput input, InputFailureRecorder inputFailureRecorder) throws MisfireException {
        final SslContext sslContext;
        try {
            final var sslContextBuilder = opAmpCaService.newServerSslContextBuilder();
            // JDK provider required: BoringSSL (OPENSSL) can load Ed25519 keys but cannot
            // complete TLS handshakes — its cipher suite negotiation doesn't recognize Ed25519.
            sslContext = GrpcSslContexts.configure(sslContextBuilder, SslProvider.JDK).build();
        } catch (Exception e) {
            throw new MisfireException("Failed to configure TLS for Collector Ingest gRPC input", e);
        }

        final var config = clusterConfigService.get(CollectorsConfig.class);
        final var port = (config != null && config.grpc() != null) ? config.grpc().port() : DEFAULT_GRPC_PORT;
        final var bindAddress = "0.0.0.0";

        final List<ServerServiceDefinition> services = List.of(
                logsServiceFactory.create(this, input).bindService());

        final NettyServerBuilder serverBuilder = NettyServerBuilder
                .forAddress(new InetSocketAddress(bindAddress, port))
                .sslContext(sslContext)
                .addTransportFilter(new AgentCertTransportFilter())
                .intercept(new AgentCertAuthInterceptor())
                .intercept(new RemoteAddressProviderInterceptor())
                .addServices(services)
                .addService(ProtoReflectionServiceV1.newInstance())
                .permitKeepAliveWithoutCalls(true)
                .permitKeepAliveTime(10, TimeUnit.SECONDS);

        try {
            this.server = serverBuilder.build().start();
        } catch (Exception e) {
            throw new MisfireException("Failed to start Collector Ingest gRPC server", e);
        }
    }

    @Override
    protected void doStop() {
        if (server != null) {
            try {
                if (!server.shutdown().awaitTermination(GRACEFUL_SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                    LOG.warn("Failed to gracefully terminate Collector Ingest gRPC server within {} ms. Forcefully terminating now.",
                            GRACEFUL_SHUTDOWN_TIMEOUT.toMillis());
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.error("Error occurred while attempting to shut down Collector Ingest gRPC server", e);
            }
        }
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {
    }

    @Override
    public MetricSet getMetricSet() {
        return localMetricRegistry;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<CollectorIngestGrpcTransport> {
        @Override
        CollectorIngestGrpcTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends ThrottleableTransport2.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return new ConfigurationRequest();
        }
    }
}
