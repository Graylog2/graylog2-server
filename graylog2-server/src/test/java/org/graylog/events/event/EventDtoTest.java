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
import org.junit.Test;

import java.net.URL;

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
}
