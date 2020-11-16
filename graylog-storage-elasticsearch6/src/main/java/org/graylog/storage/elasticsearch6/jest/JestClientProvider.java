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
package org.graylog.storage.elasticsearch6.jest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.graylog2.indexer.cluster.jest.RequestResponseLogger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Singleton
public class JestClientProvider implements Provider<JestClient> {
    private final JestClientFactory factory;
    private final CredentialsProvider credentialsProvider;
    private final AtomicReference<JestClient> instance = new AtomicReference<>();

    @Inject
    public JestClientProvider(@Named("elasticsearch_hosts") List<URI> elasticsearchHosts,
                              @Named("elasticsearch_connect_timeout") Duration elasticsearchConnectTimeout,
                              @Named("elasticsearch_socket_timeout") Duration elasticsearchSocketTimeout,
                              @Named("elasticsearch_idle_timeout") Duration elasticsearchIdleTimeout,
                              @Named("elasticsearch_max_total_connections") int elasticsearchMaxTotalConnections,
                              @Named("elasticsearch_max_total_connections_per_route") int elasticsearchMaxTotalConnectionsPerRoute,
                              @Named("elasticsearch_max_retries") int elasticsearchMaxRetries,
                              @Named("elasticsearch_discovery_enabled") boolean discoveryEnabled,
                              @Named("elasticsearch_discovery_filter") @Nullable String discoveryFilter,
                              @Named("elasticsearch_discovery_frequency") Duration discoveryFrequency,
                              @Named("elasticsearch_discovery_default_scheme") String defaultSchemeForDiscoveredNodes,
                              @Named("elasticsearch_compression_enabled") boolean compressionEnabled,
                              @Named("elasticsearch_discovery_default_user") @Nullable String defaultUserForDiscoveredNodes,
                              @Named("elasticsearch_discovery_default_password") @Nullable String defaultPasswordForDiscoveredNodes,
                              ObjectMapper objectMapper) {
        this.factory = new JestClientFactory() {
            @Override
            protected HttpClientBuilder configureHttpClient(HttpClientBuilder builder) {
                return super.configureHttpClient(builder)
                    .addInterceptorLast(new RequestResponseLogger())
                    .disableAutomaticRetries();
            }
        };
        this.credentialsProvider = new BasicCredentialsProvider();
        final Set<HttpHost> preemptiveAuthHosts = new HashSet<>();
        final List<String> hosts = elasticsearchHosts.stream()
            .map(hostUri -> {
                if (!Strings.isNullOrEmpty(hostUri.getUserInfo())) {
                    final Iterator<String> splittedUserInfo = Splitter.on(":")
                        .split(hostUri.getUserInfo())
                        .iterator();
                    if (splittedUserInfo.hasNext()) {
                        final String username = splittedUserInfo.next();
                        final String password = splittedUserInfo.hasNext() ? splittedUserInfo.next() : null;
                        credentialsProvider.setCredentials(
                            new AuthScope(hostUri.getHost(), hostUri.getPort(), AuthScope.ANY_REALM, hostUri.getScheme()),
                            new UsernamePasswordCredentials(username, password)
                        );

                        if (!Strings.isNullOrEmpty(username) || !Strings.isNullOrEmpty(password)) {
                            preemptiveAuthHosts.add(new HttpHost(hostUri.getHost(), hostUri.getPort(), hostUri.getScheme()));
                        }
                    }
                }
                return hostUri.toString();
            })
            .collect(Collectors.toList());

        if (discoveryEnabled && !Strings.isNullOrEmpty(defaultUserForDiscoveredNodes) && !Strings.isNullOrEmpty(defaultPasswordForDiscoveredNodes)) {
            credentialsProvider.setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, defaultSchemeForDiscoveredNodes),
                    new UsernamePasswordCredentials(defaultUserForDiscoveredNodes, defaultPasswordForDiscoveredNodes)
            );
        }

        final HttpClientConfig.Builder httpClientConfigBuilder = new HttpClientConfig
                .Builder(hosts)
                .credentialsProvider(credentialsProvider)
                .connTimeout(Math.toIntExact(elasticsearchConnectTimeout.toMilliseconds()))
                .readTimeout(Math.toIntExact(elasticsearchSocketTimeout.toMilliseconds()))
                .maxConnectionIdleTime(elasticsearchIdleTimeout.toSeconds(), TimeUnit.SECONDS)
                .maxTotalConnection(elasticsearchMaxTotalConnections)
                .defaultMaxTotalConnectionPerRoute(elasticsearchMaxTotalConnectionsPerRoute)
                .multiThreaded(true)
                .discoveryEnabled(discoveryEnabled)
                .discoveryFilter(discoveryFilter)
                .discoveryFrequency(discoveryFrequency.toSeconds(), TimeUnit.SECONDS)
                .defaultSchemeForDiscoveredNodes(defaultSchemeForDiscoveredNodes)
                .preemptiveAuthTargetHosts(preemptiveAuthHosts)
                .requestCompressionEnabled(compressionEnabled)
                .retryHandler(new GraylogJestRetryHandler(elasticsearchMaxRetries))
                .objectMapper(objectMapper);

        factory.setHttpClientConfig(httpClientConfigBuilder.build());
    }

    @Override
    public JestClient get() {
        return this.instance.updateAndGet(instance -> instance == null ? factory.getObject() : instance);
    }
}
