/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.outputs;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog2.Configuration;
import org.graylog2.indexer.Indexer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.fail;

public class BatchedElasticSearchOutputTest {

    private MetricRegistry metricRegistry;
    private Indexer indexer;

    @BeforeMethod
    public void setUp() {
        metricRegistry = new MetricRegistry();
        indexer = mock(Indexer.class);
    }

    @Test
    public void flushingBatchWritesBulk() {
        final Configuration config = new Configuration() {
            @Override
            public int getOutputBatchSize() {
                return 10;
            }
        };
        when(indexer.isConnectedAndHealthy()).thenReturn(true);

        final List<Message> messages = buildMessages(3);
        final BatchedElasticSearchOutput output = new BatchedElasticSearchOutput(metricRegistry, indexer, config);

        try {
            for (Message message : messages) {
                output.write(message);
            }
        } catch (Exception e) {
            fail("Output should not throw", e);
        }

        output.flush(false);

        verify(indexer, times(1)).bulkIndex(eq(messages));
    }

    @Test
    public void dontFlushWritesIfElasticsearchIsUnhealthy() {
        final Configuration config = new Configuration() {
            @Override
            public int getOutputBatchSize() {
                return 10;
            }
        };
        when(indexer.isConnectedAndHealthy()).thenReturn(false);

        final List<Message> messages = buildMessages(3);
        final BatchedElasticSearchOutput output = new BatchedElasticSearchOutput(metricRegistry, indexer, config);

        try {
            for (Message message : messages) {
                output.write(message);
            }
        } catch (Exception e) {
            fail("Output should not throw", e);
        }

        output.flush(false);

        verify(indexer, never()).bulkIndex(eq(messages));
    }

    @Test
    public void flushIfBatchSizeIsExceeded() {
        final int batchSize = 5;
        final Configuration config = new Configuration() {
            @Override
            public int getOutputBatchSize() {
                return batchSize;
            }
        };
        when(indexer.isConnectedAndHealthy()).thenReturn(true);

        final List<Message> messages = buildMessages(batchSize + 1);
        final BatchedElasticSearchOutput output = new BatchedElasticSearchOutput(metricRegistry, indexer, config);

        try {
            for (Message message : messages) {
                output.write(message);
            }
        } catch (Exception e) {
            fail("Output should not throw", e);
        }

        // Give the asynchronous flush a chance to finish
        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);

        verify(indexer, times(1)).bulkIndex(eq(messages.subList(0, batchSize)));
    }

    @Test
    public void dontFlushEmptyBuffer() {
        final Configuration config = new Configuration() {
            @Override
            public int getOutputBatchSize() {
                return 10;
            }
        };
        when(indexer.isConnectedAndHealthy()).thenReturn(true);

        final BatchedElasticSearchOutput output = new BatchedElasticSearchOutput(metricRegistry, indexer, config);

        output.flush(false);

        verify(indexer, never()).bulkIndex(anyListOf(Message.class));
    }

    private List<Message> buildMessages(final int count) {
        final ImmutableList.Builder<Message> builder = ImmutableList.builder();
        for (int i = 0; i < count; i++) {
            builder.add(new Message("message" + i, "test", Tools.iso8601()));
        }

        return builder.build();
    }
}
