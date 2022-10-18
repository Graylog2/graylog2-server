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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoJodaDateTimeSerializerTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    public void serializeZonedDateTime() throws Exception {
        final TestBean testBean = new TestBean(new DateTime(2016, 12, 13, 16, 0, DateTimeZone.forOffsetHours(2)));
        final String valueAsString = objectMapper.writeValueAsString(testBean);
        assertThat(valueAsString)
                .isNotNull()
                .isEqualTo("{\"date_time\":\"2016-12-13T14:00:00.000+0000\"}");
    }

    @Test
    public void serializeNull() throws Exception {
        final TestBean testBean = new TestBean(null);
        final String valueAsString = objectMapper.writeValueAsString(testBean);
        assertThat(valueAsString)
                .isNotNull()
                .isEqualTo("{\"date_time\":null}");
    }

    static class TestBean {
        @JsonSerialize(using = MongoJodaDateTimeSerializer.class)
        DateTime dateTime;

        public TestBean(DateTime dateTime) {
            this.dateTime = dateTime;
        }
    }
}
