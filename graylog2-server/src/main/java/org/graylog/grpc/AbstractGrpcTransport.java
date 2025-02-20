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
package org.graylog.grpc;

import com.codahale.metrics.MetricSet;
import com.google.common.eventbus.EventBus;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.InlineBinaryField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport2;
import org.graylog2.security.encryption.EncryptedValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

public abstract class AbstractGrpcTransport extends ThrottleableTransport2 {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractGrpcTransport.class);
    private static final Duration GRACEFUL_SHUTDOWN_TIMEOUT = Duration.ofSeconds(10);

    private static final String CK_BIND_ADDRESS = "bind_address";
    private static final String CK_PORT = "port";
    private static final String CK_BEARER_TOKEN = "bearer_token";
    private static final String CK_INSECURE = "insecure";
    private static final String CK_TLS_CERT = "tls_cert";
    private static final String CK_TLS_KEY = "tls_key";
    private static final String CK_TLS_CLIENT_CA = "tls_client_ca";
    private static final String CK_MAX_INBOUND_MSG_SIZE = "max_inbound_msg_size";

    protected final LocalMetricRegistry localMetricRegistry;
    private final Configuration configuration;
    private final EncryptedValueService encryptedValueService;

    private Server server;

    public AbstractGrpcTransport(EventBus eventBus,
                                 Configuration configuration,
                                 LocalMetricRegistry localMetricRegistry,
                                 EncryptedValueService encryptedValueService) {
        super(eventBus, configuration);
        this.localMetricRegistry = localMetricRegistry;
        this.configuration = configuration;
        this.encryptedValueService = encryptedValueService;
    }

    abstract protected List<ServerServiceDefinition> grpcServices(MessageInput input);

    @Override
    protected void doLaunch(MessageInput input, InputFailureRecorder inputFailureRecorder) throws MisfireException {

        final var bindAddress = configuration.getString(CK_BIND_ADDRESS, "127.0.0.1");
        final var port = configuration.getInt(CK_PORT, 4317);
        final var insecure = configuration.getBoolean(CK_INSECURE, false);
        final var maxInboundMsgSize = configuration.getInt(CK_MAX_INBOUND_MSG_SIZE, GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE);
        final var requireBearerToken = configuration.encryptedValueIsSet(CK_BEARER_TOKEN);

        final NettyServerBuilder serverBuilder = NettyServerBuilder
                .forAddress(new InetSocketAddress(bindAddress, port))
                .intercept(new RemoteAddressProviderInterceptor())
                .addServices(grpcServices(input))
                .addService(ProtoReflectionServiceV1.newInstance())
                .permitKeepAliveWithoutCalls(true)
                .permitKeepAliveTime(10, TimeUnit.SECONDS)
                .maxInboundMessageSize(maxInboundMsgSize);

        if (requireBearerToken) {
            final var token = encryptedValueService.decrypt(configuration.getEncryptedValue(CK_BEARER_TOKEN));
            serverBuilder.intercept(new BearerTokenAuthInterceptor(token));
        }

        if (!insecure) {
            serverBuilder.sslContext(getSslContext());
        }

        try {
            this.server = serverBuilder.build().start();
        } catch (Exception e) {
            throw new MisfireException("Failed to start gRPC server", e);
        }
    }

    private SslContext getSslContext() {
        final var fields = new Config().getRequestedConfiguration();

        final List<String> missingFieldNames = new ArrayList<>();
        if (configuration.getString(CK_TLS_CERT, "").isBlank()) {
            missingFieldNames.add(fields.getField(CK_TLS_CERT).getHumanName());
        }
        if (!configuration.encryptedValueIsSet(CK_TLS_KEY)) {
            missingFieldNames.add(fields.getField(CK_TLS_KEY).getHumanName());
        }

        if (!missingFieldNames.isEmpty()) {
            throw new RuntimeException(f("Secure mode is enabled, but required configuration for %s is missing.",
                    missingFieldNames.stream().map(s -> StringUtils.wrap(s, '"'))
                            .collect(Collectors.joining(", "))));
        }

        final Base64.Decoder base64Decoder = Base64.getDecoder();

        final var certChain = base64Decoder.decode(configuration.getString(CK_TLS_CERT));
        final var privateKey = base64Decoder.decode(
                encryptedValueService.decrypt(configuration.getEncryptedValue(CK_TLS_KEY)));

        final var clientCaCert = !configuration.getString(CK_TLS_CLIENT_CA, "").isBlank() ?
                base64Decoder.decode(configuration.getString(CK_TLS_CLIENT_CA)) :
                null;

        try {
            final var contextBuilder = GrpcSslContexts
                    .forServer(new ByteArrayInputStream(certChain), new ByteArrayInputStream(privateKey));
            if (clientCaCert != null) {
                contextBuilder.clientAuth(ClientAuth.REQUIRE).trustManager(new ByteArrayInputStream(clientCaCert));
            }
            return contextBuilder.build();
        } catch (SSLException e) {
            throw new RuntimeException(f("Failed setting up TLS for gRPC server: {}.", e.getLocalizedMessage()), e);
        }
    }

    @Override
    protected void doStop() {
        if (server != null) {
            try {
                if (!server.shutdown().awaitTermination(GRACEFUL_SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                    LOG.warn("Failed to gracefully terminate gRPC server within {} ms. Forcefully terminating now.",
                            GRACEFUL_SHUTDOWN_TIMEOUT.toMillis());
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.error("Error occurred while attempting to shut down gRPC server", e);
            }
        }
    }

    public static class Config extends ThrottleableTransport2.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final var request = super.getRequestedConfiguration();

            request.addField(ConfigurationRequest.Templates.bindAddress(CK_BIND_ADDRESS));
            request.addField(ConfigurationRequest.Templates.portNumber(CK_PORT, 4317));

            request.addField(new TextField(
                    CK_BEARER_TOKEN,
                    "Required bearer token",
                    "",
                    "A static bearer token that the server will enforce for all clients. If set, clients," +
                            "must include this token in the Authorization header of their requests.",
                    ConfigurationField.Optional.OPTIONAL,
                    true,
                    TextField.Attribute.IS_PASSWORD
            ));

            request.addField(
                    new BooleanField(
                            CK_INSECURE,
                            "Allow Insecure Connections",
                            false,
                            "Disable TLS encryption to allow insecure connections to the server"
                    )
            );

            request.addField(
                    new InlineBinaryField(
                            CK_TLS_CERT,
                            "TLS Server Certificate Chain",
                            "PEM-encoded certificate chain used by the input to authenticate itself to " +
                                    "clients.",
                            ConfigurationField.Optional.OPTIONAL,
                            false,
                            ""
                    )
            );

            request.addField(
                    new InlineBinaryField(
                            CK_TLS_KEY,
                            "TLS Server Private Key",
                            "PEM-encoded PKCS8 private key used by the server for TLS encryption. " +
                                    "Please note, that if you run this input on a Forwarder, the key will also be " +
                                    "stored in the local filesystem of the Forwarder.",
                            ConfigurationField.Optional.OPTIONAL,
                            true,
                            "")
            );

            request.addField(new InlineBinaryField(
                            CK_TLS_CLIENT_CA,
                            "TLS Client Certificate Chain",
                    "PEM-encoded certificate chain used to validate client certificates during mutual TLS " +
                            "authentication. Clients that fail to provide a trusted certificate will be rejected.",
                            ConfigurationField.Optional.OPTIONAL,
                    false,
                    ""
                    )
            );

            request.addField(new NumberField(
                    CK_MAX_INBOUND_MSG_SIZE,
                    "Maximum size of gRPC inbound messages.",
                    GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE,
                    "Maximum size of inbound gRPC messages in bytes. Messages larger than this will be rejected.",
                    NumberField.Attribute.ONLY_POSITIVE)
            );

            return request;
        }
    }


    @Override
    public MetricSet getMetricSet() {
        return localMetricRegistry;
    }
}
