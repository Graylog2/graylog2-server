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
package org.graylog.events.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EventTimestampConverterTest {

    private EventTimestampConverter converter;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        converter = new EventTimestampConverter();
        objectMapper = new ObjectMapperProvider().get();
    }

    @Test
    public void convert() {
        assertThat(converter.convert("2019-07-02 14:57:52.159")).isEqualTo(DateTime.parse("2019-07-02T14:57:52.159Z"));
        assertThat(converter.convert("2019-07-02T14:57:52.159")).isEqualTo(DateTime.parse("2019-07-02T14:57:52.159Z"));
        assertThat(converter.convert("2019-07-02 14:57:52")).isEqualTo(DateTime.parse("2019-07-02T14:57:52.000Z"));
        assertThat(converter.convert("2019-07-02T14:57:52")).isEqualTo(DateTime.parse("2019-07-02T14:57:52.000Z"));
    }

    @Test
    public void convertWithObjectMapper() throws Exception {
        assertThat(objectMapper.readValue("{\"date\":\"2019-07-02 14:57:52.159\"}", DTO.class).date)
                .isEqualTo(DateTime.parse("2019-07-02T14:57:52.159Z"));
        assertThat(objectMapper.readValue("{\"date\":\"2019-07-02T14:57:52.159\"}", DTO.class).date)
                .isEqualTo(DateTime.parse("2019-07-02T14:57:52.159Z"));

        assertThat(objectMapper.readValue("{\"date\":\"2019-07-02 14:57:51\"}", DTO.class).date)
                .isEqualTo(DateTime.parse("2019-07-02T14:57:51.000Z"));
        assertThat(objectMapper.readValue("{\"date\":\"2019-07-02T14:57:51\"}", DTO.class).date)
                .isEqualTo(DateTime.parse("2019-07-02T14:57:51.000Z"));
    }

    private static class DTO {
        @JsonProperty
        @JsonDeserialize(converter = EventTimestampConverter.class)
        DateTime date;
    }
}