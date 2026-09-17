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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class EventDtoTest {
    @Test
    public void ignoreIdFieldWithUnderscore() throws Exception {
        final URL eventString = Resources.getResource(getClass(), "filter-event-from-elasticsearch.json");
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();

        final EventDto eventDto = objectMapper.readValue(eventString, EventDto.class);

        assertThat(eventDto.id()).isEqualTo("01DNM0DVJDV52NA5VEBTYJ6PJY");
    }

    @Test
    public void deserializeWithESTimestamps() throws Exception {
        // Checks that the EventDto is using the "ESMongoDateTimeDeserializer" deserializer to be able
        // to parse our ES timestamps.

        final URL eventString = Resources.getResource(getClass(), "aggregation-event-from-elasticsearch.json");
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();

        final EventDto eventDto = objectMapper.readValue(eventString, EventDto.class);

        assertThat(eventDto.eventTimestamp()).isEqualTo(DateTime.parse("2019-08-21T07:48:01.326Z"));
        assertThat(eventDto.processingTimestamp()).isEqualTo(DateTime.parse("2019-09-25T10:35:57.116Z"));
        assertThat(eventDto.timerangeStart()).get().isEqualTo(DateTime.parse("2019-08-21T07:47:41.213Z"));
        assertThat(eventDto.timerangeEnd()).get().isEqualTo(DateTime.parse("2019-08-21T07:48:41.212Z"));
    }

    @Test
    public void sortsFieldsAlphabeticallyWhenBuilt() {
        final Map<String, String> unsorted = new LinkedHashMap<>();
        unsorted.put("charlie", "3");
        unsorted.put("zulu", "26");
        unsorted.put("alpha", "1");

        final EventDto eventDto = testDto().fields(unsorted).build();

        assertThat(eventDto.fields().keySet()).containsExactly("alpha", "charlie", "zulu");
    }

    @Test
    public void sortsGroupByFieldsAlphabeticallyWhenBuilt() {
        final Map<String, String> unsorted = new LinkedHashMap<>();
        unsorted.put("beta", "2");
        unsorted.put("alpha", "1");

        final EventDto eventDto = testDto().groupByFields(unsorted).build();

        assertThat(eventDto.groupByFields().keySet()).containsExactly("alpha", "beta");
    }

    @Test
    public void sortsFieldsWhenDeserializedFromElasticsearch() throws Exception {
        final URL eventString = Resources.getResource(getClass(), "unsorted-fields-event-from-elasticsearch.json");
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();

        final EventDto eventDto = objectMapper.readValue(eventString, EventDto.class);

        assertThat(eventDto.fields().keySet()).containsExactly("alpha", "charlie", "zulu");
    }

    private EventDto.Builder testDto() {
        return EventDto.builder()
                .id("01DNM0DVJDV52NA5VEBTYJ6PJY")
                .eventDefinitionType("aggregation-v1")
                .eventDefinitionId("event-definition-id")
                .eventTimestamp(DateTime.parse("2019-08-21T07:48:01.326Z"))
                .processingTimestamp(DateTime.parse("2019-09-25T10:35:57.116Z"))
                .message("message")
                .source("source")
                .keyTuple(ImmutableList.of())
                .priority(1)
                .alert(false)
                .fields(ImmutableMap.of())
                .groupByFields(ImmutableMap.of());
    }
}
