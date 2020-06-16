package org.graylog.storage.elasticsearch6;

import org.graylog.storage.elasticsearch6.testing.ElasticsearchInstanceES6;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.indices.IndicesGetAllMessageFieldsIT;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;

import static org.graylog.storage.elasticsearch6.testing.TestUtils.jestClient;

public class IndicesGetAllMessageFieldsES6IT extends IndicesGetAllMessageFieldsIT {
    @Rule
    public final ElasticsearchInstance elasticsearch = ElasticsearchInstanceES6.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected IndicesAdapter indicesAdapter() {
        return new IndicesAdapterES6(jestClient(elasticsearch),
                new ObjectMapperProvider().get(),
                new IndexingHelper());
    }
}
