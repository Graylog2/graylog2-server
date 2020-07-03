package org.graylog.storage.elasticsearch7.stats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Response;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

public class StatsApi {
    private final ObjectMapper objectMapper;

    @Inject
    public StatsApi(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode indexStats(RestHighLevelClient client, String index) throws IOException {
        return stats(client, index);
    }

    public JsonNode indexStatsWithShardLevel(RestHighLevelClient client, String index) throws IOException {
        return indexStatsWithShardLevel(client, Collections.singleton(index)).path(index);
    }

    public JsonNode indexStatsWithShardLevel(RestHighLevelClient client, Collection<String> indices) throws IOException {
        final JsonNode stats = stats(client, indices, request -> {
            request.addParameter("level", "shards");
            request.addParameter("ignore_unavailable", "true");
        });

        return stats.path("indices");
    }

    public JsonNode indexStatsWithDocsAndStore(RestHighLevelClient client, Collection<String> indices) throws IOException {
        final JsonNode stats = stats(client, indices, ImmutableSet.of("store", "docs"));

        return stats.path("indices");
    }

    public Optional<Long> storeSizes(RestHighLevelClient client, String index) throws IOException {
        final JsonNode stats = stats(client, Collections.singleton(index), Collections.singleton("store"));
        final JsonNode sizeInBytes = stats.path("indices")
                .path(index)
                .path("primaries")
                .path("store")
                .path("size_in_bytes");
        return Optional.of(sizeInBytes).filter(JsonNode::isNumber).map(JsonNode::asLong);
    }

    private JsonNode stats(RestHighLevelClient client, String index) throws IOException {
        return stats(client, Collections.singleton(index), Collections.emptySet(), (request) -> {});
    }

    private JsonNode stats(RestHighLevelClient client,
                           Collection<String> indices,
                           Collection<String> metrics) throws IOException {
        return stats(client, indices, metrics, (request) -> {});
    }

    private JsonNode stats(RestHighLevelClient client,
                           Collection<String> indices,
                           Consumer<Request> fn) throws IOException {
        return stats(client, indices, Collections.emptySet(), fn);
            }

    private JsonNode stats(RestHighLevelClient client,
                           Collection<String> indices,
                           Collection<String> metrics,
                           Consumer<Request> fn) throws IOException {
        final StringBuilder endpoint = new StringBuilder();
        if (!indices.isEmpty()) {
            final String joinedIndices = String.join(",", indices);
            endpoint.append("/");
            endpoint.append(joinedIndices);
        }
        endpoint.append("/_stats");
        if (!metrics.isEmpty()) {
            final String joinedMetrics = String.join(",", metrics);
            endpoint.append("/");
            endpoint.append(joinedMetrics);
        }

        final Request request = new Request("GET", endpoint.toString());
        fn.accept(request);
        final Response response = client.getLowLevelClient().performRequest(request);

        return objectMapper.readTree(response.getEntity().getContent());
    }
}
