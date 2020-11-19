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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.fields.FieldValueType;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EventTest {
    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void fromDto() {
        final DateTime now = DateTime.parse("2019-01-01T00:00:00.000Z");
        final ImmutableList<String> keyTuple = ImmutableList.of("a", "b");

        final EventDto eventDto = EventDto.builder()
                .id("01DF119QKMPCR5VWBXS8783799")
                .eventDefinitionType("aggregation-v1")
                .eventDefinitionId("54e3deadbeefdeadbeefaffe")
                .originContext("urn:graylog:message:es:graylog_0:199a616d-4d48-4155-b4fc-339b1c3129b2")
                .eventTimestamp(now)
                .processingTimestamp(now)
                .timerangeStart(now)
                .timerangeEnd(now.minusHours(1))
                .streams(ImmutableSet.of("000000000000000000000002"))
                .sourceStreams(ImmutableSet.of("000000000000000000000001"))
                .message("Test message")
                .source("source")
                .keyTuple(keyTuple)
                .key(String.join("|", keyTuple))
                .priority(4)
                .alert(false)
                .fields(ImmutableMap.of("hello", "world"))
                .build();

        assertThat(Event.fromDto(eventDto)).satisfies(event -> {
            assertThat(event.getId()).isEqualTo("01DF119QKMPCR5VWBXS8783799");
            assertThat(event.getEventDefinitionType()).isEqualTo("aggregation-v1");
            assertThat(event.getEventDefinitionId()).isEqualTo("54e3deadbeefdeadbeefaffe");
            assertThat(event.getOriginContext()).isEqualTo("urn:graylog:message:es:graylog_0:199a616d-4d48-4155-b4fc-339b1c3129b2");
            assertThat(event.getEventTimestamp()).isEqualTo(now);
            assertThat(event.getProcessingTimestamp()).isEqualTo(now);
            assertThat(event.getTimerangeStart()).isEqualTo(now);
            assertThat(event.getTimerangeEnd()).isEqualTo(now.minusHours(1));
            assertThat(event.getStreams()).isEqualTo(ImmutableSet.of("000000000000000000000002"));
            assertThat(event.getSourceStreams()).isEqualTo(ImmutableSet.of("000000000000000000000001"));
            assertThat(event.getMessage()).isEqualTo("Test message");
            assertThat(event.getSource()).isEqualTo("source");
            assertThat(event.getKeyTuple()).isEqualTo(keyTuple);
            assertThat(event.getPriority()).isEqualTo(4);
            assertThat(event.getAlert()).isFalse();
            assertThat(event.getField("hello").dataType()).isEqualTo(FieldValueType.STRING);
            assertThat(event.getField("hello").value()).isEqualTo("world");
        });
    }
}
