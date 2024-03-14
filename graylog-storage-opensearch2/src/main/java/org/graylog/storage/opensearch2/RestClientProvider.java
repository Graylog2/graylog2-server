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
package org.graylog.storage.opensearch2;

import com.google.common.base.Suppliers;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.CredentialsProvider;
import org.graylog2.configuration.ElasticsearchClientConfiguration;
import org.graylog2.configuration.IndexerHosts;
import org.graylog2.configuration.RunsWithDataNode;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.graylog2.security.TrustManagerAndSocketFactoryProvider;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.jetbrains.annotations.NotNull;
import org.opensearch.client.RestClient;
import org.opensearch.client.sniff.OpenSearchNodesSniffer;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Singleton
public class RestClientProvider implements Provider<RestClient> {
    private final Supplier<RestClient> clientSupplier;
    private final GracefulShutdownService shutdownService;
    private final ElasticsearchClientConfiguration configuration;
    private final CredentialsProvider credentialsProvider;
    private final TrustManagerAndSocketFactoryProvider trustManagerAndSocketFactoryProvider;
    private final Boolean runsWithDataNode;
    private final IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider;

    @Inject
    public RestClientProvider(
            GracefulShutdownService shutdownService,
            @IndexerHosts List<URI> hosts,
            ElasticsearchClientConfiguration configuration,
            CredentialsProvider credentialsProvider,
            TrustManagerAndSocketFactoryProvider trustManagerAndSocketFactoryProvider,
            @RunsWithDataNode Boolean runsWithDataNode,
            IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider) {
        this.shutdownService = shutdownService;
        this.configuration = configuration;
        this.credentialsProvider = credentialsProvider;
        this.trustManagerAndSocketFactoryProvider = trustManagerAndSocketFactoryProvider;
        this.runsWithDataNode = runsWithDataNode;
        this.indexerJwtAuthTokenProvider = indexerJwtAuthTokenProvider;
        this.clientSupplier = Suppliers.memoize(() -> createClient(hosts));
    }


    @NotNull
    private RestClient createClient(List<URI> hosts) {
        final RestClient client = buildBasicRestClient(hosts);

        var sniffer = SnifferWrapper.create(
                client,
                TimeUnit.SECONDS.toMillis(5),
                configuration.discoveryFrequency(),
                mapDefaultScheme(configuration.defaultSchemeForDiscoveredNodes())
        );

        if (configuration.discoveryEnabled()) {
            sniffer.add(FilteredOpenSearchNodesSniffer.create(configuration.discoveryFilter()));
        }
        if (configuration.isNodeActivityLogger()) {
            sniffer.add(NodeListSniffer.create());
        }

        sniffer.build().ifPresent(s -> shutdownService.register(s::close));
        return client;
    }

    private OpenSearchNodesSniffer.Scheme mapDefaultScheme(String defaultSchemeForDiscoveredNodes) {
        return switch (defaultSchemeForDiscoveredNodes.toUpperCase(Locale.ENGLISH)) {
            case "HTTP" -> OpenSearchNodesSniffer.Scheme.HTTP;
            case "HTTPS" -> OpenSearchNodesSniffer.Scheme.HTTPS;
            default ->
                    throw new IllegalArgumentException("Invalid default scheme for discovered OS nodes: " + defaultSchemeForDiscoveredNodes);
        };
    }

    @Override
    public RestClient get() {
        return this.clientSupplier.get();
    }

    public RestClient buildBasicRestClient(List<URI> hosts) {
        boolean isJwtAuthentication = runsWithDataNode || configuration.indexerUseJwtAuthentication();
        final HttpHost[] esHosts = hosts.stream().map(uri -> new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme())).toArray(HttpHost[]::new);
        final var restClientBuilder = RestClient.builder(esHosts)
                .setRequestConfigCallback(requestConfig -> {
                            requestConfig
                                    .setConnectTimeout(Math.toIntExact(configuration.elasticsearchConnectTimeout().toMilliseconds()))
                                    .setSocketTimeout(Math.toIntExact(configuration.elasticsearchSocketTimeout().toMilliseconds()))
                                    .setExpectContinueEnabled(configuration.useExpectContinue());
                            // manually handle Auth if we use JWT
                            if (!isJwtAuthentication) {
                                requestConfig.setAuthenticationEnabled(true);
                            }
                            return requestConfig;
                        }
                )
                .setHttpClientConfigCallback(httpClientConfig -> {
                    httpClientConfig
                            .setMaxConnTotal(configuration.elasticsearchMaxTotalConnections())
                            .setMaxConnPerRoute(configuration.elasticsearchMaxTotalConnectionsPerRoute());

                    if (isJwtAuthentication) {
                        httpClientConfig.addInterceptorLast((HttpRequestInterceptor) (request, context) -> request.addHeader("Authorization", indexerJwtAuthTokenProvider.get()));
                    } else {
                        httpClientConfig.setDefaultCredentialsProvider(credentialsProvider);
                    }

                    if (configuration.muteDeprecationWarnings()) {
                        httpClientConfig.addInterceptorFirst(new OpenSearchFilterDeprecationWarningsInterceptor());
                    }

                    if (hosts.stream().anyMatch(host -> host.getScheme().equalsIgnoreCase("https"))) {
                        httpClientConfig.setSSLContext(trustManagerAndSocketFactoryProvider.getSslContext());
                    }
                    return httpClientConfig;
                })
                .setChunkedEnabled(configuration.compressionEnabled());

        return restClientBuilder.build();
    }
}
