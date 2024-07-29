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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.failure.ProcessingFailureCause;
import org.graylog.testing.messages.MessagesExtension;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MessagesExtension.class)
class SerializationMemoizingMessageTest {

    @Test
    void serializeTwice(MessageFactory messageFactory) throws JsonProcessingException {
        final Message wrappedMsg = messageFactory.createMessage("test message", "test source",
                DateTime.now());
        wrappedMsg.addProcessingError(
                new Message.ProcessingError(ProcessingFailureCause.InvalidTimestampException, "", ""));

        final SerializationMemoizingMessage msg = new SerializationMemoizingMessage(wrappedMsg, true);

        final ObjectMapper objectMapper = new ObjectMapperProvider().get();

        final Meter tsMeter = new Meter();
        final byte[] serializedBytes = msg.serialize(objectMapper, tsMeter);

        assertThat(msg.cacheStats().hitCount()).isEqualTo(0);
        assertThat(msg.cacheStats().loadSuccessCount()).isEqualTo(1);
        assertThat(new String(serializedBytes, StandardCharsets.UTF_8)).contains("\"message\":\"test message\"");
        assertThat(tsMeter.getCount()).isEqualTo(1);

        final Meter tsMeter2 = new Meter();
        final byte[] serializedBytes2 = msg.serialize(objectMapper, tsMeter2);

        assertThat(msg.cacheStats().hitCount()).isEqualTo(1);
        assertThat(msg.cacheStats().loadSuccessCount()).isEqualTo(1);
        assertThat(serializedBytes2).isEqualTo(serializedBytes);
        assertThat(tsMeter2.getCount()).isEqualTo(1);
    }

    @Test
    void differentObjectMappers(MessageFactory messageFactory) throws JsonProcessingException {
        final var msg = new SerializationMemoizingMessage(
                messageFactory.createMessage("test message", "test source", DateTime.now()),
                true);

        msg.serialize(new ObjectMapperProvider().get(), new Meter());
        msg.serialize(new ObjectMapperProvider().get(), new Meter());

        assertThat(msg.cacheStats().loadCount()).isEqualTo(2);
    }
}
