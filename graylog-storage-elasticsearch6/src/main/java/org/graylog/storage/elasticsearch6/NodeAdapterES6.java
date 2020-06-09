package org.graylog.storage.elasticsearch6;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Ping;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.cluster.jest.JestUtils;

import javax.inject.Inject;
import java.util.Optional;

public class NodeAdapterES6 implements NodeAdapter {
    private final JestClient jestClient;

    @Inject
    public NodeAdapterES6(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    @Override
    public Optional<String> version() {
        final Ping request = new Ping.Builder().build();
        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Unable to retrieve Elasticsearch version");
        return Optional.ofNullable(jestResult.getJsonObject().path("version").path("number").asText(null));
    }
}
