package org.graylog2.rest.models.system.metrics.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Collection;

@AutoValue
public abstract class MetricsSummaryResponse {
    @JsonProperty
    public abstract int total();
    @JsonProperty
    public abstract Collection metrics();

    @JsonCreator
    public static MetricsSummaryResponse create(@JsonProperty("total") int total, @JsonProperty("metrics") Collection metrics) {
        return new AutoValue_MetricsSummaryResponse(total, metrics);
    }

    public static MetricsSummaryResponse create(Collection metrics) {
        return new AutoValue_MetricsSummaryResponse(metrics.size(), metrics);
    }
}
