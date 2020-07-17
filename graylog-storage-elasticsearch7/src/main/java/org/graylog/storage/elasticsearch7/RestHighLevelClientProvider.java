package org.graylog.storage.elasticsearch7;

import com.github.joschi.jadconfig.util.Duration;
import org.graylog.shaded.elasticsearch7.org.apache.http.HttpHost;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestClient;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestClientBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;
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

@Singleton
public class RestHighLevelClientProvider implements Provider<RestHighLevelClient> {

    private static final Logger LOG = LoggerFactory.getLogger(RestHighLevelClientProvider.class);

    private final RestHighLevelClient client;

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
            @Named("elasticsearch_compression_enabled") boolean compressionEnabled) {
        client = buildClient(
                hosts,
                connectTimeout,
                socketTimeout,
                maxTotalConnections,
                maxTotalConnectionsPerRoute);

        registerShutdownHook(shutdownService);
    }

    @Override
    public RestHighLevelClient get() {
        return client;
    }

    private RestHighLevelClient buildClient(
            List<URI> hosts,
            Duration connectTimeout,
            Duration socketTimeout,
            int maxTotalConnections,
            int maxTotalConnectionsPerRoute) {
        final HttpHost[] esHosts = hosts.stream().map(uri -> new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme())).toArray(HttpHost[]::new);

        final RestClientBuilder restClientBuilder = RestClient.builder(esHosts)
                .setRequestConfigCallback(requestConfig -> requestConfig
                        .setConnectTimeout(Math.toIntExact(connectTimeout.toMilliseconds()))
                        .setSocketTimeout(Math.toIntExact(socketTimeout.toMilliseconds()))
                )
                .setHttpClientConfigCallback(httpClientConfig -> httpClientConfig
                        .setMaxConnTotal(maxTotalConnections)
                        .setMaxConnPerRoute(maxTotalConnectionsPerRoute)
                );

        return new RestHighLevelClient(restClientBuilder);
    }

    private void registerShutdownHook(GracefulShutdownService shutdownService) {
        shutdownService.register(() -> {
            try {
                client.close();
            } catch (IOException e) {
                LOG.warn("Failed to close Elasticsearch RestHighLevelClient", e);
            }
        });
    }
}
