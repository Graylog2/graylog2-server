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
package org.graylog2.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MongoInstantDeserializerTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    public void deserializeFromString() throws Exception {
        final String json = "{\"instant\":\"2016-12-13T14:00:00Z\"}";
        final TestBean value = objectMapper.readValue(json, TestBean.class);
        assertThat(value.instant).isEqualTo(Instant.parse("2016-12-13T14:00:00Z"));
    }

    @Test
    public void deserializeFromEmbeddedDate() throws Exception {
        final Instant expected = Instant.parse("2016-12-13T14:00:00Z");
        final JsonParser jsonParser = mock(JsonParser.class);
        when(jsonParser.currentToken()).thenReturn(JsonToken.VALUE_EMBEDDED_OBJECT);
        when(jsonParser.getEmbeddedObject()).thenReturn(Date.from(expected));

        final MongoInstantDeserializer deserializer = new MongoInstantDeserializer();
        final Instant result = deserializer.deserialize(jsonParser, null);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void deserializeUnsupportedTokenThrows() throws Exception {
        final JsonParser jsonParser = mock(JsonParser.class);
        when(jsonParser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_INT);

        final MongoInstantDeserializer deserializer = new MongoInstantDeserializer();
        assertThatThrownBy(() -> deserializer.deserialize(jsonParser, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported token");
    }

    @Test
    public void deserializeNull() throws Exception {
        final String json = "{\"instant\":null}";
        final TestBean value = objectMapper.readValue(json, TestBean.class);
        assertThat(value.instant).isNull();
    }

    static class TestBean {
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        Instant instant;
    }
}
