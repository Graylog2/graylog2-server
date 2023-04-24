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
package org.graylog.storage.elasticsearch7;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.base.Suppliers;
import org.graylog.shaded.elasticsearch7.org.apache.http.HttpHost;
import org.graylog.shaded.elasticsearch7.org.apache.http.client.CredentialsProvider;
import org.graylog.shaded.elasticsearch7.org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.graylog.shaded.elasticsearch7.org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.graylog.shaded.elasticsearch7.org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.graylog.shaded.elasticsearch7.org.apache.http.nio.reactor.IOReactorException;
import org.graylog.shaded.elasticsearch7.org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestClient;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestClientBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.sniff.ElasticsearchNodesSniffer;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.sniff.NodesSniffer;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.sniff.Sniffer;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Singleton
public class RestHighLevelClientProvider implements Provider<RestHighLevelClient> {
    private static final Logger LOG = LoggerFactory.getLogger(RestHighLevelClientProvider.class);
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
            @Named("elasticsearch_mute_deprecation_warnings") boolean muteElasticsearchDeprecationWarnings,
            CredentialsProvider credentialsProvider) {
        clientSupplier = Suppliers.memoize(() -> {
            final RestHighLevelClient client = buildClient(hosts,
                    connectTimeout,
                    socketTimeout,
                    maxTotalConnections,
                    maxTotalConnectionsPerRoute,
                    useExpectContinue,
                    muteElasticsearchDeprecationWarnings,
                credentialsProvider);

            if (discoveryEnabled) {
                final Sniffer sniffer = createNodeDiscoverySniffer(client.getLowLevelClient(), discoveryFrequency, defaultSchemeForDiscoveredNodes, discoveryFilter);
                shutdownService.register(sniffer::close);
            }

            return client;
        });
    }

    private Sniffer createNodeDiscoverySniffer(RestClient restClient, Duration discoveryFrequency, String defaultSchemeForDiscoveredNodes, String discoveryFilter) {
        final NodesSniffer nodesSniffer = FilteredElasticsearchNodesSniffer.create(
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

    private ElasticsearchNodesSniffer.Scheme mapDefaultScheme(String defaultSchemeForDiscoveredNodes) {
        switch (defaultSchemeForDiscoveredNodes.toUpperCase(Locale.ENGLISH)) {
            case "HTTP": return ElasticsearchNodesSniffer.Scheme.HTTP;
            case "HTTPS": return ElasticsearchNodesSniffer.Scheme.HTTPS;
            default: throw new IllegalArgumentException("Invalid default scheme for discovered ES nodes: " + defaultSchemeForDiscoveredNodes);
        }
    }

    @Override
    public RestHighLevelClient get() {
        return this.clientSupplier.get();
    }

    public static class CustomHttpClientConfigCallback implements RestClientBuilder.HttpClientConfigCallback {
        @Override
        public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {

            // Add custom exception handler.
            // - https://hc.apache.org/httpcomponents-core-ga/tutorial/html/nio.html#d5e601
            // - This always handles the exception and just logs a warning.
            try {
                DefaultConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
                ioReactor.setExceptionHandler(new IOReactorExceptionHandler() {
                    @Override
                    public boolean handle(IOException e) {
                        LOG.warn("System may be unstable: IOReactor encountered a checked exception : " + e.getMessage(), e);
                        return true; // Return true to note this exception as handled, it will not be re-thrown
                    }

                    @Override
                    public boolean handle(RuntimeException e) {
                        LOG.warn("System may be unstable: IOReactor encountered a runtime exception : " + e.getMessage(), e);
                        return true; // Return true to note this exception as handled, it will not be re-thrown
                    }
                });

                httpClientBuilder.setConnectionManager(new PoolingNHttpClientConnectionManager(ioReactor));
            } catch (IOReactorException e) {
                throw new RuntimeException(e);
            }

            return httpClientBuilder;
        }
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

        CustomHttpClientConfigCallback configurationCallback = new CustomHttpClientConfigCallback();

        HttpAsyncClientBuilder httpClientConfig = HttpAsyncClientBuilder.create();

        httpClientConfig.setMaxConnTotal(maxTotalConnections)
                .setMaxConnPerRoute(maxTotalConnectionsPerRoute)
                .setDefaultCredentialsProvider(credentialsProvider);

        if (muteElasticsearchDeprecationWarnings) {
            httpClientConfig.addInterceptorFirst(new ElasticsearchFilterDeprecationWarningsInterceptor());
        }

        configurationCallback.customizeHttpClient(httpClientConfig);

        final RestClientBuilder restClientBuilder = RestClient.builder(esHosts)
                .setRequestConfigCallback(requestConfig -> requestConfig
                        .setConnectTimeout(Math.toIntExact(connectTimeout.toMilliseconds()))
                        .setSocketTimeout(Math.toIntExact(socketTimeout.toMilliseconds()))
                        .setExpectContinueEnabled(useExpectContinue)
                        .setAuthenticationEnabled(true)
                )
                .setHttpClientConfigCallback(configurationCallback);

        return new RestHighLevelClient(restClientBuilder);
    }
}
