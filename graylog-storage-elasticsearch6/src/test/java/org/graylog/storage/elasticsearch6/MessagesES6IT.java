package org.graylog.storage.elasticsearch6;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.messages.MessagesIT;
import org.graylog2.system.processing.InMemoryProcessingStatusRecorder;

public class MessagesES6IT extends MessagesIT {
    @Override
    protected MessagesAdapter createMessages(MetricRegistry metricRegistry) {
        return new MessagesAdapterES6(jestClient(), true, metricRegistry, new InMemoryProcessingStatusRecorder());
    }
}
