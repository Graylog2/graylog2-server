package org.graylog.storage.elasticsearch7;

import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerIT;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.junit.Rule;

public class IndexFieldTypePollerES7IT extends IndexFieldTypePollerIT {
    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    @Override
    protected IndicesAdapter createIndicesAdapter() {
        return null;
    }

    @Override
    protected IndexFieldTypePollerAdapter createIndexFieldTypePollerAdapter() {
        return new IndexFieldTypePollerAdapterES7(elasticsearch.elasticsearchClient());
    }

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }
}
