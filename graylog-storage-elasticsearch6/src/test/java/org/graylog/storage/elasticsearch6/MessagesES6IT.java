package org.graylog.storage.elasticsearch6;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.messages.MessagesIT;

public class MessagesES6IT extends MessagesIT {
    @Override
    protected MessagesAdapter createMessagesAdapter(MetricRegistry metricRegistry) {
        return new MessagesAdapterES6(jestClient(), true, metricRegistry);
    }
}
