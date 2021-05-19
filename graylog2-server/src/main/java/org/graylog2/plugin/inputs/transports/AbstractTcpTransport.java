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
package org.graylog2.plugin.inputs.transports;

import com.codahale.metrics.Gauge;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.inputs.transports.netty.ByteBufMessageAggregationHandler;
import org.graylog2.inputs.transports.netty.ChannelRegistrationHandler;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.inputs.transports.netty.ExceptionLoggingChannelHandler;
import org.graylog2.inputs.transports.netty.RawMessageHandler;
import org.graylog2.inputs.transports.netty.ServerSocketChannelFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.util.KeyUtil;
import org.graylog2.plugin.inputs.util.ConnectionCounter;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

public abstract class AbstractTcpTransport extends NettyTransport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTcpTransport.class);

    private static final String CK_TLS_CERT_FILE = "tls_cert_file";
    private static final String CK_TLS_KEY_FILE = "tls_key_file";
    private static final String CK_TLS_ENABLE = "tls_enable";
    private static final String CK_TLS_KEY_PASSWORD = "tls_key_password";
    private static final String CK_TLS_CLIENT_AUTH = "tls_client_auth";
    private static final String CK_TLS_CLIENT_AUTH_TRUSTED_CERT_FILE = "tls_client_auth_cert_file";
    private static final String CK_TCP_KEEPALIVE = "tcp_keepalive";

    private static final String TLS_CLIENT_AUTH_DISABLED = "disabled";
    private static final String TLS_CLIENT_AUTH_OPTIONAL = "optional";
    private static final String TLS_CLIENT_AUTH_REQUIRED = "required";
    private static final ImmutableMap<String, String> TLS_CLIENT_AUTH_OPTIONS = ImmutableMap.of(
            TLS_CLIENT_AUTH_DISABLED, TLS_CLIENT_AUTH_DISABLED,
            TLS_CLIENT_AUTH_OPTIONAL, TLS_CLIENT_AUTH_OPTIONAL,
            TLS_CLIENT_AUTH_REQUIRED, TLS_CLIENT_AUTH_REQUIRED);

    private static final Supplier<Set<String>> secureDefaultCiphers = Suppliers.memoize(AbstractTcpTransport::getSecureCipherSuites);

    private final ConnectionCounter connectionCounter;
    private final AtomicInteger connections;
    private final AtomicLong totalConnections;

    protected final Configuration configuration;
    protected final EventLoopGroup parentEventLoopGroup;
    private final NettyTransportConfiguration nettyTransportConfiguration;
    private final org.graylog2.Configuration graylogConfiguration;
    private final AtomicReference<Channel> channelReference;

    private final boolean tlsEnable;
    private final String tlsKeyPassword;
    private File tlsCertFile;
    private File tlsKeyFile;
    private final File tlsClientAuthCertFile;
    private final String tlsClientAuth;
    private final boolean tcpKeepalive;

    private ChannelGroup childChannels;
    protected EventLoopGroup childEventLoopGroup;
    private ServerBootstrap bootstrap;

    public AbstractTcpTransport(
            Configuration configuration,
            ThroughputCounter throughputCounter,
            LocalMetricRegistry localRegistry,
            EventLoopGroup parentEventLoopGroup,
            EventLoopGroupFactory eventLoopGroupFactory,
            NettyTransportConfiguration nettyTransportConfiguration,
            org.graylog2.Configuration graylogConfiguration) {
        super(configuration, eventLoopGroupFactory, throughputCounter, localRegistry);
        this.configuration = configuration;
        this.parentEventLoopGroup = parentEventLoopGroup;
        this.nettyTransportConfiguration = nettyTransportConfiguration;
        this.graylogConfiguration = graylogConfiguration;
        this.channelReference = new AtomicReference<>();
        this.childChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        this.tlsEnable = configuration.getBoolean(CK_TLS_ENABLE);
        this.tlsCertFile = getTlsFile(configuration, CK_TLS_CERT_FILE);
        this.tlsKeyFile = getTlsFile(configuration, CK_TLS_KEY_FILE);
        this.tlsKeyPassword = configuration.getString(CK_TLS_KEY_PASSWORD);
        this.tlsClientAuth = configuration.getString(CK_TLS_CLIENT_AUTH, TLS_CLIENT_AUTH_DISABLED);
        this.tlsClientAuthCertFile = getTlsFile(configuration, CK_TLS_CLIENT_AUTH_TRUSTED_CERT_FILE);

        this.tcpKeepalive = configuration.getBoolean(CK_TCP_KEEPALIVE);

        this.connections = new AtomicInteger();
        this.totalConnections = new AtomicLong();
        this.connectionCounter = new ConnectionCounter(connections, totalConnections);
        this.localRegistry.register("open_connections", new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return connections.get();
            }
        });
        this.localRegistry.register("total_connections", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return totalConnections.get();
            }
        });
    }

    private File getTlsFile(Configuration configuration, String configKey) {
        return new File(configuration.getString(configKey, ""));
    }

    protected ServerBootstrap getBootstrap(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> parentHandlers = getChannelHandlers(input);
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> childHandlers = getChildChannelHandlers(input);

        childEventLoopGroup = eventLoopGroupFactory.create(workerThreads, localRegistry, "workers");

        return new ServerBootstrap()
                .group(parentEventLoopGroup, childEventLoopGroup)
                .channelFactory(new ServerSocketChannelFactory(nettyTransportConfiguration.getType()))
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(8192))
                .option(ChannelOption.SO_RCVBUF, getRecvBufferSize())
                .childOption(ChannelOption.SO_RCVBUF, getRecvBufferSize())
                .childOption(ChannelOption.SO_KEEPALIVE, tcpKeepalive)
                .handler(getChannelInitializer(parentHandlers))
                .childHandler(getChannelInitializer(childHandlers));
    }

    @Override
    public void launch(final MessageInput input) throws MisfireException {
        try {
            bootstrap = getBootstrap(input);
            bootstrap.bind(socketAddress)
                    .addListener(new InputLaunchListener(channelReference, input, getRecvBufferSize()))
                    .syncUninterruptibly();
        } catch (Exception e) {
            throw new MisfireException(e);
        }
    }

    @Nullable
    @Override
    public SocketAddress getLocalAddress() {
        final Channel channel = channelReference.get();
        if (channel != null) {
            return channel.localAddress();
        }

        return null;
    }

    @Override
    public void stop() {
        final Channel channel = channelReference.get();
        if (channel != null) {
            channel.close();
            channel.closeFuture().syncUninterruptibly();
        }

        childChannels.close().syncUninterruptibly();

        if (childEventLoopGroup != null) {
            childEventLoopGroup.shutdownGracefully();
        }
        bootstrap = null;
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getChildChannelHandlers(MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlers = new LinkedHashMap<>();
        final CodecAggregator aggregator = getAggregator();

        handlers.put("channel-registration", () -> new ChannelRegistrationHandler(childChannels));
        handlers.put("traffic-counter", () -> throughputCounter);
        handlers.put("connection-counter", () -> connectionCounter);
        if (tlsEnable) {
            LOG.info("Enabled TLS for input [{}/{}]. key-file=\"{}\" cert-file=\"{}\"", input.getName(), input.getId(), tlsKeyFile, tlsCertFile);
            handlers.put("tls", getSslHandlerCallable(input));
        }
        handlers.putAll(getCustomChildChannelHandlers(input));
        if (aggregator != null) {
            LOG.debug("Adding codec aggregator {} to channel pipeline", aggregator);
            handlers.put("codec-aggregator", () -> new ByteBufMessageAggregationHandler(aggregator, localRegistry));
        }
        handlers.put("rawmessage-handler", () -> new RawMessageHandler(input));
        handlers.put("exception-logger", () -> new ExceptionLoggingChannelHandler(input, LOG, this.tcpKeepalive));

        return handlers;
    }

    private Callable<ChannelHandler> getSslHandlerCallable(MessageInput input) {
        final File certFile;
        final File keyFile;
        if (tlsCertFile.exists() && tlsKeyFile.exists()) {
            certFile = tlsCertFile;
            keyFile = tlsKeyFile;
        } else {
            LOG.warn("TLS key file or certificate file does not exist, creating a self-signed certificate for input [{}/{}].", input.getName(), input.getId());

            final String tmpDir = System.getProperty("java.io.tmpdir");
            checkState(tmpDir != null, "The temporary directory must not be null!");
            final Path tmpPath = Paths.get(tmpDir);
            if (!Files.isDirectory(tmpPath) || !Files.isWritable(tmpPath)) {
                throw new IllegalStateException("Couldn't write to temporary directory: " + tmpPath.toAbsolutePath());
            }

            try {
                final SelfSignedCertificate ssc = new SelfSignedCertificate(configuration.getString(CK_BIND_ADDRESS) + ":" + configuration.getString(CK_PORT));
                certFile = ssc.certificate();

                if (!Strings.isNullOrEmpty(tlsKeyPassword)) {
                    keyFile = KeyUtil.generatePKCS8FromPrivateKey(tmpPath, tlsKeyPassword.toCharArray(), ssc.key());
                    ssc.privateKey().delete();
                }
                else {
                    keyFile = ssc.privateKey();
                }
            } catch (GeneralSecurityException e) {
                final String msg = String.format(Locale.ENGLISH, "Problem creating a self-signed certificate for input [%s/%s].", input.getName(), input.getId());
                throw new IllegalStateException(msg, e);
            }
        }

        final ClientAuth clientAuth;
        switch (tlsClientAuth) {
            case TLS_CLIENT_AUTH_DISABLED:
                LOG.debug("Not using TLS client authentication");
                clientAuth = ClientAuth.NONE;
                break;
            case TLS_CLIENT_AUTH_OPTIONAL:
                LOG.debug("Using optional TLS client authentication");
                clientAuth = ClientAuth.OPTIONAL;
                break;
            case TLS_CLIENT_AUTH_REQUIRED:
                LOG.debug("Using mandatory TLS client authentication");
                clientAuth = ClientAuth.REQUIRE;
                break;
            default:
                throw new IllegalArgumentException("Unknown TLS client authentication mode: " + tlsClientAuth);
        }

        return buildSslHandlerCallable(nettyTransportConfiguration.getTlsProvider(), certFile, keyFile, tlsKeyPassword, clientAuth, tlsClientAuthCertFile, input);
    }

    private Callable<ChannelHandler> buildSslHandlerCallable(SslProvider tlsProvider, File certFile, File keyFile, String password, ClientAuth clientAuth, File clientAuthCertFile, MessageInput input) {
        return new Callable<ChannelHandler>() {
            @Override
            public ChannelHandler call() throws Exception {
                try {
                    return new SslHandler(createSslEngine(input));
                } catch (SSLException e) {
                    LOG.error("Error creating SSL context. Make sure the certificate and key are in the correct format: cert=X.509 key=PKCS#8");
                    throw e;
                }
            }

            private SSLEngine createSslEngine(MessageInput input) throws IOException, CertificateException, OperatorCreationException, PKCSException {
                final X509Certificate[] clientAuthCerts;
                if (EnumSet.of(ClientAuth.OPTIONAL, ClientAuth.REQUIRE).contains(clientAuth)) {
                    if (clientAuthCertFile.exists()) {
                        clientAuthCerts = KeyUtil.loadX509Certificates(clientAuthCertFile.toPath());
                    } else {
                        LOG.warn("Client auth configured, but no authorized certificates / certificate authorities configured for input [{}/{}]",
                                input.getName(), input.getId());
                        clientAuthCerts = null;
                    }
                } else {
                    clientAuthCerts = null;
                }

                // Netty's SSLContextBuilder chokes on some PKCS8 key file formats. So we need to pass a
                // private key and keyCertChain instead of the corresponding files.
                PrivateKey privateKey = KeyUtil.privateKeyFromFile(password, keyFile);
                X509Certificate[] keyCertChain = KeyUtil.loadX509Certificates(certFile.toPath());
                final SslContextBuilder sslContext = SslContextBuilder.forServer(privateKey, keyCertChain)
                        .sslProvider(tlsProvider)
                        .clientAuth(clientAuth)
                        .trustManager(clientAuthCerts);
                if (!graylogConfiguration.getEnabledTlsProtocols().isEmpty()) {
                    sslContext.protocols(graylogConfiguration.getEnabledTlsProtocols());
                }
                if (tlsProvider.equals(SslProvider.OPENSSL)) {
                    // Netty tcnative does not adhere jdk.tls.disabledAlgorithms: https://github.com/netty/netty-tcnative/issues/530
                    // We need to build our own cipher list
                    sslContext.ciphers(secureDefaultCiphers.get());
                }

                // TODO: Use byte buffer allocator of channel
                return sslContext.build().newEngine(ByteBufAllocator.DEFAULT);
            }
        };
    }

    private static Set<String> getSecureCipherSuites() {
        final Set<String> openSslCipherSuites = OpenSsl.availableOpenSslCipherSuites();
        return openSslCipherSuites.stream().filter(s -> !(s.contains("CBC") || s.contains("AES128-SHA") || s.contains("AES256-SHA") )).collect(Collectors.toSet());
    }

    @ConfigClass
    public static class Config extends NettyTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest x = super.getRequestedConfiguration();

            x.addField(
                    new TextField(
                            CK_TLS_CERT_FILE,
                            "TLS cert file",
                            "",
                            "Path to the TLS certificate file",
                            ConfigurationField.Optional.OPTIONAL
                    )
            );
            x.addField(
                    new TextField(
                            CK_TLS_KEY_FILE,
                            "TLS private key file",
                            "",
                            "Path to the TLS private key file",
                            ConfigurationField.Optional.OPTIONAL
                    )
            );
            x.addField(
                    new BooleanField(
                            CK_TLS_ENABLE,
                            "Enable TLS",
                            false,
                            "Accept TLS connections"
                    )
            );
            x.addField(
                    new TextField(
                            CK_TLS_KEY_PASSWORD,
                            "TLS key password",
                            "",
                            "The password for the encrypted key file.",
                            ConfigurationField.Optional.OPTIONAL,
                            TextField.Attribute.IS_PASSWORD
                    )
            );
            x.addField(
                    new DropdownField(
                            CK_TLS_CLIENT_AUTH,
                            "TLS client authentication",
                            TLS_CLIENT_AUTH_DISABLED,
                            TLS_CLIENT_AUTH_OPTIONS,
                            "Whether clients need to authenticate themselves in a TLS connection",
                            ConfigurationField.Optional.OPTIONAL
                    )
            );
            x.addField(
                    new TextField(
                            CK_TLS_CLIENT_AUTH_TRUSTED_CERT_FILE,
                            "TLS Client Auth Trusted Certs",
                            "",
                            "TLS Client Auth Trusted Certs  (File or Directory)",
                            ConfigurationField.Optional.OPTIONAL)
            );
            x.addField(
                    new BooleanField(
                            CK_TCP_KEEPALIVE,
                            "TCP keepalive",
                            false,
                            "Enable TCP keepalive packets"
                    )
            );

            return x;
        }
    }

    private static class InputLaunchListener implements ChannelFutureListener {
        private final AtomicReference<Channel> channelReference;
        private final MessageInput input;
        private final int expectedRecvBufferSize;

        public InputLaunchListener(AtomicReference<Channel> channelReference, MessageInput input, int expectedRecvBufferSize) {
            this.channelReference = channelReference;
            this.input = input;
            this.expectedRecvBufferSize = expectedRecvBufferSize;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                final Channel channel = future.channel();
                channelReference.set(channel);
                LOG.debug("Started channel {}", channel);

                final ServerSocketChannelConfig channelConfig = (ServerSocketChannelConfig) channel.config();
                final int receiveBufferSize = channelConfig.getReceiveBufferSize();
                if (receiveBufferSize != expectedRecvBufferSize) {
                    LOG.warn("receiveBufferSize (SO_RCVBUF) for input {} (channel {}) should be {} but is {}.",
                            input, channel, expectedRecvBufferSize, receiveBufferSize);
                }
            } else {
                LOG.warn("Failed to start channel for input {}", input, future.cause());
            }
        }
    }
}
