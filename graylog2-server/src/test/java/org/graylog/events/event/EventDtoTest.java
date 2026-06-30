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
package org.graylog.events.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class EventDtoTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    public void ignoreIdFieldWithUnderscore() throws Exception {
        final URL eventString = Resources.getResource(getClass(), "filter-event-from-elasticsearch.json");

        final EventDto eventDto = objectMapper.readValue(eventString, EventDto.class);

        assertThat(eventDto.id()).isEqualTo("01DNM0DVJDV52NA5VEBTYJ6PJY");
    }

    @Test
    public void deserializeWithESTimestamps() throws Exception {
        // Checks that the EventDto is using the "ESMongoDateTimeDeserializer" deserializer to be able
        // to parse our ES timestamps.

        final URL eventString = Resources.getResource(getClass(), "aggregation-event-from-elasticsearch.json");

        final EventDto eventDto = objectMapper.readValue(eventString, EventDto.class);

        assertThat(eventDto.eventTimestamp()).isEqualTo(DateTime.parse("2019-08-21T07:48:01.326Z"));
        assertThat(eventDto.processingTimestamp()).isEqualTo(DateTime.parse("2019-09-25T10:35:57.116Z"));
        assertThat(eventDto.timerangeStart()).get().isEqualTo(DateTime.parse("2019-08-21T07:47:41.213Z"));
        assertThat(eventDto.timerangeEnd()).get().isEqualTo(DateTime.parse("2019-08-21T07:48:41.212Z"));
    }

    @Test
    public void excludedByRuleIdRoundTripsThroughJackson() throws Exception {
        final EventDto dto = sampleBuilder()
                .excludedByRuleId("rule-42")
                .build();
        final String json = objectMapper.writeValueAsString(dto);
        final EventDto back = objectMapper.readValue(json, EventDto.class);
        assertThat(back.excludedByRuleId()).isEqualTo("rule-42");
    }

    @Test
    public void excludedByRuleIdDefaultsToNull() {
        final EventDto dto = sampleBuilder().build();
        assertThat(dto.excludedByRuleId()).isNull();
    }

    private EventDto.Builder sampleBuilder() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        return EventDto.builder()
                .id("dead-beef")
                .message("message")
                .sourceStreams(Set.of("stream-id-1"))
                .eventTimestamp(now)
                .alert(false)
                .eventDefinitionId("deadbeef")
                .priority(2)
                .keyTuple(List.of())
                .eventDefinitionType("aggregation-v1")
                .processingTimestamp(now)
                .streams(Set.of())
                .source("localhost")
                .fields(Map.of());
    }
}
