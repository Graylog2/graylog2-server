package org.graylog.storage.elasticsearch6;

import org.graylog.storage.elasticsearch6.testing.ElasticsearchInstanceES6;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.cluster.NodeIT;
import org.junit.Rule;

import static org.graylog.storage.elasticsearch6.testing.TestUtils.jestClient;

public class NodeES6IT extends NodeIT {
    @Rule
    public final ElasticsearchInstance elasticsearch = ElasticsearchInstanceES6.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected NodeAdapter nodeAdapter() {
        return new NodeAdapterES6(jestClient(elasticsearch));
    }
}
