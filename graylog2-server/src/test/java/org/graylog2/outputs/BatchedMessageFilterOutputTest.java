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
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.outputs.filter.AllOutputsFilter;
import org.graylog2.outputs.filter.DefaultFilteredMessage;
import org.graylog2.outputs.filter.FilteredMessage;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.outputs.FilteredMessageOutput;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MessagesExtension.class)
class BatchedMessageFilterOutputTest {
    @Mock
    private Cluster cluster;
    @Mock
    private MessageQueueAcknowledger acknowledger;
    @Mock
    private Stream defaultStream;
    @Mock
    private IndexSet indexSet;
    @Mock(extraInterfaces = MessageOutput.class)
    private FilteredMessageOutput targetOutput1;

    private MessageFactory messageFactory;
    private BatchedMessageFilterOutput output;
    private int outputBatchSize;
    private final int shutdownTimeoutMs = "true".equals(System.getenv("CI")) ? 500 : 100; // be more graceful when running on ci infrastructure
    private final int outputFlushInterval = 1;

    @BeforeEach
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    void setUp(MessageFactory messageFactory) {
        this.messageFactory = messageFactory;
        this.outputBatchSize = 3;
        this.output = new BatchedMessageFilterOutput(
                Map.of("targetOutput1", targetOutput1),
                new AllOutputsFilter(Map.of(ElasticSearchOutput.FILTER_KEY, mock(FilteredMessageOutput.class))),
                new MetricRegistry(),
                cluster,
                acknowledger,
                outputBatchSize,
                outputFlushInterval,
                shutdownTimeoutMs,
                Executors.newSingleThreadScheduledExecutor()
        );

        lenient().when(defaultStream.getIndexSet()).thenReturn(indexSet);
    }

    @AfterEach
    public void after() {
        if (output != null) {
            output.cancelFlushTask();
        }
    }

    @Test
    public void writeMessages() throws Exception {
        final var messageList = sendMessages(output, outputBatchSize);

        verify(targetOutput1, times(1)).writeFiltered(messagesWithOutput(messageList));
    }

    @Test
    public void writeMoreMessages() throws Exception {
        sendMessages(output, outputBatchSize * 3);

        verify(targetOutput1, times(3)).writeFiltered(anyList());
    }

    @Test
    public void forceFlush() throws Exception {
        final var messageList = sendMessages(output, outputBatchSize - 1);

        // No interactions yet because we flush on 3 messages but only sent 2.
        verifyNoInteractions(targetOutput1);

        output.forceFlush();

        verify(targetOutput1, times(1)).writeFiltered(messagesWithOutput(messageList));
    }

    @Test
    public void waitForScheduledFlush() throws Exception {
        // Call initialize to start the scheduled flush job.
        output.initialize();

        final var messageList = sendMessages(output, outputBatchSize - 1);

        Thread.sleep(outputFlushInterval * 1000L + 100); // Let the scheduled flush job run

        verify(targetOutput1, times(1)).writeFiltered(eq(messagesWithOutput(messageList)));
    }

    @Test
    public void stopWithHealthyCluster() throws Exception {
        when(cluster.isConnected()).thenReturn(true);
        when(cluster.isDeflectorHealthy()).thenReturn(true);

        final var messageList = sendMessages(output, outputBatchSize - 1);

        output.stop();

        verify(targetOutput1, times(1)).writeFiltered(messagesWithOutput(messageList));
    }

    @Test
    @Timeout(1)
    public void stopWithDisconnectedCluster() throws Exception {
        when(cluster.isConnected()).thenReturn(false);

        sendMessages(output, outputBatchSize - 1);
        output.stop();

        verifyNoInteractions(targetOutput1);
    }

    /**
     * Test that shutdown can proceed even if the index request ends up being blocked.
     */
    @Test
    @Timeout(1)
    public void stopWithIndexingBlocked() throws Exception {
        when(cluster.isConnected()).thenReturn(true);
        when(cluster.isDeflectorHealthy()).thenReturn(true);

        doAnswer(invocation -> {
            // this will block until interrupted
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                // Ignore
            }
            return null;
        }).when(targetOutput1).writeFiltered(anyList());

        final var messageList = sendMessages(output, outputBatchSize - 1);

        // The shutdownTimeoutMs is < than this test's @Timeout(1), so the stop method should return before the
        // test timeout triggers.
        output.stop();

        verify(targetOutput1, times(1)).writeFiltered(messagesWithOutput(messageList));
    }

    private List<FilteredMessage> messagesWithOutput(List<Message> messages) {
        return messages.stream()
                .map(message -> DefaultFilteredMessage.forDestinationKeys(message, Set.of(ElasticSearchOutput.FILTER_KEY)))
                .collect(Collectors.toList());
    }

    private List<Message> buildMessages(final int count) {
        final ImmutableList.Builder<Message> builder = ImmutableList.builder();
        for (int i = 0; i < count; i++) {
            final Message message = messageFactory.createMessage("message" + i, "test", Tools.nowUTC());
            message.addStream(defaultStream);
            builder.add(message);
        }

        return builder.build();
    }

    private List<Message> sendMessages(BatchedMessageFilterOutput output, int count) throws Exception {
        final var messageList = buildMessages(count);

        for (Message entry : messageList) {
            output.write(entry);
        }

        return messageList;
    }
}
