package org.graylog.storage.elasticsearch7;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RequestOptions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Response;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;

import javax.inject.Inject;
import java.io.IOException;

public class PlainJsonApi {
    private final ObjectMapper objectMapper;

    @Inject
    public PlainJsonApi(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode perform(RestHighLevelClient c, Request request, RequestOptions requestOptions) throws IOException {
        request.setOptions(requestOptions);

        final Response response = c.getLowLevelClient().performRequest(request);

        return objectMapper.readTree(response.getEntity().getContent());
    }
}
