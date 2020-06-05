package org.graylog.storage.elasticsearch6;

import com.codahale.metrics.MetricRegistry;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.messages.MessagesIT;

import java.util.Map;

public class MessagesES6IT extends MessagesIT {
    private final IndexingHelper indexingHelper = new IndexingHelper();

    @Override
    protected MessagesAdapter createMessagesAdapter(MetricRegistry metricRegistry) {
        return new MessagesAdapterES6(jestClient(), true, metricRegistry);
    }

    @Override
    protected boolean indexMessage(String index, Map<String, Object> source, String id) {
        final Index indexRequest = indexingHelper.prepareIndexRequest(index, source, "1");
        final DocumentResult indexResponse = JestUtils.execute(jestClient(), indexRequest, () -> "Unable to index message");

        return indexResponse.isSucceeded();
    }
}
