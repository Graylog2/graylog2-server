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