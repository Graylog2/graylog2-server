package org.graylog.storage.elasticsearch7;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Response;

import javax.inject.Inject;

public class PlainJsonApi {
    private final ObjectMapper objectMapper;
    private final ElasticsearchClient client;

    @Inject
    public PlainJsonApi(ObjectMapper objectMapper,
                        ElasticsearchClient client) {
        this.objectMapper = objectMapper;
        this.client = client;
    }

    public JsonNode perform(Request request, String errorMessage) {
        return client.execute((c, requestOptions) -> {
            request.setOptions(requestOptions);
            final Response response = c.getLowLevelClient().performRequest(request);
            return objectMapper.readTree(response.getEntity().getContent());
        }, errorMessage);
    }
}
