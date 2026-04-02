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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;
import jakarta.inject.Named;
import org.graylog.collectors.CollectorCaService;
import org.graylog.inputs.otel.transport.OtlpHttpUtils;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.inputs.transports.AbstractHttpTransport;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.graylog2.utilities.IpSubnet;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * An HTTP transport for collector-managed agents that auto-configures Ed25519 mTLS
 * using the OpAMP CA hierarchy. Overrides the TLS handler to use certificates from
 * {@link CollectorCaService} and adds {@link AgentCertChannelHandler} to extract the
 * agent identity from the client certificate on each connection.
 * <p>
 * The HTTP handler is replaced with {@link CollectorIngestHttpHandler} which enforces
 * agent identity presence and embeds the agent instance UID in journal records.
 */
public class CollectorIngestHttpTransport extends AbstractHttpTransport {
    public static final String NAME = "CollectorIngestHttpTransport";
    static final int DEFAULT_HTTP_PORT = 14401;

    private final CollectorCaService collectorCaService;

    @AssistedInject
    public CollectorIngestHttpTransport(@Assisted Configuration configuration,
                                        EventLoopGroup eventLoopGroup,
                                        EventLoopGroupFactory eventLoopGroupFactory,
                                        NettyTransportConfiguration nettyTransportConfiguration,
                                        ThroughputCounter throughputCounter,
                                        LocalMetricRegistry localMetricRegistry,
                                        TLSProtocolsConfiguration tlsConfiguration,
                                        @Named("trusted_proxies") Set<IpSubnet> trustedProxies,
                                        CollectorCaService collectorCaService) {
        super(withTlsDefaults(configuration), eventLoopGroup, eventLoopGroupFactory,
                nettyTransportConfiguration, throughputCounter, localMetricRegistry,
                tlsConfiguration, trustedProxies, OtlpHttpUtils.LOGS_PATH);
        this.collectorCaService = collectorCaService;
    }

    private static Configuration withTlsDefaults(Configuration userConfig) {
        final var merged = Optional.ofNullable(userConfig.getSource()).map(HashMap::new).orElse(new HashMap<>());
        merged.put(CK_TLS_ENABLE, true);
        merged.put(CK_TLS_CLIENT_AUTH, TLS_CLIENT_AUTH_REQUIRED);
        return new Configuration(merged);
    }

    @Override
    protected Callable<? extends ChannelHandler> createSslHandler(MessageInput input) {
        return () -> {
            final SslContext sslContext = collectorCaService.newServerSslContextBuilder().build();
            return sslContext.newHandler(PooledByteBufAllocator.DEFAULT);
        };
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getCustomChildChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlers = new LinkedHashMap<>();

        // Add AgentCertChannelHandler first — it must fire on SslHandshakeCompletionEvent
        // which occurs after the TLS handler (added by the parent's getChildChannelHandlers)
        handlers.put("agent-cert-handler", AgentCertChannelHandler::new);

        // Add all parent custom handlers (HTTP decoder, decompressor, encoder, aggregator, etc.)
        handlers.putAll(super.getCustomChildChannelHandlers(input));

        // Replace the http-handler with the collector ingest variant that enforces agent identity.
        // Note: rawmessage-handler, codec-aggregator, and exception-logger are added downstream
        // by AbstractTcpTransport and are unreachable because CollectorIngestHttpHandler does not
        // fire messages downstream. These cannot be removed without overriding getChildChannelHandlers.
        handlers.replace("http-handler", () -> new CollectorIngestHttpHandler(input));

        return handlers;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<CollectorIngestHttpTransport> {
        @Override
        CollectorIngestHttpTransport create(Configuration configuration);

        @Override
        CollectorIngestHttpTransport.Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractHttpTransport.Config {
        private static final int DEFAULT_MAX_CHUNK_SIZE = 4 * 1024 * 1024;

        private static final Set<String> ALLOWED_FIELDS = Set.of(
                CK_BIND_ADDRESS,
                CK_PORT,
                CK_RECV_BUFFER_SIZE,
                CK_NUMBER_WORKER_THREADS,
                CK_MAX_CHUNK_SIZE,
                CK_IDLE_WRITER_TIMEOUT,
                CK_TCP_KEEPALIVE
        );

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final var config = new ConfigurationRequest();

            super.getRequestedConfiguration().getFields().values().stream()
                    .filter(field -> ALLOWED_FIELDS.contains(field.getName()))
                    .forEach(config::addField);

            config.addField(ConfigurationRequest.Templates.portNumber(CK_PORT, DEFAULT_HTTP_PORT));
            config.addField(new NumberField(
                    CK_MAX_CHUNK_SIZE,
                    "Max. HTTP chunk size",
                    DEFAULT_MAX_CHUNK_SIZE,
                    "The maximum HTTP chunk size in bytes (e.g. length of HTTP request body)",
                    ConfigurationField.Optional.OPTIONAL));

            return config;
        }
    }
}
