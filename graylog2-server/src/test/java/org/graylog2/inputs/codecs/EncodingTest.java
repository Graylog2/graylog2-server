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
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
    void GelfCodecTestUTF8() {
        GelfCodec gelfCodecUTF8 = new GelfCodec(configUTF8, Mockito.mock(GelfChunkAggregator.class));

        final Message message = gelfCodecUTF8.decode(rawUTF8);
        assertThat(message.getMessage()).isEqualTo(MESSAGE);

        Assertions.assertThrows(IllegalStateException.class, () -> {gelfCodecUTF8.decode(rawUTF16);});
    }

    @Test
    void GelfCodecTestUTF16() {
        GelfCodec gelfCodecUTF16 = new GelfCodec(configUTF16, Mockito.mock(GelfChunkAggregator.class));

        final Message message = gelfCodecUTF16.decode(rawUTF16);
        assertThat(message.getMessage()).isEqualTo(MESSAGE);

        Assertions.assertThrows(IllegalStateException.class, () -> {gelfCodecUTF16.decode(rawUTF8);});
    }

    @Test
    void JsonPathCodecTestUTF8() {
        Map<String, Object> jsonPathCollectionUTF8 = new HashMap<>();
        jsonPathCollectionUTF8.put(Codec.Config.CK_CHARSET_NAME, StandardCharsets.UTF_8.name());
        jsonPathCollectionUTF8.put(JsonPathCodec.CK_PATH, MSG_FIELD);
        JsonPathCodec jsonPathCodecUTF8 = new JsonPathCodec(new Configuration(jsonPathCollectionUTF8),objectMapperProvider.get());

        final Message message = jsonPathCodecUTF8.decode(rawUTF8);
        assertThat(message.getMessage()).contains(MESSAGE);

        Assertions.assertThrows(PathNotFoundException.class, () -> {jsonPathCodecUTF8.decode(rawUTF16);});
    }

    @Test
    void JsonPathCodecTestUTF16() {
        Map<String, Object> jsonPathCollectionUTF16 = new HashMap<>();
        jsonPathCollectionUTF16.put(Codec.Config.CK_CHARSET_NAME, StandardCharsets.UTF_16.name());
        jsonPathCollectionUTF16.put(JsonPathCodec.CK_PATH, MSG_FIELD);
        JsonPathCodec jsonPathCodecUTF16 = new JsonPathCodec(new Configuration(jsonPathCollectionUTF16),objectMapperProvider.get());

        final Message message = jsonPathCodecUTF16.decode(rawUTF16);
        assertThat(message.getMessage()).contains(MESSAGE);

        Assertions.assertThrows(PathNotFoundException.class, () -> {jsonPathCodecUTF16.decode(rawUTF8);});
    }
}
