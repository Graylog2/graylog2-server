package org.graylog.storage.elasticsearch7.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Response;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldMappingApi {
    private final ObjectMapper objectMapper;

    @Inject
    public FieldMappingApi(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, String> fieldTypes(RestHighLevelClient client, String index) throws IOException {
        final Response result = client.getLowLevelClient().performRequest(request(index));
        final JsonNode response = objectMapper.readTree(result.getEntity().getContent());
        final JsonNode fields = response.path(index).path("mappings").path("properties");
        return Streams.stream(fields.fields())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().path("type").asText()));
    }

    private Request request(String index) {
        return new Request("GET", "/" + index + "/_mapping");
    }
}
