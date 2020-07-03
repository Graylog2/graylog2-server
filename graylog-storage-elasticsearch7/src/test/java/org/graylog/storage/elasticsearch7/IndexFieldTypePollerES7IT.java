package org.graylog.storage.elasticsearch7;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.storage.elasticsearch7.mapping.FieldMappingApi;
import org.graylog.storage.elasticsearch7.state.StateApi;
import org.graylog.storage.elasticsearch7.stats.StatsApi;
import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerIT;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;

public class IndexFieldTypePollerES7IT extends IndexFieldTypePollerIT {
    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Override
    protected IndicesAdapter createIndicesAdapter() {
        return new IndicesAdapterES7(
                elasticsearch.elasticsearchClient(),
                new StateApi(objectMapper),
                new StatsApi(objectMapper)
        );
    }

    @Override
    protected IndexFieldTypePollerAdapter createIndexFieldTypePollerAdapter() {
        return new IndexFieldTypePollerAdapterES7(elasticsearch.elasticsearchClient(), new FieldMappingApi(objectMapper));
    }

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }
}
