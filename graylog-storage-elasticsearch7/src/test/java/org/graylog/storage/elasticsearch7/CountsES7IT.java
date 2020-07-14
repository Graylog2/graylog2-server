package org.graylog.storage.elasticsearch7;

import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.counts.CountsIT;
import org.junit.Rule;

public class CountsES7IT extends CountsIT {
    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected CountsAdapter countsAdapter() {
        return new CountsAdapterES7(elasticsearch.elasticsearchClient());
    }
}
