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
package org.graylog2.indexer.messages;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.failure.ProcessingFailureCause;
import org.graylog.testing.messages.MessagesExtension;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MessagesExtension.class)
class SerializationMemoizingMessageTest {

    @Test
    void getInputMessageSizeReturnsFieldValue(MessageFactory messageFactory) {
        final Message message = messageFactory.createMessage("test", "source", DateTime.now(DateTimeZone.UTC));
        message.addField(Message.FIELD_GL2_INPUT_MESSAGE_SIZE, 500L);
        final SerializationMemoizingMessage msg = new SerializationMemoizingMessage(message);

        assertThat(msg.getInputMessageSize()).isEqualTo(500L);
    }

    @Test
    void getInputMessageSizeFallsBackToGetSizeWhenFieldAbsent(MessageFactory messageFactory) {
        final Message message = messageFactory.createMessage("test", "source", DateTime.now(DateTimeZone.UTC));
        final SerializationMemoizingMessage msg = new SerializationMemoizingMessage(message);

        assertThat(msg.getInputMessageSize()).isEqualTo(message.getSize());
    }

    @Test
    void getInputMessageSizeFallsBackToGetSizeWhenFieldIsNotANumber(MessageFactory messageFactory) {
        final Message message = messageFactory.createMessage("test", "source", DateTime.now(DateTimeZone.UTC));
        message.addField(Message.FIELD_GL2_INPUT_MESSAGE_SIZE, "not-a-number");
        final SerializationMemoizingMessage msg = new SerializationMemoizingMessage(message);

        assertThat(msg.getInputMessageSize()).isEqualTo(message.getSize());
    }

    @Test
    void getInputMessageSizeHandlesIntegerFieldValue(MessageFactory messageFactory) {
        final Message message = messageFactory.createMessage("test", "source", DateTime.now(DateTimeZone.UTC));
        // Some codecs might set this as an Integer rather than Long
        message.addField(Message.FIELD_GL2_INPUT_MESSAGE_SIZE, 42);
        final SerializationMemoizingMessage msg = new SerializationMemoizingMessage(message);

        assertThat(msg.getInputMessageSize()).isEqualTo(42L);
    }

    @Test
    void serializeTwice(MessageFactory messageFactory) throws IOException {
        final Message wrappedMsg = spy(messageFactory.createMessage("test message", "test source",
                DateTime.now(DateTimeZone.UTC)));
        wrappedMsg.addProcessingError(
                new Message.ProcessingError(ProcessingFailureCause.InvalidTimestampException, "", ""));

        final SerializationMemoizingMessage msg = new SerializationMemoizingMessage(wrappedMsg);

        final ObjectMapper objectMapper = new ObjectMapperProvider().get();

        verify(wrappedMsg, times(0)).toElasticSearchObject(eq(objectMapper), any(Meter.class));

        final Meter tsMeter = new Meter();
        final byte[] serializedBytes = msg.serialize(new DefaultSerializationContext(objectMapper, tsMeter));

        verify(wrappedMsg, times(1)).toElasticSearchObject(eq(objectMapper), any(Meter.class));
        assertThat(new String(serializedBytes, StandardCharsets.UTF_8)).contains("\"message\":\"test message\"");
        assertThat(tsMeter.getCount()).isEqualTo(1);

        final Meter tsMeter2 = new Meter();
        final byte[] serializedBytes2 = msg.serialize(new DefaultSerializationContext(objectMapper, tsMeter2));

        verify(wrappedMsg, times(1)).toElasticSearchObject(eq(objectMapper), any(Meter.class));
        assertThat(serializedBytes2).isEqualTo(serializedBytes);
        assertThat(tsMeter2.getCount()).isEqualTo(1);
    }

    @Test
    void differentObjectMappers(MessageFactory messageFactory) throws IOException {
        final Message wrappedMsg = spy(messageFactory.createMessage("test message", "test source",
                DateTime.now(DateTimeZone.UTC)));
        final var msg = new SerializationMemoizingMessage(wrappedMsg);

        final var context1 = new DefaultSerializationContext(new ObjectMapperProvider().get(), new Meter());
        final var context2 = new DefaultSerializationContext(new ObjectMapperProvider().get(), new Meter());

        msg.serialize(context1);
        msg.serialize(context2);

        verify(wrappedMsg).toElasticSearchObject(eq(context1.objectMapper()), any(Meter.class));
        verify(wrappedMsg).toElasticSearchObject(eq(context2.objectMapper()), any(Meter.class));
    }
}
