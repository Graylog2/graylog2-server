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
package org.graylog.plugins.views.search.searchtypes.eventlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.event.EventDto;
import org.graylog.plugins.views.search.searchtypes.events.EventSummary;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class EventSummaryTest {

    private EventDto buildTestEvent(DateTime timestamp, String key) {
        return EventDto.builder()
                .id("dead-beef")
                .message("message")
                .sourceStreams(Set.of("stream-id-1", "stream-id-2"))
                .eventTimestamp(timestamp)
                .alert(false)
                .eventDefinitionId("deadbeef")
                .priority(2)
                .key(key)
                .keyTuple(key != null ? List.of(key) : List.of())
                .eventDefinitionType("aggregation-v1")
                .processingTimestamp(timestamp)
                .streams(Set.of())
                .source("localhost")
                .fields(Map.of())
                .build();
    }

    @Test
    public void testParseRawEvent() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final var rawEvent = buildTestEvent(now, null);

        EventSummary eventSummary = EventSummary.parse(rawEvent);
        assertThat(eventSummary.id()).isEqualTo("dead-beef");
        assertThat(eventSummary.message()).isEqualTo("message");
        assertThat(eventSummary.streams()).isEqualTo(ImmutableSet.of("stream-id-1", "stream-id-2"));
        assertThat(eventSummary.timestamp().toString(Tools.ES_DATE_FORMAT_FORMATTER))
                .isEqualTo(now.toString(Tools.ES_DATE_FORMAT_FORMATTER));
        assertThat(eventSummary.alert()).isEqualTo(false);
    }

    @Test
    public void parseShouldPreserveEventKey() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final var rawEvent = buildTestEvent(now, "testkey_1");

        EventSummary eventSummary = EventSummary.parse(rawEvent);
        assertThat(eventSummary.key()).isEqualTo("testkey_1");
    }

    @Test
    public void parseShouldHandleNullEventKey() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final var rawEvent = buildTestEvent(now, null);

        EventSummary eventSummary = EventSummary.parse(rawEvent);
        assertThat(eventSummary.key()).isNull();
    }

    @Test
    public void serializedJsonShouldContainKeyField() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final var rawEvent = buildTestEvent(now, "testkey_1");

        EventSummary eventSummary = EventSummary.parse(rawEvent);
        final String json = objectMapper.writeValueAsString(eventSummary);
        assertThat(json).contains("\"key\":\"testkey_1\"");
    }
}
