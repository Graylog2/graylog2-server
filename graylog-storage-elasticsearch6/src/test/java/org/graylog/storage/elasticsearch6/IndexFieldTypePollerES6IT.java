package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.storage.elasticsearch6.testing.ElasticsearchInstanceES6;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerIT;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;

import static org.graylog.storage.elasticsearch6.testing.TestUtils.jestClient;

public class IndexFieldTypePollerES6IT extends IndexFieldTypePollerIT {
    @Rule
    public final ElasticsearchInstance elasticsearch = ElasticsearchInstanceES6.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected IndicesAdapter createIndicesAdapter() {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        return new IndicesAdapterES6(jestClient(elasticsearch), objectMapper, new IndexingHelper());
    }

    @Override
    protected IndexFieldTypePollerAdapter createIndexFieldTypePollerAdapter() {
        return new IndexFieldTypePollerAdapterES6(jestClient(elasticsearch));
    }
}
