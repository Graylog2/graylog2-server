package org.graylog.storage.elasticsearch6;

import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.counts.CountsIT;
import org.junit.Rule;

public class CountsES6IT extends CountsIT {
    @Rule
    public final ElasticsearchInstance elasticsearch = ElasticsearchInstance.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected CountsAdapter countsAdapter() {
        return new CountsAdapterES6(jestClient());
    }
}
