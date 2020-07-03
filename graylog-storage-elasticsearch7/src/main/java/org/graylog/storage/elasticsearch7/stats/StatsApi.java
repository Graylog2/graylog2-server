package org.graylog.storage.elasticsearch7.stats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Response;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsApi {
    private final ObjectMapper objectMapper;

    public enum State {
        Open,
        Closed;
    }

    @Inject
    public StatsApi(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Long> storeSizes(RestHighLevelClient client) throws IOException {
        final JsonNode stats = stats(client);
        return Streams.stream(stats.path("metadata").path("indices").fields())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()
                        .path("primaries")
                        .path("store")
                        .path("size_in_bytes")
                        .asLong(0)));
    }

    public State indexState(RestHighLevelClient client, String index) throws IOException {
        final JsonNode stats = stats(client);
        final String stateString = stats.path("metadata").path("indices").path(index).path("state").asText();
        return parseState(stateString);
    }

    private State parseState(String stateString) {
        switch (stateString.toLowerCase(Locale.ENGLISH)) {
            case "open": return State.Open;
            case "close": return State.Closed;
        }

        throw new IllegalStateException("Unable to parse index state: " + stateString);
    }

    private JsonNode stats(RestHighLevelClient client) throws IOException {
        final Request request = new Request("GET", "/_cluster/state");
        final Response response = client.getLowLevelClient().performRequest(request);

        return objectMapper.readTree(response.getEntity().getContent());
    }
}
