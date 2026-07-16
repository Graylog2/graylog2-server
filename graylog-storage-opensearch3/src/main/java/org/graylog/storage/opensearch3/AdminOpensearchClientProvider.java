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
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.security.certutil.ClientCertSslContextFactory;
import org.graylog.security.certutil.ClientCertSslContextFactoryImpl;
import org.graylog.storage.opensearch3.client.CustomAsyncOpenSearchClient;
import org.graylog.storage.opensearch3.client.CustomOpenSearchClient;
import org.graylog2.configuration.IndexerHosts;
import org.graylog2.indexer.security.IndexerAdminCertConstants;
import org.opensearch.client.transport.OpenSearchTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Clock;
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

    private final ClientCertSslContextFactory sslContextFactory;
    private final List<URI> hosts;
    private final OfficialOpensearchClientProvider transportProvider;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private volatile OfficialOpensearchClient cachedClient;
    private volatile DynamicTransport dynamicTransport;
    private volatile ScheduledExecutorService drainScheduler;
    private volatile Instant currentCertExpiresAt;

    @Inject
    public AdminOpensearchClientProvider(ClientCertSslContextFactory sslContextFactory,
                                         @IndexerHosts List<URI> hosts,
                                         OfficialOpensearchClientProvider transportProvider,
                                         ObjectMapper objectMapper,
                                         Clock clock) {
        this.sslContextFactory = sslContextFactory;
        this.hosts = hosts;
        this.transportProvider = transportProvider;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Returns the admin client. The same {@link OfficialOpensearchClient} instance is returned
     * across the lifetime of this provider; only the internal transport (and underlying cert)
     * is rotated when the cert nears expiry.
     */
    @Nonnull
    public OfficialOpensearchClient getAdminClient() {
        if (cachedClient != null && !needsRefresh(clock.instant())) {
            return cachedClient;
        }
        return initOrRefresh();
    }

    private synchronized OfficialOpensearchClient initOrRefresh() {
        final Instant now = clock.instant();
        if (cachedClient != null && !needsRefresh(now)) {
            return cachedClient;
        }

        try {
            final OpenSearchTransport newTransport = sslContextFactory.buildClientCertSslContext(IndexerAdminCertConstants.ADMIN_CN, CERT_LIFETIME)
                    .map(TransportConfig::clientCertAuth)
                    .map(certAuth -> transportProvider.buildTransport(hosts, certAuth))
                    .orElse(transportProvider.buildTransport(hosts));

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
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build admin OpenSearch client", e);
        }

        return cachedClient;
    }

    private boolean needsRefresh(Instant now) {
        return currentCertExpiresAt == null
                || !now.isBefore(currentCertExpiresAt.minus(REFRESH_BEFORE_EXPIRY));
    }

    private static ScheduledExecutorService createDrainScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "admin-opensearch-transport-drain");
            t.setDaemon(true);
            return t;
        });
    }
}
