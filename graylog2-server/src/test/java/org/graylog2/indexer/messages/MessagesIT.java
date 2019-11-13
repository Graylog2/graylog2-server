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
import io.searchbox.client.JestResult;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.graylog2.system.processing.InMemoryProcessingStatusRecorder;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class MessagesIT extends ElasticsearchBaseTest {
    private Messages messages;

    @Before
    public void setUp() throws Exception {
        messages = new Messages(new MetricRegistry(), jestClient(), new InMemoryProcessingStatusRecorder());
    }

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
}
