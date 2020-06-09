package org.graylog.storage.elasticsearch6;

import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.indices.IndicesGetAllMessageFieldsIT;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

public class IndicesGetAllMessageFieldsES6IT extends IndicesGetAllMessageFieldsIT {
    @Override
    protected IndicesAdapter indicesAdapter() {
        return new IndicesAdapterES6(jestClient(),
                new ObjectMapperProvider().get(),
                new IndexingHelper());
    }
}
