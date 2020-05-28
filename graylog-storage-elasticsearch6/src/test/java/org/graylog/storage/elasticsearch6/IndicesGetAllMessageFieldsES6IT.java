package org.graylog.storage.elasticsearch6;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.indices.IndicesGetAllMessageFieldsIT;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.system.processing.InMemoryProcessingStatusRecorder;

public class IndicesGetAllMessageFieldsES6IT extends IndicesGetAllMessageFieldsIT {
    @Override
    protected IndicesAdapter indicesAdapter() {
        return new IndicesAdapterES6(jestClient(),
                new ObjectMapperProvider().get(),
                new Messages(new MetricRegistry(), jestClient(), new InMemoryProcessingStatusRecorder(), true));
    }
}
