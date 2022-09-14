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
import org.graylog.shaded.opensearch2.org.apache.http.client.CredentialsProvider;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestClientBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.graylog.shaded.opensearch2.org.opensearch.client.sniff.NodesSniffer;
import org.graylog.shaded.opensearch2.org.opensearch.client.sniff.OpenSearchNodesSniffer;
import org.graylog.shaded.opensearch2.org.opensearch.client.sniff.Sniffer;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Singleton
public class RestHighLevelClientProvider implements Provider<RestHighLevelClient> {
    private final Supplier<RestHighLevelClient> clientSupplier;

    @SuppressWarnings("unused")
    @Inject
    public RestHighLevelClientProvider(
            GracefulShutdownService shutdownService,
            @Named("elasticsearch_hosts") List<URI> hosts,
            @Named("elasticsearch_connect_timeout") Duration connectTimeout,
            @Named("elasticsearch_socket_timeout") Duration socketTimeout,
            @Named("elasticsearch_idle_timeout") Duration elasticsearchIdleTimeout,
            @Named("elasticsearch_max_total_connections") int maxTotalConnections,
            @Named("elasticsearch_max_total_connections_per_route") int maxTotalConnectionsPerRoute,
            @Named("elasticsearch_max_retries") int elasticsearchMaxRetries,
            @Named("elasticsearch_discovery_enabled") boolean discoveryEnabled,
            @Named("elasticsearch_discovery_filter") @Nullable String discoveryFilter,
            @Named("elasticsearch_discovery_frequency") Duration discoveryFrequency,
            @Named("elasticsearch_discovery_default_scheme") String defaultSchemeForDiscoveredNodes,
            @Named("elasticsearch_use_expect_continue") boolean useExpectContinue,
            @Named("elasticsearch_mute_deprecation_warnings") boolean muteOpenSearchDeprecationWarnings,
            CredentialsProvider credentialsProvider) {
        clientSupplier = Suppliers.memoize(() -> {
            final RestHighLevelClient client = buildClient(hosts,
                    connectTimeout,
                    socketTimeout,
                    maxTotalConnections,
                    maxTotalConnectionsPerRoute,
                    useExpectContinue,
                    muteOpenSearchDeprecationWarnings,
                credentialsProvider);

            if (discoveryEnabled) {
                final Sniffer sniffer = createNodeDiscoverySniffer(client.getLowLevelClient(), discoveryFrequency, defaultSchemeForDiscoveredNodes, discoveryFilter);
                shutdownService.register(sniffer::close);
            }

            return client;
        });
    }

    private Sniffer createNodeDiscoverySniffer(RestClient restClient, Duration discoveryFrequency, String defaultSchemeForDiscoveredNodes, String discoveryFilter) {
        final NodesSniffer nodesSniffer = FilteredOpenSearchNodesSniffer.create(
                restClient,
                TimeUnit.SECONDS.toMillis(5),
                mapDefaultScheme(defaultSchemeForDiscoveredNodes),
                discoveryFilter
        );
        return Sniffer.builder(restClient)
                .setSniffIntervalMillis(Math.toIntExact(discoveryFrequency.toMilliseconds()))
                .setNodesSniffer(nodesSniffer)
                .build();
    }

    private OpenSearchNodesSniffer.Scheme mapDefaultScheme(String defaultSchemeForDiscoveredNodes) {
        switch (defaultSchemeForDiscoveredNodes.toUpperCase(Locale.ENGLISH)) {
            case "HTTP": return OpenSearchNodesSniffer.Scheme.HTTP;
            case "HTTPS": return OpenSearchNodesSniffer.Scheme.HTTPS;
            default: throw new IllegalArgumentException("Invalid default scheme for discovered OS nodes: " + defaultSchemeForDiscoveredNodes);
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
            CredentialsProvider credentialsProvider) {
        final HttpHost[] esHosts = hosts.stream().map(uri -> new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme())).toArray(HttpHost[]::new);

        final RestClientBuilder restClientBuilder = RestClient.builder(esHosts)
                .setRequestConfigCallback(requestConfig -> requestConfig
                        .setConnectTimeout(Math.toIntExact(connectTimeout.toMilliseconds()))
                        .setSocketTimeout(Math.toIntExact(socketTimeout.toMilliseconds()))
                        .setExpectContinueEnabled(useExpectContinue)
                        .setAuthenticationEnabled(true)
                )
                .setHttpClientConfigCallback(httpClientConfig -> {
                    httpClientConfig
                        .setMaxConnTotal(maxTotalConnections)
                        .setMaxConnPerRoute(maxTotalConnectionsPerRoute)
                        .setDefaultCredentialsProvider(credentialsProvider);

                    if(muteElasticsearchDeprecationWarnings) {
                        httpClientConfig.addInterceptorFirst(new OpenSearchFilterDeprecationWarningsInterceptor());
                    }

                    return httpClientConfig;
                });

        return new RestHighLevelClient(restClientBuilder);
    }
}
