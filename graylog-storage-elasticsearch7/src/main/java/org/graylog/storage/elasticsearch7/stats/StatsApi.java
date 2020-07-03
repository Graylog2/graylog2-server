package org.graylog.storage.elasticsearch7.stats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Response;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
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
        final JsonNode stats = stats(client, request -> {
            request.addParameter("level", "shards");
            request.addParameter("ignore_unavailable", "true");
        });

        return stats.path("indices").path(index);
    }

    public JsonNode indexStatsWithDocsAndStore(RestHighLevelClient client, Collection<String> indices) throws IOException {
        final JsonNode stats = stats(client, indices, request -> {
            request.addParameter("store", "true");
            request.addParameter("docs", "true");
        });

        return stats.path("indices");
    }

    private JsonNode stats(RestHighLevelClient client, String index) throws IOException {
        return stats(client, Collections.singleton(index), (request) -> {});
    }

    private JsonNode stats(RestHighLevelClient client, Consumer<Request> fn) throws IOException {
        return stats(client, Collections.emptySet(), fn);
    }

    private JsonNode stats(RestHighLevelClient client, Collection<String> indices, Consumer<Request> fn) throws IOException {
        final String joinedIndices = String.join(",", indices);

        final Request request = new Request("GET", "/_stats/" + joinedIndices);
        fn.accept(request);
        final Response response = client.getLowLevelClient().performRequest(request);

        return objectMapper.readTree(response.getEntity().getContent());
    }
}
