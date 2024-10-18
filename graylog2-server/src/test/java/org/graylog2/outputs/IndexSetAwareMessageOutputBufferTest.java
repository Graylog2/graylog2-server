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

import org.graylog.testing.messages.MessagesExtension;
import org.graylog2.outputs.filter.DefaultFilteredMessage;
import org.graylog2.outputs.filter.FilteredMessage;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MessagesExtension.class)
class IndexSetAwareMessageOutputBufferTest {
    @Mock
    private Consumer<List<FilteredMessage>> flusher;

    private MessageFactory messageFactory;
    private IndexSetAwareMessageOutputBuffer buffer;

    @BeforeEach
    void setUp(MessageFactory messageFactory) {
        this.messageFactory = messageFactory;
        this.buffer = new IndexSetAwareMessageOutputBuffer(BatchSizeConfig.forCount(5),
                new ObjectMapperProvider().get());
    }

    @Test
    void shouldFlush() {
        // No interactions yet, we want to flush because we don't know the last execution time.
        assertThat(buffer.shouldFlush(Duration.ofSeconds(1))).isTrue();

        // Trigger a flush
        buffer.flush(flusher);

        // The last flush just happened, flush interval not reached.
        assertThat(buffer.shouldFlush(Duration.ofDays(1))).isFalse();

        // The last flush was more than 1 ns ago, flush.
        assertThat(buffer.shouldFlush(Duration.ofNanos(1))).isTrue();
    }

    @Test
    void appendAndFlush() {
        final var messages = createNMessages(6);

        // With a buffer size of 5, only the 5th call should flush the messages.
        buffer.appendAndFlush(messages.get(0), flusher);
        verifyNoInteractions(flusher);
        buffer.appendAndFlush(messages.get(1), flusher);
        verifyNoInteractions(flusher);
        buffer.appendAndFlush(messages.get(2), flusher);
        verifyNoInteractions(flusher);
        buffer.appendAndFlush(messages.get(3), flusher);
        verifyNoInteractions(flusher);
        buffer.appendAndFlush(messages.get(4), flusher);
        // Flusher should only get the first 5 messages.
        verify(flusher, times(1)).accept(messages.subList(0, 5));

        // The 6th message shouldn't trigger a flush.
        buffer.appendAndFlush(messages.get(5), flusher);
        verifyNoMoreInteractions(flusher);
    }

    @Test
    void flush() {
        createNMessages(2).forEach(message -> buffer.appendAndFlush(message, flusher));
        buffer.flush(flusher);

        verify(flusher, times(1)).accept(argThat(argument -> {
            assertThat(argument).hasSize(2);
            return true;
        }));

        buffer.flush(flusher);
        buffer.flush(flusher);

        verify(flusher, times(2)).accept(List.of());
    }

    private List<FilteredMessage> createNMessages(int num) {
        return IntStream.range(1, num + 1)
                .mapToObj(i -> createMessage("" + i, Set.of()))
                .toList();
    }

    private FilteredMessage createMessage(String message, Set<String> outputs) {
        return DefaultFilteredMessage.forDestinationKeys(messageFactory.createMessage(message, "source", Tools.nowUTC()), outputs);
    }
}
