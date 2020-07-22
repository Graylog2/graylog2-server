package org.graylog.storage.elasticsearch7;

import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.cluster.NodeIT;
import org.junit.Rule;

public class NodeES7IT extends NodeIT {
    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected NodeAdapter nodeAdapter() {
        return new NodeAdapterES7(elasticsearch.elasticsearchClient());
    }
}
