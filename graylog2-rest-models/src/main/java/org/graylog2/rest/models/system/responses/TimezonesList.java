package org.graylog2.rest.models.system.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Multimap;

@AutoValue
@JsonAutoDetect
public abstract class TimezonesList {
    @JsonProperty
    public abstract Multimap<String, String> timezones();

    @JsonCreator
    public static TimezonesList create(@JsonProperty("timezones") Multimap<String, String> timezones) {
        return new AutoValue_TimezonesList(timezones);
    }
}
