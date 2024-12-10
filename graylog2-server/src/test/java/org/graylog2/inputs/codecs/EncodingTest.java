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
package org.graylog2.inputs.codecs;

import com.jayway.jsonpath.PathNotFoundException;
import org.graylog.testing.messages.MessagesExtension;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.failure.InputProcessingException;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MessagesExtension.class)
class EncodingTest {
    private final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
    final String MSG_FIELD = "short_message";
    final String MESSAGE = "äöüß";
    final String jsonString = "{"
            + "\"version\": \"1.1\","
            + "\"" + MSG_FIELD + "\": \"" + MESSAGE + "\","
            + "\"host\": \"example.org\","
            + "}";
    RawMessage rawUTF8 = new RawMessage(jsonString.getBytes(StandardCharsets.UTF_8));
    RawMessage rawUTF16 = new RawMessage(jsonString.getBytes(StandardCharsets.UTF_16));
    Configuration configUTF8 = new Configuration(Collections.singletonMap(Codec.Config.CK_CHARSET_NAME, StandardCharsets.UTF_8.name()));
    Configuration configUTF16 = new Configuration(Collections.singletonMap(Codec.Config.CK_CHARSET_NAME, StandardCharsets.UTF_16.name()));

    @Test
    void GelfCodecTestUTF8(MessageFactory messageFactory) {
        GelfCodec gelfCodecUTF8 = new GelfCodec(configUTF8, Mockito.mock(GelfChunkAggregator.class), messageFactory);

        final Message message = gelfCodecUTF8.decodeSafe(rawUTF8).get();
        assertThat(message.getMessage()).isEqualTo(MESSAGE);

        Assertions.assertThrows(InputProcessingException.class, () -> gelfCodecUTF8.decodeSafe(rawUTF16).get());
    }

    @Test
    void GelfCodecTestUTF16(MessageFactory messageFactory) {
        GelfCodec gelfCodecUTF16 = new GelfCodec(configUTF16, Mockito.mock(GelfChunkAggregator.class), messageFactory);

        final Message message = gelfCodecUTF16.decodeSafe(rawUTF16).get();
        assertThat(message.getMessage()).isEqualTo(MESSAGE);

        Assertions.assertThrows(InputProcessingException.class, () -> gelfCodecUTF16.decodeSafe(rawUTF8).get());
    }

    @Test
    void JsonPathCodecTestUTF8(MessageFactory messageFactory) {
        Map<String, Object> jsonPathCollectionUTF8 = new HashMap<>();
        jsonPathCollectionUTF8.put(Codec.Config.CK_CHARSET_NAME, StandardCharsets.UTF_8.name());
        jsonPathCollectionUTF8.put(JsonPathCodec.CK_PATH, MSG_FIELD);
        JsonPathCodec jsonPathCodecUTF8 = new JsonPathCodec(new Configuration(jsonPathCollectionUTF8), objectMapperProvider.get(), messageFactory);

        final Message message = jsonPathCodecUTF8.decodeSafe(rawUTF8).get();
        assertThat(message.getMessage()).contains(MESSAGE);

        Assertions.assertThrows(PathNotFoundException.class, () -> jsonPathCodecUTF8.decodeSafe(rawUTF16).get());
    }

    @Test
    void JsonPathCodecTestUTF16(MessageFactory messageFactory) {
        Map<String, Object> jsonPathCollectionUTF16 = new HashMap<>();
        jsonPathCollectionUTF16.put(Codec.Config.CK_CHARSET_NAME, StandardCharsets.UTF_16.name());
        jsonPathCollectionUTF16.put(JsonPathCodec.CK_PATH, MSG_FIELD);
        JsonPathCodec jsonPathCodecUTF16 = new JsonPathCodec(new Configuration(jsonPathCollectionUTF16), objectMapperProvider.get(), messageFactory);

        final Message message = jsonPathCodecUTF16.decodeSafe(rawUTF16).get();
        assertThat(message.getMessage()).contains(MESSAGE);

        Assertions.assertThrows(PathNotFoundException.class, () -> jsonPathCodecUTF16.decodeSafe(rawUTF8).get());
    }
}
