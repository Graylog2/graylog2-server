/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.messages;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Maps;
import io.searchbox.client.JestResult;
import io.searchbox.core.Count;
import io.searchbox.core.CountResult;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import joptsimple.internal.Strings;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.TestIndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.Message;
import org.graylog2.system.processing.InMemoryProcessingStatusRecorder;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class MessagesIT extends ElasticsearchBaseTest {
    private Messages messages;

    @Before
    public void setUp() throws Exception {
        messages = new Messages(new MetricRegistry(), jestClient(), new InMemoryProcessingStatusRecorder());
    }

    private static final IndexSetConfig indexSetConfig = IndexSetConfig.builder()
            .id("index-set-1")
            .title("Index set 1")
            .description("For testing")
            .indexPrefix("graylog")
            .creationDate(ZonedDateTime.now())
            .shards(1)
            .replicas(0)
            .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
            .rotationStrategy(MessageCountRotationStrategyConfig.createDefault())
            .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
            .retentionStrategy(DeletionRetentionStrategyConfig.createDefault())
            .indexAnalyzer("standard")
            .indexTemplateName("template-1")
            .indexOptimizationMaxNumSegments(1)
            .indexOptimizationDisabled(false)
            .build();
    private static final IndexSet indexSet = new TestIndexSet(indexSetConfig);

    @Test
    public void getResultDoesNotContainJestMetadataFields() throws Exception {
        final String index = UUID.randomUUID().toString();
        final Map<String, Object> source = new HashMap<>();
        source.put("message", "message");
        source.put("source", "source");
        source.put("timestamp", "2017-04-13 15:29:00.000");
        final Index indexRequest = messages.prepareIndexRequest(index, source, "1");
        final DocumentResult indexResponse = jestClient().execute(indexRequest);

        assertThat(indexResponse.isSucceeded()).isTrue();

        final ResultMessage resultMessage = messages.get("1", index);
        final Message message = resultMessage.getMessage();
        assertThat(message).isNotNull();
        assertThat(message.hasField(JestResult.ES_METADATA_ID)).isFalse();
        assertThat(message.hasField(JestResult.ES_METADATA_VERSION)).isFalse();
    }

    @Test
    public void testIfTooLargeBatchesGetSplitUp() throws Exception {
        // This test assumes that ES is configured with bulk_max_body_size to 100MB
        // Check if we can index about 300MB of messages (once the large batch gets split up)
        final int MESSAGECOUNT = 303;
        final ArrayList<Map.Entry<IndexSet, Message>> largeMessageBatch = createMessageBatch(MESSAGECOUNT);
        final List<String> failedItems = this.messages.bulkIndex(largeMessageBatch);

        assertThat(failedItems).isEmpty();

        Thread.sleep(2000); // wait for ES to finish indexing
        final Count count = new Count.Builder().build();
        final CountResult result = jestClient().execute(count);

        assertThat(result.getCount()).isEqualTo(MESSAGECOUNT);
    }

    private ArrayList<Map.Entry<IndexSet, Message>> createMessageBatch(int size) {
        final ArrayList<Map.Entry<IndexSet, Message>> messageList = new ArrayList<>();

        // Each Message is about 1 MB
        final String message = Strings.repeat('A', 1024 * 1024);
        for (int i = 0; i < size; i++) {
            messageList.add(Maps.immutableEntry(indexSet, new Message(i + message, "source", DateTime.now())));
        }
        return messageList;
    }
}
