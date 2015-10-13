/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.inputs.transports;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Callables;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.transports.util.KeyUtil;
import org.graylog2.plugin.inputs.util.ConnectionCounter;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public abstract class AbstractTcpTransport extends NettyTransport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTcpTransport.class);

    private static final String CK_TLS_CERT_FILE = "tls_cert_file";
    private static final String CK_TLS_KEY_FILE = "tls_key_file";
    private static final String CK_TLS_ENABLE = "tls_enable";
    private static final String CK_TLS_KEY_PASSWORD = "tls_key_password";
    private static final String CK_TLS_CLIENT_AUTH = "tls_client_auth";
    private static final String CK_TLS_CLIENT_AUTH_TRUSTED_CERT_FILE = "tls_client_auth_cert_file";

    private static final String TLS_CLIENT_AUTH_DISABLED = "disabled";
    private static final String TLS_CLIENT_AUTH_OPTIONAL = "optional";
    private static final String TLS_CLIENT_AUTH_REQUIRED = "required";
    private static final Map<String, String> TLS_CLIENT_AUTH_OPTIONS = ImmutableMap.of(
            TLS_CLIENT_AUTH_DISABLED, TLS_CLIENT_AUTH_DISABLED,
            TLS_CLIENT_AUTH_OPTIONAL, TLS_CLIENT_AUTH_OPTIONAL,
            TLS_CLIENT_AUTH_REQUIRED, TLS_CLIENT_AUTH_REQUIRED);

    protected final Executor bossExecutor;
    protected final Executor workerExecutor;
    protected final ConnectionCounter connectionCounter;
    protected final Configuration configuration;

    private final boolean tlsEnable;
    private final String tlsKeyPassword;
    private File tlsCertFile;
    private File tlsKeyFile;
    private final File tlsClientAuthCertFile;
    private final String tlsClientAuth;

    public AbstractTcpTransport(
            Configuration configuration,
            ThroughputCounter throughputCounter,
            LocalMetricRegistry localRegistry,
            Executor bossPool,
            Executor workerPool,
            ConnectionCounter connectionCounter) {
        super(configuration, throughputCounter, localRegistry);
        this.configuration = configuration;
        this.bossExecutor = bossPool;
        this.workerExecutor = workerPool;
        this.connectionCounter = connectionCounter;

        this.tlsEnable = configuration.getBoolean(CK_TLS_ENABLE);
        this.tlsCertFile = getTlsFile(configuration, CK_TLS_CERT_FILE);
        this.tlsKeyFile = getTlsFile(configuration, CK_TLS_KEY_FILE);
        this.tlsKeyPassword = configuration.getString(CK_TLS_KEY_PASSWORD);
        this.tlsClientAuth = configuration.getString(CK_TLS_CLIENT_AUTH, TLS_CLIENT_AUTH_DISABLED);
        this.tlsClientAuthCertFile = getTlsFile(configuration, CK_TLS_CLIENT_AUTH_TRUSTED_CERT_FILE);


        this.localRegistry.register("open_connections", connectionCounter.gaugeCurrent());
        this.localRegistry.register("total_connections", connectionCounter.gaugeTotal());
    }

    private File getTlsFile(Configuration configuration, String configKey) {
        return new File(configuration.getString(configKey, ""));
    }

    @Override
    protected Bootstrap getBootstrap() {
        final ServerBootstrap bootstrap =
                new ServerBootstrap(new NioServerSocketChannelFactory(bossExecutor, workerExecutor));

        bootstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(8192));
        bootstrap.setOption("receiveBufferSize", getRecvBufferSize());
        bootstrap.setOption("child.receiveBufferSize", getRecvBufferSize());

        return bootstrap;
    }

    @Override
    protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getBaseChannelHandlers(
            MessageInput input) {
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> baseChannelHandlers = super.getBaseChannelHandlers(input);
        final LinkedHashMap<String, Callable<? extends ChannelHandler>> handlerList = Maps.newLinkedHashMap();
        baseChannelHandlers.put("connection-counter", Callables.returning(connectionCounter));

        if (!tlsEnable) {
            return baseChannelHandlers;
        }

        if (!tlsCertFile.exists() || !tlsKeyFile.exists()) {
            LOG.warn("TLS key file or certificate file does not exist, creating a self-signed certificate for input [{}/{}].", input.getName(), input.getId());

            try {
                final SelfSignedCertificate ssc = new SelfSignedCertificate(configuration.getString(CK_BIND_ADDRESS) + ":" + configuration.getString(CK_PORT));
                tlsCertFile = ssc.certificate();
                tlsKeyFile = ssc.privateKey();
            } catch (CertificateException e) {
                LOG.error(String.format(Locale.ENGLISH, "Problem creating a self-signed certificate for input [%s/%s].",
                        input.getName(), input.getId()), e);
                return baseChannelHandlers;
            }
        }

        if (tlsCertFile.exists() && tlsKeyFile.exists()) {
            handlerList.put("tls", buildSslHandlerCallable());
        }

        LOG.info("Enabled TLS for input [{}/{}]. key-file=\"{}\" cert-file=\"{}\"", input.getName(), input.getId(), tlsKeyFile, tlsCertFile);
        handlerList.putAll(baseChannelHandlers);

        return handlerList;
    }

    private Callable<ChannelHandler> buildSslHandlerCallable() {
        return new Callable<ChannelHandler>() {
            @Override
            public ChannelHandler call() throws Exception {
                try {
                    return new SslHandler(createSslEngine());
                } catch (SSLException e) {
                    LOG.error("Error creating SSL context. Make sure the certificate and key are in the correct format: cert=X.509 key=PKCS#8");
                    throw e;
                }
            }

            private SSLEngine createSslEngine() throws IOException, GeneralSecurityException {
                final SSLContext instance = SSLContext.getInstance("TLS");
                TrustManager[] initTrustStore = new TrustManager[0];

                if (TLS_CLIENT_AUTH_OPTIONAL.equals(tlsClientAuth) || TLS_CLIENT_AUTH_REQUIRED.equals(tlsClientAuth)) {
                    if (tlsClientAuthCertFile.exists()) {
                        initTrustStore = KeyUtil.initTrustStore(tlsClientAuthCertFile);
                    } else {
                        LOG.warn("client auth configured, but no authorized certificates / certificate authorities configured");
                    }
                }

                instance.init(KeyUtil.initKeyStore(tlsKeyFile, tlsCertFile, tlsKeyPassword), initTrustStore, new SecureRandom());
                final SSLEngine engine = instance.createSSLEngine();

                engine.setUseClientMode(false);

                switch (tlsClientAuth) {
                    case TLS_CLIENT_AUTH_DISABLED:
                        LOG.debug("Not using TLS client authentication");
                        break;
                    case TLS_CLIENT_AUTH_OPTIONAL:
                        LOG.debug("Using optional TLS client authentication");
                        engine.setWantClientAuth(true);
                        break;
                    case TLS_CLIENT_AUTH_REQUIRED:
                        LOG.debug("Using mandatory TLS client authentication");
                        engine.setNeedClientAuth(true);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown TLS client authentication mode: " + tlsClientAuth);
                }

                return engine;
            }
        };
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

            return x;
        }
    }
}
