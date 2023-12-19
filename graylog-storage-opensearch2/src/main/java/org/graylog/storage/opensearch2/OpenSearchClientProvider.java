package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientOptions;
import org.opensearch.client.transport.rest_client.RestClientTransport;

import javax.inject.Inject;
import javax.inject.Provider;

public class OpenSearchClientProvider implements Provider<OpenSearchClient> {
    private final RestClientTransport transport;

    @Inject
    public OpenSearchClientProvider(RestClient restClient, ObjectMapper objectMapper) {
        this.transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
    }

    @Override
    public OpenSearchClient get() {
        return new OpenSearchClient(transport);
    }

    public OpenSearchClient getWithRequestOptions(RequestOptions requestOptions) {
        return new OpenSearchClient(transport.withRequestOptions(new RestClientOptions(requestOptions)));
    }
}
