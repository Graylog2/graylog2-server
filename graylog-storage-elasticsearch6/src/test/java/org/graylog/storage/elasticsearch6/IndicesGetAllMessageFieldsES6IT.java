package org.graylog.storage.elasticsearch6;

import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.indices.IndicesGetAllMessageFieldsIT;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;

public class IndicesGetAllMessageFieldsES6IT extends IndicesGetAllMessageFieldsIT {
    @Rule
    public final ElasticsearchInstance elasticsearch = ElasticsearchInstance.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected IndicesAdapter indicesAdapter() {
        return new IndicesAdapterES6(jestClient(),
                new ObjectMapperProvider().get(),
                new IndexingHelper());
    }
}
