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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoZonedDateTimeDeserializerTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    public void deserializeZonedDateTime() throws Exception {
        final String json = "{\"date_time\":\"2016-12-13T16:00:00.000+0200\"}";
        final TestBean value = objectMapper.readValue(json, TestBean.class);
        assertThat(value.dateTime).isEqualTo(ZonedDateTime.of(2016, 12, 13, 14, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    public void deserializeNull() throws Exception {
        final String json = "{\"date_time\":null}";
        final TestBean value = objectMapper.readValue(json, TestBean.class);
        assertThat(value.dateTime).isNull();
    }

    static class TestBean {
        @JsonDeserialize(using = MongoZonedDateTimeDeserializer.class)
        ZonedDateTime dateTime;
    }
}