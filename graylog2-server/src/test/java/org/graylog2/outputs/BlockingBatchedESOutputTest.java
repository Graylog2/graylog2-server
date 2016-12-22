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
import com.google.common.collect.Maps;
import org.graylog2.Configuration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.journal.NoopJournal;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BlockingBatchedESOutputTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private MetricRegistry metricRegistry;
    private NoopJournal journal;
    private Configuration config;

    @Mock
    private Messages messages;
    @Mock
    private Cluster cluster;

    @Before
    public void setUp() throws Exception {
        this.metricRegistry = new MetricRegistry();
        this.journal = new NoopJournal();
        this.config = new Configuration() {
            @Override
            public int getOutputBatchSize() {
                return 3;
            }
        };
    }

    @Test
    public void write() throws Exception {
        when(cluster.isConnected()).thenReturn(true);
        when(cluster.isDeflectorHealthy()).thenReturn(true);

        final BlockingBatchedESOutput output = new BlockingBatchedESOutput(metricRegistry, messages, cluster, config, journal);

        final List<Map.Entry<IndexSet, Message>> messageList = buildMessages(config.getOutputBatchSize());

        for (Map.Entry<IndexSet, Message> entry : messageList) {
            output.writeMessageEntry(entry);
        }

        verify(messages, times(1)).bulkIndex(eq(messageList));
    }

    @Test
    public void writeDoesNotFlushIfClusterIsNotConnected() throws Exception {
        when(cluster.isConnected()).thenReturn(false);

        doThrow(RuntimeException.class).when(cluster).waitForConnectedAndDeflectorHealthy();

        final BlockingBatchedESOutput output = new BlockingBatchedESOutput(metricRegistry, messages, cluster, config, journal);

        final List<Map.Entry<IndexSet, Message>> messageList = buildMessages(config.getOutputBatchSize());

        for (Map.Entry<IndexSet, Message> entry : messageList) {
            try {
                output.writeMessageEntry(entry);
            } catch(RuntimeException ignore) {
            }
        }

        verify(messages, never()).bulkIndex(eq(messageList));
    }

    @Test
    public void writeDoesNotFlushIfClusterIsUnhealthy() throws Exception {
        when(cluster.isConnected()).thenReturn(true);
        when(cluster.isDeflectorHealthy()).thenReturn(false);

        doThrow(RuntimeException.class).when(cluster).waitForConnectedAndDeflectorHealthy();

        final BlockingBatchedESOutput output = new BlockingBatchedESOutput(metricRegistry, messages, cluster, config, journal);

        final List<Map.Entry<IndexSet, Message>> messageList = buildMessages(config.getOutputBatchSize());

        for (Map.Entry<IndexSet, Message> entry : messageList) {
            try {
                output.writeMessageEntry(entry);
            } catch(RuntimeException ignore) {
            }
        }

        verify(messages, never()).bulkIndex(eq(messageList));
    }

    @Test
    public void forceFlushIfTimedOut() throws Exception {
        when(cluster.isConnected()).thenReturn(true);
        when(cluster.isDeflectorHealthy()).thenReturn(true);

        final BlockingBatchedESOutput output = new BlockingBatchedESOutput(metricRegistry, messages, cluster, config, journal);

        final List<Map.Entry<IndexSet, Message>> messageList = buildMessages(config.getOutputBatchSize() - 1);

        for (Map.Entry<IndexSet, Message> entry : messageList) {
            output.writeMessageEntry(entry);
        }

        // Should flush the buffer even though the batch size is not reached yet
        output.forceFlushIfTimedout();

        verify(messages, times(1)).bulkIndex(eq(messageList));
    }

    private List<Map.Entry<IndexSet, Message>> buildMessages(final int count) {
        final ImmutableList.Builder<Map.Entry<IndexSet, Message>> builder = ImmutableList.builder();
        for (int i = 0; i < count; i++) {
            builder.add(Maps.immutableEntry(mock(IndexSet.class), new Message("message" + i, "test", Tools.nowUTC())));
        }

        return builder.build();
    }
}