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
package org.graylog.storage.opensearch3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.security.certutil.CaKeystore;
import org.graylog.security.certutil.CaKeystoreException;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;
import org.graylog.storage.opensearch3.client.CustomAsyncOpenSearchClient;
import org.graylog.storage.opensearch3.client.CustomOpenSearchClient;
import org.graylog2.cluster.certificates.CertificateSigningRequest;
import org.graylog2.configuration.IndexerHosts;
import org.graylog2.indexer.security.IndexerAdminCertConstants;
import org.graylog2.security.TrustManagerAndSocketFactoryProvider;
import org.opensearch.client.transport.OpenSearchTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.graylog.storage.opensearch3.OfficialOpensearchClientProvider.TransportConfig;

/**
 * Provides an {@link OfficialOpensearchClient} that authenticates via a short-lived in-memory
 * client certificate signed by Graylog's CA. The certificate uses
 * {@link IndexerAdminCertConstants#ADMIN_DN}, which the Data Node configures as
 * {@code plugins.security.authcz.admin_dn} — bypassing the security plugin and granting
 * superuser access for tasks like reindexing system indices.
 *
 * <p>Requires a configured CA (Data Node only). Calls fail if no CA is present.
 *
 * <p>The returned client is stable: the same {@link OfficialOpensearchClient} instance is
 * returned forever, while the underlying transport is hot-swapped through a
 * {@link DynamicTransport} when the cert nears expiry. This makes the returned client safe
 * to cache in adapter constructors.
 */
@Singleton
public class AdminOpensearchClientProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AdminOpensearchClientProvider.class);

    static final Duration CERT_LIFETIME = Duration.ofMinutes(15);
    static final Duration REFRESH_BEFORE_EXPIRY = Duration.ofMinutes(1);
    private static final String KEY_ALIAS = "admin";

    private final CaKeystore caKeystore;
    private final List<URI> hosts;
    private final TrustManagerAndSocketFactoryProvider trustManagerAndSocketFactoryProvider;
    private final OfficialOpensearchClientProvider transportProvider;
    private final ObjectMapper objectMapper;

    private volatile OfficialOpensearchClient cachedClient;
    private volatile DynamicTransport dynamicTransport;
    private volatile ScheduledExecutorService drainScheduler;
    private volatile Instant currentCertExpiresAt;

    @Inject
    public AdminOpensearchClientProvider(CaKeystore caKeystore,
                                         @IndexerHosts List<URI> hosts,
                                         TrustManagerAndSocketFactoryProvider trustManagerAndSocketFactoryProvider,
                                         OfficialOpensearchClientProvider transportProvider,
                                         ObjectMapper objectMapper) {
        this.caKeystore = caKeystore;
        this.hosts = hosts;
        this.trustManagerAndSocketFactoryProvider = trustManagerAndSocketFactoryProvider;
        this.transportProvider = transportProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the admin client. The same {@link OfficialOpensearchClient} instance is returned
     * across the lifetime of this provider; only the internal transport (and underlying cert)
     * is rotated when the cert nears expiry.
     */
    @Nonnull
    public OfficialOpensearchClient getAdminClient() {
        if (cachedClient != null && !needsRefresh(now())) {
            return cachedClient;
        }
        return initOrRefresh();
    }

    /**
     * Overridable for tests that need to control time.
     */
    @VisibleForTesting
    Instant now() {
        return Instant.now();
    }

    private synchronized OfficialOpensearchClient initOrRefresh() {
        final Instant now = now();
        if (cachedClient != null && !needsRefresh(now)) {
            return cachedClient;
        }

        if (!caKeystore.exists()) {
            throw new CaKeystoreException("Cannot build admin OpenSearch client: no CA configured. "
                    + "This feature requires running with the Graylog Data Node.");
        }

        try {
            final SSLContext sslContext = buildAdminSslContext();
            final OpenSearchTransport newTransport = transportProvider.buildTransport(hosts, TransportConfig.clientCertAuth(sslContext));

            if (cachedClient == null) {
                this.drainScheduler = createDrainScheduler();
                this.dynamicTransport = new DynamicTransport(newTransport, drainScheduler);
                this.cachedClient = new OfficialOpensearchClient(
                        new CustomOpenSearchClient(dynamicTransport),
                        new CustomAsyncOpenSearchClient(dynamicTransport),
                        objectMapper);
                LOG.info("Built admin OpenSearch client with a {} min cert lifetime.", CERT_LIFETIME.toMinutes());
            } else {
                dynamicTransport.swap(newTransport);
                LOG.debug("Rotated admin OpenSearch client certificate.");
            }
            this.currentCertExpiresAt = now.plus(CERT_LIFETIME);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build admin OpenSearch client", e);
        }

        return cachedClient;
    }

    private boolean needsRefresh(Instant now) {
        return currentCertExpiresAt == null
                || !now.isBefore(currentCertExpiresAt.minus(REFRESH_BEFORE_EXPIRY));
    }

    private SSLContext buildAdminSslContext() throws Exception {
        final CertRequest certRequest = CertRequest.selfSigned(IndexerAdminCertConstants.ADMIN_CN)
                .isCA(false)
                .validity(CERT_LIFETIME);
        final KeyPair keyPair = CertificateGenerator.generate(certRequest);

        final char[] keystorePassword = RandomStringUtils.secure().nextAlphanumeric(96).toCharArray();
        final KeyStore csrKeystore = keyPair.toKeystore(KEY_ALIAS, keystorePassword);
        final InMemoryKeystoreInformation csrKeystoreInfo = new InMemoryKeystoreInformation(csrKeystore, keystorePassword);

        final PKCS10CertificationRequest csr = CsrGenerator.generateCSR(
                csrKeystoreInfo, KEY_ALIAS, IndexerAdminCertConstants.ADMIN_CN, List.of());
        final CertificateChain certChain = caKeystore.signCertificateRequest(
                new CertificateSigningRequest(IndexerAdminCertConstants.ADMIN_CN, csr), CERT_LIFETIME);

        final KeyStore signedKeystore = KeyStore.getInstance("PKCS12");
        signedKeystore.load(null, null);
        signedKeystore.setKeyEntry(KEY_ALIAS, keyPair.privateKey(), keystorePassword, certChain.toCertificateChainArray());

        final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(signedKeystore, keystorePassword);
        final X509TrustManager trustManager = trustManagerAndSocketFactoryProvider.getTrustManager();
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), new TrustManager[]{trustManager}, new SecureRandom());
        return sslContext;
    }

    private static ScheduledExecutorService createDrainScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "admin-opensearch-transport-drain");
            t.setDaemon(true);
            return t;
        });
    }
}
