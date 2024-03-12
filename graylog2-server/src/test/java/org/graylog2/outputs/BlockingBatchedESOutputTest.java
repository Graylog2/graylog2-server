/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.outputs;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import org.graylog.testing.messages.MessagesExtension;
import org.graylog2.Configuration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.messages.MessageWithIndex;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.journal.NoopJournal;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MessagesExtension.class)
public class BlockingBatchedESOutputTest {

    private Configuration config;

    @Mock
    private Messages messages;

    @Mock
    private MessageQueueAcknowledger acknowledger;

    @Mock
    private Cluster cluster;

    private BlockingBatchedESOutput output;
    private MessageFactory messageFactory;

    @BeforeEach
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp(MessageFactory messageFactory) throws Exception {
        this.messageFactory = messageFactory;

        MetricRegistry metricRegistry = new MetricRegistry();
        NoopJournal journal = new NoopJournal();
        this.config = new Configuration() {
            @Override
            public int getOutputBatchSize() {
                return 3;
            }

            @Override
            public int getShutdownTimeout() {
                return "true".equals(System.getenv("CI")) ? 500 : 100; // be more graceful when running on ci infrastructure
            }
        };

        output = new BlockingBatchedESOutput(metricRegistry, messages, config, journal, acknowledger, cluster, Executors.newSingleThreadScheduledExecutor());
        output.initialize();
    }

    @AfterEach
    public void tearDown() {
        output.stop();
    }

    @Test
    public void write() throws Exception {
        final List<MessageWithIndex> messageList = sendMessages(output, config.getOutputBatchSize());

        verify(messages, times(1)).bulkIndex(eq(messageList));
    }

    @Test
    public void forceFlushIfTimedOut() throws Exception {
        final List<MessageWithIndex> messageList = sendMessages(output, config.getOutputBatchSize() - 1);

        // Should flush the buffer even though the batch size is not reached yet
        output.forceFlushIfTimedout();

        verify(messages, times(1)).bulkIndex(eq(messageList));
    }

    @Test
    public void flushWithService() throws Exception {
        final List<MessageWithIndex> messageList = sendMessages(output, config.getOutputBatchSize() - 1);

        Thread.sleep(config.getOutputFlushInterval() * 1000L + 100); // let the flushservice run

        verify(messages, times(1)).bulkIndex(eq(messageList));
    }

    @Test
    public void stop_withHealthyCluster() throws Exception {
        when(cluster.isConnected()).thenReturn(true);
        when(cluster.isDeflectorHealthy()).thenReturn(true);

        final List<MessageWithIndex> messageList = sendMessages(output, config.getOutputBatchSize() - 1);

        output.stop();

        verify(messages, times(1)).bulkIndex(eq(messageList));
    }

    @Test
    @Timeout(1)
    public void stop_withDisconnectedCluster() throws Exception {
        when(cluster.isConnected()).thenReturn(false);

        sendMessages(output, config.getOutputBatchSize() - 1);
        output.stop();

        verifyNoInteractions(messages);
    }

    /**
     * Test that shutdown can proceed even if the index request ends up being blocked.
     */
    @Test
    @Timeout(1)
    public void stop_withIndexingBlocked() throws Exception {
        when(cluster.isConnected()).thenReturn(true);
        when(cluster.isDeflectorHealthy()).thenReturn(true);

        when(messages.bulkIndex(any())).thenAnswer(invocation -> {
            // this will block until interrupted
            new CountDownLatch(1).await();
            return null;
        });

        final List<MessageWithIndex> messageList = sendMessages(output, config.getOutputBatchSize() - 1);

        // shutdown timeout is < test timeout
        output.stop();

        verify(messages, times(1)).bulkIndex(eq(messageList));
    }

    private List<MessageWithIndex> buildMessages(final int count) {
        final ImmutableList.Builder<MessageWithIndex> builder = ImmutableList.builder();
        for (int i = 0; i < count; i++) {
            builder.add(new MessageWithIndex(messageFactory.createMessage("message" + i, "test", Tools.nowUTC()), mock(IndexSet.class)));
        }

        return builder.build();
    }

    private List<MessageWithIndex> sendMessages(BlockingBatchedESOutput output, int count) throws Exception {
        final List<MessageWithIndex> messageList = buildMessages(count);

        for (MessageWithIndex entry : messageList) {
            output.writeMessageEntry(entry);
        }

        return messageList;
    }

}
