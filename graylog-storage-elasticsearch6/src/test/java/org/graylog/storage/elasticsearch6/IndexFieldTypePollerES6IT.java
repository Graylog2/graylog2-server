package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerIT;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import static org.mockito.Mockito.mock;

public class IndexFieldTypePollerES6IT extends IndexFieldTypePollerIT {
    @Override
    protected IndicesAdapter createIndicesAdapter() {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        return new IndicesAdapterES6(jestClient(), objectMapper, mock(Messages.class));
    }
}
