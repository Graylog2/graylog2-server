package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.core.MainResponse;
import org.graylog2.indexer.cluster.NodeAdapter;

import javax.inject.Inject;
import java.util.Optional;

public class NodeAdapterES7 implements NodeAdapter {
    private final ElasticsearchClient client;

    @Inject
    public NodeAdapterES7(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public Optional<String> version() {
        final MainResponse result = client.execute(RestHighLevelClient::info,
                "Unable to retrieve Elasticsearch version from node");
        return Optional.of(result.getVersion().getNumber());
    }
}
