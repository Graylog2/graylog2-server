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
package org.graylog2.outputs;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.jayway.awaitility.Duration;
import org.graylog2.Configuration;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.journal.NoopJournal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BatchedElasticSearchOutputTest {

    private MetricRegistry metricRegistry;
    @Mock
    private Messages messages;
    @Mock
    private Cluster cluster;

    @Before
    public void setUp() {
        metricRegistry = new MetricRegistry();
    }

    @Test
    public void flushingBatchWritesBulk() throws Exception {
        final Configuration config = new Configuration() {
            @Override
            public int getOutputBatchSize() {
                return 10;
            }
        };
        when(cluster.isConnectedAndHealthy()).thenReturn(true);

        Messages messages = mock(Messages.class);
        final List<Message> messageList = buildMessages(3);
        MetricRegistry metricRegistry = new MetricRegistry();

        final BatchedElasticSearchOutput output = new BatchedElasticSearchOutput(metricRegistry, messages,
                cluster, config, new NoopJournal());

        for (Message message : messageList) {
            output.write(message);
        }

        output.flush(false);

        verify(messages, times(1)).bulkIndex(eq(messageList));
    }

    @Test
    public void dontFlushWritesIfElasticsearchIsUnhealthy() throws Exception {
        final Configuration config = new Configuration() {
            @Override
            public int getOutputBatchSize() {
                return 10;
            }
        };
        when(cluster.isConnectedAndHealthy()).thenReturn(false);

        final List<Message> messageList = buildMessages(3);
        BatchedElasticSearchOutput output = new BatchedElasticSearchOutput(metricRegistry, messages, cluster, config, new NoopJournal());

        for (Message message : messageList) {
            output.write(message);
        }

        output.flush(false);

        verify(messages, never()).bulkIndex(eq(messageList));
    }

    @Test
    public void flushIfBatchSizeIsExceeded() throws Exception {
        final int batchSize = 5;
        final Configuration config = new Configuration() {
            @Override
            public int getOutputBatchSize() {
                return batchSize;
            }
        };
        when(cluster.isConnectedAndHealthy()).thenReturn(true);

        final List<Message> messageList = buildMessages(batchSize + 1);
        final BatchedElasticSearchOutput output = new BatchedElasticSearchOutput(metricRegistry, messages, cluster, config, new NoopJournal());

        for (Message message : messageList) {
            output.write(message);
        }

        await().atMost(Duration.FIVE_SECONDS).until(new Runnable() {
            @Override
            public void run() {
                verify(messages, times(1)).bulkIndex(eq(messageList.subList(0, batchSize)));
            }
        });
    }

    @Test
    public void dontFlushEmptyBuffer() {
        final Configuration config = new Configuration() {
            @Override
            public int getOutputBatchSize() {
                return 10;
            }
        };
        when(cluster.isConnectedAndHealthy()).thenReturn(true);

        final BatchedElasticSearchOutput output = new BatchedElasticSearchOutput(metricRegistry, messages, cluster, config, new NoopJournal());

        output.flush(false);

        verify(messages, never()).bulkIndex(anyListOf(Message.class));
    }

    private List<Message> buildMessages(final int count) {
        final ImmutableList.Builder<Message> builder = ImmutableList.builder();
        for (int i = 0; i < count; i++) {
            builder.add(new Message("message" + i, "test", Tools.iso8601()));
        }

        return builder.build();
    }
}
