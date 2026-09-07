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
package org.graylog2.rest.resources.streams.responses;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StreamDTOResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    void serializesFavoriteFields() throws Exception {
        final StreamDTOResponse toTest = new StreamDTOResponse(
                "stream-id",
                "admin",
                List.of(),
                Stream.MatchingType.AND,
                "desc",
                new DateTime(0, DateTimeZone.UTC),
                List.of(),
                false,
                "title",
                null,
                false,
                false,
                "index-set-id",
                true,
                List.of(),
                List.of("source", "http_method")
        );

        final String json = objectMapper.writeValueAsString(toTest);

        assertThat(json).contains("\"favorite_fields\":[\"source\",\"http_method\"]");
    }

    @Test
    void serializesNullFavoriteFieldsAsNull() throws Exception {
        final StreamDTOResponse toTest = new StreamDTOResponse(
                "stream-id",
                "admin",
                List.of(),
                Stream.MatchingType.AND,
                "desc",
                new DateTime(0, DateTimeZone.UTC),
                List.of(),
                false,
                "title",
                null,
                false,
                false,
                "index-set-id",
                true,
                List.of(),
                null
        );

        final String json = objectMapper.writeValueAsString(toTest);

        assertThat(json).contains("\"favorite_fields\":null");
    }
}
