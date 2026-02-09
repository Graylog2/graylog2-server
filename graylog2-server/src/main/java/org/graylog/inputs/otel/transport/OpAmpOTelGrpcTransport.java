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
package org.graylog.inputs.otel.transport;

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
import org.graylog.inputs.grpc.RemoteAddressProviderInterceptor;
import org.graylog2.opamp.OpAmpCaService;
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
 * A gRPC transport for OpAMP-managed agents that auto-configures Ed25519 mTLS
 * using the OpAMP CA hierarchy. Unlike {@link org.graylog.inputs.grpc.AbstractGrpcTransport},
 * this transport does not expose TLS certificate/key configuration fields -- it obtains
 * all cryptographic material from {@link OpAmpCaService}.
 * <p>
 * The transport installs {@link AgentCertTransportFilter} and {@link AgentCertAuthInterceptor}
 * to extract the agent's instance UID from the client certificate for each connection.
 */
public class OpAmpOTelGrpcTransport extends ThrottleableTransport2 {
    private static final Logger LOG = LoggerFactory.getLogger(OpAmpOTelGrpcTransport.class);

    public static final String NAME = "OpAmpGrpcTransport";
    private static final Duration GRACEFUL_SHUTDOWN_TIMEOUT = Duration.ofSeconds(10);

    private static final String CK_BIND_ADDRESS = "bind_address";
    private static final String CK_PORT = "port";

    private final Configuration configuration;
    private final LocalMetricRegistry localMetricRegistry;
    private final OpAmpOTelLogsService.Factory logsServiceFactory;
    private final OpAmpCaService opAmpCaService;

    private Server server;

    @Inject
    public OpAmpOTelGrpcTransport(EventBus eventBus,
                                  @Assisted Configuration configuration,
                                  LocalMetricRegistry localMetricRegistry,
                                  OpAmpOTelLogsService.Factory logsServiceFactory,
                                  OpAmpCaService opAmpCaService) {
        super(eventBus, configuration);
        this.configuration = configuration;
        this.localMetricRegistry = localMetricRegistry;
        this.logsServiceFactory = logsServiceFactory;
        this.opAmpCaService = opAmpCaService;
    }

    @Override
    protected void doLaunch(MessageInput input, InputFailureRecorder inputFailureRecorder) throws MisfireException {
        final SslContext sslContext;
        try {
            final var sslContextBuilder = opAmpCaService.newServerSslContextBuilder();
            sslContext = GrpcSslContexts.configure(sslContextBuilder, SslProvider.JDK).build();
        } catch (Exception e) {
            throw new MisfireException("Failed to configure TLS for OpAMP gRPC input", e);
        }

        final var bindAddress = configuration.getString(CK_BIND_ADDRESS, "0.0.0.0");
        final var port = configuration.getInt(CK_PORT, 4317);

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
            throw new MisfireException("Failed to start OpAMP gRPC server", e);
        }
    }

    @Override
    protected void doStop() {
        if (server != null) {
            try {
                if (!server.shutdown().awaitTermination(GRACEFUL_SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                    LOG.warn("Failed to gracefully terminate OpAMP gRPC server within {} ms. Forcefully terminating now.",
                            GRACEFUL_SHUTDOWN_TIMEOUT.toMillis());
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.error("Error occurred while attempting to shut down OpAMP gRPC server", e);
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
    public interface Factory extends Transport.Factory<OpAmpOTelGrpcTransport> {
        @Override
        OpAmpOTelGrpcTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends ThrottleableTransport2.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final var request = super.getRequestedConfiguration();
            request.addField(ConfigurationRequest.Templates.bindAddress(CK_BIND_ADDRESS));
            request.addField(ConfigurationRequest.Templates.portNumber(CK_PORT, 4317));
            return request;
        }
    }
}
