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