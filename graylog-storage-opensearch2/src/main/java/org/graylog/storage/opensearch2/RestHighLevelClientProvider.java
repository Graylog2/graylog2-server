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

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.base.Suppliers;
import org.graylog.shaded.opensearch2.org.apache.http.HttpHost;
import org.graylog.shaded.opensearch2.org.apache.http.HttpRequestInterceptor;
import org.graylog.shaded.opensearch2.org.apache.http.client.CredentialsProvider;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestClientBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.sniff.OpenSearchNodesSniffer;
import org.graylog2.configuration.IndexerHosts;
import org.graylog2.configuration.RunsWithDataNode;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.graylog2.security.TrustManagerAndSocketFactoryProvider;
import org.graylog2.system.shutdown.GracefulShutdownService;

import javax.annotation.Nullable;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Singleton
public class RestHighLevelClientProvider implements Provider<RestHighLevelClient> {
    private final Supplier<RestHighLevelClient> clientSupplier;
    private final TrustManagerAndSocketFactoryProvider trustManagerAndSocketFactoryProvider;

    @SuppressWarnings("unused")
    @Inject
    public RestHighLevelClientProvider(
            GracefulShutdownService shutdownService,
            @IndexerHosts List<URI> hosts,
            @Named("elasticsearch_connect_timeout") Duration connectTimeout,
            @Named("elasticsearch_socket_timeout") Duration socketTimeout,
            @Named("elasticsearch_idle_timeout") Duration elasticsearchIdleTimeout,
            @Named("elasticsearch_max_total_connections") int maxTotalConnections,
            @Named("elasticsearch_max_total_connections_per_route") int maxTotalConnectionsPerRoute,
            @Named("elasticsearch_max_retries") int elasticsearchMaxRetries,
            @Named("elasticsearch_discovery_enabled") boolean discoveryEnabled,
            @Named("elasticsearch_node_activity_logger_enabled") boolean nodeActivity,
            @Named("elasticsearch_discovery_filter") @Nullable String discoveryFilter,
            @Named("elasticsearch_discovery_frequency") Duration discoveryFrequency,
            @Named("elasticsearch_discovery_default_scheme") String defaultSchemeForDiscoveredNodes,
            @Named("elasticsearch_use_expect_continue") boolean useExpectContinue,
            @Named("elasticsearch_mute_deprecation_warnings") boolean muteOpenSearchDeprecationWarnings,
            CredentialsProvider credentialsProvider,
            TrustManagerAndSocketFactoryProvider trustManagerAndSocketFactoryProvider,
            @RunsWithDataNode Boolean runsWithDataNode,
            @Named("indexer_use_jwt_authentication") boolean indexerUseJwtAuthentication,
            IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider) {

        this.trustManagerAndSocketFactoryProvider = trustManagerAndSocketFactoryProvider;

        clientSupplier = Suppliers.memoize(() -> {
            final RestHighLevelClient client = buildClient(hosts,
                    connectTimeout,
                    socketTimeout,
                    maxTotalConnections,
                    maxTotalConnectionsPerRoute,
                    useExpectContinue,
                    muteOpenSearchDeprecationWarnings,
                    credentialsProvider,
                    runsWithDataNode || indexerUseJwtAuthentication,
                    indexerJwtAuthTokenProvider);

            var sniffer = LegacySnifferWrapper.create(
                    client.getLowLevelClient(),
                    TimeUnit.SECONDS.toMillis(5),
                    discoveryFrequency,
                    mapDefaultScheme(defaultSchemeForDiscoveredNodes)
            );

            if (discoveryEnabled) {
                sniffer.add(LegacyFilteredOpenSearchNodesSniffer.create(discoveryFilter));
            }
            if (nodeActivity) {
                sniffer.add(LegacyNodeListSniffer.create());
            }

            sniffer.build().ifPresent(s -> shutdownService.register(s::close));

            return client;
        });
    }

    private OpenSearchNodesSniffer.Scheme mapDefaultScheme(String defaultSchemeForDiscoveredNodes) {
        switch (defaultSchemeForDiscoveredNodes.toUpperCase(Locale.ENGLISH)) {
            case "HTTP":
                return OpenSearchNodesSniffer.Scheme.HTTP;
            case "HTTPS":
                return OpenSearchNodesSniffer.Scheme.HTTPS;
            default:
                throw new IllegalArgumentException("Invalid default scheme for discovered OS nodes: " + defaultSchemeForDiscoveredNodes);
        }
    }

    @Override
    public RestHighLevelClient get() {
        return this.clientSupplier.get();
    }

    private RestHighLevelClient buildClient(
            List<URI> hosts,
            Duration connectTimeout,
            Duration socketTimeout,
            int maxTotalConnections,
            int maxTotalConnectionsPerRoute,
            boolean useExpectContinue,
            boolean muteElasticsearchDeprecationWarnings,
            CredentialsProvider credentialsProvider,
            boolean isJwtAuthentication,
            final IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider) {
        final HttpHost[] esHosts = hosts.stream().map(uri -> new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme())).toArray(HttpHost[]::new);
        final RestClientBuilder restClientBuilder = RestClient.builder(esHosts)
                .setRequestConfigCallback(requestConfig -> {
                            requestConfig
                                    .setConnectTimeout(Math.toIntExact(connectTimeout.toMilliseconds()))
                                    .setSocketTimeout(Math.toIntExact(socketTimeout.toMilliseconds()))
                                    .setExpectContinueEnabled(useExpectContinue);
                            // manually handle Auth if we use JWT
                            if (!isJwtAuthentication) {
                                requestConfig.setAuthenticationEnabled(true);
                            }
                            return requestConfig;
                        }
                )
                .setHttpClientConfigCallback(httpClientConfig -> {
                    httpClientConfig
                            .setMaxConnTotal(maxTotalConnections)
                            .setMaxConnPerRoute(maxTotalConnectionsPerRoute);

                    if (isJwtAuthentication) {
                        httpClientConfig.addInterceptorLast((HttpRequestInterceptor) (request, context) -> request.addHeader("Authorization", indexerJwtAuthTokenProvider.get()));
                    } else {
                        httpClientConfig.setDefaultCredentialsProvider(credentialsProvider);
                    }

                    if (muteElasticsearchDeprecationWarnings) {
                        httpClientConfig.addInterceptorFirst(new LegacyOpenSearchFilterDeprecationWarningsInterceptor());
                    }

                    if (hosts.stream().anyMatch(host -> host.getScheme().equalsIgnoreCase("https"))) {
                        httpClientConfig.setSSLContext(trustManagerAndSocketFactoryProvider.getSslContext());
                    }

                    return httpClientConfig;
                });

        return new RestHighLevelClient(restClientBuilder);
    }
}
