/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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