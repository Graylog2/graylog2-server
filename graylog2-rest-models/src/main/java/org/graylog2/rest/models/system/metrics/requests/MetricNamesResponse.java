package org.graylog2.rest.models.system.metrics.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
public abstract class MetricNamesResponse {
    @JsonProperty
    public abstract Set<String> names();

    @JsonCreator
    public static MetricNamesResponse create(@JsonProperty("names") Set<String> names) {
        return new AutoValue_MetricNamesResponse(names);
    }
}
