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
package org.graylog2.bootstrap.preflight.web.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import okhttp3.OkHttpClient;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.security.CustomCAX509TrustManager;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.versionprobe.VersionProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Optional;

@Singleton
public class PreflightConnectionChecker {

    private static final Logger LOG = LoggerFactory.getLogger(PreflightConnectionChecker.class);
    private final ObjectMapper objectMapper;
    private final IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider;
    private final CustomCAX509TrustManager trustManager;

    @Inject
    public PreflightConnectionChecker(ObjectMapper objectMapper, final IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider,
                                      final CustomCAX509TrustManager trustManager) {

        this.objectMapper = objectMapper;
        this.indexerJwtAuthTokenProvider = indexerJwtAuthTokenProvider;
        this.trustManager = trustManager;
    }

    public Optional<SearchVersion> checkConnection(DataNodeDto node) {
        final OkHttpClient okHttpClient = buildConnectivityCheckOkHttpClient(trustManager, indexerJwtAuthTokenProvider);
        final VersionProbe versionProbe = new VersionProbe(objectMapper, okHttpClient, 1, Duration.seconds(1), true, true, indexerJwtAuthTokenProvider);
        try {
            return versionProbe.probe(Collections.singletonList(URI.create(node.getTransportAddress())));
            // TODO: version probe doesn't throw exceptions, it just logs them. But we need access to them!
        } catch (Exception e) {
            LOG.error("Failed to reach datanode", e);
            return Optional.empty();
        }
    }

    private static OkHttpClient buildConnectivityCheckOkHttpClient(final X509TrustManager trustManager, IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider) {
        try {
            final var clientBuilder = new OkHttpClient.Builder();
            final var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);

            clientBuilder.authenticator((route, response) -> response.request()
                    .newBuilder()
                    .header("Authorization", indexerJwtAuthTokenProvider.get())
                    .build());

            return clientBuilder.build();
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            LOG.error("Could not set Graylog CA trust manager: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

}
