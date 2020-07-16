package org.graylog.storage.elasticsearch7.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Response;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;

import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldMappingApi {
    private final ObjectMapper objectMapper;
    private final ElasticsearchClient client;

    @Inject
    public FieldMappingApi(ObjectMapper objectMapper,
                           ElasticsearchClient client) {
        this.objectMapper = objectMapper;
        this.client = client;
    }

    public Map<String, String> fieldTypes(String index) {
        final JsonNode result = client.execute((c, requestOptions) -> {
            final Response response = c.getLowLevelClient().performRequest(request(index));
            return objectMapper.readTree(response.getEntity().getContent());
        }, "Unable to retrieve field types of index " + index);
        final JsonNode fields = result.path(index).path("mappings").path("properties");
        //noinspection UnstableApiUsage
        return Streams.stream(fields.fields())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().path("type").asText()));
    }

    private Request request(String index) {
        return new Request("GET", "/" + index + "/_mapping");
    }
}
