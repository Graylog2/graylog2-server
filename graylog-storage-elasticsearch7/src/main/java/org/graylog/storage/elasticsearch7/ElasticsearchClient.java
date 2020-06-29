package org.graylog.storage.elasticsearch7;

import com.github.joschi.jadconfig.util.Duration;
import org.graylog.shaded.elasticsearch7.org.apache.http.HttpHost;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.ElasticsearchException;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RequestOptions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestClient;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public class ElasticsearchClient {
    private final RestHighLevelClient client;

    @Inject
    public ElasticsearchClient(@Named("elasticsearch_hosts") List<URI> elasticsearchHosts,
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
                               @Named("elasticsearch_compression_enabled") boolean compressionEnabled) {
        this.client = new RestHighLevelClient(RestClient.builder(
                elasticsearchHosts.stream().map(uri -> new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme())).toArray(HttpHost[]::new)
        ).setRequestConfigCallback(requestConfig -> requestConfig
                .setConnectTimeout(Math.toIntExact(elasticsearchConnectTimeout.toMilliseconds()))
                .setSocketTimeout(Math.toIntExact(elasticsearchSocketTimeout.toMilliseconds()))
        ).setHttpClientConfigCallback(httpClientConfig -> httpClientConfig
                .setMaxConnTotal(elasticsearchMaxTotalConnections)
                .setMaxConnPerRoute(elasticsearchMaxTotalConnectionsPerRoute)
        ));
    }

    public <R> R execute(ThrowingBiFunction<RestHighLevelClient, RequestOptions, R, IOException> fn) {
        try {
            return fn.apply(client, requestOptions());
        } catch (Exception e) {
            throw exceptionFrom(e);
        }
    }

    private RequestOptions requestOptions() {
        return RequestOptions.DEFAULT;
    }

    private ElasticsearchException exceptionFrom(Exception e) {
        return new ElasticsearchException(e);
    }
}
