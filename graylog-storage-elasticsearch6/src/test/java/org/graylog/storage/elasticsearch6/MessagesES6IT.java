package org.graylog.storage.elasticsearch6;

import com.codahale.metrics.MetricRegistry;
import io.searchbox.client.JestResult;
import io.searchbox.core.Count;
import io.searchbox.core.CountResult;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.messages.MessagesIT;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class MessagesES6IT extends MessagesIT {
    private final IndexingHelper indexingHelper = new IndexingHelper();

    @Override
    protected MessagesAdapter createMessagesAdapter(MetricRegistry metricRegistry) {
        return new MessagesAdapterES6(jestClient(), true, metricRegistry);
    }

    @Override
    protected Double messageCount(String indexName) {
        final Count count = new Count.Builder().addIndex(indexName).build();

        final CountResult result = JestUtils.execute(jestClient(), count, () -> "Unable to count documents");
        return result.getCount();
    }

    @Test
    public void getResultDoesNotContainJestMetadataFields() throws Exception {
        final String index = UUID.randomUUID().toString();
        final Map<String, Object> source = new HashMap<>();
        source.put("message", "message");
        source.put("source", "source");
        source.put("timestamp", "2017-04-13 15:29:00.000");

        assertThat(indexMessage(index, source, "1")).isTrue();

        final ResultMessage resultMessage = messages.get("1", index);
        final Message message = resultMessage.getMessage();
        assertThat(message).isNotNull();
        assertThat(message.hasField(JestResult.ES_METADATA_ID)).isFalse();
        assertThat(message.hasField(JestResult.ES_METADATA_VERSION)).isFalse();
    }

    private boolean indexMessage(String index, Map<String, Object> source, @SuppressWarnings("SameParameterValue") String id) {
        final Index indexRequest = indexingHelper.prepareIndexRequest(index, source, id);
        final DocumentResult indexResponse = JestUtils.execute(jestClient(), indexRequest, () -> "Unable to index message");

        return indexResponse.isSucceeded();
    }
}
