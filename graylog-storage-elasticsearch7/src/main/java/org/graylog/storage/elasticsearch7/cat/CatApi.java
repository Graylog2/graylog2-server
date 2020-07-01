package org.graylog.storage.elasticsearch7.cat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RequestOptions;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Response;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class CatApi {
    private final ObjectMapper objectMapper;

    @Inject
    public CatApi(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<NodeResponse> nodes(RestHighLevelClient c, RequestOptions requestOptions) throws IOException {
        final Request request = request("GET", "nodes", requestOptions);
        request.addParameter("h", "id,name,host,ip,fileDescriptorMax,diskUsed,diskTotal,diskUsedPercent");
        request.addParameter("full_id", "true");
        return perform(c, request, new TypeReference<List<NodeResponse>>() {});
    }

    private <R> R perform(RestHighLevelClient c, Request request, TypeReference<R> responseClass) throws IOException {
        final Response response = c.getLowLevelClient().performRequest(request);

        return returnType(response, responseClass);
    }

    private <R> R returnType(Response response, TypeReference<R> responseClass) throws IOException {
        return objectMapper.readValue(response.getEntity().getContent(), responseClass);
    }

    private Request request(String method, String endpoint, RequestOptions requestOptions) {
        final Request request = new Request(method, "/_cat/" + endpoint);
        request.addParameter("format", "json");
        request.setOptions(requestOptions);

        return request;
    }
}
