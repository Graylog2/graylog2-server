package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class SeriesDescription {
    @JsonProperty
    public abstract String type();

    public static SeriesDescription create(String type) {
        return new AutoValue_SeriesDescription(type);
    }
}
