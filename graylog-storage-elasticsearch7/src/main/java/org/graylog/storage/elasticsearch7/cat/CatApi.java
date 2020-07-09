package org.graylog.storage.elasticsearch7.cat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Response;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class CatApi {
    private final ObjectMapper objectMapper;
    private final ElasticsearchClient client;

    @Inject
    public CatApi(ObjectMapper objectMapper,
                  ElasticsearchClient client) {
        this.objectMapper = objectMapper;
        this.client = client;
    }

    public List<NodeResponse> nodes() {
        final Request request = request("GET", "nodes");
        request.addParameter("h", "id,name,host,ip,fileDescriptorMax,diskUsed,diskTotal,diskUsedPercent");
        request.addParameter("full_id", "true");
        return perform(request, new TypeReference<List<NodeResponse>>() {}, "Unable to retrieve nodes list");
    }

    private <R> R perform(Request request, TypeReference<R> responseClass, String errorMessage) {
        return client.execute((c, requestOptions) -> {
            request.setOptions(requestOptions);

            final Response response = c.getLowLevelClient().performRequest(request);
            return returnType(response, responseClass);
        }, errorMessage);
    }

    private <R> R returnType(Response response, TypeReference<R> responseClass) throws IOException {
        return objectMapper.readValue(response.getEntity().getContent(), responseClass);
    }

    private Request request(String method, String endpoint) {
        final Request request = new Request(method, "/_cat/" + endpoint);
        request.addParameter("format", "json");

        return request;
    }
}
