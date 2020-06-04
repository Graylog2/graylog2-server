package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerIT;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

public class IndexFieldTypePollerES6IT extends IndexFieldTypePollerIT {
    @Override
    protected IndicesAdapter createIndicesAdapter() {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        return new IndicesAdapterES6(jestClient(), objectMapper, new IndexingHelper());
    }

    @Override
    protected IndexFieldTypePollerAdapter createIndexFieldTypePollerAdapter() {
        return new IndexFieldTypePollerAdapterES6(jestClient());
    }
}
