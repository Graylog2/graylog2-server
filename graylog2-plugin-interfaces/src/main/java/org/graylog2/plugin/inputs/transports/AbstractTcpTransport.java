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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.DatatypeConverter;

import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Callables;

public abstract class AbstractTcpTransport extends NettyTransport {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTcpTransport.class);

    private static final String CK_TLS_CERT_FILE = "tls_cert_file";
    private static final String CK_TLS_KEY_FILE = "tls_key_file";
    private static final String CK_TLS_ENABLE = "tls_enable";
    private static final String CK_TLS_KEY_PASSWORD = "tls_key_password";
    public static final String CK_TLS_NEED_CLIENT_AUTH = "tls_need_client_auth";
    public static final String CK_TLS_WANT_CLIENT_AUTH = "tls_want_client_auth";
    public static final String CK_TLS_CLIENT_AUTH_TRUSTED_CERT_FILE = "tls_client_auth_cert_file";
    private static final Joiner JOINER = Joiner.on("_").skipNulls();

    protected final Executor bossExecutor;
    protected final Executor workerExecutor;
    protected final ConnectionCounter connectionCounter;
    protected final Configuration configuration;

    private final boolean tlsEnable;
    private final String tlsKeyPassword;
    private File tlsCertFile;
    private File tlsKeyFile;
    private final File tlsClientAuthCertFile;
    private final boolean tlsNeedClientAuth;
    private final boolean tlsWantClientAuth;

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
        this.tlsNeedClientAuth = configuration.getBoolean(CK_TLS_NEED_CLIENT_AUTH);
        this.tlsWantClientAuth = configuration.getBoolean(CK_TLS_WANT_CLIENT_AUTH);
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
                LOG.error(String.format("Problem creating a self-signed certificate for input [%s/%s].", input.getName(), input.getId()), e);
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

            private SSLEngine createSslEngine() throws FileNotFoundException, IOException, GeneralSecurityException {
                SSLContext instance = SSLContext.getInstance("TLS");
                TrustManager[] initTrustStore;
                if ((tlsWantClientAuth || tlsNeedClientAuth) && tlsClientAuthCertFile.exists()) {
                    initTrustStore = initTrustStore();
                }
                else {
                    initTrustStore = new TrustManager[0];
                }
                instance.init(initKeyStore(), initTrustStore, new SecureRandom());
                SSLEngine engine = instance.createSSLEngine();
                engine.setUseClientMode(false);
                engine.setNeedClientAuth(tlsNeedClientAuth);
                engine.setNeedClientAuth(tlsWantClientAuth);
                return engine;
            }

            private TrustManager[] initTrustStore()
                    throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
                KeyStore trustStore = KeyStore.getInstance("JKS");
                trustStore.load(null, null);
                loadCertificates(trustStore, tlsClientAuthCertFile, CertificateFactory.getInstance("X.509"));
                LOG.info("TrustStore: " + trustStore + " aliases: " + join(trustStore.aliases()));
                TrustManagerFactory instance = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                instance.init(trustStore);
                return instance.getTrustManagers();
            }

            private void loadCertificates(KeyStore trustStore, File file, CertificateFactory cf)
                    throws CertificateException, KeyStoreException, IOException {
                if (file.isFile()) {
                    List<Certificate> certChain = Lists
                            .newArrayList(cf.generateCertificates(new FileInputStream(file)));
                    for (int i = 0; i < certChain.size(); i++) {
                        Certificate cert = certChain.get(i);
                        trustStore.setCertificateEntry(JOINER.join(file.getAbsolutePath(), i), cert);
                        LOG.debug("adding certificate to truststore:", cert.toString());
                    }
                }
                else if (file.isDirectory()) {
                    for (Path f : Files.newDirectoryStream(file.toPath())) {
                        loadCertificates(trustStore, f.toFile(), cf);
                    }

                }
            }

            private String join(Enumeration<String> aliases) {
                StringBuffer stringBuffer = new StringBuffer();
                while (aliases.hasMoreElements()) {
                    stringBuffer.append(aliases.nextElement());
                    if (aliases.hasMoreElements()) {
                        stringBuffer.append(", ");
                    }
                }
                return stringBuffer.toString();
            }

            private KeyManager[] initKeyStore() throws FileNotFoundException, IOException, GeneralSecurityException {
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(null, null);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                Collection< ? extends Certificate> certChain = cf
                        .generateCertificates(new FileInputStream(tlsCertFile));

                PrivateKey pk = loadPrivateKey(tlsKeyFile);
                char[] password = Strings.isNullOrEmpty(tlsKeyPassword) ? new char[0] : tlsKeyPassword.toCharArray();
                ks.setKeyEntry("key", pk, password, certChain.toArray(new Certificate[certChain.size()]));
                LOG.info("KeyStore: " + ks + " aliases: " + join(ks.aliases()));
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, password);
                return kmf.getKeyManagers();
            }

            public PrivateKey loadPrivateKey(File file) throws IOException, GeneralSecurityException {
                PrivateKey key = null;
                try (InputStream is = new FileInputStream(file)) {

                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    StringBuilder builder = new StringBuilder();
                    boolean inKey = false;
                    for (String line = br.readLine(); line != null; line = br.readLine()) {
                        if (!inKey) {
                            if (line.startsWith("-----BEGIN ") && line.endsWith(" PRIVATE KEY-----")) {
                                inKey = true;
                            }
                            continue;
                        }
                        else {
                            if (line.startsWith("-----END ") && line.endsWith(" PRIVATE KEY-----")) {
                                inKey = false;
                                break;
                            }
                            builder.append(line);
                        }
                    }
                    //
                    byte[] encoded = DatatypeConverter.parseBase64Binary(builder.toString());
                    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    key = kf.generatePrivate(keySpec);
                }
                return key;
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
                new BooleanField(CK_TLS_NEED_CLIENT_AUTH, "TLS Need Client Auth", false, "TLS Need Client Auth"));
            x.addField(
                new BooleanField(CK_TLS_WANT_CLIENT_AUTH, "TLS Want Client Auth", false, "TLS Want Client Auth"));
            x.addField(new TextField(CK_TLS_CLIENT_AUTH_TRUSTED_CERT_FILE, "TLS Client Auth Trusted Certs", "",
                    "TLS Client Auth Trusted Certs  (File or Directory)", ConfigurationField.Optional.OPTIONAL));

            return x;
        }
    }
}
