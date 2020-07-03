package org.graylog.storage.elasticsearch7.cluster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RequestOptions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Response;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;

import javax.inject.Inject;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

public class ClusterStateApi {
    private final ObjectMapper objectMapper;

    @Inject
    public ClusterStateApi(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Set<String>> fields(RestHighLevelClient client, RequestOptions requestOptions, Collection<String> indices) throws IOException {
        final Request request = request(Collections.singleton("metadata"), indices);

        final Response response = client.getLowLevelClient().performRequest(request);

        final JsonNode jsonResponse = objectMapper.readTree(response.getEntity().getContent());

        return Streams.stream(jsonResponse.path("metadata").path("indices").fields())
                .flatMap(index -> allFieldsFromIndex(index.getKey(), index.getValue()))
                .collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, Collectors.toSet())));
    }

    private Stream<Map.Entry<String, String>> allFieldsFromIndex(String indexName, JsonNode indexMapping) {
        return Streams.stream(indexMapping.path("mappings").fields())
                .flatMap(documentType -> allFieldsFromDocumentType(indexName, documentType.getValue()));
    }

    private Stream<? extends Map.Entry<String, String>> allFieldsFromDocumentType(String indexName, JsonNode documentType) {
        return Streams.stream(documentType.path("properties").fields())
                .map(field -> new AbstractMap.SimpleEntry<>(indexName, field.getKey()));
    }

    private Request request(Collection<String> metrics, Collection<String> indices) {
        checkArgument(!metrics.isEmpty(), "At least one metric must be provided.");
        final String joinedMetrics = String.join(",", metrics);
        final StringBuilder apiEndpoint = new StringBuilder("/_cluster/state/").append(joinedMetrics);
        if (!indices.isEmpty()) {
            final String joinedIndices = String.join(",", indices);
            apiEndpoint.append("/");
            apiEndpoint.append(joinedIndices);
        }

        return new Request("GET", apiEndpoint.toString());
    }

}
