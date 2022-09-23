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
package org.graylog2.plugin.indexer.searches.timeranges;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class AbsoluteRangeTest {


    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(TimeRange.class, AbsoluteRange.class);
    }

    @Test
    void testDeserialize() throws IOException {
        final AbsoluteRange range = objectMapper.readValue("{\"type\":\"absolute\",\"from\":\"2022-08-30T10:53:59.910Z\",\"to\":\"2022-08-30T11:43:59.910Z\"}", AbsoluteRange.class);
        assertThat(range.type()).isEqualTo(AbsoluteRange.ABSOLUTE);
        assertThat(range.from()).isNotNull();
        assertThat(range.getTo()).isNotNull();
    }

    @Test
    void testSerialize() throws JsonProcessingException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final AbsoluteRange range = AbsoluteRange.builder()
                .from(now.minus(1000 * 60 * 50))
                .to(now)
                .build();
        final String serialized = objectMapper.writeValueAsString(range);
        assertThat(serialized).contains("\"type\":\"absolute\"");
    }

    @Test
    void testBuilderWithoutExplicitType() throws JsonProcessingException {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final AbsoluteRange range = AbsoluteRange.builder()
                .from(now.minus(1000 * 60 * 50))
                .to(now)
                .build();
        assertThat(range.type()).isEqualTo(AbsoluteRange.ABSOLUTE);
    }
}
