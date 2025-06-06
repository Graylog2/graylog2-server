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
import org.graylog2.indexer.messages.IndexingResults;
import org.graylog2.indexer.messages.MessageWithIndex;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.outputs.filter.DefaultFilteredMessage;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.SuppressForbidden;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.messages.ImmutableMessage.wrap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MessagesExtension.class)
public class ElasticSearchOutputTest {

    @Mock
    private Messages messages;

    @Mock
    private IndexingResults indexingResults;

    @Mock
    private IndexSet defaultIndexSet;

    @Mock
    private Stream defaultStream;

    @Mock
    private IndexSet testIndexSet;

    @Mock
    private Stream testStream;

    private ElasticSearchOutput output;
    private MessageFactory messageFactory;

    @BeforeEach
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp(MessageFactory messageFactory) throws Exception {
        this.messageFactory = messageFactory;

        output = new ElasticSearchOutput(new MetricRegistry(), messages);
        output.initialize();

        lenient().when(defaultStream.getIndexSet()).thenReturn(defaultIndexSet);
        lenient().when(testStream.getIndexSet()).thenReturn(testIndexSet);
        lenient().when(indexingResults.errors()).thenReturn(ImmutableList.of());
        lenient().when(messages.bulkIndex(any())).thenReturn(indexingResults);
    }

    @AfterEach
    public void tearDown() {
        output.stop();
    }

    @Test
    public void write() throws Exception {
        final List<Message> messageList = buildMessages(3);

        for (final var message : messageList) {
            output.write(message);
        }

        verify(messages, times(1)).bulkIndex(eq(List.of(
                new MessageWithIndex(wrap(messageList.get(0)), defaultIndexSet)
        )));
        verify(messages, times(1)).bulkIndex(eq(List.of(
                new MessageWithIndex(wrap(messageList.get(1)), defaultIndexSet)
        )));
        verify(messages, times(1)).bulkIndex(eq(List.of(
                new MessageWithIndex(wrap(messageList.get(2)), defaultIndexSet)
        )));

        verifyNoMoreInteractions(messages);
    }

    @Test
    public void writeList() throws Exception {
        final List<Message> messageList = buildMessages(3);

        output.write(messageList);

        verify(messages, times(1)).bulkIndex(eq(List.of(
                new MessageWithIndex(wrap(messageList.get(0)), defaultIndexSet),
                new MessageWithIndex(wrap(messageList.get(1)), defaultIndexSet),
                new MessageWithIndex(wrap(messageList.get(2)), defaultIndexSet)
        )));

        verifyNoMoreInteractions(messages);
    }

    @Test
    public void writeListWithMultipleStreams() throws Exception {
        final List<Message> messageList = buildMessages(4);

        // The bulkIndex method should receive a separate message per stream/index-set. If a message has two streams
        // with two different index sets, we should see the same message twice, one per index-set.
        messageList.forEach(message -> message.addStream(testStream));
        messageList.get(3).removeStream(defaultStream);

        output.write(messageList);

        verify(messages, times(1)).bulkIndex(argThat(argument -> {
            assertThat(argument).size().isEqualTo(7);
            assertThat(argument).containsExactlyInAnyOrderElementsOf(List.of(
                    new MessageWithIndex(wrap(messageList.get(0)), defaultIndexSet),
                    new MessageWithIndex(wrap(messageList.get(0)), testIndexSet),
                    new MessageWithIndex(wrap(messageList.get(1)), defaultIndexSet),
                    new MessageWithIndex(wrap(messageList.get(1)), testIndexSet),
                    new MessageWithIndex(wrap(messageList.get(2)), defaultIndexSet),
                    new MessageWithIndex(wrap(messageList.get(2)), testIndexSet),
                    // Only one message for the 4th message because it only contains the test stream.
                    new MessageWithIndex(wrap(messageList.get(3)), testIndexSet)
            ));
            return true;
        }));

        verifyNoMoreInteractions(messages);
    }

    @Test
    public void writeFiltered() throws Exception {
        final List<Message> messageList = buildMessages(2);

        output.writeFiltered(List.of(
                // The first message should not be written to the output because the output's filter key is not included.
                DefaultFilteredMessage.forDestinationKeys(messageList.get(0), Set.of("foo")),
                DefaultFilteredMessage.forDestinationKeys(messageList.get(1), Set.of("foo", ElasticSearchOutput.FILTER_KEY))
        ));

        verify(messages, times(1)).bulkIndex(eq(List.of(
                new MessageWithIndex(wrap(messageList.get(1)), defaultIndexSet)
        )));

        verifyNoMoreInteractions(messages);
    }

    @Test
    public void writeFilteredWithMultipleStreams() throws Exception {
        final List<Message> messageList = buildMessages(2);

        messageList.forEach(message -> message.addStream(testStream));

        output.writeFiltered(List.of(
                // The first message should not be written to the output because the output's filter key is not included.
                DefaultFilteredMessage.forDestinationKeys(messageList.get(0), Set.of("foo")),
                DefaultFilteredMessage.forDestinationKeys(messageList.get(1), Set.of("foo", ElasticSearchOutput.FILTER_KEY))
        ));

        verify(messages, times(1)).bulkIndex(argThat(argument -> {
            assertThat(argument).size().isEqualTo(2);
            assertThat(argument).containsExactlyInAnyOrderElementsOf(List.of(
                    new MessageWithIndex(wrap(messageList.get(1)), defaultIndexSet),
                    new MessageWithIndex(wrap(messageList.get(1)), testIndexSet)
            ));
            return true;
        }));

        verifyNoMoreInteractions(messages);
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
}
